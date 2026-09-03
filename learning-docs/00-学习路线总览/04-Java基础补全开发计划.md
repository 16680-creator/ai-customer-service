# 04-Java基础补全开发计划

> 2026-09 制定。来源：`learning-docs/01-Java基础` 目录盘点——目前仅 2 篇
> （[01-Java17核心特性](../01-Java基础/01-Java17核心特性.md)、
> [02-Maven多模块管理](../01-Java基础/02-Maven多模块管理.md)），
> 对照一线 Java 后端知识地图（JVM / 并发 / 集合源码 / 反射与代理 / I/O / 语言级细节）找出 6 个空白。
> 与 [01-Spring](01-Spring技术补全开发计划.md) / [02-中间件](02-中间件补全开发计划.md) /
> [03-微服务治理](03-微服务治理补全开发计划.md) 三份计划**并行推进，互不阻塞**；
> 本计划定位是**学习层补全**：每个阶段产出一篇技术文档放入 `01-Java基础/`，
> 并配一个落在本项目真实代码上的实践锚点（学完立刻在项目里验证）。
>
> **✅ 2026-09 进度**：P1~P6 六篇学习文档（03~08，均含「高频面试题」与学习检查清单）
> 已全部产出；剩余为需运行环境（JVM 实测/压测/Docker）的代码实践项与部分代码注释/单测落库。

## 〇、盘点：已覆盖 vs 空白

### A. 已覆盖（不重复补）✅

| 主题 | 覆盖位置 |
|------|----------|
| Java 17 语言特性（Record/Sealed/模式匹配/Text Block/var） | 01-Java17核心特性 §2 |
| Stream / Optional / 集合工厂方法 | 01-Java17核心特性 §2.6~2.8 |
| 泛型、异常体系、注解 | 01-Java17核心特性 §4~6 |
| 线程池与 CompletableFuture **基础用法** | 01-Java17核心特性 §3（进阶归本计划 P2） |
| Maven 多模块 / BOM / 依赖冲突 / 注解处理器配置 | 02-Maven多模块管理 |

### B. 空白（本计划补齐）⚠️

| # | 空白 | 为什么必须补 |
|---|------|--------------|
| 1 | JVM 内存模型与 GC | 项目已有多容器部署与真实日志，但「OOM 怎么查、GC 怎么调」零覆盖；面试硬通货 |
| 2 | 并发编程进阶（JMM/JUC/锁） | 现有文档只有线程池两小节；chat 线程池、状态机 synchronized、分库分表并发写都是现场 |
| 3 | 集合框架与源码 | 只讲了工厂方法；HashMap/ConcurrentHashMap 原理是面试必考，也是排查缓存问题的底子 |
| 4 | 反射、动态代理与 SPI | Starter 自动装配已落地但「为什么放个文件就能装配」的底层原理空白；Mapper 接口无实现也是同一原理 |
| 5 | I/O 模型与网络编程 | 项目重度使用 SSE/WebSocket，但 BIO/NIO/Reactor 一片空白 |
| 6 | 语言级易错点与序列化 | BigDecimal 金额精度（资损级）、Jackson 时区/多态序列化是项目真实风险点 |

**归属边界**（避免与既有文档重复）：JUnit/Mockito/Testcontainers 归 `08-测试`；
Spring AOP/事务/自动装配的框架层实现归 `02-Spring微服务`；Redis/MQ 等中间件**用法**归 `04-中间件`；
本计划只补**语言、JVM、标准库**这一层。

---

## 一、总览与排期

| 阶段 | 主题 | 产出文档（01-Java基础/） | 实践锚点（本项目） | 预估 |
|------|------|--------------------------|--------------------|------|
| P1 | JVM 内存模型与 GC 实战 | 03-JVM内存模型与GC实战.md | order 加 JVM 参数 + OOM 排查案例 | 3 天 |
| P2 | 并发编程进阶 | 04-并发编程进阶.md | 复盘 ObservabilityExecutorConfig + 虚拟线程实验分支 | 3~4 天 |
| P3 | 集合框架与源码 | 05-集合框架与源码.md | 复盘 AgentToolRegistry / 网关白名单选型 + 手写 LRU | 2~3 天 |
| P4 | 反射、动态代理与 SPI | 06-反射动态代理与SPI.md | 手写「迷你 MyBatis」20 行 + common SPI 扩展点 | 2~3 天 |
| P5 | I/O 模型与网络编程 | 07-IO模型与Netty基础.md | NIO echo server + chat SSE 抓包复盘 | 2~3 天 |
| P6 | 语言级易错点与序列化 | 08-语言级细节与序列化.md | 购物车 BigDecimal 边界单测 + common 统一 Jackson | 2 天 |

**依赖关系**：

```
P3 集合 ──→ P2 并发（ConcurrentHashMap / 线程池源码以集合为前提）
P4 反射/代理 ──（案例衔接）02-Spring微服务/10 已落地的自定义 Starter
P1 / P5 / P6 独立，任意穿插
收尾：总览 README（00-学习路线总览）同步更新 01-Java基础 行链接
```

**纪律**：学习文档同样入 Git 走 review；凡涉及改代码的实践任务走独立分支，
测试先行，`mvn verify` 过门禁才可合并；不动主干业务逻辑。

---

## 二、P1：JVM 内存模型与 GC 实战

### 现状锚点

- `01-Java17核心特性.md` 只讲语言特性，JVM 层零覆盖
- 项目已有真实可观测材料：根目录遗留运行日志（`green-svc.log`、`red-service-test.log`）、
  `docker-compose.yml` 多容器部署、`07-运维部署` 的 Prometheus/Grafana 体系——
  但没有任何一篇讲「内存怎么看、GC 怎么调、OOM 怎么查」

### 学习要点

1. 运行时数据区：堆 / 虚拟机栈 / 本地方法栈 / 元空间 / 程序计数器；
   对象创建与内存分配（TLAB、逃逸分析概览）
2. GC 基础：分代假说、可达性分析（GC Roots）、四种引用（强软弱虚——ThreadLocal 与缓存都会用到）
3. 收集器演进：Parallel → CMS（历史地位）→ **G1（JDK 17 默认，重点）** → ZGC 概览；
   G1 的 Region、Mixed GC、暂停预测模型
4. 常用参数：`-Xms/-Xmx/-Xmn`、`-XX:MetaspaceSize`、GC 日志（JDK 17 统一日志 `-Xlog:gc*`）
5. 排查工具链：`jps/jstat/jmap/jstack/jcmd` → Arthas（dashboard/thread/heapdump）→ MAT 分析 dump

### 实践任务（落在本项目）

- 给 `ai-cs-order` 配置 `-Xms/-Xmx` 与 GC 日志参数，本地压测时用 `jstat -gcutil` 观察 young/full GC 节奏
- 构造一个 OOM 案例（如无界缓存集合持续填充）→ `jmap` dump → MAT 定位支配树；
  与 02-中间件计划里「Redis 无界 key」风险呼应，写进同一篇文档的「内存失控两个现场」
- 用 `jstack` 抓一次线程栈，对照 01-P6 订单状态机已落地的 `synchronized` 段，找到 BLOCKED 线程

### 任务清单（✅ 文档部分 2026-09 落地）

- [x] 产出文档《JVM 内存模型与 GC 实战》，含本项目启动参数建议表
- [ ] order 服务 JVM 参数 + GC 日志接入，并附 `jstat` 观察记录（需本地运行环境）
- [ ] OOM 案例：代码 + dump 分析截图/结论 + 修复对照（补 eviction 或上限）（需 MAT 环境）
- [ ] jstack 实录：状态机 synchronized 的线程栈解读小节（方法已写入文档 §六，实测留待压测）
- [x] 面试自测清单（内存区域/引用类型/G1 流程/排查思路四问）→ 文档 §七/§八

### 面试要点

对象一定分配在堆上吗（逃逸分析/标量替换）；G1 与 CMS 的本质区别（Region 化、可预测停顿）；
内存泄漏与内存溢出的关系与排查路径；为什么微服务容器里要显式设 `-Xmx`（容器内存限额与 OOMKilled）。

---

## 三、P2：并发编程进阶

### 现状锚点

- `01-Java17核心特性.md` §3 仅两小节（Executors 创建、CompletableFuture 组合），无 JMM、无 JUC 工具、无锁原理
- 项目真实现场：chat 的 `ObservabilityExecutorConfig` 是唯一显式线程池；01-P6 订单状态机
  并发 reset 用了 `synchronized`；user 分库分表后存在并发写；SSE 流式输出是典型 IO 密集场景

### 学习要点

1. JMM：主内存/工作内存、happens-before 规则；volatile 的可见性与禁止重排（原理层面）
2. 锁：synchronized 锁升级（偏向→轻量→重量）；AQS 队列同步器与 ReentrantLock；读写锁
3. ThreadLocal：与内存泄漏（Entry 弱引用、必须 remove）；结合项目「用户上下文透传」场景讲正确姿势
4. JUC 工具：CountDownLatch / Semaphore / CyclicBarrier 选型；原子类与 CAS（ABA 概览）
5. 线程池深入：7 参数、拒绝策略、`shutdown` vs `shutdownNow`、动态调参；为什么禁用 `Executors` 快捷方法
6. ConcurrentHashMap（1.8 CAS + 桶头 synchronized）、CopyOnWriteArrayList（监听器列表场景）
7. **Java 21 虚拟线程预研**：Boot 3.2 `spring.threads.virtual.enabled=true` 一键开关；
   SSE / LLM 调用等 IO 密集场景的收益与 Pin 问题概览

### 实践任务（落在本项目）

- 复盘 `ObservabilityExecutorConfig`：核心线程数/队列/拒绝策略是否匹配 IO 密集负载，
  写一页 ADR 式说明（含计算依据：`N_threads ≈ N_cpu * (1 + W/C)`）
- 用 JMeter/ab 对 chat SSE 接口压测，观察队列堆积与拒绝行为，给出参数结论
- **虚拟线程实验分支**：chat 打开 `spring.threads.virtual.enabled=true` 压测对比；
  数据说话后再决定是否合入——默认不合主干

### 任务清单（✅ 文档部分 2026-09 落地）

- [x] 产出文档《并发编程进阶》，含 JMM/锁/JUC/线程池四节 + 虚拟线程预研小节
- [x] `ObservabilityExecutorConfig` 参数复盘说明 → 文档 §5.3 逐参数审视（代码内 ADR 注释待补）
- [ ] SSE 压测记录 + 结论（进文档，不进代码库大文件）（需压测环境）
- [ ] 虚拟线程对比数据（分支内），合入与否给出明确结论（需压测环境）
- [x] 面试自测清单（happens-before 八条挑五条/锁升级/CAS/线程池参数四问）→ 文档 §七/§八

### 面试要点

volatile 能保证原子性吗（不能，只保证可见性/有序性）；synchronized 与 ReentrantLock 取舍；
ThreadLocal 内存泄漏根因（弱引用 key + 强引用 value）；线程池参数如何依据负载类型定；
虚拟线程适用边界（IO 密集、禁止 pin 的 synchronized 块——Java 21 与 24 的差异）。

---

## 四、P3：集合框架与源码

### 现状锚点

- 现有文档只讲 `List.of/Set.of` 工厂与 Stream
- 项目真实使用：网关白名单 `Set.of`、chat 的 `AgentToolRegistry`（工具注册表，Map 检索）、
  购物车 `List`、01-P2 领域事件监听器列表——都是「集合选型为什么这样」的活案例

### 学习要点

1. HashMap（1.8）：扰动函数、容量/负载因子、链表转红黑树的 8/6 阈值与原因、resize 高低位拆分；
   **为什么重写 equals 必须重写 hashCode**
2. List 家族：ArrayList 扩容 1.5 倍、LinkedList 与 ArrayDeque 选型、fail-fast 与 modCount
3. TreeMap / LinkedHashMap：accessOrder 与 LRU 手写练习
4. 并发容器：ConcurrentHashMap 1.8 演进（1.7 分段锁对比）、CopyOnWriteArrayList 适用边界
5. 不可变集合：`List.of` 不可变 vs `Collections.unmodifiableList` 视图；防御性拷贝在 DTO 上的应用

### 实践任务（落在本项目）

- 复盘 `AgentToolRegistry` 与网关白名单的集合选型，把「为什么是这个集合」写成 3~5 行注释或文档小节
- 手写 LRU 两版（LinkedHashMap 一版 + 双向链表一版）+ 单测，
  与 `04-中间件/01-Redis缓存实战` 的淘汰策略呼应
- 找一处用 `Arrays.asList`/`subList` 的易错点（不可 add、与原数组联动），写进文档「坑位清单」

### 任务清单（✅ 文档部分 2026-09 落地）

- [x] 产出文档《集合框架与源码》，含 HashMap 源码走读 + 选型决策表
- [x] AgentToolRegistry / 白名单选型复盘小节 → 文档 §一（代码注释待补）
- [ ] LRU 两版实现 + 单测（完整代码已在文档 §四，独立示例类/单测待落）
- [x] 面试自测清单（HashMap 扰动/扩容/线程安全三问 + ArrayList 扩容）→ 文档 §七/§八

### 面试要点

为什么容量是 2 的幂（位运算替代取模）；8/6 阈值与泊松分布；HashMap 死循环的 1.7 背景（头插）
与 1.8 为何仍有并发问题；ConcurrentHashMap size 为什么用 CounterCell。

---

## 五、P4：反射、动态代理与 SPI

### 现状锚点

- `02-Spring微服务/10-自定义Starter与自动装配` 已落地 `AutoConfiguration.imports`，
  但「放个文件就能装配」的底层链路（类加载 → SPI 思想 → 反射实例化）在 Java 基础层是空白
- MyBatis-Plus 的 Mapper 是「接口无实现类」——JDK 动态代理的典型现场
- 父 POM `annotationProcessorPaths` 已同时配置 Lombok + MapStruct（编译期注解处理器路线）

### 学习要点

1. 类加载：加载-验证-准备-解析-初始化五阶段；双亲委派模型与「打破」场景（SPI/JDBC、Tomcat 隔离）
2. 反射 API：`Constructor/Method/Field`、`setAccessible(true)` 的代价；MethodHandle 概览（为什么框架开始弃用反射）
3. JDK 动态代理 vs CGLIB：接口限制、final 类/方法限制——**Spring AOP 默认策略的根因**
4. SPI：`ServiceLoader` + `META-INF/services`；与 JDBC 驱动发现、
   Spring 的 `spring.factories` → `AutoConfiguration.imports` 的血缘关系
5. 两条注解路线：运行时注解 + 反射（Spring）vs 编译期注解处理器（Lombok/MapStruct）对比表

### 实践任务（落在本项目）

- 用 JDK Proxy + 反射手写一个 **20 行「迷你 MyBatis」**：接口方法 → 方法上的注解 SQL → 代理内拼接打印；
  呼应 `03-数据库与ORM/02-MyBatisPlus实战`
- 给 `ai-cs-common` 加一个真实 SPI 扩展点：`ServiceLoader` 加载 `RuleProvider`
  （chat 已有 `StaticRuleProvider` / `KnowledgeRuleProvider` 两实现，天然素材）
- 用反射读一遍 `AutoConfiguration.imports` 文件并实例化一个配置类，画出「从文件到 Bean」链路图

### 任务清单（✅ 文档部分 2026-09 落地）

- [x] 产出文档《反射、动态代理与 SPI》，含类加载 + 双亲委派 + 代理对比表
- [x] 迷你 MyBatis 示例代码 → 文档 §五 完整代码（独立单测待落）
- [ ] common 的 RuleProvider SPI 化 → 文档 §六 已给取舍结论：单服务内 Spring 注入足够，SPI 仅作机制验证，不动主分支
- [ ] 「从 imports 文件到 Bean」链路图 → 文档 §4.2 已含文字链路，Mermaid 图待补
- [x] 面试自测清单（双亲委派/代理区别/SPI 与自动装配关系三问）→ 文档 §七/§八

### 面试要点

双亲委派是什么、为什么、何时打破；JDK 代理与 CGLIB 在 Spring 里的默认选择（Boot 2 起默认 CGLIB 及原因）；
`@Autowired` 的注入本质（反射 + BeanPostProcessor）；SPI 与 Spring Factories 的演进
（Boot 2.7 弃用 spring.factories → 3.0 imports 文件）。

---

## 六、P5：I/O 模型与网络编程

### 现状锚点

- 项目重度依赖长连接：chat 流式对话用 SSE（`text/event-stream`）、notify 用 WebSocket/STOMP、
  服务间 Feign/RestTemplate、客户端连 Redis/ES——但 Java 基础层 BIO/NIO 零覆盖
- `04-中间件/05-SSE与WebSocket实时通信` 只讲用法，不讲底层模型

### 学习要点

1. BIO → NIO：阻塞/非阻塞、Buffer/Channel/Selector、多路复用（epoll 概览）；
   Reactor 模式三形态（单线程/多线程/主从）
2. AIO 简述与「为什么 Linux 上 AIO 名存实亡」
3. 零拷贝：`sendfile`/`mmap` 概念——与 RocketMQ（mmap）、Kafka（sendfile）、ES 的关系
4. Netty 概览：EventLoop、Pipeline、ByteBuf；为什么 Redis/ES/gRPC 的 Java 客户端都基于 Netty
5. HTTP 协议层：keep-alive、chunked 传输、SSE 帧格式（`data:`/`id:`/`retry:`）——
   直接解释 chat 流式接口的响应行为

### 实践任务（落在本项目）

- 用原生 NIO 写一个 echo server（Selector 版），再对照 Netty 三行版，体会差距
- 抓 chat SSE 响应（curl -N + DevTools），逐帧解读事件流格式，写进文档「项目现场」小节
- 说明网关（WebFlux/Netty）到 chat（Servlet/Tomcat）的 I/O 模型差异，
  解释为什么 SSE 需要关注异步上下文切换——为 03-微服务治理计划的响应式改造铺垫

### 任务清单（✅ 2026-09 全部落地）

- [x] 产出文档《I/O 模型与 Netty 基础》，含 Reactor 三形态图 + 零拷贝对照表
- [x] NIO echo server 示例代码 → 文档 §一 完整可运行代码 + §六 Netty 对照版
- [x] chat SSE 现场解读小节（帧格式 + 响应头）→ 文档 §五
- [x] 面试自测清单（BIO/NIO 区别/epoll/零拷贝三问）→ 文档 §七/§八

### 面试要点

select/poll/epoll 区别；Netty EventLoop 与线程绑定的意义（无锁串行化）；
TCP 粘包拆包（ByteBuf 与定长/分隔符/长度域三策略）；SSE 与 WebSocket 的选型边界
（单向推送/自动重连/文本协议 vs 双向/二进制/子协议）。

---

## 七、P6：语言级易错点与序列化

### 现状锚点

- 购物车与订单金额使用 `BigDecimal`（`CartServiceImpl` 的总价计算、01-Java17 文档 §2.7 引用过），
  精度处理是资损级问题
- 父 POM 同时配置 Lombok 1.18.30 + MapStruct 1.5.5（annotationProcessorPaths 顺序敏感，
  02-Maven 文档提过配置但未展开原理）
- Redis 缓存序列化（01-P2 计划采用 `GenericJackson2JsonRedisSerializer`）、
  MQ 消息 JSON 序列化散布各服务，LocalDateTime 时区坑真实存在

### 学习要点

1. `BigDecimal`：scale/roundingMode、`equals` vs `compareTo`、`divide` 必须显式精度与舍入；
   金额字段规范（DECIMAL(x,y) vs 分为单位的整数）——给出本项目选型结论
2. `java.time`：LocalDateTime / Instant / ZoneId 三者关系；与前端、JSON、MySQL 的时区一致性清单
3. String 与编码：UTF-8 编解码、常量池与 `intern`（概览）
4. 序列化：为什么禁用 Java 原生序列化（安全+体积+兼容）；Jackson 定制
   （LocalDateTime 格式、多态 `@class` 类型信息——Redis 缓存能反序列化回业务对象的前提）；
   `transient` 与 serialVersionUID
5. 对象契约：equals/hashCode、clone 与深浅拷贝、不可变对象设计（与 Record 衔接）
6. Lombok 原理（AST 改写）与 MapStruct 原理（生成 Impl 类）；`@Data` 对 equals/继承的坑、
   `@Builder` 与默认值

### 实践任务（落在本项目）

- 复盘 `CartServiceImpl` 金额计算：补精度边界单测（`0.1+0.2` 类、除法舍入、`compareTo` 陷阱）
- 在 common 提供统一 `ObjectMapper` 默认 Bean（LocalDateTime/时区/非 NULL 策略），
  与 01-P4 的 Starter 自动装配收编路径衔接（该任务与 01 计划共享分支策略，改动小、收益大）
- 写「Lombok + MapStruct 共存」原理小节：为什么 processor 顺序敏感（02-Maven 文档 Q3 的原理展开）

### 任务清单（✅ 文档部分 2026-09 落地）

- [x] 产出文档《语言级细节与序列化》，含 BigDecimal 规范 + Jackson 配置基线
- [ ] 购物车精度边界单测补齐（TDD：先红后绿）→ 单测模板在文档 §1.2，待进 order 模块跑通
- [ ] common 统一 ObjectMapper Bean → 配置基线在文档 §4.2，代码合入与 01-P4 Starter 收编协同
- [x] 面试自测清单（BigDecimal 三问/时区三问/序列化三问）→ 文档 §七/§八

### 面试要点

BigDecimal 为什么用 String 构造（double 构造的精度陷阱）；`equals` 与 `compareTo` 不一致的坑；
LocalDateTime 序列化为什么要指定格式与时区；Jackson 多态序列化的 `@JsonTypeInfo` 默认风险；
原生序列化的反序列化漏洞背景（一句话级）。

---

## 八、收尾任务

- [x] `00-学习路线总览/README.md`：第一阶段「01-Java基础」行更新为新目录清单，
      进阶专题表追加本计划链接 ✅ 2026-09
- [ ] 本文件任务清单全部勾选后归档（剩余：需运行环境的代码实践项），与 01/02/03 三份计划同目录并列

## 九、风险与边界

| 风险 | 缓解 |
|------|------|
| 学习计划与 01/02/03 开发计划争抢时间 | 每阶段「1 篇文档 + 1 个实践锚点」最小闭环；纯学习阶段不阻塞任何开发阶段 |
| 源码学习陷入细节、无限膨胀 | 每篇源码文档只设两条验收线：面试能讲清 + 项目现场能定位 |
| 虚拟线程实验影响 chat 稳定性 | 仅实验分支压测，数据说话，不默认合入主干 |
| P6 改 common ObjectMapper 波及全服务序列化行为 | 配置只做「补充默认值」，逐字段对齐现有行为后再合入；必要时 `@ConditionalOnMissingBean` 允许服务覆盖 |
| 与既有文档内容重复 | 归属边界见「〇、盘点」：语言/JVM/标准库归本计划，框架层归 01/03，中间件用法归 04 |

## 附：完成后 `01-Java基础/` 目录预期

```
01-Java基础/
├── 01-Java17核心特性.md          （已有）
├── 02-Maven多模块管理.md         （已有）
├── 03-JVM内存模型与GC实战.md      （P1）
├── 04-并发编程进阶.md             （P2）
├── 05-集合框架与源码.md           （P3）
├── 06-反射动态代理与SPI.md        （P4）
├── 07-IO模型与Netty基础.md        （P5）
└── 08-语言级细节与序列化.md       （P6）
```
