# JVM 内存模型与 GC 实战

> 本项目使用 **JDK 17**（默认收集器 **G1**）。
> 来源：[00-学习路线总览/04-Java基础补全开发计划](../00-学习路线总览/04-Java基础补全开发计划.md) P1。
> 对应项目：`docker-compose.yml` 多容器部署、各服务启动脚本、根目录遗留运行日志。

---

## 一、运行时数据区

```
┌────────────────────────────────────────────┐
│                  JVM 进程                    │
│  ┌──────────────────────────────────────┐  │
│  │              堆（Heap）               │  │  ← 线程共享
│  │  ┌─────────┐  ┌──────────────────┐   │  │
│  │  │ 年轻代   │  │    老年代         │   │  │
│  │  │Eden+S0+S1│  │  （G1 是 Region） │   │  │
│  │  └─────────┘  └──────────────────┘   │  │
│  └──────────────────────────────────────┘  │
│  ┌──────────────────────────────────────┐  │
│  │  元空间 Metaspace（JDK8+，取代永久代）  │  │  ← 线程共享，存类元数据
│  └──────────────────────────────────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐   │
│  │虚拟机栈   │ │本地方法栈 │ │程序计数器   │   │  ← 线程私有
│  └──────────┘ └──────────┘ └───────────┘   │
└────────────────────────────────────────────┘
```

| 区域 | 线程共享 | 存什么 | 会 OOM 吗 |
|------|---------|--------|-----------|
| 堆 | ✅ | 对象实例、数组 | 会（`java.lang.OutOfMemoryError: Java heap space`） |
| 元空间 | ✅ | 类元数据、方法字节码、运行时常量池 | 会（`Metaspace`，类加载器泄漏时） |
| 虚拟机栈 | 私有 | 栈帧（局部变量表、操作数栈） | 会（`StackOverflowError` / `OOM: unable to create thread`） |
| 程序计数器 | 私有 | 当前执行字节码行号 | 唯一不会 OOM 的区域 |
| 直接内存 | — | NIO `DirectByteBuffer`、Netty ByteBuf | 会（`Direct buffer memory`），不算堆但受进程内存约束 |

**本项目对照**：chat 服务里有大量短生命周期对象（每次对话的 `Document` 分片、Prompt 字符串、
JSON 反序列化临时对象）——都分配在**年轻代 Eden 区**，靠 Minor GC 快速回收；
而 `AgentToolRegistry` 里注册的工具 Bean、Spring 容器本身则是**老年代**常驻对象。

---

## 二、对象的一生与分配规则

```java
// 一次 AI 对话请求中对象的典型生命周期
public Result<ChatResponse> chat(ChatRequest req) {
    // 1. 绝大多数对象在 Eden 区分配（TLAB：每个线程在 Eden 预分配一小块，
    //    分配时只需指针碰撞 + CAS 失败重试，无需全局加锁）
    List<Document> chunks = splitIntoChunks(req.getMessage());

    // 2. Eden 满 → 触发 Minor GC：存活对象复制到 S0/S1，年龄 +1
    // 3. 年龄 > 15（默认）或大对象（超过 -XX:PretenureSizeThreshold 语义，G1 按 Region 判断）
    //    → 晋升老年代

    // 4. 逃逸分析：如果对象不逃出方法，HotSpot 通过【标量替换】把它拆成基本类型
    //    放在栈上（HotSpot 并没有真正的"栈上分配"，这点面试容易被追问）
    var temp = new int[]{chunks.size(), req.getSessionId().length()};
    return Result.success(new ChatResponse(200, assemble(chunks)));
}
```

**对象进入老年代的场景**（面试常问）：
1. 年龄达到阈值（默认 15，G1 动态判定会提前）
2. 大对象直接进老年代（G1 中超过 Region 一半的对象是 Humongous 对象）
3. 动态年龄判定：S 区同龄对象总和 > 一半，大于该年龄的全部晋升
4. Minor GC 后 Survivor 放不下的存活对象

---

## 三、GC 基础：怎么判断"该回收了"

### 3.1 可达性分析（不是引用计数）

Java 用**可达性分析**：从 **GC Roots** 出发，不可达的对象即可回收。

**GC Roots 包括**：虚拟机栈（局部变量）引用的对象、静态字段引用的对象、
常量引用、JNI 引用、活跃线程、锁持有的对象等。

> 为什么不用引用计数？循环引用数不掉（A→B、B→A 都不为 0）。
> Python/早期 COM 用引用计数 + 辅助机制，Java/JVM 系一律可达性分析。

### 3.2 四种引用（强软弱虚）——ThreadLocal 与缓存的底子

```java
Object strong = new Object();                 // 强引用：可达就永不回收（哪怕 OOM）
SoftReference<Cache> soft = new SoftReference<>(cache);   // 软引用：内存不足才回收（适合缓存）
WeakReference<Key> weak = new WeakReference<>(key);       // 弱引用：下次 GC 必回收
                                              // ← ThreadLocalMap 的 key 就是弱引用
PhantomReference<T> phantom =                 // 虚引用：唯一用途是配合 ReferenceQueue
    new PhantomReference<>(obj, queue);       //   获知对象被回收（堆外内存释放用它）
```

---

## 四、收集器：从 Parallel 到 G1（JDK 17 默认）

| 收集器 | 算法 | 线程 | 状态 | 一句话 |
|--------|------|------|------|--------|
| Serial / Serial Old | 复制 / 标记整理 | 单线程 | 仍在 | 客户端/小内存 |
| Parallel Scavenge / Old | 复制 / 标记整理 | 多线程 | 仍在 | **吞吐量优先**，批处理 |
| CMS | 标记清除 | 并发 | **JDK 14 移除** | 历史：首停顿低，但有碎片、并发模式失败 |
| **G1** | Region 化标记整理+复制 | 并发 | **JDK 9+ 默认** | **停顿可预测**，大堆首选 |
| ZGC | 着色指针+读屏障 | 并发 | JDK 15 生产 | 停顿 <1ms（JDK 21 起分代） |

### 4.1 G1 核心机制（面试重点）

- **Region 化堆**：堆切成 ~2048 个等大 Region（1~32MB，2 的幂），每个 Region
  动态扮演 Eden / Survivor / Old / Humongous 角色，物理上不再有"连续的分代"
- **可预测停顿**：`-XX:MaxGCPauseMillis=200`（默认目标 200ms），G1 根据每个 Region
  的**回收价值**（垃圾比例 × 回收耗时）排序，优先回收价值高的——这就是名字 Garbage First 的由来
- **Mixed GC**：老年代占比超过 IHOP（`-XX:InitiatingHeapOccupancyPercent`，默认 45%）
  后启动并发标记周期，随后几轮"年轻 GC + 部分老年代 Region"混合回收
- **SATB**（Snapshot-At-The-Beginning）+ **Remembered Set** 处理跨 Region 引用，
  写屏障维护 RSet（这是 G1 内存开销比 CMS 高的原因，通常占堆的 10%~20%）

### 4.2 常用参数（本项目可直接用的建议值）

```bash
# 微服务容器内（每个服务实例 512MB~1GB 堆足够）
java -Xms1g -Xmx1g \                          # Xms=Xmx 避免堆伸缩抖动（务必相等）
     -XX:MetaspaceSize=128m \                 # 初始元空间，避免启动期 Full GC
     -XX:MaxMetaspaceSize=256m \              # 封顶，防类加载器泄漏吃光内存
     -Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=20m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/logs/dump/ \           # OOM 自动留现场（必配！）
     -jar ai-cs-order-1.0.0-SNAPSHOT.jar
```

> **容器里的坑（必知）**：JDK 8u191+/10+ 感知 cgroup 限制，但如果你 `-Xmx` 设得
> 比容器 limit 大，JVM 堆外内存 + 线程栈 + Metaspace 会把 RSS 推过限额，
> 被 Linux OOM Killer **直接 kill（exit 137 / OOMKilled）**——这不是 Java 的
> `OutOfMemoryError`，`-XX:+HeapDumpOnOutOfMemoryError` 也不会触发。
> 容器限额 2G 时，`-Xmx` 建议 ≤ 1.5G 给堆外留空间。

---

## 五、排查工具链（实战顺序）

### 5.1 命令行四件套

```bash
jps -l                          # 1. 找进程
jstat -gcutil <pid> 1000        # 2. 每秒看一次 GC 概况
#   S0 S1   E    O    M    CCS  YGC YGCT  FGC FGCT  GCT
#    0 100 45.2 78.9 95.1  89.0 152 3.2   8   2.1   5.3
#   ↑ O 持续 90%+ 且 FGC 频繁 → 老年代出问题；M 95%+ → 元空间可能泄漏

jmap -histo:live <pid> | head -20        # 3. 看对象直方图（哪个类实例最多）
jmap -dump:live,format=b,file=heap.hprof <pid>   # 4. 导堆（live 会先触发 Full GC）

jstack <pid> > thread.txt       # 线程栈：看锁等待、死锁、CPU 热点线程
jcmd <pid> VM.flags             # 看 JVM 实际生效参数（验证配置有没有吃进去）
```

### 5.2 Arthas（生产不敢 jmap 时的首选）

```bash
dashboard                # 实时：线程、内存、GC 总览
thread -n 3              # CPU 最高的 3 个线程（自带把 tid 转成栈）
thread --state BLOCKED   # 被锁住的线程（对应本项目状态机 synchronized 段）
heapdump /tmp/heap.hprof # 等价 jmap dump 但更温和
sc -d com.aics.common.result.Result   # 查类加载器/元数据（排查类泄漏）
```

### 5.3 MAT 分析 dump 的三步法

1. **Leak Suspects 报告**：自动找嫌疑（通常是"一个集合持有 N 万个对象"）
2. **支配树（Dominator Tree）**：按 Retained Heap 排序，找到"一个人拖住一大片"的对象
3. **GC Roots 引用链**：右键 → Path to GC Roots (exclude weak/soft) —— 定位是谁引用着它

---

## 六、两个经典现场（与本项目对照）

### 现场 1：缓存无界增长 → 堆 OOM

```java
// ❌ 反面教材：本地内存缓存没有上限（和 02-中间件计划里"Redis 无界 key"同理）
private final Map<String, ChatResponse> cache = new HashMap<>();  // 永不清理

// ✅ 正确姿势一：LRU 上限（见 05-集合框架与源码 的手写实现）
// ✅ 正确姿势二：Caffeine 带权重与过期
Cache<String, ChatResponse> cache = Caffeine.newBuilder()
    .maximumWeight(64 * 1024 * 1024)     // 按字节数限重
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .weigher((k, v) -> v.reply().length())
    .build();
```

### 现场 2：CPU 100% 排查（五步法，背下来）

```bash
top                          # 1. 找到吃 CPU 的 java 进程 pid
top -Hp <pid>                # 2. 找到进程内吃 CPU 的线程 tid
printf '%x\n' <tid>          # 3. tid 转十六进制（如 0x1a2b）
jstack <pid> | grep -A 30 '0x1a2b'   # 4. 按 nid 找线程栈
# 5. 看栈顶在干嘛：死循环？正则回溯？序列化大对象？GC 线程（GC task thread）？
```

> 如果第 4 步发现是 `GC task thread` 在吃 CPU → 不是业务问题，是 GC 问题，
> 回到 `jstat -gcutil` 看 FGC 频率，大概率老年代泄漏或堆太小。

---

## 七、高频面试题（含参考答案）

**Q1：JVM 内存区域哪些线程共享、哪些私有？私有区域为什么必须私有？**
A：堆、元空间共享；虚拟机栈、本地方法栈、程序计数器私有。栈私有是因为每个线程的
方法调用链必须独立（局部变量不能混）；程序计数器私有是因为线程切换后要恢复到正确的
执行位置，多字节码指令不是原子的，必须每线程记自己的行号。

**Q2：对象一定分配在堆上吗？**
A：不一定。经 JIT 逃逸分析后，不逃逸的对象会做**标量替换**——把对象拆散成基本类型
分配在栈帧里（HotSpot 没有"栈上分配对象"的实现，效果由标量替换达成）；此外还有
**锁消除**（无竞争的同步对象锁被去掉）和**TLAB 快速分配**。大对象或堆外场景也有例外。

**Q3：什么情况下对象会进入老年代？**
A：① 年龄达阈值（默认 15）；② 大对象直接进（G1 为 Humongous Region）；③ 动态年龄
判定（Survivor 中同龄对象超一半整体晋升）；④ Minor GC 后 Survivor 装不下的存活对象。

**Q4：G1 和 CMS 的本质区别？为什么 JDK 9 后默认 G1、14 移除 CMS？**
A：CMS 基于**标记-清除**，老年代连续、有碎片，标记时 STW（re-mark）随堆增大而不可控，
且有并发模式失败退化为 Serial Old 的风险；G1 把堆 **Region 化**，以**停顿预测模型**
为基础优先回收价值高的 Region，局部复制整理、无碎片，停顿目标可控。CMS 的根本缺陷
（碎片 + 不可控 STW）在大堆微服务里致命，故被移除。

**Q5：G1 的 Mixed GC 什么时候触发？**
A：并发标记周期结束后进入 Mixed 阶段。触发条件：老年代占用超过 IHOP
（默认堆的 45%，自适应模式下动态调整），先并发标记，再分若干轮同时回收
年轻代 + 部分老年代 Region，直到满足停顿目标或清理完。

**Q6：强软弱虚四种引用的区别与使用场景？**
A：强——可达即不回收；软——内存不足时回收，适合缓存（如图片缓存）；弱——下次 GC
即回收，`ThreadLocalMap` 的 key 就是弱引用（防止 ThreadLocal 对象泄漏）；虚——
不能通过它拿到对象，唯一作用是对象回收时收到通知（ReferenceQueue），堆外内存
（DirectByteBuffer）靠它释放。**软引用适合缓存但会让 GC 变慢，高并发下更推荐
显式容量上限（Caffeine）**。

**Q7：内存泄漏和内存溢出什么关系？**
A：泄漏是原因之一，溢出是结果。泄漏 = 该回收的对象被静态集合/ThreadLocal/未关闭
资源等长期持有，老年代逐步堆满 → FGC 频繁 → 最终 OOM。但 OOM 也可能没有泄漏：
堆配小了、瞬时流量造大对象、元空间不够等。排查先看 dump 直方图判断"涨得快的是
不是该死的对象"。

**Q8：线上 OOM 了，你的排查步骤？**
A：① 先看 `-XX:+HeapDumpOnOutOfMemoryError` 是否已配（有 dump 直接 MAT）；没有则
`jmap -dump` 抓一份（服务可短暂 STW 才允许）；② MAT 看 Leak Suspects + 支配树找
Retained Heap 大头；③ Path to GC Roots（排除弱软引用）找持有者；④ 结合代码修复
（加上限/过期/换 Caffeine）；⑤ 上限兜底：缓存容量、分页大小、导出行数都要有 max。

**Q9：容器里 Java 服务被 OOMKilled（exit 137），但没有 OOM 异常，为什么？**
A：这是 cgroup 层面的问题：JVM 的 RSS（堆 + 元空间 + 线程栈 + 堆外直接内存 + GC 开销）
超过容器 memory limit，被内核 OOM Killer 杀掉，Java 的 `OutOfMemoryError` 根本没机会
抛。对策：`-Xmx` 留出堆外余量（约 limit 的 70~75%）、限制 `-XX:MaxMetaspaceSize`、
控制线程数（每个线程栈默认 1MB）、`-XX:NativeMemoryTracking=summary` 定位堆外去向。

**Q10：CPU 100% 怎么定位？（完整说一遍五步法）**
A：见上文第六节——top 找进程 → top -Hp 找线程 → tid 转十六进制 → jstack 按 nid 定位栈
→ 看栈顶。要能说出"如果栈顶是 GC 线程则转 GC 排查"这一步，是加分项。

**Q11：Metaspace 和永久代的区别？什么会导致 Metaspace 涨？**
A：永久代（≤JDK7）在堆内、大小固定易 OOM；JDK8 起改 Metaspace，用本地内存，
默认不设上限。涨的常见原因：动态生成类（CGLIB 代理、Groovy 脚本、反射
`GeneratedMethodAccessor`）、类加载器泄漏（热部署/插件未卸载）。`jstat -gcutil` 的
M 列持续 95%+ 或 `jcmd GC.class_stats` 看类数量异常增长即可确认。

**Q12：-Xms 和 -Xmx 为什么要设成一样？**
A：堆伸缩需要向 OS 申请/归还内存，伸缩过程伴随 Full GC 与页错误，生产流量波动下
会造成周期性延迟毛刺。设为相等以空间换稳定；内存紧张的环境（容器限额）则显式
规划好余量再等值分配。

---

## 八、学习检查清单

- [ ] 能画出运行时数据区并说出每个区域 OOM 的异常名
- [ ] 说得出对象进老年代的 4 种场景、G1 的 Humongous 对象判定
- [ ] 能用 `jstat -gcutil` 的输出判断是堆问题还是元空间问题
- [ ] 背下 CPU 100% 五步法，知道栈顶是 GC 线程时怎么办
- [ ] 知道容器 OOMKilled 与 Java OOM 的区别及参数余量规划
- [ ] 能用 MAT 三步法（Leak Suspects → 支配树 → GC Roots 链）走完一个 dump

## 九、动手实践（对照 04 计划 P1 任务）

1. 给 `ai-cs-order` 本地启动加 `-Xms1g -Xmx1g -Xlog:gc*`，压测时 `jstat -gcutil` 观察一轮
2. 写一个无界缓存导致 OOM 的最小案例 → `jmap` dump → MAT 截图支配树 → 修复为 Caffeine
3. 触发一次购物车并发请求，`jstack` 里找到状态机 `synchronized` 上的 BLOCKED 线程

---

## 下一步

→ [04-并发编程进阶](./04-并发编程进阶.md)
