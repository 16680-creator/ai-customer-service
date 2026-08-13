package com.aics.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 写操作确认记录（chat 侧 DTO，与 ai-cs-message 的 AgentConfirmationDTO 一致）
 */
@Data
public class AgentConfirmationDTO {

    /** 所属执行 ID */
    private String runId;

    /** 待确认动作 */
    private String action;

    /** 操作摘要 SHA-256 */
    private String payloadDigest;

    /** 状态：PENDING/CONFIRMED/REJECTED/EXPIRED */
    private String status;

    /** 确认人 */
    private Long confirmedBy;

    /** 确认超时时间 */
    private LocalDateTime timeoutAt;
}
