package com.aics.notify.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 通知通道：/ws-stomp CONNECT 鉴权，/user/{id}/queue/notify 用户定向推送。
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompConnectAuthInterceptor stompConnectAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("${AICS_ALLOWED_ORIGINS:http://localhost:5173}");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompConnectAuthInterceptor);
    }
}
