package com.aics.knowledge.ops;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识库运营配置。
 */
@Data
@ConfigurationProperties(prefix = "aics.rag.cluster")
public class OpsProperties {

    /** 聚类相似度阈值 */
    private double similarityThreshold = 0.82;

    /** 缺口命中率阈值 */
    private double gapHitRateThreshold = 0.4;

    /** 聚类最小提问数（不足则返回 INSUFFICIENT_DATA） */
    private int minQuestions = 20;

    /** 缺口检测抽样检索 Top-K */
    private int gapTopK = 5;
}