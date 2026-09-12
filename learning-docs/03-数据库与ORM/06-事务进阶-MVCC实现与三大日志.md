# 06-事务进阶：MVCC 实现与三大日志

> [01-MySQL核心知识](01-MySQL核心知识.md) 给了隔离级别表，但没回答表背后的"怎么做到的"：RR 凭什么不幻读？扣库存时另一边为什么能读到旧值？库突然断电，已提交的订单为什么没丢？
> 本篇补齐这三问：**MVCC（undo 版本链 + ReadView）+ 三大日志（redo/undo/binlog）+ 两阶段提交**。
> 锚定文件：`deploy/mysql/mysql.cnf`（`innodb-flush-log-at-trx-commit=1`）、`deploy/mysql/seata-undo-log.sql`、`deploy/mysql/master.cnf`。
> 前置阅读：[01 §五 事务](01-MySQL核心知识.md)、[05-MySQL锁机制与主从复制读写分离](05-MySQL锁机制与主从复制读写分离.md)（本篇只讲"读不加锁"的并发，加锁并发在 05）。

---

## 一、为什么隔离级别表不够用

`orders` 表的支付回调场景：用户手点 + 渠道重试，两个事务并发改同一订单；同时客服后台在查这张订单。三个问题：

| 问题 | 隔离级别表能回答吗 | 真正的答案在哪 |
|---|---|---|
| 客服查询会不会阻塞支付回调的写？ | 不能 | MVCC：普通 SELECT 读快照，不加锁（§二、三） |
| 回调事务改了一半宕机，会不会出现"扣了库存没生成订单"？ | 不能 | redo/undo 与崩溃恢复（§五） |
| 主从之间凭什么复制？ | 不能 | binlog（§四），复制原理在 [05 §五](05-MySQL锁机制与主从复制读写分离.md) |

记忆框架：**InnoDB 处理"读写冲突"靠 MVCC（多版本），处理"写写冲突"靠锁（05 篇）。** 两套机制别混着答。

## 二、undo log 与版本链

每行记录有两个隐藏列：

```
id | name | balance | DB_TRX_ID(最近修改它的事务) | DB_ROLL_PTR(指向上一个版本)
```

事务改一行时，InnoDB 先把**旧值**抄进 undo log，再把新值写进主记录，`DB_ROLL_PTR` 指向 undo 里的旧版本。旧版本自己也有 `DB_ROLL_PTR`，于是串成一条**版本链**：

```
最新值(trx_id=300) → undo: v2(trx_id=200) → undo: v1(trx_id=100) → NULL
```

undo log 一职两用：**回滚**（按链反向恢复）和 **MVCC**（沿链找可见版本）。所以它写在 `ROLLBACK SEGMENT` 里，随事务结束择机清理（不能马上删——还有别的读事务要用）。

> ⚠️ 高频混淆：`deploy/mysql/seata-undo-log.sql` 里那张 `undo_log` **表是 Seata AT 模式的业务表**（存 SQL 前后镜像做反向补偿），和 InnoDB 引擎内部的 undo log **只是同名，毫无关系**。机制见 [02-Spring微服务/06-Seata分布式事务AT模式](../02-Spring微服务/06-Seata分布式事务AT模式.md)。

## 三、ReadView：可见性怎么判定

事务执行**快照读**（普通 `SELECT`）的瞬间，拍一张"当时活跃事务名单"的快照，就是 ReadView：

| 字段 | 含义 |
|---|---|
| `m_ids` | 生成时刻所有**活跃**（已开始未提交）事务 ID 集合 |
| `min_trx_id` | 活跃名单里最小的 |
| `max_trx_id` | 下一个将分配的事务 ID（不是最大活跃 ID！） |
| `creator_trx_id` | 自己 |

拿版本链上某个版本的 `trx_id` 去判定：

```
trx_id == creator            → 自己改的，可见
trx_id < min_trx_id          → 生成快照前已提交，可见
trx_id >= max_trx_id         → 快照之后才开启的事务，不可见
min ≤ trx_id < max：
    在 m_ids 里（当时还没提交）→ 不可见，沿 roll_ptr 找上一版本重判
    不在 m_ids 里（已提交）   → 可见
```

**手算例**（面试常考）：事务 A（trx=200）与 B（trx=300）先后开启，B 未提交时把 `orders.status` 从 `PENDING_PAY` 改成 `PAID`；A 开事务后第一次 SELECT，此时生成 ReadView：`m_ids={200,300}`（假设 A 自己 trx=100 已在名单内则剔除自己）。

- A 读到该行：最新版本 `trx_id=300` 在 `m_ids` → 不可见 → 沿链回退到 `trx_id=100` 的旧版本 `PENDING_PAY` → **A 读到旧值，不被 B 的未提交修改干扰**。这就是 RR 下"可重复读"的实现。
- **RC 与 RR 的唯一实现差异**：RC **每条** SELECT 都重新生成 ReadView（所以能看到别人新提交的 → 不可重复读）；RR 只在**第一次**快照读时生成、整个事务复用（所以全程同一视野）。一张表记住：**RC 频繁拍照，RR 只拍一张**。

RR 下"快照读不幻读"靠上面这套；"当前读不幻读"靠间隙锁/临键锁（`SELECT ... FOR UPDATE`，见 [05 §二](05-MySQL锁机制与主从复制读写分离.md)）。**快照读 vs 当前读**是 MVCC 篇的黄金切入点：普通 SELECT 走 MVCC；`UPDATE/DELETE/INSERT`、`FOR UPDATE`、`LOCK IN SHARE MODE` 走当前读，必须读最新已提交版本并加锁。

## 四、三大日志分工

| | redo log | undo log | binlog |
|---|---|---|---|
| 层 | InnoDB 引擎 | InnoDB 引擎 | Server 层（所有引擎共用） |
| 内容 | **物理**：某页某偏移"改成什么" | **逻辑**：旧值（反向操作） | **逻辑**：语句/行变更（本项目 ROW 格式记行） |
| 作用 | 崩溃恢复：保证**已提交不丢** | 回滚 + MVCC | 复制 + 归档 + CDC（Canal） |
| 写法 | 循环写（ib_logfile，写满checkpoint） | 随表空间，段式 | 追加写（`mysql-bin.00000N`） |
| 项目参数 | `innodb-log-file-size=64M` | 无显式参数 | `binlog-format=ROW`、`expire-logs-days=7` |

一条 UPDATE 的完整落盘顺序（不用死记，理解"为什么是这个顺序"）：

```
1. Buffer Pool 改内存页（脏页）     ——最快，先干活
2. 写 undo log                     ——留后路
3. 写 redo log buffer（prepare 状态）
4. 写 binlog（Server 层）并 fsync
5. redo log 置为 commit 状态       ——两阶段提交完成
```

**为什么必须两阶段提交**（redo 与 binlog 之间）：两份日志属于两个层，做不到一个事务里原子写。若先写 redo 后写 binlog，宕机时主库恢复出了这行、从库没有 → 主从不一致；反之从库多了这行。用 redo 的 prepare/commit 两态 + binlog 作"裁判"：恢复时 redo 处于 prepare，就去看 binlog——binlog 完整则提交（可复制），不完整则回滚。**这是"redo 保 crash-safe、binlog 保复制"协同的根基**，也是 05 篇主从复制能成立的前提。

## 五、刷盘与本项目配置逐行解读

`deploy/mysql/mysql.cnf` 与本篇相关的三行：

```ini
innodb-flush-log-at-trx-commit=1   # 每次 COMMIT 都把 redo fsync 落盘 → 双1 之一
innodb-log-file-size=64M           # redo 文件太小：高写入时 checkpoint 频繁刷脏页
innodb-buffer-pool-size=256M       # 单机学习环境够用；生产按"物理内存的 50%~70%"起步
```

| 参数取值 | 丢数据窗口 | 性能 | 适用 |
|---|---|---|---|
| `=1`（本项目） | 不丢已提交事务 | 每事务一次 fsync，最慢 | 支付/订单，**默认必选** |
| `=2` | OS 崩溃才丢 ≤1s | 提交写 OS page cache | 可容忍秒级丢失的日志类 |
| `=0` | MySQL 崩溃丢 ≤1s | 最快 | 不建议业务库使用 |

配套 `sync_binlog=1`（本项目未显式写，MySQL 8.0 默认即 1）合称**双 1**：`trx_commit=1` 管 redo，`sync_binlog=1` 管 binlog。高写入场景的优化方向不是改掉双 1，而是**组提交**（多个事务的 fsync 合并成一次）——这也是 05 篇读写分离里"为什么从库能追上主库"的原因之一。

`expire-logs-days=7` 意味着 binlog 只留 7 天：**从库断追超过 7 天就只能重建，备份恢复的可用窗口也只有 7 天**——这个约束直接决定了 [09-备份恢复与数据安全](09-备份恢复与数据安全.md) 的备份频率下限。

## 六、崩溃恢复流程（把三块拼起来）

MySQL 重启后：

1. 扫 redo log，重放**所有**已 fsync 的物理变更（包括还没提交事务的）→ 脏页状态恢复到崩溃前；
2. 对 redo 处于 prepare 的事务，查 binlog：完整 → 提交；不完整 → 用 undo log 回滚；
3. 长事务的大回滚可能拖慢启动——**别开着超大事务不管**（呼应 05 §四死锁排查里的"长事务"观察项）。

一句话总结：**redo 负责"说好要做的一定做到"，undo 负责"做了没说好的撤回来"，binlog 负责"让从库和备份也做到"**。

## 七、动手练习

1. `SHOW ENGINE INNODB STATUS\G` 找到 LOG 段，记录 Log sequence number 与 Last checkpoint，写一条大 UPDATE 后再看差值（未 checkpoint 的 redo 量）。
2. 开两个 mysql 客户端模拟 §三手算例：B 不提交，A 连查两次（RR 下结果相同）；再 `SET SESSION transaction_isolation='READ-COMMITTED'` 重试（第二次能读到 B 提交的新值）。
3. `SHOW BINARY LOGS;` 对照 `expire-logs-days=7` 估算当前保留的 binlog 总量。

## 八、面试要点

- "RR 怎么实现的？" → MVCC（版本链 + ReadView）防快照读幻读 + 临键锁防当前读幻读，两层分开说。
- "RC 和 RR 实现上差在哪？" → 只差 ReadView 生成时机（每语句一次 vs 事务首次一次）。
- "redo 和 binlog 有什么区别？为什么都要？" → 层级（引擎/Server）、内容（物理/逻辑）、用途（crash-safe/复制）三维对比 + 两阶段提交。
- "双 1 是什么？改成 2 会怎样？" → 丢秒级数据换吞吐；业务库（尤其含支付）不动它。
- "Seata 的 undo_log 和 InnoDB 的 undo log 是一回事吗？" → 不是，一张业务表 vs 引擎机制；本篇 §二。

## 学习检查清单

- [ ] 能画出一条记录的版本链并标注 DB_TRX_ID / DB_ROLL_PTR
- [ ] 能用 m_ids/min/max 手算一个版本是否可见
- [ ] 能说出 RC/RR 在 ReadView 上的唯一差异
- [ ] 能默写 UPDATE 的 5 步落盘顺序并解释两阶段提交为什么必须存在
- [ ] 知道本项目 `mysql.cnf` 三行 redo 相关参数的含义与取舍
- [ ] 知道 binlog 只留 7 天对备份策略意味着什么（→ 09 篇）
