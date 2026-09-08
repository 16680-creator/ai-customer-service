package com.aics.mq.producer;

import com.aics.mq.dto.WorkOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * 工单消息生产者。
 * <p>
 * 与 {@link com.aics.mq.consumer.WorkOrderListener} 监听的 topic 保持一致。
 * 使用同步发送（{@link RocketMQTemplate#convertAndSend}），发送失败抛异常由调用方处理；
 * 不在此处重试以避免重复投递（消费端按幂等设计）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderProducer {

    /** 消息主题 */
    public static final String TOPIC = "work-order-topic";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送工单消息
     *
     * @param message 工单消息（至少包含 ticketNo）
     */
    public void send(WorkOrderMessage message) {
        log.info("发送工单消息到RocketMQ: ticketNo={}, simulateFail={}",
                message.getTicketNo(), message.getSimulateFail());
        rocketMQTemplate.convertAndSend(TOPIC, message);
        log.info("工单消息发送成功: ticketNo={}", message.getTicketNo());
    }
}
