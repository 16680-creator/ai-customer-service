package com.aics.knowledge.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 提问聚类编排服务：聚类 → 缺口检测 → 报告。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionClusterService {

    private final EmbeddingClusterService embeddingClusterService;
    private final KnowledgeGapDetector gapDetector;
    private final OpsProperties properties;

    /**
     * 执行聚类并生成报告。
     *
     * @param period               统计周期
     * @param questions            提问列表
     * @param gapHitRateThreshold  缺口命中率阈值（null 用配置）
     * @return 聚类报告；数据不足返回 INSUFFICIENT_DATA
     */
    public ClusterReport cluster(String period, List<QuestionItem> questions, Double gapHitRateThreshold) {
        ClusterReport report = new ClusterReport();
        report.setPeriod(period);
        report.setTotalQuestions(questions == null ? 0 : questions.size());
        if (questions == null || questions.size() < properties.getMinQuestions()) {
            report.setStatus("INSUFFICIENT_DATA");
            report.setTopics(List.of());
            report.setGapTopics(List.of());
            log.info("聚类数据不足: total={}, min={}", report.getTotalQuestions(), properties.getMinQuestions());
            return report;
        }
        double gapThreshold = gapHitRateThreshold == null
                ? properties.getGapHitRateThreshold() : gapHitRateThreshold;

        List<ClusterTopic> topics = embeddingClusterService.cluster(questions);
        List<ClusterTopic> gaps = new ArrayList<>();
        for (ClusterTopic topic : topics) {
            Double rate = gapDetector.hitRate(topic, "knowledge");
            if (rate != null) {
                topic.setHitRate(rate);
                topic.setGapFlag(rate < gapThreshold);
                if (topic.isGapFlag()) {
                    gaps.add(topic);
                }
            }
        }
        report.setTopics(topics);
        report.setGapTopics(gaps);
        report.setStatus("OK");
        log.info("聚类报告生成: total={}, topics={}, gaps={}", report.getTotalQuestions(),
                topics.size(), gaps.size());
        return report;
    }
}