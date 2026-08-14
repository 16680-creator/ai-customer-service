package com.aics.chat.dto;

import lombok.Data;

/**
 * 转人工通知（chat 侧 DTO，与 ai-cs-notify 的 HandoffNoticeDTO 一致）
 */
@Data
public class HandoffNoticeDTO {

    /** 工单号 */
    private String ticketNo;

    /** 用户 ID */
    private Long userId;

    /** 优先级 */
    private String priority;

    /** 订单号 */
    private String orderNo;

    /** 移交摘要 */
    private String summary;
}
