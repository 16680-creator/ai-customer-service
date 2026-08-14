package com.aics.notify.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

/**
 * NotifyServiceImpl 单元测试（静态推送委托）
 */
class NotifyServiceImplTest {

    private final NotifyServiceImpl notifyService = new NotifyServiceImpl();

    @Test
    @DisplayName("sendToUser - 委托静态定向推送")
    void sendToUser_shouldDelegate() {
        try (MockedStatic<com.aics.notify.websocket.NotifyWebSocketHandler> mocked =
                     mockStatic(com.aics.notify.websocket.NotifyWebSocketHandler.class)) {
            notifyService.sendToUser("1001", "hello");

            mocked.verify(() -> com.aics.notify.websocket.NotifyWebSocketHandler.sendMessageToUser("1001", "hello"));
        }
    }

    @Test
    @DisplayName("broadcast - 委托静态广播")
    void broadcast_shouldDelegate() {
        try (MockedStatic<com.aics.notify.websocket.NotifyWebSocketHandler> mocked =
                     mockStatic(com.aics.notify.websocket.NotifyWebSocketHandler.class)) {
            notifyService.broadcast("hello-all");

            mocked.verify(() -> com.aics.notify.websocket.NotifyWebSocketHandler.broadcastMessage("hello-all"));
        }
    }

    @Test
    @DisplayName("getOnlineCount - 返回在线用户数")
    void getOnlineCount_shouldReturn() {
        try (MockedStatic<com.aics.notify.websocket.NotifyWebSocketHandler> mocked =
                     mockStatic(com.aics.notify.websocket.NotifyWebSocketHandler.class)) {
            mocked.when(com.aics.notify.websocket.NotifyWebSocketHandler::getOnlineCount).thenReturn(3);

            assertEquals(3, notifyService.getOnlineCount());
        }
    }
}
