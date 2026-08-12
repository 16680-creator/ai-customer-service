package com.aics.knowledge.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 提问聚类编排服务 —— 把"聚类"和"缺口检测"串成完整流程。
 *
 * <h3>学习要点（技术：编排层 / 数据量门槛）</h3>
 * <ul>
 *   <li><b>编排职责</b>：先校验数据量（不足 20 条返回 INSUFFICIENT_DATA，
 *       避免小样本产生无意义主题），再聚类 → 逐主题缺口检测 → 汇总报告。</li>
 *   <li><b>状态表达</b>：OK / INSUFFICIENT_DATA 两种状态让前端能区分
 *       "正常结果"与"数据不足提示"。</li>
 * </ul>
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