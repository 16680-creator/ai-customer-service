package com.aics.order.service;

import com.aics.common.exception.BusinessException;
import com.aics.order.pay.channel.MockPayChannel;
import com.aics.order.pay.channel.PayChannelFactory;
import com.aics.order.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付服务单元测试
 * 验证：通过渠道工厂路由到对应渠道（当前为 MOCK 模拟渠道），无效支付方式被拒绝。
 */
class PaymentServiceTest {

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        // 注册模拟渠道；后续支付宝/微信/银联接入后，在此加入对应渠道实现即可
        PayChannelFactory factory = new PayChannelFactory(List.of(new MockPayChannel()));
        paymentService = new PaymentServiceImpl(factory);
    }

    @Test
    @DisplayName("创建模拟支付 - 返回模拟收银台地址")
    void createPayment_mock_shouldReturnCashierUrl() {
        String payUrl = paymentService.createPayment("ORD001", new BigDecimal("220.00"), "MOCK");

        assertNotNull(payUrl);
        assertTrue(payUrl.contains("mock-pay"));
        assertTrue(payUrl.contains("ORD001"));
        assertTrue(payUrl.contains("220.00"));
    }

    @Test
    @DisplayName("创建支付 - 未配置/未接入的支付方式应抛出 BusinessException")
    void createPayment_notImplementedMethod_shouldThrowException() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> paymentService.createPayment("ORD004", BigDecimal.TEN, "UNIONPAY"));
        assertTrue(exception.getMessage().contains("不支持的支付方式"));
    }

    @Test
    @DisplayName("创建支付 - null 支付方式应抛出异常")
    void createPayment_nullMethod_shouldThrowException() {
        assertThrows(Exception.class,
                () -> paymentService.createPayment("ORD005", BigDecimal.TEN, null));
    }
}