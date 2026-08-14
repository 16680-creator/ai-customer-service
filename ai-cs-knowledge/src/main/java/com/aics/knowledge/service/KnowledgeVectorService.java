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
 * <h3>【AI 技术详解】RAG 入库链路</h3>
 * <pre>
 *   原始文档 → TokenTextSplitter 切块 → EmbeddingModel 向量化 → 写入 Chroma
 *   （附带元数据：knowledgeBase、documentId、title）
 * </pre>
 *
 * <h3>【AI 技术详解】为什么需要切块（Chunking）</h3>
 * <ul>
 *   <li><b>大模型上下文有限</b>：不能把整本手册塞进去（DeepSeek 64K Token 限制）</li>
 *   <li><b>检索精度</b>：相似度检索是按"片段"匹配的，块越小越精准</li>
 *   <li><b>独立溯源</b>：分块后每个片段可独立检索、独立溯源（引用来源）</li>
 *   <li><b>TokenTextSplitter</b>：按 Token 数量切分（默认 800 Token/块），
 *       保证每个片段在模型上下文窗口内</li>
 * </ul>
 *
 * <h3>【AI 技术详解】元数据（Metadata）的作用</h3>
 * <ul>
 *   <li><b>knowledgeBase</b>：知识库标识，用于按库过滤检索（多知识库隔离）</li>
 *   <li><b>documentId</b>：文档 ID，用于溯源与删除定位</li>
 *   <li><b>title</b>：文档标题，用于前端引用卡片展示</li>
 * </ul>
 *
 * <h3>【技术关联】读/写分离架构</h3>
 * <ul>
 *   <li><b>写路径（本服务）</b>：知识库写 DB 后经 RocketMQ 异步触发向量化</li>
 *   <li><b>读路径（ai-cs-chat）</b>：对话时做向量检索</li>
 *   <li><b>一致性保证</b>：共用同一 Chroma 集合与同一 bge-m3 模型，保证向量空间一致</li>
 *   <li><b>异步解耦</b>：知识库写 DB 后经 RocketMQ 异步触发本服务，天然可重试</li>
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
            // 1) 切块：按 Token 切分（适配 bge-m3 上下文窗口），长文档变成多个可独立检索的小片段
            List<Document> chunks = new TokenTextSplitter().split(new Document(doc.getContent()));
            // 2) 为每个分块附加元数据：knowledgeBase 用于检索过滤，documentId/title 用于溯源
            chunks.forEach(chunk -> {
                chunk.getMetadata().put("knowledgeBase", KNOWLEDGE_BASE);
                chunk.getMetadata().put("documentId", doc.getId());
                chunk.getMetadata().put("title", doc.getTitle());
            });
            // 3) add 内部会对每个 chunk 调用 EmbeddingModel 生成向量并写入 Chroma（入库）
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