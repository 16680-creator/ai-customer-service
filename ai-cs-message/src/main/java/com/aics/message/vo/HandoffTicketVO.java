package com.aics.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转人工工单响应 VO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载创建转人工工单后的精简结果（工单号 + 状态），供调用方（chat 模块）展示与后续通知。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "转人工工单响应")
public class HandoffTicketVO {

    @Schema(description = "工单号", example = "HF202601011200001234")
    private String ticketNo; // 服务端生成的唯一工单号：HF + 时间戳 + 4 位随机数字

    @Schema(description = "工单状态：OPEN/ASSIGNED/CLOSED", example = "OPEN")
    private String status;
}
