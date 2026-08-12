package com.aics.notify.consumer;

import com.aics.notify.websocket.NotifyWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单状态变更通知消费者 —— 监听 RocketMQ 订单事件并实时推送。
 *
 * <h3>学习要点（技术：MQ 异步解耦 + WebSocket 推送）</h3>
 * <ul>
 *   <li><b>链路</b>：订单服务支付成功 → 发 RocketMQ（notify-topic）→ 本消费者收到
 *       → 经 {@link NotifyWebSocketHandler} 推送给用户浏览器。订单与通知完全解耦。</li>
 *   <li><b>为什么用 MQ 而非直接调用</b>：订单主流程不需要等通知完成；MQ 削峰、
 *       失败可重试，通知延迟可接受。</li>
 *   <li><b>@RocketMQMessageListener</b>：RocketMQ starter 注解式消费，自动注册消费者。</li>
 * </ul>
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