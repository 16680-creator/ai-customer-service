package com.aics.gateway.guardrail;

import com.aics.common.util.JwtUtil;
import com.aics.gateway.filter.AuthFilter;
import com.aics.gateway.filter.SlidingWindowRateLimiter;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 3.2 网关安全 —— BDD 步骤定义（features/gateway/ 下 2 个 Feature）：
 * 限流（SlidingWindowRateLimiter）与身份可信透传（AuthFilter 移除伪造身份头）。
 */
public class GatewayGuardrailSteps {

    private static final String SECRET = "aics-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    private SlidingWindowRateLimiter limiter;
    private int maxRequests;
    private int windowSeconds;
    private boolean limited;
    private boolean allowed;

    // ==================== F1 限流 ====================

    @Given("限流窗口为 {int} 秒内最多 {int} 次")
    public void window(int seconds, int requests) {
        windowSeconds = seconds;
        maxRequests = requests;
        limiter = new SlidingWindowRateLimiter();
    }

    @When("同一用户已发起 {int} 次请求")
    public void fireRequests(int count) {
        for (int i = 0; i < count; i++) {
            limiter.tryAcquire("u:1", maxRequests, windowSeconds);
        }
    }

    @And("该用户再次发起第 {int} 次请求")
    public void oneMoreRequest(int count) {
        limited = !limiter.tryAcquire("u:1", maxRequests, windowSeconds);
    }

    @Then("第 {int} 次请求被限流")
    public void assertLimited(int count) {
        assertEquals(true, limited, "第 " + count + " 次请求应被限流");
    }

    @And("返回 429 提示且不转发下游")
    public void assert429NotForwarded() {
        // 限流器拒绝即过滤器返回 429 且不调用下游（映射逻辑由 RateLimitFilterTest 覆盖）
        assertEquals(true, limited);
    }

    @When("用户 A 已发起 {int} 次请求")
    public void userARequests(int count) {
        for (int i = 0; i < count; i++) {
            limiter.tryAcquire("u:A", maxRequests, windowSeconds);
        }
    }

    @And("用户 B 发起第 {int} 次请求")
    public void userBRequest(int count) {
        allowed = limiter.tryAcquire("u:B", maxRequests, windowSeconds);
    }

    @Then("用户 B 请求放行")
    public void assertUserBAllowed() {
        assertEquals(true, allowed, "不同用户应独立限流");
    }

    // ==================== F2 身份可信透传 ====================

    private org.springframework.web.server.ServerWebExchange downstreamExchange;

    @Given("客户端请求头携带伪造的 {string}")
    public void spoofedHeader(String headerLine) {
        // headerLine 形如 "X-User-Id: 999"
        String[] parts = headerLine.split(":", 2);
        spoofedName = parts[0].trim();
        spoofedValue = parts[1].trim();
    }

    private String spoofedName;
    private String spoofedValue;

    @Given("网关从 JWT 解析出真实 userId={long}")
    public void jwtUserId(long userId) {
        token = JwtUtil.generateToken(String.valueOf(userId),
                Map.of("username", "zhangsan"), SECRET, 60_000L);
    }

    private String token;

    @When("网关透传身份头给下游")
    public void passThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/chat/stream/sse")
                .header("Authorization", "Bearer " + token)
                .header(spoofedName, spoofedValue)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AuthFilter filter = new AuthFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            // exchange.mutate() 返回 MutativeDecorator，不能强转为 MockServerWebExchange
            downstreamExchange = invocation.getArgument(0,
                    org.springframework.web.server.ServerWebExchange.class);
            return Mono.empty();
        });
        filter.filter(exchange, chain).block();
    }

    @Then("下游收到的 X-User-Id 仅为 {string}")
    public void downstreamUserId(String userId) {
        HttpHeaders headers = downstreamExchange.getRequest().getHeaders();
        assertEquals(List.of(userId), headers.get("X-User-Id"), "下游应只收到可信 X-User-Id");
    }

    @And("伪造的 {string} 被移除")
    public void spoofedRemoved(String value) {
        HttpHeaders headers = downstreamExchange.getRequest().getHeaders();
        assertEquals(false, headers.get("X-User-Id").contains(value), "伪造值必须被移除");
    }
}
