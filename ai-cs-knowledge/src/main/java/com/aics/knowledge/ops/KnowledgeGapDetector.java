package com.aics.knowledge.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库缺口检测 —— 找出"用户常问但知识库答不上来"的主题。
 *
 * <h3>学习要点（技术：缺口分析 / 数据驱动运营）</h3>
 * <ul>
 *   <li><b>思路</b>：对每个主题的代表问题在知识向量库中做检索，
 *       Top-K 命中非空即算"命中"；命中率低于阈值（默认 0.4）标记为知识库缺口。</li>
 *   <li><b>价值</b>：运营据此知道"该补哪块知识"，FAQ 收录有了数据依据。</li>
 *   <li><b>降级</b>：检索异常不计入分母，避免一次抖动误标缺口。</li>
 * </ul>
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