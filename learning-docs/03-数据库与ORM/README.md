# 数据库与 ORM（03 模块）

> 本专题是 `learning-docs` 的**第三个模块**，覆盖本项目全部持久化技术：MySQL 8.0（主力库，1 主 2 从 + 分库分表）、MyBatis-Plus（唯一 ORM）。
> 写法与全库一致：**每篇都锚定 `deploy/mysql/` 的真实建表脚本与真实配置**，不写"通用教程"；项目没落地的主题（xtrabackup、gh-ost 等）显式标注"工程未落地"并给目标态方案。

---

## 一、文档地图

| 篇 | 主题 | 一句话定位 | 难度 |
|---|---|---|---|
| [01-MySQL核心知识](01-MySQL核心知识.md) | 部署、表设计示例、索引基础、事务与隔离级别、慢 SQL 入门 | **新手第一篇**：EXPLAIN 入门与索引最左前缀在这里 | ★★ |
| [02-MyBatisPlus实战](02-MyBatisPlus实战.md) | Entity/Mapper/Wrapper/分页/自动填充/Service 封装 | ORM 日常开发全部姿势 | ★★ |
| [03-ShardingSphere用户表分库分表实战](03-ShardingSphere用户表分库分表实战.md) | 2 库 × 4 表分片、绑定表、踩坑记录、存量迁移 | 项目唯一真正落地的分片实战 | ★★★ |
| [04-分布式ID规范](04-分布式ID规范.md) | 雪花 ID 结构、MyBatis-Plus 接入、三条项目规范 | 分库分表的前置约束 | ★ |
| [05-MySQL锁机制与主从复制读写分离](05-MySQL锁机制与主从复制读写分离.md) | 锁体系全景、死锁排查、1 主 2 从部署、读写分离升级路线 | 并发正确性 + 复制拓扑 | ★★★ |
| [06-事务进阶-MVCC实现与三大日志](06-事务进阶-MVCC实现与三大日志.md) | undo 版本链 + ReadView 手算、redo/undo/binlog、两阶段提交、双 1 | **面试最高频**的原理篇，接住 01 隔离级别表背后的问题 | ★★★★ |
| [07-建表规范与字段类型选型](07-建表规范与字段类型选型.md) | 字段类型选型表、必备字段、快照冗余（反范式）、真实 DDL 规范审视 | 把 `deploy/mysql/*.sql` 14 个脚本的隐式规范显式化 | ★★ |
| [08-大表治理-冷热分离与数据归档](08-大表治理-冷热分离与数据归档.md) | 表增长模型、归档 vs 分区 vs 分库分表决策、Online DDL | 表长大之前先想好退路 | ★★★ |
| [09-备份恢复与数据安全](09-备份恢复与数据安全.md) | mysqldump/xtrabackup、binlog 点恢复、延时从库、从库备份的过滤缺口 | **冗余 ≠ 备份**：误删会同步到从库 | ★★★ |
| [10-数据库面试专练](10-数据库面试专练.md) | 高频题分组、追问链演练、考前速览卡 | 串联 01~09 的验收出口 | ★★★ |

## 二、与其他模块的分工边界（查重声明）

| 主题 | 在哪里讲 | 本模块讲不讲 |
|---|---|---|
| EXPLAIN 全列、索引失效八场景、ICP、**深分页**、join 优化 | [12-性能工程/05-SQL调优与执行计划](../12-性能工程/05-SQL调优与执行计划.md) | 不重复；01 只留入门，08/10 只做链接 |
| Seata AT/TCC、XID 传播、undo_log 表机制 | [02-Spring微服务/06](../02-Spring微服务/06-Seata分布式事务AT模式.md)、[07](../02-Spring微服务/07-服务调用统一与SeataXID传播.md) | 06 篇只辨析"Seata 的 undo_log 表 ≠ InnoDB 的 undo log"，机制不展开 |
| binlog 三种格式、Canal CDC 链路 | [04-中间件/11-CanalCDC商品索引同步](../04-中间件/11-CanalCDC商品索引同步.md) | 06/09 篇只讲 binlog 在两阶段提交/复制/恢复中的**角色**，格式细节链接过去 |
| Redis 缓存一致性、Redisson 分布式锁 | [04-中间件/01](../04-中间件/01-Redis缓存实战.md)、[07](../04-中间件/07-Redisson分布式锁.md) | 不讲 |
| MongoDB 对话审计归档的落地细节 | [04-中间件/10-MongoDB对话审计归档](../04-中间件/10-MongoDB对话审计归档.md) | 08 篇只讲 MySQL 侧"什么数据、什么时候、怎么搬" |
| 主从切换、仲裁、脑裂 | [13-稳定性工程/04-高可用架构模式](../13-稳定性工程/04-高可用架构模式.md) | 09 篇只讲备份与恢复，切换流程链接过去 |
| 国产数据库选型与迁移 | [18-银行金融开发/15-国产数据库选型与迁移](../18-银行金融开发/15-国产数据库选型与迁移.md) | 不讲 |
| 向量数据库（Chroma） | [05-AI集成/05-向量数据库](../05-AI集成/05-向量数据库/README.md) | 不讲 |

## 三、本项目的数据库资产清单

```
MySQL 8.0（docker-compose.yml，单节点）          ← 01 篇锚点
├── user_db          sys_role / sys_user_role
├── user_db_0/1      sys_user_0~3（ShardingSphere 2库×4表）← 03 篇锚点
├── knowledge_db     kb_category / kb_document / kb_tag
├── chat_db          chat_session / chat_message / chat_feedback
├── nacos_config     Nacos 持久化
├── ai_customer_service  cart_item / orders / order_item / coupon（order-init.sql）
└── product_db       库存与商品（Seata 分支事务参与方）

主从拓扑（deploy/docker-compose/docker-compose-master-slave.yml）← 05/09 篇锚点
├── master.cnf   server-id=1，binlog ROW + FULL
├── slave.cnf    server-id=2，relay-log，read-only=1，replicate-do-db 过滤
└── slave2.cnf   server-id=3

其他
├── seata-undo-log.sql   Seata AT 前后镜像表（order/product 两库各一张）
├── canal-init.sql       CDC 账号与位点（见 04-中间件/11）
└── mysql.cnf            双 1、慢日志 2s、utf8mb4（06 篇刷盘小节逐行解读）
```

## 四、学习路径

**新手（会 CRUD 就行）**：01 → 02 → 07，配合把 `docker-compose.yml` 里的 MySQL 跑起来，用 01 的练习建一张表插几行数据。

**进阶（要讲清"为什么"）**：05 → 06 → 09，这三篇回答面试三连："锁怎么加的？""MVCC 怎么实现的？""库被误删了怎么办？"。

**架构（要扛住增长）**：03 → 04 → 08，按"先分 ID、再分片、最后想归档"的顺序读——顺序反了会像很多项目一样先分库分表再后悔。

**验收**：[10-数据库面试专练](10-数据库面试专练.md) 的四层追问链全部能接住第二轮追问，即认为本模块过关。

## 五、更新日志

- 2026-09：第五批补全。此前模块只有 01~05 且无 README；本批补 README + 4 篇（06 事务进阶与三大日志、07 建表规范、08 大表治理、09 备份恢复）+ 1 篇面试专练（10），缺口分析见 [00-学习路线总览/05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md)。
