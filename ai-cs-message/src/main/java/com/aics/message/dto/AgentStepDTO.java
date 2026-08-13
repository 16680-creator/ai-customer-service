package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent 步骤轨迹请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载追加 Agent 步骤轨迹（agent_step 表）的入参，字段与实体对齐。
 * 同 (runId, stepNo) 重复上报时按步骤号幂等覆盖；输入输出为脱敏摘要。
 * </p>
 */
@Data
@Schema(description = "Agent 步骤轨迹请求")
public class AgentStepDTO {

    @Schema(description = "所属执行ID", example = "uuid-run-001")
    @NotBlank(message = "执行ID不能为空")
    private String runId;

    @Schema(description = "步骤序号", example = "1")
    @NotNull(message = "步骤序号不能为空")
    private Integer stepNo;

    @Schema(description = "步骤类型：SAFETY/INTENT/LOCATE_ORDER/CHECK_POLICY/RECOMMEND/CONFIRM/EXECUTE/HANDOFF", example = "LOCATE_ORDER")
    @NotBlank(message = "步骤类型不能为空")
    private String stepType;

    @Schema(description = "工具名（无工具为空）", example = "locateOrderTool")
    private String toolName;

    @Schema(description = "输入摘要（敏感字段脱敏）", example = "userId=10001, orderNo=ORD001")
    private String inputDigest;

    @Schema(description = "输出摘要", example = "定位到订单 ORD001（¥199 无线蓝牙耳机）")
    private String outputDigest;

    @Schema(description = "耗时（毫秒，默认 0）", example = "320")
    private Long durationMs;

    @Schema(description = "状态：SUCCESS/FAILED/SKIPPED（默认 SUCCESS）", example = "SUCCESS")
    private String status;

    @Schema(description = "错误摘要")
    private String errorSummary;
}
