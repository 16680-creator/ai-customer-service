# CDC 与实时同步链路

> **分工声明**：Canal 本身是什么、binlog 三种格式、本项目 Canal 链路的组件选型与部署边界，已在 [04-中间件/11-CanalCDC商品索引同步](../04-中间件/11-CanalCDC商品索引同步.md) 讲透（MySQL→Canal→RocketMQ→ES 全链路与启用顺序）。本篇不再重复 Canal 用法，聚焦**链路工程学**——快照与增量怎么衔接、位点怎么管、乱序迟到怎么治、Exactly-once 怎么落地、对账怎么建，把"能同步"升级为"敢依赖"。
> 对应项目：`ai-cs-search/src/main/java/com/aics/search/cdc/ProductCdcConsumer.java`（消费端幂等落地）、`ai-cs-search/src/main/java/com/aics/search/cdc/CanalChangeEvent.java`（事件信封）、`deploy/mysql/canal-init.sql`（最小权限账号）、MySQL binlog 配置（`deploy/mysql/master.cnf`）。
> 相关：[04-中间件/11](../04-中间件/11-CanalCDC商品索引同步.md)（前置必读）、[04-中间件/02-RocketMQ消息队列](../04-中间件/02-RocketMQ消息队列.md)（重试与死信）、[08-数据质量与对账](./08-数据质量与对账.md)（本篇第 6 节的展开）、[05-批处理与调度编排](./05-批处理与调度编排.md)（对账任务的调度形态）。

---

## 一、先结论：CDC 链路的四道坎

| 坎 | 问题 | 解法（本篇章节） |
|---|---|---|
| **初始化** | 存量数据怎么进读模型？增量从哪开始追？ | 快照 + 增量衔接（§3） |
| **断点续传** | 消费端重启后，从哪继续？会不会重复/漏？ | 位点管理（§4） |
| **顺序** | 事件乱序/迟到，读模型回退到旧值 | 版本比较 + 分区有序（§5） |
| **语义** | MQ 至少一次投递，端到端怎么不重复生效？ | 幂等消费 = 端到端 Exactly-once（§6） |
| **兜底** | 以上全对了，仍有未知的丢/坏 | 全链路对账 + 重放（§7） |

> 本项目链路：MySQL（ROW/FULL binlog）→ Canal Server → RocketMQ `c-product-sync` → `ProductCdcConsumer` → ES `product_catalog`。Canal Server/Adapter 容器编排**未落地**（缺口清单已立项），消费端与幂等逻辑**已落地**。

---

## 二、binlog 原理：只补三件工程要用的知识

binlog 基础（三种格式、为什么 ROW+FULL）请看 [04-中间件/11](../04-中间件/11-CanalCDC商品索引同步.md) 第四节，这里只补 CDC 链路工程直接依赖的三点：

**① binlog 事件自带坐标**。每个事件都有唯一坐标 `(file, position)`，或开启 GTID 后的全局唯一 `(server_uuid, gno)`。**位点管理的本质就是记住"最后一个已处理事件的坐标"**。

```
mysql-bin.000003  │ ... │ pos=101324: UPDATE product SET price=188 WHERE id=1001
                  │      pos=101640: UPDATE product SET stock=99  WHERE id=1001
                                             ▲
                                 消费端位点 = 101640（处理到这里了）
```

**② binlog 在从库/订阅者之间是"顺序流"**。MySQL 保证单库单线程复制顺序与提交顺序一致（组提交内按 commit order）。Canal 模拟 slave 拉取时**同一 instance 内事件有序**——这是本项目 [04-中间件/11](../04-中间件/11-CanalCDC商品索引同步.md) 第十节说"单 instance 通常可保持同一表变化顺序"的依据。

**③ binlog 不等于业务日志**。一条 `UPDATE ... WHERE status='A'` 可能影响多行，ROW 格式下拆成**多行事件**；DDL（改表结构）也是事件。消费端要按事件粒度处理，不能按"一条 SQL=一条消息"假设。本项目 `CanalChangeEvent.data` 是 `List<Map<String,Object>>`（`CanalChangeEvent.java:10-15`），正是为"一个事件多行"设计的。

---

## 三、快照 + 增量：冷启动的正确姿势

### 3.1 为什么不能"先开增量再慢慢补历史"

```
错误做法：T0 开启 CDC 增量 → T1 再开始全量导出
  T0~T1 之间的存量数据 → 既不在快照里，增量又从 T0 开始 → 恰好覆盖？
  不，增量从"当前位点"开始，当前位点之前的历史数据永久漏掉 ❌
```

### 3.2 两种标准衔接方案

| 方案 | 流程 | 适用 |
|---|---|---|
| **先快照后增量**（停写窗口） | ① 记录当前 binlog 位点 P0 → ② 全量导出+导入读模型 → ③ 消费从 P0 开始追增量 | 可接受短暂停写/读模型可短暂滞后；小表 |
| **快照与增量并行**（不停写） | ① 记位点 P0 → ② 并行：全量导出 + CDC 事件缓冲不消费 → ③ 快照导入完成 → ④ 从 P0 回放缓冲的增量（幂等去重） | 生产标准做法；依赖幂等消费 |

```
时间轴 ──────────────────────────────────────▶
         P0 │←──── 全量导出（长）────→│
            │←── 增量事件持续产生 ─────────────────→
            │         （缓冲/落盘，不消费）  │
            └─────────────── 快照导入完成 ──┤
                                           ▼
                                    从 P0 开始回放增量
                                    （重复部分被幂等吃掉）
```

**关键认知**：并行方案必然把"快照期间已发生的变更"回放一遍，覆盖到快照数据上——**只要消费幂等，重复无害**。这也是为什么 §6 的幂等是一切方案的前提。

### 3.3 本项目对应

`deploy/mysql/canal-init.sql` 创建 canal 账号含 `REPLICATION SLAVE, REPLICATION CLIENT` 权限，即订阅增量位点用的；商品表存量小（演示库），[04-中间件/11](../04-中间件/11-CanalCDC商品索引同步.md) 的端到端验收步骤是"先建 ES mapping → 开增量 → UPDATE 验证"，隐含假设是存量已通过全量初始化（或演示数据脚本 `demo-data.sql` 直接灌入两端）。**若商品数据量到千万级，快照衔接就是必修课**。

---

## 四、位点管理：断点续传的两种存法

### 4.1 位点存哪里

| 存法 | 机制 | 特点 |
|---|---|---|
| **订阅端自管** | Canal Server 把位点落到自己的 meta 库/zk；重启接着上次拉 | 链路默认行为；Canal 挂了不丢事件（位点没推进） |
| **中间件代管** | 经 MQ 后，"位点"由 MQ 消费进度（ConsumeQueue offset）替代 | 本项目的实际形态：Canal→MQ 后，search 重启恢复靠 **RocketMQ 消费位点**，而非 binlog 位点 |
| **消费端自管** | 业务表记录已处理到的 ID/时间 | 用于对账与补偿扫描，不替代上面两者 |

**链路视角**：完整链路有**两级位点**——Canal 的 binlog 位点（MySQL→MQ 段）与 RocketMQ 消费位点（MQ→ES 段）。排障时要分段看：

```
排查"ES 没更新"的位点检查单（呼应 04-中间件/11 第十五节 Q2）：
① MySQL binlog 写到哪了：SHOW MASTER STATUS
② Canal meta 位点推进了吗：canal meta 库/日志
③ RocketMQ c-product-sync 有没有消息堆积：MQ 控制台
④ search 消费位点/重试队列：MQ 消费者状态 + 死信 topic
⑤ ES 文档：GET product_catalog/_doc/{id}
```

### 4.2 位点回退与重复消费

位点只保证**不丢**（至少一次），不保证不重：

```
search 消费了消息、写了 ES，但消费位点上报 MQ 前进程被杀
  → 重启后从旧位点重新投递 → 同一 UPDATE 事件被处理两次
  → ES _id 固定为 product.id 的 upsert 让第二次覆盖第一次，结果一致 ✅
```

**结论：位点管理选择"至少一次 + 幂等消费"组合，而不是试图做"恰好一次投递"**——分布式系统里前者是可实现的工程解，后者在跨系统边界上基本是幻觉（§6）。

---

## 五、乱序与迟到

### 5.1 乱序从哪来

```
单 Canal instance 单 topic 单消费者 → 有序（本项目 MVP 现状）

一旦扩容，乱序的入口至少有四个：
① Canal instance 水平拆分（按库/表分 instance）→ 同表事件可能跨实例
② MQ 多队列/多消费者并发 → 同一商品的事件并行处理
③ 消费失败重试 → 事件 B 失败重试期间，事件 C（更新）已先成功
④ 读模型端并行写（批量 flush 乱序完成）
```

### 5.2 后果演示

```
正确顺序：A(199→188) → B(188→168) → ES=168 ✓
乱序到达：B 先写(168) → A 后写(188) → ES 回退到 188 ❌（旧值覆盖新值）
```

### 5.3 治理三板斧（与 04-中间件/11 §10.2 一致并展开）

| 手段 | 做法 | 成本 |
|---|---|---|
| **分区有序** | MQ 生产端按 `product.id` 哈希选队列 → 同一商品事件串行 | 零侵入，首选 |
| **版本比较** | 事件带 `updated_at` 或 binlog 位点作 version；消费端 `if (new_version <= old_version) skip` | 读模型需能存版本；ES 可用 external version：`version_type=external`，旧版本写入被 ES 直接拒绝 |
| **最终值幂等** | upsert 天然"最后写赢"——配合版本比较就是"**最新**写赢" | 已落地（见 §6） |

**迟到事件**（比乱序更隐蔽）：事件不是到晚了，而是"在 MQ 重试队列里躺了几分钟才来"，此时它携带的版本已落后。版本比较同样兜住；若无版本字段，则靠对账发现"读模型旧于事实源"再补偿（§7）。

---

## 六、Exactly-once 语义：端到端怎么达成

### 6.1 先泼冷水：投递语义的真相

| 语义 | 在 MQ 层面 | 在端到端层面 |
|---|---|---|
| At-most-once | 不重试 | 会丢 |
| **At-least-once** | 重试直到确认 ✅ 多数 MQ 默认 | **会重复** |
| Exactly-once | 需事务消息/事务性消费，且只覆盖"MQ 内部" | **跨系统无普适解** |

**工程结论：端到端 Exactly-once = At-least-once 投递 + 幂等消费**，业内称为 Effectively-once。别在面试里说"我们 MQ 配置了 exactly-once"——配置项只管 MQ 自己那一跳。

### 6.2 幂等的三种实现强度

| 强度 | 实现 | 例子 |
|---|---|---|
| 天然幂等 | 覆盖写（upsert）、状态机置位 | 本项目 ES 同步；订单关单 |
| 判重幂等 | 处理前查"处理过没有"（去重表/Redis SETNX） | 通用但多一次 IO |
| 事务幂等 | 消费+位点提交在同一事务 | 需存储支持，链路复杂 |

### 6.3 项目现场：ProductCdcConsumer 的幂等设计（已落地）

```java
// ai-cs-search/src/main/java/com/aics/search/cdc/ProductCdcConsumer.java:43-64（节选）
void syncOne(String type, Map<String, Object> row) {
    Object id = row.get("id");
    if (id == null) { return; }                              // ① 无主键事件直接忽略
    String docId = String.valueOf(id);
    // ② 删除判定：type=DELETE 或逻辑删除标记 deleted=1
    if ("DELETE".equalsIgnoreCase(type) || "1".equals(String.valueOf(row.get("deleted")))) {
        try {
            elasticsearchClient.delete(d -> d.index(INDEX).id(docId));
        } catch (Exception ignored) { /* 文档不存在等 → 删除天然幂等 */ }
        return;
    }
    // ③ upsert：ES _id 固定 = product.id → 同一事件重放 N 次结果一致（覆盖写幂等）
    elasticsearchClient.index(i -> i.index(INDEX).id(docId).document(row));
    // ④ 失败必须抛出 → RocketMQ 重试 → 至少一次语义成立；吞异常=丢事件
    throw new IllegalStateException("CDC 商品索引同步失败: id=" + docId, e);
}
```

四步对照 §6.2：③ 是**天然幂等**（覆盖写），② 的重复删除也是幂等，④ 保障 at-least-once 成立，① 防脏事件。注释里写明的策略（`ProductCdcConsumer.java:17-18`："ES document id 固定为 product.id，同一条 INSERT/UPDATE 重放均是覆盖 upsert；DELETE 反复执行时 documentMissing 忽略"）就是 §6.1 结论的代码化。

**但注意当前实现的两个已知短板**（学习价值极高）：
1. **无版本比较**——乱序场景（§5.2）会旧值覆盖新值；扩容前必须补 version/external version。
2. **DELETE 与 upsert 竞态**——"先 UPDATE 后 DELETE"乱序成"先 DELETE 后 UPDATE"会复活文档；同样靠版本或分区有序解决。

---

## 七、全链路对账与重放

### 7.1 为什么幂等+位点+版本还不够

还存在**未建模的丢失面**：MQ 消息超过重试次数进死信没人处理、ES 集群丢副本、Canal meta 位点被误重置跳过事件、binlog 过期清理（`expire-logs-days=7`，[04-中间件/11](../04-中间件/11-CanalCDC商品索引同步.md)）期间 Canal 宕机超一周。**对账是唯一能兜住"未知未知"的手段**。

### 7.2 对账设计三层次（详见 08 篇，此处给链路版）

| 层次 | 比什么 | 频率 | 命中什么问题 |
|---|---|---|---|
| 总量对账 | `SELECT COUNT(*) FROM product WHERE deleted=0` vs ES `_count` | 每 5~10 分钟 | 大面积丢失/堆积 |
| 分片对账 | 按商品类目/ID 段分组比对计数 | 小时级 | 局部缺口 |
| 明细对账 | 逐条比关键字段（price/status/deleted）或比对 `updated_at` | 每日+抽样 | 字段级不一致、迟到覆盖 |

```sql
-- 总量对账（目标态 SQL，落成定时任务，调度见 05 篇）
-- MySQL 侧：
SELECT COUNT(*) FROM aics_product.product WHERE deleted = 0;
-- ES 侧：
-- GET product_catalog/_count {"query":{"term":{"deleted":"0"}}}
-- 差异 > 阈值 → 告警 + 记录差异 → 触发补偿
```

### 7.3 补偿与重放

```
发现差异（如 MySQL 有 id=10086，ES 没有）
  ① 单条补偿：直接按 MySQL 当前值 upsert ES（读模型以 MySQL 为准）
  ② 段级重放：按主键段/时间窗从 MySQL 重导出重灌（幂等所以敢重放）
  ③ 全量重建：极端情况（mapping 变更/大面积腐烂）——
     新索引别名写入 → 全量灌 → 别名原子切换（零停机换索引）
```

**重放为什么"敢"做**：因为消费端幂等（§6）——重放是重复执行，结果不变。这就是"幂等"在 CDC 链路里的乘数效应：**位点可回退、快照可并行、对账可补偿、升级可重建，全都建立在同一次幂等设计上**。

---

## 八、面试高频问答

**Q1：CDC 链路里位点是什么？为什么会有两级位点？**
A：位点=已处理事件的坐标，保证断点续传不丢。Canal 链路有两级：Canal 的 binlog 位点（file/position 或 GTID，管 MySQL→MQ 段）与 RocketMQ 消费位点（管 MQ→消费段）。排查"下游没更新"必须分段检查两级位点，不能只看一处。

**Q2：全量初始化和增量同步怎么衔接才不漏数据？**
A：先记录当前 binlog 位点 P0，再全量导出导入，然后从 P0 消费增量；生产上用不停写的并行方案——快照导出与增量事件缓冲并行，快照完成后从 P0 回放。因为回放会与快照重叠，所以**幂等消费是方案成立的前提**。

**Q3：MQ 至少一次投递，怎么做到端到端不重复生效？**
A：端到端 Exactly-once = at-least-once + 幂等消费。幂等按强度分：天然幂等（固定主键 upsert、状态机置位）、判重幂等（去重表/Redis）、事务幂等（消费与位点同事务）。MQ 层的"exactly-once 配置"只覆盖 MQ 内部一跳，跨系统必须靠消费端幂等。

**Q4：本项目 CDC 消费端是怎么保证幂等的？**
A：ES document id 固定为 MySQL product.id，同一 UPDATE 重放是覆盖 upsert；DELETE 重复执行文档不存在被忽略；ES 异常时抛出让 RocketMQ 重试而不是吞掉（吞掉等于把 at-least-once 变成 at-most-once）。已知短板是没有版本比较，乱序扩容前要补 external version。

**Q5：CDC 事件乱序怎么产生？怎么治理？**
A：单实例单队列有序；扩容引入乱序——多 instance 拆分、MQ 多队列并发、失败重试错峰、消费端并行写。治理三板斧：按业务键哈希分区保单键有序；事件带版本（binlog 位点/updated_at）做旧版本丢弃（ES external version）；最终靠"最新写赢"的版本化 upsert 兜底。

**Q6：为什么有了幂等和位点还要对账？**
A：幂等管"重复无害"，位点管"已知的从哪继续"，但都兜不住未建模丢失：死信没人处理、存储丢数据、位点误操作、binlog 过期清理期间宕机。对账以事实源为准绳周期性比对总量/分片/明细，发现差异触发单条补偿、段级重放或全量重建，是链路一致性的最后防线。

**Q7：快照与增量并行方案为什么会重复处理数据？为什么无害？**
A：P0 位点之后的增量事件与全量快照在时间上重叠，快照完成后从 P0 回放必然重放快照期间已发生的变更。无害的原因是消费幂等：覆盖写让回放结果与首放一致。所以方案选择上"先做幂等，再谈并行快照"。

**Q8：重放（replay）为什么在 CDC 链路里是安全的？**
A：因为消费端幂等设计使重复执行无副作用（本项目固定 _id upsert）。于是位点可回退、对账发现缺口可按段重放、索引重建可全量重灌。可以说幂等是 CDC 链路所有"可重试性"的乘数：一次幂等设计，换来全链路每个环节的容错自由。

---

## 九、动手练习

1. 走读 `ProductCdcConsumer.java`（70 行），逐行标注 §6.3 四步（忽略/upsert/删除/抛异常）对应的行号，再回答：若把抛异常改成 log.warn 吞掉，链路语义会退化成什么？
2. 给 `CanalChangeEvent` 增加版本字段的预研：画出"外部版本写入 ES（external version）"的时序图，标注乱序事件被 ES 拒绝的位置。
3. 设计商品总量对账任务：MySQL `COUNT(*) WHERE deleted=0` 与 ES `_count` 每 10 分钟比对，差异>0 记录差异 id 并告警——写出该任务作为 XXL-Job handler 的伪代码（调度配置参照 [05-批处理与调度编排](./05-批处理与调度编排.md)）。
4. 复现乱序：用两个线程把 `UPDATE price=188` 与 `UPDATE price=168` 的 CDC 消息乱序发给本地消费端，观察 ES 最终值；再给消费端加版本比较后重试，验证"最新写赢"。
5. 按 §4.1 的五步检查单，对本项目链路（MySQL→Canal→MQ→search→ES）写一份排障 runbook：每一步给出具体命令/控制台入口与"异常时结论"。

---

> 上一篇：[03-数仓分层与指标口径](./03-数仓分层与指标口径.md) ｜ 下一篇：[05-批处理与调度编排](./05-批处理与调度编排.md)
