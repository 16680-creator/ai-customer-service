package com.aics.order.service.impl;

import com.aics.order.pay.channel.PayChannel;
import com.aics.order.pay.channel.PayChannelFactory;
import com.aics.order.pay.channel.PayContext;
import com.aics.order.pay.channel.PayResult;
import com.aics.order.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 支付服务实现：委托给支付渠道工厂（解耦）
 *
 * <p>下单不再直接写死渠道逻辑，而是通过 {@link PayChannelFactory}
 * 按支付方式路由到对应的 {@link PayChannel} 实现。
 * 新增支付方式（支付宝/微信/银联/聚合）只需新增渠道实现类。
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PayChannelFactory payChannelFactory;

    public PaymentServiceImpl(PayChannelFactory payChannelFactory) {
        this.payChannelFactory = payChannelFactory;
    }

    @Override
    public String createPayment(String orderNo, BigDecimal payAmount, String paymentMethod) {
        PayChannel channel = payChannelFactory.getChannel(paymentMethod);
        PayResult result = channel.createPayment(PayContext.builder()
                .orderNo(orderNo)
                .payAmount(payAmount)
                .subject("订单 " + orderNo)
                .notifyUrl("/api/pay/callback/" + paymentMethod)
                .build());
        // REDIRECT 渠道返回跳转地址；QRCODE 渠道返回二维码内容（前端渲染二维码）
        String payUrl = StringUtils.hasText(result.getPayUrl()) ? result.getPayUrl() : result.getCodeUrl();
        log.info("创建支付成功: orderNo={}, method={}, payType={}, payUrl={}", orderNo, paymentMethod, result.getPayType(), payUrl);
        return payUrl;
    }
}