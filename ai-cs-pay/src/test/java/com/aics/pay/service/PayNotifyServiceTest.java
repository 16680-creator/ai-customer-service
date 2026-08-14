package com.aics.pay.service;

import com.aics.pay.channel.NotifyContext;
import com.aics.pay.channel.NotifyResult;
import com.aics.pay.channel.PayChannel;
import com.aics.pay.channel.PayChannelFactory;
import com.aics.pay.client.OrderPayClient;
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

@ExtendWith(MockitoExtension.class)
class PayNotifyServiceTest {

    @Mock
    private PayChannelFactory payChannelFactory;
    @Mock
    private PayChannel payChannel;
    @Mock
    private PayTransactionService payTransactionService;
    @Mock
    private OrderPayClient orderPayClient;

    @InjectMocks
    private PayNotifyService payNotifyService;

    @Test
    void processNotify_success_shouldMarkSuccessAndConfirmOrder() {
        when(payChannelFactory.getChannel("ALIPAY")).thenReturn(payChannel);
        when(payChannel.parseNotify(any())).thenReturn(NotifyResult.builder()
                .orderNo("ORD001").success(true).amount(new BigDecimal("199.00")).transactionId("tx1").build());

        payNotifyService.processNotify("ALIPAY", NotifyContext.builder().params(Map.of()).build());

        verify(payTransactionService).markSuccess("ORD001", "tx1", new BigDecimal("199.00"));
        verify(orderPayClient).confirmPay("ORD001", "ALIPAY", new BigDecimal("199.00"), "tx1");
    }

    @Test
    void processNotify_notSuccess_shouldNotConfirm() {
        when(payChannelFactory.getChannel("MOCK")).thenReturn(payChannel);
        when(payChannel.parseNotify(any())).thenReturn(NotifyResult.builder().orderNo("ORD001").success(false).build());

        payNotifyService.processNotify("MOCK", NotifyContext.builder().params(Map.of()).build());

        verify(orderPayClient, never()).confirmPay(any(), any(), any(), any());
    }

    @Test
    void syncByQuery_success_shouldConfirmOrder() {
        when(payChannelFactory.getChannel("ALIPAY")).thenReturn(payChannel);
        when(payChannel.queryPayment("ORD001")).thenReturn(PayChannel.STATUS_SUCCESS);

        boolean handled = payNotifyService.syncByQuery("ORD001", "ALIPAY");

        verify(orderPayClient).confirmPay("ORD001", "ALIPAY", null, null);
        assertTrue(handled);
    }

    @Test
    void syncByQuery_pending_shouldNotConfirm() {
        when(payChannelFactory.getChannel("ALIPAY")).thenReturn(payChannel);
        when(payChannel.queryPayment("ORD001")).thenReturn(PayChannel.STATUS_PENDING);

        assertFalse(payNotifyService.syncByQuery("ORD001", "ALIPAY"));
        verify(orderPayClient, never()).confirmPay(any(), any(), any(), any());
    }

    @Test
    void closeOrder_shouldCloseChannelAndMarkClosed() {
        when(payChannelFactory.getChannel("WECHAT")).thenReturn(payChannel);

        payNotifyService.closeOrder("ORD001", "WECHAT");

        verify(payChannel).closeOrder("ORD001");
        verify(payTransactionService).markClosed("ORD001");
    }
}