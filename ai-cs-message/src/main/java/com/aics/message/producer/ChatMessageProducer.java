package com.aics.message.producer;

import com.aics.message.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * 聊天消息 RocketMQ 生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /** 消息主题 */
    private static final String TOPIC = "chat-message-topic";

    /**
     * 发送聊天消息到 RocketMQ
     *
     * @param message 聊天消息
     */
    public void send(ChatMessage message) {
        log.info("发送聊天消息到RocketMQ: sessionId={}, role={}", message.getSessionId(), message.getRole());
        rocketMQTemplate.convertAndSend(TOPIC, message);
        log.info("聊天消息发送成功: sessionId={}", message.getSessionId());
    }
}
