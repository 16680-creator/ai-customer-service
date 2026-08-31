package com.aics.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 网关路由配置（含分布式限流 / 断路器 / 重试三层韧性）。
 *
 * <h3>学习要点（03-P3：网关韧性增强）</h3>
 * <ul>
 *   <li><b>分布式限流</b>：RequestRateLimiter 基于 Redis + Lua 令牌桶，
 *       多实例网关共享同一份配额（对比 {@code RateLimitFilter} 的本地内存实现，
 *       配额互不共享、多实例形同虚设）；键 = 可信用户ID，未认证退化为客户端 IP。</li>
 *   <li><b>断路器按路由命名</b>：CircuitBreaker 实例按 name 区分，共享 name 会让
 *       一个服务的失败统计污染所有路由（一个服务挂 → 全站熔断），所以每条路由独立命名。</li>
 *   <li><b>重试只对幂等 GET</b>：POST/PUT 重试可能造成重复下单/重复扣款，
 *       网关层只在 GET 上对 5xx 重试。</li>
 * </ul>
 */
@Configuration
public class RouteConfig {

    private final RateLimitProperties rateLimitProperties;

    public RouteConfig(RateLimitProperties rateLimitProperties) {
        this.rateLimitProperties = rateLimitProperties;
    }

    /**
     * 分布式限流器（覆盖 SCG 自动装配的同名默认 Bean，默认速率由配置驱动）。
     * 自动装配的 Bean 带 @ConditionalOnMissingBean，此处定义即接管；
     * Redis 模板/脚本由 RedisRateLimiter 自身的 ApplicationContextAware 装配。
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(rateLimitProperties.getReplenishRate(),
                rateLimitProperties.getBurstCapacity());
    }

    /**
     * 自定义路由规则
     * 注意：生产环境建议通过 Nacos 配置中心动态管理路由，此处仅作本地开发兜底
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           RedisRateLimiter redisRateLimiter,
                                           KeyResolver userKeyResolver) {
        return builder.routes()
                // 用户服务
                .route("ai-cs-user", r -> r
                        .path("/api/user/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-user", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-user"))
                // 知识库服务
                .route("ai-cs-knowledge", r -> r
                        .path("/api/knowledge/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-knowledge", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-knowledge"))
                // 向量知识库（chat 服务的 RAG 入库/检索）
                .route("ai-cs-rag", r -> r
                        .path("/api/rag/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-rag", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-chat"))
                // AI 对话服务
                .route("ai-cs-chat", r -> r
                        .path("/api/chat/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-chat", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-chat"))
                // 可观测性服务（LLM 调用链 / trace / 指标，chat 服务内部）
                // 注意：ObservabilityController 映射为 /api/observability，直接透传不去前缀
                .route("ai-cs-observability", r -> r
                        .path("/api/observability/**")
                        .filters(f -> addResilience(f, "cb-observability", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-chat"))
                // Prompt 管理服务（chat 服务内部）
                // 注意：PromptController 映射为 /api/prompts，直接透传不去前缀
                .route("ai-cs-prompt", r -> r
                        .path("/api/prompts/**")
                        .filters(f -> addResilience(f, "cb-prompt", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-chat"))
                // Agent 编排服务（chat 服务内部的 AgentController，映射为 /chat/agent/**）
                // 注意：对话入口是 POST /chat/agent（无 /chat 后缀），需单独重写；
                // 其余接口（confirm/runs/health）按 /api/agent/** → /chat/agent/** 重写
                .route("ai-cs-agent-chat", r -> r
                        .path("/api/agent/chat")
                        .filters(f -> addResilience(f.rewritePath("/api/agent/chat", "/chat/agent"), "cb-agent", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-chat"))
                .route("ai-cs-agent", r -> r
                        .path("/api/agent/**")
                        .filters(f -> addResilience(f.rewritePath("/api/agent/(?<segment>.*)", "/chat/agent/${segment}"), "cb-agent", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-chat"))
                // 搜索服务
                .route("ai-cs-search", r -> r
                        .path("/api/search/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-search", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-search"))
                // 消息服务（Controller 前缀为 /api/message，无需去前缀）
                .route("ai-cs-message", r -> r
                        .path("/api/message/**")
                        .filters(f -> addResilience(f, "cb-message", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-message"))
                // 通知服务（Controller 前缀为 /api/notify，无需去前缀）
                .route("ai-cs-notify", r -> r
                        .path("/api/notify/**")
                        .filters(f -> addResilience(f, "cb-notify", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-notify"))
                // 订单服务
                .route("ai-cs-order", r -> r
                        .path("/api/order/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-order", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-order"))
                // 购物车服务（订单模块）
                .route("ai-cs-cart", r -> r
                        .path("/api/cart/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-cart", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-order"))
                // 支付回调（订单模块）
                .route("ai-cs-pay", r -> r
                        .path("/api/pay/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-pay", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-pay"))
                // RocketMQ 调度服务（独立 ai-cs-mq）
                .route("ai-cs-mq", r -> r
                        .path("/api/mq/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-mq", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-mq"))
                // 商品服务
                .route("ai-cs-product", r -> r
                        .path("/api/product/**")
                        .filters(f -> addResilience(f.stripPrefix(1), "cb-product", redisRateLimiter, userKeyResolver))
                        .uri("lb://ai-cs-product"))
                .build();
    }

    /**
     * 限流键解析：优先可信用户ID（AuthFilter 注入的 X-User-Id），未认证退化为客户端 IP。
     * 与旧 RateLimitFilter 的键策略一致，保证迁移前后限流粒度不变。
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String userId = request.getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("u:" + userId);
            }
            InetSocketAddress remote = request.getRemoteAddress();
            String ip = remote == null ? "unknown" : remote.getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * 给单条路由叠加三层韧性：GET 重试 → 断路器（带统一降级响应）→ Redis 分布式限流。
     *
     * @param cbName 断路器实例名，必须按路由独立命名（共享实例会让失败统计互相污染）
     */
    private GatewayFilterSpec addResilience(GatewayFilterSpec f, String cbName,
                               RedisRateLimiter redisRateLimiter, KeyResolver userKeyResolver) {
        f.retry(c -> c.setRetries(2)
                        .setMethods(HttpMethod.GET)
                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY))
                .circuitBreaker(c -> c.setName(cbName)
                        .setFallbackUri("forward:/gateway-fallback"))
                .requestRateLimiter(c -> {
                    c.setRateLimiter(redisRateLimiter);
                    c.setKeyResolver(userKeyResolver);
                    // 超限默认响应 429 TOO_MANY_REQUESTS（RequestRateLimiter 内置默认）
                });
        return f;
    }
}
