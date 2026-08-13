package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 写操作确认记录请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载记录 Agent 写操作确认（agent_confirmation 表）的入参，字段与实体对齐。
 * 同 (runId, action) 重复上报时幂等覆盖（如从 PENDING 更新为 CONFIRMED/REJECTED）。
 * </p>
 */
@Data
@Schema(description = "Agent 写操作确认请求")
public class AgentConfirmationDTO {

    @Schema(description = "所属执行ID", example = "uuid-run-001")
    @NotBlank(message = "执行ID不能为空")
    private String runId;

    @Schema(description = "待确认动作：CREATE_EXCHANGE/CREATE_RETURN/CREATE_REFUND", example = "CREATE_EXCHANGE")
    @NotBlank(message = "待确认动作不能为空")
    private String action;

    @Schema(description = "操作摘要的SHA-256", example = "a1b2c3d4e5f6...")
    @NotBlank(message = "操作摘要不能为空")
    private String payloadDigest;

    @Schema(description = "状态：PENDING/CONFIRMED/REJECTED/EXPIRED（默认 PENDING）", example = "PENDING")
    private String status;

    @Schema(description = "确认人（用户ID）", example = "10001")
    private Long confirmedBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmedAt;

    @Schema(description = "确认超时时间", example = "2026-01-01T10:30:00")
    @NotNull(message = "确认超时时间不能为空")
    private LocalDateTime timeoutAt;
}
