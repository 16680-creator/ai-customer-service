package com.aics.notify.consumer;

import com.aics.notify.websocket.NotifyWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单状态变更通知消费者
 * 消费 ai-cs-order 投递的 notify-topic 消息，通过 WebSocket 推送给用户
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "notify-topic",
        consumerGroup = "notify-consumer-group"
)
public class OrderNotifyConsumer implements RocketMQListener<Map<String, String>> {

    @Override
    public void onMessage(Map<String, String> message) {
        String userId = message.get("userId");
        String content = message.get("message");
        log.info("收到订单通知消息: userId={}, message={}", userId, content);
        if (userId != null && content != null) {
            NotifyWebSocketHandler.sendMessageToUser(userId, content);
        }
    }
}