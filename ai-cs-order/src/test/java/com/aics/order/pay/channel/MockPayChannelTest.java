package com.aics.order.pay.channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟支付渠道单元测试
 */
class MockPayChannelTest {

    private final MockPayChannel channel = new MockPayChannel();

    @Test
    @DisplayName("渠道标识 - 返回 MOCK")
    void getMethod_shouldReturnMock() {
        assertEquals("MOCK", channel.getMethod());
    }

    @Test
    @DisplayName("下单 - 返回收银台地址，渠道状态为待支付")
    void createPayment_shouldReturnCashierUrlAndPending() {
        PayResult result = channel.createPayment(PayContext.builder()
                .orderNo("ORD001")
                .payAmount(new BigDecimal("199.00"))
                .build());

        assertEquals("REDIRECT", result.getPayType());
        assertNotNull(result.getPayUrl());
        assertTrue(result.getPayUrl().contains("orderNo=ORD001"));
        assertEquals(MockPayChannel.STATUS_PENDING, channel.queryPayment("ORD001"));
    }

    @Test
    @DisplayName("模拟支付成功后 - 渠道状态变为 SUCCESS")
    void markPaid_shouldUpdateChannelState() {
        channel.createPayment(PayContext.builder().orderNo("ORD002").payAmount(BigDecimal.TEN).build());

        channel.markPaid("ORD002");

        assertEquals(MockPayChannel.STATUS_SUCCESS, channel.queryPayment("ORD002"));
    }

    @Test
    @DisplayName("解析通知 - 支付成功返回订单号与金额")
    void parseNotify_success_shouldReturnResult() {
        NotifyContext ctx = NotifyContext.builder()
                .params(Map.of("orderNo", "ORD001", "result", "SUCCESS", "amount", "199.00"))
                .build();

        NotifyResult result = channel.parseNotify(ctx);

        assertTrue(result.isSuccess());
        assertEquals("ORD001", result.getOrderNo());
        assertEquals(new BigDecimal("199.00"), result.getAmount());
    }

    @Test
    @DisplayName("解析通知 - 支付失败不置为成功")
    void parseNotify_fail_shouldNotSuccess() {
        NotifyContext ctx = NotifyContext.builder()
                .params(Map.of("orderNo", "ORD002", "result", "FAIL"))
                .build();

        NotifyResult result = channel.parseNotify(ctx);

        assertFalse(result.isSuccess());
        assertEquals("ORD002", result.getOrderNo());
    }

    @Test
    @DisplayName("退款 - 返回退款单号并将渠道状态置为已退款")
    void refund_shouldMarkRefunded() {
        channel.createPayment(PayContext.builder().orderNo("ORD003").payAmount(BigDecimal.TEN).build());

        RefundResult result = channel.refund("ORD003", BigDecimal.TEN);

        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getRefundNo());
        assertEquals(MockPayChannel.STATUS_REFUNDED, channel.queryPayment("ORD003"));
    }
}