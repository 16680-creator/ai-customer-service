# 18-Spring 面试专练与追问链

> 本模块 01~17 篇的**验收出口**。写法对齐 [03-数据库与ORM/10-数据库面试专练](../03-数据库与ORM/10-数据库面试专练.md)：先给四层答题模板，再按主题分组刷高频题（每题一句话答案 + 展开锚点），然后做三条追问链演练，最后一张考前速览卡。
>
> 覆盖范围：**Spring Framework 本体**（IoC/AOP/事务/MVC/自动装配）+ **Spring 生态在本项目的落地**（Cache/事件/Security/StateMachine/STOMP/Spring AI）。
> 与其他面试资料的分工：
> - [05-AI集成/面试题/02-Spring-AI框架](../05-AI集成/面试题/02-Spring-AI框架.md)——Spring AI 专项；
> - [18-银行金融开发/11](../18-银行金融开发/11-银行岗面试题库与简历映射.md) A33 等——银行方向话术；
> - [12-性能工程](../12-性能工程/README.md)、[13-稳定性工程](../13-稳定性工程/README.md)——JVM/性能/稳定性向。

---

## 一、四层答题模板（先定性、再机制、后本项目、兜底边界）

被问任何 Spring 原理题，按这四层组织，信息密度最高：

1. **一句话定性**（是什么/解决什么）：如"`@Transactional` 是 AOP 环绕通知 + ThreadLocal 绑定连接"。
2. **机制展开**（怎么实现）：代理 → `TransactionInterceptor` → 事务管理器 → ThreadLocal 绑定 `Connection` → 提交/回滚。
3. **落到本项目**（证明真做过）：`OrderServiceImpl` 双层事务、`rollbackFor = Exception.class` 全仓统一、`@CacheEvict` 与事务的顺序分析。
4. **边界与代价**（体现工程判断）：同类自调用失效；跨线程无效；与 `@CacheEvict` 叠用时的提交前窗口。

> **为什么是这四层**：第 1 层证明"你懂"，第 2 层证明"你深"，第 3 层证明"你真做过"，第 4 层证明"你有判断"。
> 只答第 1 层是背书；直接跳到第 3 层是"只会用不会讲"。

---

## 二、高频题分组刷（判断 / 权衡 / 排查 / 设计）

### 2.1 IoC 容器与 Bean 生命周期组

| 题 | 一句话答案 | 展开锚点 |
|---|---|---|
| IoC 和 DI 什么关系 | IoC 是思想（创建权反转给容器），DI 是实现手段（依赖从外面递进来） | [16 §一](16-IoC容器与Bean生命周期.md) |
| Bean 生命周期几步 | 实例化 → 属性填充 → Aware → BPP.before → 初始化(@PostConstruct) → BPP.after（**AOP 代理在此生成**）→ 使用 → 销毁(@PreDestroy) | [16 §三](16-IoC容器与Bean生命周期.md) |
| 为什么推荐构造器注入 | 依赖不可变 + 启动 fail-fast + 便于单测 + **循环依赖早暴露** | `OrderServiceImpl` 的 `@RequiredArgsConstructor` |
| 循环依赖怎么解决 | 三级缓存（成品/半成品/工厂）只救**单例+setter/字段**；构造器环启动即报错 | [16 §5.2](16-IoC容器与Bean生命周期.md) |
| 第三级缓存为什么是工厂 | 能在"提前暴露引用"时决定返回原始对象还是代理（`getObject()`） | 同上 |
| `@PostConstruct` 里能开事务吗 | 不能（代理在更晚的 BPP.after 才生成）；且此时其他 Bean 可能未就绪 | [16 §3.1](16-IoC容器与Bean生命周期.md) |
| 项目里 Bean 启动初始化都干什么 | 建模型注册表（`ChatModelRegistry`）、建 ES 索引（`SearchServiceImpl`）、灌 Sentinel 规则、建库表（`PaySchemaInitializer` ApplicationRunner） | 各文件 `@PostConstruct` |
| 单例 Bean 线程安全吗 | 容器保证"单例"，不保证"线程安全"；共享可变状态要自己处理——项目用"不可变快照 + volatile 原子替换" | `ChatModelRegistry:30-31,76-77` |

### 2.2 AOP 与声明式事务组（面试重灾区）

| 题 | 一句话答案 | 展开锚点 |
|---|---|---|
| 注解为什么能自动生效 | 注解只是贴纸，**代理**是执行者；调用经过代理 → 生效 | [15 §一、三](15-SpringAOP与声明式事务原理.md) |
| JDK 代理与 CGLIB 区别 | 机制上：有接口 JDK 造"兄弟"，无接口 CGLIB 造"儿子"，final 方法 CGLIB 代理不了；**Boot 2.x 起默认全用 CGLIB**（`spring.aop.proxy-target-class=true`，有接口也走 CGLIB） | [15 §四](15-SpringAOP与声明式事务原理.md) |
| Mapper 接口没实现类为何能注入 | MyBatis `MapperFactoryBean` 注册的是 **JDK 动态代理**，方法调用被拦截去执行 SQL | 同上 |
| `@Transactional` 原理一句话 | AOP 环绕通知 + ThreadLocal 保存当前连接（`TransactionSynchronizationManager`） | [15 §5.1](15-SpringAOP与声明式事务原理.md) |
| 事务为什么会失效 | 七查：自调用 / 非 public / 吞异常 / 受检异常没 rollbackFor / 子线程 / 非 InnoDB / 传播行为 | [15 §六](15-SpringAOP与声明式事务原理.md) |
| 七个传播行为 | `REQUIRED` 默认"有则加入无则新建"；`REQUIRES_NEW` 各过各的；`NESTED` 用 savepoint；其余"有/无所谓/不许有" | [15 §5.3](15-SpringAOP与声明式事务原理.md) |
| 为什么全仓都写 `rollbackFor = Exception.class` | 默认只回滚运行时异常；业务大量抛受检异常时必须显式声明 | `OrderServiceImpl`/`ProductServiceImpl`（grep 12 处） |
| `@Transactional` 和 `@GlobalTransactional` 叠着用谁的活 | 本地事务管本服务表；Seata 全局事务用 XID 串起跨服务调用 | [02-Spring微服务/06](06-Seata分布式事务AT模式.md) |
| 事务里删缓存有什么坑 | `@CacheEvict` 在方法返回后执行，但与提交的先后由拦截器 `order` 决定（默认同级、**不保证**）→ 按最坏情况"提交前删"评估回填窗口 | `ProductServiceImpl:189-192`、[02-Spring微服务/09 §1.4](09-SpringCache与事务领域事件.md) |
| 多个注解叠加的顺序 | **确定的两条**：事件监听器挂提交后（AFTER_COMMIT）、幂等占位早于事务提交；**不保证的一条**：缓存删除与事务提交的先后（两个 advisor 默认 order 同级） | [15 §八](15-SpringAOP与声明式事务原理.md) |
| 怎么验证代理生效 | 打印类名（`$$EnhancerBySpringCGLIB` / `$Proxy`）、`AopUtils`、`AspectJProxyFactory` 单测 | `IdempotentAspectTest` |

### 2.3 Spring MVC 与参数校验组

| 题 | 一句话答案 | 展开锚点 |
|---|---|---|
| 一个请求的完整链路 | Filter → DispatcherServlet → HandlerMapping → HandlerAdapter（解析+校验）→ Controller → 序列化 → 异常出口 | [17 §一](17-SpringMVC请求全链路与参数校验.md) |
| 四个取数注解 | `@RequestHeader` 头 / `@RequestParam` 查询串 / `@PathVariable` 路径 / `@RequestBody` JSON 体 | `CartController`、`ChatController` |
| Filter / Interceptor / AOP 区别 | 归属不同（Servlet/MVC/AOP）、能力不同（拿不到方法 / 拿到 handler / 拿到参数返回值）、顺序外→内 | [17 §三](17-SpringMVC请求全链路与参数校验.md)；项目三实体 |
| Filter 里抛异常能被 `@ControllerAdvice` 接住吗 | 不能（在 DispatcherServlet 之外）；要自己写响应（Security 的 EntryPoint 就是） | [17 §3.1](17-SpringMVC请求全链路与参数校验.md) |
| 三种校验异常 | `MethodArgumentNotValidException`（@RequestBody）/ `BindException`（对象绑定）/ `ConstraintViolationException`（方法级 @Validated） | `GlobalExceptionHandler:42,55,68` |
| `@Valid` 和 `@Validated` 区别 | `@Valid` 是 Jakarta 标准（参数/级联）；`@Validated` 是 Spring 的，类上加才启用方法级校验 | [17 §2.6](17-SpringMVC请求全链路与参数校验.md) |
| 404 为什么没走统一返回体 | 默认容器直接处理 404；要开 `throw-exception-if-no-handler-found` 才抛 `NoHandlerFoundException` | `GlobalExceptionHandler:102-107` |
| SSE 接口和普通接口的区别 | 返回 `SseEmitter`，请求线程立即归还，后续异步推送；ThreadLocal 需显式跨线程传播 | `ChatController:167`、`TraceInterceptor:68-71` |

### 2.4 自动装配与配置组

| 题 | 一句话答案 | 展开锚点 |
|---|---|---|
| 自动装配原理 | `@EnableAutoConfiguration` 读 `AutoConfiguration.imports` → 候选配置类 → 逐个过条件注解 → 生效注册 | [02-Spring微服务/10](10-自定义Starter与自动装配.md)、[16 §六](16-IoC容器与Bean生命周期.md) |
| 自动装配与包扫描关系 | 两套独立来源：一个管自己的包，一个管依赖 jar；项目已把 common 从包扫描迁到自动装配 | [16 §二](16-IoC容器与Bean生命周期.md) |
| `@ConditionalOnMissingBean` 的意义 | 用户自定义永远赢（Starter 的设计铁律） | `CommonAutoConfigurationTest` 的"让位"用例 |
| 条件注解有哪些 | `OnClass` / `OnProperty` / `OnMissingBean` / `OnBean` / `OnWebApplication` + `@AutoConfigureAfter` | `IdempotentAutoConfiguration`、`MessagePublisherAutoConfiguration` |
| 怎么测自动装配 | `ApplicationContextRunner` 三段式：装了没 / 该跳时跳没跳 / 用户覆盖时让没让 | `CommonAutoConfigurationTest`（5 用例） |
| `@ConfigurationProperties` 比 `@Value` 强在哪 | 一组配置聚合、松散绑定、类型不符启动报错、IDE 可提示 | `MinioProperties` 等 5+ 个 Properties 类 |
| Boot 2 和 Boot 3 自动装配区别 | `spring.factories` → `META-INF/spring/...AutoConfiguration.imports`；`@Configuration` → `@AutoConfiguration` | [02-Spring微服务/10 §五](10-自定义Starter与自动装配.md) |

### 2.5 缓存与事件组

| 题 | 一句话答案 | 展开锚点 |
|---|---|---|
| Spring Cache 三层抽象 | `CacheManager` → `Cache` → 注解 AOP（`CacheInterceptor`） | [02-Spring微服务/09](09-SpringCache与事务领域事件.md) |
| `@Cacheable` 的 sync 参数 | `sync=true` 防缓存击穿（同一 key 并发只放一个进方法） | `ProductServiceImpl:109,256` |
| 缓存一致性怎么做的 | 先更库后删缓存 + 分 cacheName TTL（30min/10min）+ 缓存删除时机权衡 | `ProductCacheConfig:43-44` |
| `@TransactionalEventListener` 四种 phase | `BEFORE_COMMIT` / `AFTER_COMMIT`（默认）/ `AFTER_ROLLBACK` / `AFTER_COMPLETION` | `OrderPaidEventListener:30` |
| 为什么支付通知要 AFTER_COMMIT | 事务回滚时监听器根本不执行 → 杜绝"没付款却推支付成功" | 同上 |
| 领域事件和 MQ 的边界 | 进程内强一致边界用 Spring 事件；跨服务用 MQ | [02-Spring微服务/09 §2.1](09-SpringCache与事务领域事件.md) |

### 2.6 Security、异步与其他生态组

| 题 | 一句话答案 | 展开锚点 |
|---|---|---|
| 为什么不用 `UserDetailsService` + Session | 微服务无状态：网关认证 + 服务授权，JWT 无 session，Security 只做资源服务器式授权 | [09-安全与设计模式/03](../09-安全与设计模式/03-SpringSecurity微服务两层安全模型.md) |
| 方法级权限怎么实现 | `@EnableMethodSecurity` + `@PreAuthorize`，底层是 AOP 拦截器读 SpEL 求值 | 同上（`UserSecurityConfig:22`） |
| `SecurityContext` 跨线程会丢吗 | 会（ThreadLocal）；且必须在 `finally` 清理防线程复用串身份 | `HeaderAuthenticationFilter:50-55` |
| 项目为什么自己建线程池而不是 `@Async` | 需要多个**命名隔离**的池（`usageExecutor`/`evalExecutor`）与不同的拒绝/优雅停机策略；`@Async` 仍可用但项目选择了显式 `ThreadPoolTaskExecutor` + 构造器注入 | `ObservabilityExecutorConfig`、`ModelUsageRecorder:106` |
| `@Scheduled` 的坑 | 默认**单线程**调度器：一个任务卡住会拖后其他任务；项目任务内部逐个 try-catch 防单条失败中断整批 | `OrderTimeoutScheduler:29,46-52` |
| `@Scheduled` 和 XXL-Job 怎么选 | 单机兜底/轻量用 `@Scheduled`；多实例只跑一次、可视化、分片用 XXL-Job（项目双轨：延迟消息为主 + 本地扫描兜底） | `OrderTimeoutScheduler:18`、[07-运维部署/05](../07-运维部署/05-XXL-Job分布式调度.md) |
| Spring State Machine 为什么不用内存持久化 | 微服务多实例无状态，DB 状态是唯一事实来源；每次请求用 DB 状态重建状态机 | [02-Spring微服务/12 §七](12-订单状态机治理.md) |
| STOMP 的 user destination 怎么找到人 | CONNECT 帧鉴权建立 Principal，user destination 按 Principal 映射 session | [02-Spring微服务/11](11-STOMP实时通知与用户目的地.md) |

---

## 三、追问链演练（面试官的第二刀最致命）

### 链 1："你们项目怎么做缓存一致性？" → 四连追问

1. **问：先更库还是先删缓存？**
   → 先更库后删缓存（`ProductServiceImpl` 的 `@CacheEvict` 注释约定）；反过来会有更大的脏读窗口。
2. **追问：删缓存失败怎么办？**
   → TTL 兜底（30min/10min 分层）+ 读路径回源重建；要更强可以上删除重试/MQ 补偿（本项目未做，如实说）。
3. **再追问：`@CacheEvict` 具体在事务的哪一刻执行？**
   → **方法返回之后执行；但与事务提交的先后由两个拦截器的 `order` 决定——默认都是 `LOWEST_PRECEDENCE`（同级），Spring 不保证谁在外**。按最坏情况（提交前删）存在"提交前并发读回填旧值"窗口，属于本仓记录的已知权衡（TTL 兜底）。
4. **再追问：那怎么做到"提交后才删"？**
   → 三条路：① 显式设 order 固定先后；② `TransactionSynchronizationManager.registerSynchronization(afterCommit)` 手动删；③ 复用 `@TransactionalEventListener(AFTER_COMMIT)` 模式（项目 `OrderPaidEventListener` 就是这个套路）。

### 链 2："讲讲 `@Transactional` 失效" → 四连追问

1. **问：最常见的失效场景？**
   → 同类自调用（`this.xxx()` 不走代理）。
2. **追问：为什么？**
   → 调用方拿到的是代理；但类内部用 `this` 调的是真身，通知没有机会执行。
3. **再追问：怎么修？**
   → 拆到另一个 Bean（推荐）/ 注入自己 / `AopContext.currentProxy()`（需 `exposeProxy`）/ 编程式事务 `TransactionTemplate`。
4. **再追问：项目里有自调用的例子吗，是 bug 吗？**
   → 用"一正一反"对比答：① **正例** `cancelExpiredOrder`——MQ/定时任务从容器拿到的是代理，外层事务正常开启，内部调私有 `doCancelOrder` 共享事务，属有意设计；② **反例（我们自己识别出的待验证隐患）** `createOrder` 通过 lambda 调 `this.doCreateOrder(...)`，**没有穿过代理**，严格推演 `doCreateOrder` 上的 `@Transactional/@GlobalTransactional` 可能失效——修复用"拆 Bean / 注入 self / 编程式事务"三选一，并用一条回滚断言集成测试锁死。主动说出这个隐患，比背十条原理更能体现工程判断。

### 链 3："自动装配到底怎么自动的？" → 四连追问

1. **问：入口在哪？**
   → `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（common 里就 5 行）。
2. **追问：那些类怎么知道该不该生效？**
   → 条件注解四件套：`@ConditionalOnClass / OnProperty / OnMissingBean / OnBean`（外加 `@AutoConfigureAfter` 控制顺序）。
3. **再追问：我自定义了同类型 Bean，会冲突吗？**
   → 不会。自动配置类在 refresh 第 ③ 阶段被处理，而用户的 `@Bean` 更早注册；`@ConditionalOnMissingBean` 检查到就不注册——**用户永远赢**。项目有测试锁定该行为。
4. **再追问：`aics.mq.type` 一个开关切换两套实现，怎么做的？**
   → 同一接口下的两个 `static @Configuration` 分支，分别带互斥的 `@ConditionalOnProperty` + `@ConditionalOnClass`；`matchIfMissing = true` 保证默认 RocketMQ 存量服务零改动。

### 链 4（加分链）："你说 Bean 生命周期里会生成代理" → 三连追问

1. **问：哪一步生成？**
   → `BeanPostProcessor.postProcessAfterInitialization`（初始化方法之后）。
2. **追问：所以 `@PostConstruct` 里调本类 `@Transactional` 方法？**
   → 无效或不可靠——那时代理还没套上。
3. **再追问：循环依赖 + AOP 会冲突吗？**
   → 不会，但需要三级缓存里的"工厂"在提前暴露时决定是否返回代理（`getEarlyBeanReference`）；构造器环依赖无解，启动报错。

## 三点五、三个最容易答错的题（避坑警示）

面试官常用"流行错误说法"钓鱼。这三题的错误答案流传最广，答对了立刻区分度拉满：

**① "单例 Bean 是线程安全的"——错。**
单例只是"容器里只有一个实例"，与线程安全无关：多线程同时读写单例的成员变量就会出问题。
正确说法：容器保证单例，线程安全自己负责——无状态 / 不可变 / 并发容器 / 不可变快照 + 原子替换（项目 `ChatModelRegistry` 的做法）。

**② "循环依赖 Spring 都能解决"——错。**
三级缓存只救"**单例 + setter/字段注入**"；构造器注入的环（A 构造器要 B、B 构造器要 A）无解，启动直接抛 `BeanCurrentlyInCreationException`。
正确答法是先反问"哪种注入方式"，再答"能不能救、怎么救"——能主动反问，这一题就赢了。

**③ "`@Transactional` 加在接口上更保险"——半错。**
Spring 能识别接口方法上的注解，但**Boot 2.x 默认 CGLIB 子类代理下接口注解不生效**（官方文档明确建议注解放在具体类上）。
正确说法：注解写在**实现类的 public 方法**上并配 `rollbackFor`——本仓 12 处 `@Transactional` 全是这么写的。

> 三题的共同点：都在考"**默认值 + 适用条件**"。背结论不如背"默认是什么、什么时候不成立"。

---

## 四、考前 30 分钟速览卡

```text
IoC        创建权反转给容器 | DI 是手段 | BeanDefinition=图纸，Bean=成品 | refresh 六阶段
生命周期    实例化→属性填充→Aware→BPP.before→@PostConstruct→BPP.after(★AOP 代理诞生)→使用→@PreDestroy
注入       构造器优先(不可变/fail-fast/可测/早暴露环) | @Qualifier 点名 | ObjectFactory 延迟获取
循环依赖    三级缓存=成品/半成品/工厂 | 只救单例+setter | 构造器环报错
AOP        Aspect/Advice(5种)/Pointcut/Weaving | @Around+proceed() 四段式 | JDK兄弟/CGLIB儿子 | Boot2.x默认CGLIB
事务         = TransactionInterceptor + ThreadLocal 连接绑定 | 7 传播 | 失效七查 | rollbackFor 全仓写死
顺序口诀     事件挂提交后 · 幂等占位早于提交 · 缓存删除与提交先后默认不保证(设order/挪AFTER_COMMIT)
MVC         Filter→DispatcherServlet→HandlerMapping→HandlerAdapter(解析+校验)→Controller→序列化→异常出口
三方对比     Filter(容器级,不知方法) / Interceptor(MVC级,知handler) / AOP(方法级,知参数返回值)
校验        @Valid(Jakarta) / @Validated(Spring 方法级) | 三异常各有 handler | 404 要开开关
自动装配     imports 清单 + 条件四件套 + @AutoConfigurationProperties | OnMissingBean 用户永远赢
项目锚点     IdempotentAspect · ProductCacheConfig · OrderServiceImpl 双层事务 · OrderPaidEventListener
            HeaderAuthenticationFilter · TraceInterceptor · ChatModelRegistry volatile 快照 · MessagePublisher 开关切换
```

---

## 五、自测记录

每轮复习后在此打分（1~5），低于 3 的回对应锚点重读：

| 主题 | 第一轮 | 第二轮 |
|---|---|---|
| Bean 生命周期八步 + 代理诞生时机 | | |
| 三级缓存与构造器环依赖 | | |
| 事务失效七查 + 传播行为 | | |
| 事务/缓存/事件/幂等四注解顺序 | | |
| 九站链路 + Filter/Interceptor/AOP 对比 | | |
| 自动装配条件四件套 + 用户覆盖 | | |
| 本项目 8 个源码锚点位置 | | |

---

## 六、怎么把这三篇"用出去"

1. **写简历**：把"Spring 生态"从"会用"升级为可举证——
   "自研 `@Idempotent` 注解组件（AOP + Redis SET NX，三层幂等定位）；common 自动装配改造（`AutoConfiguration.imports` + 条件装配 + `ApplicationContextRunner` 契约测试）；支付事件 AFTER_COMMIT 保证通知与事务一致性"。
2. **面试讲故事**：每个原理题都按"四层模板"答，并主动补一句"我们项目里……"（锚点见本篇表格）。
3. **继续深挖**：本模块 01~17 篇 + [05-AI集成](../05-AI集成/01-SpringAI框架集成/01-SpringAI入门.md)（Spring AI）→ [12-性能工程](../12-性能工程/README.md)（JVM/线程池）→ [13-稳定性工程](../13-稳定性工程/README.md)（SLO/降级）。

> 最后一句诚实提醒：面试官对"背答案"很敏感。**凡是本仓没落地的（如 `@Async` 全量改造、Vault 配置加密、灰度路由）就如实说"评估过/计划中"**，这一条比多背十道题更能建立信任。
