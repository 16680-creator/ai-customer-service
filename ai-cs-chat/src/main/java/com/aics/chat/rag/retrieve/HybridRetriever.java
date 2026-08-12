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
 * <h3>学习要点（技术：多路检索编排 / 优雅降级）</h3>
 * <ul>
 *   <li><b>策略模式</b>：四种检索模式（向量/Hybrid/改写/图谱）通过 {@link RetrievalMode}
 *       枚举分派到不同方法，新增模式无需改动调用方。</li>
 *   <li><b>降级设计</b>：每种增强都有两层兜底——①全局开关未开启时直接回退纯向量；
 *       ②外部依赖（搜索服务/LLM/图谱）异常时 catch 后回退纯向量。保证"核心对话永远可用"。</li>
 *   <li><b>存量兼容</b>：默认 VECTOR，业务方不传新参数行为不变，这是渐进式改造的关键。</li>
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
        int k = topK <= 0 ? 5 : topK;
        switch (mode) {
            case HYBRID -> {
                if (!properties.isHybridEnabled()) {
                    return vector(knowledgeBase, query, k, "hybrid 全局未启用");
                }
                return hybrid(knowledgeBase, query, k);
            }
            case HYBRID_QUERY_REWRITE -> {
                if (!properties.isRewriteEnabled()) {
                    return vector(knowledgeBase, query, k, "rewrite 全局未启用");
                }
                return rewriteHybrid(knowledgeBase, query, k);
            }
            case GRAPH_RAG -> {
                if (!properties.isGraphEnabled()) {
                    return vector(knowledgeBase, query, k, "graph 全局未启用");
                }
                return graph(knowledgeBase, query, k);
            }
            default -> {
                return vector(knowledgeBase, query, k, null);
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
        List<Document> docs = knowledgeBaseService.search(knowledgeBase, query, topK, 0.0);
        return buildResult(query, docs, RetrievalMode.VECTOR.name(), degradeReason);
    }

    /**
     * Hybrid 混合检索 —— 通过 Feign 调用 ai-cs-search 的 /search/hybrid。
     *
     * <p><b>为什么混合</b>：向量检索对语义相似好、对精确串（型号/订单号）差；
     * ES 关键词(BM25)恰好相反。两者 RRF 融合可兼顾精确与语义。
     * 跨服务调用走 Feign（HTTP 契约），遵守微服务间不直接依赖内部类的约束。</p>
     */
    private RetrieveResult hybrid(String knowledgeBase, String query, int topK) {
        try {
            Result<ChatHybridPageVO> result = searchFeignClient.hybridSearch(knowledgeBase, query, 1, topK);
            if (result == null || !result.isSuccess() || result.getData() == null
                    || result.getData().getRecords() == null) {
                return vector(knowledgeBase, query, topK, "搜索服务无数据");
            }
            List<Document> docs = new ArrayList<>();
            for (ChatHybridSearchResult r : result.getData().getRecords()) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("knowledgeBase", knowledgeBase);
                if (r.getDocumentId() != null) {
                    meta.put("documentId", r.getDocumentId());
                }
                if (r.getTitle() != null) {
                    meta.put("title", r.getTitle());
                }
                if (r.getPage() != null) {
                    meta.put("page_number", r.getPage());
                }
                Document doc = Document.builder()
                        .id(r.getDocumentId() != null ? r.getDocumentId() : "hybrid-" + docs.size())
                        .text(r.getContent() == null ? "" : r.getContent())
                        .metadata(meta)
                        .score(r.getScore())
                        .build();
                docs.add(doc);
            }
            return buildResult(query, docs, RetrievalMode.HYBRID.name(), null);
        } catch (Exception e) {
            log.warn("混合检索失败，降级纯向量: kb={}, query={}, err={}", knowledgeBase, query, e.getMessage());
            return vector(knowledgeBase, query, topK, "搜索服务不可用");
        }
    }

    /**
     * 查询改写 + HyDE —— 提升模糊问题的召回。
     *
     * <p><b>查询改写</b>：LLM 把"那个功能怎么用"拆成多个精确子查询，扩大召回面；
     * <b>HyDE</b>（Hypothetical Document Embeddings）：让 LLM 先生成"假设性标准答案文档"，
     * 用它的向量去检索——假设文档比问题本身包含更多关键词，命中率更高。
     * 多路结果用 {@link MultiQueryMerger}（RRF）融合去重。</p>
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