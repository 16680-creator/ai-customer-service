package com.aics.order.statemachine;

import com.aics.order.enums.OrderStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;

/**
 * 订单状态机配置：只声明合法迁移，不承载跨服务副作用。
 *
 * <p>每次业务调用通过 {@link OrderStateTransitionService} 重置到 DB 当前态再发事件，
 * 不使用内存 persister（多实例下内存状态不可靠），DB status 是唯一事实来源。</p>
 */
@Configuration
public class OrderStateMachineConfig {

    @Bean
    public StateMachine<OrderStatus, OrderEvent> orderStateMachine() throws Exception {
        StateMachineBuilder.Builder<OrderStatus, OrderEvent> builder = StateMachineBuilder.builder();
        builder.configureStates().withStates()
                .initial(OrderStatus.PENDING_PAY)
                .states(java.util.EnumSet.allOf(OrderStatus.class));
        builder.configureTransitions()
                .withExternal().source(OrderStatus.PENDING_PAY).target(OrderStatus.PAID).event(OrderEvent.PAY).and()
                .withExternal().source(OrderStatus.PENDING_PAY).target(OrderStatus.CANCELLED).event(OrderEvent.CANCEL).and()
                .withExternal().source(OrderStatus.PENDING_PAY).target(OrderStatus.CANCELLED).event(OrderEvent.TIMEOUT).and()
                .withExternal().source(OrderStatus.PAID).target(OrderStatus.REFUNDING).event(OrderEvent.REFUND_REQUEST).and()
                .withExternal().source(OrderStatus.REFUNDING).target(OrderStatus.REFUNDED).event(OrderEvent.REFUND_SUCCESS);
        return builder.build();
    }
}
