# SQL 调优与执行计划

> 对应项目：`deploy/mysql/order-init.sql:24-45`（orders 表真实索引 DDL）、`deploy/mysql/mysql.cnf:19-21`（慢查询日志已开启）、`ai-cs-order/.../OrderServiceImpl.java:197-200`（真实查询案例）。
> 相关：[01-MySQL核心知识](../03-数据库与ORM/01-MySQL核心知识.md)（**索引类型/最左前缀/EXPLAIN 入门/慢日志开启已讲——本篇不重复，只写调优实操**）、[05-MySQL锁机制与主从复制读写分离](../03-数据库与ORM/05-MySQL锁机制与主从复制读写分离.md)（**锁、唯一索引仲裁、idx_expire_time 超时扫描的并发语义在那边**）、[02-MyBatisPlus实战](../03-数据库与ORM/02-MyBatisPlus实战.md)、[06-接口性能优化实战](./06-接口性能优化实战.md)（N+1 与批量写入在应用层怎么改）。

---

## 一、先给结论：拿到一条慢 SQL 的固定动作

```
1. EXPLAIN 看四件事（优先级从高到低）：
   type 是不是 ALL（全表扫）→ rows 是不是远大于结果集 → key 是不是 NULL
   → Extra 有没有 Using filesort / Using temporary
2. 若走索引但仍慢：看 Extra 的 Using index condition / Using where，估算回表与过滤比例
3. 改法按"性价比"排序：加/改索引 > 改写 SQL（覆盖索引/延迟关联）> 应用层缓存 > 升级硬件
4. 改完必须 EXPLAIN 复验 + 慢日志复测，一次只改一处（方法论见 [08](./08-性能问题定位方法论.md)）
```

**与前置文档的分工**（本篇开头明确）：索引是什么、B+ 树结构、最左前缀原理、慢日志怎么开——[01-MySQL核心知识](../03-数据库与ORM/01-MySQL核心知识.md) 已讲；锁与并发——[05-MySQL锁机制](../03-数据库与ORM/05-MySQL锁机制与主从复制读写分离.md) 已讲。本篇全篇是**动手调优**：EXPLAIN 每一列、失效场景、进阶写法。

---

## 二、EXPLAIN 全列逐个讲

以本项目 orders 表（`deploy/mysql/order-init.sql:24-45`）为例：

```sql
EXPLAIN SELECT order_no, status FROM orders
WHERE user_id = 1001 AND status = 1 ORDER BY create_time DESC LIMIT 20;
```

| 列 | 含义 | 调优关注点 |
|---|---|---|
| `id` | select 序号；越大越先执行 | 子查询/union 时看执行顺序 |
| `select_type` | SIMPLE / PRIMARY / SUBQUERY / DERIVED | DERIVED（派生表）多→考虑改 join |
| `table` | 本步访问的表 | — |
| `partitions` | 命中的分区 | 未分区表恒 NULL |
| **`type`** | **访问方式（见等级表）** | **第一优先：`ALL` = 全表扫描，基本要优化** |
| `possible_keys` | 优化器"可能"用的索引 | 有值但 `key` 为 NULL = 索引存在但没用上（查 §三） |
| **`key`** | **实际用的索引** | NULL = 没走索引；用错索引 → `FORCE INDEX` 应急验证 |
| `key_len` | 用的索引字节数 | 判断联合索引用了几列：`int=4字节`，`varchar(n) utf8mb4≈4n+2`，可空 +1 |
| `ref` | 与索引比较的对象 | `const`（常量）最好；`func` 说明有函数参与 |
| **`rows`** | **预估扫描行数** | 第二优先：与结果集差 2~3 个量级就该怀疑 |
| `filtered` | 该层过滤后剩余比例 % | 配合 rows 估算"扫了 10 万只留 100"这类低效 |
| **`Extra`** | **附加行为（见下）** | **第三优先：filesort/temporary 是红色信号** |

### 2.1 type 等级表（从好到坏）

| 等级 | 含义 | 本项目场景例 |
|---|---|---|
| `system`/`const` | 主键/唯一键等值，最多 1 行 | `WHERE order_no = 'A2026...'`（`order-init.sql:42` 的 `uk_order_no`） |
| `eq_ref` | join 时用被驱动表主键/唯一键 | 订单 join 订单项按主键关联 |
| `ref` | 普通索引等值 | `WHERE user_id = 1001`（`idx_user_id`） |
| `range` | 索引范围扫描 | `WHERE expire_time < NOW()`（`idx_expire_time`，超时扫描的访问方式，见 [05-MySQL锁机制 §3.3](../03-数据库与ORM/05-MySQL锁机制与主从复制读写分离.md)） |
| `index` | 扫整棵索引树（比 ALL 好在索引小） | 覆盖索引的全扫；仍是反模式 |
| **`ALL`** | **全表扫描** | 需优化，除非表只有几百行 |

### 2.2 Extra 关键值表

| 值 | 含义 | 动作 |
|---|---|---|
| `Using index` | 覆盖索引：查询列全在索引里，**零回表** | ✅ 好，主动制造（§四） |
| `Using index condition` | ICP 生效：在索引层先过滤再回表 | ✅ 好（§四） |
| `Using where` | 服务层再过滤 | 中性，配合 rows/filtered 评估 |
| `Using filesort` | **排序在内存/磁盘额外完成** | ❌ order by 列不在索引序里，补联合索引 |
| `Using temporary` | **建临时表**（group by/distinct 常见） | ❌ 通常要改索引或改写 |
| `Using join buffer (Block Nested Loop)` | join 没走索引，用缓存块兜底 | ❌ 给被驱动表 join 列加索引 |
| `Select tables optimized away` | 聚合直接从索引得答案 | ✅ 极优（如 MIN/MAX 走索引首尾） |

---

## 三、索引失效八场景（每个都给前后 EXPLAIN 差异）

> orders 表索引：`uk_order_no(order_no)`、`idx_user_id(user_id)`、`idx_status(status)`、`idx_expire_time(expire_time)`（`order-init.sql:42-45`）。

| # | 场景 | 反例（type=ALL） | 正解 |
|---|---|---|---|
| 1 | **索引列套函数/运算** | `WHERE YEAR(create_time)=2026` | `WHERE create_time >= '2026-01-01' AND create_time < '2027-01-01'` |
| 2 | **隐式类型转换** | `WHERE order_no = 202601`（列是 varchar，传数字→整列传varchar只失一次效的反向） | `WHERE order_no = 'A202601'`（与列类型一致） |
| 3 | **前导模糊** | `WHERE order_no LIKE '%2026%'` | 前缀可定的用 `LIKE 'A2026%'`；两端模糊上 ES（本项目搜索走 [03-Elasticsearch](../04-中间件/03-Elasticsearch搜索引擎.md)） |
| 4 | **OR 两边未全覆盖** | `WHERE user_id=1 OR phone='x'`（phone 无索引） | 都有索引才走；否则拆两条 UNION 各自走索引 |
| 5 | **联合索引不满足最左** | `KEY(a,b,c)` 下 `WHERE b=1` | 无论如何带上 a；`WHERE a=1 AND b=1` 才完整利用 |
| 6 | **`!=` / `NOT IN` / `IS NOT NULL`** | `WHERE status != 1`（若 status=1 占 99%） | 优化器按**选择性**判断，反着写：`WHERE status IN (0,2,3)`；小占比取反有时更快 |
| 7 | **隐式字符集/排序规则不一致** | join 时两表列 charset 不同，索引作废 | 建表统一 utf8mb4（本项目 init.sql 统一，跨表 join 前查 `SHOW CREATE TABLE`） |
| 8 | **优化器判定全表更快** | 低选择性列上强行走索引反而回表更多 | 尊重优化器：给低选择性列删索引或做联合索引的一部分（`idx_status` 单列选择性低，实战中常与 user_id 组合成联合索引） |

**通用心法**：失效大多是"索引列被加工"或"优化器的成本估算"。八条背下来，EXPLAIN 的 `possible_keys 有但 key 为 NULL` 时逐条对号。

---

## 四、覆盖索引与索引下推 ICP

### 4.1 覆盖索引：查询列全在索引里，省掉回表

```sql
-- 现状：idx_user_id 只有 user_id，下面查询要回表取其他列
EXPLAIN SELECT order_no, total_amount, status FROM orders WHERE user_id = 1001;
-- type=ref，但每行都要"索引→主键→回表"一次

-- 覆盖索引改造（评估写入放大后）：
ALTER TABLE orders ADD KEY idx_user_cover (user_id, status, order_no, total_amount);
EXPLAIN SELECT order_no, status FROM orders WHERE user_id = 1001;   -- Extra: Using index ✅
```

**取舍**：覆盖索引把"读的随机 IO"换成"写的多份索引拷贝"——订单表写入频繁时，把**最高频的一两个查询**做覆盖即可，不要给每个查询都造一个。

### 4.2 索引下推 ICP（MySQL 5.6+，默认开启）

```sql
-- 联合索引 (user_id, status)：单独的 idx_status 不可用时的组合
EXPLAIN SELECT * FROM orders WHERE user_id = 1001 AND status = 1;
-- 无 ICP：引擎按 user_id 取出全部行 → 全部回表 → 服务层再过滤 status
-- 有 ICP：引擎在索引层就判断 status=1（Extra: Using index condition），只回表命中的行
```

ICP 的收益 = **回表次数从"前导列命中数"降到"全条件命中数"**。`status` 选择性越高收益越大；在 `Extra` 看到 `Using index condition` 即生效。

### 4.3 前缀索引：长字符串的省内存方案

```sql
-- 比如 knowledge 库里长 URL/长文本列不适合整列建索引
ALTER TABLE doc ADD KEY idx_url_prefix (url(32));
-- 选择性校验（接近 1 才值得建）：
SELECT COUNT(DISTINCT LEFT(url,32)) / COUNT(*) FROM doc;
```

**限制**：前缀索引**不能覆盖索引、不能排序利用**——它只存前缀。定长哈希列（本项目向量缓存用 SHA-256 定长 key 是同思想，见 `VectorCacheStore.java:101-103`）是更强的替代：完整等值 + 可覆盖。

---

## 五、深分页优化

> [01-MySQL核心知识 §7.2](../03-数据库与ORM/01-MySQL核心知识.md) 已给游标分页骨架；这里补全两类方案与取舍。

```sql
-- 慢：LIMIT 100000, 20 = 扫描并丢弃前 10 万行（rows≈100020）
SELECT * FROM orders WHERE user_id = 1001 ORDER BY create_time DESC LIMIT 100000, 20;

-- 方案A 游标/滚动分页（记住上一页末尾的位置，天然适合"下一页"式翻页）
SELECT * FROM orders WHERE user_id = 1001 AND create_time < '2026-08-01 10:00:00'
ORDER BY create_time DESC LIMIT 20;

-- 方案B 延迟关联（必须支持"跳页"时）
SELECT o.* FROM orders o
JOIN (SELECT id FROM orders WHERE user_id = 1001
      ORDER BY create_time DESC LIMIT 100000, 20) t ON o.id = t.id;
-- 内层只扫覆盖索引（不回表取大字段），外层用主键精确取 20 行
```

| 方案 | 适用 | 限制 |
|---|---|---|
| A 游标 | 信息流/AI 客服"查最近订单"类（`OrderServiceImpl.listOrders` 就是这种形态） | 不能跳页；排序键需唯一化（create_time 加 id 做次序 tie-break） |
| B 延迟关联 | 后台管理要跳页 | 深页仍要扫索引，只是不回表；1e7 级数据要配合归档 |

---

## 六、join 优化

| 原则 | 说明 |
|---|---|
| **小表驱动大表** | 驱动表 = join 里先扫描的表（`EXPLAIN` 第一行）。给**被驱动表**的关联列建索引，`eq_ref/ref` 优于 `ALL` |
| 关联列类型一致 | §三场景 7：隐式转换让被驱动表索引全废 |
| `STRAIGHT_JOIN` 应急 | 优化器选错驱动表时人工指定（先 EXPLAIN 验证再上） |
| BNL 信号 | `Using join buffer (Block Nested Loop)` = 没走索引，加大 join_buffer_size 只是止痛不是治病 |
| 拆解复杂 join | 3+ 表 join 先 `EXPLAIN` 看行数乘积；行数大的先过滤再 join（先缩小驱动表） |

**本项目对照**：order_item 按 `idx_order_no`（`order-init.sql:59`）与 orders 关联是标准形态；分库分表后的 user 库（[03-ShardingSphere用户表分库分表实战](../03-数据库与ORM/03-ShardingSphere用户表分库分表实战.md)）**跨片 join 受限**，应用层组装（两次查询内存关联）是本项目采取的方向，join 优化主要针对单片内的 orders/order_item。

### join 内部算法速览（EXPLAIN 看不到，但决定上限）

| 算法 | 触发条件 | 复杂度 | 优化含义 |
|---|---|---|---|
| Index Nested-Loop Join | 被驱动表关联列有索引 | 驱动行数 × 索引查找 O(log n) | 理想形态，`eq_ref/ref` |
| Simple NLJ | 无索引 | 驱动行数 × 被驱动全扫 = O(n×m) | ❌ 必须建索引 |
| **Block Nested-Loop (BNL)** | 无索引且 join buffer 缓存被驱动表分块 | O(n×m/buffer) | `Using join buffer` 信号；治本靠索引 |
| Hash Join（MySQL 8.0.18+） | 无索引时优化器自动选 | O(n+m) + 建哈希表 | 大表无索引 join 的兜底；小结果集更优 |

**实务结论**：8.0 的 Hash Join 缓解了"没索引就灾难"，但建索引依然是首选——哈希表构建本身要内存与 CPU，且驱动表大时依然吃力。看到 `Using join buffer` 先确认是 BNL 还是 Hash Join（`EXPLAIN FORMAT=TREE` 更直观），再决定加索引还是接受。

---

## 七、项目现场：orders 表索引设计与慢日志

### 7.1 真实 DDL 与它的查询一一对应

```sql
-- deploy/mysql/order-init.sql:41-45（节选）
PRIMARY KEY (`id`),
UNIQUE KEY `uk_order_no` (`order_no`),      -- → 按单号查单（OrderServiceImpl.java:197-200 等值 + userId 越权校验）
KEY `idx_user_id` (`user_id`),              -- → listOrders：我的订单列表（按 userId 等值 + create_time 倒序）
KEY `idx_status` (`status`),                -- → 状态维度扫描（超时取消/对账）
KEY `idx_expire_time` (`expire_time`)       -- → XXL-Job/MQ 兜底的超时扫描（range），并发语义见 05-锁 §3.3
```

**可做的调优演练**：`listOrders` 是 `WHERE user_id=? ORDER BY create_time DESC`——EXPLAIN 会看到 `ref + Using filesort`（order by 列不在索引里）。把它改成联合索引 `(user_id, create_time)` 即可消除 filesort：这正是"看 Extra 反推索引"的标准练习。

### 7.2 慢查询日志已开启（现成的调优入口）

```ini
# deploy/mysql/mysql.cnf:19-21（真实配置，随 mysql 容器生效）
slow-query-log=1
slow-query-log-file=/var/log/mysql/slow.log
long-query-time=2
```

```bash
# 现场取慢 SQL → EXPLAIN → 改 → 复验的闭环命令
docker exec aics-mysql mysql -uroot -proot -e "SHOW VARIABLES LIKE 'long_query_time';"
docker exec aics-mysql tail -20 /var/log/mysql/slow.log
# 在 mysql 客户端里 EXPLAIN 慢日志里的语句，按 §一固定动作处理
```

> 慢日志阈值 2s 偏宽松，压测期建议临时降到 0.5s 抓更多样本；`long_query_time` 与接口 SLA 对齐才有意义（[08 §四](./08-性能问题定位方法论.md)）。

---

## 八、面试高频问答

**Q1：EXPLAIN 你先看哪几个字段？**
A：按序四件事：`type` 是否 ALL（访问方式）；`rows` 与结果集的差量（扫描效率）；`key` 是否 NULL（索引是否真的用了）；`Extra` 的 `Using filesort/temporary`（额外排序与临时表）。四件看完 80% 的问题已定位，`key_len/ref/filtered` 用于细看联合索引利用率和过滤比例。

**Q2：哪些情况索引会失效？**
A：八类——索引列套函数或运算；隐式类型转换（列 varchar 传数字等）；前导 `%` 模糊；OR 两侧有列无索引；联合索引不满足最左前缀；`!=`/`NOT IN`/`IS NOT NULL`（视选择性）；隐式字符集/排序规则不一致；优化器成本估算判定全表更快。共同本质两条：索引列被"加工"后无法走有序树，或走索引的成本估算反而更高。

**Q3：什么是覆盖索引？为什么能提速？**
A：查询所需的所有列都包含在某个索引里，`Extra` 显示 `Using index`，引擎无需按主键回聚簇索引取行。提速原理：省掉每行一次的回表随机 IO，把随机读变成索引上的顺序读。代价是写入维护更多索引副本，只给高频查询做覆盖。

**Q4：索引下推（ICP）是什么？**
A：MySQL 5.6+ 特性：联合索引中"范围列之后"的条件可以在**存储引擎层**先过滤再回表，`Extra` 显示 `Using index condition`。收益是回表次数从"前导列命中数"降到"全条件命中数"。本项目 `(user_id, status)` 这类组合里 status 过滤就能下推。

**Q5：深分页为什么慢，怎么优化？**
A：`LIMIT 100000,20` 要定位到第 100020 行并丢弃前 10 万行，offset 越大扫描越多。优化：①游标分页——记住上页末尾位置用 `WHERE create_time < 上页末尾` 接着取，天然适合流式翻页；②延迟关联——内层子查询在覆盖索引上取主键、外层按主键精确回表，把回表从 10 万次降到 20 次；③数据量大配合归档表。

**Q6：join 怎么优化？什么是小表驱动大表？**
A：核心是让"被驱动表"的关联列有索引（理想 `eq_ref`），驱动表选过滤后行数小的。流程：EXPLAIN 看两表顺序与被驱动表 type；被驱动表是 ALL 就加索引；看到 `Block Nested Loop` 说明没走索引。类型/字符集一致防隐式转换失效；超过 3 表的 join 考虑先过滤再关联或应用层组装。

**Q7：慢查询日志配了但没抓到东西，说明没有慢 SQL 吗？**
A：不能。`long_query_time` 阈值内的"次慢 SQL"在高并发下累积开销同样可观（QPS×耗时）；且 `log_queries_not_using_indexes` 类开关、执行计划突变的语句也可能漏网。补充手段：`SHOW FULL PROCESSLIST` 实时抓、Performance Schema 的 statements 汇总、压测期临时调低阈值。

**Q8：给 orders 表的 `WHERE user_id=? ORDER BY create_time DESC` 查询，你会怎么建索引？**
A：联合索引 `(user_id, create_time)`：user_id 等值定位后，索引内数据天然按 create_time 有序，order by 直接走索引序消除 `Using filesort`；若还要取非索引列，末尾按需追加覆盖列（如 `order_no, status`）。注意与现有单列 `idx_user_id` 的冗余——新联合索引建立后单列索引可评估删除，减少写放大。

---

## 九、动手练习

1. 在 aics_order 库对 `listOrders` 的形态建一个 10 万行测试数据，跑 `EXPLAIN SELECT * FROM orders WHERE user_id=1 ORDER BY create_time DESC LIMIT 20`，确认 `Using filesort`；然后建 `(user_id, create_time)` 联合索引复验消失。
2. 对 orders 表验证 §三的 8 个失效场景：每个写一对正反 SQL，截图对比 type/key/Extra 差异，整理成自己的失效速查卡。
3. 用延迟关联改写 `LIMIT 100000,20`，对比前后 `rows` 与真实耗时（10 万数据 + 慢日志阈值 0.5s）。
4. 设计覆盖索引：为"我的订单列表页"（order_no、status、total_amount、create_time）建联合索引并验证 `Using index`，评估该索引对下单写入的影响，写 3 行取舍结论。
5. 把 `deploy/mysql/mysql.cnf:21` 的 `long-query-time` 临时改为 0.5 重启容器，压测 `sse-chat.js` + 下单链路，抓出 3 条新慢 SQL 并按 §一固定动作各给出一条优化建议。

---

> 上一篇：[04-内存泄漏排查实战](./04-内存泄漏排查实战.md) ｜ 下一篇：[06-接口性能优化实战](./06-接口性能优化实战.md)
