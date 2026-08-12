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
 * 知识文档向量化服务
 *
 * <p>将知识库文档内容切块、向量化后写入 Chroma（与 ai-cs-chat 共用 aics-knowledge 集合），
 * RAG 对话使用 knowledgeBase = "knowledge" 即可检索知识库文档。</p>
 *
 * <p>核心流程（{@link #vectorize}）：</p>
 * <ol>
 *   <li>内容校验：空内容直接跳过</li>
 *   <li>切分：使用 Spring AI 的 TokenTextSplitter 按 Token 切块（适配 bge-m3 上下文窗口）</li>
 *   <li>Embedding：由 {@link com.aics.knowledge.config.KnowledgeAiConfig} 注入的
 *       EmbeddingModel（硅基流动 bge-m3）生成向量</li>
 *   <li>写 Chroma：每个分块附带 knowledgeBase / documentId / title 元数据，
 *       供 RAG 检索过滤与溯源</li>
 * </ol>
 *
 * <p>为什么知识库模块自己也要向量化：</p>
 * <ul>
 *   <li>ai-cs-chat 的 VectorStore 主要面向"检索"（读路径），而知识库模块是"写入方"（写路径），
 *       两者共用同一 Chroma 集合与同一 Embedding 模型，向量空间必须一致</li>
 *   <li>知识库模块负责自身内容的全生命周期（创建/更新/删除 → 切块/Embedding/写库/删库），
 *       避免对话模块耦合知识库内部结构</li>
 *   <li>通过 RocketMQ 解耦：知识库写 DB 后异步向量化，对话侧无感</li>
 * </ul>
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