package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型用量配额请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载设置/更新模型用量配额（model_usage_quota 表）的入参，按 (userId, scenario)
 * 幂等 upsert：已存在则更新，不存在则插入。
 * windowType 未传时默认 DAILY（实体初始值保证）；quotaTokens/quotaCost 为 NULL 表示不限。
 *
 * <h3>【设计原理】为什么 userId 用 @NotNull、scenario 用 @NotBlank 强校验</h3>
 * <p>(userId, scenario) 是 upsert 的"定位键"：缺任何一个都无法定位/创建配额记录，
 * 属于"缺了就没法干"的字段，必须强校验；而 windowType/quotaTokens 等是"可调参数"，
 * 缺省有默认语义（DAILY / NULL=不限），保持可选。</p>
 *
 * <h3>【设计原理】为什么 DTO 里新增了 periodStart（可选）</h3>
 * <p>upsert 契约要求"已存在则更新 windowType/quotaTokens/quotaCost/periodStart"，
 * periodStart 用于滚动窗口对齐，因此作为可选字段进入 DTO；不传时更新路径保留原值
 * （见 ModelUsageQuotaServiceImpl 的可空字段不覆盖策略）。</p>
 * </p>
 */
@Data
@Schema(description = "模型用量配额请求")
public class ModelUsageQuotaDTO {

    @Schema(description = "用户ID", example = "10001")
    // upsert 定位键之一：与 scenario 组合唯一（表级 uk_user_scenario），缺失则无法定位记录
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "场景", example = "chat")
    @NotBlank(message = "场景不能为空")
    private String scenario;

    @Schema(description = "窗口：DAILY/WEEKLY/MONTHLY（默认 DAILY）", example = "DAILY")
    private String windowType; // 可选：不传时由实体初始值保证为 DAILY

    @Schema(description = "Token配额（NULL=不限）", example = "100000")
    private Long quotaTokens;

    @Schema(description = "费用配额（元，NULL=不限）", example = "50.000000")
    private BigDecimal quotaCost;

    @Schema(description = "窗口起始时间（可空）", example = "2026-08-14T00:00:00")
    private LocalDateTime periodStart;
}
