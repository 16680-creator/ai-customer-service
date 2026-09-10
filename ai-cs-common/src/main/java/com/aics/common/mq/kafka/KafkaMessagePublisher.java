package com.aics.common.mq.kafka;

import com.aics.common.mq.MessageBrokerType;
import com.aics.common.mq.MessagePublishException;
import com.aics.common.mq.MessagePublisher;
import com.aics.common.mq.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 实现。
 * <p>
 * 与 RocketMQ 实现的语义对齐点：</p>
 * <ul>
 *   <li>{@code key} 映射为 Kafka Record Key —— 相同 Key 落到同一分区，
 *       获得「分区内有序」，等价于 RocketMQ Tag 的业务分组意图；</li>
 *   <li>KafkaTemplate 天然异步，这里用 {@code future.get(timeout)} 等待结果，
 *       对外暴露与 RocketMQ 一致的「发送失败即抛异常」语义。</li>
 * </ul>
 *
 * <p><b>延迟消息</b>：Kafka 没有服务端延迟消息。本实现降级为 {@link TaskScheduler}
 * 进程内延时投递，只能用于本地演示 / 非关键路径；进程重启即丢失。
 * 生产上需要可靠延迟应改用：定时任务扫描 + 时间轮，或 RocketMQ / Pulsar。</p>
 */
@Slf4j
public class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final MqProperties properties;
    private final TaskScheduler delayScheduler;

    public KafkaMessagePublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                                 MqProperties properties,
                                 TaskScheduler delayScheduler) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.delayScheduler = delayScheduler;
    }

    @Override
    public void send(String topic, String key, Object payload) {
        try {
            // 等待 ack，把 Kafka 的异步模型收敛成「失败即抛」的同步语义
            kafkaTemplate.send(topic, key, payload)
                    .get(properties.getSendTimeoutMillis(), TimeUnit.MILLISECONDS);
            log.debug("Kafka 消息发送成功: topic={}, key={}", topic, key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagePublishException("Kafka 消息发送被中断: topic=" + topic, e);
        } catch (Exception e) {
            throw new MessagePublishException("Kafka 消息发送失败: topic=" + topic + ", key=" + key, e);
        }
    }

    @Override
    public void sendDelayed(String topic, String key, Object payload, long delayMillis) {
        log.warn("Kafka 无原生延迟消息，降级为进程内调度（重启会丢失）: topic={}, key={}, delayMillis={}",
                topic, key, delayMillis);
        delayScheduler.schedule(() -> {
            try {
                send(topic, key, payload);
            } catch (Exception e) {
                // 调度线程里不能再抛，只能记录；这本身就是「非可靠延迟」的代价
                log.error("Kafka 降级延迟消息投递失败: topic={}, key={}", topic, key, e);
            }
        }, Instant.now().plusMillis(delayMillis));
    }

    @Override
    public MessageBrokerType brokerType() {
        return MessageBrokerType.KAFKA;
    }
}
