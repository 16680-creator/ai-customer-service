# 05 - CQRS 与事件溯源

> **目标**：理解"读写分离"的进阶形态，以及"用事件记录状态"的取舍。
> **前提**：[02-DDD](02-DDD领域驱动设计.md) 的领域事件、[03-分布式一致性](03-分布式一致性理论.md)。
> **纪律**：CQRS 和事件溯源**可以独立使用**，不是必须一起上；多数系统只需要"轻量 CQRS"。

---

## 一、从读写分离到 CQRS

| 阶段 | 做法 | 解决的问题 |
|---|---|---|
| 读写分离 | 主库写、从库读 | 读压力 |
| 缓存 | Redis 缓存查询结果 | 热点读 |
| **CQRS** | **命令模型与查询模型分离**（可以是不同库/不同结构） | 读写模型冲突、查询复杂 |

CQRS（Command Query Responsibility Segregation）：**写用一套模型，读用另一套模型**。

```text
命令侧：校验 → 聚合 → 事件 → 写库
查询侧：读优化视图（宽表/ES/缓存）← 投影 ← 事件
```

---

## 二、CQRS 的三种形态

### 2.1 轻量 CQRS（多数项目够用）

- 同一个库；
- 写走领域模型，读走专门查询（DTO/视图/ES）；
- 不做事件溯源。

> 本项目：写走 `OrderService`，读走 `OrderVO` + 列表查询，本质就是轻量 CQRS。

### 2.2 读写分离 CQRS

- 写库 + 读库（主从）；
- 读库可以是不同存储（MySQL → ES）。

**代价**：同步延迟、最终一致。

### 2.3 完整 CQRS + 事件溯源

- 写侧只存事件，不存当前状态；
- 读侧由事件投影生成；
- 可重放、可审计、可回溯。

**代价**：复杂度高、最终一致、事件版本管理。

---

## 三、事件溯源（Event Sourcing）

### 3.1 核心思想

```text
不存"当前状态"，存"导致状态变化的事件序列"。
状态 = 事件重放的结果。
```

例：

```text
传统：orders 表 status=PAID, pay_time=...
ES：  OrderCreated(orderNo, items, amount)
      OrderPaid(orderNo, tradeNo, paidAt)
      OrderShipped(orderNo, logisticsNo)
```

### 3.2 事件存储

| 字段 | 说明 |
|---|---|
| `aggregate_id` | 聚合 ID（如 orderNo） |
| `seq` | 聚合内序号（乐观锁） |
| `event_type` | 事件类型 + 版本 |
| `payload` | 事件内容（JSON/Protobuf） |
| `created_at` | 发生时间 |

**写入规则**：同一聚合的事件按 seq 追加，`(aggregate_id, seq)` 唯一 → 并发冲突时重试。

### 3.3 重放与快照

- 重放：从事件 0 开始 apply，得到当前状态；
- 快照：每 N 个事件存一次状态，重放从快照开始（避免全量重放）。

### 3.4 事件版本与兼容

事件一旦写入**不可修改**。字段演进靠：

- 新增字段可空；
- 事件类型加版本（`OrderPaidV2`）；
- 反序列化时做兼容转换（upcasting）。

---

## 四、CQRS 与事件溯源的关系

| 组合 | 说明 | 适用 |
|---|---|---|
| 只有 CQRS | 读写模型分离，仍存当前状态 | 查询复杂、读多写多 |
| 只有 ES | 存事件，读时重放/投影 | 审计、账务、可回溯 |
| CQRS + ES | 写存事件，读靠投影 | 金融账务、协作编辑、复杂领域 |

> 面试常问"必须一起用吗？"——**不必须**，这是最常见的误区。

---

## 五、最终一致与读模型延迟

CQRS/ES 的读侧是异步投影，会出现"写完立刻查不到"。

解决方案：

1. **写后读主**（同一聚合短时间读命令侧）；
2. **返回版本号**，前端轮询/等待投影位点；
3. **同步投影**（牺牲性能换一致）；
4. **业务容忍**（列表页延迟 1s 可接受）。

> 要能回答"延迟多大、怎么兜底"，否则就是空谈最终一致。

---

## 六、适用与不适用

| 适合 | 不适合 |
|---|---|
| 审计要求高（账务、订单流水） | 简单 CRUD 后台 |
| 查询模型复杂（多维度报表） | 强一致要求、低延迟 |
| 需要回溯/重放（对账、纠纷） | 团队不熟悉事件建模 |
| 读写负载差异大 | 事件量巨大但价值低（日志类） |

---

## 七、技术选型

| 方案 | 说明 |
|---|---|
| **自建事件表**（MySQL） | 最简单，适合中小规模；注意归档 |
| **Kafka** | 事件流事实标准；需自己管 offset/重放 |
| **EventStoreDB** | 专用事件库；支持投影/订阅 |
| **Axon Framework** | Java 生态的 CQRS/ES 框架 |
| **Debezium + outbox** | 业务库变更 → 事件流（事务性发件箱） |

---

## 八、本项目的轻量实践

| 能力 | 现状 | 评价 |
|---|---|---|
| 领域事件 | `OrderPaidEvent` + `AFTER_COMMIT` | ✅ 事件驱动雏形 |
| 消息表 | 支付事务消息（RocketMQ） | ✅ 事务性消息 |
| 审计 | MongoDB 对话归档 | ✅ 只追加 |
| 读模型 | `OrderVO` 查询 | ⚠️ 同库，未分离 |
| 事件重放 | 无 | ❌ 可补 |

**可落地的最小改造**：

```text
1. 加 order_event 表（order_no, seq, type, payload, created_at）
2. 下单/支付/取消时，同一事务写事件
3. 用事件投影出 order_summary 读表（异步）
4. 对账/客服查询走读表
5. 出问题时用事件重放修复读表
```

> 这是"轻量 ES"，不需要引入新中间件，能讲清原理即可。

---

## 九、最小实现示例（Java 骨架）

### 9.1 事件表

```sql
CREATE TABLE order_event (
  order_no   VARCHAR(32) NOT NULL,
  seq        INT         NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload    JSON        NOT NULL,
  created_at DATETIME    NOT NULL,
  PRIMARY KEY (order_no, seq)
);
```

### 9.2 追加事件（乐观锁）

```java
@Transactional
public void append(String orderNo, List<DomainEvent> events) {
    int seq = eventMapper.maxSeq(orderNo);
    for (DomainEvent e : events) {
        try {
            eventMapper.insert(new OrderEvent(orderNo, ++seq, e.type(), toJson(e)));
        } catch (DuplicateKeyException dup) {
            throw new OptimisticLockException("并发修改，请重试");
        }
    }
}
```

### 9.3 重放

```java
public Order rebuild(String orderNo) {
    Order order = new Order();
    for (OrderEvent e : eventMapper.findByOrderNo(orderNo)) {
        order.apply(e);   // 每种事件一个 apply
    }
    return order;
}
```

### 9.4 投影

```java
@RocketMQMessageListener(topic = "order-event", consumerGroup = "order-projection")
public class OrderProjection implements RocketMQListener<OrderEvent> {
    public void onMessage(OrderEvent e) {
        if (projectionMapper.alreadyApplied(e.orderNo(), e.seq())) return; // 幂等
        projectionMapper.apply(e);
        projectionMapper.markApplied(e.orderNo(), e.seq());
    }
}
```

> 投影必须**幂等**（重复消费）且**按 seq 顺序**（或用版本号跳过旧事件）。

---

## 十、常见坑

| 坑 | 说明 | 解法 |
|---|---|---|
| 事件不可变却改了 payload | 历史重放会错 | 加版本 + upcasting |
| 投影不幂等 | 重复消费导致数据翻倍 | 唯一键 + 版本号 |
| 读模型延迟被忽略 | 用户"刚保存就看不到" | 写后读主/版本号等待 |
| 事件表无限增长 | 查询慢、备份大 | 归档 + 快照 |
| 用 ES 替代所有 CRUD | 过度设计 | 只对核心域用 |
| 事件里放敏感信息 | 泄露风险 | 脱敏/加密 |

---

## 十一、面试题

| 题 | 答法骨架 |
|---|---|
| CQRS 解决什么？ | 读写模型冲突；写用领域模型，读用优化视图 |
| 必须和事件溯源一起用吗？ | 不必须；两者可独立 |
| 事件溯源怎么查当前状态？ | 重放事件/投影；快照加速 |
| 读模型延迟怎么办？ | 写后读主、版本号等待、同步投影、业务容忍 |
| 事件版本怎么演进？ | 加字段可空、事件版本化、upcasting |
| 你们项目用了吗？ | 轻量：领域事件 + 事务消息；未做完整 ES |

---

## 附：与 DDD 的关系

DDD 的**领域事件**是 CQRS/ES 的原料；聚合负责产生事件，事件负责传播与重建。

> 一句话：**DDD 定义"发生什么"，CQRS/ES 决定"怎么存、怎么读"。**
