# 05-性能压测实战：k6 与 JMeter

> 2026-09 落地记录：此前全仓没有任何压测产物——网关限流参数（replenish-rate=5/burst=10）、SSE 弹性参数（TimeLimiter 30s/60s）都是"拍出来"的，没有数据背书。本批补了两个可直接运行的 k6 脚本（`scripts/loadtest/k6/`）与本篇方法论。诚实说：**基线报告待执行**（需要完整服务+中间件跑起来），本文给出脚本、读数方法与报告模板，数据留待实测回填。
> 脚本精读只讲设计意图，运行细节见 [scripts/loadtest/README](../../scripts/loadtest/README.md)。

---

## 一、为什么压测、压什么

没有基线数据时，"系统扛不扛得住"全靠感觉。本项目至少三个参数该有数据背书：

| 待验证参数 | 现值 | 压测场景 |
|---|---|---|
| 网关限流 replenish-rate / burst | 5 / 10 | 30 req/s 恒定压 → 看拒绝比例与放行时延是否稳定 |
| SSE 链路 TimeLimiter | chatService 30s / sseChatService 60s | 20 VU 挂流式会话 → 首 token 时延、整流时长 |
| JVM/容器资源水位 | 默认 | 压测期间观察 GC、内存（联动 [04-Prometheus可观测性](../07-运维部署/04-Prometheus可观测性.md)） |

原则：**压测不是"跑个数字好看"，是给每个容量相关的配置找到它的拐点**。

## 二、工具选型：k6 vs JMeter

| 维度 | k6 | JMeter |
|---|---|---|
| 形态 | JS 脚本，CLI 一把梭 | GUI + XML（.jmx） |
| CI 友好度 | 高（脚本即代码，进 git） | 低（XML diff 灾难） |
| 阈值/门禁 | thresholds 原生支持，超了直接 fail 退出码 | 需插件（Assert + CI 集成） |
| 协议广度 | HTTP/WS/gRPC 为主，SSE 原生支持一般 | 协议大而全（JDBC/FTP/JMS…） |
| 上手曲线 | 会 JS 十分钟上手 | GUI 拖拽，概念多 |

本项目分工：**k6 做高频回归型场景（限流、SSE），JMeter 做偶发的业务流程编排（登录→下单→支付）**——前者进 CI，后者给测试同学用 GUI 搭。

## 三、k6 两个脚本的精读

脚本在 `scripts/loadtest/k6/`。这里只讲两个最关键的设计决策。

### 3.1 gateway-rate-limit.js：用"恒定到达率"而不是 VU 压限流

```js
scenarios: {
  burst: {
    executor: 'constant-arrival-rate',   // 关键：无论响应多慢，始终 30 req/s 到达
    rate: 30, timeUnit: '1s', duration: '30s',
    preAllocatedVUs: 20, maxVUs: 50,
  },
},
```

限流测的是**到达速率 vs 拒绝行为**的关系。如果用 VU 模型（每个 VU 打完一轮 sleep 再来），被限流后响应变慢，实际到达率会自己掉下来——压出来的"429 比例"失真。`constant-arrival-rate` 强制 30 req/s 恒定到达，429 比例 = (30 − 放行能力)/30，正好反推出限流的实际放行水位（理论值：突发 10 + 5/s×时长）。

读数要点：`checks` 里 "429 限流拒绝" 是**预期产物**（所以 thresholds 只约束放行请求的 p95，不把 429 当失败）；同时去 Grafana 看 Redis 侧与网关指标交叉验证。

### 3.2 sse-chat.js：SSE 的"VU ≠ RPS"与首 token 时延

```js
executor: 'ramping-vus',        // 阶梯到 20 VU：每个 VU = 一条挂着的流式会话
thresholds: {
  http_req_waiting: ['p(95)<2000'],   // ≈ 首 token 时延（TTFB 近似）
  http_req_duration: ['p(95)<15000'], // 整条流完成时长
},
```

两个要点：

1. **k6 的 http 会把整条流读完才返回**。所以 `http_req_waiting`（TTFB）≈ 首 token 到达时间，`http_req_duration` = 流结束时间——对"用户等多久看到第一个字、等多久读完"这两个真实体验指标刚好够用。需要逐帧级指标（token 间隔抖动等）要换 xk6-sse 扩展，文档里标注了。
2. **SSE 场景的容量单位是并发连接数（VU），不是每秒请求数**。20 VU = 20 条同时挂着的会话，每条持续数秒——这和普通接口压测的心智完全不同。

## 四、JMeter：登录/下单业务流程（GUI 搭建，不提交 .jmx）

JMeter 的 .jmx 是 XML，手写易错且 diff 无意义，所以仓库不提交，按以下步骤 GUI 搭 5 分钟即可（测试计划保存到本地）：

```text
1. 测试计划 → 添加 线程组：50 线程，Ramp-Up 10s，Loop 10
2. HTTP 请求默认值：Server = localhost:8080（网关）；Content-Type: application/json
3. 请求 1 登录：POST /api/user/login  {"username":"test","password":"..."}
   → JSON 提取器：取 $.data.token 存变量 ${token}
4. HTTP 信息头管理器：Authorization = Bearer ${token}
5. 请求 2 下单：POST /api/order/create  {"productId":1,"quantity":1}
   → JSON 断言：$.code == 200
6. 聚合报告 + 查看结果树（调试期开，正式压测关——结果树吃性能）
7. CSV Data Set Config：多用户数据文件（避免所有请求同一账号，撞业务唯一键）
```

跑完聚合报告看四列：**Samples（样本数）、Average/p95（时延）、Throughput（吞吐）、Error%（错误率）**——任何一个"全绿但 Error% 5%"的报告都是废报告，先看错误率。

## 五、基线报告模板（待回填）

> 状态：**待执行**。前置：docker-compose 基础设施 + 被测服务全部健康、压测机与被测机分离（至少分容器跑）。

| 场景 | 目标 | 实测结果 | 结论/调参建议 |
|---|---|---|---|
| 网关限流（30 req/s） | 429 比例稳定、放行 p95 < 500ms | _待填_ | replenish-rate/burst 是否调整 |
| SSE 20 VU 流式 | 首 token p95 < 2s、失败率 < 5% | _待填_ | TimeLimiter 30s/60s 是否合理 |
| 登录+下单 50 线程 | 错误率 0、p95 < 800ms | _待填_ | MySQL/Redis 水位、Seata 锁等待 |

每行结论都要能落到一个**动作**（调参/扩容/不改），否则这次压测白跑。

## 六、压测期间看什么（与可观测性联动）

压测的价值一半在压，一半在**同时观测**（观测栈拉起：`docker compose -f deploy/docker-compose/docker-compose-observability.yml up -d`）：

| 指标族 | 来源 | 压测中关注 |
|---|---|---|
| http_server_requests_seconds | 各服务 /actuator/prometheus | p95/p99 与 k6 读数互相印证 |
| resilience4j 断路器状态 | chat 服务 | 什么时候 OPEN——和阈值设定的关系 |
| 网关限流拒绝 | gateway 指标 | 与 k6 的 429 计数对账 |
| JVM GC / 内存 | micrometer JVM 指标 | Full GC 是否被压出来 |
| Trace（Tempo） | 压测样本的 trace | 慢在网关/业务/DB 哪一段 |

这套联动正是 [04-Prometheus可观测性](../07-运维部署/04-Prometheus可观测性.md) 建好的管道的用武之地——压测是让可观测性"活起来"的最快方式。

## 七、踩坑与要点

1. **本机既当压测机又当被测机**：资源互相挤压，绝对数字不可信，只可做相对比较；正式基线要分离环境。
2. **限流键基数**：限流 key 若含 userId/IP，压测脚本要造多账号，否则 Redis 里只有一个键，测出的行为不代表多用户真实分布。
3. **SSE 的连接堆积**：流式会话压完确认连接真的关了（TIME_WAIT/服务端连接数），长连接泄漏会把下一轮压测污染成"慢在连接建立"。
4. **压测数据会写脏业务库**：下单压测后清 demo 数据（`deploy/mysql/drop-all.sql` 有既有工具），别把压测订单带进演示环境。
5. **thresholds 即门禁**：k6 的 thresholds 失败时进程退出码非 0——将来接 CI 时直接用这个特性做"性能回归门禁"。

## 八、面试要点总结

> 本项目压测体系分工明确：k6 承担回归型场景并进 CI——限流场景用 constant-arrival-rate 保证到达率不受响应变慢影响、以 429 比例反推放行水位，SSE 场景以 VU 模拟并发流式会话、用 http_req_waiting 近似首 token 时延；JMeter 用 GUI 搭建登录→下单业务流程并配合 CSV 多账号。压测必须与可观测性联动（Prometheus 指标、熔断状态、Tempo 链路）交叉验证，产出物是"场景/目标/实测/动作"四列的基线报告，每个结论必须落到调参或扩容动作。

```text
关键词：constant-arrival-rate = 限流正确姿势 · SSE 容量单位是并发连接不是 RPS
http_req_waiting ≈ 首 token（k6 无逐帧 SSE）· thresholds = CI 性能门禁
报告四看 = 样本/p95/吞吐/错误率 · 每个结论落到一个动作
```

## 学习检查清单

- [ ] 本地跑通两个 k6 脚本并解释各自的 executor 选择
- [ ] 用 JMeter GUI 完成登录→下单流程并读出聚合报告四指标
- [ ] 回填第五节基线报告至少一行
- [ ] 能说出把 k6 接进 CI 的门禁方案（thresholds 退出码）
