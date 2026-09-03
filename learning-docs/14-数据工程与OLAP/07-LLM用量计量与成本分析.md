# LLM 用量计量与成本分析

> **分工声明**：计量链路的**实现规格**——架构总览、五张表定义、配置项（`aics.usage.*`）、接口清单、OTLP 接入与 CI 门禁——已在 [05-AI集成/01-SpringAI框架集成/05-LLM可观测性评估与成本治理实现文档](../05-AI集成/01-SpringAI框架集成/05-LLM可观测性评估与成本治理实现文档.md) 完整覆盖（对应 OpenSpec 变更 `2026-08-14-llm-observability-cost-governance`）。本篇不复述实现，聚焦**计量数据的分析与运营**：token 怎么读、费用怎么按 scenario/模型归因、单价表怎么维护、成本看板怎么建、缓存到底省了多少钱。
> 对应项目：`ai-cs-chat/src/main/java/com/aics/chat/observability/ModelUsageRecorder.java`（计量落点，150 行）、`ai-cs-chat/src/main/java/com/aics/chat/observability/ModelUsageProperties.java`（单价配置）、`ai-cs-chat/src/main/java/com/aics/chat/observability/QuotaService.java`（配额判定）、`deploy/mysql/llm-observability-init.sql:29-49`（`model_usage` 计量表）、`ai-cs-message/src/main/java/com/aics/message/controller/ModelUsageController.java`（统计接口）。
> 相关：[02-ClickHouse实战](./02-ClickHouse实战.md)（成本报表的 CH 目标态）、[03-数仓分层与指标口径](./03-数仓分层与指标口径.md)（费用口径设计原则）、[11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存](../11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存.md)（语义缓存——缓存节省测算的对象）、[05-AI集成/01-SpringAI框架集成/10-提示词版本管理与灰度发布实战](../05-AI集成/01-SpringAI框架集成/10-提示词版本管理与灰度发布实战.md)（按 prompt 版本归因的延伸）。

---

## 一、先结论：现有计量链路总览

```
ai-cs-chat（业务侧，只管"记"）
  ResilientAiService / RagEval / Agent ... 每次调用 LLM 后
        │ record(scenario, provider, model, in, out, status, err, pricingKey)
        ▼
  ModelUsageRecorder.java:74-115
    ├─ aics.usage.enabled 总开关，关闭时零开销返回（:78-80）
    ├─ TraceContext 关联 requestId/userId（:83-87）→ 用量可回溯到请求
    ├─ estimateCost(pricingKey, in, out)（:127-137）→ BigDecimal 精确计费
    └─ usageExecutor 异步 Feign 上报，失败仅告警（:106-114）→ 计量不阻断业务
        │ Feign
        ▼
ai-cs-message（存储侧，只管"算"）
  ModelUsageController.java:47 POST /api/model-usage/records（落库）
  ModelUsageController.java:65 GET  /api/model-usage/stats（聚合统计）
        │
        ▼
  MySQL chat_db.model_usage（llm-observability-init.sql:29-49，明细事实表）
  MySQL chat_db.model_usage_quota（:52-64，配额配置，uk_user_scenario 唯一）
```

**设计三原则**（读代码前先记住，后面处处印证）：

| 原则 | 落点 |
|---|---|
| 计量不阻断业务 | 异步线程池 + 失败仅告警（`ModelUsageRecorder.java:104-114` 注释原文："计量不影响业务"） |
| 用量可回溯请求 | requestId 由 TraceContext 注入（`:81-87` 注释原文："按 requestId 既能看调用链也能看花了多少钱"） |
| 估算与精确分离 | `estimated` 标记（`:98-100` 注释原文："避免'估算当精确'误导成本决策"） |

---

## 二、token 计量：输入/输出/缓存命中

### 2.1 三个 token 数从哪来

| 字段 | 含义 | 注意 |
|---|---|---|
| `input_tokens` | 提示词（system+history+RAG 上下文+用户问题）消耗 | RAG 场景下**检索上下文是大头**——这解释了为什么 RAG 的单次成本远高于纯 chat |
| `output_tokens` | 生成回答消耗 | 流式输出中**常常拿不到**（见 2.2） |
| `total_tokens` | 两者之和 | 落库时服务层兜底计算（`ModelUsageController.java:49` 注释："totalTokens 未传时按 input+output 兜底"） |

**单价不对称**：业界惯例输出单价是输入的 2~8 倍（生成计算远贵于预填），所以**成本优化优先砍输出、其次砍输入**——压缩 RAG 上下文、控制回答长度、缩短历史窗口。

### 2.2 流式与估算行（estimated 标记）

```java
// ModelUsageRecorder.java:98-100
// estimated 标记：流式调用常取不到精确 usage，此时按估算记且打标，
// 统计时可按标记过滤，避免"估算当精确"误导成本决策
dto.setEstimated(inputTokens == null || outputTokens == null);
```

**运营纪律**：成本报表必须区分精确/估算两行——估算占比突增说明流式 usage 回传链路劣化，先修链路再谈账单。

### 2.3 缓存命中如何影响 token 计量

三类"缓存"对 token 数的影响完全不同（缓存机制本身见 [11-数据结构与算法/09](../11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存.md)）：

| 缓存类型 | 命中时 token 计量 | 成本影响 |
|---|---|---|
| **供应商侧前缀缓存**（prompt cache） | 供应商账单把命中前缀按折扣价计（用量接口可能给 `cached_tokens` 字段） | 直接降低 input 单价，需在单价表里为"命中价"单列 |
| **本项目语义缓存**（`SemanticCacheService`：相似问题直接返回缓存答案） | **一次 LLM 调用都不发生**，`model_usage` 里无记录 | 成本节省 = 未发生调用的"虚拟费用"（§5.3 测算） |
| **本项目向量缓存**（`VectorCacheStore`：缓存 embedding） | 省的是 **embedding 调用**费用，不在 `model_usage`（它记的是 LLM 而非 embedding） | 若要完整成本视图，embedding 调用也应计量（见 §6 练习） |

**关键认知**：语义缓存命中在 `model_usage` 里是"缺失数据"，直接 SUM 会高估"若无缓存时的花费"的参照系——测算节省必须走"虚拟调用"口径（§5.3）。

---

## 三、按 scenario 与模型归因

### 3.1 两个归因维度

`model_usage` 表（`llm-observability-init.sql:29-49`）天然带齐归因维度：

| 维度 | 字段 | 取值（表注释原文，:34） |
|---|---|---|
| **场景** | `scenario` | chat / rag / agent / summary / vision / nl2sql / eval |
| **模型** | `provider` + `model` | provider=供应商，model=模型名 |

外加三个追溯维度：`request_id`（回溯调用链，配合 `llm_trace` 表）、`user_id`（用户级配额与账单）、`create_time`（时间窗）。索引设计与归因查询一一对应（:45-48）：`idx_scenario_time`、`idx_model_time`、`idx_user_time`。

### 3.2 归因分析 SQL（可复制，MySQL 现状版）

```sql
-- ① 场景×模型 成本矩阵（近 30 天）：运营第一张表
SELECT scenario, model,
       COUNT(*)                       AS calls,
       SUM(total_tokens)              AS tokens,
       SUM(estimated_cost)            AS cost,
       ROUND(SUM(estimated_cost)/COUNT(*), 6) AS avg_cost_per_call,
       SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END) AS failed_calls
FROM chat_db.model_usage
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY scenario, model
ORDER BY cost DESC;

-- ② 单请求成本 TopN（抓"烧钱请求"，配 llm_trace 定位到具体链路）
SELECT request_id, scenario, model, input_tokens, output_tokens, estimated_cost
FROM chat_db.model_usage
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY estimated_cost DESC LIMIT 20;

-- ③ 估算行占比（数据质量哨兵，见 §2.2）
SELECT scenario,
       ROUND(AVG(estimated), 4) AS estimated_ratio
FROM chat_db.model_usage
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY scenario;
```

**归因后的三个运营动作**：① 某场景成本异常 → 查该场景 prompt 是否膨胀（历史窗口/上下文截断策略）；② 某模型成本占比高 → 评估路由策略（见 §5.4 配额降级与模型路由）；③ 失败调用成本高 → 失败也花钱（FAILED 的 input token 已消耗），优先修错误率。

---

## 四、单价表维护

### 4.1 配置结构（现状，已落地）

```yaml
# 前缀 aics.usage（实现文档 §3.2），单价单位：元/百万 Token（业界惯例）
aics:
  usage:
    enabled: true
    pricing:
      deepseek-chat:
        input: 1.0        # 元/百万 Token（示例值，按实际合同价配置）
        output: 2.0
    default-pricing:      # 未配置单价的模型兜底
      input: 0.5
      output: 0.5
    executor-size: 2      # 异步上报线程池
```

对应绑定类 `ModelUsageProperties.java:32-51`：`pricing` 为 `Map<String, ModelPrice>`、`defaultPricing` 兜底、`ModelPrice.input/output` 均为 `BigDecimal`（**金额不用 double**）。

### 4.2 pricingKey 与展示模型名分离（本项目最值得学的设计）

```java
// ModelUsageRecorder.java:96-97（注释原文）
// 学习点：pricingKey 与展示 model 分离——单价配置按内部模型 id（如 siliconflow-qwen3-32b），
// 供应商展示名（如 Qwen/Qwen3-32B）变化不会导致费用查不到单价
BigDecimal estimateCost(String pricingKey, int inputTokens, int outputTokens)
```

为什么关键：模型展示名来自供应商（`Qwen/Qwen3-32B`），供应商改名/换版本是常态；内部模型 ID 才是路由与配置的稳定锚点。**计费键与展示键混用 = 供应商一改名，账单算不出**。

### 4.3 精确计算与维护流程

```java
// ModelUsageRecorder.java:127-137（节选）
BigDecimal inCost = BigDecimal.valueOf(inputTokens)
        .divide(PER_MILLION, 10, RoundingMode.HALF_UP)   // 10 位小数防除不尽抛异常
        .multiply(price.getInput());
return inCost.add(outCost).setScale(6, RoundingMode.HALF_UP);  // 落库毫分精度
```

**单价表维护三流程**：① 新模型上架 → pricing 加条目（忘了配也能跑，走 default-pricing，但**要在报表里监控"兜底单价占比"**，兜底占比高=账单失真）；② 供应商调价 → 更新条目并**记生效日期**（历史账单按当时单价，重算需按 `create_time` 分段）；③ 下架模型 → 保留条目不删除（历史明细还在引用该键）。

---

## 五、成本看板与缓存节省测算

### 5.1 看板指标树

```
LLM 成本总览（日/周/月）
├── 总花费 / 总 token / 总调用次数
├── 归因切片：scenario × model × user（§3.2 SQL ①）
├── 效率指标：单请求平均成本、单会话平均成本、输出/输入 token 比
├── 质量哨兵：估算行占比、FAILED 调用成本占比、兜底单价占比
└── 节省项：语义缓存节省（§5.3）、配额降级节省（§5.4）
```

### 5.2 看板的数据源：现状与目标态

| | 现状 | 目标态 |
|---|---|---|
| 数据源 | MySQL `model_usage` 直查 | CH `model_usage_day` 预聚合（02 篇 §4 物化视图） |
| 查询出口 | `ModelUsageController.java:65` `/api/model-usage/stats`（参数 userId/scenario/model/时间范围） | CH 查询接口 + Grafana 直接连 CH |
| 响应量级 | 业务库聚合，量小可用 | **按天/按 scenario 报表秒级出数**（缺口验收标准） |
| 标注 | — | ⚠️ CH 侧**工程未落地**，现状为 MySQL 内存聚合 |

### 5.3 缓存节省测算（口径敏感，先定口径）

```
虚拟调用成本法：
  节省额 = Σ(命中次数_i × 该问题的"虚拟单次成本")

虚拟单次成本怎么估？
  ① 用同类问题的真实计量记录做代理：同 scenario+model 的
     AVG(input_tokens/output_tokens) × 单价（ BigDecimal，同 §4.3 公式）
  ② 或抽样重放 N 个命中问题真实调用一次，取平均成本 × 全量命中数

口径陷阱（§2.3 的呼应）：
  ✗ 不能用 model_usage 总花费 ÷ 总调用 的平均成本代理——
    命中的多为高频简单问题，成本低于平均值，会高估节省
  ✓ 代理样本要取"与命中问题相似"的调用，且节省额单独成列，不冲抵总成本
```

**为什么单列而不冲抵**：财务视角"实际支出"与"避免支出"是两类数；混在一起会让"总成本环比下降"无法归因（是缓存生效还是调用量真降了）。

### 5.4 配额联动（已落地的成本治理闭环）

`model_usage_quota` 表（`llm-observability-init.sql:52-64`：`uk_user_scenario` 唯一、DAILY/WEEKLY/MONTHLY 窗口、token 与费用双配额）+ `QuotaService.check()`。实现文档 §二 的要点：**配额判定结果会驱动 `ModelRouter` 走 `QUOTA_DOWNGRADE` 自动切到 cheap 档**——成本治理不是只"限流"，而是"超了就换便宜模型"，这一步把计量数据变成了实时决策输入。

---

## 六、面试高频问答

**Q1：一次 LLM 调用的成本怎么算？为什么输出比输入贵？**
A：成本 = input_tokens/1e6 × 输入单价 + output_tokens/1e6 × 输出单价。生成阶段是逐 token 自回归解码（每步全量 KV 参与计算），预填阶段可高度并行，所以输出单价通常是输入的 2~8 倍。优化顺序：先砍输出（限长、压缩回答），再砍输入（压缩 RAG 上下文与历史窗口）。

**Q2：流式调用拿不到精确 usage 怎么办？**
A：按可得的 token 数（或估算）记录并打 `estimated` 标记，落库字段独立，统计时可过滤。本项目 `ModelUsageRecorder` 的设计原则是"估算不当精确"——成本报表区分精确/估算，估算占比突增即链路劣化信号，先修链路再谈账单。

**Q3：模型计费键为什么要和展示名分离？**
A：展示名来自供应商（如 Qwen/Qwen3-32B），会随供应商改名/换版本漂移；计费键用内部稳定模型 ID（如 siliconflow-qwen3-32b）。混用会导致供应商一改名就查不到单价、账单中断。本项目 `ModelUsageRecorder` 的 8 参重载就是为此设计（pricingKey 与展示 model 解耦）。

**Q4：怎么把一笔费用归因到"哪个场景、哪个模型、哪次请求"？**
A：计量行落库时带 scenario/provider/model 与 TraceContext 注入的 requestId/userId；requestId 连通 llm_trace 调用链——按 requestId 既能看链路也能看花费。三个二级索引（scenario/model/user × time）正好对应三种归因查询路径。

**Q5：语义缓存省了多少钱，怎么测算？**
A：虚拟调用成本法：节省额 = Σ 命中次数 × 该类问题的虚拟单次成本；虚拟成本用"同类问题真实计量的平均 token × 单价"代理或抽样重放校准。两个口径陷阱：不能用全局平均成本代理（命中问题偏简单，会高估）；节省额单列成"避免支出"，不冲抵实际支出，否则成本波动无法归因。

**Q6：成本看板要放哪些指标？数据量大了查不动怎么办？**
A：指标树：总花费/调用数、scenario×model×user 归因切片、单请求平均成本、估算行占比与失败成本占比两个质量哨兵、缓存节省与配额降级两项节省。数据量大时把明细从 MySQL 迁 CH，物化视图按天×scenario×model 预聚合，看板只读聚合表——这是本项目技术缺口清单里"ClickHouse 用量分析"的目标态（未落地）。

**Q7：配额怎么参与成本治理？**
A：按 userId+scenario 唯一键配 DAILY/WEEKLY/MONTHLY 窗口的 token 与费用双配额（model_usage_quota 表）；QuotaService.check 判定超限后不是简单拒绝，而是驱动模型路由自动降级到 cheap 档——计量数据反哺实时路由决策，把"事后看账单"变成"事中控成本"。

**Q8：为什么计量落库要用异步且失败只告警？**
A：计量是非功能旁路，Feign 落库是网络 IO，同步执行会把每次 LLM 调用拖慢一个 RTT；独立线程池隔离（usageExecutor）+ 失败仅告警，保证"计量不影响业务"。代价是计量行可能少量丢失——旁路数据允许最终一致性缺失，用对账/抽样校准即可，不能反噬主链路。

---

## 七、动手练习

1. 走读 `ModelUsageRecorder.java`（150 行），标注 §一三原则与 §2.2/§4.2 两个设计（estimated 标记、pricingKey 分离）的行号；再回答：若 record 改成同步落库，SSE 首字延迟会受什么影响？
2. 执行 §3.2 的三条 SQL（先跑 `deploy/mysql/llm-observability-init.sql` 与对话流量造数），解释"场景×模型成本矩阵"里 cost 最高的组合该怎么进一步归因。
3. 给"兜底单价占比"写一条监控 SQL：统计 pricing 未命中（无法从数据判断）的场景可用"多配置两套单价跑对比"或代码插桩——先在 `ModelUsageProperties` 里加 `defaultPricing` 命中计数器并暴露到统计接口。
4. 实现缓存节省测算脚本：从语义缓存的命中统计（`SemanticCacheService` 的 ZSET 指标，见 [11-数据结构与算法/09](../11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存.md) §6.2）读命中次数，乘以同 scenario 平均单次成本，输出日报。
5. ⚠️ 目标态推演：把 §5.1 指标树映射到 02 篇 §4 的 `model_usage_day` 预聚合表，标出每个指标对应 `countMerge/sumMerge` 的查询写法，并画出"Grafana 面板 → CH → MySQL 事实源"的三层关系图。

---

> 上一篇：[06-BI看板与ECharts可视化](./06-BI看板与ECharts可视化.md) ｜ 下一篇：[08-数据质量与对账](./08-数据质量与对账.md)
