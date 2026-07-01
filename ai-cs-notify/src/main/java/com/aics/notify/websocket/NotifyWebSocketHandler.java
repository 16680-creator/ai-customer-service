package com.aics.notify.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器
 */
@Slf4j
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    /** 在线连接管理：userId -> WebSocketSession */
    private static final ConcurrentHashMap<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            SESSION_MAP.put(userId, session);
            log.info("WebSocket连接建立: userId={}, 当前在线数={}", userId, SESSION_MAP.size());
        } else {
            log.warn("WebSocket连接建立但无法获取userId: sessionId={}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到WebSocket消息: sessionId={}, payload={}", session.getId(), payload);

        // 心跳响应
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            SESSION_MAP.remove(userId);
            log.info("WebSocket连接关闭: userId={}, status={}, 当前在线数={}", userId, status, SESSION_MAP.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = extractUserId(session);
        log.error("WebSocket传输错误: userId={}", userId, exception);
        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 向所有连接的客户端广播消息
     *
     * @param message 消息内容
     */
    public static void broadcastMessage(String message) {
        log.info("广播消息: 在线用户数={}", SESSION_MAP.size());
        SESSION_MAP.forEach((userId, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("广播消息失败: userId={}", userId, e);
            }
        });
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    public static void sendMessageToUser(String userId, String message) {
        WebSocketSession session = SESSION_MAP.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("发送消息给用户: userId={}", userId);
            } catch (IOException e) {
                log.error("发送消息给用户失败: userId={}", userId, e);
            }
        } else {
            log.warn("用户不在线，无法发送消息: userId={}", userId);
        }
    }

    /**
     * 获取当前在线用户数
     *
     * @return 在线用户数
     */
    public static int getOnlineCount() {
        return SESSION_MAP.size();
    }

    /**
     * 从 WebSocketSession 中提取用户ID
     *
     * @param session WebSocket会话
     * @return 用户ID
     */
    private String extractUserId(WebSocketSession session) {
        // 从URI查询参数中获取userId
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("userId".equals(kv[0]) && kv.length == 2) {
                    return kv[1];
                }
            }
        }
        return null;
    }
}
