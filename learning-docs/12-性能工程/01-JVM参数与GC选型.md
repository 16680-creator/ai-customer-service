# JVM 参数与 GC 选型

> 对应项目：`deploy/docker-compose/docker-compose-all.yml`（11 个 JVM 进程的内存参数全在这一个文件）、`ai-cs-chat/Dockerfile` 等 9 份 Dockerfile（参数透传入口）、`docs/05-部署上线.md`（ENTRYPOINT 示例）。
> 相关：[03-JVM内存模型与GC实战](../01-Java基础/03-JVM内存模型与GC实战.md)（内存区域/GC 基础与容器 OOMKilled 的坑已讲，本篇不重复）、[01-Docker容器化](../07-运维部署/01-Docker容器化.md)、[02-GC日志与停顿分析](./02-GC日志与停顿分析.md)（参数定了之后怎么看效果）。

---

## 一、先给结论：本项目该用哪个 GC、堆怎么设

| 问题 | 结论 | 一句话理由 |
|---|---|---|
| GC 用哪个？ | **G1（JDK 17 默认，不显式设置）** | 各服务堆仅 512m，G1 是 JDK 9+ 默认收集器，小堆下停顿/吞吐均衡；ZGC 的收益要在大堆（4G+）才体现 |
| 堆设多大？ | 业务服务 `-Xms256m -Xmx512m`（现状）；建议演进为 `Xms=Xmx` | Xms=Xmx 避免堆伸缩时的 resize 抖动 |
| 容器百分比参数？ | **建议补 `MaxRAMPercentage`** | 现状全部用绝对值写死；百分比方案对"一份配置跑多规格机器"更友好 |
| 最该补的参数？ | `-XX:+HeapDumpOnOutOfMemoryError` + `-Xlog:gc*` + `MaxMetaspaceSize` | 现状 7 个业务服务一个都没配（见 §二），出问题没有现场 |

**选择 G1 的完整决策树**（写进你的 ADR）：

```
堆大小 / 延迟要求？
├─ 堆 < 4G（本项目 512m）────────→ G1（JDK 17 默认，零配置成本）
├─ 堆 ≥ 4G 且 p99 停顿 < 10ms 是硬指标（如网关、实时推荐）
│     └─ JDK 21+？──→ 分代 ZGC（停顿 <1ms，吞吐损耗 ~5-10%）
│     └─ JDK 17？───→ ZGC 非分代（可用，但吞吐损耗更大）
└─ 批处理/吞吐优先，不在乎单次停顿 → Parallel GC（-XX:+UseParallelGC）
```

Shenandoah 何时考虑：非 Oracle JDK（Temurin/Amazon Corretto 自带）且已在用它、又想要低停顿——它和 ZGC 定位重叠，**没有存量就别新引入**。

---

## 二、项目现场：11 个 JVM 进程的内存参数

`deploy/docker-compose/docker-compose-all.yml` 是本项目唯一集中配 JVM 的地方（实际生效，非示例）。逐行核对如下：

| 进程 | 参数 | 位置 | 说明 |
|---|---|---|---|
| Nacos | `JVM_XMS: 256m` / `JVM_XMX: 512m` | `docker-compose-all.yml:67-68` | Nacos 官方镜像约定的环境变量名 |
| Elasticsearch | `ES_JAVA_OPTS: "-Xms512m -Xmx512m"` | `docker-compose-all.yml:91` | ES 官方约定变量；ES 还要堆外（mmap 索引） |
| RocketMQ NameServer | `JAVA_OPT_EXT: "-Xms256m -Xmx256m"` | `docker-compose-all.yml:114` | 轻量路由注册，256m 够用 |
| RocketMQ Broker | `JAVA_OPT_EXT: "-Xms512m -Xmx512m"` | `docker-compose-all.yml:135` | 承担存储与消费调度，比 namesrv 大 |
| 7 个业务服务 | `JAVA_OPTS: "-Xms256m -Xmx512m"` | `docker-compose-all.yml:175`（gateway）、`:193`（user）、`:214`（knowledge）、`:236`（chat）、`:254`（search）、`:275`（message）、`:294`（notify） | 单机全量部署，给每服务留余量 |

**Dockerfile 透传机制**（这是 `JAVA_OPTS` 能生效的原因）：

```dockerfile
# ai-cs-chat/Dockerfile:21（gateway/user/knowledge/message/notify/search 同款）
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
```

`${JAVA_OPTS:-}` 表示"环境变量没设就展开为空"。compose 里配了 `JAVA_OPTS` 就拼进 java 命令行——**环境变量到 JVM 参数的桥就在这一行**。

**⚠️ 关键差异（工程现场，直接影响内存安全）**：

```dockerfile
# ai-cs-order/Dockerfile:21 —— 没有 ${JAVA_OPTS} 透传！
ENTRYPOINT ["java", "-jar", "app.jar"]
# ai-cs-product/Dockerfile:11 —— 同样没有
ENTRYPOINT ["java", "-jar", "app.jar"]
```

order 和 product 两个服务**即使你在 compose 里配了 `JAVA_OPTS` 也不会生效**：JVM 走默认行为——按容器内存限额的 **25%**（`MaxRAMPercentage` 默认值）设最大堆，若容器没设 memory limit 则按宿主机总内存算。这就是 §五"为什么要显式设堆"的活教材。

**文档示例 vs 实际镜像**：`docs/05-部署上线.md:50-54` 的 ENTRYPOINT 示例带了一组好参数，但那只是文档，实际 9 份 Dockerfile 均未采用：

```dockerfile
# docs/05-部署上线.md:50-54（示例，未落地到真实 Dockerfile）
ENTRYPOINT ["java", \
    "-Xms256m", "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/logs/", \
    "-jar", "app.jar"]
```

---

## 三、G1 / ZGC / Shenandoah 选型对比

> GC 基础（Region、SATB、IHOP）见 [03-JVM内存模型与GC实战 §四](../01-Java基础/03-JVM内存模型与GC实战.md)，此处只做选型视角的横向对比。

| 维度 | G1（本项目默认） | ZGC | Shenandoah |
|---|---|---|---|
| 出现 | JDK 7u4，**JDK 9+ 默认** | JDK 11 实验，15 生产，**21 分代** | JDK 12 生产（随 OpenJDK 发行版） |
| 停顿量级 | 十~百毫秒级（目标可设） | **亚毫秒级**（分代版 <1ms） | 亚毫秒~十毫秒级 |
| 并发整理 | 部分并发（复制仍 STW，按 Region 分批） | **全并发**（着色指针 + 读屏障） | 全并发（Brooks 转发指针） |
| 吞吐损耗 | 小（~5%） | 中（5-10%，分代后明显改善） | 中 |
| 内存开销 | RSet 占堆 10-20% | 着色指针占引用位数（64 位下 4 bit）+ 转发表 | 转发指针每对象 1 字 |
| 最低堆下限 | 无硬性 | 官方建议 4G+ 才划算 | 无硬性 |
| 运维复杂度 | 低（默认即用） | 中（关注停顿统计口径） | 中（依赖发行版 backport） |
| 典型场景 | 微服务 512m~8G 通用 | 网关/实时引擎大堆低延迟 | 已用 Corretto/Temurin 的低延迟需求 |

**为什么本项目三个理由全部指向 G1**：
1. 堆 512m——ZGC 的停顿优势在小堆上被"GC 本来就快"稀释，反而多付读屏障开销；
2. JDK 17——分代 ZGC 要 21，17 的 ZGC 无分代，老年代回收全量扫描成本高；
3. 业务形态——对话/订单类服务 p99 停顿 100ms 内无感，G1 的 `MaxGCPauseMillis` 默认 200ms 已达标。

**启动参数怎么写**（G1 是默认，写不写都在；显式写的意义是"防止有人换了 JDK 或镜像后行为漂移"）：

```bash
# 可选：显式声明 + 停顿目标（本项目建议值）
-XX:+UseG1GC -XX:MaxGCPauseMillis=100
```

---

## 四、堆与元空间：业务服务必配的 6 个参数

> 内存区域划分（Eden/Survivor/Old/Metaspace）与对象分配规则不重复展开，见 [03-JVM内存模型与GC实战 §一§二](../01-Java基础/03-JVM内存模型与GC实战.md)。

| 参数 | 建议值（本项目 512m 档） | 为什么 |
|---|---|---|
| `-Xms` = `-Xmx` | 256m / 512m（现状）→ 建议 512m/512m | 不等时 JVM 需向 OS 扩/缩堆，引发额外 GC 与 RSS 抖动 |
| `-XX:MetaspaceSize=128m` | 初始阈值 | 默认约 21m 就触发第一次元空间 Full GC 重校准，Spring Boot 启动类加载量大，必踩 |
| `-XX:MaxMetaspaceSize=256m` | 封顶 | 防类加载器泄漏（CGLIB 代理、动态生成类）无限吃 RSS |
| `-XX:+HeapDumpOnOutOfMemoryError` | 开 | OOM 瞬间自动留 dump，没有它泄漏排查只能"守株待兔" |
| `-XX:HeapDumpPath=/app/logs/` | 指定目录 | 默认写工作目录，容器里常没有写权限 |
| `-Xlog:gc*:file=/app/logs/gc.log:time,uptime:filecount=5,filesize=20m` | 开 | 分析停顿/晋升的唯一数据源，见 [02](./02-GC日志与停顿分析.md) |

**元空间为什么单独强调**：本项目 chat 服务跑 Spring AI + 动态代理 + 大量 `@Configuration`，类数量比普通 CRUD 服务多 30%+。元空间不受 `-Xmx` 管，默认无限增长——它吃的是**堆外的 RSS**，溢出方式是容器 OOMKilled 而非 `OutOfMemoryError`。

---

## 五、容器内内存计算：UseContainerSupport 与 MaxRAMPercentage

### 5.1 两个参数的关系

```
UseContainerSupport（容器感知总开关，JDK 10+ / 8u191+ 默认开启）
   │
   ├─ 读取 cgroup 的 memory limit 作为"机器内存"
   │
   └─ MaxRAMPercentage（默认 25）→ MaxRAM = 容器限额 × 25%
      （与之互补：-Xmx 直接写死绝对值，二者只能生效一个，-Xmx 优先）
```

全仓 `grep -rn "MaxRAMPercentage\|UseContainerSupport"` 结果：**0 命中**——项目没有显式使用，全靠默认行为。这不是错误，但你需要知道默认行为是什么（下表）。

### 5.2 现状推演：每个服务的堆到底是多少

| 场景 | JVM 看到的"内存" | 最大堆 | 评价 |
|---|---|---|---|
| 7 个配了 `JAVA_OPTS` 的服务 | `-Xmx512m` 直接生效 | 512m | ✅ 明确可控 |
| order/product（无透传），容器无 mem limit | 宿主机内存（如 16G） | **4G** | ❌ 单机起 11 个服务时互相挤压，触发宿主 swap/OOM killer |
| order/product（无透传），容器 limit 1G | 1G | 256m | 堆偏小但安全；限额一变堆跟着变，性能不可预期 |
| ES | `-Xms512m -Xmx512m` | 512m | ✅ ES 堆外（mmap）另算，容器 limit 建议 ≥2G |

### 5.3 为什么必须给容器里的 JVM 显式设堆（三句话答完）

1. **默认 25% 太小或太飘**：限额 1G 只有 256m 堆，稍微高点并发就频繁 GC；不设限额则按宿主机算，多实例部署直接内存超卖。
2. **RSS ≠ 堆**：`RSS = 堆 + 元空间 + 线程栈(线程数×-Xss) + 直接内存(NIO/Netty) + JIT CodeCache`。堆外部分不受 `-Xmx` 约束，把 `-Xmx` 设成限额的 100% 必被 OOM Killer 杀（exit 137，`-XX:+HeapDumpOnOutOfMemoryError` 也救不了，因为根本没走到 Java OOM）——这个坑 [03-JVM内存模型与GC实战 §4.2](../01-Java基础/03-JVM内存模型与GC实战.md) 已详述。
3. **配额要成对出现**：`-Xmx` 或 `MaxRAMPercentage` 二选一显式化，且容器 `mem_limit` 要大于堆 + 堆外预算。

### 5.4 内存预算公式（拿去就能算）

```
容器 mem_limit ≥ MaxHeap + MaxMetaspace + 线程数 × Xss(默认1m) + DirectMemory + CodeCache(≈64~240m) + 余量

以 chat 服务为例（现状 512m 堆、Tomcat 200 线程但通常活跃 20~50）：
  512m(堆) + 256m(元空间) + 50×1m(栈) + 64m(直接内存) + 128m(CodeCache) ≈ 1.1G
  → 容器 mem_limit 建议 1.5G；若限额只有 1G，堆应降到 384m 或用 MaxRAMPercentage=40
```

### 5.5 两种设法的取舍

| 方案 | 写法 | 适合 | 缺点 |
|---|---|---|---|
| 绝对值 | `-Xms512m -Xmx512m` | 规格固定的单机 compose（**本项目现状**） | 换规格机器要逐个改 |
| 百分比 | `-XX:MaxRAMPercentage=50 -XX:InitialRAMPercentage=50` | K8s 多规格、HPA 弹性副本 | 需要确认容器真的设了 limit，否则按宿主机算又飘了 |

> 项目正在向 K8s 迁移（`deploy/k8s/` 已有部分 manifest，缺口见 [05-技术缺口分析](../00-学习路线总览/05-技术缺口分析与补全计划.md)），届时 percentages 方案是更优解。

---

## 六、可复制命令：给本项目补齐参数的三种姿势

```bash
# ① 单机 compose：改 JAVA_OPTS（保持 Xms/Xmx 原值，追加观测三件套）
#    deploy/docker-compose/docker-compose-all.yml:236 处的 ai-chat-service 举例：
#      JAVA_OPTS: "-Xms256m -Xmx512m
#                  -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m
#                  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/
#                  -Xlog:gc*:file=/app/logs/gc.log:time,uptime:filecount=5,filesize=20m"
docker compose -f deploy/docker-compose/docker-compose-all.yml up -d ai-chat-service

# ② 验证参数真的吃进去了（进容器问 JVM，而不是看配置文件）
docker exec aics-chat-service sh -c 'jcmd 1 VM.flags | tr " " "\n" | grep -E "MaxHeapSize|MetaspaceSize|UseG1GC"'
# 期望看到：-XX:MaxHeapSize=536870912（512m） -XX:+UseG1GC

# ③ 修复 order/product 透传缺失（对齐 ai-cs-chat/Dockerfile:21 的写法）
#    ai-cs-order/Dockerfile:21 改为：
#      ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]

# ④ 观察容器实际 RSS，验证预算公式
docker stats --no-stream | grep -E "aics-chat|aics-order"
```

> Nacos 的 GC 日志写法可抄：`tools/nacos/bin/startup.sh:114` 是仓库内唯一的真实 `-Xlog:gc*` 配置（`filecount=10,filesize=100m` 滚动），业务服务照抄改路径即可。

### 参数变更评审清单（每次动 JVM 参数前过一遍）

| 检查项 | 问题 | 不过关的后果 |
|---|---|---|
| 堆外预算 | 限额 − 堆 ≥ 元空间+栈+直接内存+CodeCache？ | OOMKilled，无 dump |
| Xms=Xmx？ | 两值是否一致 | 堆伸缩抖动 |
| dump 落盘 | HeapDumpPath 目录存在且可写？ | OOM 时 dump 失败，白配 |
| 日志滚动 | gc 日志 filecount×filesize 封顶多少？ | 磁盘被打爆 |
| 观测先行 | 改参数前的基线数据在哪？ | 改了说不清收益 |
| 单变量 | 一次只改了一组相关参数？ | 无法归因 |
| 回滚方案 | 旧值记录在案、能一键还原？ | 生产事故无法退回 |

---

## 七、面试高频问答

**Q1：为什么 JDK 17 项目不用显式指定 G1？**
A：JDK 9 起 G1 就是服务端默认收集器，`-XX:+UseG1GC` 写不写行为一致。显式写的价值在于防御性——防止基础镜像换了 JVM 发行版或参数集导致默认值漂移，以及让 `MaxGCPauseMillis` 等配套目标有挂载点。

**Q2：ZGC 和 G1 怎么选？**
A：看堆和延迟预算。堆 <4G、p99 停顿 100ms 内可接受 → G1，简单且吞吐好；堆 ≥4G 且要求 p99 <10ms（如网关、实时引擎）→ JDK 21 用分代 ZGC（<1ms），JDK 17 只能用非分代 ZGC（吞吐损耗更大，需实测）。本项目各服务堆 512m，G1 是唯一合理解。

**Q3：容器里的 JVM 为什么会被 OOMKilled 而不是抛 OutOfMemoryError？**
A：OOMKilled 是内核行为（cgroup memory 检测到超额直接 kill，exit 137），发生在进程申请物理页时；Java 的 `OutOfMemoryError` 是 JVM 内部对堆/元空间分配失败的响应。堆外内存（元空间、线程栈、直接内存、CodeCache）不受 `-Xmx` 管，把堆设满限额后这些堆外部分把 RSS 推过 cgroup 限额，内核就直接杀进程，dump 参数也不会触发。所以容器限额必须大于"堆 + 堆外"总预算。

**Q4：UseContainerSupport 和 MaxRAMPercentage 是什么关系？**
A：前者是容器感知总开关（JDK 10+/8u191+ 默认开启），让 JVM 读 cgroup 的 memory limit 当作机器内存；后者是在这个"感知内存"上计算最大堆的百分比（默认 25）。即 `MaxHeap ≈ 容器限额 × MaxRAMPercentage`。它与 `-Xmx` 互斥（`-Xmx` 优先），适合一份镜像跑多种规格容器的 K8s 场景。

**Q5：`-Xms` 和 `-Xmx` 为什么建议相等？**
A：不等时 JVM 在堆使用率高时向 OS 申请扩堆、空闲时归还，扩缩涉及全堆调整并伴随额外 GC，RSS 忽大忽小影响同宿主其他容器。设为相等是"用一点内存换确定性"的常规取舍。长跑服务内存稳态化后，抖动成本大于省下的闲置内存。

**Q6：本项目 order 服务镜像里 `java -jar` 没带任何参数，堆会是多大？有什么风险？**
A：走默认：JVM 容器感知开启时按容器 cgroup 限额的 25% 设最大堆；若容器没设限额则按宿主机总内存的 25%。风险有二：一是宿主机内存大时堆虚大，多服务同宿主时总承诺内存超卖，触发 OOM Killer；二是堆大小随部署环境漂移，性能基线不可复现。修复是让 Dockerfile 透传 `${JAVA_OPTS}`（对齐 `ai-cs-chat/Dockerfile:21`）或在镜像里写死参数。

**Q7：元空间不设上限会怎样？怎么设？**
A：Metaspace 默认无上限（受物理内存限制），类加载器泄漏（动态生成类、热部署残留）会持续吃堆外 RSS，最终容器 OOMKilled。设 `-XX:MaxMetaspaceSize` 封顶后，泄漏会以 `Metaspace` 的 `OutOfMemoryError` 显式暴露并自动 dump，问题从"玄学宕机"变成"有现场可查"。初始 `MetaspaceSize` 也要设，否则默认 21M 左右就触发首次 Full GC 重校准。

**Q8：Shenandoah 什么场景值得用？**
A：主要是"已经在用自带 Shenandoah 的发行版（Temurin/Corretto 等 OpenJDK 分发）且需要低停顿"的存量场景。它与 ZGC 定位重叠：新项目选型时 JDK 21 直接选分代 ZGC，JDK 17 及以下低延迟需求不强烈就用 G1，没有必要为一个新能力引入 Shenandoah 的额外运维面。

---

## 八、动手练习

1. 用 `docker exec aics-chat-service jcmd 1 VM.flags` 导出当前全部生效参数，与 compose 里写的 `JAVA_OPTS` 逐条比对，找出"配置了但没生效"或"默认值出乎意料"的参数各一个。
2. 复刻 §5.4 的预算公式给 `ai-cs-search`（ES 客户端 + Tomcat + Spring）算一份内存预算，并给出容器 mem_limit 与堆参数建议。
3. 修 `ai-cs-order/Dockerfile:21` 的透传缺失（抄 `ai-cs-chat/Dockerfile:21` 一行即可），本地 `docker build` 后用 `jcmd 1 VM.flags` 验证 `JAVA_OPTS` 生效。
4. 给 compose 里 7 个业务服务的 `JAVA_OPTS` 追加 §四的 6 个参数，压测一轮（k6 脚本见 `scripts/loadtest/k6/`），对比追加前后 `docker stats` 的 RSS 曲线。
5. 写一段 ADR（架构决策记录）：为什么本项目 512m 堆选 G1 而不是 ZGC；什么条件下触发重评（提示：堆升级到 4G+ / 升 JDK 21 / p99 停顿 SLA 收紧）。

---

> 下一篇：[02-GC日志与停顿分析](./02-GC日志与停顿分析.md)
