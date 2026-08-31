package com.aics.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 分布式限流键解析器测试：认证用户按用户ID隔离，未认证按客户端 IP 隔离。
 */
class UserKeyResolverTest {

    private final RouteConfig config = new RouteConfig(new RateLimitProperties());

    @Test
    @DisplayName("带 X-User-Id 头 - 按可信用户ID限流")
    void keyByUserId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/user/1").header("X-User-Id", "42"));
        assertEquals("u:42", config.userKeyResolver().resolve(exchange).block());
    }

    @Test
    @DisplayName("未认证 - 按客户端 IP 限流")
    void keyByRemoteIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/user/1")
                        .remoteAddress(new InetSocketAddress("10.0.0.7", 50000)));
        assertEquals("ip:10.0.0.7", config.userKeyResolver().resolve(exchange).block());
    }

    @Test
    @DisplayName("无远端地址 - 兜底 unknown 键（不抛异常）")
    void keyFallbackWhenNoRemote() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/user/1").build());
        assertEquals("ip:unknown", config.userKeyResolver().resolve(exchange).block());
    }
}
