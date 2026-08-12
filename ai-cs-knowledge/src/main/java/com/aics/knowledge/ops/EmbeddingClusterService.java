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
 * 基于 Embedding 向量余弦相似度的贪心聚类。
 *
 * <p>逐条计算提问向量，与已有主题质心比较：相似度 ≥ 阈值则归入该主题并更新质心，
 * 否则新建主题。主题名为成员中出现频次最高的提问文本。</p>
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
                continue;
            }
            float[] vector = embed(q.getText());
            if (vector == null) {
                continue;
            }
            int bestIndex = -1;
            double bestSimilarity = 0;
            for (int i = 0; i < centroids.size(); i++) {
                double sim = cosine(vector, centroids.get(i));
                if (sim > bestSimilarity) {
                    bestSimilarity = sim;
                    bestIndex = i;
                }
            }
            if (bestIndex >= 0 && bestSimilarity >= properties.getSimilarityThreshold()) {
                ClusterTopic topic = topics.get(bestIndex);
                topic.getQuestionIds().add(q.getId());
                topic.setCount(topic.getCount() + 1);
                // 更新质心
                centroids.set(bestIndex, average(centroids.get(bestIndex), vector, topic.getCount()));
            } else {
                ClusterTopic topic = new ClusterTopic();
                topic.setTopic(q.getText());
                topic.setQuestionIds(new ArrayList<>(List.of(q.getId())));
                topic.setCount(1);
                topics.add(topic);
                centroids.add(vector);
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