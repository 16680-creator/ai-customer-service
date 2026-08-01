package com.aics.order.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.enums.PaymentMethod;
import com.aics.order.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 支付服务实现（策略模式）
 * 当前为模拟实现，生产环境替换为真实支付SDK调用
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Override
    public String createPayment(String orderNo, BigDecimal payAmount, String paymentMethod) {
        PaymentMethod method = parseMethod(paymentMethod);

        // 模拟生成支付跳转URL
        String payUrl = switch (method) {
            case WECHAT -> "https://pay.weixin.qq.com/pay?order=" + orderNo + "&amount=" + payAmount;
            case ALIPAY -> "https://openapi.alipay.com/pay?order=" + orderNo + "&amount=" + payAmount;
            case BANK_CARD -> "https://bank.pay.com/pay?order=" + orderNo + "&amount=" + payAmount;
        };

        log.info("创建支付: orderNo={}, method={}, amount={}, payUrl={}", orderNo, method, payAmount, payUrl);
        return payUrl;
    }

    @Override
    public boolean verifyCallback(String paymentMethod, String rawData) {
        // 模拟签名验证，生产环境需实现真实验签逻辑
        log.info("验证支付回调签名: method={}", paymentMethod);
        return true;
    }

    private PaymentMethod parseMethod(String paymentMethod) {
        try {
            return PaymentMethod.valueOf(paymentMethod);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "不支持的支付方式: " + paymentMethod);
        }
    }
}
