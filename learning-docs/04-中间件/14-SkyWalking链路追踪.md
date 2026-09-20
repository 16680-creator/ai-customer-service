# 14-SkyWalking 链路追踪：从零开始理解 APM（认知拓展，工程未落地）

> **定位**：本仓可观测栈是国际组合 **Prometheus + Grafana + Tempo + OTel Collector**（`deploy/observability/`，chat 经 micrometer-tracing-bridge-otel 输出 OTLP）。**SkyWalking 是国内 Java 岗 JD 出现率最高的 APM**，与 Tempo 是"同一问题的两派答案"。本篇讲清它是什么、怎么做到"无侵入"、与你手写的链路（`TraceInterceptor`）差在哪，并给接入评估。
> 前置阅读：[07-运维部署/04-Prometheus可观测性](../07-运维部署/04-Prometheus可观测性.md)（本仓观测栈）、[04-中间件/13](13-Nginx与OpenResty反向代理.md)、[02-Spring微服务/15](../02-Spring微服务/15-SpringAOP与声明式事务原理.md)（代理思想——Agent 与它同源不同路）。

---

## ⚡ 30 秒速记卡

```text
① APM = 应用性能管理：Trace(一次请求全链路) + Metric(指标) + 日志关联
② SkyWalking 三件套：Agent(Java 字节码增强，零代码) + OAP(聚合分析) + UI/存储
③ Trace 树 = 一个 TraceID 下挂多个 Span（entry/exit/local），靠 context 跨进程跨线程传播
④ 无侵入原理 = 启动时 javaagent 改字节码，在方法前后织入埋点——思想与 Spring AOP 同源，
   只是"改字节码"发生在类加载时而非"套代理"
⑤ 与本仓关系：Tempo+OTel 是"标准协议+自建 SDK"，SkyWalking 是"国产全家桶+字节码增强"；
   SkyWalking 亦支持接收 OTLP——理论上可与 Tempo 并存于同一 Collector
```

---

## 一、从"一次慢请求"说起：为什么需要 APM

你的客服对话一次请求会穿过：Gateway → chat → ES/Redis/RocketMQ → LLM API。线上反馈"慢"时：

- **没有 APM**：逐个服务翻日志、对时间戳，人工拼出链路；
- **有 APM**：拿 TraceID 一搜，整棵调用树 + 每段耗时直接展开，瓶颈段一眼可见。

你项目里已经手写过一条链路（`ai-cs-chat/.../observability/TraceInterceptor` + `TraceContextHolder.capture()/restore()` 跨线程传播、落库 `llm_trace`）——**APM 就是把这件手工活做成平台化、全自动**。这是最好的理解锚点。

---

## 二、架构与核心概念

```text
┌──────────┐  字节码增强   ┌─────────────┐   gRPC 上报   ┌─────────┐   查询   ┌──────────┐
│ 业务服务  │ ──加载时织入──►│ SkyWalking  │ ────────────► │  OAP    │ ───────► │   UI     │
│ (chat…)  │   埋点探针    │  Agent      │  Span 数据    │ 聚合/采样│          │ 拓扑/追踪 │
└──────────┘              └─────────────┘               └────┬────┘          └──────────┘
                                                             ▼
                                                    存储（ES / BanyanDB / H2）
```

| 概念 | 含义 | 与你手写链路的对应 |
|---|---|---|
| **Trace** | 一次请求的完整调用树，全局唯一 TraceID | 你的 `requestId` |
| **Span** | 树上节点：entry(进服务)/exit(出服务调 DB/HTTP/MQ)/local(本地段) | 你的 `llm_trace` 步骤记录 |
| **Segment** | 单个进程内的 Span 集合 | 你的单服务内 trace 段 |
| **上下文传播** | 跨进程走 HTTP 头（sw8），跨线程走装饰后的线程池/Runnable | 你的 `TraceContextHolder.capture()/restore()`——SkyWalking 自动做 |
| **采样** | OAP/Agent 侧可配采样率，控存储成本 | 你的 `ObservabilityProperties` 采样 |

---

## 三、无侵入的秘密：Java Agent 字节码增强

```text
启动参数：java -javaagent:skywalking-agent.jar -Dskywalking.agent.service_name=aics-chat -jar app.jar

流程：JVM 加载类时 → Agent 按 Plugin 匹配规则（如 okhttp/Mysql/RocketMQ/Feign…）
     → ByteBuddy 改写该类字节码，在方法前后插入"创建 Span / 上报"逻辑
     → 业务代码零改动，依赖零引入
```

**与 Spring AOP 的对照（高频追问）**：

| | Spring AOP | SkyWalking Agent |
|---|---|---|
| 织入对象 | 容器管理的 Bean | **所有匹配的类**（连 JDK/RocketMQ 客户端都埋） |
| 织入方式 | 运行期生成代理对象 | 类加载期改字节码 |
| 触发条件 | 注解/切点声明 | 插件规则内置 |
| 代码侵入 | 需在 Bean 体系内 | 完全零侵入 |

> 代价也要会说：字节码增强有**升级/兼容风险**（新版本依赖要等探针适配）、排障时堆栈出现织入帧——这是"魔法"的通病。

---

## 四、SkyWalking vs Tempo/OTel vs Zipkin/Jaeger 选型对照

| 维度 | SkyWalking | Tempo + OTel（本仓） | Zipkin / Jaeger |
|---|---|---|---|
| 接入方式 | Agent 字节码增强（零代码） | SDK/micrometer（要引依赖，你的 chat 已引） | SDK |
| 协议 | 自有 gRPC + 兼容 OTLP | **OTel 标准协议** | Zipkin JSON / OTLP |
| 存储 | ES/BanyanDB/H2 | 对象存储（Tempo 便宜、按 TraceID 查） | Cassandra/ES/内存 |
| 附加能力 | 拓扑图、告警、JVM 指标、**日志同 UI**、浏览器探针 | Grafana 生态联动（metrics/logs/traces 同屏） | 轻量、仅 Trace |
| 团队适配 | 国内资料多、中文 UI、开箱即用 | 需要自己拼栈（本项目已拼好） | 老牌简单 |
| 一句话 | **国产全家桶，快** | **标准协议，自由组合** | 轻量入门 |

> 记忆口径：**要快、要全、团队熟 SkyWalking → 上它；要标准协议、Grafana 一体化、长期投资 → OTel 系**。
> 两者并非二选一：OTel Collector 可把 OTLP 同时转发 Tempo 和 SkyWalking（后端可换，探针协议不变——这正是你仓选 OTel 的价值）。

---

## 五、如果要引入本项目：接入点评估（未落地）

| 方案 | 做法 | 评估 |
|---|---|---|
| 并行接入（推荐试验） | OAP + ES/BanyanDB 进 compose observability profile；Collector 加一条 `otlp/skywalking` exporter；chat 的 OTLP 上报**不用改** | 存量链路零改动，与 Tempo 双写对比两套 UI |
| Agent 全量替换 | 启动参数挂 agent，去掉 micrometer 依赖 | 覆盖面更大（DB/MQ 自动埋点），但与你手写 `llm_trace`（带 LLM token 成本维度）**互补不重叠** |
| 注意点 | SkyWalking 对 LLM 语义（token/模型/成本）无内建模型——这正是你自建 `ModelUsageRecorder` 的价值 | 通用 APM ≠ AI 可观测，两者拼图才完整 |

---

## 六、动手实验（15 分钟）

```bash
# 1. 起一套最小 SkyWalking（H2 存储，试验用）
docker run -d --name oap -p 11800:11800 -p 12800:12800 apache/skywalking-oap-server:10.0.1
docker run -d --name swui -p 8080:8080 \
  -e SW_OAP_ADDRESS=http://oap:12800 apache/skywalking-ui:10.0.1

# 2. 把任意一个本仓服务用 agent 方式启动（指向 oap）
#    java -javaagent:/path/skywalking-agent.jar \
#         -Dskywalking.collector.backend_service=oap:11800 \
#         -Dskywalking.agent.service_name=ai-cs-order -jar ai-cs-order.jar
# 3. 打一次 /cart/list → UI 里看拓扑与调用树，对比 Tempo 里的同一请求
```

---

## 七、面试要点总结

```text
关键词：
APM 三支柱 Trace/Span/日志 · Agent=javaagent+ByteBuddy 类加载期织入（零侵入）
entry/exit/local Span · 跨进程 sw8 头 / 跨线程装饰线程池（对照手写 capture/restore）
SkyWalking(国产全家桶+Agent) vs OTel/Tempo(标准协议+SDK) · 采样控成本
项目锚点：TraceInterceptor+TraceContextHolder（手写版）· micrometer-bridge-otel OTLP→Collector→Tempo
         ModelUsageRecorder（LLM 维度 APM 不覆盖 → 互补）
```

## 学习检查清单

- [ ] 能画出 Agent→OAP→存储→UI 四段架构
- [ ] 能解释字节码增强与 Spring AOP 的同源与差异
- [ ] 能说出 Trace/Span/Segment 与本仓手写链路字段的对应关系
- [ ] 能给"SkyWalking vs Tempo"一套有条件的选型口径
- [ ] 能说出"通用 APM 为什么覆盖不了 LLM 用量观测"

## 下一步

- [04-中间件/09-AI与治理中间件部署](09-AI与治理中间件部署.md)：本仓已部署的治理组件；
- [07-运维部署/04-Prometheus可观测性](../07-运维部署/04-Prometheus可观测性.md)：本仓观测栈完整搭建。
