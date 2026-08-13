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
 * <h3>【AI 技术详解】知识库缺口分析</h3>
 * <ul>
 *   <li><b>问题</b>：用户经常问某些问题，但知识库没有相关内容，导致 AI 回答质量差</li>
 *   <li><b>方案</b>：对每个主题的代表问题在知识向量库中做检索，
 *       Top-K 命中非空即算"命中"；命中率低于阈值（默认 0.4）标记为知识库缺口</li>
 *   <li><b>价值</b>：运营据此知道"该补哪块知识"，FAQ 收录有了数据依据</li>
 * </ul>
 *
 * <h3>【AI 技术详解】缺口检测流程</h3>
 * <pre>
 *   1. 获取聚类主题（来自 EmbeddingClusterService）
 *   2. 对每个主题的代表问题做向量检索
 *   3. 计算命中率 = 命中代表问题数 / 总代表问题数
 *   4. 命中率 &lt; 阈值 → 标记为"知识库缺口"
 *   5. 运营据此补充知识库内容
 * </pre>
 *
 * <h3>【技术关联】与 EmbeddingClusterService 的关系</h3>
 * <pre>
 *   知识库运维流程：
 *       EmbeddingClusterService.cluster()  // 聚类高频问题
 *           ↓
 *       KnowledgeGapDetector.hitRate()     // 检测知识缺口 ← 本类
 *           ↓
 *       运营补充知识库内容
 *           ↓
 *       RagEvalServiceImpl.evaluate()      // 评估补充效果
 * </pre>
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
        String kb = knowledgeBase == null ? "knowledge" : knowledgeBase;   // 默认检索 knowledge 库
        for (String q : topic.getRepresentativeQuestions()) {
            total++;
            try {
                // 用代表问题在知识向量库检索：Top-K 非空即算"命中"
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
                log.warn("缺口检测检索失败: query={}, err={}", q, e.getMessage());   // 检索异常不计入分母
            }
        }
        if (total == 0) {
            return null;
        }
        double rate = (double) hit / total;   // 命中率 = 命中代表问题数 / 总代表问题数
        topic.setHitRate(rate);
        // 命中率低于阈值 -> 标记为"知识库缺口"（用户常问但知识库答不上来）
        topic.setGapFlag(rate < properties.getGapHitRateThreshold());
        return rate;
    }
}