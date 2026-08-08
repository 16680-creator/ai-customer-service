package com.aics.order.pay.channel;

import lombok.Builder;
import lombok.Data;

/**
 * 退款结果
 */
@Data
@Builder
public class RefundResult {

    /** 商户退款单号 */
    private String refundNo;

    /** 退款状态：SUCCESS / PROCESSING / FAILED */
    private String status;
}