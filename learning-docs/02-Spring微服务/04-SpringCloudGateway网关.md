# Spring Cloud Gateway 网关

> 本项目使用 **Spring Cloud Gateway** 作为统一 API 入口（端口 8080）。
> 对应项目文件：`ai-cs-gateway/` 模块（WebFlux 响应式技术栈——pom 里排除了 `spring-boot-starter-web`，网关基于 Netty，不能与 Spring MVC 共存）。

核心源码一览：

```
ai-cs-gateway/src/main/java/com/aics/gateway/
├── config/
│   ├── RouteConfig.java          # Java DSL 路由 + 每路由三层韧性（限流/熔断/重试）
│   ├── CorsConfig.java           # 全局跨域（CorsWebFilter Bean）
│   ├── LoadBalancerConfig.java   # 负载均衡算法切换（轮询 / 最少连接）
│   └── RateLimitProperties.java  # aics.gateway.rate-limit.* 动态配置
├── filter/
│   ├── AuthFilter.java           # JWT + API Key 双凭证鉴权、身份可信透传
│   ├── RateLimitFilter.java      # 旧内存限流（默认关闭，Redis 故障时兜底）
│   ├── SlidingWindowRateLimiter.java
│   └── TokenBucketRateLimiter.java
├── loadbalancer/
│   ├── LeastConnectionsLoadBalancer.java
│   ├── InstanceInFlightFilter.java   # 实例在途请求统计（配合最少连接）
│   └── InstanceInFlightRegistry.java
└── controller/
    ├── GatewayFallbackController.java  # 断路器统一降级端点
    └── HealthController.java           # /api/health 服务健康面板
```

---

## 一、网关解决什么问题？

```
【没有网关】前端要对接 N 个服务（每个服务 Controller 前缀还不一样）
  登录 → localhost:8081/user/login
  对话 → localhost:8083/chat/send
  下单 → localhost:8087/order/create
  商品 → localhost:8088/product/**

【有网关】前端只对接一个地址，统一走 /api 前缀
  所有 → localhost:8080/api/xxx → 网关按路由规则转发
```

网关的核心职责（本项目全部落地）：

1. **路由转发**：按路径把请求分发到对应服务（`RouteConfig`，见第四节）
2. **统一鉴权**：JWT（人）+ API Key（机器）双凭证校验，不合法直接 401（`AuthFilter`）
3. **身份可信透传**：剥离客户端伪造的 `X-User-Id`，注入网关验证过的可信身份
4. **跨域处理**：统一 CORS（`CorsConfig`）
5. **分布式限流**：Redis + Lua 令牌桶，多实例共享配额（`RequestRateLimiter`）
6. **熔断降级**：下游挂了返回统一 503 格式，不透传 500（`CircuitBreaker` filter）
7. **重试**：只对幂等 GET 重试（`Retry` filter）
8. **负载均衡**：轮询 / 最少连接可配置切换（`LoadBalancerConfig`）
9. **可观测**：Actuator + Prometheus 指标 + OTLP 链路追踪导出

---

## 二、本项目网关配置详解

```yaml
# ai-cs-gateway/src/main/resources/application.yml（节选，注释有删减）
server:
  shutdown: graceful                    # 优雅停机：等在途请求处理完再下线
  port: 8080

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
  application:
    name: ai-cs-gateway
  data:
    # Redis 供 RequestRateLimiter（分布式令牌桶）使用
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: aics
      config:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        namespace: aics
        file-extension: yml
  config:
    import:                             # 从 Nacos 拉公共配置与网关专属配置
      - optional:nacos:aics-shared.yml
      - optional:nacos:ai-cs-gateway.yml

# ===== 网关能力配置 =====
aics:
  gateway:
    rate-limit:
      enabled: false          # 旧内存限流开关，默认关闭（多实例配额不共享）
      algorithm: sliding-window
      requests: 60
      window-seconds: 60
      qps: 5
      replenish-rate: 5       # 分布式限流：每用户每秒补充令牌数
      burst-capacity: 10      # 分布式限流：桶容量（短时突发上限）
    auth:
      api-keys: ""            # API Key 白名单（keyId:secret,逗号分隔），空=仅 JWT
    loadbalancer:
      algorithm: round-robin  # round-robin | least-connections

# ===== 全链路追踪：Span 经 OTel Collector 导出至 Tempo =====
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING:1.0}
  otlp:
    tracing:
      endpoint: ${OTLP_ENDPOINT:http://127.0.0.1:4318/v1/traces}
```

**注意**：路由规则和 CORS 都**不在 yml 里配置**——路由在 `RouteConfig`（Java DSL），CORS 在 `CorsConfig`（Java Bean）。也没有开启 `discovery.locator` 自动路由（原因见第四节）。

---

## 三、Gateway 核心概念

```
┌─────────────────────────────────────────────────┐
│                   Gateway                        │
│                                                  │
│  Route（路由）                                    │
│  ├── Predicate（断言）：匹配条件                   │
│  │   • Path=/api/user/**                        │
│  │   • Method=GET                               │
│  │   • Header=X-Token, \d+                      │
│  │                                              │
│  ├── Filter（过滤器）：处理请求/响应               │
│  │   • StripPrefix（剥离路径前缀）                │
│  │   • RewritePath（正则重写路径）                │
│  │   • RequestRateLimiter（分布式限流）           │
│  │   • CircuitBreaker（断路器）                  │
│  │                                              │
│  └── URI（目标地址）                              │
│      • lb://ai-cs-user  （从 Nacos 负载均衡）     │
│      • http://localhost:8081（直连，少用）        │
└─────────────────────────────────────────────────┘
```

两类过滤器的区别：

|       | GlobalFilter                                            | GatewayFilter                                             |
| ----- | ------------------------------------------------------- | --------------------------------------------------------- |
| 作用范围  | **所有路由**                                                | **单条路由**                                                  |
| 注册方式  | `@Component`                                            | 路由定义里 `.filters(...)`                                     |
| 本项目例子 | `AuthFilter`、`RateLimitFilter`、`InstanceInFlightFilter` | retry / circuitBreaker / requestRateLimiter / stripPrefix |

---

## 四、路由配置：RouteConfig（Java DSL）

### 4.1 为什么不用 discovery.locator 自动路由？

`discovery.locator.enabled=true` 能按服务名自动建路由（`/ai-cs-chat/**` → ai-cs-chat），
但**每条路由无法单独挂过滤器**（限流/熔断/重试）、前缀规则也只能一刀切。
本项目改用 Java DSL 显式声明 16 条路由，每条路由都通过统一的 `addResilience` 挂上三层韧性。

### 4.2 路由定义（节选自 RouteConfig）

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                       RedisRateLimiter redisRateLimiter,
                                       KeyResolver userKeyResolver) {
    return builder.routes()
            // 模式一：stripPrefix(1) —— 去掉 /api 前缀再转发
            // 下游 UserController 映射为 /user/**，所以 /api/user/** → /user/**
            .route("ai-cs-user", r -> r
                    .path("/api/user/**")
                    .filters(f -> addResilience(f.stripPrefix(1), "cb-user", redisRateLimiter, userKeyResolver))
                    .uri("lb://ai-cs-user"))
            // 模式二：透传 —— 下游 Controller 本来就映射 /api/xxx，不去前缀
            .route("ai-cs-message", r -> r
                    .path("/api/message/**")
                    .filters(f -> addResilience(f, "cb-message", redisRateLimiter, userKeyResolver))
                    .uri("lb://ai-cs-message"))
            // 模式三：rewritePath —— 正则重写（Agent 接口在 chat 服务内是 /chat/agent/**）
            .route("ai-cs-agent", r -> r
                    .path("/api/agent/**")
                    .filters(f -> addResilience(
                            f.rewritePath("/api/agent/(?<segment>.*)", "/chat/agent/${segment}"),
                            "cb-agent", redisRateLimiter, userKeyResolver))
                    .uri("lb://ai-cs-chat"))
            .build();
}
```

### 4.3 全量路由表（16 条）

| 路由 id               | 网关路径                  | 目标服务                 | 前缀处理           | 转发后路径                 |
| ------------------- | --------------------- | -------------------- | -------------- | --------------------- |
| ai-cs-user          | /api/user/**          | lb://ai-cs-user      | stripPrefix(1) | /user/**              |
| ai-cs-knowledge     | /api/knowledge/**     | lb://ai-cs-knowledge | stripPrefix(1) | /knowledge/**         |
| ai-cs-rag           | /api/rag/**           | lb://ai-cs-chat      | stripPrefix(1) | /rag/**               |
| ai-cs-chat          | /api/chat/**          | lb://ai-cs-chat      | stripPrefix(1) | /chat/**              |
| ai-cs-observability | /api/observability/** | lb://ai-cs-chat      | 透传             | /api/observability/** |
| ai-cs-prompt        | /api/prompts/**       | lb://ai-cs-chat      | 透传             | /api/prompts/**       |
| ai-cs-agent-chat    | /api/agent/chat       | lb://ai-cs-chat      | rewritePath    | /chat/agent           |
| ai-cs-agent         | /api/agent/**         | lb://ai-cs-chat      | rewritePath    | /chat/agent/**        |
| ai-cs-search        | /api/search/**        | lb://ai-cs-search    | stripPrefix(1) | /search/**            |
| ai-cs-message       | /api/message/**       | lb://ai-cs-message   | 透传             | /api/message/**       |
| ai-cs-notify        | /api/notify/**        | lb://ai-cs-notify    | 透传             | /api/notify/**        |
| ai-cs-order         | /api/order/**         | lb://ai-cs-order     | stripPrefix(1) | /order/**             |
| ai-cs-cart          | /api/cart/**          | lb://ai-cs-order     | stripPrefix(1) | /cart/**              |
| ai-cs-pay           | /api/pay/**           | lb://ai-cs-pay       | stripPrefix(1) | /pay/**               |
| ai-cs-mq            | /api/mq/**            | lb://ai-cs-mq        | stripPrefix(1) | /mq/**                |
| ai-cs-product       | /api/product/**       | lb://ai-cs-product   | stripPrefix(1) | /product/**           |

学习点：**rag / cart 两条路由证明"路由 ≠ 服务"**——路径前缀按业务划分，目标服务按部署划分，
一条路径前缀可以指向任意服务（RAG 归 chat 服务托管、购物车归 order 服务托管）。

### 4.4 每条路由的统一韧性：addResilience

```java
/** 给单条路由叠加三层韧性：GET 重试 → 断路器 → Redis 分布式限流。 */
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
            });
    return f;
}

/** 限流键：优先可信用户ID（AuthFilter 注入的 X-User-Id），未认证退化为客户端 IP。 */
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return Mono.just("u:" + userId);
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        String ip = remote == null ? "unknown" : remote.getAddress().getHostAddress();
        return Mono.just("ip:" + ip);
    };
}
```

---

## 五、全局过滤器：JWT + API Key 双凭证鉴权（AuthFilter）

### 5.1 为什么是"双凭证"？

|      | JWT（Bearer Token）               | API Key（X-API-Key 头）                |
| ---- | ------------------------------- | ----------------------------------- |
| 面向对象 | **人**（浏览器会话）                    | **机器**（第三方系统、定时任务、内部脚本）             |
| 获取方式 | 登录后签发                           | 预共享密钥（配置中心下发）                       |
| 格式   | `Authorization: Bearer <token>` | `X-API-Key: <keyId>:<secret>`       |
| 透传身份 | JWT subject → X-User-Id         | keyId → X-User-Id，角色固定 ROLE_SERVICE |

机器没有登录态，走预共享密钥；校验通过后同样注入 `X-User-Id`，
下游的权限与限流逻辑对"人/机器"两种来源**完全无感知**。

### 5.2 过滤器核心逻辑（节选）

```java
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    /** JWT 验签密钥（Nacos aics-shared.yml 下发，需与 user 服务签发密钥一致） */
    @Value("${aics.jwt.secret:...}")
    private String jwtSecret;

    /** 白名单（不需要认证），前缀匹配 */
    private static final List<String> WHITE_LIST = List.of(
            "/user/login", "/user/register", "/user/captcha",          // 直连下游用
            "/api/user/login", "/api/user/register", "/api/user/captcha", // 走网关用
            "/api/health", "/health",
            "/doc.html", "/webjars/", "/v3/api-docs", "/swagger-resources");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单放行——但先剥掉客户端伪造的身份头（防注入下游）
        if (isWhiteListed(path)) {
            return chain.filter(exchange.mutate()
                    .request(stripIdentityHeaders(exchange.getRequest())).build());
        }

        // 2. 提取并校验 JWT
        String token = extractToken(exchange.getRequest());
        if (token != null && JwtUtil.validateToken(token, jwtSecret)) {
            Claims claims = JwtUtil.parseToken(token, jwtSecret);
            // 3. 先移除伪造头，再注入可信身份（3.2 F2：下游只信任网关透传的身份）
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .headers(h -> { h.remove("X-User-Id"); h.remove("X-User-Name"); h.remove("X-User-Roles"); })
                    .header("X-User-Id", JwtUtil.getSubject(token, jwtSecret))
                    .header("X-User-Name", String.valueOf(claims.get("username")))
                    .header("X-User-Roles", normalizeRole(claims.get("role")))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        // 4. 无 Token / Token 无效 → 回退 API Key 认证（机器调用）
        Mono<Void> apiKeyResult = tryApiKeyAuth(exchange, chain);
        if (apiKeyResult != null) {
            return apiKeyResult;
        }
        return unauthorized(exchange.getResponse(), "未认证，请先登录");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;  // 最先执行；限流过滤器 +150 跟在后面
    }
}
```

### 5.3 API Key 认证的三个安全细节

1. **常量时间比较**：secret 用 `MessageDigest.isEqual()` 比对，防止时序侧信道逐字节猜测；
2. **密钥不透传**：校验通过后移除原始 `X-API-Key` 头，密钥不会流向下游；
3. **身份注入对齐 JWT 路径**：`X-User-Id = keyId`、`X-User-Roles = ROLE_SERVICE`，下游无感知。

### 5.4 下游服务获取用户身份

```java
// 订单服务 OrderController：必填——没有网关注入的身份头直接 400
@GetMapping("/list")
public Result<List<OrderVO>> listOrders(@RequestHeader("X-User-Id") Long userId) {
    return Result.success(orderService.listOrders(userId));
}

// 对话服务 ChatController：可空——匿名也能对话，但订单查询工具拿不到用户
@PostMapping("/send")
public Result<String> chat(@RequestHeader(value = "X-User-Id", required = false) Long userId, ...) {
    ChatUserContext.setUserId(userId);   // ThreadLocal，供 @Tool 方法（订单查询）读取
    ...
}
```

完整身份链路：**网关 AuthFilter 校验 → 注入 X-User-Id → Controller 读请求头 → ThreadLocal → @Tool 工具按用户取数（数据权限）**。

---

## 六、跨域配置（CORS）

### 为什么需要？

```
前端: http://localhost:5173  （Vite 开发服务器）
后端: http://localhost:8080  （Gateway）
浏览器同源策略：端口不同 → 跨域 → 浏览器拦截响应
```

### 本项目的解决方式（CorsConfig Bean）

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));   // 注意：不是 setAllowedOrigins！
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);                          // 预检请求缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
```

**学习点：`allowedOriginPatterns` vs `allowedOrigins`**
`allowCredentials(true)`（允许携带 Cookie）与 `allowedOrigins("*")` **不能同时配置**——
CORS 规范禁止"通配来源 + 携带凭证"组合，Spring 会直接抛异常。`allowedOriginPatterns("*")`
会在响应时把 `*` 回写成请求的实际 Origin，是官方提供的折中方案。

### 生产环境建议

把 `*` 收紧为真实前端域名列表（如 `https://your-domain.com`），方法收紧为实际用到的 GET/POST/PUT/DELETE。

---

## 七、负载均衡：轮询 vs 最少连接

`lb://` 背后是 Spring Cloud LoadBalancer。本项目支持两种算法，
通过 `aics.gateway.loadbalancer.algorithm` 一行配置切换。

```java
@Configuration
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfig.LeastConnectionsConfiguration.class)
public class LoadBalancerConfig {

    public static class LeastConnectionsConfiguration {
        @Bean
        @ConditionalOnProperty(name = "aics.gateway.loadbalancer.algorithm",
                               havingValue = "least-connections")
        public ReactorLoadBalancer<ServiceInstance> leastConnectionsLoadBalancer(...) {
            return new LeastConnectionsLoadBalancer(supplierProvider, serviceId, registry);
        }
    }
}
```

**学习点：LoadBalancer 子上下文**
LoadBalancer 为每个下游服务创建独立子上下文；内置 `RoundRobinLoadBalancer` 带
`@ConditionalOnMissingBean`——上面的配置类在每个子上下文里实例化，算法配置为
`least-connections` 时注册自定义均衡器（默认轮询自动退位），否则不产生 Bean 回退轮询。
"配置切换算法、不改框架源码"就是靠这两层条件装配实现的。

### 最少连接（Least Connections）的原理

```
轮询：     请求1→A  请求2→B  请求3→A  请求4→B   （不管每个请求处理多久）
最少连接：  查每个实例"正在处理的请求数"，把新请求分给最闲的
```

轮询假设"每个请求耗时相近"——AI 客服场景不成立：LLM 对话耗时从几百毫秒到几十秒不等，
慢请求会在轮询下持续砸向同一批实例。最少连接按在途请求数分流，耗时方差大时负载更均衡。

- **在途计数从哪来**：`InstanceInFlightFilter`（GlobalFilter）在负载均衡器选定实例后 +1，
  响应终结时（`doFinally`，覆盖正常完成/异常/客户端断开三种路径）-1，计数不泄漏；
- **为什么统计放过滤器而不是均衡器内部**：均衡器的 `choose()` 只负责"选谁"，
  拿不到请求后续生命周期；过滤器能包住 `chain.filter(exchange)` 的整个 Mono，
  是唯一能同时看到"选了谁"和"什么时候结束"的位置；
- **并列最少怎么办**：`LeastConnectionsLoadBalancer` 找出所有在途数并列最小的实例，
  用 `AtomicInteger` 轮询取一个，避免流量集中到"恰好最少"的同一实例。

---

## 八、网关三层韧性：分布式限流 / 断路器 / 重试

### 8.1 分布式限流：RequestRateLimiter（Redis + Lua 令牌桶）

旧实现 `RateLimitFilter` 是**本地内存**限流——网关起两个实例，配额各算各的，全局限流形同虚设。
新方案用 SCG 内置 `RequestRateLimiter`：

```
每请求 → Lua 脚本在 Redis 原子执行令牌桶扣减（多实例共享一份计数）→ 超限 429
```

- 自动装配的 `redisRateLimiter` Bean 带 `@ConditionalOnMissingBean`，
  `RouteConfig` 定义同名 Bean（速率取 `RateLimitProperties` 的 replenish-rate/burst-capacity）即接管；
- `RateLimitProperties` 用 `@ConfigurationProperties`：Nacos 改值触发自动重绑定，**免重启**；
- 键策略 = 可信用户优先（`userKeyResolver`），与旧实现一致，迁移前后限流粒度不变；
- Redis 不可用时：把 `aics.gateway.rate-limit.enabled` 改回 `true` 切回内存限流兜底
  （`RateLimitFilter` 保留未删，支持 sliding-window / token-bucket 两种算法）。

### 8.2 断路器：CircuitBreaker filter（按路由独立命名）

下游挂掉时网关不再透传 500，而是熔断并 forward 到统一降级端点：

```java
.circuitBreaker(c -> c.setName("cb-user").setFallbackUri("forward:/gateway-fallback"))
```

- **为什么每条路由独立命名**：断路器实例按 name 隔离，共享 name 会让一个服务的
  失败统计污染所有路由（一个服务挂 → 全站熔断）；
- 降级端点 `GatewayFallbackController` 返回统一 `Result` 结构的 503
  （`GATEWAY_SERVICE_UNAVAILABLE`）——前端拿到可识别的业务响应格式而非裸错误页；
  forward 是网关内部跳转，不占用下游资源；
- 依赖：`spring-cloud-starter-circuitbreaker-reactor-resilience4j`。

### 8.3 重试：只对幂等 GET

```java
f.retry(c -> c.setRetries(2).setMethods(HttpMethod.GET)
        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY))
```

**POST/PUT 绝不在网关层重试**——重复下单/重复扣款是非幂等灾难；
写路径的容错交给调用方（Feign fallback + 熔断）。

### 8.4 Java DSL 的一个坑

`RouteSpec.filters(...)` 的参数是 `UnaryOperator<GatewayFilterSpec>`（要返回值），
不是 `Consumer`——lambda 里必须 `return`，多个 filter 用
`addResilience(f.stripPrefix(1), ...)` 链式组合而非语句块。

---

## 九、Gateway vs Nginx

| 对比   | Gateway        | Nginx   |
| ---- | -------------- | ------- |
| 层级   | 应用层（Java）      | 网络层（C）  |
| 性能   | 较高             | 极高      |
| 动态路由 | 支持（从 Nacos 发现） | 需手动配置   |
| 业务逻辑 | 可以写 Java 过滤器   | 只能用 Lua |
| 适用场景 | 微服务内部网关        | 最外层反向代理 |

**生产架构**：Nginx（最外层）→ Gateway（微服务网关）→ 各服务

---

## 十、动手练习

前置：启动 Nacos、Redis，然后启动 Gateway（8080）+ Chat 服务（8083）。

1. **直连下游**：`curl -X POST "http://localhost:8083/chat/send?sessionId=1&message=hi"`
   （注意是 `/chat/send`，没有 `/api` 前缀）
2. **通过网关**：先登录拿 Token
   `curl -X POST http://localhost:8080/api/user/login -H "Content-Type: application/json" -d '{...}'`
   再带 Token 访问
   `curl -X POST "http://localhost:8080/api/chat/send?sessionId=1&message=hi" -H "Authorization: Bearer <token>"`
3. 对比两种方式的响应（应该一样）——同时体会 `stripPrefix(1)` 把 `/api/chat/send` 变成了 `/chat/send`
4. **观察 401**：不带 Token 访问 `/api/chat/send`，应返回 `{"code":401,...}`
5. **观察身份防伪造**：带上伪造头 `X-User-Id: 999` 再请求，网关会剥除它，
   下游拿不到伪造身份（可看 chat 服务日志验证）
6. **观察 429**：短时间连发超过 `burst-capacity`（10）个请求，观察限流响应
7. **观察 503 降级**：停掉 Chat 服务再请求，应收到统一格式的
   `GATEWAY_SERVICE_UNAVAILABLE` 响应（而不是连接拒绝/500）

---

## 学习检查清单

- [ ] 理解网关的核心职责（路由/鉴权/跨域/限流/熔断/重试/负载均衡）
- [ ] 理解 Route = Predicate + Filter + URI，以及 GlobalFilter 与 GatewayFilter 的区别
- [ ] 会用 Java DSL 配置路由，说得出 stripPrefix / 透传 / rewritePath 三种前缀处理模式的适用场景
- [ ] 理解为什么本项目不用 discovery.locator 自动路由（每条路由要独立挂韧性过滤器）
- [ ] 会写全局过滤器做 JWT 鉴权，理解 API Key 的机器调用场景
- [ ] 理解"先剥除伪造身份头、再注入可信身份"的透传原则
- [ ] 理解 CORS 跨域的原因，以及 allowedOriginPatterns 与 allowedOrigins 的区别
- [ ] 理解 `lb://` 负载均衡的含义，说得清轮询 vs 最少连接的适用场景（AI 对话耗时方差大）
- [ ] 说得清本地内存限流 vs Redis 分布式限流的差异
- [ ] 理解断路器按路由命名的必要性（失败统计隔离）
- [ ] 记住网关重试只对幂等 GET 的原因

---

## 下一步

→ [03-数据库与ORM/01-MySQL核心知识](../03-数据库与ORM/01-MySQL核心知识.md)
