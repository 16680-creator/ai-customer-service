package com.aics.notify.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocketConfig 单元测试（注册 /ws/notify 端点）
 */
class WebSocketConfigTest {

    @Test
    @DisplayName("registerWebSocketHandlers - 注册 /ws/notify 处理器")
    void registerWebSocketHandlers_shouldRegister() {
        WebSocketConfig config = new WebSocketConfig();
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        when(registry.addHandler(any(), any())).thenReturn(mock(WebSocketHandlerRegistration.class));

        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(any(), eq("/ws/notify"));
    }
}
