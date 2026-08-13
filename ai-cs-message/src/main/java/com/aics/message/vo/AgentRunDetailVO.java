package com.aics.message.vo;

import com.aics.message.dto.AgentStepDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 执行详情响应 VO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载按 runId 查询 Agent 执行轨迹（审计回放）的响应：
 * run 的元数据 + 该执行下全部步骤（按 stepNo 升序）。
 * </p>
 */
@Data
@Schema(description = "Agent 执行详情响应")
public class AgentRunDetailVO {

    @Schema(description = "执行ID", example = "uuid-run-001")
    private String runId;

    @Schema(description = "会话ID", example = "1001")
    private Long sessionId;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "识别意图（多意图逗号分隔）", example = "AFTER_SALE,PRODUCT_RECOMMEND")
    private String intent;

    @Schema(description = "情绪：POSITIVE/NEUTRAL/NEGATIVE/ANGRY", example = "NEGATIVE")
    private String sentiment;

    @Schema(description = "状态：RUNNING/WAITING_CONFIRM/COMPLETED/CANCELLED/HANDOFF/FAILED", example = "RUNNING")
    private String status;

    @Schema(description = "当前步骤号", example = "2")
    private Integer currentStep;

    @Schema(description = "Prompt/规则版本", example = "v1.2")
    private String promptVersion;

    @Schema(description = "失败摘要")
    private String errorSummary;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "步骤轨迹（按步骤序号升序）")
    private List<AgentStepDTO> steps;
}
