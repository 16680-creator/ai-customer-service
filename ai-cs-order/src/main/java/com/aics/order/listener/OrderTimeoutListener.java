package com.aics.order.listener;

import com.aics.order.event.OrderTimeoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 订单超时取消消息监听器
 * 消费延迟消息，检查订单状态，未支付则执行取消
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "order-timeout-topic",
        consumerGroup = "order-timeout-consumer-group"
)
public class OrderTimeoutListener implements RocketMQListener<String> {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(String orderNo) {
        log.info("收到订单超时消息，发布领域事件: orderNo={}", orderNo);
        try {
            eventPublisher.publishEvent(new OrderTimeoutEvent(orderNo));
        } catch (Exception e) {
            log.error("发布订单超时领域事件失败: orderNo={}", orderNo, e);
        }
    }
}
