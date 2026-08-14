package com.aics.notify.consumer;

import com.aics.notify.websocket.NotifyWebSocketHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mockStatic;

/**
 * OrderNotifyConsumer 单元测试（RocketMQ 消息 → WebSocket 推送委托）
 */
class OrderNotifyConsumerTest {

    private final OrderNotifyConsumer consumer = new OrderNotifyConsumer();

    @Test
    @DisplayName("收到完整消息 - 推送给指定用户")
    void onMessage_complete_shouldPush() {
        Map<String, String> msg = new HashMap<>();
        msg.put("userId", "1001");
        msg.put("message", "您的订单已发货");
        try (MockedStatic<NotifyWebSocketHandler> mocked = mockStatic(NotifyWebSocketHandler.class)) {
            consumer.onMessage(msg);

            mocked.verify(() -> NotifyWebSocketHandler.sendMessageToUser("1001", "您的订单已发货"));
        }
    }

    @Test
    @DisplayName("消息缺少 message - 静默忽略")
    void onMessage_missingMessage_shouldIgnore() {
        Map<String, String> msg = new HashMap<>();
        msg.put("userId", "1001");
        try (MockedStatic<NotifyWebSocketHandler> mocked = mockStatic(NotifyWebSocketHandler.class)) {
            consumer.onMessage(msg);

            mocked.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("消息缺少 userId - 静默忽略")
    void onMessage_missingUserId_shouldIgnore() {
        Map<String, String> msg = new HashMap<>();
        msg.put("message", "您的订单已发货");
        try (MockedStatic<NotifyWebSocketHandler> mocked = mockStatic(NotifyWebSocketHandler.class)) {
            consumer.onMessage(msg);

            mocked.verifyNoInteractions();
        }
    }
}
