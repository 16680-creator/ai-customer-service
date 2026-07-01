package com.aics.gateway.filter;

import com.aics.common.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.List;

/**
 * 网关认证过滤器
 * 校验 JWT Token，将用户信息透传至下游服务
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    /** 白名单路径（不需要认证） */
    private static final List<String> WHITE_LIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/user/captcha",
            "/doc.html",
            "/webjars/",
            "/v3/api-docs",
            "/swagger-resources"
    );

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_NAME_HEADER = "X-User-Name";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 获取 Token
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            log.warn("请求未携带Token: {}", path);
            return unauthorized(exchange.getResponse(), "未认证，请先登录");
        }

        // 校验 Token
        if (!JwtUtil.validateToken(token)) {
            log.warn("Token无效或已过期: {}", path);
            return unauthorized(exchange.getResponse(), "Token无效或已过期");
        }

        // 解析用户信息并透传至下游
        try {
            String userId = JwtUtil.getSubject(token);
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(USER_ID_HEADER, userId)
                    .header(USER_NAME_HEADER, String.valueOf(JwtUtil.parseToken(token).get("username")))
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.error("Token解析异常: {}", e.getMessage());
            return unauthorized(exchange.getResponse(), "Token解析失败");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 从请求头中提取 Token
     */
    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 返回 401 未认证响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
