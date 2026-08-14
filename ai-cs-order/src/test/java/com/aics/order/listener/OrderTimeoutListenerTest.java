package com.aics.order.listener;

import com.aics.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * 订单超时消息监听器测试
 * TDD: 验证消息消费触发取消逻辑、异常不外抛
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderTimeoutListener listener;

    @Test
    @DisplayName("收到超时消息 - 应调用 cancelExpiredOrder")
    void onMessage_shouldCallCancelExpiredOrder() {
        String orderNo = "20260801120000010001";

        listener.onMessage(orderNo);

        verify(orderService, times(1)).cancelExpiredOrder(orderNo);
    }

    @Test
    @DisplayName("收到超时消息 - 订单已支付时 cancelExpiredOrder 内部跳过，不抛异常")
    void onMessage_orderAlreadyPaid_shouldNotThrow() {
        String orderNo = "20260801120000010002";
        doNothing().when(orderService).cancelExpiredOrder(orderNo);

        assertDoesNotThrow(() -> listener.onMessage(orderNo));
        verify(orderService).cancelExpiredOrder(orderNo);
    }

    @Test
    @DisplayName("收到超时消息 - 处理异常时不外抛（容错）")
    void onMessage_exceptionThrown_shouldNotPropagate() {
        String orderNo = "20260801120000010003";
        doThrow(new RuntimeException("DB connection lost"))
                .when(orderService).cancelExpiredOrder(orderNo);

        // 监听器内部 catch 了异常，不应向外传播
        assertDoesNotThrow(() -> listener.onMessage(orderNo));
        verify(orderService).cancelExpiredOrder(orderNo);
    }

    @Test
    @DisplayName("收到空消息 - 不应抛异常")
    void onMessage_nullOrderNo_shouldNotThrow() {
        doNothing().when(orderService).cancelExpiredOrder(null);

        assertDoesNotThrow(() -> listener.onMessage(null));
    }
}
