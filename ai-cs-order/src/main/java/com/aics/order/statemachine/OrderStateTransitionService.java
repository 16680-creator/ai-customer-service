package com.aics.order.statemachine;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

/**
 * DB 状态到状态机事件的适配器：集中校验合法迁移，返回目标状态码。
 */
@Service
@RequiredArgsConstructor
public class OrderStateTransitionService {

    private final StateMachine<OrderStatus, OrderEvent> orderStateMachine;

    public OrderStatus transit(String currentStatus, OrderEvent event) {
        OrderStatus current = parse(currentStatus);
        synchronized (orderStateMachine) {
            orderStateMachine.stop();
            orderStateMachine.getStateMachineAccessor().doWithAllRegions(access ->
                    access.resetStateMachine(new DefaultStateMachineContext<>(current, null, null, null)));
            orderStateMachine.start();
            boolean accepted = orderStateMachine.sendEvent(MessageBuilder.withPayload(event).build());
            if (!accepted) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "订单状态不允许迁移: " + current.getCode() + " --" + event + "--> ?");
            }
            return orderStateMachine.getState().getId();
        }
    }

    private OrderStatus parse(String status) {
        return java.util.Arrays.stream(OrderStatus.values())
                .filter(item -> item.getCode().equals(status))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "未知订单状态: " + status));
    }
}
