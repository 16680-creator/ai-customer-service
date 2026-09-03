# 性能工程

> 本专题是 `learning-docs` 的 **第十二个模块**（`12-性能工程`）。前面的模块教你"把功能做对"，本模块教你"把系统做快"——以及在"变慢"发生时，**用数据定位根因**，而不是靠猜。
> **最大特色**：所有分析都锚定 `ai-customer-service` 的真实 JVM 参数、真实索引 DDL、真实线程池配置、真实限流过滤器；项目没落地的主题（火焰图工具、MAT 流程）会**显式标注"工程未落地"**并给出目标态方案。
> 前置阅读：[00-学习路线总览](../00-学习路线总览/README.md)、[01-Java基础/03-JVM内存模型与GC实战](../01-Java基础/03-JVM内存模型与GC实战.md)、[08-测试/05-性能压测实战-k6与JMeter](../08-测试/05-性能压测实战-k6与JMeter.md)

---

## 一、为什么要专门学性能工程

功能会跑只是起点。上线之后真正消耗你时间的是：接口为什么慢、容器为什么被 kill、GC 为什么频繁、SQL 为什么全表扫描。这四类问题有一个共同点：**没有测量就没有答案**。

| 场景 | 只会写功能 | 懂性能工程之后 |
|---|---|---|
| 容器里的服务被 OOMKilled（exit 137） | 重启了事，隔天再犯 | 知道 RSS = 堆 + 元空间 + 线程栈 + 堆外，知道 `-Xmx` 与容器限额的关系，知道 `MaxRAMPercentage` 怎么算 |
| 压测 p95 超标 | 加机器 / 调线程池碰运气 | 先看 GC 日志停顿、再火焰图定位热点、再 EXPLAIN 慢 SQL，每一步有数据 |
| 接口偶尔 500ms、偶尔 5s | "网络抖动吧" | 分层归因：缓存命中走了哪一级、LLM 首 token 占了多少、下游超时有没有触发熔断 |
| 写一个内存缓存 | `new HashMap<>()` 直接上 | 有界 + 淘汰 + 降级，像本项目 `VectorCacheStore` 那样从根上防住泄漏 |
| 面试被问"你优化过什么" | 讲不出数据 | 能说清基线是多少、改了什么、收益是多少、怎么验证的 |

一句话：**性能工程的本质是测量学，不是玄学。** 本项目的 k6 脚本、GC 日志配置、限流 429、TTFT 指标都是现成的测量抓手。

### 与既有模块的分工边界（查重声明）

| 主题 | 已在哪讲过 | 本模块只讲 |
|---|---|---|
| JVM 内存区域、GC 基础、G1 机制 | [01-Java基础/03](../01-Java基础/03-JVM内存模型与GC实战.md) | GC **选型对比**、容器内存计算、日志逐字段解读、泄漏排查深化 |
| 并发基础、线程池入门 | [01-Java基础/04](../01-Java基础/04-并发编程进阶.md) | 线程池**调优**（估算、容量、动态调参）与背压体系 |
| 限流算法原理（令牌桶/滑动窗口） | [11-数据结构与算法/10](../11-数据结构与算法/10-限流算法-四大算法与网关实现.md) | 限流在**容量防御链**中的位置（线程/吞吐维度） |
| 缓存淘汰算法与两级缓存结构 | [11-数据结构与算法/09](../11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存.md) | 缓存的**防泄漏与性能收益**视角 |
| 索引基础、EXPLAIN 入门、慢日志开启 | [03-数据库与ORM/01](../03-数据库与ORM/01-MySQL核心知识.md) | EXPLAIN **全列**、失效八场景、ICP、深分页、join 的实操 |
| 锁与主从复制 | [03-数据库与ORM/05](../03-数据库与ORM/05-MySQL锁机制与主从复制读写分离.md) | 不涉及（只做链接引用） |
| 压测工具与脚本用法 | [08-测试/05](../08-测试/05-性能压测实战-k6与JMeter.md) | 拿到压测数据后的**定位方法论** |

---

## 二、知识地图

```
                              性能工程
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
   【JVM 层】                【代码与数据层】           【链路与容量层】
        │                        │                        │
   GC 选型 G1/ZGC            SQL 调优 EXPLAIN          接口优化三板斧
   ├ 停顿 vs 吞吐            ├ type 等级/Extra         ├ N+1 与批量化
   ├ 容器内存计算             ├ 索引失效八场景           ├ CompletableFuture 并行
   │  (MaxRAMPercentage)     ├ 覆盖索引/ICP            ├ 缓存分层（三级）
   ├ GC 日志逐字段解读        ├ 深分页/join             └ 大对象与序列化
   ├ jstat / 晋升失败         │                             │
   └ 内存泄漏排查            【剖析】                  【线程与背压】
      ├ heap dump            ├ 火焰图怎么读               ├ 线程池七参数
      ├ MAT 支配树           ├ ON-CPU vs OFF-CPU         ├ IO/CPU 密集估算
      └ 五大泄漏源            ├ 采样偏差                  ├ 队列与拒绝策略
                             └ async-profiler/JFR        ├ SSE 长连接
                                                          └ 背压：限流→队列→超时
                                 │
                          【方法论】
                          ├ USE / RED 指标
                          ├ 六层排查决策树
                          ├ 先测量后优化
                          └ 收益测算（阿姆达尔）
```

---

## 三、文档清单与学习路线

### 第一阶段：JVM 与内存（1 周）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 01 | [JVM 参数与 GC 选型](./01-JVM参数与GC选型.md) | G1/ZGC/Shenandoah 对比、堆与元空间必配参数、容器内内存计算、为什么要显式设堆（docker-compose 现场） | ⭐⭐ |
| 02 | [GC 日志与停顿分析](./02-GC日志与停顿分析.md) | `-Xlog:gc*` 逐字段解读、jstat 观测、晋升失败/To-space exhausted、用停顿数据反推参数 | ⭐⭐⭐ |
| 03 | [火焰图与性能剖析](./03-火焰图与性能剖析.md) | async-profiler/JFR/Arthas 对比、火焰图读法、ON-CPU vs OFF-CPU、采样偏差（⚠️ 工程未落地，目标态方案） | ⭐⭐⭐ |
| 04 | [内存泄漏排查实战](./04-内存泄漏排查实战.md) | heap dump 四种获取、MAT 支配树/泄漏报告、五大泄漏源、`VectorCacheStore` 有界缓存正例 | ⭐⭐⭐ |

### 第二阶段：SQL 与接口（1 周）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 05 | [SQL 调优与执行计划](./05-SQL调优与执行计划.md) | EXPLAIN 全列、索引失效八场景、覆盖索引/ICP、前缀索引、深分页、join 优化（orders 表真实索引现场） | ⭐⭐⭐ |
| 06 | [接口性能优化实战](./06-接口性能优化实战.md) | N+1 与批量写入、CompletableFuture 并行、对话链路三级缓存、大对象、连接池（对话/订单双链路案例） | ⭐⭐⭐ |

### 第三阶段：容量与方法论（3-4 天）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 07 | [线程池调优与背压](./07-线程池调优与背压.md) | 七参数、IO/CPU 密集估算、队列与拒绝策略、Tomcat 线程配置、SSE 占线程与异步化、网关 429 背压 | ⭐⭐⭐ |
| 08 | [性能问题定位方法论](./08-性能问题定位方法论.md) | USE/RED 方法、六层排查决策树、先测量后优化、优化收益测算（与压测文档分工互补） | ⭐⭐ |

---

## 四、本项目中的性能现场（核心特色）

> 学完理论后，**回到这里对照配置和源码看一遍**。带 ⚠️ 的是"工程未落地"项——正是你要动手补的改造点。

| 性能点 | 项目现状 | 文件与位置 | 对应文档 |
|---|---|---|---|
| **业务服务堆显式设置** | 7 个 Java 服务统一 `JAVA_OPTS: "-Xms256m -Xmx512m"` | `deploy/docker-compose/docker-compose-all.yml:175`（gateway）起 7 处 | [01](./01-JVM参数与GC选型.md) |
| **中间件 JVM 配置** | Nacos `JVM_XMS/XMX`、ES `ES_JAVA_OPTS`、RocketMQ `JAVA_OPT_EXT` | 同文件 `:67-68` `:91` `:114` `:135` | [01](./01-JVM参数与GC选型.md) |
| **Dockerfile 参数透传差异** | chat/gateway 等用 `${JAVA_OPTS:-}` 透传；**order/product 没透传**（直接 `java -jar`，堆走默认 25%）⚠️ | `ai-cs-chat/Dockerfile:21` vs `ai-cs-order/Dockerfile:21`、`ai-cs-product/Dockerfile:11` | [01](./01-JVM参数与GC选型.md) |
| **GC 日志配置** | Nacos 启动脚本配了 `-Xlog:gc*:file=...:filecount=10,filesize=100m` | `tools/nacos/bin/startup.sh:114` | [02](./02-GC日志与停顿分析.md) |
| **剖析工具** | ⚠️ 全仓无 async-profiler/JFR/Arthas profiler 落地，无采样数据 | 全仓 grep 0 命中 | [03](./03-火焰图与性能剖析.md) |
| **有界缓存防泄漏（正例）** | L1 进程内 LRU + L2 Redis，容量由配置控制 | `ai-cs-chat/.../cache/VectorCacheStore.java:48-54` + `application.yml:173-176` | [04](./04-内存泄漏排查实战.md) |
| **ThreadLocal 清理（正例）** | Agent 编排结束 `finally` 里 `ChatUserContext.clear()` | `ai-cs-chat/.../agent/AgentController.java:177-180` | [04](./04-内存泄漏排查实战.md) |
| **OOM 自动 dump** | 文档示例配了 `-XX:+HeapDumpOnOutOfMemoryError`，**实际 Dockerfile/compose 未配** ⚠️ | `docs/05-部署上线.md:50-54` | [04](./04-内存泄漏排查实战.md) |
| **慢查询日志** | `slow-query-log=1`、`long-query-time=2` 已开启 | `deploy/mysql/mysql.cnf:19-21` | [05](./05-SQL调优与执行计划.md) |
| **orders 表索引** | `uk_order_no` / `idx_user_id` / `idx_status` / `idx_expire_time` | `deploy/mysql/order-init.sql:42-45` | [05](./05-SQL调优与执行计划.md) |
| **循环远程调用（改造点）** | 下单循环 Feign 扣库存、循环 insert 订单项 ⚠️ | `ai-cs-order/.../OrderServiceImpl.java:105-107` `:132-142` | [06](./06-接口性能优化实战.md) |
| **三级缓存命中顺序** | 热门问答 → 语义缓存 → 完整 RAG（LLM 秒级→缓存毫秒级） | `ai-cs-chat/src/main/resources/application.yml:165-187` | [06](./06-接口性能优化实战.md) |
| **TTFT 首 token 指标** | 流式调用记录 `firstTokenMs` | `ai-cs-chat/.../ResilientAiService.java:279-286` | [06](./06-接口性能优化实战.md) |
| **可观测性独立线程池** | usage/eval 两个 `ThreadPoolTaskExecutor`，隔离主链路 | `ai-cs-chat/.../config/ObservabilityExecutorConfig.java:27-58` | [07](./07-线程池调优与背压.md) |
| **SSE 长连接** | `SseEmitter` 5 分钟超时；Agent 用默认 `ForkJoinPool` ⚠️ | `ChatServiceImpl.java:156`、`AgentController.java:138` | [07](./07-线程池调优与背压.md) |
| **网关限流背压** | 超限直接 429 不转发下游（令牌桶/滑动窗口双算法） | `ai-cs-gateway/.../filter/RateLimitFilter.java:59-63` | [07](./07-线程池调优与背压.md) |
| **SSE 压测脚本** | 20 VU 挂长连接，首 token p95<2s 阈值 | `scripts/loadtest/k6/sse-chat.js` | [08](./08-性能问题定位方法论.md) |

---

## 五、速查表：现象 → 第一反应

> 排查时按这张表先动第一下手，别一上来就改代码。详细决策树见 [08](./08-性能问题定位方法论.md)。

| 现象 | 第一反应 | 命令/入口 | 深入 |
|---|---|---|---|
| 容器被 kill，exit 137 | RSS 超过容器限额，不是 Java OOM | `docker inspect <c> --format '{{.State.OOMKilled}}'` | [01 §五](./01-JVM参数与GC选型.md) |
| 接口偶发 200ms→3s | GC 停顿 / 缓存击穿 | `-Xlog:gc*` 日志找 `Pause ... ms` | [02](./02-GC日志与停顿分析.md) |
| 老年代持续 90%+ 不回落 | 内存泄漏 | `jmap -histo:live` + heap dump + MAT | [04](./04-内存泄漏排查实战.md) |
| CPU 100% | 热点代码 / GC 线程 / 正则回溯 | `top -Hp` → jstack → 火焰图 | [03](./03-火焰图与性能剖析.md) |
| SQL 慢日志有记录 | 执行计划退化 | `EXPLAIN` 看 type/rows/Extra | [05](./05-SQL调优与执行计划.md) |
| 列表接口越翻越慢 | 深分页 | 延迟关联 / 游标分页 | [05 §六](./05-SQL调优与执行计划.md) |
| 批量操作特别慢 | N+1 查询/写入 | 看循环里的 `select/insert/Feign` | [06 §二](./06-接口性能优化实战.md) |
| 并发一上来就 504/超时 | 线程池或连接池打满 | `jstack` 看线程状态、Hikari 指标 | [07](./07-线程池调优与背压.md) |
| 网关返回 429 | 限流生效（背压正常工作） | `RateLimitFilter` 日志 `触发限流: key=` | [07 §八](./07-线程池调优与背压.md) |
| SSE 首 token 慢 | 模型路由/RAG 检索在前 | 看 TTFT 指标与 k6 `http_req_waiting` | [06 §七](./06-接口性能优化实战.md) |

### 常用命令速查

```bash
# —— JVM ——
jps -l                                                    # 找进程
jstat -gcutil <pid> 1000                                  # GC 概况每秒刷
jcmd <pid> VM.flags                                       # 验证 JVM 参数实际生效
jcmd <pid> GC.heap_dump /tmp/heap.hprof                   # 导堆 dump
jstack <pid> > stack.txt                                  # 线程栈
jcmd <pid> JFR.start duration=60s settings=profile filename=/tmp/a.jfr   # 60 秒剖析（JDK17 零依赖）

# —— MySQL ——
docker exec -it aics-mysql mysql -uroot -proot
EXPLAIN SELECT ...;                                       # 执行计划
SHOW VARIABLES LIKE 'slow_query_log';                     # 慢日志开关
docker exec aics-mysql tail -20 /var/log/mysql/slow.log   # 看慢日志

# —— 容器 ——
docker stats --no-stream                                  # 容器 CPU/内存快照
docker exec aics-chat-service jcmd 1 VM.flags             # 进容器看 JVM 参数
docker inspect <c> --format '{{.State.OOMKilled}}'        # 排查 exit 137

# —— 压测 ——
k6 run -e BASE=http://localhost:8080 -e PATH=/api/chat/stream scripts/loadtest/k6/sse-chat.js
k6 run scripts/loadtest/k6/gateway-rate-limit.js          # 限流拐点
```

### 指标口径速记

| 口径 | 定义 | 常见误用 |
|---|---|---|
| TTFT（首 token 时延） | 发出请求到收到第一个 token | 别拿总时延冒充它；k6 里近似 `http_req_waiting` |
| p95/p99 | 95%/99% 分位延迟 | 别只看均值——长尾才是用户体感 |
| GC 停顿占比 | Σ停顿时长 / 观测窗口 | 并发标记是并发的，别计入 STW |
| 命中率 | 命中次数 / 总请求 | 分层缓存要逐层统计，混算无意义 |

---

## 六、学习方法建议

1. **先建立基线，再谈优化**：没有"优化前 p95 是多少"的数据，"优化后快了"就是自欺。k6 脚本已经给了你基线模板（[08-测试/05](../08-测试/05-性能压测实战-k6与JMeter.md)）。
2. **每篇文档配一次真实操作**：01 的参数改在 docker-compose 上试、02 的日志让 Nacos 跑起来看、05 的 EXPLAIN 在 aics_order 库里跑。
3. **对照"工程未落地"清单动手**：本模块标了 4 处 ⚠️（剖析工具、OOM dump、批量写入、Agent 专用线程池），每一处都是真实改造点，也是面试的"我做过什么"。
4. **先测量、再归因、后优化**：顺序错了会演变成"调参玄学"。养成写"优化记录"（基线→假设→改动→复测）的习惯。
5. **面试答题带现场**：本项目参数、类名、行号都是你的弹药库，比背概念有说服力得多。

### 学习检查清单（学完本模块应能）

- [ ] 说清本项目 11 个 JVM 进程各自的堆配置在哪个文件哪一行，order/product 的透传缺失风险
- [ ] 不查资料写出 G1/ZGC 选型决策树与容器内存预算公式
- [ ] 拿到一条 G1 日志，逐字段读出回收类型、前后占用、停顿时长，识别 To-space exhausted
- [ ] 用 jstat -gcutil 三十秒内给出"是否泄漏"的方向判断
- [ ] 打开 MAT 完成支配树 → Path to GC Roots 的完整定罪链
- [ ] 对 orders 表任一查询跑 EXPLAIN 并解读全部 12 列
- [ ] 指出本项目对话链路三级缓存的命中顺序与各自量级
- [ ] 解释 SSE 为什么吃线程，说出 Agent 链路的默认 ForkJoinPool 风险
- [ ] 画出背压四级闸门并指出每一级在项目里的落点
- [ ] 用 USE/RED + 六层决策树完整走一遍"接口 p95 暴涨"的演练
- [ ] 独立写一份含基线/假设/复测三段的优化记录，并能指出哪些主题在本项目"未落地"（4 处 ⚠️）

---

> 返回 [学习路线总览](../00-学习路线总览/README.md)
