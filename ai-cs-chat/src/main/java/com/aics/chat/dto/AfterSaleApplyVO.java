package com.aics.chat.dto;

import lombok.Data;

/**
 * 售后申请结果（chat 侧 DTO，与 ai-cs-order 的 AfterSaleApplyVO 一致）
 */
@Data
public class AfterSaleApplyVO {

    /** 申请单号 */
    private String applicationNo;

    /** 状态 */
    private String status;

    /** 售后动作 */
    private String actionType;

    /** 订单号 */
    private String orderNo;

    /** 商品名称 */
    private String productName;

    /** 数量 */
    private Integer quantity;

    /** 原因 */
    private String reason;
}
