# 21-RabbitMQ 概念模型与 AMQP 协议：从零开始理解"只学不引"的消息第三极（认知拓展，工程明确不引入）

> **定位**：[00-学习路线总览/02-中间件补全计划 C 类](../00-学习路线总览/02-中间件补全开发计划.md) 明确决策：**RabbitMQ 不引入**（RocketMQ 栈已定，再引第三套 MQ 是负资产——这个决策本身是对的，本篇不推翻）。但社招 JD 里 RabbitMQ 出现率极高，且它的 **AMQP 路由模型、confirm/ack 可靠性机制**是理解所有 MQ 的最佳教具。本篇只建概念坐标系 + 与你已学的 RocketMQ/Kafka 对照，零工程改动。
> 前置阅读：[04-中间件/02-RocketMQ消息队列](02-RocketMQ消息队列.md)、[04-中间件/08-事务消息与死信](08-RocketMQ事务消息与死信队列.md)、[04-中间件/12-Kafka双栈](12-Kafka与RocketMQ双栈切换.md)（三套 MQ 放一起才看得清设计取舍）。

---

## ⚡ 30 秒速记卡

```text
① AMQP 核心模型：Producer → Exchange(路由器) → Binding(规则) → Queue → Consumer
   ——RocketMQ 的"路由"退化成 topic 直连；RabbitMQ 把路由做成了可编程层
② 四种 Exchange：direct(精确) / topic(通配) / fanout(广播) / headers(头匹配)
③ 可靠性三板斧：生产端 confirm / broker 端持久化+仲裁队列 / 消费端 ack+nack+死信(DLX)
④ 与 RocketMQ 差异关键词：RabbitMQ 无事务消息(用 confirm+本地表代替)、无原生延迟(死信/插件模拟)、
   吞吐万级~十万级 vs RocketMQ 十万级+；路由能力反而更强
⑤ 什么时候会选它：中小流量、复杂路由、多协议(STOMP/MQTT 插件)、运维轻——你 notify 的 STOMP 它原生支持
```

---

## 一、AMQP 模型：把"路由"从 topic 里拆出来

你熟悉的 RocketMQ 模型很直接：**Producer → topic → queue → Consumer**，路由基本等于"选 topic"（加个 tag 二次过滤）。
AMQP 在中间插了一层**可编程路由器**：

```text
Producer ──publish──► Exchange ──按 Binding 规则──► Queue(s) ──push──► Consumer
                         │
   direct:  routing key 精确匹配        "order.paid" → 只进订单队列
   topic:   通配符匹配（*.paid / order.#） "order.*" → 订单+退款都进
   fanout:  无脑广播到所有绑定队列       同一事件喂给"积分/通知/风控"三个系统
   headers: 按消息头匹配（少用）
```

**为什么值得学**：fanout+topic 这套组合正是"**一个业务事件、多个订阅方各取所需**"的标准解法。你项目里 `OrderPaidEvent` 发 MQ 通知、`knowledge-doc-sync` 同步索引——如果路由需求变复杂（同一事件按规则进不同队列），RabbitMQ 的模型就是参考答案（RocketMQ 里你会用多个 topic + 生产端判断来模拟）。

---

## 二、可靠性三板斧（对照你已经会的东西）

| 环节 | RabbitMQ 机制 | RocketMQ 对应（你已学） |
|---|---|---|
| 生产端不丢 | **publisher confirm**（broker 异步 ack）+ mandatory/备份交换器 | 同步 send + SendResult（[04-02](02-RocketMQ消息队列.md)） |
| broker 不丢 | 消息持久化 + 队列持久化 + **仲裁队列**（Raft 副本，3.x8+ 推荐替代镜像队列） | 刷盘 SYNC_FLUSH + 主从同步（DLedger） |
| 消费端不丢 | 手动 **ack**；`basicNack(requeue)` 重回队列；重试上限 → **DLX 死信交换器** | CONSUME 失败重投 16 次 → **%DLQ%**（[04-08](08-RocketMQ事务消息与死信队列.md)） |

> **关键差异：没有事务消息**。RocketMQ 的半消息+回查（pay 支付成功一致性，本仓 `PaySuccessTransactionListener` 在用）在 RabbitMQ 里**没有等价物**——通用做法是"本地消息表 + confirm 回调扫表补发"。面试问到"RabbitMQ 怎么做分布式事务"，标准答案就是这句 + 指出它根本不该承担这个职责。

### 两个常见"缺失能力"的模拟术（面试高频）

| 能力 | RabbitMQ 的模拟 | 本仓 RocketMQ 的原生答案 |
|---|---|---|
| 延迟消息 | TTL + 死信队列（过期转入 DLX）；或 delayed-message 插件 | **原生延迟级别**（order 超时关单在用，[04-12](12-Kafka与RocketMQ双栈切换.md) 记录了 Kafka 缺这能力的降级方案） |
| 优先级队列 | 队列 x-max-priority | 无原生（业务侧多队列模拟） |

---

## 三、三套 MQ 总对照（收口表）

| 维度 | RabbitMQ | RocketMQ（本仓主力） | Kafka（本仓第二栈） |
|---|---|---|---|
| 模型灵魂 | **可编程路由**（Exchange/Binding） | topic+queue+tag，简单直给 | 分区日志 + 消费组位移 |
| 吞吐量级 | 万 ~ 十万/s | 十万/s+ | **百万/s** |
| 延迟 | 低（推模式，µs~ms 级） | 低 | 低（批量攒批略高） |
| 事务消息 | ❌（本地表+confirm 替代） | ✅ 半消息+回查 | ❌（transaction 是跨分区原子写，语义不同——04-12 已辨析） |
| 延迟消息 | ❌（TTL+DLX/插件） | ✅ 原生级别 | ❌（04-12 降级调度） |
| 死信 | DLX（延迟触发型还能复用它） | %DLQ% 重投次数触发 | 应用侧 DLT（04-12 用 `DeadLetterPublishingRecoverer` 对齐） |
| 协议生态 | AMQP + STOMP/MQTT 插件 | 自协议（含 Remoting/gRPC 5.x） | 自协议（KRaft） |
| 选型一句话 | 中小流量+复杂路由+多协议 | 国内电商/金融业务消息首选 | 大数据管道/日志/流式 |

---

## 四、"只学不引"的边界与触发条件（照抄仓库纪律的写法）

**维持不引入的依据**：已有两套 MQ 栈（RocketMQ 主力 + Kafka 双栈验证抽象层），第三套只增加运维面与心智负担，无业务收益（[02-计划 C 类](../00-学习路线总览/02-中间件补全开发计划.md)）。

**将来重评的触发条件**（写下来防止反复横跳）：
1. 出现大量"同一事件按复杂规则分发到 N 类订阅方"的需求，且 RocketMQ 的 topic 模拟让生产端逻辑臃肿；
2. 接入需要 **MQTT/STOMP 多协议**的 IoT/移动端场景（RabbitMQ 插件原生支持——你 notify 的 STOMP 会是现成对照）。

---

## 五、动手实验（10 分钟，纯概念体验）

```bash
docker run -d --name rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
# 浏览器 http://localhost:15672（guest/guest）
# 控制台手动建：fanout 交换器 ex.aics + 两个队列 q1/q2 并绑定
# 再在 Exchanges 页面往 ex.aics 发一条消息 → 两个队列同时收到（理解 fanout 广播）
```

管理台另一个价值：**看消息的完整生命周期**（Ready/Unacked 状态、消费者限流 prefetch）——比 RocketMQ Console 更直观，适合建立"消息积压/未确认"的直觉。

---

## 六、面试要点总结

```text
关键词：
AMQP 路由层：Exchange(direct/topic/fanout/headers)+Binding —— 与 RocketMQ topic 模型的本质差异
可靠性三板斧 confirm/持久化+仲裁队列/ack+DLX · 无事务消息→本地消息表+confirm
延迟=TTL+DLX 或插件 · 吞吐低于 RocketMQ/Kafka · 仲裁队列(Raft)替代镜像队列
触发重评条件：复杂事件分发 / MQTT·STOMP 多协议
项目锚点：RocketMQ 全家（02/08/12）· pay 事务消息 · order 延迟关单 · notify STOMP · C 类决策原文
表态口径：项目统一 RocketMQ，RabbitMQ 只作概念坐标系——这句话本身就是加分项
```

## 学习检查清单

- [ ] 能画出 AMQP 五元模型并解释四种 Exchange 的区别
- [ ] 能把"可靠性三板斧"逐一映射到你已学的 RocketMQ 机制
- [ ] 能答"RabbitMQ 怎么做延迟/事务"并指出其非原生的本质
- [ ] 能背出"为什么本项目不引入"的决策与两条重评触发条件

## 下一步

- [22-Eureka 与 Consul 注册中心对照](22-Eureka与Consul注册中心对照.md)：T1 补充的最后一篇；
- [04-中间件/12-Kafka与RocketMQ双栈切换](12-Kafka与RocketMQ双栈切换.md)：回看"抽象层"如何让 MQ 可插拔。
