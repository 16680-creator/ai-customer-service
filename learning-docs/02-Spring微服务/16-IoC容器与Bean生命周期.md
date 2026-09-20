# 16-IoC 容器与 Bean 生命周期：从零开始理解「谁 new 的对象」

> 前置阅读：
> - [02-Spring微服务/01-SpringBoot核心原理](01-SpringBoot核心原理.md)——注解与依赖注入的**使用层**（入门篇）；
> - [02-Spring微服务/10-自定义Starter与自动装配](10-自定义Starter与自动装配.md)——本仓自动装配改造实录；
> - [02-Spring微服务/15-SpringAOP与声明式事务原理](15-SpringAOP与声明式事务原理.md)——替身（代理）是**在 Bean 生命周期的哪一步**产生的（本篇第五节揭晓）。
>
> 本篇回答四个问题：
> 1. `@Service` 标注的类，**是谁 new 出来的**？对象什么时候创建、什么时候销毁？
> 2. `@Autowired` 把依赖"塞"进去的动作，**发生在哪一刻**？
> 3. 循环依赖（A 依赖 B、B 依赖 A）为什么三级缓存能解决，构造器注入又为什么不行？
> 4. 自动装配到底"自动"在哪——它和包扫描是两种什么关系？

---

## ⚡ 30 秒速记卡（先背这个，再往下看推导）

```text
① IoC = 创建对象的权力反转给容器；DI = 依赖由容器递进来。BeanDefinition 是"图纸"，Bean 是"成品房"
② 生命周期八步：实例化 → 属性填充 → Aware → BPP.before → @PostConstruct → BPP.after（★代理诞生）→ 使用 → @PreDestroy
③ 构造器注入三理由：fail-fast / 可单测 / 早暴露循环依赖；三级缓存只救"单例 + setter/字段"
④ 自动装配 = imports 清单 + 条件注解；@ConditionalOnMissingBean 保证"用户自定义永远赢"
⑤ 单例 ≠ 线程安全：共享可变状态用"不可变快照 + volatile 原子替换"（见 ChatModelRegistry）
```

> 下面每一步推导都在解释这五条。读完再回来默写一遍，能全对就算过关。

---

## 一、先建立直觉：没有容器时的痛

假设没有 Spring，你要用 `OrderServiceImpl`，得自己动手：

```java
public class Main {
    public static void main(String[] args) {
        // 为了 new 一个 OrderServiceImpl，你得先把它依赖的 11 个对象全部造出来……
        OrderMapper orderMapper = new OrderMapperImpl();
        CouponMapper couponMapper = new CouponMapperImpl();
        PromotionService promotionService = new PromotionServiceImpl(couponMapper, ...);
        ProductClient productClient = new ProductClient("http://localhost:8088");
        OrderCreateLockService lock = new OrderCreateLockService(redissonClient);
        // ...省略 10 行...
        OrderService orderService = new OrderServiceImpl(orderMapper, ..., lock);
    }
}
```

三个致命问题：

1. **依赖链深了，手工组装会失控**（谁依赖谁、造的顺序、造几次）；
2. **多例/单例难管理**（同一个 service 被 3 个地方用到，new 3 个？还是共享 1 个？）；
3. **换实现要改调用方**（`new OrderMapperImpl()` 换成别的实现，所有 new 的地方都要改）。

Spring 的解法就一句话：

> **把"造对象、塞依赖、管生死"的活从程序员手里收走，交给一个总管——容器（Container）。**
> 你只负责"贴标签"（`@Service`）和"声明需求"（构造器参数），剩下的容器做。

这就是 **IoC（Inversion of Control，控制反转）**：**控制"对象怎么创建"的权力反转给了框架**；
**DI（Dependency Injection，依赖注入）**是它的实现手段：**我把依赖从外面递给你，而不是你自己去找**。

> 记忆类比（好莱坞原则）：**Don't call us, we'll call you.**
> 你不要主动去 `new` 对象、不要主动去找依赖；容器创建好一切，在合适的时候交到你手里。

---

## 二、容器启动时发生了什么：`refresh()` 全景

Spring 容器的启动，核心是一个方法：`AbstractApplicationContext#refresh()`。它像一条流水线，把"配置"一步步变成"可用的一堆 Bean"。

**简化成 6 个阶段**（记这个版本就够面试用了）：

```text
① 准备环境     prepareRefresh() / Environment
     读 application.yml、环境变量、命令行参数，决定 activeProfiles

② 拿到"配方"   obtainFreshBeanFactory() / 读配置
     把 @Component / @Bean / 自动装配 扫描成 BeanDefinition（"图纸"，不是对象！）

③ 预加工       invokeBeanFactoryPostProcessors()
     ★ 自动装配在这一阶段发生（解析 @ConditionalOnXxx，往图纸列表里增删 BeanDefinition；
       自动配置类清单就是这一步读 imports 文件得到的）

④ 注册后置处理器 registerBeanPostProcessors()
     BeanPostProcessor 是一个个"加工站"，等会儿每个 Bean 都要依次通过它们

⑤ 实例化单例   finishBeanFactoryInitialization()
     ★★ 逐个创建所有非懒加载单例 Bean —— 第四节"Bean 的一生"就发生在这里 ★★

⑥ 收尾         finishRefresh()
     启动内嵌 Tomcat、发布 ApplicationReadyEvent（此时服务可对外提供服务）
```

> **小白最容易搞混的一点**：
> **BeanDefinition 是"图纸"（描述怎么造），Bean 是"成品房"（真实对象）。**
> 扫描/条件装配动的都是图纸；对象在第 ⑤ 步才真正 `new` 出来。

### Spring Boot 在 `refresh()` 外面又包了几层

```java
// ai-cs-order/src/main/java/com/aics/order/OrderApplication.java:20-22
public static void main(String[] args) {
    SpringApplication.run(OrderApplication.class, args);   // ← 一切从这里开始
}
```

`SpringApplication.run` 的大致步骤：

```text
1. 推断应用类型（Servlet / Reactive）→ 本项目各服务是 Servlet，gateway 是 Reactive
2. 准备 Environment（把 application.yml / Nacos / 环境变量合并进来）
3. 创建容器（ApplicationContext，本项目的实现类一般是 AnnotationConfigServletWebServerApplicationContext）
4. 调用 refresh()  ← 上面那条流水线
     （"自动配置类清单"就是在 refresh 第 ③ 阶段的 ConfigurationClassPostProcessor 里，
       读每个 jar 的 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 得到的）
5. 发布 ApplicationStartedEvent / ApplicationReadyEvent（监听这两个事件可以做"启动后初始化"）
```

这也解释了为什么 `@SpringBootApplication` 能"一行注解起一个 Web 服务"：

```text
@SpringBootApplication =
    @SpringBootConfiguration  （本质是 @Configuration，本类是配置类）
  + @EnableAutoConfiguration  （去读 imports 文件，把一大串自动配置类拉进来）
  + @ComponentScan            （扫描本包及子包的 @Component/@Service/@Controller…）
```

**两套"装配来源"第一次同框，注意区分（面试高频）**：

| | 包扫描 | 自动装配 |
|---|---|---|
| 谁触发 | `@ComponentScan`（`@SpringBootApplication` 自带） | `@EnableAutoConfiguration` → 读 `AutoConfiguration.imports` |
| 扫哪里 | **你自己的包**（如 `com.aics.order`） | **依赖 jar 里的自动配置类**（如 `com.aics.common.autoconfigure.*`） |
| 条件化 | 无（扫到就注册） | 强条件化（`@ConditionalOnClass/Property/MissingBean`…） |
| 项目演进 | 各服务原本 `scanBasePackages` 扫 `com.aics.common`，**已删除** | common 改为 `imports` 登记（见 [02-Spring微服务/10](10-自定义Starter与自动装配.md)） |

### 2.1 容器家族：`BeanFactory` vs `ApplicationContext`（一句话分清）

面试常问"BeanFactory 和 ApplicationContext 什么区别"，一张表说透：

| | `BeanFactory` | `ApplicationContext` |
|---|---|---|
| 定位 | IoC 的**最小接口**（只会"按图纸造 Bean"） | `extends BeanFactory`，是**企业级容器** |
| 额外能力 | 无 | 事件发布（`ApplicationEventPublisher`）/ 资源加载 / 国际化 / 与 AOP 自动集成 |
| Bean 创建时机 | 默认**懒**：`getBean()` 时才造 | 默认**预实例化**：refresh 第 ⑤ 步把所有单例一次造齐 |
| 项目实际用的是谁 | 一般不直接用 | 各服务启动时的 `ServletWebServerApplicationContext`（gateway 是 Reactive 版） |

> **为什么要分"懒 / 预实例化"**：预实例化 = 启动时就把所有依赖问题暴露出来
> （缺 Bean、环依赖、配置错都**启动即报错**），这是 Boot "失败要早"哲学的一部分；
> 懒加载省资源，但把错误推迟到第一次使用才爆，排障更难。

---

## 三、Bean 的一生：从 class 到销毁的 8 个阶段

下面这张表是**本篇的核心**，建议背下来（面试"Bean 生命周期"的标准答案就照这张表说）：

| # | 阶段 | 发生了什么 | 项目证据 |
|---|---|---|---|
| 1 | **实例化** | 调构造器 `new` 出对象（此时依赖还没塞） | `OrderServiceImpl` 的 `@RequiredArgsConstructor` 生成构造器 |
| 2 | **属性填充** | `@Autowired` / `@Value` 注入 | `ObservabilityWebConfig` 两个 final 字段 |
| 3 | **Aware 回调** | 容器把"自身能力"告诉 Bean：`BeanNameAware`/`ApplicationContextAware`… | 项目未用 Aware，改用了更推荐的 `ObjectFactory`（见 4.3） |
| 4 | **BeanPostProcessor.before** | 加工站①：每个 Bean 都要过一遍 | 框架内部使用（如 `@Value` 解析） |
| 5 | **初始化方法** | `@PostConstruct` → `InitializingBean.afterPropertiesSet()` → `initMethod` | `ChatModelRegistry.init()`（`@PostConstruct`） |
| 6 | **BeanPostProcessor.after** | 加工站②：**AOP 代理就在这一步生成** | `IdempotentAspect` 管辖的 Bean 在此被换成"替身" |
| 7 | **使用** | 放进单例池，被注入给别人 | 全项目 |
| 8 | **销毁** | 容器关闭时：`@PreDestroy` → `DisposableBean.destroy()` | `Neo4jGraphStore.close()`（`@PreDestroy` 关 Driver） |

把八步连成一张纵向图（适合贴在工位上）：

```text
 class 文件（图纸已在前面的阶段就绪）
    │  ① 实例化：调构造器 / 工厂方法 new 出来
    ▼
 半成品对象 ──② 属性填充：@Autowired / @Value 注入依赖──► 依赖就位
    │  ③ Aware 回调：BeanNameAware / ApplicationContextAware…
    ▼
    │  ④ BeanPostProcessor.postProcessBeforeInitialization（加工站·前）
    ▼
    │  ⑤ 初始化：@PostConstruct → afterPropertiesSet() → initMethod
    ▼
    │  ⑥ BeanPostProcessor.postProcessAfterInitialization（加工站·后）
    │     ★ AOP 代理在这里"套壳"——容器外交出去的已经是替身
    ▼
 成品 Bean ──⑦ 放入单例池，被注入给别人使用──►
    │  ⑧ 容器关闭：@PreDestroy → destroy()
    ▼
   GC 回收
```

**几个必须记住的推论：**

- 第 6 步在**初始化方法之后**：所以 `@PostConstruct` 里调用自己的 `@Transactional` 方法**不会有事务**（那时它自己还是个"光身"，代理还没套上）；
- 对象的"依赖注入完成"发生在第 2 步，而"可对外服务"要到第 6 步之后——**中间任何一环抛异常，启动就失败**（fail-fast）；
- 单例 Bean 的这套流程**整个应用生命周期只走一次**；`@Scope("prototype")` 才每次取都重走一遍。

### 3.1 `@PostConstruct` 在项目里的四种典型用途

```java
// ① 初始化"注册表/缓存结构"：ChatModelRegistry（chat 模型路由）
//    ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ChatModelRegistry.java:33-41
@PostConstruct
void init() { rebuild(); }                               // 启动时按配置构建模型客户端
@EventListener(RefreshScopeRefreshedEvent.class)
public void onRefreshScopeRefreshed(...) { rebuild(); }  // Nacos 配置刷新时重建

// ② 条件性"降级开关"：VisionModelClient（视觉模型未配置就只告警不启用）
//    ai-cs-chat/src/main/java/com/aics/chat/service/impl/VisionModelClient.java:113
@PostConstruct
void init() { ... }

// ③ 启动即建外部资源：SearchServiceImpl（启动时确保 ES 索引存在）
//    ai-cs-search/src/main/java/com/aics/search/service/impl/SearchServiceImpl.java:45
@PostConstruct
public void initDefaultEsIndex() { createEsIndexIfNeeded("knowledge"); }

// ④ 注册"规则"到第三方 SDK：SentinelFlowConfig（启动时把限流规则灌进 Sentinel）
//    ai-cs-chat/src/main/java/com/aics/chat/config/SentinelFlowConfig.java:27
@PostConstruct
public void initFlowRules() { ... }
```

> **小白提示**：`@PostConstruct` 属于 `jakarta.annotation` 包（Java 标准注解，不是 Spring 的），
> Spring 只是"认"它并在第 5 步调用。写错包（老项目的 `javax.annotation`）在 Boot 3 里会**完全不生效**——是个常见坑。

### 3.2 `@PreDestroy`：优雅释放资源

```java
// ai-cs-chat/src/main/java/com/aics/chat/rag/graph/Neo4jGraphStore.java:125-127
@PreDestroy
public void close() {
    driver.close();     // 应用关闭时关闭 Neo4j Driver，避免连接/线程泄漏
}
```

### 3.3 `ApplicationRunner` vs `@PostConstruct`：启动后做事的两条路

```java
// ai-cs-pay/src/main/java/com/aics/pay/config/PaySchemaInitializer.java:16
public class PaySchemaInitializer implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) { ... 初始化表结构 ... }
}
```

| | `@PostConstruct` | `ApplicationRunner` / `CommandLineRunner` |
|---|---|---|
| 触发时机 | **单个 Bean 初始化后**（第 5 步） | **整个容器 refresh 完成后、服务对外可用前** |
| 能否安全依赖其他 Bean | 有风险（可能拿到未初始化完的 Bean） | 安全（所有 Bean 都已就绪） |
| 项目用途 | 注册表构建、索引检查、规则注册 | 建表、数据预热、启动自检 |

> **好记的话**：`@PostConstruct` 是"我准备好了"，`ApplicationRunner` 是"**全公司都准备好了**"。

### 3.4 三个小实验：亲眼看见 Bean 的一生（强烈建议动手）

1. **日志实验**：给任意一个服务类加 `@PostConstruct` / `@PreDestroy` 打日志，启停服务，观察打印顺序与时机；
2. **加工站实验**：自定义一个 `BeanPostProcessor` 打印每个 Bean 的名字，体会"**每个 Bean 都要过加工站**"：
   ```java
   @Component
   public class LifecycleLogger implements BeanPostProcessor {
       @Override
       public Object postProcessBeforeInitialization(Object bean, String beanName) {
           log.info("BPP.before → {}", beanName);   // 启动时会刷出几百行：全仓 Bean 依次路过
           return bean;
       }
   }
   ```
   （实验完记得删——生产环境别给每个 Bean 打日志）
3. **清点实验**：访问 `/actuator/beans`（`/actuator/**` 已在 user 服务 Security 白名单），看容器里到底注册了哪些 Bean、作用域是什么。
   排查"这个类到底注册没注册 / 装了几份"时，这个端点比翻代码快得多。

---

## 四、依赖注入怎么写：三种方式与项目的选择

### 4.1 三种注入方式对比

| 方式 | 写法 | 优点 | 缺点 |
|---|---|---|---|
| **构造器注入** ⭐ | `@RequiredArgsConstructor` + `final` 字段 | 依赖不可变、缺依赖**启动就报错**、利于写单测（直接 `new`）、暴露循环依赖 | 字段多时构造器长（Lombok 帮你生成） |
| setter 注入 | `@Autowired setXxx()` | 可选依赖方便 | 对象可能处于"注入一半"的状态 |
| 字段注入 | `@Autowired private X x;` | 写起来最短 | 无法设 final、单测要反射/起容器、隐藏循环依赖 |

**项目的选择：构造器注入为主流**。证据遍布全仓：

```java
// ai-cs-order/src/main/java/com/aics/order/service/impl/OrderServiceImpl.java:44-59
@Service
@RequiredArgsConstructor                       // ← Lombok 为所有 final 字段生成构造器
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;      // ← final：一旦注入不可再改
    private final ApplicationEventPublisher eventPublisher;
    ...
}
```

**为什么项目几乎不用字段注入？三个理由（面试可直接答）**：

1. **fail-fast**：少了 Bean，启动即失败，而不是运行到某行才 NPE；
2. **可测性**：单测直接 `new OrderServiceImpl(mock1, mock2, ...)`，不需要启动 Spring 容器；
3. **循环依赖早暴露**：构造器环依赖启动直接报错（详见第五节），逼你重新设计而不是"用魔法蒙混过关"。

> 项目里仍有少量字段注入：`@Value("${order.timeout-minutes:30}") private int timeoutMinutes;`（`OrderServiceImpl:61`）。
> 拆开看就明白：**它注入的不是"协作对象"，而是一个配置值**；同理 `TraceInterceptor` 里的采样配置走构造器，因为那是个完整的 Properties 对象。

### 4.2 `@Autowired` 的匹配规则（面试常问）

```text
先按【类型】找 ─┬─ 找到 1 个 ───────────► 注入
               ├─ 找到多个 ─┬─ 有 @Primary ─► 用它
               │            ├─ 有 @Qualifier("name") ─► 用指定名的
               │            └─ 都没写 ─► 按字段名/参数名匹配；再不行 → 启动报错
               └─ 找到 0 个 ─┬─ required=true（默认）► 报错
                            └─ required=false ──────► 注入 null
```

**项目实证（同名 Bean 的显式指定）**：

```java
// ai-cs-chat/src/main/java/com/aics/chat/observability/ModelUsageRecorder.java:36-42
public ModelUsageRecorder(ModelUsageProperties properties,
                          ModelUsageFeignClient modelUsageFeignClient,
                          @Qualifier("usageExecutor") ThreadPoolTaskExecutor usageExecutor) {
    // usageExecutor / evalExecutor 都是 ThreadPoolTaskExecutor 类型 → 必须用 @Qualifier 点名
}
```

### 4.3 `ObjectFactory`：一种"更晚才拿"的注入

```java
// ai-cs-chat/src/main/java/com/aics/chat/config/ObservabilityWebConfig.java:30-31
private final ObservabilityProperties observabilityProperties;
private final ObjectFactory<TraceRecorder> traceRecorderFactory;   // ← 不是直接注入 Bean，而是注入"能拿到 Bean 的工厂"

// TraceInterceptor 里：真正用的时候才取
traceRecorderFactory.getObject().record(ctx);   // TraceInterceptor.java:79
```

**为什么绕这一下？** 三个动机（也是 IoC 的进阶用法）：

1. **延迟获取**：`TraceInterceptor` 是 `new` 出来的（不是容器管理的 Bean），本来就无法被注入；
2. **打破循环**：如果 Bean A 构造时依赖 B、B 又依赖 A，用 `ObjectFactory` 把"取 B"推迟到运行时，环就断了；
3. **动态性**：每次 `getObject()` 都走容器，可拿到最新的（配合刷新作用域）。

> 顺带一个"设计小洁癖"：`ObservabilityWebConfig` 也被刻意分成两个类——
> `WebMvcConfigurer` 配拦截器只做"注册"，`TraceInterceptor` 独立成类做"逻辑"，**符合单一职责**（第七节还会遇到它）。

---

## 五、作用域与循环依赖

### 5.1 作用域（Scope）

| 作用域 | 含义 | 项目实例 |
|---|---|---|
| `singleton`（默认） | 整个容器**一个实例** | 99% 的 Bean |
| `prototype` | 每次 `getBean()` 都新建 | 项目未使用 |
| `request` / `session` | 每个 HTTP 请求/会话一个 | 项目未使用（服务无状态） |
| **refresh（Spring Cloud）** | **配置刷新时整个 Bean 重建** | `ChatModelRegistry` 关联的模型配置（见下方注释） |

项目里有一个非常值得学习的"刷新作用域 + 单例缓存"的取舍：

```java
// ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ChatModelRegistry.java:30-31（注释原文）
// 设计要点：注册表保持长生命周期单例，只换不可变 Map 快照；
//           运行时健康状态不与 RefreshScope Bean 一起重建
private volatile Map<String, ModelClientHolder> clients = Map.of();
```

**白话解释**：Nacos 配置一改，`@RefreshScope` 的 Bean 会被销毁重建——如果"模型健康状态/统计"也放在那种 Bean 里，刷新一次全丢了。
该项目的做法是：**不可变配置重建，运行时状态留在长生命周期单例里**，并用 `volatile` 引用整体替换避免看到"半成品"。

> 这是"单例 Bean 线程安全"的正面示范：**共享可变状态是单例最大的坑**，
> 这里的解法是"不可变 + 原子替换"（先构建 `next`，最后一行才 `clients = Map.copyOf(next)`）。

### 5.2 循环依赖：三级缓存到底在存什么

先描述问题：

```text
A 构造需要 B；B 构造需要 A  → 死锁式互相等待
```

Spring 对**单例 + setter/字段注入**的循环依赖有一套 "三级缓存" 解法：

| 缓存级别 | 名字 | 存什么 |
|---|---|---|
| 一级 | `singletonObjects` | **成品**（走完生命周期、该套的代理都套好了） |
| 二级 | `earlySingletonObjects` | **半成品**（实例化了、依赖还没填） |
| 三级 | `singletonFactories` | **工厂**（`ObjectFactory`，需要时能提前"生产"出早期引用） |

流程（A 与 B 互相依赖）：

```text
1. 造 A：实例化 A（半成品）→ 把 A 的早期工厂放三级缓存
2. 填 A 的依赖：发现需要 B → 去造 B
3. 造 B：实例化 B（半成品）→ 放三级缓存
4. 填 B 的依赖：发现需要 A → 三级缓存命中！通过工厂拿到 A 的"早期引用"（必要时提前生成代理）→ B 完成 → 成品入一级缓存
5. 回到 A：把 B（成品）注入 A → A 完成 → 成品入一级缓存
```

**为什么第三级缓存是"工厂"而不是直接放对象？**（高频追问）

因为 A 可能还要被 AOP 包一层代理：工厂的 `getObject()` 能决定"此刻提前返回的是原始对象还是代理对象"。
如果只有一个二级缓存存死对象，就会产生"循环依赖的一方注入了未代理的对象"的不一致问题。

**构造器注入的循环依赖为什么解决不了？**

因为构造器注入要求"**在实例化 A 之前就拿到 B**"——而 A 连半成品都还没有，三级缓存无从谈起，只能抛 `BeanCurrentlyInCreationException`。

> **面试结论**：Spring 只能解决**单例 + 字段/setter 注入**的循环依赖；构造器循环依赖 = 设计问题，拆类 / 用 `ObjectFactory` / `@Lazy` 才是正道。
> 本仓 **grep `@Lazy` 零命中**——不是因为用不上，而是因为全仓以构造器注入为主，环依赖在启动时就被发现了（这就是"早暴露"的价值）。

---

## 六、自动装配：IoC 的"条件化批量装配"

自动装配不是新机制，它只是 **IoC 的"免配置分发版"**：让别人 jar 里的 Bean 自动进入你的容器，而且"合适才进"。

### 6.1 入口文件：只有 5 行字的清单

```text
// ai-cs-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.aics.common.autoconfigure.CommonWebAutoConfiguration
com.aics.common.autoconfigure.MinioAutoConfiguration
com.aics.common.ai.embedding.EmbeddingAutoConfig
com.aics.common.idempotent.IdempotentAutoConfiguration
com.aics.common.mq.MessagePublisherAutoConfiguration
```

每个服务引入 `ai-cs-common` 后，Boot 启动时读到这个文件，会**候选**这 5 个自动配置类；
但"候选 ≠ 生效"——**最终进不进容器由每个类的条件注解决定**。

### 6.2 条件注解：项目里用到的全都在这里

| 条件注解 | 含义 | 项目实证 |
|---|---|---|
| `@ConditionalOnClass` | classpath 有某类才生效 | `MinioAutoConfiguration`（有 `MinioClient`）；`IdempotentAutoConfiguration`（有 `StringRedisTemplate`） |
| `@ConditionalOnMissingBean` | 用户没自定义才兜底 | 全项目 5 个自动配置**每处都写** |
| `@ConditionalOnProperty` | 配置开关控制 | `EmbeddingAutoConfig`（`aics.ai.embedding.provider=local`，缺省也生效）；`MessagePublisherAutoConfiguration`（`aics.mq.type`） |
| `@ConditionalOnWebApplication(SERVLET)` | 仅在 Servlet Web 应用生效 | `CommonWebAutoConfiguration`（**真实存在的坑**：网关也依赖 common） |
| `@ConditionalOnBean` | 某 Bean 已存在才生效 | `MessagePublisherAutoConfiguration`（要求 `RocketMQTemplate`/`KafkaTemplate` 存在） |
| `@AutoConfigureAfter` | 排在指定自动配置之后 | 同上（必须排在 RocketMQ/Kafka 官方自动配置之后） |

> **为什么这一条不是"教科书摆设"**：`ai-cs-gateway` 也依赖 `ai-cs-common`，但它基于 WebFlux——
> `ai-cs-gateway/pom.xml:43-44` 的注释写着"排除 Spring MVC，网关基于 WebFlux，两者不兼容"。
> 如果 `GlobalExceptionHandler`（依赖 `HttpServletRequest`、属于 Spring MVC 的 `@RestControllerAdvice`）
> 被装进网关，启动就会因缺少 MVC 相关类而失败。
> `@ConditionalOnWebApplication(type = SERVLET)` 就是这堵防火墙：**同一份自动装配，
> 在 11 个 Servlet 服务里生效，在 Reactive 网关里自动跳过。**

### 6.3 三个"看得懂就懂了"的真实片段

**① 有 Redis 才装切面，没有就整体跳过（依赖 optional 的威力）**

```java
// ai-cs-common/src/main/java/com/aics/common/idempotent/IdempotentAutoConfiguration.java:20-31
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)     // ← 没引 Redis 的服务，整个装配直接跳过
@ConditionalOnProperty(prefix = "aics.idempotent", name = "enabled",
                       havingValue = "true", matchIfMissing = true)   // ← 可整体关闭
@EnableConfigurationProperties(IdempotentProperties.class)
public class IdempotentAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean        // ← 业务方自己定义了 IdempotentAspect 就让位
    public IdempotentAspect idempotentAspect(StringRedisTemplate redisTemplate,
                                            IdempotentProperties properties) { ... }
}
```

**② 一个开关切换两套消息中间件实现（同一接口两个分支互斥装配）**

```java
// ai-cs-common/src/main/java/com/aics/common/mq/MessagePublisherAutoConfiguration.java:40-71（节选）
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "aics.mq", name = "type",
                       havingValue = "rocketmq", matchIfMissing = true)   // 默认 RocketMQ
static class RocketMqPublisherConfiguration { ... }

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "aics.mq", name = "type", havingValue = "kafka")
static class KafkaPublisherConfiguration { ... }
```

**③ 用户自定义 Bean 一定赢：`@ConditionalOnMissingBean` 的"让位"语义**

```java
// ai-cs-common/src/main/java/com/aics/common/ai/embedding/EmbeddingAutoConfig.java:18-22
@Bean
@ConditionalOnMissingBean(EmbeddingModel.class)   // 有别的 EmbeddingModel（如 openai）就不注册本地哈希版
public EmbeddingModel hashEmbeddingModel() {
    return new HashEmbeddingModel();
}
```

**为什么"用户一定赢"？** 自动配置类在 refresh 的第 ③ 阶段处理，而用户自己的 `@Bean`/`@Component` 更早入册；
`@ConditionalOnMissingBean` 检查时看到用户已经注册了，就自动退让。**这是所有 Starter 的设计铁律**——你永远可以覆盖框架的默认实现。

### 6.4 `@ConfigurationProperties`：配置绑定的正确姿势

项目的配置类统一用 `@ConfigurationProperties(prefix = "...")` + `@EnableConfigurationProperties` 注册，而不是到处 `@Value`：

```java
// ai-cs-common/src/main/java/com/aics/common/storage/MinioProperties.java:9-26
@Data
@ConfigurationProperties(prefix = "aics.minio")     // 绑定 aics.minio.*
public class MinioProperties {
    private String endpoint = "http://...:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "ai-cs";
    private String publicEndpoint;
}
```

对比 `@Value` 的优势：

| | `@Value("${aics.minio.endpoint}")` | `@ConfigurationProperties` |
|---|---|---|
| 一组配置 | 每个字段一行，散落各处 | **一个类聚合**，一处维护 |
| 松散绑定 | 不支持 | 支持 `access-key` / `accessKey` / `ACCESS_KEY` 多种写法 |
| 类型安全 | 字符串转换手写错才报错 | 类型不符**启动即报错** |
| IDE 提示 | 无 | 有（可生成 metadata） |

> 项目里 `@Value` 只出现在少数"单值/带默认值"场景（如 `OrderServiceImpl:61` 的 `order.timeout-minutes:30`）。
> 需要一组配置时一律 `Properties` 类：`MinioProperties` / `MqProperties` / `IdempotentProperties` /
> `ModelRouterProperties` / `ObservabilityProperties` ……（grep `@ConfigurationProperties` 可见全貌）。

### 6.5 怎么验证自动装配：`ApplicationContextRunner`

自动装配的正确性不需要启动整个应用，Boot 提供了"轻量容器跑一跑"的工具：

```java
// ai-cs-common/src/test/java/com/aics/common/autoconfigure/CommonAutoConfigurationTest.java:24-27
@Test
@DisplayName("Servlet Web 应用 - 自动提供 GlobalExceptionHandler")
void webAutoConfigurationShouldProvideExceptionHandler() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonWebAutoConfiguration.class))
            .run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
}
```

同一个测试类里还有两个"必考场景"：

```java
// 条件不满足 → 不装配（Embedding provider=openai 时本地模型退场）
.withPropertyValues("aics.ai.embedding.provider=openai")
.run(context -> assertThat(context).doesNotHaveBean(HashEmbeddingModel.class));

// 用户自定义 → 自动配置让位（MinioClient 只应有一个）
.withBean(MinioClient.class, () -> ...)
.run(context -> assertThat(context).hasSingleBean(MinioClient.class));
```

> 记住这个三段式：**"装了没 / 该跳过时跳没跳 / 用户覆盖时让没让"**——写 Starter 的验收就是这三条。

---

## 七、新手最常踩的 6 个坑

1. **Bean 不在扫描范围**：`@Service` 写在 `com.aics.xxx` 之外，启动后 `@Autowired` 报 "no such bean"。
   项目里 `@SpringBootApplication(scanBasePackages = {"com.aics.order"})`（`OrderApplication:13`）显式圈定范围——**扫描范围必须覆盖你所有的组件包**。
2. **自己 `new` 出来的对象没有"魔法"**：`new OrderServiceImpl(...)` 得到的对象**不是**容器里的 Bean，
   它身上的 `@Transactional` / `@Cacheable` / `@Idempotent` **全部失效**（因为没套代理，回到第 15 篇第一节）。
3. **同类型多 Bean 注入报错**：`ThreadPoolTaskExecutor` 项目里有 `usageExecutor` 和 `evalExecutor` 两个 →
   必须 `@Qualifier("usageExecutor")` 点名（`ModelUsageRecorder:38`）。
4. **`@ConfigurationProperties` 忘了 `@EnableConfigurationProperties`（或 `@Component`）**：类写好了但没被注册，绑定不生效。
5. **`@PostConstruct` 里做重活 / 调需要代理的方法**：启动期阻塞，且此时**代理还没套上**（第 6 步在之后），`@Transactional` 无效。
6. **单例 Bean 里放可变状态**：`private List<X> cache = new ArrayList<>()` 被多线程读写 → 数据错乱。
   正解看 `ChatModelRegistry`：**不可变快照 + volatile 原子替换**（5.1 节）。

---

## 八、面试要点总结

> 一句话主线：**IoC 把"创建与装配对象"的权力反转给容器；容器按 refresh() 流水线把图纸（BeanDefinition）变成成品（Bean）；每张成品都走完"实例化→注入→初始化→代理→使用→销毁"的一生。**

```text
关键词：
IoC/DI · BeanDefinition（图纸）vs Bean（成品）· refresh() 六阶段 · 实例化→属性填充→@PostConstruct→BPP.after（AOP 代理在此生成）→@PreDestroy
构造器注入三理由（fail-fast / 可测 / 早暴露循环依赖）· @Qualifier 点名 · ObjectFactory 延迟获取
三级缓存：成品/半成品/工厂；只救单例+setter，不救构造器环依赖
自动装配 = imports 清单 + 条件注解（OnClass/OnProperty/OnMissingBean/OnWebApplication）+ @ConfigurationProperties + ApplicationContextRunner 三段式测试
项目锚点：imports 5 行 · ChatModelRegistry @PostConstruct+volatile 快照 · Neo4jGraphStore @PreDestroy · PaySchemaInitializer ApplicationRunner · MessagePublisherAutoConfiguration 开关切换
```

### 高频追问链

1. **问：依赖注入有几种方式，你们项目用哪种？**
   答：构造器/setter/字段三种；项目以**构造器注入**（`@RequiredArgsConstructor` + final）为主——理由：启动期 fail-fast、便于单测、循环依赖早暴露。
2. **追问：那循环依赖 Spring 怎么处理？**
   答：三级缓存（成品/半成品/工厂）解决**单例 + 字段/setter** 的环；构造器环无解，启动直接报错。
3. **再追问：为什么第三级缓存放工厂而不是对象？**
   答：为了在"提前暴露引用"时能决定返回原始对象还是 AOP 代理（`getObject()` 支持 `getEarlyBeanReference` 逻辑），避免循环依赖注入"未代理对象"的不一致。
4. **再追问：`@PostConstruct` 里调本类 `@Transactional` 方法行不行？**
   答：不行（或不可靠）。AOP 代理在 `BeanPostProcessor.after` 阶段才生成，晚于 `@PostConstruct`。
5. **再追问：自动装配和包扫描什么关系？**
   答：两套独立的 Bean 注册来源；项目已把 common 从"包扫描"迁移为"自动装配"（`AutoConfiguration.imports` + 条件注解），未引依赖/条件不满足的服务零影响。

---

## 学习检查清单

- [ ] 能说出 IoC 与 DI 的区别（控制权反转 vs 注入这个动作）
- [ ] 能画出 refresh() 的六阶段，并指出"自动装配发生在第几阶段"
- [ ] 能按顺序背出 Bean 生命周期的 8 步，并解释"AOP 代理在哪一步生成"
- [ ] 能说出构造器注入的三个理由，并能解释为什么它能"早暴露循环依赖"
- [ ] 能讲清三级缓存每一级存什么、为什么第三级是工厂
- [ ] 能读懂 `IdempotentAutoConfiguration` 的四个注解决定"装不装/退不退"
- [ ] 会用 `ApplicationContextRunner` 写出"装了没/跳没跳/让没让"三段式测试
- [ ] 能列举项目里 4 个 `@PostConstruct` 的真实用途和 1 个 `ApplicationRunner`

## 下一步

- [17-SpringMVC 请求全链路与参数校验](17-SpringMVC请求全链路与参数校验.md)：容器把 Bean 都造好了，一个 HTTP 请求进来之后，从 Filter 到 Controller 到异常出口走了哪些站；
- [18-Spring 面试专练与追问链](18-Spring面试专练与追问链.md)：把 15/16/17 三篇串成一份可直接背的面试答卷。
