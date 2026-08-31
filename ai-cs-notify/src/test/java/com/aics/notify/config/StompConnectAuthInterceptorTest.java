package com.aics.notify.config;

import com.aics.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CONNECT 鉴权测试：仅合法 JWT 可建立 Principal，防止 URL userId 伪造。
 */
class StompConnectAuthInterceptorTest {

    private static final String SECRET = "aics-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    private StompConnectAuthInterceptor interceptor() {
        StompConnectAuthInterceptor interceptor = new StompConnectAuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtSecret", SECRET);
        return interceptor;
    }

    @Test
    void validBearerTokenShouldCreateUserPrincipal() {
        String token = JwtUtil.generateToken("42", Map.of("username", "alice"), SECRET, 60_000L);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<?> result = interceptor().preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null);
        assertEquals("42", StompHeaderAccessor.wrap(result).getUser().getName());
    }

    @Test
    void missingTokenShouldRejectConnect() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        assertThrows(IllegalArgumentException.class,
                () -> interceptor().preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null));
    }
}
