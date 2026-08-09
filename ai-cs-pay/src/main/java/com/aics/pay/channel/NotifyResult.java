package com.aics.pay.channel;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 标准化支付通知结果（各渠道验签/解密后统一转换为该结构）
 */
@Data
@Builder
public class NotifyResult {

    /** 商户订单号 */
    private String orderNo;

    /** 是否支付成功（渠道侧确认） */
    private boolean success;

    /** 支付金额（统一"元"） */
    private BigDecimal amount;

    /** 渠道流水号（微信 transaction_id / 支付宝 trade_no / 银联 queryId） */
    private String transactionId;
}