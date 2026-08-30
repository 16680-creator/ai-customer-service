package com.aics.pay.controller;

import com.aics.common.exception.BusinessException;
import com.aics.pay.channel.NotifyContext;
import com.aics.pay.channel.NotifyResult;
import com.aics.pay.channel.PayChannel;
import com.aics.pay.channel.PayChannelFactory;
import com.aics.pay.channel.PayResult;
import com.aics.pay.channel.PayContext;
import com.aics.pay.client.OrderPayClient;
import com.aics.pay.service.PayCompensationService;
import com.aics.pay.service.PayNotifyService;
import com.aics.pay.service.PayTransactionService;
import com.aics.pay.dto.OrderPayDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付服务 Controller 层单元测试
 * 覆盖：发起支付、查单兜底、关单、补偿、模拟支付/退款、渠道回调
 */
@ExtendWith(MockitoExtension.class)
class PayControllersTest {

    @Mock
    private PayChannelFactory payChannelFactory;
    @Mock
    private PayTransactionService payTransactionService;
    @Mock
    private OrderPayClient orderPayClient;
    @Mock
    private PayNotifyService payNotifyService;
    @Mock
    private PayCompensationService payCompensationService;
    @Mock
    private com.aics.pay.channel.MockPayChannel mockPayChannel;
    @Mock
    private PayChannel payChannel;

    @InjectMocks
    private PayController payController;
    @InjectMocks
    private MockPayController mockPayController;
    @InjectMocks
    private PayCallbackController payCallbackController;

    private OrderPayDetailVO pendingOrder(Long userId) {
        OrderPayDetailVO vo = new OrderPayDetailVO();
        vo.setOrderNo("ORD001");
        vo.setUserId(userId);
        vo.setStatus("PENDING_PAY");
        vo.setPayAmount(new BigDecimal("99.90"));
        vo.setPaymentMethod("MOCK");
        return vo;
    }

    // ==================== PayController ====================

    @Test
    @DisplayName("创建支付 - 正常返回渠道支付参数")
    void createPayment_ok() {
        OrderPayDetailVO order = pendingOrder(100L);
        when(orderPayClient.getOrderDetail("ORD001")).thenReturn(order);
        when(payChannelFactory.getChannel("MOCK")).thenReturn(payChannel);
        when(payChannel.createPayment(any(PayContext.class)))
                .thenReturn(PayResult.builder().payType("mock").payUrl("http://pay").build());

        var result = payController.createPayment(100L, Map.of("orderNo", "ORD001"));

        assertEquals(200, result.getCode());
        assertEquals("ORD001", result.getData().get("orderNo"));
        verify(payTransactionService).createOrUpdatePending(eq("ORD001"), eq(100L), eq("MOCK"), any());
    }

    @Test
    @DisplayName("创建支付 - 订单不可支付抛异常")
    void createPayment_notPayable() {
        OrderPayDetailVO order = pendingOrder(100L);
        order.setStatus("PAID");
        when(orderPayClient.getOrderDetail("ORD001")).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> payController.createPayment(100L, Map.of("orderNo", "ORD001")));
    }

    @Test
    @DisplayName("查单兜底 - 待支付订单向渠道查询后返回最新状态")
    void queryPayStatus_withFallback() {
        OrderPayDetailVO order = pendingOrder(100L);
        when(orderPayClient.getOrderDetail("ORD001")).thenReturn(order);
        when(payNotifyService.syncByQuery("ORD001", "MOCK")).thenReturn(true);

        var result = payController.queryPayStatus(100L, "ORD001");

        assertEquals(200, result.getCode());
        verify(payNotifyService).syncByQuery("ORD001", "MOCK");
    }

    @Test
    @DisplayName("关单 - body 缺失时使用默认支付方式")
    void closeOrder_nullBody() {
        var result = payController.closeOrder(null);
        assertEquals(200, result.getCode());
        verify(payNotifyService).closeOrder(null, "MOCK");
    }

    @Test
    @DisplayName("补偿对账 - 返回对账结果")
    void compensate_ok() {
        when(payCompensationService.compensate()).thenReturn(Map.of("fixed", 1));
        var result = payController.compensate();
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().get("fixed"));
    }

    // ==================== MockPayController ====================

    @Test
    @DisplayName("模拟支付成功 - 触发与真实渠道一致的通知处理")
    void mockPay_success() {
        OrderPayDetailVO order = pendingOrder(100L);
        OrderPayDetailVO paid = pendingOrder(100L);
        paid.setStatus("PAID");
        when(orderPayClient.getOrderDetail("ORD001")).thenReturn(order, paid);

        var result = mockPayController.mockPay(100L, Map.of("orderNo", "ORD001", "result", "SUCCESS"));

        assertEquals(200, result.getCode());
        verify(mockPayChannel).markPaid("ORD001");
        verify(payNotifyService).processNotify(eq("MOCK"), any(NotifyContext.class));
    }

    @Test
    @DisplayName("模拟支付取消 - 不触发通知处理")
    void mockPay_cancel() {
        OrderPayDetailVO order = pendingOrder(100L);
        when(orderPayClient.getOrderDetail("ORD001")).thenReturn(order);

        var result = mockPayController.mockPay(100L, Map.of("orderNo", "ORD001", "result", "FAIL"));

        assertEquals(200, result.getCode());
        verify(payNotifyService, never()).processNotify(anyString(), any(NotifyContext.class));
    }

    @Test
    @DisplayName("模拟支付 - 订单不可支付抛异常")
    void mockPay_notPayable() {
        when(orderPayClient.getOrderDetail("ORD001")).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> mockPayController.mockPay(100L, Map.of("orderNo", "ORD001")));
    }

    @Test
    @DisplayName("模拟退款 - 已支付订单退款成功")
    void mockRefund_ok() {
        OrderPayDetailVO paid = pendingOrder(100L);
        paid.setStatus("PAID");
        OrderPayDetailVO refunded = pendingOrder(100L);
        refunded.setStatus("REFUNDED");
        when(orderPayClient.getOrderDetail("ORD001")).thenReturn(paid, refunded);

        var result = mockPayController.mockRefund(100L, Map.of("orderNo", "ORD001"));

        assertEquals(200, result.getCode());
        verify(mockPayChannel).refund(eq("ORD001"), any());
        verify(orderPayClient).refundConfirm("ORD001");
    }

    // ==================== PayCallbackController ====================

    @Test
    @DisplayName("渠道回调 - 处理成功返回 SUCCESS")
    void payCallback_ok() {
        HttpServletRequest request = new MockHttpServletRequest();
        var result = payCallbackController.payCallback("MOCK", request);
        assertEquals("SUCCESS", result.get("code"));
    }

    @Test
    @DisplayName("渠道回调 - 处理异常返回 FAIL（渠道会重试通知）")
    void payCallback_fail() {
        doThrow(new RuntimeException("sign error")).when(payNotifyService)
                .processNotify(eq("WECHAT"), any(NotifyContext.class));
        var result = payCallbackController.payCallback("WECHAT", new MockHttpServletRequest());
        assertEquals("FAIL", result.get("code"));
    }
}
