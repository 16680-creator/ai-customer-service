package com.aics.chat.dto;

import lombok.Data;

/**
 * Agent 步骤轨迹（chat 侧 DTO，与 ai-cs-message 的 AgentStepDTO 一致）
 */
@Data
public class AgentStepDTO {

    /** 所属执行 ID */
    private String runId;

    /** 步骤序号 */
    private Integer stepNo;

    /** 步骤类型 */
    private String stepType;

    /** 工具名 */
    private String toolName;

    /** 输入摘要 */
    private String inputDigest;

    /** 输出摘要 */
    private String outputDigest;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 状态：SUCCESS/FAILED/SKIPPED */
    private String status;

    /** 错误摘要 */
    private String errorSummary;
}
