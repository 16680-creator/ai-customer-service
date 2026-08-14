package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 线上采样评估记录请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载上报线上采样评估（online_eval_record 表）的入参，字段与实体对齐。
 * judgeStatus 必填（SUCCESS/FAILED/SKIPPED）；llmScore 为 1-5，
 * 评分失败（FAILED/SKIPPED）时为空。
 *
 * <h3>【设计原理】为什么 judgeStatus 必填而 llmScore 可选</h3>
 * <p>judgeStatus 决定这条评估记录"是否可信"（SUCCESS 才算分、FAILED 要告警、
 * SKIPPED 属策略跳过），缺失时统计口径无法确定，必须强校验；
 * llmScore 只有在 SUCCESS 时才存在，天然可选，且不设 @Min/@Max 硬校验——
 * 评分数值由 LLM-as-Judge 侧保证，这里保持宽松，避免评分模型升级产生误杀。</p>
 * </p>
 */
@Data
@Schema(description = "线上采样评估记录请求")
public class OnlineEvalRecordDTO {

    @Schema(description = "请求ID（关联 llm_trace）", example = "trace-uuid-001")
    private String requestId;

    @Schema(description = "会话ID", example = "1001")
    private Long sessionId;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "问题摘要（截断）", example = "用户咨询退货政策")
    private String questionDigest;

    @Schema(description = "回答摘要（截断）", example = "7 天无理由退货，需商品完好")
    private String answerDigest;

    @Schema(description = "LLM-as-Judge 评分（1-5）", example = "4")
    private Integer llmScore;

    @Schema(description = "评分状态：SUCCESS/FAILED/SKIPPED", example = "SUCCESS")
    @NotBlank(message = "评分状态不能为空")
    private String judgeStatus;

    @Schema(description = "评分失败摘要")
    private String errorSummary;
}
