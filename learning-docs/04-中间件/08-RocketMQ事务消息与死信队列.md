# 08-RocketMQ 事务消息与死信队列

> 2026-08 落地记录：支付成功通知从「同步 Feign 回调」改造为「RocketMQ 事务消息」；
> notify 消费者显式重试次数，死信可观测。

## 一、事务消息解决什么问题

支付成功后要通知 order 确认订单。旧链路的问题：

```
渠道回调 → markSuccess(本地事务) → Feign confirmPay
                                    ↑ 网络抖动/order 重启 = 支付事件丢失
                                    （只能靠查单兜底补偿）
```

事务消息把「本地事务」与「消息必达」绑定：

```
① 发半消息（对消费者不可见）
② Broker 回调 executeLocalTransaction：执行本地事务（markSuccess）
   ├─ 成功 → COMMIT_MESSAGE（消息转为可消费）
   └─ 失败 → ROLLBACK_MESSAGE（半消息删除）
③ 本地事务未决（如应用宕机）→ Broker 定时回查 checkLocalTransaction
   └─ 以支付流水状态二次判定：SUCCESS→COMMIT / PENDING→UNKNOWN / CLOSED→ROLLBACK
```

**核心不变式**：`流水落库成功 ⇒ 消息必达 order`；order 侧 `confirmPay` 幂等，
构成「至少一次投递 + 幂等消费」的标准安全组合。

## 二、代码落点

```
ai-cs-common/.../mq/PaySuccessMessage.java      # 两端共享的消息契约
ai-cs-pay/.../mq/
├── PaySuccessMessageProducer.java              # sendMessageInTransaction(TOPIC, msg, payload)
└── PaySuccessTransactionListener.java          # executeLocalTransaction + checkLocalTransaction
ai-cs-pay/.../service/PayNotifyService.java     # aics.pay.tx-notify.enabled=true 走事务消息（默认）
ai-cs-order/.../listener/PaySuccessListener.java # 消费 → confirmPay（幂等）
```

**rocketmq-spring 2.3.0 API 变化（踩坑）**：
- `@RocketMQTransactionListener` 的 `txProducerGroup` 属性已移除，监听器默认绑定主 `RocketMQTemplate`；
- 发送签名变为 `sendMessageInTransaction(destination, message, arg)`，destination 即 topic；
- 回查参数类型是 `MessageExt`（继承自 `Message`），消息体 JSON 反序列化取回 payload。

## 三、死信队列（DLQ）

消费失败 → RocketMQ 按**递增间隔**重试 → 重试耗尽进入死信主题：

```
notify-topic 消费失败 ×3（maxReconsumeTimes=3）
  → %RETRY%notify-consumer-group（重试主题）
  → %DLQ%notify-consumer-group（死信主题）
```

- `OrderNotifyConsumer` 显式配置 `maxReconsumeTimes = 3`（教学用，快速进 DLQ；生产可按需调大）
- 死信需要人工介入：补数据后用后台工具重发，或修好消费逻辑让重放成功

## 四、验证方式

- 单测：`PaySuccessTransactionListenerTest`（6 用例）
  - executeLocalTransaction：成功 COMMIT / 异常 ROLLBACK
  - checkLocalTransaction：SUCCESS→COMMIT、PENDING→UNKNOW、CLOSED→ROLLBACK、缺失→ROLLBACK
- 集成冒烟：mock 支付回调 → pay 日志出现「事务消息本地事务执行成功」→ order 日志出现
  「收到支付成功事件」→ 订单状态变为 PAID

## 五、面试高频

1. **事务消息 vs 本地消息表**：事务消息由 Broker 半消息+回查实现，业务无感但依赖 RocketMQ；本地消息表用业务表存事件+定时投递，任何 MQ 都能做但侵入业务。
2. **回查为什么需要 UNKNOWN**：本地事务可能还在执行（慢查询/长事务），直接 COMMIT/ROLLBACK 都可能错判。
3. **消息消费幂等的常用手段**：状态机幂等（本项目 confirmPay）、唯一键幂等、去重表。
