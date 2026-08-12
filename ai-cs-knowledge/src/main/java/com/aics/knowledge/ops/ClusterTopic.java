package com.aics.knowledge.ops;

import lombok.Data;

import java.util.List;

/**
 * 聚类主题。
 */
@Data
public class ClusterTopic {

    /** 主题名（代表问题） */
    private String topic;

    /** 成员提问 ID */
    private List<Long> questionIds;

    /** 成员数 */
    private int count;

    /** 占比 */
    private double ratio;

    /** 代表问题（高频 Top-3） */
    private List<String> representativeQuestions;

    /** 是否知识库缺口 */
    private boolean gapFlag;

    /** 主题内知识库命中率（可空） */
    private Double hitRate;
}