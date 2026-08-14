package com.aics.chat.dto;

import lombok.Data;

/**
 * 转人工工单结果（chat 侧 DTO，与 ai-cs-message 的 HandoffTicketVO 一致）
 */
@Data
public class HandoffTicketVO {

    /** 工单号 */
    private String ticketNo;

    /** 状态 */
    private String status;
}
