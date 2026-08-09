package com.aics.pay.channel;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付上下文：渠道下单所需参数（与具体渠道无关）
 */
@Data
@Builder
public class PayContext {

    /** 商户订单号（渠道侧幂等键） */
    private String orderNo;

    /** 支付金额（统一使用"元"） */
    private BigDecimal payAmount;

    /** 商品描述 */
    private String subject;

    /** 异步通知地址 */
    private String notifyUrl;
}