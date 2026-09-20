# 15-Spring AOP 与声明式事务原理：从零开始理解「代理」

> 前置阅读：
> - [01-Java基础/06-反射动态代理与SPI](../01-Java基础/06-反射动态代理与SPI.md)——JDK 动态代理 / CGLIB 的 **Java 语言层**原理（本篇讲它们在 Spring 里怎么被使用）；
> - [02-Spring微服务/09-SpringCache与事务领域事件](09-SpringCache与事务领域事件.md)——`@Cacheable` 与 `@TransactionalEventListener` 的**使用层**；
> - [02-Spring微服务/14-分布式幂等设计](14-分布式幂等设计.md)——本仓第一个真实 `@Aspect` 的成品。
>
> 本篇回答三个问题：
> 1. 一个注解（`@Transactional` / `@Cacheable` / `@Idempotent` / `@PreAuthorize`）为什么能"自动干活"？
> 2. 它什么时候**不干活**（失效场景）——这是面试与线上事故的高发区；
> 3. 项目里哪些代码是代理在背后替你执行。

---

## ⚡ 30 秒速记卡（先背这个，再往下看推导）

```text
① 注解是"贴纸"，代理是"执行者"——调用不经过代理，注解就是废纸（自调用是头号坑）
② @Transactional = 事务拦截器 + ThreadLocal 绑定连接 —— 换线程就失效
③ 失效七查：自调用 / 非 public / 吞异常 / 受检异常没 rollbackFor / 子线程 / 非 InnoDB / 传播行为
④ 叠注解只背两条"确定"的：事件监听器挂"提交后"；幂等占位早于"提交"；缓存删除与提交的先后不保证
⑤ Boot 2.x 默认全用 CGLIB 代理（proxy-target-class=true），有接口也一样
```

> 下面每一步推导都在解释这五条。读完再回来默写一遍，能全对就算过关。

---

## 一、先建立直觉：没有 AOP 的世界长什么样

假设没有 Spring AOP，你要给"下单"方法加 4 件事：开启事务、记录耗时、校验权限、打印日志。

代码会变成这样（**伪代码，每个方法都要重复一遍**）：

```java
public OrderVO createOrder(...) {
    checkPermission();          // ← 每个方法都要写一遍
    long start = System.currentTimeMillis();   // ← 每个方法都要写一遍
    TransactionStatus tx = txManager.begin();  // ← 每个方法都要写一遍
    try {
        OrderVO vo = 真正的业务逻辑();          // ← 只有这一行是"业务"
        tx.commit();
        return vo;
    } catch (Exception e) {
        tx.rollback();
        throw e;
    } finally {
        log.info("耗时 {}", System.currentTimeMillis() - start);
    }
}
```

问题：**业务代码被"非业务代码"淹没**，而且只要漏写一处，权限/事务就出缺口。

AOP 的思路：**把"业务"和"通用逻辑"分开写**，让框架在调用业务方法的前后"自动插入"通用逻辑。

> 生活类比：你不是在公司门口每次自己刷门禁、登记、开灯——这些由"前台"统一做。
> 你只管进去干活。AOP 就是给方法配了一个**前台**（代理对象）。

```text
调用方 ──► [代理对象] ──► 真实业务对象
              │
              ├─ 进门之前：开事务 / 查权限 / 记开始时间
              ├─ 出来之后：提交事务 / 删缓存 / 记耗时
              └─ 出事之后：回滚事务 / 释放占位
```

**关键结论（记住这一句，全篇都从它推导）**：

> **调用方拿到的不是业务对象本身，而是一个替身（代理）。
> 注解生效的位置不在你的方法里，而在替身进出你方法的那一圈。**
>
> 所以：**绕开替身 → 注解失效**（后面第六节全靠这句解释）。

---

## 二、四个名词，一张表说清

学 AOP 最先被术语劝退。先用"前台"类比记住四个词：

| 术语 | 英文 | 前台类比 | 项目实例 |
|---|---|---|---|
| **切面** | Aspect | 整个前台团队（一套横切逻辑） | `IdempotentAspect`（幂等切面） |
| **通知** | Advice | 前台具体动作：进门查证件、出门登记 | `@Around`（环绕）、`@Before`、`@After`、`@AfterReturning`、`@AfterThrowing` |
| **切点** | Pointcut | 哪些人需要走前台（筛选规则） | `@annotation(idempotent)`——"所有打了 `@Idempotent` 的方法" |
| **织入** | Weaving | 把前台安插到门口的施工过程 | 容器启动时创建代理对象（并非改字节码，是运行期代理） |

五种通知的执行顺序（用一个方法 `doSomething()` 举例）：

```text
          ┌─────────────── @Around 开始 ───────────────┐
调用 ──►  │  @Before                                   │
          │      ┌───── 真实方法 doSomething() ─────┐    │
          │      │  正常返回 → @AfterReturning       │    │
          │      │  抛异常   → @AfterThrowing        │    │
          │      └───── @After（永远执行，类似 finally）┘   │
          └─────────────── @Around 结束 ───────────────┘
```

**记住**：

- `@Around` 最强大：能拿到 `ProceedingJoinPoint`，可以决定"放行/拦截/改参数/改返回值"——项目里的幂等切面、`@Cacheable`、`@Transactional` 底层都是环绕通知；
- `@After` 相当于 `finally`，`@AfterThrowing` 只在异常时执行。

---

## 三、项目里的第一个真实切面：`IdempotentAspect` 逐段拆解

位置：`ai-cs-common/src/main/java/com/aics/common/idempotent/`（5 个类，组件全貌见 [14-分布式幂等设计](14-分布式幂等设计.md)）。这是**全仓唯一手写的 `@Aspect`**，所以拿它当教学样本最合适。

### 3.1 先看注解：它只是一个"贴纸"，什么都不会做

```java
// Idempotent.java —— 只有三样东西：key、ttl、message
@Documented
@Target(ElementType.METHOD)          // 只能贴在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行期还能读到（关键！反射/切面靠它）
public @interface Idempotent {
    String key();                     // SpEL 表达式，如 "'pay:callback:' + #orderNo"
    long ttlSeconds() default 300;    // 去重窗口
    String message() default "重复请求，请勿重复提交";
}
```

> **小白最容易误解的点**：注解本身**没有任何执行能力**。
> 它只是一张贴纸，真正"干活"的是**读到这张贴纸的人**（切面）。
> 类比：处方（注解）不会治病，执行处方的是药房（切面）。

`@Retention(RUNTIME)` 是切面能读到它的前提——编译后注解信息被保留，切面运行时反射读取（`@Retention` 三级：SOURCE 只在源码 / CLASS 只在字节码 / RUNTIME 运行期可读）。

### 3.2 再看切面：一段"围住"业务方法的代码

```java
// IdempotentAspect.java:29-50
@Aspect                       // ① 声明"我是一个切面"
public class IdempotentAspect {

    @Around("@annotation(idempotent)")   // ② 切点+通知：贴了 @Idempotent 的方法都归我管
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // ③ 进门前：算 Redis key（SpEL 解析）
        String redisKey = properties.getKeyPrefix() + resolveKey(joinPoint, idempotent);
        Duration ttl = Duration.ofSeconds(idempotent.ttlSeconds());

        // ④ 尝试"占位"：SET NX EX——抢到才放行
        boolean acquired;
        try {
            acquired = Boolean.TRUE.equals(ops.setIfAbsent(redisKey, TOKEN, ttl));
        } catch (RuntimeException e) {
            log.warn("幂等组件 Redis 异常，fail-open 放行 key={}", redisKey, e);
            return joinPoint.proceed();     // Redis 挂了也要放行（降级思想）
        }
        if (!acquired) {
            throw new IdempotentRejectException(idempotent.message());  // 重复请求 → 409
        }

        // ⑤ 放行业务方法（joinPoint.proceed() 就是"调用真实方法"）
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            tryRelease(redisKey);           // ⑥ 业务失败：释放占位，允许重试
            throw t;                        //    异常原样上抛，不吞
        }
    }
}
```

**这一段代码里有 4 个"通用套路"，你在任何切面里都会见到：**

| 套路 | 本切面的体现 | 你将来写切面也要这么做 |
|---|---|---|
| 前置：条件判断 | `setIfAbsent` 抢占位 | 权限不够就抛异常，别放行 |
| 中置：放行业务 | `joinPoint.proceed()` | **必须调用它**，否则业务方法根本不执行 |
| 后置：成功收尾 | 占位保留到 TTL 到期 | 如删缓存、提交事务 |
| 异常：补偿 + 原样上抛 | `tryRelease` 后 `throw t` | **不要吞异常**，让上层决定 |

### 3.3 一个关键参数：切点表达式 `@annotation(idempotent)`

`@Around` 括号里是**切点表达式**，决定"哪些方法进这个前台"。常见三种写法：

| 写法 | 含义 | 项目实例 |
|---|---|---|
| `@annotation(idempotent)` | 贴了某注解的方法 | `IdempotentAspect` |
| `execution(* com.aics.order.service..*.*(..))` | 包路径匹配（按方法签名） | 教科书写法，本项目未用 |
| `@within` / `@target` | 类上贴了注解 | 少用 |

> 小技巧：`@annotation(idempotent)` 里的 `idempotent` 会自动绑定到方法参数上（见 `around(..., Idempotent idempotent)`），
> 于是你能直接读注解的属性（`idempotent.key()` / `ttlSeconds()` / `message()`）——这是"切面读贴纸"的落地点。

### 3.4 执行时序（把这条图刻进脑子）

```text
用户请求
   │
   ▼
[IdempotentAspect.around]  ← 代理对象
   │  ① 解析 SpEL → key = aics:idem:pay:callback:O1
   │  ② Redis SET NX EX（第一次成功 / 第二次失败）
   │       ├─ 失败 ► 抛 IdempotentRejectException(409)，业务方法【完全没被执行】
   │       └─ 成功 ► 往下
   │  ③ joinPoint.proceed()
   ▼
[真实业务方法]  ← 购物车/支付/下单等
   │  业务异常 ► 回到 ④ 的 catch：删除 key，再抛
   │  正常返回 ► key 留到 TTL 自然过期
   ▼
返回给用户
```

**最容易考的细节**：重复请求被拒绝时，业务方法**一次都没被触达**（测试 `IdempotentAspectTest#duplicateCallShouldReject` 用 `assertThat(target.calls).isZero()` 锁死了这个语义）。

### 3.5 怎么测试切面：`AspectJProxyFactory`

切面代码不适合启动整个 Spring 容器来测。项目里的做法是"手工造一个代理"：

```java
// ai-cs-common/src/test/java/com/aics/common/idempotent/IdempotentAspectTest.java:55-59
private DemoService proxy(DemoService t) {
    AspectJProxyFactory factory = new AspectJProxyFactory(t);   // 以真实对象为target
    factory.addAspect(new IdempotentAspect(redisTemplate, properties)); // 手工加切面
    return factory.getProxy();    // 返回代理对象，注解从此生效
}
```

- 这是纯单元测试：`StringRedisTemplate` 用 Mockito 模拟，**不依赖真实 Redis**；
- 换句话说，测试里做的正是 Spring 容器启动时会做的事（给对象套代理）——**理解了这段测试，就理解了"注解是怎么生效的"**。

---

## 四、代理是谁？JDK 动态代理 vs CGLIB

Spring 要造"替身"，只有两条路（Java 层原理见 [01-Java基础/06](../01-Java基础/06-反射动态代理与SPI.md)）：

| | JDK 动态代理 | CGLIB |
|---|---|---|
| 前提 | 目标类**实现接口** | 目标类**没有接口**也可以 |
| 生成物 | `com.sun.proxy.$ProxyXX`（与目标类**兄弟关系**：同接口不同类） | `目标类$$EnhancerBySpringCGLIB$$xxx`（目标类的**子类**） |
| 能否代理 final 类/方法 | 接口方法可代理 | **final 方法不能被增强**（无法重写） |
| **Spring Boot 2.x 起默认** | **默认不用**（`spring.aop.proxy-target-class=true`） | **默认统一用 CGLIB**——即使类实现了接口；改 `spring.aop.proxy-target-class=false` 才切回 JDK |

**项目里的三处证据**：

1. **注入的是接口，代理对调用方透明**：`CartController` 注入的类型是 `CartService`（接口），实现是 `CartServiceImpl`。
   注意按 Boot 2.x 默认配置，实际生成的是 CGLIB 子类代理（`CartServiceImpl$$EnhancerBySpringCGLIB`，同时实现了 `CartService`），
   按接口注入照常工作——**你感知不到代理是哪种，但排查时请按"默认 CGLIB"来想**。
2. **MyBatis Mapper 也是代理**：`CartItemMapper` 你从来没写过实现类，为什么能注入？
   ——MyBatis 的 `MapperFactoryBean` 为每个 Mapper 接口注册了一个 **JDK 动态代理 Bean**，调用 `selectById` 时被代理拦截去执行 SQL。这也是"代理"最震撼的用法：**接口当实现用**。
3. **`@Configuration(proxyBeanMethods = false)`**：`MessagePublisherAutoConfiguration` 里的两个 `@Configuration(proxyBeanMethods = false)` 表示"本配置类不用被 CGLIB 增强"。
   为什么？——`@Configuration` 默认会被 CGLIB 代理，以保证 `@Bean` 方法互调时仍返回同一个单例；关掉它能省一次代理创建（Boot 的自动配置类普遍这么写，因为内部都是 `static class` 不互相调用）。

> **一句话记忆**：
> 机制上：有接口可造"兄弟"（JDK），没接口造"儿子"（CGLIB）；
> **Boot 2.x 默认全用 CGLIB**；无论哪种——**没有替身 → 注解不生效**。

---

## 五、`@Transactional` 的全部秘密：AOP + 一个 ThreadLocal 连接

### 5.1 一句话原理

> `@Transactional` = **环绕通知**（开事务/提交/回滚） + **ThreadLocal 保存当前连接**（保证同一个线程里多次 DB 操作走同一个事务）。

拆开看执行流程：

```text
[TransactionInterceptor]（也是个切面，"替身"的一层）
   ① 进入方法前：事务管理器 getTransaction()
      → 从连接池取一条 Connection（DataSourceTransactionManager）
      → 关闭自动提交、开启事务
      → 把 Connection 塞进 TransactionSynchronizationManager 的 ThreadLocal
      → 打标记："本线程已有活跃事务"（isActualTransactionActive = true）
   ② joinPoint.proceed() → 你的业务方法
      → 方法里的 Mapper 执行 SQL 时，MyBatis 从 ThreadLocal 拿同一条 Connection
      → 于是多条 SQL 在同一个事务里（要么全提交，要么全回滚）
   ③ 正常返回：commit  → ④ 抛异常：rollback（默认只回滚 RuntimeException/Error）
   ⑤ finally：清 ThreadLocal，把连接还回连接池
```

**为什么必须是 ThreadLocal？** 因为一个连接池里有很多连接，线程 A 用连接 1、线程 B 用连接 2；"当前线程正在用哪条连接"这件事，只能存在线程自己的口袋里。

### 5.2 项目实证：一个方法上叠了两层事务注解

```java
// ai-cs-order/src/main/java/com/aics/order/service/impl/OrderServiceImpl.java:78-79
@GlobalTransactional(rollbackFor = Exception.class, name = "order-create")  // ① Seata 全局事务（跨服务）
@Transactional(rollbackFor = Exception.class)                              // ② Spring 本地事务（本服务 DB）
public OrderVO doCreateOrder(Long userId, List<Long> cartItemIds, Long couponId, String paymentMethod) {
    ...
}
```

两层的分工（详见 [02-Spring微服务/06-Seata分布式事务AT模式](06-Seata分布式事务AT模式.md)）：

- `@Transactional`：管**本服务**的 order/cart/coupon 三张表；
- `@GlobalTransactional`：管**跨服务**（order → product 扣库存 → coupon），Seata 用 XID 把各服务的本地事务串成一个全局事务。

> 另外注意 `createOrder`（入口）和 `doCreateOrder` 被拆成了两个方法（`:73-79`）。**设计意图**很清楚：
> 入口先加 Redis 分布式锁，**锁在全局事务外层**，拿到锁才进入事务边界，避免"锁内空占事务资源"。
>
> ⚠️ **但这里恰好是"代理视角"的高危点（本项目待验证的隐患，值得你亲手确认）**：
> `createOrder` 内部是通过 lambda 调 `this.doCreateOrder(...)`
> （`orderCreateLockService.withCreateLock(userId, () -> doCreateOrder(...))` 只是"加锁后执行回调"，
> 并没有让调用穿过另一个 Bean 的代理）——严格按 AOP 语义推演，**这是同类自调用，
> `doCreateOrder` 上的 `@Transactional` / `@GlobalTransactional` 可能并未生效**
> （代理套在 `createOrder` 这层，而 `createOrder` 自身没有任何注解）。
> 佐证：`OrderServiceTest` 用 `@InjectMocks` 直接持有实现类实例来测，**根本不存在代理**，所以单元测试发现不了这个问题。
>
> 建议的验证与修复：
> 1. 加一条**回滚断言**集成测试：让第二次扣库存抛异常，断言 `orders` 表里查不到半成品订单（`@SpringBootTest`，从容器里拿 `OrderService`）；
> 2. 若确认失效，按第六节场景 1 的三选一修复——**拆出独立 Bean（如 `OrderTransactionalService`）最稳**，也可注入 self 或 `AopContext.currentProxy()`；
> 3. 这个案例的教训值得背：**"锁在事务外"的设计要成立，前提是两次调用跨越代理边界**——设计对了、调用方式错了，等于没做。

### 5.3 七个传播行为（面试必问，别死背，用一句话理解）

| 传播行为 | 人话解释 | 典型用途 |
|---|---|---|
| `REQUIRED`（默认） | **有就加入，没有就开一个** | 90% 场景 |
| `REQUIRES_NEW` | **不加入，自己另开一个**（原事务挂起） | "不管主流程成败都要留痕"：写审计日志 |
| `NESTED` | 嵌套：内部用 savepoint，内部失败可只回滚内部 | 批量导入"单条失败不影响整批" |
| `SUPPORTS` | 有就用，没有也行（无所谓） | 查询方法 |
| `NOT_SUPPORTED` | 不支持事务：**有也挂起** | 事务里做耗时操作（如调 LLM） |
| `MANDATORY` | **必须已有事务**，否则报错 | 强约束"必须被事务方法调用" |
| `NEVER` | **必须没事务**，否则报错 | 极少用 |

> 记忆口诀：**REQUIRED 最常用；NEW 是各过各的；NESTED 是子报表；其余三个是"有/无所谓/不许有"**。

### 5.4 其余常用属性（项目里的写法）

| 属性 | 项目实证 | 说明 |
|---|---|---|
| `rollbackFor = Exception.class` | `OrderServiceImpl`、`ProductServiceImpl` **全部**这么写 | 默认只回滚运行时异常；**显式写死最保险**（收到受检异常也回滚） |
| `readOnly = true` | 项目未使用 | 查询方法可加，MySQL 侧可优化 |
| `timeout` | 项目未使用 | 超过秒数自动回滚，防慢 SQL 拖死连接 |
| `isolation` | 项目未使用 | 隔离级别，改用 DB 默认 + 业务层取舍 |

---

## 六、失效的六大场景（把"代理视角"用到底）

### 场景 1：同类自调用（最高频，面试必考）

```java
@Service
public class DemoService {
    public void a() {
        this.b();          // ✗ this 是"真身"，不是"替身"！
    }

    @Transactional
    public void b() { /* 事务不会开启 */ }
}
```

外面调用 `a()` 时进的是替身，但 `a()` 内部写 `this.b()` 用的是**真身的 this**，替身根本没参与 → 注解失效。

**项目里的两个对照，一正一反**：

- ✅ **正例** `cancelExpiredOrder`：MQ 监听器 / 定时任务从容器里拿到的 `orderService` 是**代理**，注解正常生效；
  它内部调用私有 `doCancelOrder` 共享外层事务——不是"想开新事务却没开"，而是有意设计；
- ⚠️ **反例（自调用）** `createOrder → 锁 → this.doCreateOrder`：`createOrder` 自身没有注解，
  锁服务的回调又是在 `this` 上直调，**这是自调用**（完整分析与建议见 5.2 节），
  属于"待验证 + 待修复"的真实样本——比任何伪代码都更有说服力。

> 修复自调用的三种办法（面试会追问）：
> ① 拆到另一个 Bean（最推荐，项目采用的就是这种"分层委托"思路）；
> ② 注入自己（`@Autowired private OrderService self;`，Spring 4.3 后也可 `AopContext.currentProxy()` 需开 `exposeProxy`）；
> ③ 改用编程式事务 `TransactionTemplate`（如下）。

方案 ② 的可运行样子（最容易写错的就是"以为注入了自己"——注入的必须是**代理**）：

```java
@Service
public class DemoService {
    @Lazy @Autowired
    private DemoService self;     // ← 容器递进来的是"替身"，不是 this

    public void entry() {
        self.doInTx();            // ✓ 经过代理 → @Transactional 生效
        // this.doInTx();         // ✗ 直调真身 → 失效（本节场景 1 的坑）
    }

    @Transactional(rollbackFor = Exception.class)
    public void doInTx() { ... }
}
```

> `@Lazy` 用来打破"自己依赖自己"的构造环；真正带着注解干活的是代理对象。

### 场景 2：方法不是 public

Spring 的 AOP 通知对**非 public 方法**（private/protected/包级）默认不生效——代理无法拦截它们（CGLIB 也不能重写 private）。
注意：`@PostConstruct`、`@Scheduled` 这类"容器直接调用"的方法从设计上就不该需要事务。

### 场景 3：异常被吞掉 / 抛的是受检异常

```java
@Transactional
public void a() {
    try {
        orderMapper.insert(x);
        throw new BusinessException("业务失败");
    } catch (Exception e) {
        log.warn("忽略", e);      // ✗ 异常被吃掉 → 事务管理器认为"一切正常" → 提交
    }
}
```

两个坑：
- **吞异常**：切面感知不到失败 → 提交；
- **受检异常不回滚**：默认只回滚 `RuntimeException` 与 `Error`。所以项目里 `@Transactional` **全部显式写 `rollbackFor = Exception.class`**（grep 实证：order/product/knowledge/common 共 6 个文件 12 处）。

### 场景 4：多线程 / 异步

事务的"当前连接"放在 **ThreadLocal** 里。你在方法里 `new Thread` 或 `@Async` 开子线程，子线程是**另一条线程 → 另一个 ThreadLocal → 另一条连接**，根本不在同一个事务里（子线程里滚了，主线程照样提交）。

> 项目的可观测性模块正是"跨线程"的正面教材：`ModelUsageRecorder` 把 Feign 落库丢进 `usageExecutor` 异步执行（`ModelUsageRecorder.java:106`），它**故意不参与主事务**——计量失败只告警，不影响业务。

### 场景 5：数据库不支持事务

MySQL 用 MyISAM 引擎时事务无效（只有 InnoDB 支持）。项目全部建表语句均为 InnoDB（见 `deploy/mysql/*-init.sql`），不存在此问题——但面试可能会问。

### 场景 6：传播行为自己设成了"不要事务"

`NOT_SUPPORTED` / `NEVER` 下方法内不会有事务；`REQUIRES_NEW` 也要注意"内层回滚不影响外层，但连接占两条"。

### 失效排查清单（背下来，面试直接答）

```text
① 是不是同类自调用（this.xxx）绕过了代理？        → 最常见
② 方法是不是 public？
③ 异常是不是被 try-catch 吞了 / 是受检异常没配 rollbackFor？
④ 是不是在子线程 / @Async 里执行？
⑤ 表引擎是不是 InnoDB？
⑥ 传播行为是不是 NOT_SUPPORTED / NEVER？
⑦ 这个类本身是不是被 new 出来的（不是 Spring Bean）？
```

---

## 七、放眼全仓：四个注解，同一个原理

项目里"声明式"能力全都站在"代理 + 注解元数据"这块地基上：

| 注解 | 底层切面（谁在干活） | 项目实例 | 生效时机 |
|---|---|---|---|
| `@Transactional` | `TransactionInterceptor` | `OrderServiceImpl:78-79` | 方法前开、方法后提交/回滚 |
| `@Cacheable` / `@CacheEvict` | `CacheInterceptor` | `ProductServiceImpl:109,119,191` | 命中直接返回（**不进方法**）；驱逐在方法成功后 |
| `@Idempotent`（自定义） | `IdempotentAspect` | `ai-cs-common/.../idempotent/` | 方法前占位、失败释放 |
| `@PreAuthorize` | `AuthorizationManagerBeforeMethodInterceptor`（方法级安全） | `ai-cs-user`（见 [09-安全与设计模式/03](../09-安全与设计模式/03-SpringSecurity微服务两层安全模型.md)） | 方法前鉴权，不通过抛 403 |

> 面试可讲的对比：**为什么这些都选"注解 + AOP"而不是在每个方法里手写？**
> ① 横切关注点（事务/缓存/安全/幂等）与业务解耦；② 统一策略改一处全生效；
> ③ 代价是"隐式魔法"——**失效场景难排查**，所以要有本节的清单兜底。

---

## 八、多个注解叠在同一个方法上，谁先谁后？

真实项目里注解经常叠着用，顺序错了会出 bug。三个项目的真实例子：

### 8.1 `@Transactional` + `@CacheEvict`

```java
// ai-cs-product/src/main/java/com/aics/product/service/impl/ProductServiceImpl.java:189-192
@Override
@Transactional(rollbackFor = Exception.class)
@CacheEvict(cacheNames = ProductCacheConfig.PRODUCT_DETAIL, key = "#productId")
public void deductStock(Long productId, int quantity) { ... }
```

**先说一个"书上很少讲、面试却能加分"的前提**：`@CacheEvict` 与 `@Transactional` 都靠 AOP 拦截器实现，
而 `@EnableCaching` 与 `@EnableTransactionManagement` 的 `order` 属性**默认都是 `Ordered.LOWEST_PRECEDENCE`（两者相同）
——也就是说，Spring 默认不承诺谁在外层**。要固定顺序必须显式指定（`@EnableCaching(order = ...)` /
`@EnableTransactionManagement(order = ...)`）。所以下面按"最坏情况（缓存拦截器在内层，删缓存发生在提交前）"来分析风险：

```text
事务拦截器（外层）开事务
  └─ 缓存拦截器（内层）→ 业务方法执行 → 返回
  └─ 缓存拦截器：方法成功 → 删缓存（此刻事务还没提交！）
事务拦截器：commit
```

**风险**：删缓存后、事务提交前，若有并发读把数据库旧值回填进缓存，缓存就会"假新鲜"（脏数据要等 TTL 才过期）。
项目在 [09-SpringCache与事务领域事件 1.4 节](09-SpringCache与事务领域事件.md) 里已记录这个权衡：
删除时机与"先更库后删缓存"的约定写进了注释，**读多写少 + TTL 兜底**下可接受。

> 进阶方案（面试加分项）：`@CacheEvict` 无法保证"提交后才删"，要严谨可以用
> `TransactionSynchronizationManager.registerSynchronization(afterCommit -> 手动删缓存)`，
> 或直接复用 `@TransactionalEventListener(AFTER_COMMIT)` 模式（项目的 `OrderPaidEventListener` 就是这种写法）。

### 8.2 `@Transactional` + `@TransactionalEventListener`

```java
// ai-cs-order/src/main/java/com/aics/order/service/impl/OrderServiceImpl.java:261（事务方法内部）
eventPublisher.publishEvent(new OrderPaidEvent(orderNo, order.getUserId()));

// ai-cs-order/src/main/java/com/aics/order/event/OrderPaidEventListener.java:30-31
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handle(OrderPaidEvent event) { ... 投递 MQ 通知 ... }
```

**重点**：事件在事务内发布，但监听器被**挂到"提交之后"执行**——事务回滚时监听器根本不执行，从而杜绝"订单没付成功却推了支付成功通知"。
四种 phase：`BEFORE_COMMIT` / `AFTER_COMMIT`（默认）/ `AFTER_ROLLBACK` / `AFTER_COMPLETION`，详见 [02-Spring微服务/09](09-SpringCache与事务领域事件.md)。

### 8.3 `@Idempotent` + `@Transactional`

切面代码里有一句很关键的检查，它"暴露"了实际执行顺序：

```java
// IdempotentAspect.java:51-54
if (TransactionSynchronizationManager.isActualTransactionActive()) {
    log.debug("幂等方法存在活跃事务，失败时将主动释放 key: {}", idempotent.key());
}
```

**能观察到"已有活跃事务"，说明切面执行时事务已经开好了**（事务拦截器在外、切面在内），
所以"占位"必然早于"事务提交"——这正是 `Idempotent` 注释里写的预期行为：
同一 key 的并发请求在占位期间直接 409 拒绝，而不是排队等行锁。
失败路径"业务异常 → 释放 key"也覆盖了"事务回滚"的情况（异常会穿过切面）。

> 边界补充：切面与事务拦截器的先后同样受 advisor order 影响（默认同级）。
> 由于"占位必须早于提交"这一语义在两种顺序下都成立（都发生在 commit 之前），此处无需显式固定 order；
> 若将来想让"幂等拒绝"发生在事务开启之前（省一次连接占用），可给切面加 `@Order` 显式提到事务外层。

> **记忆口诀（只背"确定"的）**：
> **事件监听器一定挂在事务提交之后；幂等占位一定早于事务提交（失败即释放）；
> 缓存删除与事务提交的先后默认不由 Spring 保证——按"最坏在提交前"评估风险，要确定就显式设 order 或挪到 AFTER_COMMIT。**

---

## 九、怎么验证"代理真的生效了"（三招）

1. **打印类名**（最快）：
   ```java
   log.info("proxy = {}", orderService.getClass());
   // JDK 代理：class com.sun.proxy.$Proxy123
   // CGLIB：class com.aics.order.service.impl.OrderServiceImpl$$EnhancerBySpringCGLIB$$abc123
   // 打印出来是"光杆类名"（没有 $$ / $Proxy）→ 说明拿到的是真身，注解必然不生效
   ```
2. **`AopUtils.isAopProxy(bean)` / `AopUtils.isCglibProxy(bean)`**（调试断点常用）；
3. **单测里手工织入**：`AspectJProxyFactory`（项目 `IdempotentAspectTest` 的做法）——不启动容器就能验证切面行为。

---

## 十、面试要点总结

> 一句话主线：**AOP 的本质是"给 Bean 套一个替身"，注解只是贴纸，生效与否取决于"调用是否经过替身"；`@Transactional` 就是"环绕通知 + ThreadLocal 绑定同一条连接"。**

```text
关键词：
Aspect/Advice/Pointcut/Weaving · @Around + joinPoint.proceed() · 前置/放行/后置/异常补偿四段式
JDK 代理（有接口、造兄弟）vs CGLIB（无接口、造儿子、final 不能代理）· Boot 2.x 默认全用 CGLIB
@Transactional = TransactionInterceptor + ThreadLocal（连接绑定、跨线程失效）
失效七查：自调用 / 非 public / 吞异常 / 受检异常没 rollbackFor / 子线程 / 非 InnoDB / 传播行为
顺序口诀：事件挂提交后 · 幂等占位早于提交（失败即释放）· 缓存删除与提交的先后默认不保证（要确定就设 order / 挪 AFTER_COMMIT）
项目锚点：IdempotentAspect（唯一手写切面）· ProductServiceImpl @CacheEvict · OrderServiceImpl 双层事务 · OrderPaidEventListener AFTER_COMMIT
```

### 高频追问链

1. **问：`@Transactional` 怎么实现的？**
   答：AOP 环绕通知（`TransactionInterceptor`）+ 连接绑定 ThreadLocal（`TransactionSynchronizationManager`）。
2. **追问：那为什么同类自调用会失效？**
   答：`this` 是真身，不走替身，通知没机会执行。
3. **再追问：`this` 调用怎么解决？**
   答：拆 Bean / 注入自己 / `AopContext.currentProxy()`；或干脆用编程式事务 `TransactionTemplate`（显式、无代理依赖）。
4. **再追问：事务里删缓存为什么有风险？**
   答：`@CacheEvict` 在方法返回后、提交前执行，存在"删除后提交前并发读回填旧值"窗口；严谨做法是 AFTER_COMMIT 里删或挂事务同步。

---

## 学习检查清单

- [ ] 能用自己的话说出"切面/通知/切点/织入"分别是什么
- [ ] 能画出 `IdempotentAspect` 的执行时序图（含"重复请求业务零触达"）
- [ ] 能说出 JDK 代理与 CGLIB 的三点区别，并解释 Mapper 接口为何能注入
- [ ] 能一句话解释 `@Transactional` 原理（AOP + ThreadLocal）
- [ ] 能背出事务失效排查清单的至少 5 条
- [ ] 能说清"事务/缓存/事件/幂等"四个注解叠加时的执行顺序
- [ ] 会用 `AspectJProxyFactory` 给一个方法写切面单测

## 下一步

- 继续看 [16-IoC 容器与 Bean 生命周期](16-IoC容器与Bean生命周期.md)：替身是"谁"造出来的、Bean 是怎么一步步变成可用对象的；
- 想看"声明式全家桶"的工程总账：[02-Spring微服务/10-自定义 Starter 与自动装配](10-自定义Starter与自动装配.md)（把这些能力收编为自动装配）。
