package com.aics.order.event;

import com.aics.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 订单超时领域事件监听器单测：事件协议适配与关单业务解耦后，监听器负责委托。
 */
class OrderTimeoutEventListenerTest {

    @Test
    @DisplayName("超时领域事件 - 应委托 OrderService 幂等关单")
    void shouldDelegateToOrderService() {
        OrderService orderService = mock(OrderService.class);
        OrderTimeoutEventListener listener = new OrderTimeoutEventListener(orderService);

        listener.handle(new OrderTimeoutEvent("ORD-TIMEOUT-1"));

        verify(orderService).cancelExpiredOrder("ORD-TIMEOUT-1");
    }
}
