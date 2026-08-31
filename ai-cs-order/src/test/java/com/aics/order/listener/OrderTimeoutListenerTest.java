package com.aics.order.listener;

import com.aics.order.event.OrderTimeoutEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * 订单超时消息监听器测试（协议适配层）：RocketMQ 消息只负责发布进程内领域事件，
 * 关单业务由 OrderTimeoutEventListener 执行。
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutListenerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderTimeoutListener listener;

    @Test
    @DisplayName("收到超时消息 - 应发布 OrderTimeoutEvent（携带 orderNo）")
    void onMessage_shouldPublishDomainEvent() {
        listener.onMessage("20260801120000010001");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        OrderTimeoutEvent timeout = (OrderTimeoutEvent) captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("20260801120000010001", timeout.orderNo());
    }

    @Test
    @DisplayName("发布事件异常 - 不外抛（避免消费线程崩溃）")
    void onMessage_publishException_shouldNotPropagate() {
        doThrow(new RuntimeException("event dispatch failed"))
                .when(eventPublisher).publishEvent(any(OrderTimeoutEvent.class));

        assertDoesNotThrow(() -> listener.onMessage("20260801120000010003"));
        verify(eventPublisher).publishEvent(any(OrderTimeoutEvent.class));
    }

    @Test
    @DisplayName("收到空消息 - 仍发布领域事件（业务监听器幂等处理）")
    void onMessage_nullOrderNo_shouldNotThrow() {
        assertDoesNotThrow(() -> listener.onMessage(null));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        OrderTimeoutEvent timeout = (OrderTimeoutEvent) captor.getValue();
        org.junit.jupiter.api.Assertions.assertNull(timeout.orderNo());
    }
}
