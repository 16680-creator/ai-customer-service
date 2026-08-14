package com.aics.chat.observability;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 模型用量计量配置项（前缀 aics.usage）
 *
 * <p>对应 docs/15 第 3.3 节「Token 与成本治理」：
 * <ul>
 *   <li>{@code enabled}：计量总开关；</li>
 *   <li>{@code pricing.<model>.input} / {@code pricing.<model>.output}：
 *       各模型每百万 Token 单价（元），如 {@code aics.usage.pricing.deepseek-chat.input=1.0}；</li>
 *   <li>{@code default-pricing}：未配置单价模型使用的默认单价（元/百万 Token）；</li>
 *   <li>{@code executor}：异步上报线程池大小（默认 2，落库失败不阻塞主链路）。</li>
 * </ul>
 * 费用估算 = 输入 Token/1e6 × 输入单价 + 输出 Token/1e6 × 输出单价。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aics.usage")
public class ModelUsageProperties {

    /** 计量总开关（默认开启） */
    private boolean enabled = true;

    /** 各模型单价（元/百万 Token）：model -> {input, output} */
    private Map<String, ModelPrice> pricing = new HashMap<>();

    /** 默认单价（未配置的模型使用） */
    private ModelPrice defaultPricing = new ModelPrice();

    /** 异步上报线程池大小 */
    private int executorSize = 2;

    /** 单模型价格配置 */
    @Getter
    @Setter
    public static class ModelPrice {
        /** 输入单价（元/百万 Token） */
        private BigDecimal input = new BigDecimal("0");

        /** 输出单价（元/百万 Token） */
        private BigDecimal output = new BigDecimal("0");
    }
}
