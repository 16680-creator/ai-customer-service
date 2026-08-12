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
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：订阅 chat-message-topic 主题，接收 chat 模块或其他上游投递的 {@link ChatMessage}，
 * 调用 {@link MessageService#saveMessage} 落库，完成消息的异步持久化。
 * 关键协作：
 * <ul>
 *     <li>上游生产者：{@code ChatMessageProducer}（本模块）或 chat 模块，发送消息到同一 topic；</li>
 *     <li>下游服务：{@link MessageService} 负责实际的入库逻辑。</li>
 * </ul>
 * 技术要点：通过 {@link RocketMQMessageListener} 声明 topic 与消费组，
 * 实现幂等消费需业务方自行保证（当前以 DB 主键自增 + sessionKey 关联为准）。
 * 异常处理：消费失败时记录日志并重新抛出，由 RocketMQ 触发重试。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "chat-message-topic",
        consumerGroup = "chat-consumer-group"
)
public class ChatMessageConsumer implements RocketMQListener<ChatMessage> {

    /** 消息服务，负责把消费到的消息写入 DB */
    private final MessageService messageService;

    /**
     * 处理一条到达的聊天消息：落库并记录日志。
     * <p>
     * 失败时抛出异常以触发 RocketMQ 重试机制；不在此处吞异常，
     * 避免消息丢失且无人感知。
     * </p>
     *
     * @param message 接收到的聊天消息（含 sessionId、sessionKey、role、content 等）
     */
    @Override
    public void onMessage(ChatMessage message) {
        log.info("消费聊天消息: sessionId={}, role={}", message.getSessionId(), message.getRole());
        try {
            messageService.saveMessage(message);
            log.info("聊天消息消费成功并入库: sessionId={}", message.getSessionId());
        } catch (Exception e) {
            // 重新抛出，让 RocketMQ 按消费组配置进行重试
            log.error("聊天消息消费失败: sessionId={}", message.getSessionId(), e);
            throw e;
        }
    }
}
