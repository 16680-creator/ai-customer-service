package com.aics.pay.channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockPayChannelTest {

    private final MockPayChannel channel = new MockPayChannel();

    @Test
    void getMethod_shouldReturnMock() {
        assertEquals("MOCK", channel.getMethod());
    }

    @Test
    void createPayment_shouldReturnCashierUrlAndPending() {
        PayResult result = channel.createPayment(PayContext.builder()
                .orderNo("ORD001").payAmount(new BigDecimal("199.00")).build());
        assertEquals("REDIRECT", result.getPayType());
        assertTrue(result.getPayUrl().contains("orderNo=ORD001"));
        assertEquals(MockPayChannel.STATUS_PENDING, channel.queryPayment("ORD001"));
    }

    @Test
    void markPaid_shouldUpdateChannelState() {
        channel.createPayment(PayContext.builder().orderNo("ORD002").payAmount(BigDecimal.TEN).build());
        channel.markPaid("ORD002");
        assertEquals(MockPayChannel.STATUS_SUCCESS, channel.queryPayment("ORD002"));
    }

    @Test
    void parseNotify_success_shouldReturnResult() {
        NotifyContext ctx = NotifyContext.builder()
                .params(Map.of("orderNo", "ORD001", "result", "SUCCESS", "amount", "199.00")).build();
        NotifyResult result = channel.parseNotify(ctx);
        assertTrue(result.isSuccess());
        assertEquals("ORD001", result.getOrderNo());
    }

    @Test
    void parseNotify_fail_shouldNotSuccess() {
        NotifyContext ctx = NotifyContext.builder()
                .params(Map.of("orderNo", "ORD002", "result", "FAIL")).build();
        assertFalse(channel.parseNotify(ctx).isSuccess());
    }

    @Test
    void closeOrder_shouldMarkClosed() {
        channel.createPayment(PayContext.builder().orderNo("ORD004").payAmount(BigDecimal.TEN).build());
        channel.closeOrder("ORD004");
        assertEquals(MockPayChannel.STATUS_CLOSED, channel.queryPayment("ORD004"));
    }

    @Test
    void refund_shouldMarkRefunded() {
        channel.createPayment(PayContext.builder().orderNo("ORD003").payAmount(BigDecimal.TEN).build());
        RefundResult result = channel.refund("ORD003", BigDecimal.TEN);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(MockPayChannel.STATUS_REFUNDED, channel.queryPayment("ORD003"));
    }
}