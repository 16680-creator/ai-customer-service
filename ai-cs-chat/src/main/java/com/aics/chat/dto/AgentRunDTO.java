package com.aics.chat.dto;

import lombok.Data;

/**
 * Agent 执行记录（chat 侧 DTO，与 ai-cs-message 的 AgentRunDTO 一致，用于 Feign 持久化）
 */
@Data
public class AgentRunDTO {

    /** 执行 ID */
    private String runId;

    /** 会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 识别意图（多意图逗号分隔） */
    private String intent;

    /** 情绪 */
    private String sentiment;

    /** 状态 */
    private String status;

    /** 当前步骤号 */
    private Integer currentStep;

    /** Prompt/规则版本 */
    private String promptVersion;

    /** 失败摘要 */
    private String errorSummary;
}
