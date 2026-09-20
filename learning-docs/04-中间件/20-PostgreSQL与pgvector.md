# 20-PostgreSQL 与 pgvector：从零开始理解"另一极"的关系库 + 向量能力（认知拓展，工程未落地）

> **定位**：本仓持久化是 **MySQL 8.0（1 主 2 从 + ShardingSphere）**（[03-数据库与ORM](../03-数据库与ORM/README.md)）+ **Chroma 向量库**（[05-AI集成/05-向量数据库](../05-AI集成/05-向量数据库/README.md)，选型篇已把 pgvector 列为"上量前的候选迁移路径"）。PostgreSQL 是 JD 出现率最高的"第二关系库"，而 **pgvector 让"小规模向量检索"不必单独建库**——一篇把两件事一起学掉。工程不引入。
> 前置阅读：[03-数据库与ORM/06-MVCC 与三大日志](../03-数据库与ORM/06-事务进阶-MVCC实现与三大日志.md)（对照理解两家的 MVCC）、[05-AI集成/05-向量数据库/02-索引算法](../05-AI集成/05-向量数据库/02-索引算法-向量检索怎么从全表扫描里活下来.md)（HNSW/IVF——pgvector 直接复用这两个算法）。

---

## ⚡ 30 秒速记卡

```text
① PG 的 MVCC 把"旧版本"留在表里（xmin/xmax 元组），靠 VACUUM 回收；MySQL 把旧版本放 undo——
   记忆钩子："PG 表内多版本+后台打扫" vs "InnoDB 表外 undo+purge 线程"
② PG 索引种类是 MySQL 的数倍：GIN(JSONB/全文)、GiST(含 pgvector 旧索引)、BRIN(时序大表)、部分索引
③ DDL 在 PG 是事务性的（加字段秒回且可回滚）；MySQL 的 Online DDL 是另一套工程学（03-08）
④ pgvector = vector 类型 + 三种距离(<-> L2 / <=> 余弦 / <#> 内积) + ivfflat / hnsw 两种索引
⑤ 选型口径：向量 < 千万级、想"一个库全搞定" → pgvector；专用向量库（Milvus）在大规模/多租户/高频更新时上
```

---

## 一、PostgreSQL vs MySQL：把"神教之争"变成六个具体差异

你在 [03-数据库与ORM/06](../03-数据库与ORM/06-事务进阶-MVCC实现与三大日志.md) 学过 InnoDB 的 MVCC——现在用"另一套实现"来检验理解：

| 维度 | MySQL (InnoDB) | PostgreSQL | 对你既有知识的影响 |
|---|---|---|---|
| MVCC 实现 | 旧版本写进 **undo log**，表上只有最新版 | 旧版本**留在表内**（xmin/xmax 标记），VACUUM/autovacuum 回收 | PG 长事务→表膨胀（不是 undo 膨胀）；"长事务危害"换了位置 |
| 索引 | B+树为主（+全文/空间可选） | B-tree / Hash / **GIN** / **GiST** / SP-GiST / **BRIN** / 部分/表达式索引 | "低区分度列建索引没用"在 PG 有例外（BRIN 按块范围存 min/max） |
| JSON | JSON 类型 + 虚拟列间接索引 | **JSONB 二进制** + GIN 直接索引 + 路径查询 | 文档型字段 PG 更顺手 |
| DDL | Online DDL 三板斧（[03-08](../03-数据库与ORM/08-大表治理-冷热分离与数据归档.md)） | **DDL 在事务里**：`BEGIN; ALTER; ROLLBACK;` 合法，加列常为元数据级秒回 | 大表加字段的心智负担小很多 |
| 复制 | binlog 主从（[03-05](../03-数据库与ORM/05-MySQL锁机制与主从复制读写分离.md)） | 物理流复制 + **逻辑复制**（按库表订阅变更流） | 逻辑复制 ≈ 内建轻量 CDC（对照 19 篇） |
| 生态扩展 | 插件有限 | **扩展机制**是灵魂：PostGIS/pgvector/citext/timescale… | "一个库多种能力"的底气来自扩展机制 |

> 记忆口径：**MySQL 把"复杂性"放运维（分库分表、Online DDL 工程学），PG 把"能力"放引擎（类型、索引、扩展）**。没有绝对优劣，只有团队与场景匹配。

---

## 二、pgvector：把向量检索塞进关系库

### 2.1 三个原语

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE kb_chunk (
  id        bigserial PRIMARY KEY,
  kb        text        NOT NULL,
  content   text        NOT NULL,
  embedding vector(1024) NOT NULL          -- 与你 Embedding 模型维度一致
);

-- 距离操作符（与你 05-向量库/01 学的三种度量一一对应）
--  `<->` L2 欧氏   `<=>` 余弦距离   `<#>` 负内积
SELECT id, content, embedding <=> $1 AS dist
FROM kb_chunk WHERE kb = 'aics'
ORDER BY embedding <=> $1
LIMIT 5;                                       -- 这就是 top-K 相似检索
```

### 2.2 两种索引 = 你已经学过的两个算法

| 索引 | 参数 | 对应你学过的 |
|---|---|---|
| `ivfflat` | `WITH (lists = 100)` 建库分桶；查询 `SET ivfflat.probes = 10` | [05-向量库/02](../05-AI集成/05-向量数据库/02-索引算法-向量检索怎么从全表扫描里活下来.md) 的 **IVF**：先粗选桶再精排；probes ↑ 召回↑ 延迟↑ |
| `hnsw` | `WITH (m = 16, ef_construction = 64)`；查询 `SET hnsw.ef_search = 40` | **HNSW** 分层跳表图：m=每点连边，ef=搜索时候选队列宽度 |

> **自带复习价值**：pgvector 的调参就是 05 模块"召回率-延迟-内存不可能三角"的实操版。注意两点差异：pgvector 的 HNSW **不支持增量删除回收**（标记删除靠 vacuum 整理）；`vector` 类型上限 2000 维，**halfvec（半精度）可到 4000 维**且省一半内存。

### 2.3 容量心算（接 05-向量库/03 的算例）

```text
1024 维 float32 = 4KB/向量（纯向量）
100 万条 ≈ 4GB 向量数据（未算索引放大）→ HNSW 索引另需 ~1.5~2 倍内存
→ 一台 16GB 内存的 PG 实例扛百万级向量绰绰有余
→ 这就是"小规模不必单独上 Milvus"的定量依据
```

---

## 三、选型决策：pgvector vs 专用向量库（接你的 Chroma 现状）

```text
你的现状：Chroma 0.5（单机、原型级）≈ 百万级以下、嵌入+检索够用

什么情况下迁 pgvector？
 ✓ 团队不想多养一个组件（备份/监控/权限全复用 PG 体系）
 ✓ 数据量 < 千万级、QPS 温和、过滤条件复杂（SQL WHERE 与向量检索天然同查）
 ✓ 已有 PG 运维经验

什么情况下迁 Milvus？
 ✓ 亿级向量 / 高 QPS / 多租户配额
 ✓ 需要标量+向量混合的专用引擎、滚动扩缩容
 ✓ 05-向量库/03 的"内存→机器数"算例开始疼了
```

> 你项目最现实的演进路径（写在 05 选型篇的钩子）：**Chroma（原型）→ pgvector（生产小规模，运维成本最低）→ Milvus（规模化）**。国产行业语境下还有 openGauss（PG 系）+ 其向量插件的一条路（[18-银行金融开发/15](../18-银行金融开发/15-国产数据库选型与迁移.md) 的国产库谱系）。

---

## 四、如果要引入本项目：接入点评估（未落地）

| 方案 | 做法 | 说明 |
|---|---|---|
| 知识库向量迁 pgvector | Spring AI 换 `PgVectorStore` starter（官方支持），表结构即 `kb_chunk` 式 | 好处：删一份 Chroma 依赖；knowledge 库的 PG 实例单独起 |
| 保持 Chroma | 不动 | 02-计划 P1 已把 Chroma 进 compose 并持久化——**已满足当前规模** |
| 顺带收益 | 若引入 PG，CDC 可改走 PG 逻辑复制（Debezium PG connector） | 但本仓 MQ 是 RocketMQ，与 19 篇决策树结论一致：不值 |

诚实结论：**当前规模下 pgvector 是"更优选项"而非"必需品"**；学习重点是差异表与 pgvector 调参——这两块面试可直接讲。

---

## 五、动手实验（15 分钟）

```bash
docker run -d --name pg -p 5432:5432 \
  -e POSTGRES_PASSWORD=pg123 -e POSTGRES_DB=aics pgvector/pgvector:pg16
docker exec -it pg psql -U postgres -d aics
```

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE t (id bigserial primary key, v vector(4));
INSERT INTO t (v) VALUES ('[1,0,0,0]'), ('[0,1,0,0]'), ('[0.9,0.1,0,0]');
CREATE INDEX ON t USING hnsw (v vector_cosine_ops);
SELECT id, v <=> '[1,0,0,0]' AS dist FROM t ORDER BY 2 LIMIT 2;   -- 体会 top-K
```

---

## 六、面试要点总结

```text
关键词：
MVCC 两派：PG 表内元组多版本+VACUUM vs InnoDB undo+purge · 长事务危害换位置（表膨胀）
索引家族：GIN(JSONB/全文)/GiST/BRIN(时序)/部分索引 · DDL 事务性
物理流复制 vs 逻辑复制（内建轻量 CDC，接 19 篇）
pgvector：vector(n)/halfvec · 距离符 <->/<=>/<#> · ivfflat(lists/probes) 与 hnsw(m/ef) 调参=不可能三角实操
选型：<千万级+SQL 过滤→pgvector；亿级/高 QPS/多租户→Milvus；国产语境 openGauss 系
项目锚点：Chroma 现状（05 选型）· 03-06 MVCC 对照 · 18-15 openGauss 谱系
```

## 学习检查清单

- [ ] 能讲清 PG 与 InnoDB 的 MVCC 差异及各自的长事务后果
- [ ] 能举出 3 个 MySQL 没有的 PG 索引类型及适用场景
- [ ] 能写出 pgvector 建表 + HNSW 索引 + top-K 查询
- [ ] 能把 ivfflat/hnsw 参数映射回 05 模块的召回-延迟-内存三角
- [ ] 能给出"Chroma→pgvector→Milvus"的演进判据

## 下一步

- [05-AI集成/05-向量数据库/03-选型与容量规划](../05-AI集成/05-向量数据库/03-选型与容量规划.md)：向量库选型的完整决策树；
- [21-RabbitMQ 概念模型](21-RabbitMQ概念模型与AMQP协议.md)：最后一个 T1 补充项。
