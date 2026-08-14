package com.aics.message.producer;

import com.aics.message.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * 聊天消息 RocketMQ 生产者
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：将 {@link ChatMessage} 通过 {@link RocketMQTemplate} 异步投递到 chat-message-topic 主题，
 * 由 {@code ChatMessageConsumer} 消费后落库，实现「发送 → 持久化」的解耦。
 * 设计要点：发送接口立即返回，不阻塞调用方；落库时机由消费者保证，
 * 即便 DB 短时不可用也不影响上游聊天链路。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    /** RocketMQ 模板，由 spring-boot-starter 自动装配注入 */
    private final RocketMQTemplate rocketMQTemplate;

    /** 消息主题：与消费者 {@link com.aics.message.consumer.ChatMessageConsumer} 监听的 topic 保持一致 */
    private static final String TOPIC = "chat-message-topic";

    /**
     * 发送聊天消息到 RocketMQ
     * <p>
     * 使用同步发送（{@link RocketMQTemplate#convertAndSend}），
     * 发送失败会抛出异常，调用方需按需处理；不在此处重试以避免重复投递。
     * </p>
     *
     * @param message 聊天消息（应至少包含 sessionId/sessionKey、role、content）
     */
    public void send(ChatMessage message) {
        log.info("发送聊天消息到RocketMQ: sessionId={}, role={}", message.getSessionId(), message.getRole());
        rocketMQTemplate.convertAndSend(TOPIC, message);
        log.info("聊天消息发送成功: sessionId={}", message.getSessionId());
    }
}
