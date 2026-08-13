package com.aics.chat.dto;

import lombok.Data;

/**
 * 售后申请请求（chat 侧 DTO，与 ai-cs-order 的 AfterSaleApplyDTO 一致，用于 Feign 反序列化）
 */
@Data
public class AfterSaleApplyDTO {

    /** 订单号 */
    private String orderNo;

    /** 商品 ID（整单售后可为空） */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 售后数量 */
    private Integer quantity;

    /** 售后动作：EXCHANGE/RETURN/REFUND */
    private String actionType;

    /** 售后原因 */
    private String reason;

    /** Agent 执行 ID */
    private String runId;

    /** 幂等键 */
    private String idempotencyKey;

    /** 证据/规则引用摘要 */
    private String evidenceSummary;
}
