package com.aics.notify.config;

import com.aics.notify.websocket.NotifyWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置 —— 注册实时通知通道。
 *
 * <h3>学习要点（技术：WebSocket / 实时推送）</h3>
 * <ul>
 *   <li><b>为什么用 WebSocket</b>：HTTP 是"请求-响应"，服务端无法主动推；
 *       WebSocket 建立长连接后双向通信，适合订单状态变更等实时通知。</li>
 *   <li><b>与 SSE 的对比</b>：SSE 只支持服务端→客户端单向（AI 流式回复用它）；
 *       WebSocket 双向（客服主动消息、通知用它）。</li>
 *   <li><b>Handler</b>：连接建立/关闭/收发消息由 {@link NotifyWebSocketHandler} 管理。</li>
 * </ul>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 旧 /ws/notify 保留一版灰度期；新客户端应使用 STOMP /ws-stomp。
        // 禁止 "*"：跨站连接可携带伪造 userId 查询参数，生产须显式白名单。
        registry.addHandler(new NotifyWebSocketHandler(), "/ws/notify")
                .setAllowedOriginPatterns("${AICS_ALLOWED_ORIGINS:http://localhost:5173}");
    }
}
