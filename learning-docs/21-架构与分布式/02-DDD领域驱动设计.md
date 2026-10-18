# 02 - DDD 领域驱动设计

> **目标**：回答两个问题——"服务边界怎么划"和"业务逻辑放哪"。
> **前提**：[01 架构演进](01-架构演进与架构风格.md)。
> **纪律**：DDD 不是"多写几个类"，它先是**沟通方法**，再是代码结构。领域简单时不要硬套。

---

## 一、为什么需要 DDD

软件复杂度有两个来源：

| 来源 | 表现 | DDD 的应对 |
|---|---|---|
| **技术复杂度** | 高并发、分布式、存储 | 架构/中间件解决 |
| **业务复杂度** | 规则多、状态多、术语不一致 | **DDD 解决** |

典型症状：

- 产品和开发对同一个词理解不同（"订单取消"到底退不退款？）；
- 一个 Service 3000 行，改一处怕炸一片；
- 服务拆分靠拍脑袋，拆完发现两个服务天天互相调用。

> DDD 的核心价值：**用统一的领域语言，把业务边界划出来，把规则收进模型里。**

---

## 二、战略设计（划边界）

### 2.1 领域与子域

| 类型 | 定义 | 投入 | 本项目的例子 |
|---|---|---|---|
| **核心域** | 公司的竞争力所在 | 最强人力、自研 | 交易链路、AI 客服 |
| **支撑域** | 支撑核心但不产生差异 | 外包/采购/简单实现 | 通知、文件存储 |
| **通用域** | 行业通用能力 | 直接用现成 | 用户认证、权限、日志 |

> 判断口诀：**"这块做不好会不会丢客户？"** 会 → 核心域。

### 2.2 限界上下文（Bounded Context）

一个**边界明确的业务范围**，边界内术语唯一、模型独立。

```text
"商品"在商品上下文 = 价格/库存/SKU
"商品"在订单上下文 = 快照（下单时的名称/价格）
"商品"在搜索上下文 = 文档（分词/权重）
```

**同一个词在不同上下文含义不同，这是正常的**——强行统一成一个模型才是灾难。

### 2.3 上下文映射（Context Map）

| 关系 | 含义 | 代码表现 |
|---|---|---|
| **防腐层（ACL）** | 下游把上游模型翻译成自己的 | 转换器/DTO，隔离外部模型 |
| **共享内核** | 两个上下文共享一小块模型 | 慎用，容易耦合 |
| **客户-供应商** | 下游提需求，上游排期 | 接口契约 |
| **遵奉者** | 下游直接跟随上游模型 | 省事但被动 |
| **开放主机服务** | 上游提供稳定 API | REST/RPC |

> 本项目 `ProductClient` + `ProductRemoteDTO` 就是防腐层的雏形：把商品服务的模型翻译成订单需要的字段。

### 2.4 事件风暴（Event Storming）

用一个工作坊把业务跑一遍：

```text
1. 贴领域事件（橙色）：订单已创建、支付成功、订单已取消……
2. 贴命令（蓝色）：创建订单、确认支付、取消订单……
3. 贴聚合（黄色）：订单、支付单、库存……
4. 贴策略（紫色）：支付成功后 → 通知、扣积分
5. 划边界：把强相关的聚合圈成限界上下文
```

产出：**事件清单 + 聚合清单 + 上下文边界**，直接指导服务拆分。

---

## 三、战术设计（写代码）

### 3.1 实体 vs 值对象

| | 实体 Entity | 值对象 Value Object |
|---|---|---|
| 标识 | 有唯一 ID | 无 ID，靠值相等 |
| 可变性 | 可变 | **不可变** |
| 例子 | Order（orderNo） | Money、Address、DateRange |

```java
// 值对象：不可变 + 自校验
public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount.signum() < 0) throw new IllegalArgumentException("金额不能为负");
    }
    public Money add(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("币种不一致");
        return new Money(amount.add(other.amount), currency);
    }
}
```

> 用值对象替代 `BigDecimal amount` 裸字段，能把"币种/精度/非负"的规则收进类型里。

### 3.2 聚合与聚合根

**聚合 = 一致性边界**：一个事务只改一个聚合。

规则：

1. 聚合根是唯一入口，外部不能直接改内部实体；
2. 聚合尽量小（只放必须强一致的字段）；
3. 聚合之间**用 ID 引用**，不要对象引用；
4. 跨聚合一致性用**领域事件 + 最终一致**。

```java
// 订单聚合根：状态迁移只能走方法，不能 setStatus
public class Order {
    private OrderNo orderNo;
    private OrderStatus status;
    private List<OrderItem> items;
    private Money payAmount;

    public void pay(PaymentId paymentId) {
        if (status != OrderStatus.PENDING_PAY) {
            throw new IllegalStateException("当前状态不可支付");
        }
        this.status = OrderStatus.PAID;
        registerEvent(new OrderPaidEvent(orderNo, paymentId));
    }
}
```

### 3.3 领域事件

**过去发生的事实**，命名用过去式：`OrderCreated`、`OrderPaid`、`OrderCancelled`。

用途：

- 聚合之间解耦（订单支付成功 → 通知/积分/履约各自订阅）；
- 最终一致（本地事务 + 事件表/消息）；
- 审计与回放。

> 本项目 `OrderPaidEvent` + `@TransactionalEventListener(AFTER_COMMIT)` 就是领域事件的落地：事务提交后才推送，回滚不推。

### 3.4 仓储（Repository）

- 面向聚合根，不是面向表；
- 接口在领域层，实现在基础设施层（依赖倒置）；
- 一个聚合一个仓储。

```java
public interface OrderRepository {
    Order findById(OrderNo orderNo);
    void save(Order order);
}
```

### 3.5 应用服务 vs 领域服务

| | 应用服务 | 领域服务 |
|---|---|---|
| 职责 | 编排用例、事务、权限 | 跨聚合的领域规则 |
| 例子 | `OrderAppService.createOrder()` | `PricingService.calculate()` |
| 是否含业务规则 | 少 | 多 |

---

## 四、贫血模型 vs 充血模型

| | 贫血 | 充血 |
|---|---|---|
| 结构 | 实体只有 getter/setter，逻辑在 Service | 实体带行为，Service 只编排 |
| 优点 | 简单、团队熟悉 | 规则内聚、可单测 |
| 缺点 | 逻辑散落、难防非法状态 | 学习成本、与 ORM 有摩擦 |

**折中**：核心域用充血（订单/支付/账务），支撑域用贫血（CRUD 管理后台）。

---

## 五、订单域重构示例（本项目）

### 5.1 现状问题

- `OrderServiceImpl` 400+ 行，下单/取消/支付确认/退款混在一起；
- 状态靠字符串 + if-else 判断（已有状态机，但未收进聚合）；
- 跨服务调用（库存/支付）直接写在业务方法里；
- 领域事件只用于通知，没有作为一致性手段。

### 5.2 划上下文

```text
订单上下文：订单生命周期（创建/支付确认/取消/退款状态）
支付上下文：渠道下单/回调/对账
商品上下文：商品/库存
售后上下文：售后申请/审核
```

### 5.3 聚合设计

```text
Order（聚合根）
 ├── OrderItem（实体，订单项）
 ├── OrderNo（值对象）
 ├── Money（值对象：金额+币种）
 └── OrderStatus（值对象/枚举）

支付单 Payment（另一个聚合，用 orderNo 关联，不强一致）
```

**边界判断**：订单项必须与订单强一致 → 放同一聚合；支付单与订单只需最终一致 → 独立聚合。

### 5.4 领域事件

```text
OrderCreated  → 锁库存/发延迟消息
OrderPaid     → 通知、积分、履约
OrderCancelled→ 回补库存、关支付单
OrderRefunded → 退款、回补库存
```

### 5.5 应用服务编排

```java
@Transactional
public OrderVO createOrder(CreateOrderCommand cmd) {
    // 1. 幂等/锁（应用层）
    // 2. 加载聚合
    Order order = Order.create(cmd, pricingService.calculate(cmd));
    // 3. 保存
    orderRepository.save(order);
    // 4. 事件落库（本地事件表，事务内）
    eventStore.append(order.pullEvents());
    return toVO(order);
}
```

### 5.6 迁移步骤（绞杀者模式）

```text
1. 先在现有代码里加"领域事件"（低风险）
2. 把状态迁移收进聚合（用状态机替换 if-else）
3. 抽出仓储接口，隔离 MyBatis
4. 拆应用服务与领域服务
5. 最后再考虑拆服务/独立库
```

> 不要一上来重写。DDD 是渐进式重构，不是推倒重来。

---

## 六、常见误区

| 误区 | 纠正 |
|---|---|
| 每个表建一个实体就是 DDD | DDD 是边界与行为，不是分层改名 |
| 聚合越大越好 | 聚合越大，锁与事务越重；小聚合 + 事件 |
| 一个模型管所有上下文 | 同一概念在不同上下文可有不同模型 |
| DDD 必须微服务 | 模块化单体同样可以用 DDD |
| 值对象没必要 | 值对象能把校验和不可变性收进类型 |
| 领域事件 = MQ 消息 | 领域事件是事实，MQ 只是传播方式 |

---

## 七、面试题

| 题 | 答法骨架 |
|---|---|
| 什么是限界上下文？ | 边界内术语唯一、模型独立；同一词在不同上下文含义不同 |
| 聚合怎么设计？ | 一致性边界；小聚合；ID 引用；跨聚合用事件 |
| 领域事件怎么保证不丢？ | 本地事件表（与业务同事务）+ 投递重试 + 消费幂等 |
| DDD 和微服务什么关系？ | 无必然关系；DDD 划边界，微服务是部署方式 |
| 你们项目用 DDD 了吗？ | 诚实：部分（状态机/事件/仓储接口），未完整落地；说清下一步 |

---

## 附：与本项目代码的对照

| DDD 概念 | 本项目现状 |
|---|---|
| 聚合根 | `Order` 有状态字段，但状态迁移在 `OrderStateTransitionService`（未收进聚合） |
| 值对象 | 无（金额是裸 `BigDecimal`） |
| 领域事件 | `OrderPaidEvent` + `AFTER_COMMIT` ✅ |
| 仓储 | 直接用 MyBatis-Plus `Mapper`（无领域仓储接口） |
| 防腐层 | `ProductRemoteDTO` + `ProductClient`（雏形） |
| 限界上下文 | 12 个服务大致对应，但 `message`/`mq` 边界模糊 |

> 面试话术："我们没完整落 DDD，但已经用了其中三样：限界上下文划服务、领域事件做解耦、状态机收敛流转。如果重做，我会先把值对象和仓储接口补上。"
