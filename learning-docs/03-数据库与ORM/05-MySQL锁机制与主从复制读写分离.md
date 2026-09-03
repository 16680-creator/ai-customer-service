# 05-MySQL 锁机制与主从复制读写分离

> 2026-09 落地记录：此前 [03-数据库与ORM] 只有索引/事务/MVCC 的基础篇（01），锁机制与主从复制完全空白；而项目里已经部署了 1 主 2 从的 MySQL 拓扑（`deploy/docker-compose/docker-compose-master-slave.yml`），应用侧却**没有**读写分离配置——本篇补齐这两块知识，并把"部署已就绪、应用未接入"的差距写成升级路线。
> 前置阅读：[01-MySQL核心知识](01-MySQL核心知识.md)（索引与事务隔离级别）。

---

## 一、为什么这个项目尤其需要懂锁

本项目的高并发写点都撞在"同一行"上：

- **支付回调**：渠道重试 + 用户手点，两个回调同时打 `pay_transaction` 同一订单的行；
- **库存扣减**（ai-cs-product，Seata 分支事务）：热门商品所有人扣同一行；
- **订单超时取消**（ai-cs-order 定时扫描）：扫描任务与支付回调可能同时改同一订单。

不懂锁，这些问题就只能靠"碰运气没出事"；懂了锁，才能说清每个方案到底挡住了什么。

## 二、锁体系全景

| 层级 | 锁 | 说明 | 本项目相关场景 |
|---|---|---|---|
| 全局 | FTWRL | 备份等场景，业务代码禁用 | 无 |
| 表级 | 表锁 / 元数据锁（MDL） | DDL 时自动加 MDL；长事务会阻塞 DDL | 发版改表时要留意 |
| 表级 | 意向锁（IS/IX） | 行锁前先在表上打"标记"，让表锁判断变快 | 自动，无需干预 |
| 行级 | 记录锁（Record Lock） | 锁索引记录本身 | `UPDATE orders SET status=... WHERE order_no=...` |
| 行级 | 间隙锁（Gap Lock） | 锁索引记录之间的**空隙**，防插入，RR 隔离级别防幻读 | 唯一性与范围查询 |
| 行级 | 临键锁（Next-Key Lock） | 记录锁 + 间隙锁的组合，InnoDB RR 的默认加锁单位 | `SELECT ... FOR UPDATE` 范围条件 |

记忆框架：**InnoDB 是"索引上的锁"**——行锁锁的是索引项，`WHERE` 条件走不到索引就升级为扫全表加锁（相当于锁全表），这是"行锁变表锁"事故的根源。

## 三、本项目中的锁场景逐个看

### 3.1 唯一索引：并发插入的仲裁者

`deploy/mysql/order-init.sql` 的 `orders` 表与支付流水表都靠唯一键兜底：

```sql
-- orders
UNIQUE KEY uk_order_no (order_no),
KEY idx_user_id (user_id),
KEY idx_status (status),
KEY idx_expire_time (expire_time)
-- pay_transaction（PaySchemaInitializer 应用级建表，DDL 同款约束）
UNIQUE KEY uk_order_no (order_no)
```

支付回调并发场景：两个回调同时把 PENDING 流水改为 SUCCESS。应用层状态检查挡住绝大多数，但"检查通过 → 写入"之间存在微小并发窗口；此时第二个事务插入/更新撞 `uk_order_no` 唯一键冲突而失败——**唯一索引本质是数据库帮你做的一把"提交时刻的锁"**。对应代码见 [02-Spring微服务/13-支付渠道集成与回调一致性](../02-Spring微服务/13-支付渠道集成与回调一致性.md) 第四节。

### 3.2 状态条件更新：用 UPDATE 的行锁做乐观闸门

订单取消/确认类操作的惯用法（`OrderServiceImpl` 超时取消、`PAID 回调幂等`）：

```sql
UPDATE orders SET status = 'CANCELLED'
WHERE order_no = ? AND status = 'PENDING_PAY';
```

这条 UPDATE 在 `order_no` 索引记录上拿**排他行锁**，且 `AND status=...` 让它只影响"还处于待支付"的行。两个并发执行者只有一个 `affected rows = 1`，另一个为 0——不需要显式 `SELECT FOR UPDATE` 就完成了互斥。这是"状态机 + 行锁"的组合拳（状态机语义见 [02-Spring微服务/12-订单状态机治理](../02-Spring微服务/12-订单状态机治理.md)）。

### 3.3 超时扫描与 idx_expire_time

`OrderTimeoutScanJob` 扫描过期订单时，`WHERE status='PENDING_PAY' AND expire_time < NOW()` 命中 `idx_expire_time`，锁范围收敛在"过期未支付"的索引区间；如果没有这个索引，RR 隔离级别下范围扫描的临键锁会大面积扩散。**索引设计直接决定锁的范围**——这是"为什么建 idx_expire_time"的真正答案。

## 四、死锁：怎么发生、怎么排查

两个事务互相持有对方需要的行锁：

```text
事务A：UPDATE orders SET ... WHERE order_no='O1';   -- 拿到 O1 行锁
事务B：UPDATE orders SET ... WHERE order_no='O2';   -- 拿到 O2 行锁
事务A：UPDATE orders SET ... WHERE order_no='O2';   -- 等 B
事务B：UPDATE orders SET ... WHERE order_no='O1';   -- 等 A → 死锁
```

InnoDB 会自动检测并回滚代价小的一方（报 `Deadlock found`），不会永久卡死。排查手段：

```sql
SHOW ENGINE INNODB STATUS;   -- LATEST DETECTED DEADLOCK 段：两个事务各持/各等哪个锁
SELECT * FROM performance_schema.data_locks;       -- 当前锁持有情况（8.0）
SELECT * FROM performance_schema.data_lock_waits;  -- 当前锁等待
```

避免原则（本项目代码都遵守）：

1. **多行更新按固定顺序**（如都按 order_no 升序）；
2. **事务尽量短**——回调链路里"验签 + 入账"后立刻提交，重活异步化；
3. 给并发更新走的 WHERE 条件建好索引（锁范围小，撞锁概率小）。

## 五、主从复制原理

```text
主库                         从库
┌────────────┐   binlog    ┌─────────┐   relay log   ┌─────────┐
│ 事务提交 →  │ ──────────→ │ IO Thread│ ────────────→ │SQL Thread│ → 重放写入
│ 写 binlog  │  dump 线程   │ 拉取binlog│              │ 重放事件  │
└────────────┘             └─────────┘               └─────────┘
```

- **binlog 三种格式**：`STATEMENT`（记 SQL，可能主从不一致）、`ROW`（记行变更，最安全、日志大——**本项目 Canal CDC 就依赖 ROW/FULL**，见 [04-中间件/11-CanalCDC商品索引同步](../04-中间件/11-CanalCDC商品索引同步.md)）、`MIXED`。
- **复制方式**：异步（默认，主库不等从库，可能丢最后几条）、半同步（至少一个从库确认收到 binlog 才返回）、组复制（MGR）。
- **主从延迟**：SQL 线程单线程重放追不上写入速率时产生，表现为"刚写入的数据从库读不到"。

## 六、本项目 1 主 2 从部署实战

`deploy/docker-compose/docker-compose-master-slave.yml` 拓扑：

| 服务 | 端口 | 配置挂载 |
|---|---|---|
| `mysql-master` | 3306 | `deploy/mysql/master.cnf` + `deploy/mysql/init.sql`（初始化 `user_db,knowledge_db,chat_db`） |
| `mysql-slave1` | 3307→3306 | `deploy/mysql/slave.cnf`，依赖 master `condition: service_healthy` |
| `mysql-slave2` | 3308→3306 | 同 slave.cnf（目录里另有 `slave2.cnf` 但 compose 未引用） |

**诚实说（动手前必读）**：compose 只负责把三个 MySQL 拉起来并挂好配置，**复制账号与 `CHANGE REPLICATION TO` 不在其中**，主从关系需要手工建立。完整步骤：

```bash
# 1. 起容器
docker compose -f deploy/docker-compose/docker-compose-master-slave.yml up -d

# 2. 主库建复制账号
docker exec -it mysql-master mysql -uroot -p -e "
  CREATE USER 'repl'@'%' IDENTIFIED BY 'repl-pass';
  GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
  SHOW MASTER STATUS;"    -- 记下 File 与 Position

# 3. 两台从库分别指向主库
docker exec -it mysql-slave1 mysql -uroot -p -e "
  CHANGE REPLICATION SOURCE TO SOURCE_HOST='mysql-master',
    SOURCE_USER='repl', SOURCE_PASSWORD='repl-pass',
    SOURCE_LOG_FILE='<上一步File>', SOURCE_LOG_POS=<Position>;
  START REPLICA; SHOW REPLICA STATUS\G"   -- 看 Slave_IO/SQL_Running 是否均为 Yes

# 4. 验证：主库建表，从库查询
```

master.cnf / slave.cnf 的关键差异就在 `server-id`（必须全链路唯一）与 binlog 相关项。

## 七、读写分离：部署已就绪，应用未接入

**现状核实**：全仓 grep 无 ShardingSphere readwrite-splitting 配置；`ai-cs-user` 的 ShardingSphere-JDBC 只做了分库分表（2 库 × 4 表，见 [03-ShardingSphere用户表分库分表实战](03-ShardingSphere用户表分库分表实战.md)），**没有读写分离**。也就是说：2 台从库目前是"闲置的备份"。

接入路线（与既有 ShardingSphere 体系同栈，推荐）——在 ShardingSphere 配置中叠加 readwrite-splitting 规则：

```yaml
rules:
  - !READWRITE_SPLITTING
    dataSources:
      readwrite_ds:
        writeDataSourceName: write_ds
        readDataSourceNames:
          - read_ds_0    # mysql-slave1
          - read_ds_1    # mysql-slave2
        loadBalancerName: round_robin
```

接入前必须想清楚的三个坑：

1. **写后立读**：下单后立刻查订单，读走从库可能还没复制到 → 主键查/强一致读要强制走主库（ShardingSphere 的 Hint 或同连接内路由）。
2. **哪些读可以下沉**：列表页、报表、搜索源数据同步可以；支付状态查询这类一致性敏感读不要下沉。
3. **从库延迟监控**：`SHOW REPLICA STATUS` 的 `Seconds_Behind_Source` 应进 Prometheus 告警（配合 [07-运维部署/04-Prometheus可观测性](../07-运维部署/04-Prometheus可观测性.md)）。

## 八、动手练习

1. 双开两个 mysql 客户端会话，按第四节脚本人为制造一次死锁，从 `SHOW ENGINE INNODB STATUS` 找出"TRANSACTION (1)/(2)"各自持有的锁。
2. 对 `orders` 写一条**不带索引**的 UPDATE 并 EXPLAIN，观察 type=ALL，推演它在并发下"行锁变表锁"的后果。
3. 把第六节的主从复制真正建起来，然后人为 `STOP REPLICA SQL_THREAD` 制造延迟，在从库读主库刚写的数据，体会"写后立读"问题。

## 九、面试要点总结

> 本项目在订单/支付表上以唯一索引与状态条件更新实现并发互斥与幂等，靠 idx_expire_time 把超时扫描的锁范围收敛到索引区间；死锁靠"固定顺序 + 短事务 + 走索引"规避，用 SHOW ENGINE INNODB STATUS 排查。部署了 1 主 2 从 compose 拓扑（ROW 格式 binlog，兼做 Canal CDC 事件源），复制账号与 CHANGE REPLICATION 需手工建立；应用侧尚未接入读写分离，升级路线是 ShardingSphere readwrite-splitting + 写后立读强制主库 + 从库延迟监控。

```text
关键词：行锁=索引上的锁 · 临键锁=记录+间隙 · 唯一键=提交时刻的锁
状态条件UPDATE = 乐观闸门(affected rows 仲裁) · 死锁四件套=顺序/短事务/索引/SHOW ENGINE
复制 = binlog(ROW) → IO线程 → relay log → SQL线程 · 主从延迟 → 写后立读
```

## 学习检查清单

- [ ] 能说清共享锁/排他锁/意向锁/间隙锁各自解决什么
- [ ] 能指出本项目三个行锁热点场景对应的表与索引
- [ ] 独立完成 1 主 2 从复制搭建并验证延迟表现
- [ ] 能写出 ShardingSphere 读写分离规则并指出"写后立读"的强制主库方案
