package com.aics.chat.service;

import com.aics.chat.rag.DocumentLoader;
import com.aics.chat.rag.rerank.RerankResultItem;
import com.aics.chat.rag.rerank.RerankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务 —— 真正的 RAG（检索增强生成）核心逻辑。
 *
 * <p>把某个知识库的文档切块、向量化后写入 {@link VectorStore}，
 * 提问时通过语义相似度检索出最相关的文档片段，供大模型生成有依据的回答。</p>
 *
 * <h3>【AI 技术详解】RAG 完整链路（两步）</h3>
 * <pre>
 *   【入库】原始文档 ──TokenTextSplitter 切块──▶ 多个文档片段 ──EmbeddingModel 向量化──▶ 写入 VectorStore
 *   【检索】用户问题 ──EmbeddingModel 向量化──▶ 向量宽召回(低阈值) ──▶ Rerank 精排 ──▶ 返回 Top-N 相关片段
 * </pre>
 *
 * <h3>【AI 技术详解】为什么需要对文档做"分块"（Chunking）？</h3>
 * <ul>
 *   <li><b>大模型上下文有限</b>：不能把整本手册塞进去（DeepSeek 64K Token 限制）</li>
 *   <li><b>检索精度</b>：相似度检索是按"片段"匹配的，块越小越精准</li>
 *   <li><b>独立溯源</b>：分块后每个片段可独立检索、独立溯源（引用来源）</li>
 *   <li><b>TokenTextSplitter</b>：按 Token 数量切分（默认 800 Token/块），
 *       保证每个片段在模型上下文窗口内</li>
 * </ul>
 *
 * <h3>【AI 技术详解】两阶段检索（Two-Stage Retrieval）</h3>
 * <ul>
 *   <li><b>第一阶段：宽召回（Recall）</b>：
 *       <ul>
 *         <li>目的：尽量多召回相关文档，宁可多不可少</li>
 *         <li>参数：Top-20，相似度阈值 0.3（较低）</li>
 *         <li>速度：快（向量检索只需计算余弦相似度）</li>
 *       </ul>
 *   </li>
 *   <li><b>第二阶段：精排（Rerank）</b>：
 *       <ul>
 *         <li>目的：从宽召回结果中精选最相关的 Top-N</li>
 *         <li>模型：bge-reranker-v2-m3（交叉编码器，精度高）</li>
 *         <li>过滤：低于 minScore（0.7）的引用不返回</li>
 *         <li>速度：慢（每次都要重新计算）</li>
 *       </ul>
 *   </li>
 *   <li><b>为什么两阶段</b>：向量相似度粗排便宜但精度一般，Rerank 用交叉编码器精排更准；
 *       先粗后精兼顾性能与精度</li>
 * </ul>
 *
 * <h3>【AI 技术详解】元数据（Metadata）的作用</h3>
 * <ul>
 *   <li><b>knowledgeBase</b>：知识库标识，用于按库过滤检索（多知识库隔离）</li>
 *   <li><b>documentId</b>：文档 ID，用于溯源与删除定位</li>
 *   <li><b>title</b>：文档标题，用于前端引用卡片展示</li>
 *   <li><b>page_number</b>：PDF 页码，用于分页溯源</li>
 * </ul>
 *
 * <h3>【技术关联】与 EmbeddingModel 的关系</h3>
 * <ul>
 *   <li>本类不直接调用 EmbeddingModel，而是通过 VectorStore 间接调用</li>
 *   <li>VectorStore.add() 内部会调用 EmbeddingModel 生成向量</li>
 *   <li>VectorStore.similaritySearch() 内部会调用 EmbeddingModel 向量化查询</li>
 *   <li>分离关注点：业务逻辑 vs 向量化细节</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final VectorStore vectorStore;
    private final DocumentLoader documentLoader;

    /**
     * Rerank 精排服务（可选注入：bean 不存在时返回 null，检索退化为基础相似度排序）
     */
    private final ObjectProvider<RerankService> rerankServiceProvider;

    /** 第一阶段宽召回的 Top-K（配置 aics.rag.recall-top-k） */
    @Value("${aics.rag.recall-top-k:20}")
    private int recallTopK = 20;

    /** 第一阶段宽召回的相似度阈值（配置 aics.rag.recall-threshold，质量由 Rerank 精排把关） */
    @Value("${aics.rag.recall-threshold:0.3}")
    private double recallThreshold = 0.3;

    /**
     * 将文本内容写入知识库（入库）。
     *
     * @param knowledgeBase 知识库标识（存入 metadata 的 {@code knowledgeBase} 字段，便于按库过滤检索）
     * @param text          原始文本内容
     * @return 写入的分块文档数量
     */
    public int addText(String knowledgeBase, String text) {
        // 1. 分块：把长文本按 token 切分成多个小片段，每个片段独立检索
        List<Document> chunks = new TokenTextSplitter().split(new Document(text));
        return addChunks(knowledgeBase, chunks);
    }

    /**
     * 将上传的文件（PDF/TXT/Markdown/Office/HTML）写入知识库（入库）。
     *
     * @param knowledgeBase 知识库标识
     * @param file          上传的文档文件
     * @return 写入的分块文档数量
     */
    public int addFile(String knowledgeBase, MultipartFile file) {
        // 把 MultipartFile 转成 Spring Resource，交给 DocumentLoader 读取
        Resource resource = file.getResource();
        List<Document> documents;
        if (isPdf(file)) {
            // PDF：按页读取（metadata 带 page_number）
            documents = documentLoader.loadPdf(resource);
        } else if (isTika(file)) {
            // Office/HTML 等：由 Apache Tika 统一解析（docx/xlsx/html/htm）
            documents = documentLoader.loadTika(resource);
        } else {
            // 其余（txt/md 等纯文本）：按文本读取
            documents = documentLoader.loadText(resource);
        }
        return addChunks(knowledgeBase, documents);
    }

    /**
     * 分块并写入向量库（入库的通用实现）。
     *
     * @param knowledgeBase 知识库标识
     * @param documents     读取到的原始文档列表
     * @return 写入的分块数量
     */
    private int addChunks(String knowledgeBase, List<Document> documents) {
        // 1. 分块：每个 Document 再切分为更小的片段，便于精准检索
        List<Document> chunks = new TokenTextSplitter().apply(documents);

        // 2. 提取文档标题：优先取第一个分块的 metadata.title，其次取原始文档的 title
        String title = extractTitle(chunks, documents);

        // 3. 给每个块打上 metadata：知识库归属 + 文档 ID + 标题，
        //    检索后可据此构建引用溯源（citation）
        chunks.forEach(chunk -> {
            Map<String, Object> meta = chunk.getMetadata();
            meta.put("knowledgeBase", knowledgeBase);
            meta.put("documentId", chunk.getId());
            if (title != null) {
                meta.put("title", title);
            }
        });

        // 4. 写入向量库（内部会调用 EmbeddingModel 把文本转成向量后存储）
        vectorStore.add(chunks);
        log.info("知识库[{}]入库完成, 共{}个分块", knowledgeBase, chunks.size());
        return chunks.size();
    }

    /**
     * 提取文档标题：优先取第一个分块的 metadata.title，其次取原始文档的 title。
     */
    private String extractTitle(List<Document> chunks, List<Document> documents) {
        if (chunks != null && !chunks.isEmpty()) {
            Object t = chunks.get(0).getMetadata().get("title");
            if (t != null) {
                return String.valueOf(t);
            }
        }
        if (documents != null && !documents.isEmpty()) {
            Object t = documents.get(0).getMetadata().get("title");
            if (t != null) {
                return String.valueOf(t);
            }
        }
        return null;
    }

    /**
     * 【AI 核心】语义检索（两阶段）：先向量宽召回，再 Rerank 精排。
     *
     * <p><b>【AI 技术详解】两阶段检索流程</b>：
     * <pre>
     * 阶段一（宽召回）：低阈值 topK=20 向量检索，保证召回率
     *   - 用户问题 → EmbeddingModel 向量化 → VectorStore 余弦相似度检索
     *   - 参数：Top-20，相似度阈值 0.3（较低，宁可多召回）
     *
     * 阶段二（精排）：
     *   - 有 RerankService 时：调用 bge-reranker 精排，按 relevanceScore 排序并过滤 minScore
     *   - 无 RerankService / Rerank 失败返回 null 时：退化按相似度降序取 Top-N
     * </pre>
     *
     * <p><b>【技术关联】与 RerankService 的关系</b>：
     * <ul>
     *   <li>RerankService 是可选依赖（ObjectProvider 注入），不存在时退化为纯向量排序</li>
     *   <li>Rerank 失败时不抛异常，静默降级，保证检索不中断</li>
     * </ul>
     *
     * @param knowledgeBase 知识库标识（用于过滤）
     * @param query         用户问题
     * @param topK          最终返回的最相关条数
     * @param threshold     相似度阈值（低于该值视为"无相关内容"）
     * @return 相关文档片段列表（按 Rerank 后的顺序，或相似度降序）
     */
    public List<Document> search(String knowledgeBase, String query, int topK, double threshold) {
        // 阶段一：宽召回
        List<Document> recallDocs = searchRaw(knowledgeBase, query, recallTopK, recallThreshold);
        if (recallDocs.isEmpty()) {
            return recallDocs;
        }

        // 阶段二：Rerank 精排（可选，bean 不存在或调用失败时退化）
        RerankService rerankService = rerankServiceProvider.getIfAvailable();
        if (rerankService != null) {
            try {
                List<RerankResultItem> reranked = rerankService.rerank(query, recallDocs, topK).block();
                if (reranked != null && !reranked.isEmpty()) {
                    List<Document> result = new ArrayList<>(reranked.size());
                    for (RerankResultItem item : reranked) {
                        int idx = item.getIndex();
                        if (idx >= 0 && idx < recallDocs.size()) {
                            result.add(recallDocs.get(idx));
                        }
                    }
                    log.info("知识库[{}]Rerank精排完成: 召回{}条 -> 返回{}条", knowledgeBase, recallDocs.size(), result.size());
                    return result;
                }
            } catch (Exception e) {
                log.warn("知识库[{}]Rerank调用异常，降级为相似度排序: {}", knowledgeBase, e.getMessage());
            }
        }

        // 退化路径：向量检索结果已按相似度降序，直接取 Top-N
        List<Document> fallback = recallDocs.size() <= topK ? recallDocs : recallDocs.subList(0, topK);
        log.info("知识库[{}]检索完成(相似度排序): 召回{}条 -> 返回{}条", knowledgeBase, recallDocs.size(), fallback.size());
        return fallback;
    }

    /**
     * 阶段一宽召回：以低阈值在向量库中做相似度检索，尽量多召回候选片段。
     */
    private List<Document> searchRaw(String knowledgeBase, String query, int topK, double threshold) {
        // 构造检索请求：query 会被 EmbeddingModel 向量化后做余弦相似度检索
        // bge-m3 检索建议：查询文本加指令前缀，提升语义检索质量
        String retrievalQuery = "为这个句子生成表示以用于检索相关文章：" + query;
        SearchRequest searchRequest = SearchRequest.builder()
                .query(retrievalQuery)
                .topK(topK)
                .similarityThreshold(threshold)
                .filterExpression("knowledgeBase == '" + knowledgeBase + "'") // 只检索当前知识库
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        log.info("知识库[{}]宽召回完成: 命中{}条", knowledgeBase, results.size());
        return results;
    }

    /**
     * 把检索到的文档片段拼装成 AI 可用的上下文文本。
     *
     * @param documents 检索命中的文档片段
     * @return 拼接后的上下文，供注入 Prompt
     */
    public String buildContext(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            sb.append("【资料").append(i + 1).append("】").append(documents.get(i).getText()).append("\n\n");
        }
        return sb.toString().trim();
    }

    /**
     * 判断是否 PDF 文件。
     */
    private boolean isPdf(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase().endsWith(".pdf");
    }

    /**
     * 判断是否 Tika 可解析的文档（Office/HTML）。
     */
    private boolean isTika(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.endsWith(".docx")
                || lower.endsWith(".xlsx")
                || lower.endsWith(".html")
                || lower.endsWith(".htm");
    }
}