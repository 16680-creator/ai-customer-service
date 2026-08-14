package com.aics.chat.observability;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线上采样评估配置项（前缀 aics.eval.online）
 *
 * <p>对应 docs/15 第 3.3 节「将离线 RAG 评估扩展为线上采样评估和用户反馈闭环」：
 * <ul>
 *   <li>{@code enabled}：线上评估总开关（默认关闭，打开后才开始采样评分）；</li>
 *   <li>{@code sample-rate}：采样率（0~1，默认 0.01，控制 Judge 调用成本）；</li>
 *   <li>{@code judge-model}：Judge 模型名（可空，空则用默认 ChatClient 模型）。</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aics.eval.online")
public class OnlineEvalProperties {

    /** 线上评估总开关（默认关闭） */
    private boolean enabled = false;

    /** 采样率 0~1（默认 0.01） */
    private double sampleRate = 0.01;

    /** Judge 模型名（可空） */
    private String judgeModel = "";
}
