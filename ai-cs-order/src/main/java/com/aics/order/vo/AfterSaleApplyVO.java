package com.aics.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后申请展示 VO
 */
@Data
@Schema(description = "售后申请展示 VO")
public class AfterSaleApplyVO {

    @Schema(description = "申请单号（AS+时间戳+序号）")
    private String applicationNo; // 申请单号，Agent 后续可凭此查询进度

    @Schema(description = "申请状态：PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED")
    private String status; // 当前申请状态

    @Schema(description = "售后动作：EXCHANGE/RETURN/REFUND")
    private String actionType;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商品名称快照")
    private String productName;

    @Schema(description = "售后数量")
    private Integer quantity;

    @Schema(description = "售后原因")
    private String reason;

    @Schema(description = "申请时间")
    private LocalDateTime createTime;
}
