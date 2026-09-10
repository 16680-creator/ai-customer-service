package com.aics.mq.producer;

import com.aics.common.mq.MessagePublisher;
import com.aics.mq.dto.WorkOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单消息生产者。
 * <p>
 * 注意此处依赖的是 {@link MessagePublisher}（抽象层），不是 {@code RocketMQTemplate}：
 * 通过配置 {@code aics.mq.type=rocketmq|kafka} 即可切换底层 Broker，
 * 业务代码零改动。topic 保持一致，消费端按同一个开关装配对应监听器。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderProducer {

    /** 消息主题（RocketMQ Topic / Kafka Topic 同名） */
    public static final String TOPIC = "work-order-topic";

    /**
     * 消息二级分类：RocketMQ 映射为 Tag，Kafka 映射为 Record Key
     * （同一 Key 落同一分区，保证同工单类型消息分区内有序）。
     */
    public static final String TAG = "WORK_ORDER";

    private final MessagePublisher messagePublisher;

    /**
     * 发送工单消息
     *
     * @param message 工单消息（至少包含 ticketNo）
     */
    public void send(WorkOrderMessage message) {
        log.info("发送工单消息到 {}: ticketNo={}, simulateFail={}",
                messagePublisher.brokerType(), message.getTicketNo(), message.getSimulateFail());
        messagePublisher.send(TOPIC, TAG, message);
        log.info("工单消息发送成功: ticketNo={}", message.getTicketNo());
    }

    /** 当前生效的消息中间件类型（供接口/日志展示开关状态） */
    public String brokerType() {
        return messagePublisher.brokerType().name();
    }
}
