package com.aics.mq.consumer.kafka;

import com.aics.mq.dto.WorkOrderMessage;
import com.aics.mq.producer.WorkOrderProducer;
import com.aics.mq.service.WorkOrderMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 工单消息消费者（Kafka 分支），仅在 {@code aics.mq.type=kafka} 时注册。
 * <p>
 * 与 RocketMQ 版本 {@code WorkOrderListener} 的差异：</p>
 * <ul>
 *   <li>消费组用 {@code groupId}，RocketMQ 用 {@code consumerGroup}；</li>
 *   <li>消息入参是 {@link ConsumerRecord}（可拿到 topic/partition/offset/key/header，
 *       便于日志排查与幂等键构造），RocketMQ 拿到的是反序列化后的消息体；</li>
 *   <li>失败重试与死信不在注解上配置，而是由 {@code KafkaConsumerConfig} 里的
 *       {@code DefaultErrorHandler} + {@code DeadLetterPublishingRecoverer} 统一处理；</li>
 *   <li>业务逻辑复用 {@link WorkOrderMessageHandler}，保证两种 Broker 语义一致。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aics.mq", name = "type", havingValue = "kafka")
public class WorkOrderKafkaListener {

    private final WorkOrderMessageHandler messageHandler;

    @KafkaListener(
            topics = WorkOrderProducer.TOPIC,
            groupId = "work-order-consumer-group"
    )
    public void onMessage(ConsumerRecord<String, WorkOrderMessage> record) {
        log.info("收到 Kafka 工单消息: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        // 抛异常 → DefaultErrorHandler 重试 3 次 → 转发到 work-order-topic-dlt
        messageHandler.handle(record.value());
    }
}
