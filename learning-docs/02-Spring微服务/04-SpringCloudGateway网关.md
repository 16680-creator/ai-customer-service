# Spring Cloud Gateway 网关

> 本项目使用 **Spring Cloud Gateway** 作为统一 API 入口（端口 8080）。
> 对应项目文件：`ai-cs-gateway/` 模块

---

## 一、网关解决什么问题？

```
【没有网关】前端要对接 N 个服务
  登录 → localhost:8081/api/user/login
  对话 → localhost:8083/api/chat/send
  下单 → localhost:8087/api/order/create
  搜索 → localhost:8086/api/search/query

【有网关】前端只对接一个地址
  所有 → localhost:8080/xxx → 网关自动路由
```

网关的核心职责：
1. **路由转发**：根据路径把请求分发到对应服务
2. **统一鉴权**：在网关层校验 JWT，不合法直接拒绝
3. **跨域处理**：统一配置 CORS
4. **限流熔断**：保护后端服务
5. **日志监控**：统一记录请求日志

---

## 二、本项目网关配置详解

```yaml
# ai-cs-gateway/src/main/resources/application.yml
server:
  port: 8080                          # 网关端口

spring:
  application:
    name: ai-cs-gateway
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: aics
      config:
        server-addr: 127.0.0.1:8848
        namespace: aics
        file-extension: yml
    gateway:
      discovery:
        locator:
          enabled: true               # 自动根据服务名创建路由
          lower-case-service-id: true  # 服务名转小写
      globalcors:
        cors-configurations:
          '[/**]':                     # 所有路径
            allowed-origins: "*"       # 允许所有来源（开发用）
            allowed-methods: "*"       # 允许所有方法
            allowed-headers: "*"       # 允许所有请求头
            allow-credentials: true    # 允许携带 Cookie
```

### 自动路由规则

开启 `discovery.locator.enabled=true` 后：

```
请求: GET http://localhost:8080/ai-cs-chat/api/chat/send
                                 ^^^^^^^^^^
                                 服务名部分

网关处理:
  1. 识别服务名: ai-cs-chat
  2. 从 Nacos 查询 ai-cs-chat 的实例地址
  3. 转发到: http://192.168.1.10:8083/api/chat/send
```

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
│  │   • AddRequestHeader                         │
│  │   • StripPrefix                              │
│  │   • RequestRateLimiter                       │
│  │                                              │
│  └── URI（目标地址）                              │
│      • lb://ai-cs-user  （负载均衡）              │
│      • http://localhost:8081（直连）              │
└─────────────────────────────────────────────────┘
```

---

## 四、自定义路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 用户服务
        - id: user-route
          uri: lb://ai-cs-user           # lb:// = 从 Nacos 负载均衡
          predicates:
            - Path=/api/user/**          # 路径匹配
          filters:
            - StripPrefix=0              # 不剥离路径前缀

        # AI 对话服务（需要更长超时）
        - id: chat-route
          uri: lb://ai-cs-chat
          predicates:
            - Path=/api/chat/**
          metadata:
            response-timeout: 60000      # AI 回复慢，超时 60 秒
            connect-timeout: 5000

        # 订单服务（需要限流）
        - id: order-route
          uri: lb://ai-cs-order
          predicates:
            - Path=/api/order/**, /api/cart/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10   # 每秒 10 个令牌
                redis-rate-limiter.burstCapacity: 20   # 突发最多 20 个
                key-resolver: "#{@userKeyResolver}"    # 按用户限流
```

---

## 五、全局过滤器（鉴权）

本项目在网关层做统一 JWT 鉴权：

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    /** 白名单路径（不需要登录） */
    private static final Set<String> WHITE_LIST = Set.of(
        "/api/user/login",
        "/api/user/register",
        "/actuator/health",
        "/swagger-ui",
        "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Token
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少认证信息");
        }

        String token = authHeader.substring(7);

        // 3. 验证 Token
        if (!JwtUtil.validateToken(token)) {
            return unauthorized(exchange, "Token 无效或已过期");
        }

        // 4. 解析用户信息，传递给下游服务
        String userId = JwtUtil.getSubject(token);
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-User-Id", userId)  // 下游服务通过 Header 获取用户 ID
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(body.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        return -100;  // 数字越小优先级越高
    }
}
```

### 下游服务获取用户 ID

```java
// 在任意微服务的 Controller 中
@GetMapping("/api/order/list")
public Result<List<OrderVO>> getOrders(
        @RequestHeader("X-User-Id") Long userId) {  // 从网关传递的 Header 中获取
    return Result.success(orderService.getByUserId(userId));
}
```

---

## 六、跨域配置（CORS）

### 为什么需要？

```
前端: http://localhost:5173  （Vite 开发服务器）
后端: http://localhost:8080  （Gateway）

浏览器同源策略：端口不同 → 跨域 → 被拦截
```

### 本项目的解决方式

在 Gateway 统一配置（见上面的 `globalcors`），前端不需要额外处理。

### 生产环境建议

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowed-origins:
        - "https://your-domain.com"    # 只允许你的域名
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
      allowed-headers: "*"
      allow-credentials: true
      max-age: 3600                    # 预检请求缓存 1 小时
```

---

## 七、Gateway vs Nginx

| 对比 | Gateway | Nginx |
|------|---------|-------|
| 层级 | 应用层（Java） | 网络层（C） |
| 性能 | 较高 | 极高 |
| 动态路由 | 支持（从 Nacos 发现） | 需手动配置 |
| 业务逻辑 | 可以写 Java 过滤器 | 只能用 Lua |
| 适用场景 | 微服务内部网关 | 最外层反向代理 |

**生产架构**：Nginx（最外层）→ Gateway（微服务网关）→ 各服务

---

## 八、动手练习

1. 启动 Gateway + Chat 服务
2. 直接访问：`curl http://localhost:8083/api/chat/send?sessionId=1&message=hi`
3. 通过网关访问：`curl http://localhost:8080/ai-cs-chat/api/chat/send?sessionId=1&message=hi`
4. 对比两种方式的结果（应该一样）
5. 不带 Token 访问需要鉴权的接口，观察 401 返回

---

## 学习检查清单

- [ ] 理解网关的五大职责
- [ ] 理解 Route = Predicate + Filter + URI
- [ ] 会配置基于服务发现的自动路由
- [ ] 会写全局过滤器做 JWT 鉴权
- [ ] 理解 CORS 跨域问题的原因和解决方案
- [ ] 理解 `lb://` 负载均衡的含义

---

## 下一步

→ [03-数据库与ORM/01-MySQL核心知识](../03-数据库与ORM/01-MySQL核心知识.md)
