package com.aics.order.event;

import com.aics.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时领域事件监听器：执行幂等关单业务。
 *
 * <p>这里用普通 {@link EventListener}（同步执行）：RocketMQ 消费线程只有在关单逻辑
 * 返回后才算消费完成；若业务异常，外层消息监听器记录错误并由 MQ 重试/告警策略兜底。
 * 与 {@code OrderPaidEventListener} 的 AFTER_COMMIT 不同——后者处理的是事务提交后的副作用。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutEventListener {

    private final OrderService orderService;

    @EventListener
    public void handle(OrderTimeoutEvent event) {
        log.info("处理订单超时领域事件: orderNo={}", event.orderNo());
        orderService.cancelExpiredOrder(event.orderNo());
    }
}
