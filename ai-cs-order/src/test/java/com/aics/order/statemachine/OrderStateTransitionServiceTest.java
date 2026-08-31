package com.aics.order.statemachine;

import com.aics.common.exception.BusinessException;
import com.aics.order.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.statemachine.StateMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 订单状态机迁移矩阵：所有合法迁移与关键非法迁移。 */
class OrderStateTransitionServiceTest {

    private OrderStateTransitionService service() throws Exception {
        StateMachine<OrderStatus, OrderEvent> machine = new OrderStateMachineConfig().orderStateMachine();
        return new OrderStateTransitionService(machine);
    }

    @Test
    @DisplayName("待支付 - 支付/取消/超时三条合法路径")
    void pendingPayTransitions() throws Exception {
        assertEquals(OrderStatus.PAID, service().transit("PENDING_PAY", OrderEvent.PAY));
        assertEquals(OrderStatus.CANCELLED, service().transit("PENDING_PAY", OrderEvent.CANCEL));
        assertEquals(OrderStatus.CANCELLED, service().transit("PENDING_PAY", OrderEvent.TIMEOUT));
    }

    @Test
    @DisplayName("已支付 - 退款申请 - 退款成功")
    void refundTransitions() throws Exception {
        assertEquals(OrderStatus.REFUNDING, service().transit("PAID", OrderEvent.REFUND_REQUEST));
        assertEquals(OrderStatus.REFUNDED, service().transit("REFUNDING", OrderEvent.REFUND_SUCCESS));
    }

    @Test
    @DisplayName("非法迁移 - 已取消不能支付，待支付不能直接退款成功")
    void illegalTransitionsMustBeRejected() throws Exception {
        assertThrows(BusinessException.class, () -> service().transit("CANCELLED", OrderEvent.PAY));
        assertThrows(BusinessException.class, () -> service().transit("PENDING_PAY", OrderEvent.REFUND_SUCCESS));
    }
}
