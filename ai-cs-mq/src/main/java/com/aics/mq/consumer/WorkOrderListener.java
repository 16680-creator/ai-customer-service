package com.aics.mq.consumer;

import com.aics.mq.dto.WorkOrderMessage;
import com.aics.mq.producer.WorkOrderProducer;
import com.aics.mq.service.WorkOrderMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 工单消息消费者（RocketMQ 分支）。
 * <p>
 * 只在 {@code aics.mq.type=rocketmq}（或缺省）时注册；
 * 切换为 Kafka 时本监听器不装配，由
 * {@link com.aics.mq.consumer.kafka.WorkOrderKafkaListener} 接管。</p>
 *
 * <p>消费失败处理链路（本演示的核心）：
 * <ul>
 *   <li>simulateFail=true 时抛出异常 → RocketMQ 按 {@code maxReconsumeTimes=3} 自动重试
 *      （默认延迟级别约 10s/30s/1m）</li>
 *   <li>重试耗尽后消息进入死信主题 {@code %DLQ%work-order-consumer-group}，
 *       由 {@link WorkOrderDlqListener} 落库失败记录</li>
 * </ul>
 * 业务逻辑在 {@link WorkOrderMessageHandler}，与 Broker 解耦。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aics.mq", name = "type", havingValue = "rocketmq", matchIfMissing = true)
@RocketMQMessageListener(
        topic = WorkOrderProducer.TOPIC,
        consumerGroup = "work-order-consumer-group",
        maxReconsumeTimes = 3
)
public class WorkOrderListener implements RocketMQListener<WorkOrderMessage> {

    private final WorkOrderMessageHandler messageHandler;

    @Override
    public void onMessage(WorkOrderMessage message) {
        messageHandler.handle(message);
    }
}
