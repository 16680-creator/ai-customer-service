# ClickHouse 实战

> 对应项目：本篇的**目标态设计**针对 `deploy/mysql/llm-observability-init.sql` 的 `model_usage` 计量表（将迁 CH 的数据源，表结构已确认），
> 工程现状：**ClickHouse 未落地**（⚠️ 见 [00-学习路线总览/05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md) B 类缺口"ClickHouse 用量分析"——中间件计划 P4 整体未做，验收标准"按天/按 scenario 的 token 与费用报表秒级出数"，本篇即该缺口的目标态/预研方案）。
> 相关：[01-OLAP与列式存储原理](./01-OLAP与列式存储原理.md)（为什么是列存）、[07-LLM用量计量与成本分析](./07-LLM用量计量与成本分析.md)（现有 MySQL 计量链路）、[05-批处理与调度编排](./05-批处理与调度编排.md)（同步与回填任务）。

---

## 一、先结论：引擎选型速查

| 引擎 | 合并时行为 | 用它当表"像什么" | 本项目对应场景 |
|---|---|---|---|
| **MergeTree** | 只归并，不去重 | 只增不改的明细流水 | `model_usage` 明细表（每次调用一条，永不改） |
| **ReplacingMergeTree** | 按 ORDER BY 键去重，保留版本最新的 | "会重放的更新事件" | CDC 逐条同步的业务表（重复投递天然去重） |
| **AggregatingMergeTree** | 把聚合中间状态合并起来 | 预聚合结果表 | 物化视图落表，按天/scenario 预算 token 与费用 |
| SummingMergeTree | 按 ORDER BY 键合并数值列求和 | 纯加总指标 | 简单累加场景（不保留明细时才用） |

> 选型口诀：**明细流水用 MergeTree，同步重放用 Replacing，报表加速用 Aggregating**。下文 §2 逐个展开。

---

## 二、MergeTree 家族：三类建表精读

### 2.1 MergeTree：明细流水（承接 model_usage 的正确引擎）

```sql
-- ⚠️ 目标态（工程未落地）：LLM 计量明细表，字段与 MySQL 版一一对应
CREATE TABLE chat_olap.model_usage
(
    `request_id`     String,
    `user_id`        Int64,
    `scenario`       LowCardinality(String),   -- 低基数：chat/rag/agent/...，字典编码（见 01 篇 §3.2）
    `provider`       LowCardinality(String),
    `model`          LowCardinality(String),
    `input_tokens`   UInt64,
    `output_tokens`  UInt64,
    `total_tokens`   UInt64,
    `estimated_cost` Decimal(12, 6),           -- 与 MySQL 版同精度，金额别用 Float
    `estimated`      UInt8,                    -- 流式估算标记
    `status`         LowCardinality(String),
    `create_time`    DateTime
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(create_time)                                  -- 按月分区
ORDER BY (create_time, scenario, model)                             -- 排序键
SETTINGS index_granularity = 8192;
```

**为什么是明细表而不是直接建聚合表**：明细是一切分析的本源——今天要"按天×scenario"，明天要"按用户×模型"，预聚合永远追不上问题。先保明细，再靠物化视图加速（§4）。

### 2.2 ReplacingMergeTree：CDC 重放场景

```sql
-- 业务表经 CDC 同步进 CH 的通用模式（本项目 Canal 链路若未来多一个 CH 目标端）
CREATE TABLE chat_olap.product
(
    `id`         Int64,
    `name`       String,
    `price`      Decimal(10, 2),
    `updated_at` DateTime,
    `version`    UInt64          -- 用 binlog 位点/时间戳当版本
)
ENGINE = ReplacingMergeTree(version)
ORDER BY id;                    -- ⭐ 排序键 = 业务主键 = 去重键
```

三个必须知道的语义：

| 语义 | 说明 |
|---|---|
| **去重发生在合并时**，不是写入时 | 查询时同一 `id` 可能仍有多版本并存 |
| 取 `version` 最大的保留 | 不传版本参数则保留最后读到的（不可靠，务必传） |
| **查询必须配合去重** | `FINAL`（慢，可用）或手动子查询取 `argMax()` |

```sql
-- 两种正确查法：
SELECT * FROM product FINAL WHERE id = 1001;           -- 简单但强制合并读，大表慢
SELECT id, argMax(name, version), argMax(price, version)   -- 推荐
FROM product WHERE id = 1001 GROUP BY id;
```

### 2.3 AggregatingMergeTree：预聚合状态表

普通 SummingMergeTree 只能"加总"，遇到 `count(distinct user_id)`、`avg` 这类不可直接相加的指标就错。AggregatingMergeTree 存的是**聚合函数的中间状态**，合并时按同一函数的状态合并，永远不会加错：

```sql
-- 预聚合表：按天 × scenario × model
CREATE TABLE chat_olap.model_usage_day
(
    `day`               Date,
    `scenario`          LowCardinality(String),
    `model`             LowCardinality(String),
    `call_count`        AggregateFunction(count, UInt64),
    `tokens_sum`        AggregateFunction(sum, UInt64),
    `cost_sum`          AggregateFunction(sum, Decimal(12, 6))
)
ENGINE = AggregatingMergeTree
PARTITION BY toYYYYMM(day)
ORDER BY (day, scenario, model);
```

> 一般不手写这张表的 INSERT——由物化视图自动写入（§4）。

---

## 三、分区与排序键

### 3.1 分区（PARTITION BY）：数据管理的单位

```
model_usage/
├── 202607_1_5_2/    ← 分区目录：{min_block}_{max_block}_{合并层级}
├── 202608_6_9_1/
└── 202609_10_11_0/
```

| 用分区做什么 | 怎么做 |
|---|---|
| 查询剪枝 | `WHERE create_time >= '2026-09-01'` 直接跳过其他月分区 |
| TTL 过期 | `TTL create_time + INTERVAL 1 YEAR` 到期整分区删除（秒级，不逐行） |
| 数据管理 | `ALTER TABLE ... DROP PARTITION '202607'` 重跑某月数据 |

**分区键怎么选**：几乎总是 `toYYYYMM(时间列)` 或 `toYYYYMMDD`（数据量更大时）。**警惕**：分区数过多（按 user 分区这类）会形成海量小目录，反而拖垮合并与查询。

### 3.2 排序键（ORDER BY）：查询性能的第一决定因素

排序键决定列存文件里数据的物理顺序，三个作用（都源自 01 篇原理）：① 稀疏索引按它剪枝 granule；② 同值聚集放大 RLE/字典压缩；③ GROUP BY 前缀列接近免排序。

```
模型：WHERE 条件最常用的列放前面，基数从低到高、时间列通常在最前或紧随其后

  ORDER BY (create_time, scenario, model)
            └──时间过滤    └──常用分组维度

反例：ORDER BY (request_id)  ← 高基数、几乎不做过滤条件，
                              稀疏索引剪枝失效，压缩也变差
```

**主键/分区/排序键三件套决策表**：

| 设计点 | 选择 | 理由 |
|---|---|---|
| 分区 | `toYYYYMM(create_time)` | 时间剪枝 + TTL 整月过期 |
| 排序键 | `(create_time, scenario, model)` | 时间范围查询最常见；低基数列促进压缩 |
| 索引粒度 | 默认 8192 | 一般不动 |

---

## 四、物化视图做预聚合

### 4.1 原理：写入时"顺手"算聚合

ClickHouse 物化视图（MATERIALIZED VIEW）是**写入触发器**：每次 INSERT 到明细表，新数据被同步算一遍聚合写入目标表。查询报表表时就是读普通小表——**把计算成本从查询时转移到写入时**。

```
INSERT model_usage（明细，不停写）
     │
     ▼（每次 INSERT 触发）
物化视图：按 (day, scenario, model) 聚合新块
     │
     ▼
model_usage_day（预聚合，行数 ≈ 天数×场景×模型，极小）
     │
     ▼
报表查询直接打这张小表 → 秒级甚至毫级出数
```

### 4.2 可复制 SQL（目标态）

```sql
-- 明细 → 预聚合 的物化视图（TO 模式：结果落到独立表，便于重建）
CREATE MATERIALIZED VIEW chat_olap.mv_model_usage_day
TO chat_olap.model_usage_day
AS
SELECT
    toDate(create_time)                                        AS day,
    scenario,
    model,
    countState(toUInt64(1))                                    AS call_count,
    sumState(total_tokens)                                     AS tokens_sum,
    sumState(estimated_cost)                                   AS cost_sum
FROM chat_olap.model_usage
GROUP BY day, scenario, model;
```

```sql
-- 查询端用 -Merge 后缀把"状态"变"值"：
SELECT
    day, scenario, model,
    countMerge(call_count)   AS calls,
    sumMerge(tokens_sum)     AS tokens,
    sumMerge(cost_sum)       AS cost
FROM chat_olap.model_usage_day
WHERE day BETWEEN '2026-09-01' AND '2026-09-07'
GROUP BY day, scenario, model
ORDER BY day, cost DESC;
```

**State/Merge 成对记法**：写入端 `xxxState` 存中间状态，查询端 `xxxMerge` 合并出值。用错（如直接 sum）就会把不可加指标算错。

---

## 五、JSONEachRow 导入

Canal Adapter/MQ 消费端产出的就是 JSON 行，`JSONEachRow` 是与它最对口的导入格式。

### 5.1 HTTP 接口导入（最常用）

```bash
# 单批导入（生产建议每批 1万~100万 行，见 §7 反模式）
cat usage.jsonl
# {"request_id":"r1","user_id":1,"scenario":"chat","provider":"siliconflow","model":"Qwen/Qwen3-32B","input_tokens":812,"output_tokens":356,"total_tokens":1168,"estimated_cost":0.031200,"estimated":0,"status":"SUCCESS","create_time":"2026-09-01 10:00:00"}
# {"request_id":"r2", ...}

cat usage.jsonl | curl -sS "http://localhost:8123/?query=INSERT%20INTO%20chat_olap.model_usage%20FORMAT%20JSONEachRow" \
  --data-binary @-
```

```bash
# 幂等重放的守护参数（重跑导入不产生重复）：
#   input_format_skip_unknown_fields=1   忽略源端新增字段（schema 演进友好）
#   date_time_input_format=best_effort   兼容多种时间格式
#   后续去重交给 ReplacingMergeTree 或导入前按 request_id 排重
```

### 5.2 建表直连 MySQL 库（预研/小规模可用）

```sql
-- MySQL 数据库引擎：不搬数据先能查（适合试水验证口径，不适合长期跑大聚合）
CREATE DATABASE mysql_src
ENGINE = MySQL('mysql-master:3306', 'chat_db', 'reader', 'password');

SELECT scenario, sum(estimated_cost)
FROM mysql_src.model_usage
WHERE create_time >= '2026-09-01'
GROUP BY scenario;
```

> 正式的日增量同步（水位线 + 对账）设计见 [05-批处理与调度编排](./05-批处理与调度编排.md)；本项目若落地，计量链路是"MySQL 为事实源 → 定时/流式导入 CH → CH 只做分析"，与商品数据"MySQL 事实源 → Canal → ES 读模型"同构。

---

## 六、目标态设计：把项目 LLM 计量数据从 MySQL 迁到 CH

> ⚠️ **工程未落地，本节为目标态/预研方案。**
> **现状证据**：全仓 grep 无任何 ClickHouse 依赖/配置；`deploy/mysql/llm-observability-init.sql:29-49` 的 `model_usage` 表为 MySQL InnoDB 明细表（含 `idx_user_time`/`idx_scenario_time`/`idx_model_time` 三个二级索引）；统计走 `ai-cs-message/.../controller/ModelUsageController.java:65` 的 `/api/model-usage/stats`（内存聚合）。数据量小的时候这套完全够用，缺口在于**数据量上来后大聚合拖业务库**。
> **要补什么**：CH 服务 + 建表 + 物化视图 + Grafana 看板 + 计量同步任务（对应缺口文档的验收标准）。

### 6.1 迁移后全景

```
ai-cs-chat（业务写路径不变）
  ModelUsageRecorder ──Feign──▶ ai-cs-message ──▶ MySQL chat_db.model_usage（事实源，保留）
                                        │
                                        ▼  同步任务（水位线增量，见 05 篇）
                                  ClickHouse chat_olap.model_usage（明细）
                                        │ 物化视图（写入触发）
                                        ▼
                                  model_usage_day（按天×scenario×model 预聚合）
                                        │
                                        ▼
                                  Grafana 看板 / 成本报表（秒级出数）

原则：MySQL 是唯一事实源，CH 是可随时重建的分析副本（对账见 08 篇）
```

### 6.2 迁移要点清单

| 要点 | 决策 |
|---|---|
| 事实源 | 仍在 MySQL，计量写入链路**零改动**（`ModelUsageRecorder` 的 Feign 上报照旧） |
| 明细表引擎 | MergeTree，字段对齐 MySQL 版（§2.1），`scenario`/`model` 用 LowCardinality |
| 去重策略 | 同步重放可能引入重复：按 `request_id` 导入前排重，或引擎换 ReplacingMergeTree(create_time) |
| 预聚合 | 物化视图 + AggregatingMergeTree（§4） |
| 一致性 | T+分钟级即可（成本报表容忍滞后）；对账 MySQL 总数 vs CH 总数（08 篇） |
| 回填 | 初次全量：按 `create_time` 分月导出 JSONEachRow 分批导入，重跑靠幂等（05 篇） |
| 查询出口 | 新增只读 stats 接口路由到 CH；MySQL 统计接口保留兜底 |

### 6.3 迁移后的核心报表 SQL（验收标准的直接实现）

```sql
-- "按天/按 scenario 的 token 与费用报表秒级出数"：
SELECT
    day, scenario,
    sumMerge(tokens_sum) AS tokens,
    sumMerge(cost_sum)   AS cost
FROM chat_olap.model_usage_day
WHERE day >= today() - 30
GROUP BY day, scenario
ORDER BY day, scenario;
```

---

## 七、常见反模式

| 反模式 | 后果 | 正确做法 |
|---|---|---|
| **逐条 INSERT**（每条一 commit） | 海量小 part，合并风暴，too many parts 拒写 | 客户端攒批 1 万+ 行/批，或 Buffer 表 |
| **频繁 UPDATE/ALTER** | 变异重写整分区 | 业务变更留 MySQL；CH 只做 Append + 合并去重 |
| **过度分区**（按 user_id 分区） | 万级目录，元数据爆炸 | 只按时间分区，user 进排序键 |
| **ORDER BY 放高基数无过滤列** | 稀疏索引剪枝失效、压缩差 | 常用过滤/分组列进排序键前缀 |
| **点查 CH 当 OLTP 用** | 单行查询反而比 MySQL 慢 | 点查回 MySQL；CH 承接聚合 |
| **物化视图直接 SUM 不可加指标** | distinct/avg 越算越错 | `xxxState`/`xxxMerge` 或 uniqState |
| **信了 FINAL 到处用** | 大表查询强制合并，超时 | 排序键设计 + argMax 子查询 |
| **金额用 Float** | 浮点误差污染账目 | `Decimal(12,6)`（与 MySQL 版对齐） |
| **删了 MySQL 数据指望 CH 同步删** | CH 没有删除语义 | 定期对账 + 按分区重刷（TTL/REPLACE PARTITION） |

---

## 八、面试高频问答

**Q1：MergeTree、ReplacingMergeTree、AggregatingMergeTree 怎么选？**
A：按"合并时行为"选。明细只增用 MergeTree；会重放/更新的事件用 ReplacingMergeTree（排序键=主键，带版本参数，查询配 FINAL 或 argMax 去重）；报表加速用 AggregatingMergeTree 存聚合状态，通常由物化视图写入，查询用 -Merge 函数。

**Q2：ReplacingMergeTree 为什么查询时还要自己去重？**
A：它的去重发生在**后台合并时**，且合并是异步的——查询时同一主键可能仍有多个版本并存。所以要么 `FINAL` 强制合并读（慢），要么查询端 `argMax(col, version)` 取最新版本。理解"写入≠生效"是它最容易踩的坑。

**Q3：分区和排序键分别解决什么问题？**
A：分区解决**数据管理**——查询剪枝按分区整块跳过、TTL/重跑按分区操作、目录粒度是月/天；排序键解决**查询与压缩**——决定列文件物理顺序，稀疏索引按它跳过 granule，同值聚集放大压缩。分区过多是反模式，排序键前缀必须匹配查询习惯。

**Q4：物化视图的原理和适用边界？**
A：ClickHouse 物化视图是写入触发的自动聚合：INSERT 明细表时同步把新数据聚合写入目标表（TO 模式可落独立表）。它把计算成本从查询时转移到写入时，适合固定维度的报表加速；边界是"查询模式要可预期"——维度一变就要建新视图，且只对**增量**生效，历史数据需手动回填。

**Q5：为什么 ClickHouse 不适合逐条写入？**
A：每次 INSERT 生成一个 part，频繁小 INSERT 会产生海量小 part，后台合并跟不上就会触发 too many parts 拒写保护。正确做法是客户端攒批（万行级/批）或经 Kafka/Buffer 表缓冲，让 part 数量可控。

**Q6：ClickHouse 里怎么实现"更新一条数据"？**
A：没有高效的原地更新。方案按优先级：① 业务上避免——CH 只做分析副本，事实源留 MySQL；② ReplacingMergeTree 追加新版本行，靠版本号+合并/查询去重；③ `ALTER TABLE ... UPDATE` mutation（异步重写数据，低频运维用）；④ 小维度表可整表重建后 REPLACE PARTITION。

**Q7：你们项目为什么要把计量数据迁到 ClickHouse？MySQL 不行吗？**
A：能跑但不抗增长。现状是 `model_usage` 明细在 MySQL，统计接口内存聚合，量小够用；随调用量增长，按天/场景/模型的大聚合会打到业务库。目标态是 MySQL 保持事实源，CH 存明细+物化视图按天×scenario×model 预聚合，报表查询只打小表，秒级出数，且 CH 可随时按 MySQL 重建，不影响业务写路径。

**Q8：JSONEachRow 导入怎么保证幂等？**
A：三层：① 传输层重试靠"导入前按业务键（request_id）排重"；② 引擎层用 ReplacingMergeTree 兜底重复版本；③ 对账层定期比对 MySQL/CH 行数与花费总和，发现缺口按时间分区重刷（导入任务本身按水位线设计，重跑不漏不重，见批处理篇）。

---

## 九、动手练习

1. 用 Docker 起一个单机 ClickHouse（`clickhouse/clickhouse-server`），按 §2.1 建 `model_usage` 目标态表，写一个 Java 程序从 `chat_db.model_usage`（先执行 `deploy/mysql/llm-observability-init.sql`）读出数据拼 JSONEachRow 批量导入 1 万行。
2. 对 1 万行数据分别做"逐条 INSERT"与"1000 行/批 INSERT"，对比 `system.parts` 里 part 数量与插入耗时，亲手复现反模式一。
3. 给明细表建 §4 的物化视图，同一查询分别打明细表与预聚合表，记录耗时差；然后把排序键故意改成 `(request_id)` 再看耗时变化，体会排序键的影响。
4. 模拟 CDC 重放：把同 5 条 `product` JSON 各导入两遍到 ReplacingMergeTree 表，先直接 COUNT 再 `argMax` 查询，验证"写入时未去重、查询须去重"的语义。
5. ⚠️ 目标态推演：按 §6.2 清单画出本项目计量链路迁移后的时序图（MySQL→同步任务→CH→Grafana），标注哪些组件已存在（Feign 上报、model_usage 表）、哪些待建（CH 服务、同步任务、看板）——这就是技术缺口文档里"ClickHouse 用量分析"项的落地分解。

---

> 上一篇：[01-OLAP与列式存储原理](./01-OLAP与列式存储原理.md) ｜ 下一篇：[03-数仓分层与指标口径](./03-数仓分层与指标口径.md)
