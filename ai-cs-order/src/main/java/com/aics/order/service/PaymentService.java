package com.aics.order.service;

import java.math.BigDecimal;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付（委托给支付渠道工厂，返回支付地址/二维码内容）
     */
    String createPayment(String orderNo, BigDecimal payAmount, String paymentMethod);
}