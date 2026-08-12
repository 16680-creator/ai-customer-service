package com.aics.knowledge.service;

import com.aics.knowledge.entity.KnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识文档向量化服务 —— 知识库"写路径"的向量化入口。
 *
 * <h3>学习要点（技术：RAG 入库链路 / Chunking / Metadata）</h3>
 * <ul>
 *   <li><b>为什么切块（Chunking）</b>：①大模型上下文有限，不能塞整本手册；
 *       ②相似度检索按"片段"匹配，块小更精准；③每块可独立检索、独立溯源。</li>
 *   <li><b>为什么带 metadata</b>：knowledgeBase 用于按库过滤、documentId 用于溯源与删除、
 *       title 用于展示——检索结果靠这些元数据才能"讲清楚出处"。</li>
 *   <li><b>读/写分离</b>：本服务是写入方（写路径），ai-cs-chat 是检索方（读路径），
 *       共用同一 Chroma 集合与同一 bge-m3 模型，保证向量空间一致。</li>
 *   <li><b>异步解耦</b>：知识库写 DB 后经 RocketMQ 异步触发本服务，天然可重试。</li>
 * </ul>
 *
 * <p>核心流程（{@link #vectorize}）：内容校验 → TokenTextSplitter 按 Token 切块 →
 * EmbeddingModel 向量化 → 写 Chroma（附带元数据）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeVectorService {

    /** 知识库向量标识（RAG 对话的 knowledgeBase 参数），写入与检索共用此值 */
    public static final String KNOWLEDGE_BASE = "knowledge";

    /**
     * Spring AI VectorStore（由 ai-cs-chat 或公共配置提供的 Chroma 实现），
     * add 时自动调用 EmbeddingModel 向量化并落库
     */
    private final VectorStore vectorStore;

    /**
     * 向量化文档并入库
     *
     * <p>处理流程：内容校验 → TokenTextSplitter 切块 → 附加元数据 → vectorStore.add
     * （内部调用 EmbeddingModel 生成向量并写入 Chroma）。</p>
     *
     * @param doc 知识文档
     * @return 入库分块数；内容为空返回 0
     */
    public int vectorize(KnowledgeDocument doc) {
        if (doc.getContent() == null || doc.getContent().isBlank()) {
            log.info("文档内容为空，跳过向量化: id={}", doc.getId());
            return 0;
        }
        try {
            // 按 Token 切分（默认配置），适配 bge-m3 的上下文长度限制
            List<Document> chunks = new TokenTextSplitter().split(new Document(doc.getContent()));
            // 为每个分块附加元数据：knowledgeBase 用于检索过滤，documentId/title 用于溯源
            chunks.forEach(chunk -> {
                chunk.getMetadata().put("knowledgeBase", KNOWLEDGE_BASE);
                chunk.getMetadata().put("documentId", doc.getId());
                chunk.getMetadata().put("title", doc.getTitle());
            });
            // add 内部会对每个 chunk 调用 EmbeddingModel 生成向量并写入 Chroma
            vectorStore.add(chunks);
            log.info("文档向量化完成: id={}, title={}, chunks={}", doc.getId(), doc.getTitle(), chunks.size());
            return chunks.size();
        } catch (Exception e) {
            log.error("文档向量化失败: id={}, err={}", doc.getId(), e.getMessage());
            return 0;
        }
    }

    /**
     * 按文档 ID 删除向量片段（通常由 DELETE 同步消息触发）
     *
     * <p>通过 Chroma 的过滤表达式按 documentId 元数据批量删除该文档的所有分块，
     * 保证 DB 删除后向量库无残留。</p>
     *
     * @param documentId 文档 ID
     * @return 删除是否成功
     */
    public boolean deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            return false;
        }
        try {
            // Chroma 过滤表达式：匹配 metadata.documentId == 当前文档 ID 的所有向量
            vectorStore.delete("documentId == '" + documentId + "'");
            log.info("按 documentId 删除向量: documentId={}", documentId);
            return true;
        } catch (Exception e) {
            log.error("按 documentId 删除向量失败: documentId={}, err={}", documentId, e.getMessage());
            return false;
        }
    }
}