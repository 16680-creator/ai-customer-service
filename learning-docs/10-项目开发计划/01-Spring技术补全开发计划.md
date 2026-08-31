# 01-Spring 技术补全开发计划（六阶段落地）

> 2026-08 制定。来源于项目 Spring 技术盘点：Security / 缓存抽象与事件体系 / Testcontainers /
> 自定义 Starter / STOMP / State Machine 六项空白。
> 其中 **Spring Security、Testcontainers** 在 [08-测试/02-Testcontainers与安全框架技术选型.md](../08-测试/02-Testcontainers与安全框架技术选型.md)
> 中为「暂缓」状态，本计划按该文档写明的触发条件正式启动：
> Security → 触发条件是「声明式权限模型」；Testcontainers → 触发条件是「具备 Docker 环境」。

## 〇、总览与排期

| 阶段 | 主题 | 核心产出 | 主要模块 | 预估 |
|------|------|----------|----------|------|
| P1 | Spring Security 方法级授权 | 「网关认证 + 服务授权」两层模型 | gateway → common → user 试点 → 全服务 | 3~4 天 |
| P2 | 缓存抽象 + 事件体系 | `@Cacheable` 铺商品缓存；领域事件解耦 | product / order | 2~3 天 |
| P3 | Testcontainers | 真实中间件集成测试基类 | user / order（需 Docker） | 2 天 |
| P4 | 自定义 Starter | common 自动装配化 | common + 全部 11 服务 | 2 天 |
| P5 | STOMP | WebSocket 通道升级 + CONNECT 鉴权 | notify + 前端 | 2~3 天 |
| P6 | Spring State Machine | 订单状态机治理 | order | 3 天 |

**依赖关系**：

```
P1 Security ──→ P5 STOMP（CONNECT 鉴权复用 Security 上下文）
P2 Cache/Event ─┐
                ├─→ P4 Starter（把 P1/P2 沉淀的公共配置收编进自动装配）
P3 独立（仅依赖 Docker）
P6 独立
```

**纪律**（宪法 2-1）：每阶段 TDD 先行、每阶段独立 feature 分支、`mvn verify` 过 JaCoCo 门禁
（行 ≥ 40%，分支 ≥ 30%）后才可合并。

---

## 一、P1：Spring Security（网关认证 + 服务授权）

### 现状锚点

- 网关 `ai-cs-gateway/.../filter/AuthFilter.java` 校验 JWT 后透传 `X-User-Id` / `X-User-Name`
  （并先清除客户端伪造的同名头），**角色目前没有透传**，只在 JWT payload 里
- 下游服务完全没有 Security：无 `SecurityFilterChain`、无方法级权限，靠业务代码手写角色判断
- 通用工具 `ai-cs-common/.../util/JwtUtil.java`，异常出口 `GlobalExceptionHandler`

### 方案

不引入登录改造（登录仍走现有 `/login` + JWT），Security 只做**资源服务器式授权**，
即选型文档指出的「网关只做认证、服务做授权」的关注点拆分：

1. **网关补角色透传**：`AuthFilter` 解析 JWT 中的 role，写入 `X-User-Roles` 头
   （同样先 remove 再写入，防伪造）。
2. **common 新增 security 包**（做成可复用配置，P4 收编进 starter）：
   - `HeaderAuthenticationFilter`：读 `X-User-Id/X-User-Name/X-User-Roles`，
     构造 `UsernamePasswordAuthenticationToken`（principal=userId，authorities=roles）放入上下文；
     头缺失或非网关入口路径 → 匿名
   - `ServiceSecurityConfig`：`SecurityFilterChain` 默认全拦截 `authenticated`，
     `@EnableMethodSecurity` 开启方法级安全；
     `AuthenticationEntryPoint` / `AccessDeniedHandler` 返回统一 `Result` JSON（对齐
     `GlobalExceptionHandler` 的响应结构，前端零改动）
3. **@PreAuthorize 铺开**：先 user 服务试点（用户管理接口 `hasRole('ADMIN')`），
   再铺 product 后台接口、order 管理端接口。
4. **服务间调用适配**（风险点）：`ai-cs-order` 的 `OrderPayClient` / `ProductStockClient`
   （RestTemplate）与 chat 的 Feign 客户端需要透传 `X-User-Id/X-User-Roles`，
   给 RestTemplate / Feign 加统一的请求拦截器。

### 任务清单（✅ 2026-08 user 试点落地，技术文档见 [09-安全与设计模式/03](../../09-安全与设计模式/03-SpringSecurity微服务两层安全模型.md)）

- [x] AuthFilter 补 `X-User-Roles` 可信透传（JWT role 标准化 ROLE_*；API Key=ROLE_SERVICE；
      白名单与普通路径均先清洗伪造头），AuthFilterTest 锁定
- [x] user 试点 `HeaderAuthenticationFilter`（有头/无头/多角色/ThreadLocal清理）+ 单测
- [x] user `SecurityFilterChain`：无状态、统一 401/403 Result、register/login/actuator/swagger 白名单
- [x] user 方法级授权：本人或 ADMIN；MockMvc 真链路验证无头401、越权403、本人/ADMIN 200
- [ ] Feign 内部调用身份头透传拦截器 → 下个服务铺开时实施（当前 order→product/pay 不依赖用户授权）
- [ ] 其余服务铺开 → 01-P4 Starter 自动装配后统一接入，避免复制配置
- [x] `mvn -pl ai-cs-gateway,ai-cs-user -am verify`：gateway 29 + user 22 测试全绿

### 验收标准

- 无 token → 网关 401；token 有效但角色不符 → 服务 403，响应体都是 `Result` 结构
- 管理接口凭普通用户角色无法访问（集成测试证明）
- 全服务 `mvn verify` 通过，启动链路无 Security 引起的循环依赖

### 面试要点

认证与授权分离的边界设计；Security Filter Chain 与 WebFlux 网关 Filter 的位置关系；
方法级安全的 AOP 原理（`@PreAuthorize` 由 `AuthorizationInterceptor` 代理生效）；
为什么微服务里不用 `UserDetailsService` + Session。

---

## 二、P2：缓存抽象 + 事件体系

### 现状锚点

- `ai-cs-product/.../ProductServiceImpl.java`、`ai-cs-order/.../CartServiceImpl.java`
  直接手写 `redisTemplate` 存取，缓存逻辑与业务耦合
- chat 的 `HotQaCacheService` / `SemanticCacheService` 是语义缓存等专用设施，**本阶段不动**
- 事件体系：仅 `ai-cs-chat/.../ChatModelRegistry.java` 一处 `@EventListener`；
  `ApplicationEventPublisher`、`@TransactionalEventListener` 全项目零使用
- 订单超时逻辑集中在 `OrderTimeoutScheduler` / `OrderTimeoutScanJob`（XXL-Job + 本地调度）

### 方案

**2.1 缓存抽象**

1. product / order 引入 `spring-boot-starter-cache`，`RedisCacheManager`
   + `GenericJackson2JsonRedisSerializer`，按缓存名分 TTL（商品详情 30min、分类树 10min、
   购物车 TTL 单独评估）
2. 改造点：`ProductServiceImpl` 查询类方法 → `@Cacheable(cacheManager, key)`；
   更新 / `deductStock` → `@CacheEvict`（先更库后删缓存的顺序写进注释约定）
3. 与 Seata / Redisson 共存说明：缓存删除放在本地事务提交后
   （可与 2.2 的 `@TransactionalEventListener(AFTER_COMMIT)` 配合，顺势打通）

**2.2 事件体系**

1. 引入 `ApplicationEventPublisher` 发布领域事件：`OrderPaidEvent`、`OrderTimeoutEvent`、
   `OrderCreatedEvent`（放 common 或 order 的 event 包）
2. 关单改造：`OrderTimeoutScanJob` 扫到超时单 → 发布 `OrderTimeoutEvent` →
   监听器执行关单 + 还券 + 还库存（从 cancel 方法中拆出），
   校验动作仍集中在状态字段（P6 再升级为状态机事件）
3. 支付成功链路：pay 服务的 `PaySuccessMessage`（common/mq）消费侧补
   `@TransactionalEventListener(phase = AFTER_COMMIT)`，事务提交后才触发通知类副作用，
   避免回滚后仍推送
4. 与 RocketMQ 分工写进文档：进程内领域事件（同一服务、强一致边界内）vs 跨服务事件（MQ）

### 任务清单（✅ 2026-08 已落地，技术文档见 [02-Spring微服务/09](../../02-Spring微服务/09-SpringCache与事务领域事件.md)）

- [x] ProductCacheConfig + 分 cacheName TTL（详情30min/分类10min）+ 缓存契约测试
- [x] `@Cacheable`（详情/分类）+ `@CacheEvict`（更新/删除/扣补库存/建分类）契约测试；
      product 54 测试全绿 + JaCoCo 门禁通过
- [x] `OrderTimeoutEvent` 发布/监听拆分（MQ listener 只做协议适配，领域 listener 执行业务）
- [x] `OrderPaidEvent` + `@TransactionalEventListener(AFTER_COMMIT)`，事务回滚不推送支付通知；
      order 81 测试全绿 + JaCoCo 门禁通过
- [x] 技术文档《Spring Cache 与事务领域事件》产出

### 面试要点

Spring Cache 三层抽象（CacheManager/Cache/注解 AOP）；缓存一致性的「先更库后删缓存」
与删除时机；`@TransactionalEventListener` 四种 phase 与默认 AFTER_COMMIT 的意义；
领域事件与 MQ 事件的选型边界。

---

## 三、P3：Testcontainers（真实中间件集成测试）

### 前置条件（硬门槛）

本机与 CI **必须有可用 Docker**（`docker info` 通过）。选型文档明确：无 Docker 环境时
引入跑不起来的依赖是负资产。**不具备则本阶段挂起，不阻塞其他阶段。**

### 方案

1. 父 POM：`testcontainers-bom:1.19.7`（import）+ `testcontainers:mysql`、
   `testcontainers:junit-jupiter`（test scope）；Redis 用 `GenericContainer` 或
   `com.redis:testcontainers-redis`
2. Boot 3.2 新特性优先：`@ServiceConnection` 替代 `@DynamicPropertySource` 样板
3. 基类 `IntegrationTestBase`（`@Testcontainers` + `@SpringBootTest` + `@Tag("integration")`），
   无 Docker 的环境用 `@EnabledIf` 探测自动跳过，CI 不挂
4. 优先替换的三个场景（选型文档已列）：
   - **ShardingSphere 真库路由**（user）：真 MySQL 验证分片落表（H2 验不了分片 SQL 语义）
   - **Seata AT 全局回滚**（order+product）：真 `undo_log` 行为，两个 MySQL 容器
   - **Redisson 锁看门狗续期**（order）：真 Redis 验证锁超时自动续期

### 任务清单

- [ ] 依赖与 `IntegrationTestBase` 基类；`docker info` 探测跳过逻辑
- [ ] user：分库分表真库集成测试
- [ ] order：Seata 回滚集成测试（模拟 product 分支抛异常，断言订单未落库）
- [ ] order：Redisson 看门狗集成测试
- [ ] CI 增加 integration job（有 Docker 的 runner 才跑，`-Dgroups=integration`）

### 面试要点

测试金字塔与集成测试的真实性取舍；`@ServiceConnection` 背后的 `ConnectionDetails`
扩展机制；Testcontainers 的 Ryuk 容器复用（`.withReuse`）加速技巧。

---

## 四、P4：自定义 Starter（common 自动装配化）

### 现状锚点

- `ai-cs-common` 是普通 jar，各服务靠包扫描 `com.aics.common` 拿到 Bean
- `EmbeddingAutoConfig` 已经用了 `@ConditionalOnProperty` + `@ConditionalOnMissingBean`，
  但模块**没有 `META-INF/spring/...AutoConfiguration.imports`**，自动装配只差最后一步
- 待收编组件：`GlobalExceptionHandler`、`Result/ResultCode`、`JwtUtil`、
  `MinioConfig/MinioProperties/FileStorageService`、`EmbeddingAutoConfig`

### 方案

不新建模块（保持依赖方向规则：common 不依赖任何业务模块），在 common 内补齐自动装配入口：

1. 新建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，
   登记四个自动配置类（`@AutoConfiguration`）：
   - `CommonWebAutoConfig`：GlobalExceptionHandler（`@ConditionalOnWebApplication`）
   - `JwtAutoConfig`：JwtUtil + 密钥 `@ConfigurationProperties` 绑定
   - `MinioAutoConfig`：`@ConditionalOnProperty("aics.minio.enabled")` 开关
   - `EmbeddingAutoConfig`：改注解为 `@AutoConfiguration`，逻辑不变
2. P1 的 `ServiceSecurityConfig`、P2 的 CacheConfig 按同样模式收编
   （`@ConditionalOnClass(SecurityFilterChain.class)` 等防误装）
3. 各服务 `Application` 类检查：去掉不再需要的 `scanBasePackages` 范围、显式 `@Import`
4. **进阶可选**（时间富余再做）：抽 `ai-cs-common-starter` 独立模块，common 退回纯工具库

### 任务清单（✅ 2026-08 已落地，技术文档见 [02-Spring微服务/10](../../02-Spring微服务/10-自定义Starter与自动装配.md)）

- [x] AutoConfiguration.imports + 3 个自动配置（Web/MinIO/Embedding）；
      `ApplicationContextRunner` 条件装配/覆盖测试 5 项
- [x] FileStorageService/MinioProperties 由自动配置提供，删除旧 MinioConfig；
      chat/knowledge/mq/notify/order/pay/product 七服务移除 common 包扫描
- [x] common/order/product 组合回归（10 + 81 + 54 测试全绿）；
      全 11 服务实启动回归需基础设施环境，后续部署验收补
- [x] 技术文档《自定义 Starter 与自动装配》产出

### 面试要点

这正是「SpringBoot 自动装配原理」的实战版：`@EnableAutoConfiguration` → imports 文件 →
`@Conditional` 系列 → `@ConfigurationProperties` 绑定。面试时可以直接以自己的 common 改造
作为案例讲。

---

## 五、P5：STOMP（WebSocket 通道升级）

### 现状锚点

- `ai-cs-notify/.../config/WebSocketConfig.java`：裸 handler 注册 `/ws/notify`，
  `setAllowedOrigins("*")`
- `NotifyWebSocketHandler`（TextWebSocketHandler）自己维护会话、自己拼消息；
  转人工通知走 `NotifyHandoffServiceImpl` + `HandoffNoticeDTO`

### 方案

1. `@EnableWebSocketMessageBroker` + `WebSocketMessageBrokerConfigurer`：
   - endpoint：`/ws-stomp`（保留 SockJS fallback）
   - 广播前缀 `/topic`（公告、排队叫号）；点对点前缀 `/queue` + user destination
2. `NotifyHandoffServiceImpl` 改用 `SimpMessagingTemplate.convertAndSendToUser(userId, ...)`
   推送转人工通知——替代手写会话路由
3. **CONNECT 鉴权**（依赖 P1）：`ChannelInterceptor` 在 CONNECT 帧校验 token，
   建立 `Principal`，user destination 才有绑定目标；未登录 CONNECT 拒绝
4. 兼容策略：旧 `/ws/notify` 保留一个版本的灰度期，前端 `ai-cs-frontend` 切
   `@stomp/stompjs` 后移除；网关确认 notify 路由的 WebSocket upgrade 透传
5. 顺手修掉 `setAllowedOrigins("*")` → 白名单

### 任务清单（✅ 后端主路径已落地，技术文档见 [02-Spring微服务/11](../../02-Spring微服务/11-STOMP实时通知与用户目的地.md)）

- [x] STOMP 配置 + CONNECT Bearer JWT 鉴权拦截器单测（合法 token → Principal，无 token 拒绝）
- [x] 转人工/通知推送迁 `convertAndSendToUser` + user destination/topic 单测；notify 25 tests + JaCoCo 门禁通过
- [ ] 前端 `@stomp/stompjs`：当前前端无任何通知 WebSocket 入口，避免添加未使用依赖；通知 UI 开发时接入
- [ ] 灰度期后删除裸 handler：保留 `/ws/notify` 一版兼容期，但已收紧 Origin 白名单；上线使用量清零后删除
- [x] 裸 WS vs STOMP 取舍与外部 broker 扩展边界文档化

### 面试要点

STOMP 帧协议与子协议协商；user destination 的 Principal→session 映射原理；
broker 简单实现 vs 外置 RabbitMQ relay 的扩展边界。

---

## 六、P6：Spring State Machine（订单状态机治理）

### 现状锚点

- `ai-cs-order/.../enums/OrderStatus.java`：5 态（PENDING_PAY / PAID / CANCELLED /
  REFUNDING / REFUNDED）
- `OrderServiceImpl` 手写状态校验散落各方法（约 213~331 行的 if 串），
  `OrderTimeoutScheduler` 直接改状态；无集中流转定义

### 方案

1. 引入 `spring-statemachine-starter:4.0.0`（兼容 Boot 3.2）：
   States = `OrderStatus`，Events = `PAY / CANCEL / REFUND_REQUEST / REFUND_SUCCESS /
   TIMEOUT`，集中定义 transition + guard（校验前置态）+ action（还券、退款等副作用）
2. **持久化模式选轻量方案**：微服务无状态，每次业务请求从 DB 读当前状态 →
   `StateMachineFactory` 构建 → `sendEvent` → 监听 transition 把新状态写库；
   **不用** `StateMachinePersister` 内存持久化（多实例下不成立），理由写进代码注释
3. `OrderServiceImpl` 的 pay / cancel / refund 与超时任务统一改为事件驱动：
   非法流转（如 `CANCELLED → PAID`）由 guard 拦截，抛业务异常统一 409 语义
4. 每条 transition 的 action 与 P2 的领域事件衔接（如 `REFUND_SUCCESS` 后发
   `OrderRefundedEvent`）

### 任务清单（✅ 2026-08 已落地，技术文档见 [02-Spring微服务/12](../../02-Spring微服务/12-订单状态机治理.md)）

- [x] 状态机配置 + 合法迁移矩阵单测（待支付三路径、退款两路径、非法迁移拒绝）
- [x] `OrderServiceImpl` 支付/取消/超时/退款接入状态机；退款显式两段 PAID→REFUNDING→REFUNDED
- [x] 状态机并发 reset 使用 synchronized；DB status 为唯一事实源（多实例不使用内存 persister）
- [x] `mvn -pl ai-cs-order verify`：84 tests + JaCoCo 门禁通过；技术文档产出

### 面试要点

状态机模式解决什么问题（合法迁移集中声明、防非法跳转）；guard/action/builder 三件套；
为什么分布式场景放弃内存持久化；与工作流引擎（Flowable）的重量级边界。

---

## 七、风险与回滚

| 风险 | 缓解 |
|------|------|
| Security 迁移波及 11 个服务启动链路 | user 试点先行；starter 条件装配保证未接入服务行为不变；出问题回滚只动 common + 网关两个点 |
| 内部调用（RestTemplate/Feign）漏传身份头导致 401 | P1 任务清单单独列拦截器项；先在测试环境全链路回归 |
| Testcontainers 卡在无 Docker | 硬门槛前置检查，挂起不影响其余阶段 |
| STOMP 需要前端同步改版 | 保留旧通道一个版本灰度 |
| 状态机对 5 个状态偏重 | 定位为「治理 + 学习」双目标；保留 OrderStatus 枚举作为唯一事实来源，随时可退回 if 校验 |
| JaCoCo 门禁不达标 | 每阶段任务清单均含测试项，TDD 保证覆盖率随实现同步增长 |

## 附：新增依赖版本清单

| 依赖 | 版本 | 管理方式 |
|------|------|----------|
| spring-boot-starter-security | 随 Boot 3.2.5（Security 6.2.x） | 父 POM dependencyManagement 可省 |
| spring-boot-starter-cache | 随 Boot | 无需管理 |
| spring-statemachine-starter | 4.0.0 | 父 POM 新增 `<statemachine.version>` |
| testcontainers-bom | 1.19.7 | 父 POM import（test scope） |
| @stomp/stompjs（前端） | 7.x | `ai-cs-frontend/package.json` |
