package com.aics.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 网关限流过滤器测试（3.2 网关限流）：窗口内超限返回 429 且不转发下游。
 *
 * <p>注：MockServerWebExchange 放行时默认状态码为 null，故"放行"断言以转发计数为准。</p>
 */
class RateLimitFilterTest {

    private RateLimitFilter filter;
    private AtomicInteger forwarded;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        forwarded = new AtomicInteger();
    }

    private MockServerWebExchange newExchange(String userId) {
        MockServerHttpRequest.BodyBuilder builder = MockServerHttpRequest
                .method(HttpMethod.POST, "/chat/stream/sse");
        if (userId != null) {
            builder.header("X-User-Id", userId);
        }
        return MockServerWebExchange.from(builder);
    }

    private GatewayFilterChain countingChain() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            forwarded.incrementAndGet();
            return Mono.empty();
        });
        return chain;
    }

    @Test
    void 超限返回429_不转发下游() {
        ReflectionTestUtils.setField(filter, "requests", 2);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60);
        ReflectionTestUtils.setField(filter, "enabled", true);

        // 前 2 次放行（转发下游）
        filter.filter(newExchange("1"), countingChain()).block();
        assertEquals(1, forwarded.get());
        filter.filter(newExchange("1"), countingChain()).block();
        assertEquals(2, forwarded.get());

        // 第 3 次触发限流：429 且不转发
        MockServerWebExchange third = newExchange("1");
        filter.filter(third, countingChain()).block();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, third.getResponse().getStatusCode());
        assertEquals(2, forwarded.get(), "超限请求不得转发下游");
    }

    @Test
    void 不同用户独立限流() {
        ReflectionTestUtils.setField(filter, "requests", 1);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60);
        ReflectionTestUtils.setField(filter, "enabled", true);

        filter.filter(newExchange("1"), countingChain()).block();
        assertEquals(1, forwarded.get());

        // 用户 2 不受用户 1 的窗口影响
        filter.filter(newExchange("2"), countingChain()).block();
        assertEquals(2, forwarded.get());
    }

    @Test
    void 关闭限流时全部放行() {
        ReflectionTestUtils.setField(filter, "requests", 1);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60);
        ReflectionTestUtils.setField(filter, "enabled", false);

        for (int i = 0; i < 3; i++) {
            filter.filter(newExchange("1"), countingChain()).block();
        }
        assertEquals(3, forwarded.get());
    }
}
