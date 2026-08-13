package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转人工工单请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载创建转人工工单（handoff_ticket 表）的入参，字段与实体对齐。
 * 工单号 ticketNo 由服务端生成（HF + 时间戳 + 4位随机数），不参与入参。
 * </p>
 */
@Data
@Schema(description = "转人工工单请求")
public class HandoffTicketDTO {

    @Schema(description = "所属执行ID", example = "uuid-run-001")
    private String runId; // 可空：工单号 ticketNo 由服务端生成不参与入参，本字段仅用于关联执行记录

    @Schema(description = "会话ID", example = "1001")
    private Long sessionId;

    @Schema(description = "用户ID", example = "10001")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "触发原因：POLICY_NOT_MET/NEGATIVE_SENTIMENT/EXECUTION_FAILED/USER_REQUEST", example = "NEGATIVE_SENTIMENT")
    @NotBlank(message = "触发原因不能为空") // 必填：转人工触发原因，坐席据此判断工单跟进方向
    private String reason;

    @Schema(description = "优先级：HIGH/NORMAL（默认 NORMAL）", example = "NORMAL")
    private String priority;

    @Schema(description = "关联订单号", example = "ORD001")
    private String orderNo;

    @Schema(description = "情绪", example = "ANGRY")
    private String sentiment;

    @Schema(description = "问题摘要", example = "换货资格不满足，已转人工")
    private String problemSummary;

    @Schema(description = "已执行步骤清单（JSON数组字符串）", example = "[{\"stepNo\":1,\"stepType\":\"INTENT\"}]")
    private String executedSteps;
}
