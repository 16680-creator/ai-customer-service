package com.aics.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关限流过滤器（3.2 推荐顺序 1：限流）。
 *
 * <p>滑动窗口内存限流，维度 = 可信用户ID（认证后由 {@link AuthFilter} 注入；
 * 未认证/白名单路径退化为客户端 IP）。超限返回 429，不转发下游。</p>
 *
 * <p>配置项（{@code aics.gateway.rate-limit.*}）：enabled（默认 true）、
 * requests（默认 60）、window-seconds（默认 60）。</p>
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** 网关透传的可信用户头（与 AuthFilter 一致） */
    private static final String USER_ID_HEADER = "X-User-Id";

    @Value("${aics.gateway.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${aics.gateway.rate-limit.requests:60}")
    private int requests;

    @Value("${aics.gateway.rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 学习点：限流过滤器放在认证之后（order +150 > AuthFilter +100）——
        // 认证前客户端可伪造 X-User-Id 刷不同的限流桶，认证后只能拿到可信身份，
        // 所以优先用“可信用户ID”做限流键，未认证/白名单路径退化为客户端 IP。
        if (!enabled) {
            return chain.filter(exchange);
        }
        String key = resolveKey(exchange);
        if (!limiter.tryAcquire(key, Math.max(1, requests), Math.max(1, windowSeconds))) {
            log.warn("请求触发限流: key={}, path={}", key, exchange.getRequest().getPath());
            return tooManyRequests(exchange.getResponse());
        }
        return chain.filter(exchange);
    }

    /**
     * 限流键：优先可信用户ID（AuthFilter 注入），否则客户端 IP。
     */
    private String resolveKey(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String userId = request.getHeaders().getFirst(USER_ID_HEADER);
        if (userId != null && !userId.isBlank()) {
            return "u:" + userId;
        }
        String ip = request.getRemoteAddress() == null
                ? "unknown" : request.getRemoteAddress().getAddress().getHostAddress();
        return "ip:" + ip;
    }

    /**
     * 返回 429 限流响应
     */
    private Mono<Void> tooManyRequests(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        String body = "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null,\"timestamp\":"
                + System.currentTimeMillis() + "}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    /**
     * 在认证过滤器之后执行（优先使用可信用户ID 限流），白名单路径退化为 IP 限流。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 150;
    }
}
