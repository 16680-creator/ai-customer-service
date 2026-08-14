package com.aics.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型用量配额响应 VO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载按 (userId, scenario) 查询模型用量配额的响应，字段与实体对齐，
 * 供成本治理预检查（配额是否触顶）使用。
 *
 * <h3>【设计原理】为什么响应要带 createTime/updateTime</h3>
 * <p>配额是治理规则数据，运营需要知道"什么时候配的、最近一次改了什么"（审计与排障），
 * 因此 VO 透出两个时间字段；createTime 由插入时自动填充、updateTime 由
 * MetaObjectHandler 在每次 updateById 时刷新。</p>
 * </p>
 */
@Data
@Schema(description = "模型用量配额响应")
public class ModelUsageQuotaVO {

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "场景", example = "chat")
    private String scenario;

    @Schema(description = "窗口：DAILY/WEEKLY/MONTHLY", example = "DAILY")
    private String windowType;

    @Schema(description = "Token配额（NULL=不限）", example = "100000")
    private Long quotaTokens;

    @Schema(description = "费用配额（元，NULL=不限）", example = "50.000000")
    private BigDecimal quotaCost;

    @Schema(description = "窗口起始时间")
    private LocalDateTime periodStart;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
