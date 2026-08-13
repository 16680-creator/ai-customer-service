package com.aics.chat.agent.model;

/**
 * 转人工信息
 *
 * @param ticketNo 工单号
 * @param reason   触发原因（POLICY_NOT_MET/NEGATIVE_SENTIMENT/EXECUTION_FAILED/USER_REQUEST）
 * @param priority 优先级（HIGH/NORMAL）
 * @param summary  移交摘要
 */
public record HandoffInfo(String ticketNo, String reason, String priority, String summary) {
}
