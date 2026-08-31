package com.aics.gateway.filter;

import com.aics.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 网关认证过滤器测试（3.2 F2 身份可信透传）：
 * 客户端伪造的 X-User-Id/X-User-Name 必须在透传前被移除，下游只收到 JWT 解析的可信身份。
 */
class AuthFilterTest {

    private static final String SECRET = "aics-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    private AuthFilter newFilter() {
        AuthFilter filter = new AuthFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
        return filter;
    }

    @Test
    void 伪造身份头被移除_仅透传可信身份() {
        // 客户端携带伪造身份头 + 合法 JWT（userId=1）
        String token = JwtUtil.generateToken("1", Map.of("username", "zhangsan", "role", "admin"), SECRET, 60_000L);
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/chat/stream/sse")
                .header("Authorization", "Bearer " + token)
                .header("X-User-Id", "999")       // 伪造
                .header("X-User-Name", "hacker")  // 伪造
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            // 注意：exchange.mutate() 返回 MutativeDecorator，不能强转为 MockServerWebExchange
            ServerWebExchange mutated = invocation.getArgument(0, ServerWebExchange.class);
            HttpHeaders headers = mutated.getRequest().getHeaders();
            // 下游只收到可信身份，且伪造值被移除
            assertEquals(List.of("1"), headers.get("X-User-Id"), "伪造的 X-User-Id 必须被移除");
            assertEquals(List.of("zhangsan"), headers.get("X-User-Name"), "伪造的 X-User-Name 必须被移除");
            assertEquals(List.of("ROLE_ADMIN"), headers.get("X-User-Roles"), "角色必须从 JWT 可信透传并标准化");
            return Mono.empty();
        });

        newFilter().filter(exchange, chain).block();
        // 透传成功 = 下游收到可信身份（上方断言已校验），MockServerWebExchange 未设置状态码属正常
        verify(chain).filter(any());
    }

    @Test
    void 未携带Token_返回401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/chat/history")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        newFilter().filter(exchange, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void 白名单路径放行_无Token也通过() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/user/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        newFilter().filter(exchange, chain).block();
        // 白名单放行 = 转发下游（MockServerWebExchange 默认状态码为 null）
        verify(chain).filter(any());
    }

    @Test
    void 白名单路径_客户端伪造身份头被移除() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/user/login")
                .header("X-User-Id", "999")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange mutated = invocation.getArgument(0, ServerWebExchange.class);
            assertNull(mutated.getRequest().getHeaders().getFirst("X-User-Id"),
                    "白名单路径也不得透传客户端伪造身份头");
            return Mono.empty();
        });

        newFilter().filter(exchange, chain).block();
    }

    // ===== API Key 认证（机器调用场景）：keyId:secret 校验 + 身份透传 =====

    private AuthFilter newFilterWithApiKeys(String apiKeysConfig) {
        AuthFilter filter = newFilter();
        ReflectionTestUtils.setField(filter, "apiKeysConfig", apiKeysConfig);
        filter.initApiKeys();
        return filter;
    }

    @Test
    void 有效APIKey_注入机器身份并移除密钥头() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/chat/agent")
                .header("X-API-Key", "svc-app:sk-secret-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange mutated = invocation.getArgument(0, ServerWebExchange.class);
            HttpHeaders headers = mutated.getRequest().getHeaders();
            assertEquals(List.of("svc-app"), headers.get("X-User-Id"), "keyId 应作为身份透传下游");
            assertEquals("api-key:svc-app", headers.getFirst("X-User-Name"));
            assertNull(headers.getFirst("X-API-Key"), "密钥不得继续向下游传播");
            return Mono.empty();
        });

        newFilterWithApiKeys("svc-app:sk-secret-123,svc-job:sk-other").filter(exchange, chain).block();
        verify(chain).filter(any());
    }

    @Test
    void 错误secret_返回401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/chat/agent")
                .header("X-API-Key", "svc-app:sk-wrong")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        newFilterWithApiKeys("svc-app:sk-secret-123").filter(exchange, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void 未配置APIKey时_携带密钥头仍401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/chat/agent")
                .header("X-API-Key", "svc-app:sk-secret-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // api-keys 为空 = 关闭 API Key 认证，仅 JWT
        newFilterWithApiKeys("").filter(exchange, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
