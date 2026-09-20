# 19-Debezium 与 Canal 的 CDC 对比：从零开始理解"变更数据捕获"的两大家族（认知拓展）

> **定位**：本仓已选 **Canal**（[04-中间件/11-CanalCDC商品索引同步](11-CanalCDC商品索引同步.md)：MySQL binlog ROW → Canal → RocketMQ `c-product-sync` → `ProductCdcConsumer` 幂等 upsert 到 ES），选型理由在 [00-学习路线总览/02-中间件补全计划 P3](../00-学习路线总览/02-中间件补全开发计划.md) 写明（投递 RocketMQ、伪装 slave 架构好讲）。**Debezium 是国际社区的事实标准**——本篇不引入，只把 CDC 知识收口成"一张对比表 + 一棵决策树"。
> 前置阅读：[04-中间件/11](11-CanalCDC商品索引同步.md)（必读）、[03-数据库与ORM/06-事务进阶与三大日志](../03-数据库与ORM/06-事务进阶-MVCC实现与三大日志.md)（binlog 三格式）、[14-数据工程与OLAP/04-CDC实时链路](../14-数据工程与OLAP/README.md)（CDC 工程学）。

---

## ⚡ 30 秒速记卡

```text
① CDC 的三个不变量：捕获(读 binlog) → 传输(带 offset/时间戳) → 消费(幂等+乱序处理+对账)
② Canal：伪装 MySQL slave 拉协议，独立 server，原生投递 RocketMQ——"中国式轻量"
③ Debezium：跑在 Kafka Connect 框架里，source connector + 内建 snapshot + offset 存 Kafka
④ 最大差异：Debezium 自带"全量快照+增量"一体与精确 offset 断点续传；Canal 全量要自己造（对账脚本）
⑤ 决策树：投 RocketMQ→Canal；Kafka+Connect 生态→Debezium；要流上再加工→Flink CDC(基于 Debezium)
```

---

## 一、回顾本仓的 CDC 链路（对照对象）

```text
MySQL(binlog ROW+FULL, canal 账号 REPLICATION SLAVE)
   → canal-server(instance: 只监听 product 库表)
   → RocketMQ topic: c-product-sync
   → ai-cs-search ProductCdcConsumer
      · 幂等：ES _id = product.id，upsert/DELETE 天然幂等
      · 乱序：消息带时间戳/位点，旧版本丢弃
      · 兜底：sync-kb-to-es.py 全量初始化 + 定时对账(MySQL count vs ES count) + 死信
```

这套"**幂等 / 乱序 / 对账**"三板斧（[14-数据工程/04](../14-数据工程与OLAP/README.md) 把它叫 CDC 工程学）在 Debezium 下**同样成立**——变的只是捕获与传输的实现。带着这个认知看对比，事半功倍。

---

## 二、两大家族的工作方式对比

### 2.1 Canal：伪装 Slave 的"拉协议"客户端

```text
Canal Server 自己注册成 MySQL 的一个 slave（REPLICATION SLAVE 权限）
   → dump 协议拉 binlog 流 → 按 instance 配置过滤库表
   → 转成自定义 JSON/Protobuf 消息 → 直接投 RocketMQ（也可 TCP 模式让客户端拉）
```

- 优点：**架构简单直白**（一个 server + 一个投递目标），国产、文档中文，与 RocketMQ 天然直连——你的选型理由；
- 代价：**全量初始化要自己造**（你用 `sync-kb-to-es.py` + 对账兜底补上了）；位点管理较朴素，运维自动化弱于 Connect 生态。

### 2.2 Debezium：Kafka Connect 框架里的 Source Connector

```text
Kafka Connect 集群（分布式，REST 管理）
   └─ Debezium MySQL connector
        · 启动时先做 snapshot（内建全量：按主键分块扫描表，记录快照水位）
        · 再切增量：读 binlog 流，封装成带 key/value/schema 的变更事件
        · offset 存在 Kafka topic → 重启精确续传（at-least-once）
   → 变更事件进 Kafka topic（<server>.<db>.<table> 命名）
   → 消费端 / Kafka Streams / Flink 继续加工
```

- 优点：**全量+增量一体**、offset 管理精确、Schema Registry 可选（连 schema 演进都管）、Connect 生态（Sink 直连 ES/S3/JDBC）；
- 代价：**绑 Kafka**（这正是本仓没选它的原因——MQ 是 RocketMQ）；组件多（Connect 集群、Schema Registry 可选）、Java/运维栈更重。

### 2.3 逐维度对比表（面试可直接背）

| 维度 | Canal | Debezium |
|---|---|---|
| 架构形态 | 独立 server（伪装 slave） | Kafka Connect 集群内的 Source Connector |
| 投递目标 | RocketMQ/Kafka/TCP 自定义 | **Kafka**（Connect 生态 Sink 二跳） |
| 全量初始化 | ❌ 自建（对账脚本/工具） | ✅ snapshot 内建（initial/incremental 快照） |
| offset 管理 | 文件/meta（较朴素） | Kafka topic，精确续传 |
| 变更事件 | 自定义结构（ES 连接器或自解析） | 标准化 envelope（before/after/source/op/ts_ms） |
| DDL 处理 | 有（可配置忽略/投递） | 有（ddl 事件，schema 演进管理更强） |
| 多数据库支持 | MySQL 系为主 | **MySQL/PG/Mongo/Oracle/SQLServer…** 全家桶 |
| 监控运维 | 基础 | Connect REST + JMX 指标丰富 |
| 生态加工 | 消费端自理 | Kafka Streams / Flink / Sink 连接器现成 |
| 上手成本（中国语境） | 低、中文资料多 | 需先接受 Kafka Connect 模型 |

---

## 三、决策树（背下来，面试"为什么选 Canal"就从这推）

```text
你们的下游 MQ 是什么？
 ├─ RocketMQ（本仓）        → Canal（原生直连）；或 Debezium→Kafka→桥接（多一套栈，不值）
 ├─ Kafka，且有 Connect 生态 → Debezium（snapshot/offset/多库支持白拿）
 └─ 没有 MQ，想直接加工流    → Flink CDC（底层就是 Debezium，捕获即计算，
                              适合"变更→实时大屏/实时风控"，本仓 14 模块的扩展方向）
多库异构（MySQL+PG+Mongo）？ → Debezium/Flink CDC（Canal 基本只覆盖 MySQL 系）
只要 MySQL→MQ 一条线？      → Canal 足够，别为 5% 的场景引入 200% 的栈
```

---

## 四、同一需求在两家的实现映射（用本仓任务做翻译练习）

| 本仓 CDC 任务（04-11） | Canal 侧做法 | Debezium 侧等价物 |
|---|---|---|
| 首次全量建索引 | `sync-kb-to-es.py` 离线全量 | connector `snapshot.mode=initial` 自动分块快照 |
| 增量变更 | canal-server 过滤 product 库表 → RocketMQ | connector 表白名单 → Kafka topic |
| 消费幂等 | ES `_id=product.id` upsert | 同样在消费端做（envelope 的 before/after 让 upsert 更省事） |
| 乱序丢弃 | 消息带 ts/位点比较 | `source.ts_ms` + ES version 外部控制 |
| 对账兜底 | MySQL count vs ES count 定时任务 | 同样需要（CDC 是 at-least-once，不是"免对账"） |
| 失败重试 | RocketMQ 重投 + 死信 | Connect 框架重试 + DLQ topic |

> **核心认知**：换捕获工具不改变"三板斧"——**幂等、乱序、对账永远要在消费端成立**，因为 binlog 消费天然 at-least-once。

---

## 五、面试要点总结

```text
关键词：
CDC 三段：捕获→传输→消费 · binlog ROW 模式是前提（03-06 双 1/格式）
Canal=伪装 slave 拉协议+直投 RocketMQ（轻、直、中文）；Debezium=Kafka Connect+snapshot+offset（重、标准、多库）
全量+增量一体 vs 全量自建对账 · envelope(before/after/op/ts) 标准变更事件
Flink CDC=Debezium 之上"捕获即计算" · at-least-once ⇒ 幂等/乱序/对账三板斧不可省
项目锚点：04-11 全链路（c-product-sync/ProductCdcConsumer/sync 脚本/对账）· 02-计划 P3 选型理由
```

## 学习检查清单

- [ ] 能画出两家的架构图并指出 offset 各存哪
- [ ] 能按"下游是什么 MQ"走一遍决策树，并说出本仓选 Canal 的完整理由
- [ ] 能把本仓 CDC 六项任务翻译到 Debezium 侧的等价物
- [ ] 能解释为什么换捕获工具不改变消费端三板斧

## 下一步

- [04-中间件/11-CanalCDC商品索引同步](11-CanalCDC商品索引同步.md)：本仓真实链路逐行解读；
- [20-PostgreSQL 与 pgvector](20-PostgreSQL与pgvector.md)：CDC 的另一大源库（逻辑复制）主角。
