package com.aics.knowledge.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库缺口检测：对主题内代表问题做知识检索，命中率低于阈值即标记为缺口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGapDetector {

    private final VectorStore vectorStore;
    private final OpsProperties properties;

    /**
     * 计算主题的知识库命中率（代表问题检索 Top-K 中非空即命中）。
     *
     * @param topic    主题
     * @param knowledgeBase 知识库标识（默认 knowledge）
     * @return 命中率；检索异常返回 null
     */
    public Double hitRate(ClusterTopic topic, String knowledgeBase) {
        if (topic == null || topic.getRepresentativeQuestions() == null
                || topic.getRepresentativeQuestions().isEmpty()) {
            return null;
        }
        int hit = 0;
        int total = 0;
        String kb = knowledgeBase == null ? "knowledge" : knowledgeBase;
        for (String q : topic.getRepresentativeQuestions()) {
            total++;
            try {
                SearchRequest request = SearchRequest.builder()
                        .query(q)
                        .topK(properties.getGapTopK())
                        .filterExpression("knowledgeBase == '" + kb + "'")
                        .build();
                List<Document> docs = vectorStore.similaritySearch(request);
                if (docs != null && !docs.isEmpty()) {
                    hit++;
                }
            } catch (Exception e) {
                log.warn("缺口检测检索失败: query={}, err={}", q, e.getMessage());
            }
        }
        if (total == 0) {
            return null;
        }
        double rate = (double) hit / total;
        topic.setHitRate(rate);
        topic.setGapFlag(rate < properties.getGapHitRateThreshold());
        return rate;
    }
}