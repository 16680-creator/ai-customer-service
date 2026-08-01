package com.aics.order.service;

import java.math.BigDecimal;

/**
 * 支付服务接口（策略模式）
 */
public interface PaymentService {

    /**
     * 创建支付（返回支付跳转URL）
     */
    String createPayment(String orderNo, BigDecimal payAmount, String paymentMethod);

    /**
     * 验证支付回调签名
     */
    boolean verifyCallback(String paymentMethod, String rawData);
}
