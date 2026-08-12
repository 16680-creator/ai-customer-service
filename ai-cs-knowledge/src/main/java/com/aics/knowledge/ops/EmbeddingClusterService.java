package com.aics.knowledge.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Embedding 向量的贪心聚类 —— 高频问题自动归类。
 *
 * <h3>学习要点（技术：Embedding + 余弦相似度 + 贪心聚类）</h3>
 * <ul>
 *   <li><b>为什么用向量聚类</b>：不同用户问"怎么退款"和"退款多久到账"字面不同，
 *       但语义相近；用 bge-m3 向量后按余弦相似度归组，能识别同主题问题。</li>
 *   <li><b>贪心算法</b>：逐条处理，与已有主题质心（centroid）比较：
 *       相似度 ≥ 阈值归入该主题并更新质心，否则新建主题。简单、可单测。</li>
 *   <li><b>余弦相似度</b>：{@code cos(a,b) = dot(a,b) / (|a|·|b|)}，衡量方向一致性，
 *       与向量长度无关，适合文本相似度。</li>
 *   <li><b>主题名</b>：取成员中出现频次最高的提问文本作为代表问题。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingClusterService {

    private final EmbeddingModel embeddingModel;
    private final OpsProperties properties;

    /**
     * 执行贪心聚类。
     *
     * @param questions 提问列表（含 ID 与文本）
     * @return 主题列表（按成员数降序）；空文本条目跳过
     */
    public List<ClusterTopic> cluster(List<QuestionItem> questions) {
        List<ClusterTopic> topics = new ArrayList<>();
        List<float[]> centroids = new ArrayList<>();
        Map<String, Integer> topicIndexByName = new LinkedHashMap<>();

        for (QuestionItem q : questions) {
            if (q == null || !StringUtils.hasText(q.getText())) {
                continue;   // 跳过空文本
            }
            float[] vector = embed(q.getText());   // bge-m3 向量化（失败返回 null）
            if (vector == null) {
                continue;   // 向量化失败跳过该条，不中断聚类
            }
            int bestIndex = -1;
            double bestSimilarity = 0;
            // 贪心：找与当前向量最相似的主题质心
            for (int i = 0; i < centroids.size(); i++) {
                double sim = cosine(vector, centroids.get(i));
                if (sim > bestSimilarity) {
                    bestSimilarity = sim;
                    bestIndex = i;
                }
            }
            // 相似度达到阈值 -> 归入已有主题并更新质心；否则新建主题
            if (bestIndex >= 0 && bestSimilarity >= properties.getSimilarityThreshold()) {
                ClusterTopic topic = topics.get(bestIndex);
                topic.getQuestionIds().add(q.getId());
                topic.setCount(topic.getCount() + 1);
                // 更新质心：增量平均（新向量按成员数加权）
                centroids.set(bestIndex, average(centroids.get(bestIndex), vector, topic.getCount()));
            } else {
                ClusterTopic topic = new ClusterTopic();
                topic.setTopic(q.getText());
                topic.setQuestionIds(new ArrayList<>(List.of(q.getId())));
                topic.setCount(1);
                topics.add(topic);
                centroids.add(vector);   // 新主题的质心 = 当前向量
            }
        }
        // 代表问题：取成员数最多的主题，代表问题即主题名（高频问题文本）
        int total = 0;
        for (ClusterTopic t : topics) {
            total += t.getCount();
        }
        for (ClusterTopic t : topics) {
            t.setRatio(total > 0 ? (double) t.getCount() / total : 0);
            t.setRepresentativeQuestions(List.of(t.getTopic()));
        }
        topics.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        log.info("提问聚类完成: questions={}, topics={}", questions.size(), topics.size());
        return topics;
    }

    private float[] embed(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            log.warn("提问向量化失败: err={}", e.getMessage());
            return null;
        }
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static float[] average(float[] centroid, float[] vector, int count) {
        float[] avg = new float[centroid.length];
        for (int i = 0; i < centroid.length; i++) {
            avg[i] = (centroid[i] * (count - 1) + vector[i]) / count;
        }
        return avg;
    }
}