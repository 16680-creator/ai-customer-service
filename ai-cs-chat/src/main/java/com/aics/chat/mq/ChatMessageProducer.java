package com.aics.chat.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 对话消息 RocketMQ 生产者
 * 将对话消息投递到 chat-message-topic，由 ai-cs-message 消费落库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /** 消息主题（与 ai-cs-message 消费端一致） */
    private static final String TOPIC = "chat-message-topic";

    /**
     * 投递一条对话消息
     *
     * @param sessionKey 会话标识（chat 字符串会话ID）
     * @param role       角色：user/assistant
     * @param content    消息内容
     */
    public void send(String sessionKey, String role, String content) {
        try {
            Map<String, String> message = new HashMap<>();
            message.put("sessionKey", sessionKey);
            message.put("role", role);
            message.put("content", content);
            log.info("投递对话消息到 RocketMQ: sessionKey={}, role={}, len={}", sessionKey, role, content.length());
            rocketMQTemplate.convertAndSend(TOPIC, message);
        } catch (Exception e) {
            log.warn("对话消息投递失败: sessionKey={}, role={}, err={}", sessionKey, role, e.getMessage());
        }
    }
}