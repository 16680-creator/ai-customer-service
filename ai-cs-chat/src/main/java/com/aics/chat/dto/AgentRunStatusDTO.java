package com.aics.chat.dto;

import lombok.Data;

/**
 * Agent run 状态更新（chat 侧 DTO，与 ai-cs-message 的 RunStatusUpdateDTO 一致）
 */
@Data
public class AgentRunStatusDTO {

    /** 状态：RUNNING/WAITING_CONFIRM/COMPLETED/CANCELLED/HANDOFF/FAILED */
    private String status;

    /** 当前步骤号 */
    private Integer currentStep;

    /** 失败摘要 */
    private String errorSummary;
}
