package com.aics.notify.config;

import com.aics.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * STOMP CONNECT 鉴权：从 Bearer JWT 建立 Principal，供 user destination 路由。
 */
@Component
@RequiredArgsConstructor
public class StompConnectAuthInterceptor implements ChannelInterceptor {

    @Value("${aics.security.jwt-secret:aics-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256}")
    private String jwtSecret;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new IllegalArgumentException("STOMP CONNECT 缺少 Bearer Token");
            }
            String token = authorization.substring("Bearer ".length());
            if (!JwtUtil.validateToken(token, jwtSecret)) {
                throw new IllegalArgumentException("STOMP CONNECT Token 无效或已过期");
            }
            String userId = JwtUtil.getSubject(token, jwtSecret);
            accessor.setUser((Principal) () -> userId);
        }
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}
