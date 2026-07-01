package com.aics.message.consumer;

import com.aics.message.entity.ChatMessage;
import com.aics.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 聊天消息 RocketMQ 消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "chat-message-topic",
        consumerGroup = "chat-consumer-group"
)
public class ChatMessageConsumer implements RocketMQListener<ChatMessage> {

    private final MessageService messageService;

    @Override
    public void onMessage(ChatMessage message) {
        log.info("消费聊天消息: sessionId={}, role={}", message.getSessionId(), message.getRole());
        try {
            messageService.saveMessage(message);
            log.info("聊天消息消费成功并入库: sessionId={}", message.getSessionId());
        } catch (Exception e) {
            log.error("聊天消息消费失败: sessionId={}", message.getSessionId(), e);
            throw e;
        }
    }
}
