package com.aics.order.service;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.pay.channel.NotifyContext;
import com.aics.order.pay.channel.NotifyResult;
import com.aics.order.pay.channel.PayChannel;
import com.aics.order.pay.channel.PayChannelFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 支付通知统一处理单元测试
 * 验证：验签失败拒绝、支付成功幂等更新订单状态、非成功通知不更新。
 */
@ExtendWith(MockitoExtension.class)
class PayNotifyServiceTest {

    @Mock
    private PayChannelFactory payChannelFactory;

    @Mock
    private PayChannel payChannel;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PayNotifyService payNotifyService;

    @Test
    @DisplayName("处理通知 - 验签/解析失败应抛出异常且不更新订单")
    void processNotify_parseFail_shouldThrowAndNotUpdateOrder() {
        when(payChannelFactory.getChannel("MOCK")).thenReturn(payChannel);
        when(payChannel.parseNotify(any())).thenThrow(
                new BusinessException(ResultCode.UNAUTHORIZED, "支付回调验签失败"));

        assertThrows(BusinessException.class,
                () -> payNotifyService.processNotify("MOCK", NotifyContext.builder().params(Map.of()).build()));
        verify(orderService, never()).handlePayCallback(any(), any());
    }

    @Test
    @DisplayName("处理通知 - 支付成功应更新订单状态")
    void processNotify_success_shouldUpdateOrder() {
        when(payChannelFactory.getChannel("MOCK")).thenReturn(payChannel);
        when(payChannel.parseNotify(any())).thenReturn(NotifyResult.builder()
                .orderNo("ORD001")
                .success(true)
                .amount(new BigDecimal("199.00"))
                .build());

        payNotifyService.processNotify("MOCK", NotifyContext.builder().params(Map.of()).build());

        verify(orderService).handlePayCallback("ORD001", "MOCK");
    }

    @Test
    @DisplayName("处理通知 - 非成功通知不更新订单")
    void processNotify_notSuccess_shouldNotUpdateOrder() {
        when(payChannelFactory.getChannel("MOCK")).thenReturn(payChannel);
        when(payChannel.parseNotify(any())).thenReturn(NotifyResult.builder()
                .orderNo("ORD002")
                .success(false)
                .build());

        payNotifyService.processNotify("MOCK", NotifyContext.builder().params(Map.of()).build());

        verify(orderService, never()).handlePayCallback(any(), any());
    }

    @Test
    @DisplayName("查单兜底 - 渠道已支付应更新订单")
    void syncByQuery_success_shouldUpdateOrder() {
        when(payChannelFactory.getChannel("ALIPAY")).thenReturn(payChannel);
        when(payChannel.queryPayment("ORD001")).thenReturn(PayChannel.STATUS_SUCCESS);

        boolean handled = payNotifyService.syncByQuery("ORD001", "ALIPAY");

        assertTrue(handled);
        verify(orderService).handlePayCallback("ORD001", "ALIPAY");
    }

    @Test
    @DisplayName("查单兜底 - 渠道未支付不更新订单")
    void syncByQuery_pending_shouldNotUpdateOrder() {
        when(payChannelFactory.getChannel("ALIPAY")).thenReturn(payChannel);
        when(payChannel.queryPayment("ORD001")).thenReturn(PayChannel.STATUS_PENDING);

        boolean handled = payNotifyService.syncByQuery("ORD001", "ALIPAY");

        assertFalse(handled);
        verify(orderService, never()).handlePayCallback(any(), any());
    }
}
