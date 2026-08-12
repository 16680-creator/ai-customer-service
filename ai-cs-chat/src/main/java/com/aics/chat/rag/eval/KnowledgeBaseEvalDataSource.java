package com.aics.chat.rag.eval;

import com.aics.chat.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于现有知识库检索链路的评估数据源（默认实现）。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeBaseEvalDataSource implements RagEvalDataSource {

    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public List<Document> retrieve(String knowledgeBase, String query, int topK) {
        return knowledgeBaseService.search(knowledgeBase, query, topK, 0.0);
    }
}