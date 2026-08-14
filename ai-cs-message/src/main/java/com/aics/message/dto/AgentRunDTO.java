package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent 执行记录请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载创建 Agent 执行记录（agent_run 表）的入参，字段与实体对齐。
 * runId 由调用方（chat 模块 Agent 编排链路）生成，作为幂等键：重复提交返回首次结果。
 * </p>
 */
@Data
@Schema(description = "Agent 执行记录请求")
public class AgentRunDTO {

    @Schema(description = "执行ID（UUID，幂等键）", example = "uuid-run-001")
    @NotBlank(message = "执行ID不能为空") // 幂等键：runId 已存在时服务端直接返回首次创建的 runId
    private String runId;

    @Schema(description = "会话ID", example = "1001")
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @Schema(description = "用户ID", example = "10001")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "识别意图（多意图逗号分隔）", example = "AFTER_SALE,PRODUCT_RECOMMEND")
    private String intent;

    @Schema(description = "情绪：POSITIVE/NEUTRAL/NEGATIVE/ANGRY", example = "NEGATIVE")
    private String sentiment;

    @Schema(description = "状态：RUNNING/WAITING_CONFIRM/COMPLETED/CANCELLED/HANDOFF/FAILED（默认 RUNNING）", example = "RUNNING")
    private String status; // 可选：不传时由实体初始值保证为 RUNNING

    @Schema(description = "当前步骤号（默认 0）", example = "0")
    private Integer currentStep; // 可选：不传时由实体初始值保证为 0

    @Schema(description = "Prompt/规则版本", example = "v1.2")
    private String promptVersion;

    @Schema(description = "失败摘要")
    private String errorSummary;
}
