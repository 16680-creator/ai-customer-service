# CPU 缓存与内存序

> 对应项目：`ai-cs-gateway/src/main/java/com/aics/gateway/filter/TokenBucketRateLimiter.java`（ConcurrentHashMap + per-bucket 锁）、
> `ai-cs-gateway/src/main/java/com/aics/gateway/loadbalancer/InstanceInFlightRegistry.java`（AtomicInteger CAS 计数）、
> `ai-cs-chat/src/main/java/com/aics/chat/service/impl/ResilientAiService.java:277`（AtomicReference 跨响应式管道传值）。
> **划界声明**：[04-并发编程进阶](../01-Java基础/04-并发编程进阶.md) 已讲 JMM、happens-before 八条规则、volatile 语义与线程池工具；本篇讲**硬件层**——缓存行、MESI、store buffer、内存屏障，回答"JMM 那些规则在物理机上为什么必须存在"。

---

## 一、先立结论：JMM 是协议，硬件是物理

Java 内存模型（主内存/工作内存/happens-before）是一份**协议规范**，它存在的唯一理由是：**物理机就是乱序且延迟可见的**。

```
你写下的代码：  counter++ （读-改-写三步）
CPU 实际发生：  读 L1 缓存 → ALU 加 → 写回 store buffer → 延迟刷缓存/内存
另一个核看到：  可能还是旧值；甚至你的两条写操作到达顺序相反
```

所以 JMM 的 volatile/锁/CAS，翻译到硬件就是**缓存一致性消息 + 内存屏障指令**。学完本篇，[04-并发编程进阶](../01-Java基础/04-并发编程进阶.md) 的八条 happens-before 规则你会"倒着推导"出来。

---

## 二、存储器层次与缓存行

### 2.1 层次结构（数量级背下来）

| 层 | 延迟 | 容量 |
|---|---|---|
| L1 缓存（每核私有） | ~1 ns | 32~64 KB |
| L2 缓存（每核私有） | ~4 ns | 数百 KB~1MB |
| L3 缓存（多核共享） | ~15 ns | 数十 MB |
| 内存 DRAM | ~100 ns | GB 级 |

**缓存与内存差一个数量级**——CPU 不直接碰内存，而是以**缓存行（cache line）为单位搬运，x86 上 64 字节**。

### 2.2 缓存行的两个推论

| 推论 | 说明 | 项目关联 |
|---|---|---|
| 空间局部性免费加速 | 访问 `a[0]` 把 `a[0..15]`（long 数组 8B×8）整行搬进缓存，顺序遍历近乎全命中 | 这就是"ArrayList 实测快于 LinkedList"的硬件原因之一（见 [02-线性结构](../11-数据结构与算法/02-线性结构-数组链表栈队列.md)） |
| 一切并发问题的最小粒度是 64B | 一致性协议以缓存行为单位，哪怕你只想保护一个 8 字节的 long | 伪共享的根源（§四） |

---

## 三、MESI：多核怎么保持缓存一致

每个缓存行在每核上有四种状态（MESI = Modified/Exclusive/Shared/Invalid）：

```
M（已修改）：本核改过，与内存不一致，本核独占 → 写回才能给别人
E（独占）：  只有本核有，与内存一致     → 可直接改，升 M
S（共享）：  多核都有只读副本，与内存一致
I（无效）：  本核这行不可用，必须重新读
```

**一次跨核写入的完整流程**（这就是"可见性延迟"的物理来源）：

```
核1 想改 x（状态 S）
  → 核1 发 Invalidate 消息，等待所有其他核回 Ack
  → 核2/核3 把 x 的缓存行标记 I，回 Ack
  → 核1 才能执行写，行升 M
  → 此后核2/核3 读 x：miss（I）→ 核1 写回并转发新值 → 各核升 S
```

**两个关键观察**：

1. 写一个共享变量要让**所有核的对应行失效**——这就是 volatile 写"立即对其他核可见"的硬件动作；
2. 等待 Ack 的空窗由 **store buffer** 填补（§五）——也埋下乱序的种子。

---

## 四、伪共享：64 字节上打群架

**定义**：两个互不相关的热变量落在同一缓存行，多核各自频繁写 → 缓存行在核间来回 Invalidate，性能坍塌。**不是逻辑 bug，是物理事故**。

```java
// 反面教材：两个计数器同处一个缓存行
class Counters { volatile long a; volatile long b; }   // a、b 在同一 64B 行
// 线程1 死循环写 a，线程2 死循环写 b：
//   每次写都要把对方的行 Invalidate → 缓存行在两核之间乒乓，吞吐掉一个数量级

// 手工补位（老办法）：凑满 64B
class Padded { volatile long a; long p1,p2,p3,p4,p5,p6,p7;   // a 独占一行
               volatile long b; long q1,q2,q3,q4,q5,q6,q7; } // b 独占一行
// 注解方案：@sun.misc.Contended（JDK8+，需 JVM 参数 -XX:-RestrictContended，否则仅 JDK 内部生效）
```

**项目视角**：为什么 `TokenBucketRateLimiter`（`TokenBucketRateLimiter.java:25`）把桶做成 **`Map<String, Bucket>`、每个 key 一个 `Bucket` 对象**，而不是一个大数组下标分桶？对象分配天然散落在不同缓存行，**结构上规避了伪共享**——这是"per-key 对象 + per-bucket 锁"设计在硬件层的隐性收益。真正要警惕伪共享的是高频数值数组（计数器数组、条带计数器），项目里没有这类热点（`InstanceInFlightRegistry` 的计数是每实例一行、写入频率低），如实说：**本项目无伪共享热点，但面试必备**。

---

## 五、store buffer 与内存屏障

### 5.1 store buffer：写不一致的"事故源头"

```
核1: x=1（等 Invalidate Ack）→ 先写进 store buffer，不等 Ack，继续执行后面的指令
核1: ready=true（也可能进了 store buffer）
核2: while(!ready); read(x)  → 可能看到 ready=true 而 x=0 ！！
```

原因：核1 的两次写还没从 store buffer 刷出（或刷新顺序被优化），核2 就读到了后写的 ready。**这不是编译器捣乱，是 CPU 为了不空等一致性 Ack 的正常流水线行为**。

### 5.2 内存屏障：把"允许乱序"收紧为"我要顺序"

| 屏障 | 作用 | 对应场景 |
|---|---|---|
| StoreStore（写写） | 前面的写先于后面的写对外可见 | 数据写完，再写 ready 标志 |
| LoadLoad（读读） | 前面的读完成后才执行后面的读 | 看到标志后，再读数据 |
| **StoreLoad（读写）** | 前面的写对外可见后，后面的读才执行；**最贵**（常表现为全屏障） | volatile 写之后的读 |

x86 是**强内存模型**：硬件只允许 StoreLoad 重排，所以 volatile 写在 x86 上主要就是一条 `lock` 前缀指令（含 StoreLoad 语义）；ARM 等**弱内存模型**上则要插入显式屏障指令——同一份 Java 代码在不同 CPU 上屏障成本不同，这就是"volatile 在 x86 便宜"的准确说法。

---

## 六、volatile 的 happens-before：规则背后的硬件指令

把 [04-并发编程进阶](../01-Java基础/04-并发编程进阶.md) 的 volatile 规则翻译到硬件：

| Java 语义 | JVM 编译器约束 | 硬件动作 |
|---|---|---|
| volatile 写 | 前后禁止重排 + StoreLoad | `lock` 前缀写 / 屏障 → 触发 Invalidate，其他核行变 I |
| volatile 读 | 后续读写不提前 | 强制从一致性协议取最新值（行若 I 则重新加载） |
| **happens-before：volatile 写 → 后续读** | 编译器不许优化跨越 | 缓存一致性 + 屏障保证"写 x=1" 先于"读 ready=true" 对所有核可见 |

§5.1 的 `x/ready` 反例，修复只需一行：

```java
int x = 0; volatile boolean ready = false;
// 线程1: x = 1; ready = true;     // volatile 写：x 的写入被屏障"钉"在 ready 之前可见
// 线程2: if (ready) { use(x); }   // volatile 读：读到 true 时必然看到 x=1
```

**这就是"volatile 保证可见性 + 有序性，不保证原子性"的完整物理解释**——`count++` 三步读改写在两个核的 store buffer 里照样丢更新。

---

## 七、CAS 与 ABA

### 7.1 CAS：硬件的原子指令

```
CAS(addr, expect, new)：
  CPU 单指令（x86: lock cmpxchg）完成 "读-比较-写"，期间其他核无法插手同一缓存行
  成功 → 返回新值；失败 → 返回当前值，由调用方决定重试（自旋）
```

`AtomicInteger.incrementAndGet` = 自旋 + `UNSAFE.compareAndSwapInt`。对比锁：CAS 无挂起/唤醒、无上下文切换，竞争低时快一个量级；竞争高时自旋空转白白烧 CPU。

### 7.2 ABA：CAS 的盲区

```
值序列：A → B → A
CAS(addr, A, C) 成功 —— 但中间已经发生过 A→B→A
对"值"无影响，对"结构"可能是灾难：链表头 A 被摘走又放回，
CAS 以为没人动过，实际 next 指针早换了 → 无锁栈的经典破坏场景
```

| 解法 | 原理 |
|---|---|
| `AtomicStampedReference` | 值 + 版本号（stamp）一起 CAS |
| `AtomicMarkableReference` | 值 + 布尔标记 |
| 业务分析排除 | 计数器/纯数值场景 ABA 无害（值回到 A 本身就等价） |

**项目现场分析（`InstanceInFlightRegistry.java:39-48`）**：

```java
public void increment(String key) { counters.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet(); }
public void decrement(String key) { ... counter.updateAndGet(current -> Math.max(0, current - 1)); }
```

- `incrementAndGet` / `updateAndGet` 是**自旋 CAS**：读旧值 → CAS 减 1 → 失败重试（`updateAndGet` 内部就是 CAS 循环）；
- **ABA 无害论证**：这里 CAS 的对象是纯计数数值，A→B→A 与"从未变化"等价，不存在结构引用问题——面试里"说出场景 + 论证无害"比背概念加分得多；
- 网关多副本各持一份计数（注释明说"每副本最少连接"），写入频率 = 请求 QPS，CAS 自旋失败率极低，无需升级 LongAdder。

### 7.3 CAS vs 锁 vs LongAdder 一张表

| 方案 | 低竞争 | 高竞争 | 适用 |
|---|---|---|---|
| CAS（AtomicXxx） | ✅ 最快 | ❌ 自旋烧 CPU | 计数器、状态标志 |
| synchronized / Lock | 一般 | ✅ 排队挂起不烧 CPU | 复合逻辑、临界区长 |
| LongAdder / 条带计数 | 一般 | ✅ 分散热点（内部就是防伪共享设计） | 极高并发统计（如全局 QPS 计数） |

---

## 八、项目现场：三个并发结构逐行走读

### 8.1 `TokenBucketRateLimiter`：ConcurrentHashMap + per-bucket 锁（混合策略）

```java
// TokenBucketRateLimiter.java:25, 38-52
private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();   // ① 定位：无锁
Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now)); // ② 创建：原子
synchronized (bucket) {                                                  // ③ 读写桶状态：细粒度锁
    double refill = elapsedNano / 1_000_000_000.0 * Math.max(1, qps);
    bucket.tokens = Math.min(capacity, bucket.tokens + refill);
    if (bucket.tokens >= 1.0) { bucket.tokens -= 1.0; return true; }
    return false;
}
```

为什么三层各选各的：

| 层 | 选择 | 硬件/并发理由 |
|---|---|---|
| 查找桶 | ConcurrentHashMap | 读多写少，CAS+synchronized 桶头已够，不需要全局锁 |
| 令牌扣减 | per-bucket `synchronized` | 逻辑是"读-算-写"三步复合（读 lastRefillNano、改 tokens），CAS 表达不了复合逻辑；锁粒度 = 单个用户，天然分散热点（也顺带分散了缓存行） |
| 字段 | 普通 double/long（非 volatile） | 锁内访问，锁的释放-获取自带 happens-before，无需 volatile |

> 类里注释还点了一个并发细节：令牌用 `double` 防止低速率下整数截断饿死——并发正确性（synchronized）与数值正确性（double）是两个独立问题，都要顾。

### 8.2 `InstanceInFlightRegistry`：纯 CAS 计数器（§7.2 已析）

`incrementAndGet`（无复合逻辑）选 CAS；`decrement` 用 `updateAndGet` + `Math.max(0, ...)` 防止 doFinally 重复触发导致负数——**CAS 循环里塞防御逻辑**的标准写法。

### 8.3 `ResilientAiService`：AtomicReference 跨响应式管道传值

```java
// ResilientAiService.java:277（invokeStream 内）
AtomicReference<Usage> usageRef = new AtomicReference<>();   // 流式场景 Usage 只能在最后一个 chunk 里拿到
...
flux.doOnNext(... usageRef.set(usage) ...)     // 回调线程 A 写
    .doFinally(sig -> { usage = usageRef.get(); ... })   // 可能是另一个线程 B 读
```

lambda 内不能改局部变量，且回调来自不同线程——`AtomicReference` 同时解决"**闭包可写性**"与"**跨线程可见性**"（set/get 各带完整内存语义）。同文件 `:253-254` 的 TraceContext/ChatUserContext 快照恢复，则是 ThreadLocal 不跨线程传播的对应处理（见 [01-进程线程与调度](./01-进程线程与调度.md) §六）。

### 8.4 `VectorCacheStore`：整表锁的取舍

`Collections.synchronizedMap(LinkedHashMap)`（`VectorCacheStore.java:48-49`）是**整表一把锁**——按本篇标准它不是最优（读读也互斥、有全局热点行），但 L1 缓存 4096 条、QPS 内部调用，锁竞争毫秒级都到不了。**选型先量竞争强度再谈硬件优化**，这是比"上无锁结构"更重要的工程判断（LRU 细节见 [09-缓存淘汰](../11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存.md)）。

---

## 九、验证工具速查

```bash
# 微基准：测伪共享/锁/CAS 差距必须用 JMH（禁用 JIT 优化、预热、误差分析）
mvn archetype:generate -DgroupId=demo -DartifactId=jmh-demo \
  -DarchetypeGroupID=org.openjdk.jmh -DarchetypeArtifactId=jmh-java-benchmark-archetype -Dversion=1.37

# 伪共享定位（Linux perf，能直接指出"两个变量在同一缓存行上乒乓"）
perf c2c record -- java -jar bench.jar && perf c2c report

# 汇编级验证 volatile 生成 lock 前缀指令
java -XX:+PrintAssembly -XX:CompileCommand=print,Demo.volatileWrite Demo
```

---

## 十、面试高频问答

**Q1：什么是缓存行？为什么它是一致性的最小单位？**
A：CPU 与内存间搬运数据的最小单位，x86 上 64 字节。MESI 一致性协议以行为单位维护状态与消息，即使只写一个 8 字节变量也会让整行参与 Invalidate——这既带来空间局部性收益，也埋下伪共享。

**Q2：MESI 四状态？一次跨核写会发生什么？**
A：M（已改、脏、独占）/E（独占干净）/S（共享只读）/I（无效）。核写共享行：先广播 Invalidate，等所有核回 Ack 把各自行置 I，才能写并升 M；其他核再读时 miss，由 M 持有者写回转发。这就是跨核写贵、可见性有延迟的物理来源。

**Q3：什么是伪共享？怎么解决？项目里有吗？**
A：无关的热变量同处一个缓存行，多核互写导致缓存行在核间乒乓，性能掉一个量级。解法：手工补位到 64B 或 `@Contended`（需 `-XX:-RestrictContended`）。项目把限流桶设计为 per-key 对象（TokenBucketRateLimiter.java:25），结构上天然分散缓存行；没有高频数值数组热点，无现实伪共享。

**Q4：store buffer 是什么？它怎么造成乱序可见？**
A：核等待 Invalidate Ack 期间暂存写入的 FIFO 缓冲，让 CPU 不空等。副作用：本核后续读写可能越过尚未刷出的写（其他核先看到 ready 再看到 x 的旧值），x86 允许 StoreLoad 重排。内存屏障（StoreLoad 等）把这种放宽收紧回来。

**Q5：volatile 在硬件上做了什么？为什么说它保证可见性和有序性但不保证原子性？**
A：写 = 屏障 + 使其他核缓存行失效（x86 上是 lock 前缀指令），读 = 按一致性协议取最新并禁止后续访问提前。写读之间的 happens-before 由缓存一致性 + 屏障保证；但 count++ 是读-改-写三步，两核的执行交叉照样丢更新，原子性必须 CAS 或锁。

**Q6：CAS 原理？ABA 是什么、何时有害、怎么解？**
A：CAS 是 `lock cmpxchg` 单指令"比较并交换"，失败由调用方重试。ABA：值经历 A→B→A 后 CAS 仍成功——对纯数值无害，对含引用的结构（无锁栈/队列）是灾难，因为"值相同"不代表"结构没变"。解法：AtomicStampedReference 带版本号，或业务论证无害（如 InstanceInFlightRegistry 的计数器）。

**Q7：为什么 TokenBucketRateLimiter 用锁而不是 CAS？**
A：令牌补充+扣减是读-算-写复合逻辑（依赖 lastRefillNano 与 tokens 两个变量的中间态），CAS 单变量原子性表达不了复合不变式；且锁粒度是 per-bucket（单用户维度），热点天然分散，竞争极低。混合策略：Map 定位无锁 + 桶内短临界区锁。

**Q8：synchronized 锁内访问的变量需要 volatile 吗？**
A：不需要。锁的释放自带 Store 屏障、获取自带 Load 屏障（monitorenter/exit 的内存语义），同一把锁内的读写有完整 happens-before。只有绕开锁的无同步访问才需要 volatile——把 TokenBucketRateLimiter 的 Bucket 字段标 volatile 纯属多余。

**Q9：AtomicReference 在响应式代码里为什么常见？**
A：两个原因叠加：lambda 修改局部变量被编译器禁止（需要容器）；Flux 的回调可能来自不同线程（需要跨线程可见性）。AtomicReference 的 set/get 携带完整 volatile 内存语义，一举两得——项目 ResilientAiService.java:277 用它在 doOnNext/doFinally 之间传递 Usage。

**Q10：什么时候用 LongAdder 替代 AtomicLong？**
A：高竞争统计场景。AtomicLong 所有线程 CAS 同一个缓存行，失败自旋浪费；LongAdder 把计数拆成 Cell[]（每个 Cell 独占缓存行，内部就是防伪共享设计），读时 sum 汇总。低竞争下两者无差，AtomicLong 反而省内存且能 get 精确值。

---

## 十一、动手练习

1. 用 JMH 写一个对照实验：单类两个 `volatile long`（同缓存行）vs 各自补位 7 个 long，两线程分别自增，量化伪共享的吞吐差（预期一个数量级）。
2. 给 `TokenBucketRateLimiter` 写一个并发正确性测试：N 线程 × M 次 `tryAcquire`（qps 已知），断言放行总数 ≈ 突发 + qps×时长；再用 `jshell` 验证把 `synchronized(bucket)` 去掉后结果如何漂移。
3. 读 `InstanceInFlightRegistry.java:39-55`，回答并写下来：为什么 `decrement` 要 `Math.max(0, ...)`？如果换成 `AtomicInteger.decrementAndGet()` 会出现什么业务异常（最少连接均衡选出负数实例？）。
4. 用 JMH 对比 `AtomicLong.incrementAndGet` 与 `LongAdder.increment` 在 1/4/16 线程下的吞吐，画出曲线，对照 §7.3 表格解释拐点位置。
5. 改造练习：给 `InstanceInFlightRegistry` 评估"多副本网关共享在途计数"方案——用 Redis INCR/DECR（原子性由谁保证？对照 [04-中间件/01-Redis缓存实战](../04-中间件/01-Redis缓存实战.md)）对比进程内 CAS 的延迟差，写 10 行取舍说明（类注释 `:22-24` 已给出作者观点，先读再写）。

---

> 上一篇：[07-字符编码与时区](./07-字符编码与时区.md)
