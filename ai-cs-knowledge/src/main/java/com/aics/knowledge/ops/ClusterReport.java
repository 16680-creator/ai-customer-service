package com.aics.knowledge.ops;

import lombok.Data;

import java.util.List;

/**
 * 聚类报告。
 */
@Data
public class ClusterReport {

    /** 统计周期 */
    private String period;

    /** 总提问数 */
    private int totalQuestions;

    /** 主题列表 */
    private List<ClusterTopic> topics;

    /** 缺口主题 */
    private List<ClusterTopic> gapTopics;

    /** OK / INSUFFICIENT_DATA */
    private String status;
}