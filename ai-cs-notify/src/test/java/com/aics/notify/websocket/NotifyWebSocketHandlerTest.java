package com.aics.notify.websocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotifyWebSocketHandler 单元测试（覆盖连接生命周期与定向/广播推送）
 */
class NotifyWebSocketHandlerTest {

    private NotifyWebSocketHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new NotifyWebSocketHandler();
        clearSessionMap();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearSessionMap();
    }

    @SuppressWarnings("unchecked")
    private void clearSessionMap() throws Exception {
        Field field = NotifyWebSocketHandler.class.getDeclaredField("SESSION_MAP");
        field.setAccessible(true);
        ConcurrentHashMap<String, WebSocketSession> map =
                (ConcurrentHashMap<String, WebSocketSession>) field.get(null);
        map.clear();
    }

    private WebSocketSession mockSession(String userId, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        String query = userId == null ? "" : "userId=" + userId;
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/notify?" + query));
        when(session.getId()).thenReturn("s-" + userId);
        when(session.isOpen()).thenReturn(open);
        return session;
    }

    @Test
    @DisplayName("连接建立 - 携带 userId 时注册在线会话")
    void afterConnectionEstablished_withUserId_shouldRegister() throws Exception {
        handler.afterConnectionEstablished(mockSession("1001", true));

        assertEquals(1, NotifyWebSocketHandler.getOnlineCount());
    }

    @Test
    @DisplayName("连接建立 - 缺少 userId 时不注册")
    void afterConnectionEstablished_withoutUserId_shouldNotRegister() throws Exception {
        handler.afterConnectionEstablished(mockSession(null, true));

        assertEquals(0, NotifyWebSocketHandler.getOnlineCount());
    }

    @Test
    @DisplayName("连接关闭 - 移除在线会话")
    void afterConnectionClosed_shouldRemove() throws Exception {
        WebSocketSession session = mockSession("1002", false);
        handler.afterConnectionEstablished(session);
        assertEquals(1, NotifyWebSocketHandler.getOnlineCount());

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertEquals(0, NotifyWebSocketHandler.getOnlineCount());
    }

    @Test
    @DisplayName("心跳消息 - 回复 pong")
    void handleTextMessage_ping_shouldReplyPong() throws Exception {
        WebSocketSession session = mockSession("1003", true);

        handler.handleTextMessage(session, new TextMessage("ping"));

        verify(session).sendMessage(new TextMessage("pong"));
    }

    @Test
    @DisplayName("传输异常 - 会话仍打开时主动关闭")
    void handleTransportError_openSession_shouldClose() throws Exception {
        WebSocketSession session = mockSession("1004", true);

        handler.handleTransportError(session, new IllegalStateException("boom"));

        verify(session).close();
    }

    @Test
    @DisplayName("定向推送 - 在线用户收到消息")
    void sendMessageToUser_online_shouldSend() throws Exception {
        WebSocketSession session = mockSession("1005", true);
        handler.afterConnectionEstablished(session);

        NotifyWebSocketHandler.sendMessageToUser("1005", "hello");

        verify(session).sendMessage(new TextMessage("hello"));
    }

    @Test
    @DisplayName("定向推送 - 离线用户静默忽略")
    void sendMessageToUser_offline_shouldIgnore() {
        assertDoesNotThrow(() -> NotifyWebSocketHandler.sendMessageToUser("9999", "hello"));
    }

    @Test
    @DisplayName("定向推送 - 会话异常时记录日志不抛出")
    void sendMessageToUser_sendFail_shouldSwallow() throws Exception {
        WebSocketSession session = mockSession("1006", true);
        handler.afterConnectionEstablished(session);
        doThrow(new IOException("send fail")).when(session).sendMessage(any(TextMessage.class));

        assertDoesNotThrow(() -> NotifyWebSocketHandler.sendMessageToUser("1006", "hello"));
    }

    @Test
    @DisplayName("广播 - 推送给所有在线用户")
    void broadcast_shouldSendToAllOnline() throws Exception {
        WebSocketSession s1 = mockSession("1007", true);
        WebSocketSession s2 = mockSession("1008", true);
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        NotifyWebSocketHandler.broadcastMessage("broadcast-msg");

        verify(s1).sendMessage(new TextMessage("broadcast-msg"));
        verify(s2).sendMessage(new TextMessage("broadcast-msg"));
    }
}
