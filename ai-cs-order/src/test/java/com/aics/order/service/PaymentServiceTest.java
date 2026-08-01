package com.aics.order.service;

import com.aics.common.exception.BusinessException;
import com.aics.order.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付服务单元测试
 * TDD: 验证策略模式支付创建、无效支付方式拒绝、回调验签
 */
class PaymentServiceTest {

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl();
    }

    // ==================== createPayment ====================

    @Test
    @DisplayName("创建微信支付 - 返回微信支付URL")
    void createPayment_wechat_shouldReturnWechatUrl() {
        String payUrl = paymentService.createPayment("ORD001", new BigDecimal("220.00"), "WECHAT");

        assertNotNull(payUrl);
        assertTrue(payUrl.contains("pay.weixin.qq.com"));
        assertTrue(payUrl.contains("ORD001"));
        assertTrue(payUrl.contains("220.00"));
    }

    @Test
    @DisplayName("创建支付宝支付 - 返回支付宝URL")
    void createPayment_alipay_shouldReturnAlipayUrl() {
        String payUrl = paymentService.createPayment("ORD002", new BigDecimal("150.50"), "ALIPAY");

        assertNotNull(payUrl);
        assertTrue(payUrl.contains("openapi.alipay.com"));
        assertTrue(payUrl.contains("ORD002"));
    }

    @Test
    @DisplayName("创建银行卡支付 - 返回银行卡URL")
    void createPayment_bankCard_shouldReturnBankUrl() {
        String payUrl = paymentService.createPayment("ORD003", new BigDecimal("99.99"), "BANK_CARD");

        assertNotNull(payUrl);
        assertTrue(payUrl.contains("bank.pay.com"));
        assertTrue(payUrl.contains("ORD003"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "paypal", "", "CREDIT_CARD"})
    @DisplayName("创建支付 - 无效支付方式应抛出 BusinessException")
    void createPayment_invalidMethod_shouldThrowException(String invalidMethod) {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> paymentService.createPayment("ORD004", BigDecimal.TEN, invalidMethod));

        assertTrue(exception.getMessage().contains("不支持的支付方式"));
    }

    @Test
    @DisplayName("创建支付 - null支付方式应抛出异常")
    void createPayment_nullMethod_shouldThrowException() {
        assertThrows(Exception.class,
                () -> paymentService.createPayment("ORD005", BigDecimal.TEN, null));
    }

    // ==================== verifyCallback ====================

    @Test
    @DisplayName("验证回调签名 - 当前模拟实现始终返回true")
    void verifyCallback_shouldReturnTrue() {
        assertTrue(paymentService.verifyCallback("WECHAT", "raw_data_string"));
        assertTrue(paymentService.verifyCallback("ALIPAY", "another_raw_data"));
    }

    @Test
    @DisplayName("验证回调签名 - 空数据不抛异常")
    void verifyCallback_emptyData_shouldNotThrow() {
        assertDoesNotThrow(() -> paymentService.verifyCallback("WECHAT", ""));
        assertDoesNotThrow(() -> paymentService.verifyCallback("WECHAT", null));
    }
}
