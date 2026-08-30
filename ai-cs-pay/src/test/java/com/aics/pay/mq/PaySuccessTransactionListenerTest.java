package com.aics.pay.mq;

import com.aics.common.mq.PaySuccessMessage;
import com.aics.pay.entity.PayTransaction;
import com.aics.pay.service.PayTransactionService;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * 支付成功事务消息监听器单元测试
 * TDD: 半消息本地事务（COMMIT/ROLLBACK）+ 回查决策（SUCCESS/PENDING/CLOSED/缺失）
 */
@ExtendWith(MockitoExtension.class)
class PaySuccessTransactionListenerTest {

    @Mock
    private PayTransactionService payTransactionService;

    @InjectMocks
    private PaySuccessTransactionListener listener;

    private PaySuccessMessage payload;
    private org.springframework.messaging.Message<PaySuccessMessage> message;

    @BeforeEach
    void setUp() throws Exception {
        payload = PaySuccessMessage.builder()
                .orderNo("ORD20260830001")
                .paymentMethod("MOCK")
                .amount(new BigDecimal("99.90"))
                .tradeNo("TX123")
                .build();
        // 回查时 payload 由 rocketmq-spring converter 还原为对象
        message = org.springframework.messaging.support.MessageBuilder.withPayload(payload).build();
    }

    @Test
    @DisplayName("半消息本地事务：流水标记成功则 COMMIT")
    void executeLocalTransaction_commit() {
        RocketMQLocalTransactionState state = listener.executeLocalTransaction(message, payload);
        assertEquals(RocketMQLocalTransactionState.COMMIT, state);
        org.mockito.Mockito.verify(payTransactionService).markSuccess(
                eq("ORD20260830001"), eq("TX123"), eq(new BigDecimal("99.90")));
    }

    @Test
    @DisplayName("半消息本地事务：流水落库异常则 ROLLBACK")
    void executeLocalTransaction_rollback() {
        doThrow(new RuntimeException("db down")).when(payTransactionService)
                .markSuccess(anyString(), anyString(), any());
        RocketMQLocalTransactionState state = listener.executeLocalTransaction(message, payload);
        assertEquals(RocketMQLocalTransactionState.ROLLBACK, state);
    }

    @Test
    @DisplayName("回查：流水已 SUCCESS 则 COMMIT")
    void checkLocalTransaction_success() {
        PayTransaction tx = new PayTransaction();
        tx.setStatus("SUCCESS");
        when(payTransactionService.getByOrderNo("ORD20260830001")).thenReturn(tx);
        assertEquals(RocketMQLocalTransactionState.COMMIT, listener.checkLocalTransaction(message));
    }

    @Test
    @DisplayName("回查：流水仍 PENDING 则 UNKNOWN 等待下次回查")
    void checkLocalTransaction_pending() {
        PayTransaction tx = new PayTransaction();
        tx.setStatus("PENDING");
        when(payTransactionService.getByOrderNo("ORD20260830001")).thenReturn(tx);
        assertEquals(RocketMQLocalTransactionState.UNKNOWN, listener.checkLocalTransaction(message));
    }

    @Test
    @DisplayName("回查：流水已关闭则 ROLLBACK")
    void checkLocalTransaction_closed() {
        PayTransaction tx = new PayTransaction();
        tx.setStatus("CLOSED");
        when(payTransactionService.getByOrderNo("ORD20260830001")).thenReturn(tx);
        assertEquals(RocketMQLocalTransactionState.ROLLBACK, listener.checkLocalTransaction(message));
    }

    @Test
    @DisplayName("回查：流水不存在则 ROLLBACK")
    void checkLocalTransaction_missing() {
        when(payTransactionService.getByOrderNo(anyString())).thenReturn(null);
        assertEquals(RocketMQLocalTransactionState.ROLLBACK, listener.checkLocalTransaction(message));
    }
}
