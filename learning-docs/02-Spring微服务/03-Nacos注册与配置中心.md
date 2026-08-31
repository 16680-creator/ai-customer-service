# Nacos 注册中心与配置中心

> 本项目使用 **Nacos v2.3.2**（Standalone 模式）作为服务注册中心和配置中心。
> 对应项目文件：`docker-compose.yml` 中的 nacos 服务、各服务的 `application.yml`

---

## 一、Nacos 在项目中的角色

```
┌─────────────────────────────────────────────────────┐
│                    Nacos :8848                        │
│                                                      │
│  ┌──────────────────┐    ┌──────────────────────┐   │
│  │   注册中心        │    │     配置中心          │   │
│  │                  │    │                      │   │
│  │ • 服务注册       │    │ • 集中管理配置        │   │
│  │ • 服务发现       │    │ • 动态推送更新        │   │
│  │ • 健康检查       │    │ • 多环境隔离          │   │
│  │ • 负载均衡       │    │ • 版本回滚           │   │
│  └──────────────────┘    └──────────────────────┘   │
└─────────────────────────────────────────────────────┘
         ↑                          ↑
    各微服务注册               各微服务拉取配置
```

---

## 二、Docker 部署 Nacos

本项目 `docker-compose.yml` 中的配置：

```yaml
nacos:
  image: nacos/nacos-server:v2.3.2
  container_name: aics-nacos
  ports:
    - "8848:8848"    # HTTP 端口（控制台 + API）
    - "9848:9848"    # gRPC 端口（2.x 新增，客户端通信用）
  environment:
    MODE: standalone                    # 单机模式
    SPRING_DATASOURCE_PLATFORM: mysql   # 配置持久化到 MySQL
    MYSQL_SERVICE_HOST: mysql
    MYSQL_SERVICE_PORT: 3306
    MYSQL_SERVICE_DB_NAME: nacos_config
    MYSQL_SERVICE_USER: root
    MYSQL_SERVICE_PASSWORD: root
    JVM_XMS: 256m                      # 最小堆内存
    JVM_XMX: 512m                      # 最大堆内存
  depends_on:
    mysql:
      condition: service_healthy        # 等 MySQL 就绪再启动
```

**关键点**：
- `MODE: standalone` → 单机模式（生产用 cluster）
- 配置数据存在 MySQL 的 `nacos_config` 库中（重启不丢失）
- 必须等 MySQL 健康后才启动（`depends_on` + `healthcheck`）

---

## 三、服务注册与发现

### 3.1 服务注册（Provider 端）

每个微服务启动时自动注册：

```yaml
# 各服务的 application.yml
spring:
  application:
    name: ai-cs-user          # ← 这就是注册到 Nacos 的服务名
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: aics        # 命名空间 ID（不是名称！）
        group: DEFAULT_GROUP
```

启动后在 Nacos 控制台可以看到：

```
服务列表：
┌────────────────┬──────────┬────────────────────┬────────┐
│ 服务名          │ 实例数    │ IP:Port            │ 状态   │
├────────────────┼──────────┼────────────────────┼────────┤
│ ai-cs-gateway  │ 1        │ 192.168.1.10:8080  │ 健康   │
│ ai-cs-user     │ 1        │ 192.168.1.10:8081  │ 健康   │
│ ai-cs-chat     │ 1        │ 192.168.1.10:8083  │ 健康   │
│ ai-cs-order    │ 1        │ 192.168.1.10:8087  │ 健康   │
└────────────────┴──────────┴────────────────────┴────────┘
```

### 3.2 服务发现（Consumer 端）

```java
// 方式一：通过 Feign（推荐）
@FeignClient(name = "ai-cs-user")  // 服务名，Nacos 自动解析地址
public interface UserFeignClient {
    @GetMapping("/api/user/{id}")
    Result<UserVO> getUser(@PathVariable Long id);
}

// 方式二：通过 DiscoveryClient（了解即可）
@Autowired
private DiscoveryClient discoveryClient;

public void findServices() {
    List<ServiceInstance> instances = discoveryClient.getInstances("ai-cs-user");
    instances.forEach(i -> 
        System.out.println(i.getHost() + ":" + i.getPort())
    );
}
```

### 3.3 健康检查机制

```
服务启动 → 注册到 Nacos → 每 5 秒发心跳
                              ↓
                    Nacos 15 秒没收到心跳 → 标记不健康
                              ↓
                    Nacos 30 秒没收到心跳 → 摘除实例
                              ↓
                    服务恢复 → 重新注册
```

---

## 四、配置中心

### 4.1 配置的层级结构

```
Nacos 配置模型：
  Namespace（命名空间）→ Group（分组）→ Data ID（配置文件）

本项目：
  Namespace: aics
  ├── Group: DEFAULT_GROUP
  │   ├── ai-cs-gateway.yml
  │   ├── ai-cs-user.yml
  │   ├── ai-cs-chat.yml
  │   └── ai-cs-order.yml
  └── Group: SHARED_GROUP
      └── common-datasource.yml  ← 共享的数据库配置
```

### 4.2 配置内容示例

在 Nacos 控制台创建 `ai-cs-order.yml`：

```yaml
# 数据源配置
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql:3306/aics_order?useUnicode=true&characterEncoding=utf8
    username: root
    password: root
  redis:
    host: redis
    port: 6379
    database: 0

# RocketMQ 配置
rocketmq:
  name-server: rocketmq-namesrv:9876
  producer:
    group: order-producer-group

# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 打印 SQL
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 4.3 服务拉取配置

```yaml
# 服务的 bootstrap.yml（优先级高于 application.yml）
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: aics
        group: DEFAULT_GROUP
        file-extension: yml
        # 自动拉取: ${spring.application.name}.${file-extension}
        # 即: ai-cs-order.yml
        
        # 共享配置
        shared-configs:
          - data-id: common-datasource.yml
            group: SHARED_GROUP
            refresh: true    # 支持动态刷新
```

### 4.4 动态刷新

```java
@RestController
@RefreshScope  // ← 关键注解：配置变更时重建这个 Bean
public class AiConfigController {

    @Value("${ai.chat.temperature:0.7}")
    private double temperature;

    @Value("${ai.chat.max-tokens:2048}")
    private int maxTokens;

    @GetMapping("/ai-config")
    public Result<Map<String, Object>> getAiConfig() {
        return Result.success(Map.of(
            "temperature", temperature,
            "maxTokens", maxTokens
        ));
    }
}
```

在 Nacos 控制台修改 `temperature` 为 0.9 → 不用重启 → 下次请求就返回 0.9。

---

## 五、命名空间隔离

### 5.1 为什么需要命名空间？

```
一套 Nacos 服务多个环境：

Namespace: dev（开发）
  └── ai-cs-order.yml → 连接本地 MySQL

Namespace: test（测试）
  └── ai-cs-order.yml → 连接测试 MySQL

Namespace: prod（生产）
  └── ai-cs-order.yml → 连接生产 MySQL 集群
```

### 5.2 创建命名空间

Nacos 控制台 → 命名空间 → 新建：
- 命名空间名：`aics`
- 描述：AI 客服系统

**注意**：配置文件中填的是**命名空间 ID**（一串 UUID），不是名称！

---

## 六、Nacos 控制台操作指南

访问 `http://localhost:8848/nacos`（默认账号 nacos/nacos）

### 常用操作

| 功能 | 路径 | 说明 |
|------|------|------|
| 查看服务 | 服务管理 → 服务列表 | 看哪些服务在线 |
| 查看实例 | 点击服务名 | 看 IP、端口、健康状态 |
| 创建配置 | 配置管理 → 配置列表 → + | 新建 yml 配置 |
| 修改配置 | 点击配置 → 编辑 | 修改后点"发布" |
| 历史版本 | 配置详情 → 历史版本 | 可以回滚 |
| 监听查询 | 配置管理 → 监听查询 | 看哪些服务在用这个配置 |

---

## 七、常见问题

### Q1: 服务注册不上去？

```bash
# 1. 检查 Nacos 是否启动
curl http://localhost:8848/nacos/v1/console/health/readiness

# 2. 检查命名空间 ID 是否正确（不是名称！）
# 在 Nacos 控制台 → 命名空间 → 复制 ID

# 3. 检查网络（Docker 内用服务名，宿主机用 localhost）
# Docker 内: server-addr: nacos:8848
# 宿主机:   server-addr: 127.0.0.1:8848
```

### Q2: 配置不生效？

```
检查顺序：
1. Data ID 格式：${spring.application.name}.${file-extension}
   → 必须是 ai-cs-order.yml，不是 ai-cs-order.yaml
2. Group 是否匹配
3. Namespace 是否匹配
4. 本地 application.yml 的同名配置会覆盖 Nacos（优先级问题）
```

### Q3: 本地开发不想启动 Nacos？

```yaml
# 直接关闭（本项目 ai-cs-chat 就是这么做的）
spring:
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
```

---

## 八、动手练习

1. 启动 Nacos：`docker-compose up -d nacos mysql`
2. 访问控制台，创建命名空间 `aics`
3. 在 `aics` 下创建配置 `ai-cs-chat.yml`，内容自定义
4. 启动 ai-cs-chat（开启 nacos config），验证配置是否拉取成功
5. 在控制台修改配置，验证 `@RefreshScope` 动态刷新

---

## 九、动态配置的两种机制（2026-08 补，03-P4 落地记录）

> 全项目此前只有 chat 的 `ModelRouterProperties` 一处用到了动态刷新能力，
> 改配置要重启服务。本节补齐两种机制的差异与本项目落地位置。

### 机制一：`@Value` 字段 → 必须配 `@RefreshScope`

`@Value` 在 Bean 初始化时一次性注入。Nacos 配置变更 → ContextRefresher 发布
RefreshEvent → RefreshScope 里缓存的 Bean 被**销毁重建**，字段才重新注入。
不加 `@RefreshScope` 的 `@Value` 永远是旧值。

反模式（不生效）：静态字段、构造期读取、`final` 字段——RefreshScope 重建代理
救不了这三类。

### 机制二：`@ConfigurationProperties` → 自动重绑定（推荐）

spring-cloud-context 在 RefreshEvent 后执行 `ConfigurationPropertiesRebinder`：
对**所有** `@ConfigurationProperties` Bean 重新 bind 环境里的新值，**不需要**
`@RefreshScope`，使用方 Bean 也不重建。更轻、无代理重建成本，是首选。

### 项目落地

| 位置 | 改造 |
|---|---|
| `ai-cs-gateway/.../config/RateLimitProperties.java` | 新增 `@ConfigurationProperties("aics.gateway.rate-limit")`，`RateLimitFilter` 的 5 个 `@Value` 全部迁入；Nacos 改限流阈值 10s 内生效不重启 |
| GatewayApplication | `@ConfigurationPropertiesScan("com.aics.gateway.config")` 注册 |

---

## 学习检查清单

- [ ] 理解 Nacos 的两个角色：注册中心 + 配置中心
- [ ] 理解 Namespace → Group → Data ID 的层级
- [ ] 会部署 Standalone 模式的 Nacos
- [ ] 理解服务注册、心跳、摘除的机制
- [ ] 会创建和修改 Nacos 配置
- [ ] 理解 @RefreshScope 动态刷新原理
- [ ] 知道本地开发如何绕过 Nacos
- [ ] 说得清 @RefreshScope（销毁重建）与 @ConfigurationProperties（重绑定）的机制差异
- [ ] 知道 @Value + 静态字段/final 字段动态刷新不生效的原因

---

## 下一步

→ [04-SpringCloudGateway网关](./04-SpringCloudGateway网关.md)
