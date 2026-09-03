# SLI · SLO 与错误预算

> 对应项目：`ai-cs-gateway/src/main/resources/application.yml`（网关限流参数，SLO 设计素材）、
> `ai-cs-chat/src/main/resources/application.yml`（SSE 首 token 超时约束）、
> `scripts/loadtest/k6/`（gateway-rate-limit.js / sse-chat.js，SLI 数据源）、
> `deploy/observability/prometheus.yml`（指标采集，SLI 供数方）。
> 相关：[07-告警治理](./07-告警治理.md)（burn-rate 告警承接本篇 SLO）、[02-容量规划与性能预算](./02-容量规划与性能预算.md)、[08-测试/05-性能压测实战](../08-测试/05-性能压测实战-k6与JMeter.md)。

---

## 一、先分清三个词：SLI / SLO / SLA

| 术语 | 全称 | 一句话定义 | 谁来定 | 例子（ai-cs 网关接口） |
|---|---|---|---|---|
| **SLI** | Service Level Indicator | 一个**测量值**：从系统视角量化服务质量的指标 | 工程师定义 | "放行请求中 p95 耗时 < 500ms 的比例" |
| **SLO** | Service Level Objective | 一个**目标值**：SLI 在时间窗口内应达到的水平 | 产品+工程共同承诺 | "该 SLI 30 天滚动窗口 ≥ 99.9%" |
| **SLA** | Service Level Agreement | 一份**合同**：违反 SLO 时的外部后果（赔偿/违约） | 法务+商务 | "月可用性 < 99.9% 赔付服务费 10%" |

**关系链**：SLI 是体温计读数，SLO 是"体温应在 36~37.2 度"的健康目标，SLA 是"发烧超 40 度要送医赔偿"的合同条款。

三条铁律：

1. **SLI 必须可测量**——不能测的"目标"只是愿望。这也是为什么本篇先确认项目里已有的测量手段（Prometheus 抓取 + k6 脚本）再定 SLO。
2. **SLO 不是越高越好**——99.99% 意味着每月只能坏 4.3 分钟，成本指数级上升。SLO 是**可靠性成本 vs 功能迭代速度**的显式权衡。
3. **SLA 面向外部，SLO 面向内部**——学习项目没有 SLA，但工程视角的 SLO 练习同样成立。

---

## 二、四黄金信号（Google SRE）

四个信号覆盖了"服务坏没坏"的绝大部分判断，不需要更复杂的指标体系：

| 信号 | 问的问题 | ai-cs 对应测量点 |
|---|---|---|
| **流量 Traffic** | 有多少请求进来？ | Micrometer `http_server_requests_seconds_count`，各服务 `/actuator/prometheus`（抓取配置见 `deploy/observability/prometheus.yml` 的 `ai-cs-services` job，8080~8090 共 11 个 target） |
| **错误 Errors** | 多少请求失败了？ | 同一指标按 `status="500"` 维度；注意**网关 429 是限流预期产物不是故障**（见 §3.2），要分开统计 |
| **时延 Latency** | 请求多快返回？ | `http_server_requests_seconds` 直方图取 p95；SSE 流式链路用"首 token 时延"单独建模（§3.3） |
| **饱和度 Saturation** | 资源还剩多少余量？ | Hikari 连接池活跃数、JVM heap 使用率、RocketMQ 消息积压 |

**常见误区**：平均时延没有意义——长尾才是用户真实体验。永远看 p95 / p99，而不是平均值（1 万个请求 99 个 50ms + 1 个 30s，平均还是"好看"的）。

### 2.1 SLI 的四种类型与选型

| 类型 | 度量什么 | 典型 SLI | ai-cs 候选 |
|---|---|---|---|
| **可用性** | 正确响应的比例 | 非 5xx 请求占比；健康检查 UP 时长 | chat `/actuator/health` UP ≥ 99.9% |
| **时延** | 响应速度的分布 | p95/p99 耗时 | SSE 首 token p95 < 2s |
| **吞吐** | 单位时间处理量 | QPS、消息消费 TPS | MQ 消费速率跟上生产速率 |
| **质量** | 结果好不好（业务侧） | 缓存命中率、检索相关度 | 语义缓存命中率、向量检索命中 |

**选型原则**：每个核心链路选 2~3 个 SLI（一个可用性 + 一个时延起步），宁少而精。SLI 比喻"体温+血压"，不是全身CT——指标越多越没人看。

**度量点选择**：SLI 应尽量在**离用户最近的层**测（网关/前端探针），而不是在每个服务内部自说自话。本项目最近的度量点是网关 8080 与 py-chat 8000；服务内部指标（`/actuator/prometheus`）用于下钻归因，不直接对外承诺。

**常见坑**：① 用"服务端自报成功率"当 SLI——服务挂了连上报都没有，SLI 反而"变好"，所以可用性 SLI 必须有外部探测兜底（如 Prometheus `up` 探针）；② 把 k6 压测的阈值直接当 SLO——压测阈值是"预期系统能做到的"，SLO 是"对用户承诺的"，后者应更保守。

---

## 三、项目现场：给 ai-cs 真实接口定 SLO

> **⚠️ 工程未落地，本节为目标态/预研方案。**
> 现状证据：全仓 grep 无任何 SLO 文档或错误预算记录；已有的可靠性手段是"代码里写死的超时/限流参数"，没有显式目标值。
> 要补什么：① 每个核心接口的 SLI 定义表（本节给出）；② Prometheus 记录规则聚合 SLI（07 篇给接入方案）；③ 错误预算看板。好在**供数基础已具备**：Prometheus 已抓全部 11 个 Java 服务，k6 压测脚本已能产出时延分布。

### 3.1 SLO 定义表（三个真实接口）

| # | 接口 | SLI 定义 | SLO（30 天滚动窗口） | 测量来源 |
|---|---|---|---|---|
| 1 | 网关同步接口（如 `/api/product/page`） | 放行请求（非 429）中 p95 耗时 < 500ms 的比例 | ≥ 99% | `scripts/loadtest/k6/gateway-rate-limit.js` 的 threshold `http_req_duration: p(95)<500` |
| 2 | SSE 流式对话 `/api/chat/stream` | 首 token 时延（TTFB 近似）p95 < 2s | ≥ 95% | `scripts/loadtest/k6/sse-chat.js` 的 threshold `http_req_waiting: p(95)<2000` |
| 3 | chat 服务可用性 | `/actuator/health` 返回 UP 的时间占比 | ≥ 99.9%（月预算 43.2 分钟） | Prometheus `up{job="ai-cs-services"}` |

**为什么 SLO 值不同**：SSE 首 token 依赖 LLM 推理（外部模型 API），本身慢且波动大，95% 是务实起点；网关同步接口只做鉴权转发，要求可以更严。**SLO 定多少取决于链路里最不可控的环节**。

### 3.2 网关限流接口：429 不算错误

网关配置（`ai-cs-gateway/src/main/resources/application.yml:48-49`）：

```yaml
aics:
  gateway:
    rate-limit:
      # 分布式限流（RequestRateLimiter）：每用户每秒补充令牌数与桶容量
      replenish-rate: 5        # 行 48：每用户每秒补充 5 个令牌
      burst-capacity: 10       # 行 49：桶容量 10，允许短时突发
```

这份配置就是 SLO 设计的现成素材：

| 参数 | 语义 | 对 SLI 的含义 |
|---|---|---|
| `replenish-rate: 5` | 稳态每用户最多 5 QPS | 超过 5 QPS 的合法流量必然被 429——这是**产品决策**不是故障 |
| `burst-capacity: 10` | 桶满时可瞬间放行 10 个 | 突发 ≤10 的请求不损失体验，计 SLI 时应计入成功样本 |

因此该接口的 SLI 必须写成"**放行请求**的 p95"，k6 脚本也是这么设计的（`gateway-rate-limit.js:26-28`）：30 req/s 恒定压力远超 replenish-rate=5，**429 被显式当作预期产物**，threshold 只约束放行请求时延。若把 429 计入错误率，SLO 永远不达标，告警永远在响——这就是定义 SLI 时要先分清"产品行为"与"系统故障"的原因。实现上分布式令牌桶由 `RequestRateLimiter`（Redis + Lua）执行，见 `ai-cs-gateway/src/main/java/com/aics/gateway/config/RouteConfig.java:27`；自研算法讲解见 [11-数据结构与算法/10](../11-数据结构与算法/10-限流算法-四大算法与网关实现.md)。

### 3.3 SSE 首 token 时延：长连接要单独建模

SSE 链路的特殊性：一次请求 = 一条长连接，传统"响应耗时"指标会统计整条流的完成时间，掩盖"用户等多久看到第一个字"这个真实体验。

项目已把这条链路的**超时上限**写死（`ai-cs-chat/src/main/resources/application.yml:74-77`）：

```yaml
# SSE 流式 LLM 调用超时（首次 token 到达限制）
sseChatService:
  timeout-duration: 60s       # 首 token 迟迟不到，60s 判死
```

入口在 `ai-cs-chat/src/main/java/com/aics/chat/controller/ChatController.java:167-168`（`POST /chat/stream/sse`，`TEXT_EVENT_STREAM`）。k6 用 `http_req_waiting`（TTFB）近似首 token 时延（`sse-chat.js:5-6` 注释明确说明），阈值 p95 < 2s（`sse-chat.js:27`）。

**SLO 与超时的关系**：TimeLimiter 60s 是"技术上的死线"（超过就熔断降级），SLO p95<2s 是"体验上的承诺"（绝大多数请求 2 秒内出字）。两者差 30 倍是正常的——超时兜底的是尾部异常，SLO 约束的是常态分布。

**首 token 时延怎么精确埋点**（比 k6 TTFB 近似更准的目标态方案）：在 `ChatServiceImpl` 收到 LLM 首个 chunk 的回调处，用 Micrometer 记录 `Timer`（如 `chat.first.token.seconds`），覆盖"用户发消息 → 首个 token 推给 SseEmitter"的完整段——k6 的 `http_req_waiting` 包含了网关转发与建连开销，埋点值会比它更小更纯。两种口径并存时要注明测量点，避免"同样叫首 token，数值差 300ms"的口径打架。

---

## 四、错误预算：SLO 的另一半

### 4.1 定义与速算

**错误预算 = 1 − SLO**：允许"不完美"的额度。

| SLO | 月错误预算（30 天） | 换算 |
|---|---|---|
| 99.9% | 43.2 分钟 | 1 天预算 ≈ 1.44 分钟 |
| 99.95% | 21.6 分钟 | 一两次 P1 故障就烧光 |
| 99% | 7.2 小时 | 相当宽裕 |
| 99.5% | 3.6 小时 | 边缘/内部服务常见过渡目标 |

**为什么它重要**：SLO 只说"要做到多少"，错误预算回答的是"**现在还剩多少，接下来该干什么**"。它是可靠性与迭代速度之间的一把可量化标尺。

### 4.2 错误预算驱动的发布节奏（Google SRE 政策）

| 预算消耗 | 状态 | 政策 |
|---|---|---|
| 0 ~ 50% | 健康 | 正常发布节奏，可跑实验性功能 |
| 50% ~ 100% | 警戒 | 只允许修复类发布 + 提高评审门槛 |
| > 100%（烧穿） | 危险 | **冻结功能发布**，全员投入可靠性改进，直到预算恢复 |
| 持续烧不穿 | 可靠性过剩 | 可以主动放宽 SLO、加快迭代——预算花不完说明承诺过度 |

> **预算怎么"恢复"**：滚动窗口（如 30 天）下，旧的消耗会自然滑出窗口——预算不是充值回满，而是随时间自然刷新。因此"冻结发布"的退出条件是"窗口内消耗回落到阈值下"，通常只需熬几天不再出故障。

以 §3.1 的 chat 服务 99.9% 为例：一次 20 分钟的 Redis 故障（语义缓存降级、首 token 时延上涨但服务未宕机，实际烧掉的是时延 SLO 的预算）+ 一次 15 分钟的发版事故 = 35 分钟，月预算 43.2 分钟只剩 8.2 分钟——按政策此时应冻结新功能发布。**没有错误预算时，这种决策靠吵架；有了错误预算，决策靠查表。**

### 4.3 burn rate：错误预算的燃烧速度

```
burn rate = 实际错误率 / 允许错误率
```

例：SLO 99.9%（允许错误率 0.1%），过去 1 小时实际错误率 1% → burn rate = 10，照这个速度 43.2 分钟的月预算 4.32 小时就烧完。**burn-rate 是 07 篇多窗口告警的核心**（快速烧 → 立刻响；慢速烧 → 工作时间处理），此处先建立概念。

### 4.4 错误预算的记账粒度（目标态）

预算要"记账"才能管理，三个粒度约定：

| 粒度 | 做法 | 例子 |
|---|---|---|
| **按天分摊** | 月预算 43.2 分钟 → 日均 1.44 分钟；当天超耗就是"透支" | 早会看昨日消耗是否超日均 |
| **按 SLO 分账** | 可用性预算与时延预算**分开记**，互不挪用 | 缓存降级烧的是时延预算，不该掩盖"可用性未烧"的事实 |
| **按接口分账** | 核心接口各自记账，防止一个接口的劣化摊平全局 | 对话接口 vs 商品查询接口分开 |

> 记账手段（目标态）：Prometheus recording rule 每 5 分钟聚合一次"已消耗错误率"，Grafana 出"预算余额"看板。核心是一条查询：`1 − avg_over_time(sli[30d])` 与 `(1 − SLO) − 已消耗` 的差值曲线。

---

## 五、从 SLI 到可执行：本项目供数链路（目标态）

现状（已具备）：`deploy/observability/prometheus.yml` 已配置 `ai-cs-services` job 抓取 11 个服务的 `/actuator/prometheus`（端口 8080~8090），`evaluation_interval: 15s`。

要补的（目标态，按顺序）：

```promql
# ① SLI 原始查询示例（chat 服务 5 分钟窗口错误率）——Prometheus 控制台可直接验证
sum(rate(http_server_requests_seconds_count{job="ai-cs-services",
        instance="host.docker.internal:8083", status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count{job="ai-cs-services",
        instance="host.docker.internal:8083"}[5m]))

# ② 可用性 SLI：up 指标本身就是现成的
avg_over_time(up{job="ai-cs-services", instance="host.docker.internal:8083"}[30d])
```

> 注：`status=~"5.."` 统计 5xx，429 不在其中；网关 429 的独立观测可用 `status="429"` 维度单查。指标名为 Micrometer 标准命名，以实际 `/actuator/prometheus` 输出为准。

**进阶：用 recording rule 固化 SLI**（避免每次手写长查询，也为 07 篇 burn-rate 告警复用）：

```promql
# 记录规则示例：chat 服务 5 分钟错误率（固化后告警直接引用 ChatErrorRate5m）
groups:
  - name: sli-recording
    rules:
      - record: ChatErrorRate5m
        expr: |
          sum(rate(http_server_requests_seconds_count{job="ai-cs-services",
                instance="host.docker.internal:8083", status=~"5.."}[5m]))
          /
          sum(rate(http_server_requests_seconds_count{job="ai-cs-services",
                instance="host.docker.internal:8083"}[5m]))
```

---

## 六、面试高频问答

**Q1：SLI、SLO、SLA 三者的区别？**
A：SLI 是测量值（指标怎么算），SLO 是内部目标（该指标要达到多少），SLA 是外部合同（达不到赔什么）。顺序上先有可测的 SLI，再定务实 SLO，最后才有 SLA。关键在 SLI 必须可测量，SLO 是可靠性成本与迭代速度的显式权衡。

**Q2：四黄金信号是什么？**
A：流量（多少请求）、错误（多少失败）、时延（多快，看 p95/p99 而非平均）、饱和度（资源剩多少余量）。Google SRE 提出，四个信号基本覆盖服务健康判断。本项目里分别对应 Micrometer 请求计数、按 status 维度的错误率、http_server_requests 直方图、连接池与 JVM 指标。

**Q3：为什么时延要看 p95/p99 而不是平均值？**
A：平均会被大多数快请求稀释，掩盖长尾。比如 99 个请求 50ms、1 个 30s，平均约 348ms 看起来不错，但用户遇到慢请求的比例是 1%，p99 暴露的才是真实体验。SLO 一般对 p95/p99 承诺。

**Q4：错误预算是什么，怎么用？**
A：错误预算 = 1 − SLO，即可靠性"欠账"额度。用途：预算剩余 >50% 正常发版；50%~100% 只发修复；烧穿则冻结功能发布、全员保可靠。它把"要不要停发版保稳定"的争论变成查表决策。

**Q5：限流被拒的请求（429）应该计入错误率吗？**
A：分情况。对"放行请求的服务质量"这个 SLI 不计入——限流是产品决策，保护系统的手段，本项目网关 replenish-rate=5/burst-capacity=10 下 429 是预期产物，k6 压测脚本也显式把 429 当预期。但对"用户体验"视角可以单独定义一条 SLI 跟踪 429 率，超过阈值说明容量规划不足。

**Q6：SLO 定 99.99% 好不好？**
A：通常不好。99.99% 月停机预算只有 4.32 分钟，一次常规发版都可能烧穿，会长期处于发布冻结。SLO 应从实际能力出发：先测出当前 p95 和错误率，定一个"跳一跳够得着"的值（本项目 SSE 首oken 就定 95% 而非 99.9%），随能力提升逐步收紧。

**Q7：burn rate 是什么，怎么驱动告警？**
A：burn rate = 实际错误率 ÷ 允许错误率，即预算烧穿速度。多窗口组合（如 5m+1h 快速烧配 page 级告警、6h+3d 慢速烧配 ticket 级）既能抓住突发故障，又能发现慢性劣化，且告警条数远少于逐指标绝对阈值——详见本模块 07 篇。

**Q8：SLO 和监控告警是什么关系？**
A：监控是"看见现状"（ SLI 供数），SLO 是"定义好坏线"，告警是"越过好坏线时叫人"。正确顺序是先定 SLO 再设计告警——没有 SLO 的告警只能拍阈值，容易又吵又漏。本项目现状是 Prometheus 采集已就绪、SLO 与告警规则均未落地，07 篇给出从 0 接入方案。

**Q9：SLI 的采集系统自己挂了怎么办？**
A：这是"元监控"问题——监控自己也会死。两层防护：① 外部黑盒探测（独立于被监控系统的拨测，如从外部定时 curl 网关）；② 监控栈自身的存活也进告警（`up{job="prometheus"}`、Alertmanager 死亡哨兵 watch dog）。本项目观测栈（9090/3000/3200）目前自身无监控，接入 Alertmanager 时应把 watch dog 一起加上。

**Q10：多实例部署时 SLI 怎么聚合才不失真？**
A：分两层看：单实例 SLI 保留（用于发现单实例劣化，如某副本连接池泄漏），全局 SLI 用 `sum(rate(...))` 全量聚合（用户真实体验）。陷阱：对时延类指标不能先平均再取分位——要 `histogram_quantile(sum by (le) ...)` 聚合直方图后取分位，顺序反了会系统性低估长尾。

---

## 七、动手练习

1. 用 `k6 run -e BASE=http://localhost:8080 -e PATH=/api/product/page scripts/loadtest/k6/gateway-rate-limit.js` 跑一遍网关压测，把 p95 与 429 比例填进 §3.1 表格第 1 行的"实测值"列（本地环境执行，脚本见 `scripts/loadtest/k6/`）。
2. 给 ai-cs-message 的 RocketMQ 消费链路定义一个 SLI（提示：消费延迟 = 消息产生到消费完成的时间差，可用积压量近似），写出 SLO 值并说明理由。
3. 假设 chat 服务月错误预算 43.2 分钟：一次演练烧掉 10 分钟、一次真实故障烧掉 25 分钟，计算剩余预算并按 §4.2 政策表写出下一步发布策略。
4. 在 Prometheus（localhost:9090）手工执行 §5 的两条查询，观察 11 个 target 中有没有 down 的实例；有则排查对应端口（对照 [08-oncall手册](./08-oncall手册与故障响应.md) 的排查作业单）。
5. 找出 `ai-cs-chat/src/main/resources/application.yml` 里全部三个 TimeLimiter（chatService/sseChatService/visionService），为每个写一行"超时死线 vs SLO 承诺"的对照（参考 §3.3 的写法）。
6. 用 §4.3 的公式为网关 429 率设计一条"配额水位"观察口径：429 占比持续 > 30% 说明 replenish-rate=5 已不满足真实流量，写出你会向上提交的调参建议（数值 + 依据）。

---

> 上一篇：无（本模块首篇） ｜ 下一篇：[02-容量规划与性能预算](./02-容量规划与性能预算.md)
