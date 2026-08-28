package com.aics.gateway.filter;

import com.aics.common.util.JwtUtil;
import jakarta.annotation.PostConstruct;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关认证过滤器 —— 统一校验 JWT / API Key 并把用户身份透传给下游。
 *
 * <h3>学习要点（技术：JWT / API Key / 网关透传 / 用户身份）</h3>
 * <ul>
 *   <li><b>为什么在网关做鉴权</b>：一次校验、处处生效——下游微服务无需各自解析 JWT。</li>
 *   <li><b>X-User-Id 透传</b>：校验通过后把用户 ID 写入请求头转发给下游；
 *       AI 对话服务据此识别"当前用户"，订单查询等工具才能按用户取数（数据权限）。</li>
 *   <li><b>白名单</b>：登录/注册等公开接口放行，其余接口无有效凭证直接 401。</li>
 *   <li><b>双凭证体系</b>：JWT（Bearer Token）面向"人"的会话请求；API Key（X-API-Key 头）
 *       面向"系统/服务"的机器调用（第三方接入、定时任务、内部脚本）——机器没有登录态，
 *       走预共享密钥，校验通过后同样注入 X-User-Id（keyId 即身份），
 *       使下游权限与限流逻辑对两种凭证完全一致。</li>
 * </ul>
 *
 * 校验 JWT Token / API Key，将用户信息透传至下游服务
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    /** JWT 验签密钥（来自 Nacos 配置中心 aics-shared.yml，需与 user 服务签发密钥一致） */
    @Value("${aics.jwt.secret:aics-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256}")
    private String jwtSecret;

    /**
     * API Key 白名单（格式：{@code keyId:secret}，逗号分隔，如
     * {@code svc-app:sk-xxxx,svc-job:sk-yyyy}）。空串表示关闭 API Key 认证。
     * keyId 即机器身份，校验通过后作为 X-User-Id 透传下游。
     */
    @Value("${aics.gateway.auth.api-keys:}")
    private String apiKeysConfig;

    /** keyId -> secret（启动时解析一次，请求期只读） */
    private final Map<String, String> apiKeySecrets = new HashMap<>();

    @PostConstruct
    void initApiKeys() {
        if (apiKeysConfig == null || apiKeysConfig.isBlank()) {
            return;
        }
        for (String pair : apiKeysConfig.split(",")) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = trimmed.indexOf(':');
            if (idx <= 0 || idx == trimmed.length() - 1) {
                log.warn("API Key 配置格式非法（应为 keyId:secret），已忽略: {}", trimmed);
                continue;
            }
            apiKeySecrets.put(trimmed.substring(0, idx), trimmed.substring(idx + 1));
        }
        log.info("API Key 认证已启用，共加载 {} 个密钥", apiKeySecrets.size());
    }

    /** 白名单路径（不需要认证） */
    private static final List<String> WHITE_LIST = List.of(
            "/user/login",
            "/user/register",
            "/user/captcha",
            "/api/user/login",
            "/api/user/register",
            "/api/user/captcha",
            "/api/health",
            "/health",
            "/doc.html",
            "/webjars/",
            "/v3/api-docs",
            "/swagger-resources"
    );

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_NAME_HEADER = "X-User-Name";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行（同时移除客户端伪造身份头，防止注入下游；
        // 3.2 F2 身份可信透传对全路径生效：下游只信任网关注入的身份）
        if (isWhiteListed(path)) {
            ServerHttpRequest stripped = request.mutate()
                    .headers(headers -> {
                        headers.remove(USER_ID_HEADER);
                        headers.remove(USER_NAME_HEADER);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(stripped).build());
        }

        // 获取 Token
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            // 无 Bearer Token：回退 API Key 认证（机器调用场景）
            Mono<Void> apiKeyResult = tryApiKeyAuth(exchange, chain);
            if (apiKeyResult != null) {
                return apiKeyResult;
            }
            log.warn("请求未携带Token: {}", path);
            return unauthorized(exchange.getResponse(), "未认证，请先登录");
        }

        // 校验 Token
        if (!JwtUtil.validateToken(token, jwtSecret)) {
            // Token 无效：同样回退 API Key（避免误杀同时携带两种凭证的调用方）
            Mono<Void> apiKeyResult = tryApiKeyAuth(exchange, chain);
            if (apiKeyResult != null) {
                return apiKeyResult;
            }
            log.warn("Token无效或已过期: {}", path);
            return unauthorized(exchange.getResponse(), "Token无效或已过期");
        }

        // 解析用户信息并透传至下游
        try {
            String userId = JwtUtil.getSubject(token, jwtSecret);
            ServerHttpRequest mutatedRequest = request.mutate()
                    // 3.2 F2 身份可信透传：先移除客户端伪造的 X-User-Id/X-User-Name，
                    // 再注入从 JWT 解析的可信身份，下游只信任网关透传的身份头
                    .headers(headers -> {
                        headers.remove(USER_ID_HEADER);
                        headers.remove(USER_NAME_HEADER);
                    })
                    .header(USER_ID_HEADER, userId)
                    .header(USER_NAME_HEADER, String.valueOf(JwtUtil.parseToken(token, jwtSecret).get("username")))
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
     * API Key 认证回退：校验 X-API-Key 头，成功则注入机器身份并放行。
     *
     * @return 放行的 Mono；未配置 API Key / 未携带 / 校验失败时返回 null（交回 JWT 401 分支）
     */
    private Mono<Void> tryApiKeyAuth(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (apiKeySecrets.isEmpty()) {
            return null;
        }
        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        // keyId:secret 形如 "svc-app:sk-xxxx"，按第一个冒号拆分；secret 用常量时间比较防时序侧信道
        int idx = apiKey.indexOf(':');
        if (idx <= 0 || idx == apiKey.length() - 1) {
            return null;
        }
        String keyId = apiKey.substring(0, idx);
        String secret = apiKey.substring(idx + 1);
        String expected = apiKeySecrets.get(keyId);
        if (expected == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        // 学习点：校验通过后移除原始 X-API-Key（密钥不应继续向下游传播），
        // 再注入与 JWT 路径完全一致的身份头——下游对"人/机器"两种来源无感知
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(API_KEY_HEADER);
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_NAME_HEADER);
                })
                .header(USER_ID_HEADER, keyId)
                .header(USER_NAME_HEADER, "api-key:" + keyId)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
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
