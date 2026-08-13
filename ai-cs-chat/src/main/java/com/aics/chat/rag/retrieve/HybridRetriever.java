package com.aics.chat.rag.retrieve;

import com.aics.chat.dto.ChatHybridPageVO;
import com.aics.chat.dto.ChatHybridSearchResult;
import com.aics.chat.feign.SearchFeignClient;
import com.aics.chat.rag.graph.GraphRagService;
import com.aics.chat.rag.graph.GraphTriple;
import com.aics.chat.rag.rewrite.QueryRewriteService;
import com.aics.chat.rag.rewrite.RewriteResult;
import com.aics.chat.service.KnowledgeBaseService;
import com.aics.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话侧统一检索编排 —— 把多种检索技术组合成一个入口。
 *
 * <h3>【AI 技术详解】多路检索编排（Multi-Route Retrieval Orchestration）</h3>
 * <ul>
 *   <li><b>为什么需要多路检索</b>：
 *       <ul>
 *         <li><b>向量检索</b>：擅长语义相似（"退款"和"退货"），但对精确串（订单号 ORD001）差</li>
 *         <li><b>关键词检索（BM25）</b>：擅长精确匹配，但对语义相似差</li>
 *         <li><b>两者互补</b>：Hybrid 混合检索可以兼顾精确与语义</li>
 *       </ul>
 *   </li>
 *   <li><b>四种检索模式</b>：
 *       <ol>
 *         <li><b>VECTOR</b>：纯向量检索（默认，存量兼容）</li>
 *         <li><b>HYBRID</b>：ES 关键词 + 向量语义 + RRF 融合</li>
 *         <li><b>HYBRID_QUERY_REWRITE</b>：混合 + LLM 改写/HyDE（提升模糊问题召回）</li>
 *         <li><b>GRAPH_RAG</b>：图谱优先，未命中降级（适合实体关联场景）</li>
 *       </ol>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】Hybrid 混合检索原理</h3>
 * <pre>
 *   用户问题 ──┬── EmbeddingModel 向量化 ──→ VectorStore 余弦相似度检索 ──→ 向量结果
 *              │
 *              └── 关键词提取 ──→ Elasticsearch BM25 检索 ──→ 关键词结果
 *                                                                      │
 *                                                                      ▼
 *                                                              RRF 融合排序
 *                                                                      │
 *                                                                      ▼
 *                                                              最终 Top-K 结果
 * </pre>
 *
 * <h3>【AI 技术详解】RRF（Reciprocal Rank Fusion）融合算法</h3>
 * <ul>
 *   <li><b>核心公式</b>：{@code score(doc) = Σ 1/(k + rank_i)}，k 通常取 60</li>
 *   <li><b>为什么有效</b>：在多路中都靠前的文档总分最高，实现"共识优先"</li>
 *   <li><b>无需训练</b>：RRF 不需要标注数据，直接基于排名融合，简单有效</li>
 * </ul>
 *
 * <h3>【AI 技术详解】查询改写与 HyDE</h3>
 * <ul>
 *   <li><b>查询改写</b>：LLM 把模糊问题（"那个功能怎么用"）拆成多个精确子查询</li>
 *   <li><b>HyDE</b>：Hypothetical Document Embeddings —— LLM 先生成"假设性标准答案"，
 *       用它的向量去检索（假设文档比问题包含更多关键词，命中率更高）</li>
 * </ul>
 *
 * <h3>【AI 技术详解】GraphRAG 图谱检索</h3>
 * <ul>
 *   <li><b>适用场景</b>：实体关联查询（如"张三买了什么商品？"需要关联用户→订单→商品）</li>
 *   <li><b>原理</b>：从问题中抽取实体 → 在知识图谱中多跳展开 → 返回关联三元组</li>
 *   <li><b>与向量检索的区别</b>：向量检索是"语义相似"，图谱检索是"关系遍历"</li>
 * </ul>
 *
 * <h3>降级设计</h3>
 * <ul>
 *   <li><b>策略模式</b>：四种检索模式通过 {@link RetrievalMode} 枚举分派，新增模式无需改动调用方</li>
 *   <li><b>两层兜底</b>：①全局开关未开启时直接回退纯向量；②外部依赖异常时 catch 后回退纯向量</li>
 *   <li><b>核心保证</b>："核心对话永远可用"，增强功能失败不影响基本回答</li>
 * </ul>
 *
 * <h3>【技术关联】与搜索服务（ai-cs-search）的关系</h3>
 * <ul>
 *   <li>Hybrid 检索通过 Feign 调用 ai-cs-search 的 /search/hybrid 接口</li>
 *   <li>搜索服务内部完成 ES + 向量 + RRF 融合，返回最终结果</li>
 *   <li>跨服务调用走 HTTP 契约，遵守微服务间不直接依赖内部类的约束</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetriever {

    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorStore vectorStore;
    private final SearchFeignClient searchFeignClient;
    private final QueryRewriteService queryRewriteService;
    private final GraphRagService graphRagService;
    private final RagRetrieveProperties properties;

    /**
     * 执行检索：按模式分派，返回带降级信息的统一结果。
     *
     * <p>调用方（如 RAG 对话）只依赖 {@link RetrieveResult} 的 documents 字段，
     * 无需关心内部用了哪种技术——这正是"编排层"的价值。</p>
     *
     * @param knowledgeBase 知识库标识
     * @param query         查询
     * @param mode          请求的检索模式
     * @param topK          Top-K
     * @return 检索结果（含实际模式与降级信息）
     */
    public RetrieveResult retrieve(String knowledgeBase, String query, RetrievalMode mode, int topK) {
        int k = topK <= 0 ? 5 : topK;   // Top-K 兜底：非法值用默认 5
        switch (mode) {                  // 策略模式：按枚举分派到不同检索实现
            case HYBRID -> {             // Hybrid = ES 关键词 + 向量语义 + RRF 融合
                if (!properties.isHybridEnabled()) {
                    // 全局开关默认关闭（Nacos aics.rag.hybrid-enabled），保证存量纯向量行为不变
                    return vector(knowledgeBase, query, k, "hybrid 全局未启用");
                }
                return hybrid(knowledgeBase, query, k);
            }
            case HYBRID_QUERY_REWRITE -> {   // 混合 + LLM 改写/HyDE
                if (!properties.isRewriteEnabled()) {
                    return vector(knowledgeBase, query, k, "rewrite 全局未启用");
                }
                return rewriteHybrid(knowledgeBase, query, k);
            }
            case GRAPH_RAG -> {              // GraphRAG：图谱优先，未命中降级
                if (!properties.isGraphEnabled()) {
                    return vector(knowledgeBase, query, k, "graph 全局未启用");
                }
                return graph(knowledgeBase, query, k);
            }
            default -> {
                return vector(knowledgeBase, query, k, null);   // VECTOR：存量默认路径
            }
        }
    }

    /**
     * 纯向量检索 —— 存量默认路径。
     *
     * <p>底层是 {@link KnowledgeBaseService#search}：把问题用 bge-m3 向量化后，
     * 在 Chroma 中做余弦相似度 Top-K 检索，并按 knowledgeBase 元数据过滤。</p>
     */
    private RetrieveResult vector(String knowledgeBase, String query, int topK, String degradeReason) {
        // 底层：问题经 bge-m3 向量化 -> Chroma 余弦相似度 Top-K 检索 -> 按 knowledgeBase 元数据过滤
        List<Document> docs = knowledgeBaseService.search(knowledgeBase, query, topK, 0.0);
        return buildResult(query, docs, RetrievalMode.VECTOR.name(), degradeReason);
    }

    /**
     * 【AI 核心】Hybrid 混合检索 —— 通过 Feign 调用 ai-cs-search 的 /search/hybrid。
     *
     * <p><b>【AI 技术详解】为什么需要混合检索</b>：
     * <ul>
     *   <li><b>向量检索的局限</b>：对语义相似好（"退款"≈"退货"），但对精确串（订单号 ORD001）差</li>
     *   <li><b>关键词检索的局限</b>：对精确匹配好，但对语义相似差（"退款"≠"退货"）</li>
     *   <li><b>混合检索的优势</b>：RRF 融合两者，兼顾精确与语义</li>
     * </ul>
     *
     * <p><b>【技术关联】跨服务调用</b>：
     * <ul>
     *   <li>Hybrid 检索通过 Feign 调用 ai-cs-search 的 /search/hybrid 接口</li>
     *   <li>搜索服务内部完成 ES + 向量 + RRF 融合，返回最终结果</li>
     *   <li>跨服务调用走 HTTP 契约，遵守微服务间不直接依赖内部类的约束</li>
     * </ul>
     */
    private RetrieveResult hybrid(String knowledgeBase, String query, int topK) {
        try {
            // 跨服务调用：Feign 调 ai-cs-search /search/hybrid（ES 与向量在搜索服务内完成 RRF 融合）
            Result<ChatHybridPageVO> result = searchFeignClient.hybridSearch(knowledgeBase, query, 1, topK);
            if (result == null || !result.isSuccess() || result.getData() == null
                    || result.getData().getRecords() == null) {
                // 搜索服务返回异常/空 -> 降级纯向量，回答不中断
                return vector(knowledgeBase, query, topK, "搜索服务无数据");
            }
            List<Document> docs = new ArrayList<>();
            // 把搜索服务的混合结果转成 Spring AI Document（统一检索结果的数据形态）
            for (ChatHybridSearchResult r : result.getData().getRecords()) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("knowledgeBase", knowledgeBase);   // 保留知识库归属（后续引用溯源/过滤用）
                if (r.getDocumentId() != null) {
                    meta.put("documentId", r.getDocumentId());   // 文档 ID：溯源与删除定位
                }
                if (r.getTitle() != null) {
                    meta.put("title", r.getTitle());             // 标题：前端引用卡片展示
                }
                if (r.getPage() != null) {
                    meta.put("page_number", r.getPage());        // 页码：PDF 分页溯源
                }
                Document doc = Document.builder()
                        .id(r.getDocumentId() != null ? r.getDocumentId() : "hybrid-" + docs.size())
                        .text(r.getContent() == null ? "" : r.getContent())
                        .metadata(meta)
                        .score(r.getScore())   // RRF 融合分写入 score，供排序/展示
                        .build();
                docs.add(doc);
            }
            return buildResult(query, docs, RetrievalMode.HYBRID.name(), null);
        } catch (Exception e) {
            // 任何异常（服务不可达/超时）都降级，保证核心对话可用性
            log.warn("混合检索失败，降级纯向量: kb={}, query={}, err={}", knowledgeBase, query, e.getMessage());
            return vector(knowledgeBase, query, topK, "搜索服务不可用");
        }
    }

    /**
     * 【AI 核心】查询改写 + HyDE —— 提升模糊问题的召回。
     *
     * <p><b>【AI 技术详解】查询改写（Query Rewrite）</b>：
     * <ul>
     *   <li><b>问题</b>：用户口语化问题（"那个功能怎么用"）直接检索召回差</li>
     *   <li><b>方案</b>：LLM 把模糊问题拆成多个精确子查询，扩大召回面</li>
     *   <li><b>示例</b>："那个功能怎么用" → ["退款功能使用方法", "如何申请退款", "退款流程"]</li>
     * </ul>
     *
     * <p><b>【AI 技术详解】HyDE（Hypothetical Document Embeddings）</b>：
     * <ul>
     *   <li><b>原理</b>：让 LLM 先生成"假设性标准答案文档"，用它的向量去检索</li>
     *   <li><b>为什么有效</b>：假设文档比问题包含更多关键词，与真实知识文档更相似</li>
     *   <li><b>流程</b>：
     *       <ol>
     *         <li>用户问"如何退款"</li>
     *         <li>LLM 生成假设文档："退款流程如下：1. 登录账号 2. 进入订单详情 3. 点击申请退款..."</li>
     *         <li>用假设文档的向量去检索，命中率更高</li>
     *       </ol>
     *   </li>
     * </ul>
     *
     * <p><b>【技术关联】多路结果融合</b>：
     * 查询改写后有多路子查询结果、还有 HyDE 结果，需要用 {@link MultiQueryMerger}（RRF）融合去重。
     * RRF 核心公式：{@code score(doc) = Σ 1/(k + rank_i)}，在多路中都靠前的文档总分最高。</p>
     */
    private RetrieveResult rewriteHybrid(String knowledgeBase, String query, int topK) {
        RewriteResult rewrite = queryRewriteService.rewrite(query);
        List<String> queries = new ArrayList<>();
        if (rewrite.getSubQueries() != null && !rewrite.getSubQueries().isEmpty()) {
            queries.addAll(rewrite.getSubQueries());
        }
        if (queries.isEmpty()) {
            queries.add(query);
        }
        List<List<Document>> lists = new ArrayList<>();
        for (String q : queries) {
            lists.add(searchVector(knowledgeBase, q, topK));
        }
        if (StringUtils.hasText(rewrite.getHydeDocument())) {
            lists.add(searchVector(knowledgeBase, rewrite.getHydeDocument(), topK));
        }
        boolean degraded = rewrite.getSubQueries() == null || rewrite.getSubQueries().isEmpty();
        List<Document> merged = MultiQueryMerger.merge(lists, topK, properties.getRrfK());
        return buildResult(query, merged, RetrievalMode.HYBRID_QUERY_REWRITE.name(),
                degraded ? "查询改写失败，使用原始问题" : null);
    }

    /** GraphRAG：图谱命中则补充上下文，否则普通向量 */
    private RetrieveResult graph(String knowledgeBase, String query, int topK) {
        List<GraphTriple> triples = graphRagService.retrieveWithGraph(query, knowledgeBase);
        List<Document> docs = searchVector(knowledgeBase, query, topK);
        if (!triples.isEmpty()) {
            List<Document> graphDocs = new ArrayList<>();
            for (GraphTriple t : triples) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("knowledgeBase", knowledgeBase);
                meta.put("source", "graph");
                meta.put("subject", t.getSubject());
                graphDocs.add(new Document("graph-" + t.getId(),
                        t.getSubject() + " " + t.getPredicate() + " " + t.getObject(), meta));
            }
            // 图谱上下文置于最前
            List<Document> combined = new ArrayList<>(graphDocs);
            combined.addAll(docs);
            return buildResult(query, combined, RetrievalMode.GRAPH_RAG.name(), null);
        }
        return buildResult(query, docs, RetrievalMode.GRAPH_RAG.name(),
                "图谱未命中，降级普通检索");
    }

    private List<Document> searchVector(String knowledgeBase, String query, int topK) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("knowledgeBase == '" + knowledgeBase + "'")
                    .build();
            List<Document> docs = vectorStore.similaritySearch(request);
            if (docs == null) {
                return List.of();
            }
            for (Document d : docs) {
                d.getMetadata().putIfAbsent("knowledgeBase", knowledgeBase);
            }
            return docs;
        } catch (Exception e) {
            log.warn("向量检索失败: kb={}, query={}, err={}", knowledgeBase, query, e.getMessage());
            return List.of();
        }
    }

    private RetrieveResult buildResult(String query, List<Document> docs, String mode, String degradeReason) {
        RetrieveResult result = new RetrieveResult();
        result.setQuery(query);
        result.setDocuments(docs);
        result.setMode(mode);
        result.setDegraded(degradeReason != null);
        result.setDegradeReason(degradeReason);
        return result;
    }
}