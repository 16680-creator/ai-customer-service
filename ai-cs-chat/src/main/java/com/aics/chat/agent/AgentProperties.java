package com.aics.chat.agent;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 编排配置项（前缀 aics.agent）
 *
 * <p>所有阈值与限制均可配置，对应验收指标「Agent 最大步骤数、超时、失败降级均可配置」。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aics.agent")
public class AgentProperties {

    /** 单次 run 最大步骤数 */
    private int maxSteps = 12;

    /** 单步超时（毫秒） */
    private long stepTimeoutMs = 15000;

    /** run 总超时（毫秒） */
    private long totalTimeoutMs = 60000;

    /** 写操作确认超时（分钟） */
    private int confirmTimeoutMinutes = 10;

    /** 意图置信度阈值，低于该值不路由工具 */
    private double intentThreshold = 0.6;

    /** 同价位商品召回价格容差（±百分比，0.15 表示 ±15%） */
    private double priceTolerance = 0.15;

    /** 售后规则知识库标识 */
    private String ruleKnowledgeBase = "after-sale-rules";

    /** 规则检索 TopK */
    private int ruleTopK = 3;

    /** 规则检索相似度阈值 */
    private double ruleSimilarityThreshold = 0.7;

    /** 触发转人工的情绪（ANGRY） */
    private String sentimentHandoff = "ANGRY";

    /** 写操作失败重试次数 */
    private int writeRetryTimes = 1;

    /** 是否启用 LLM 意图识别（false 直接走规则兜底） */
    private boolean llmIntentEnabled = true;

    /** 推荐默认数量上限 */
    private int recommendLimit = 3;
}
