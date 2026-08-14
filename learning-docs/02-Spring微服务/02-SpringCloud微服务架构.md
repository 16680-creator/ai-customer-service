# Spring Cloud 微服务架构

> 本项目使用 **Spring Cloud 2023.0.1 + Spring Cloud Alibaba 2023.0.1.0** 构建微服务体系。
> 核心组件：Nacos（注册/配置中心）、Spring Cloud Gateway（网关）、OpenFeign（服务间调用）

---

## 一、为什么需要微服务？

### 单体 vs 微服务

```
【单体架构】                    【微服务架构（本项目）】
┌─────────────────┐           ┌──────┐ ┌──────┐ ┌──────┐
│  用户  订单  商品  │           │ User │ │Order │ │ Chat │ ...
│  搜索  消息  AI   │    →     └──┬───┘ └──┬───┘ └──┬───┘
│  全部在一个 JAR   │              └────────┼────────┘
└─────────────────┘                       │
                                    ┌─────▼─────┐
                                    │  Gateway   │
                                    └───────────┘
```

| 对比项 | 单体 | 微服务 |
|--------|------|--------|
| 部署 | 整体部署，改一行重启全部 | 独立部署，互不影响 |
| 开发 | 代码耦合，冲突多 | 各团队独立开发 |
| 扩展 | 只能整体扩容 | 哪个服务压力大扩哪个 |
| 复杂度 | 低 | 高（需要注册中心、网关等） |

---

## 二、本项目的微服务全景

```
                        用户浏览器
                            │
                    ┌───────▼───────┐
                    │   Gateway     │  ← 统一入口 :8080
                    │  路由 + 鉴权   │
                    └───────┬───────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
    ┌───────▼──┐    ┌──────▼───┐    ┌──────▼───┐
    │   User   │    │   Chat   │    │  Order   │  ...
    │  :8081   │    │  :8083   │    │  :8087   │
    └──────────┘    └──────────┘    └──────────┘
            │               │               │
            └───────────────┼───────────────┘
                            │
                    ┌───────▼───────┐
                    │    Nacos      │  ← 注册中心 + 配置中心
                    │    :8848      │
                    └───────────────┘
```

---

## 三、Nacos 注册中心

### 3.1 是什么？

Nacos 解决两个问题：
1. **服务注册与发现**：服务启动时告诉 Nacos "我在哪"，调用方从 Nacos 查 "它在哪"
2. **配置中心**：集中管理所有服务的配置，修改后实时推送

### 3.2 项目中的配置

```yaml
# ai-cs-gateway/src/main/resources/application.yml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848   # Nacos 地址
        namespace: aics                # 命名空间（隔离环境）
        group: DEFAULT_GROUP           # 分组
      config:
        server-addr: 127.0.0.1:8848
        namespace: aics
        group: DEFAULT_GROUP
        file-extension: yml            # 配置文件格式
```

### 3.3 工作流程

```
1. ai-cs-user 启动 → 向 Nacos 注册："我是 ai-cs-user，地址 192.168.1.10:8081"
2. ai-cs-chat 启动 → 向 Nacos 注册："我是 ai-cs-chat，地址 192.168.1.10:8083"
3. Gateway 需要转发到 user → 问 Nacos："ai-cs-user 在哪？" → 得到地址 → 转发
4. 每 30 秒发心跳，Nacos 发现服务挂了就摘除
```

### 3.4 本地开发技巧

```yaml
# 本地开发时可以关闭 Nacos（不用启动 Nacos 也能跑）
spring:
  cloud:
    nacos:
      discovery:
        enabled: false   # 关闭注册
      config:
        enabled: false   # 关闭配置拉取
```

---

## 四、Spring Cloud Gateway 网关

### 4.1 网关的作用

```
没有网关：前端要记住每个服务的端口
  用户相关 → localhost:8081
  AI对话  → localhost:8083
  订单相关 → localhost:8087

有了网关：前端只访问一个地址
  所有请求 → localhost:8080 → 网关自动路由到对应服务
```

### 4.2 本项目的网关配置

```yaml
# ai-cs-gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true              # 开启服务发现路由
          lower-case-service-id: true # 服务名转小写
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins: "*"     # 允许跨域（开发用）
            allowed-methods: "*"
            allowed-headers: "*"
            allow-credentials: true
```

### 4.3 路由规则

开启 `discovery.locator.enabled=true` 后，自动生成路由：

```
http://localhost:8080/ai-cs-user/api/user/login
                      ^^^^^^^^^^
                      服务名（Nacos 中注册的）

→ 网关自动转发到 → http://ai-cs-user实例:8081/api/user/login
```

### 4.4 自定义路由（更精细的控制）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://ai-cs-user          # lb:// 表示从 Nacos 负载均衡
          predicates:
            - Path=/api/user/**         # 匹配路径
          filters:
            - StripPrefix=0             # 不剥离前缀
            
        - id: chat-service
          uri: lb://ai-cs-chat
          predicates:
            - Path=/api/chat/**
          filters:
            - name: RequestRateLimiter  # 限流
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

### 4.5 网关过滤器（鉴权）

```java
// ai-cs-gateway/src/main/java/com/aics/gateway/filter/AuthGlobalFilter.java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Set<String> WHITE_LIST = Set.of(
        "/api/user/login",
        "/api/user/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // 白名单放行
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        
        // 检查 Token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !JwtUtil.validateToken(token.replace("Bearer ", ""))) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;  // 优先级最高
    }
}
```

---

## 五、服务间调用（OpenFeign）

### 5.1 场景

订单服务需要查询商品信息：

```
用户下单 → OrderService → 需要商品信息 → 调用 ProductService
```

### 5.2 使用 Feign 声明式调用

```java
// 在 ai-cs-order 中定义 Feign 客户端
@FeignClient(name = "ai-cs-product")  // 服务名（Nacos 中注册的）
public interface ProductFeignClient {

    @GetMapping("/api/product/{id}")
    Result<ProductVO> getProduct(@PathVariable("id") Long productId);

    @PostMapping("/api/product/deduct-stock")
    Result<Boolean> deductStock(@RequestParam Long productId,
                                @RequestParam int quantity);
}

// 在 Service 中像调用本地方法一样使用
@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private ProductFeignClient productClient;
    
    public OrderVO createOrder(Long userId, Long productId, int qty) {
        // 1. 查询商品（远程调用，但写起来像本地方法）
        Result<ProductVO> productResult = productClient.getProduct(productId);
        if (!productResult.isSuccess()) {
            throw new BusinessException("商品不存在");
        }
        
        // 2. 扣减库存
        productClient.deductStock(productId, qty);
        
        // 3. 创建订单
        // ...
    }
}
```

### 5.3 负载均衡

```
ai-cs-product 部署了 3 个实例：
  实例1: 192.168.1.10:8088
  实例2: 192.168.1.11:8088
  实例3: 192.168.1.12:8088

Feign + LoadBalancer 自动轮询：
  第1次调用 → 实例1
  第2次调用 → 实例2
  第3次调用 → 实例3
  第4次调用 → 实例1（循环）
```

---

## 六、服务容错（Sentinel / Resilience4j）

### 6.1 为什么需要容错？

```
OrderService → ProductService（挂了！）
                    ↓
            如果没有容错：
            OrderService 一直等待 → 线程池耗尽 → OrderService 也挂了
                    ↓
            雪崩效应：所有服务都挂
```

### 6.2 解决方案

```java
// 降级：ProductService 挂了，返回默认值
@FeignClient(name = "ai-cs-product", fallback = ProductFallback.class)
public interface ProductFeignClient { ... }

@Component
public class ProductFallback implements ProductFeignClient {
    @Override
    public Result<ProductVO> getProduct(Long productId) {
        return Result.fail("商品服务暂时不可用");
    }
}
```

---

## 七、配置中心实战

### 7.1 Nacos 配置管理

```
Nacos 控制台 (http://localhost:8848/nacos)
├── 命名空间: aics
│   ├── DEFAULT_GROUP
│   │   ├── ai-cs-gateway.yml    ← 网关配置
│   │   ├── ai-cs-user.yml       ← 用户服务配置
│   │   ├── ai-cs-chat.yml       ← AI 对话配置
│   │   └── ai-cs-order.yml      ← 订单配置
│   └── SHARED_GROUP
│       └── common.yml           ← 公共配置（数据库连接等）
```

### 7.2 动态刷新

```java
@RestController
@RefreshScope  // 配置变更时自动刷新这个 Bean
public class ConfigController {
    
    @Value("${ai.chat.max-tokens:2048}")
    private int maxTokens;  // Nacos 中修改后，不用重启就生效
    
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        return Result.success(Map.of("maxTokens", maxTokens));
    }
}
```

---

## 八、动手练习

1. 启动 Nacos：`docker-compose up -d nacos`，访问 `http://localhost:8848/nacos`（账号 nacos/nacos）
2. 启动 Gateway + User 服务，观察 Nacos 控制台的"服务列表"
3. 通过网关访问：`curl http://localhost:8080/ai-cs-user/api/user/list`
4. 在 Nacos 中修改配置，验证 `@RefreshScope` 是否生效

---

## 学习检查清单

- [ ] 理解微服务 vs 单体的优劣
- [ ] 理解 Nacos 注册发现的工作流程
- [ ] 理解 Gateway 的路由和过滤器机制
- [ ] 会用 Feign 进行服务间调用
- [ ] 理解负载均衡和容错降级
- [ ] 理解配置中心的动态刷新

---

## 下一步

→ [03-Nacos注册与配置中心](./03-Nacos注册与配置中心.md)
