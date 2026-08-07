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
 * 将知识库文档内容切块、向量化后写入 Chroma（与 ai-cs-chat 共用 aics-knowledge 集合），
 * RAG 对话使用 knowledgeBase = "knowledge" 即可检索知识库文档。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeVectorService {

    /** 知识库向量标识（RAG 对话的 knowledgeBase 参数） */
    public static final String KNOWLEDGE_BASE = "knowledge";

    private final VectorStore vectorStore;

    /**
     * 向量化文档并入库
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
            List<Document> chunks = new TokenTextSplitter().split(new Document(doc.getContent()));
            chunks.forEach(chunk -> {
                chunk.getMetadata().put("knowledgeBase", KNOWLEDGE_BASE);
                chunk.getMetadata().put("documentId", doc.getId());
                chunk.getMetadata().put("title", doc.getTitle());
            });
            vectorStore.add(chunks);
            log.info("文档向量化完成: id={}, title={}, chunks={}", doc.getId(), doc.getTitle(), chunks.size());
            return chunks.size();
        } catch (Exception e) {
            log.error("文档向量化失败: id={}, err={}", doc.getId(), e.getMessage());
            return 0;
        }
    }
}