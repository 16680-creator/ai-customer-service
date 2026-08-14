package com.aics.chat.dto;

import lombok.Data;

/**
 * 转人工工单（chat 侧 DTO，与 ai-cs-message 的 HandoffTicketDTO 一致）
 */
@Data
public class HandoffTicketDTO {

    /** 所属执行 ID */
    private String runId;

    /** 会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 触发原因：POLICY_NOT_MET/NEGATIVE_SENTIMENT/EXECUTION_FAILED/USER_REQUEST */
    private String reason;

    /** 优先级：HIGH/NORMAL */
    private String priority;

    /** 关联订单号 */
    private String orderNo;

    /** 情绪 */
    private String sentiment;

    /** 问题摘要 */
    private String problemSummary;

    /** 已执行步骤清单（JSON 数组字符串） */
    private String executedSteps;
}
