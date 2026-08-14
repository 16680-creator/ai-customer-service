package com.aics.chat.rag.eval;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 评估用检索数据源抽象（复用真实检索链路）。
 */
public interface RagEvalDataSource {

    /**
     * 按知识库检索。
     *
     * @param knowledgeBase 知识库标识
     * @param query         查询
     * @param topK          Top-K
     * @return 命中文档（按相关度降序）
     */
    List<Document> retrieve(String knowledgeBase, String query, int topK);
}