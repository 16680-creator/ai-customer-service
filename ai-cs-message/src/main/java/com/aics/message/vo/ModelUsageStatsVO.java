package com.aics.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型用量统计响应 VO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载按条件（userId/scenario/model/时间范围）聚合后的模型用量统计结果：
 * 调用次数、各 Token 维度求和与估算费用求和，供成本治理看板与配额预检查使用。
 *
 * <h3>【设计原理】为什么统计字段用 Long（而非 int）</h3>
 * <p>Token 累加是"求和"，int 上限约 21 亿，多用户/多场景聚合很容易溢出；
 * Long 与 MySQL BIGINT 对齐，配合 Service 层 {@code mapToLong(...).sum()} 天然安全。</p>
 *
 * <h3>【设计原理】为什么费用合计用 BigDecimal</h3>
 * <p>费用合计最终用于配额预检查（quota_cost 比较），double 的累加误差可能导致
 * "明明没超配额却被误判超支"；BigDecimal 保证与 DECIMAL(12,6) 精确一致。</p>
 * </p>
 */
@Data
@Schema(description = "模型用量统计响应")
public class ModelUsageStatsVO {

    @Schema(description = "调用次数")
    private Long callCount; // 由满足条件的记录条数直接得出（list.size()）

    @Schema(description = "输入Token总数")
    private Long inputTokens;

    @Schema(description = "输出Token总数")
    private Long outputTokens;

    @Schema(description = "总Token数")
    private Long totalTokens;

    @Schema(description = "估算费用合计（元）")
    private BigDecimal estimatedCost;
}
