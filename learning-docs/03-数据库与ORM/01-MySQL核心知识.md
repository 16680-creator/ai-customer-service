# MySQL 核心知识

> 本项目使用 **MySQL 8.0** 作为主数据库，存储用户、订单、商品、知识库等业务数据。
> 对应项目文件：`docker-compose.yml`（MySQL 容器）、`deploy/mysql/init.sql`（建表脚本）

---

## 一、MySQL 在项目中的角色

```
┌─────────────────────────────────────────────────┐
│                  MySQL 8.0                        │
│                                                  │
│  数据库列表：                                     │
│  ├── nacos_config    ← Nacos 配置持久化           │
│  ├── aics_user       ← 用户、角色、权限           │
│  ├── aics_order      ← 订单、购物车、支付         │
│  ├── aics_product    ← 商品、分类、库存           │
│  ├── aics_knowledge  ← 知识库文档                 │
│  └── aics_message    ← 消息、会话记录             │
└─────────────────────────────────────────────────┘
```

---

## 二、Docker 部署 MySQL

```yaml
# docker-compose.yml 中的配置
mysql:
  image: mysql:8.0
  container_name: aics-mysql
  ports:
    - "3306:3306"
  environment:
    MYSQL_ROOT_PASSWORD: root       # root 密码
    TZ: Asia/Shanghai               # 时区
  volumes:
    - mysql-data:/var/lib/mysql     # 数据持久化
    - ./deploy/mysql/mysql.cnf:/etc/mysql/conf.d/mysql.cnf  # 自定义配置
    - ./deploy/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql  # 初始化脚本
  command: --default-authentication-plugin=mysql_native_password
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-proot"]
    interval: 10s
    timeout: 5s
    retries: 10
    start_period: 30s              # 给 MySQL 30 秒启动时间
```

**关键点**：
- `init.sql` 只在**第一次启动**（数据卷为空）时执行
- 如果改了 init.sql，需要删除数据卷重新创建：`docker volume rm ai-customer-service_mysql-data`

---

## 三、表设计（以订单服务为例）

### 3.1 购物车表

```sql
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `product_id`    BIGINT       NOT NULL COMMENT '商品ID',
    `product_name`  VARCHAR(200) NOT NULL COMMENT '商品名称（冗余，避免跨服务查询）',
    `product_price` DECIMAL(10,2) NOT NULL COMMENT '加入时价格',
    `quantity`      INT          NOT NULL DEFAULT 1 COMMENT '数量',
    `selected`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否选中',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),          -- 按用户查询
    INDEX `idx_product_id` (`product_id`)     -- 按商品查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';
```

### 3.2 订单表

```sql
CREATE TABLE IF NOT EXISTS `orders` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`       VARCHAR(64)   NOT NULL COMMENT '订单编号（唯一）',
    `user_id`        BIGINT        NOT NULL,
    `total_amount`   DECIMAL(10,2) NOT NULL COMMENT '订单总额',
    `pay_amount`     DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '0待付款 1已付款 2已发货 3已完成 4已取消',
    `pay_time`       DATETIME      NULL COMMENT '支付时间',
    `expire_time`    DATETIME      NOT NULL COMMENT '支付截止时间',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_order_no` (`order_no`),
    INDEX `idx_user_status` (`user_id`, `status`)  -- 联合索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

---

## 四、索引（性能核心）

### 4.1 索引是什么？

```
没有索引：查 user_id=100 的订单 → 全表扫描 100 万行 → 慢！
有索引：  查 user_id=100 的订单 → B+树定位 → 只读几行 → 快！
```

### 4.2 索引类型

| 类型 | 说明 | 示例 |
|------|------|------|
| PRIMARY KEY | 主键索引，唯一+非空 | `id` |
| UNIQUE | 唯一索引 | `order_no` |
| INDEX | 普通索引 | `idx_user_id` |
| 联合索引 | 多列组合 | `idx_user_status(user_id, status)` |

### 4.3 联合索引的最左前缀原则

```sql
-- 联合索引: idx_user_status(user_id, status)

-- ✅ 能用到索引
SELECT * FROM orders WHERE user_id = 100;
SELECT * FROM orders WHERE user_id = 100 AND status = 1;

-- ❌ 用不到索引（跳过了第一列）
SELECT * FROM orders WHERE status = 1;
```

### 4.4 EXPLAIN 分析 SQL

```sql
EXPLAIN SELECT * FROM orders WHERE user_id = 100 AND status = 1;
```

关注字段：
- `type`: ALL（全表扫描❌）→ index → range → ref → eq_ref → const（最好✅）
- `rows`: 预估扫描行数（越少越好）
- `Extra`: Using index（覆盖索引✅）、Using filesort（需要优化❌）

---

## 五、事务

### 5.1 什么是事务？

```java
// 下单操作：扣库存 + 创建订单 + 清购物车 → 必须同时成功或同时失败
@Transactional
public OrderVO createOrder(Long userId, List<Long> cartItemIds) {
    // 1. 扣减库存（product 服务）
    productClient.deductStock(productId, quantity);
    
    // 2. 创建订单
    orderMapper.insert(order);
    
    // 3. 清除购物车
    cartItemMapper.deleteBatchIds(cartItemIds);
    
    // 如果任何一步抛异常 → 全部回滚
}
```

### 5.2 隔离级别

| 级别 | 脏读 | 不可重复读 | 幻读 | 性能 |
|------|------|-----------|------|------|
| READ UNCOMMITTED | ✓ | ✓ | ✓ | 最高 |
| READ COMMITTED | ✗ | ✓ | ✓ | 高 |
| **REPEATABLE READ** | ✗ | ✗ | ✓ | 中（MySQL默认） |
| SERIALIZABLE | ✗ | ✗ | ✗ | 最低 |

MySQL 8.0 默认 **REPEATABLE READ**，通过 MVCC + 间隙锁解决大部分并发问题。

---

## 六、Spring Boot 连接 MySQL

```yaml
# application.yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/aics_order?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    hikari:                    # 连接池配置
      maximum-pool-size: 20   # 最大连接数
      minimum-idle: 5         # 最小空闲连接
      connection-timeout: 30000
      idle-timeout: 600000
```

---

## 七、慢 SQL 优化实战

### 7.1 开启慢查询日志

```ini
# deploy/mysql/mysql.cnf
[mysqld]
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 1          # 超过 1 秒记录
log_queries_not_using_indexes = 1  # 没用索引的也记录
```

### 7.2 常见优化手段

```sql
-- ❌ 慢：SELECT * 返回所有列
SELECT * FROM orders WHERE user_id = 100;

-- ✅ 快：只查需要的列
SELECT id, order_no, total_amount, status FROM orders WHERE user_id = 100;

-- ❌ 慢：对索引列使用函数
SELECT * FROM orders WHERE YEAR(create_time) = 2024;

-- ✅ 快：用范围查询
SELECT * FROM orders WHERE create_time >= '2024-01-01' AND create_time < '2025-01-01';

-- ❌ 慢：大偏移量分页
SELECT * FROM orders LIMIT 100000, 20;

-- ✅ 快：游标分页
SELECT * FROM orders WHERE id > 100000 ORDER BY id LIMIT 20;
```

---

## 八、动手练习

1. 连接 MySQL：`docker exec -it aics-mysql mysql -uroot -proot`
2. 查看数据库：`SHOW DATABASES;`
3. 查看表结构：`DESC aics_order.cart_item;`
4. 插入测试数据，用 `EXPLAIN` 分析查询
5. 创建一个没有索引的查询，对比加索引前后的 EXPLAIN 结果

---

## 学习检查清单

- [ ] 理解 InnoDB 引擎和 B+ 树索引
- [ ] 会设计表结构（主键、索引、字段类型）
- [ ] 理解联合索引的最左前缀原则
- [ ] 会用 EXPLAIN 分析 SQL 性能
- [ ] 理解事务 ACID 和隔离级别
- [ ] 理解连接池（HikariCP）的作用
- [ ] 知道常见的慢 SQL 优化手段

---

## 下一步

→ [02-MyBatisPlus实战](./02-MyBatisPlus实战.md)
