# RocketMQ 消息队列

> 本项目使用 **RocketMQ 5.1.4** 处理异步消息：订单超时取消、支付回调通知等。
> 对应项目文件：`docker-compose.yml`（RocketMQ 容器）、`ai-cs-order`（消息生产者/消费者）

---

## 一、为什么需要消息队列？

### 同步 vs 异步

```
【同步下单】用户等 3 秒
  创建订单(50ms) → 扣库存(50ms) → 发短信(800ms) → 发通知(500ms) → 更新搜索(1s)
  总耗时: 2.4 秒（用户一直在等）

【异步下单】用户等 100ms
  创建订单(50ms) → 扣库存(50ms) → 发一条消息到 MQ → 返回"下单成功"
                                         ↓（后台异步处理）
                                   发短信、发通知、更新搜索...
```

### 本项目的使用场景

| 场景 | Topic | 说明 |
|------|-------|------|
| 订单超时取消 | ORDER_TIMEOUT | 下单后 30 分钟未支付 → 自动取消 |
| 支付成功通知 | PAY_SUCCESS | 支付完成 → 通知发货、更新状态 |
| 知识库更新 | KNOWLEDGE_UPDATE | 文档上传 → 异步向量化入库 |

---

## 二、核心概念

```
┌──────────────────────────────────────────────────────────┐
│                      RocketMQ                             │
│                                                           │
│  Producer（生产者）                                       │
│  └── 发送消息到 Topic                                     │
│                                                           │
│  Topic（主题）= 消息的分类                                 │
│  ├── ORDER_TIMEOUT                                       │
│  ├── PAY_SUCCESS                                         │
│  └── KNOWLEDGE_UPDATE                                    │
│                                                           │
│  Consumer（消费者）                                       │
│  └── 订阅 Topic，接收并处理消息                            │
│                                                           │
│  NameServer：路由注册中心（类似 DNS）                      │
│  Broker：消息存储和转发的服务器                            │
└──────────────────────────────────────────────────────────┘
```

---

## 三、Docker 部署

```yaml
# docker-compose.yml
rocketmq-namesrv:
  image: apache/rocketmq:5.1.4
  container_name: aics-rocketmq-namesrv
  ports:
    - "9876:9876"           # NameServer 端口
  command: sh mqnamesrv
  environment:
    JAVA_OPT_EXT: "-server -Xms256m -Xmx256m"

rocketmq-broker:
  image: apache/rocketmq:5.1.4
  container_name: aics-rocketmq-broker
  ports:
    - "10911:10911"         # Broker 端口
    - "10909:10909"         # VIP 通道端口
  command: sh mqbroker -n rocketmq-namesrv:9876 -c /home/rocketmq/rocketmq-5.1.4/conf/broker.conf
  environment:
    JAVA_OPT_EXT: "-server -Xms512m -Xmx512m"
  depends_on:
    rocketmq-namesrv:
      condition: service_healthy
```

---

## 四、Spring Boot 集成

### 4.1 依赖

```xml
<!-- ai-cs-order/pom.xml 中已有 -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
```

### 4.2 配置

```yaml
rocketmq:
  name-server: localhost:9876       # NameServer 地址
  producer:
    group: order-producer-group     # 生产者组名
    send-message-timeout: 3000      # 发送超时
    retry-times-when-send-failed: 2 # 失败重试次数
```

---

## 五、生产者（发送消息）

### 5.1 订单超时场景

```java
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 创建订单后，发送延迟消息（30分钟后检查是否支付）
     */
    public OrderVO createOrder(Long userId, CreateOrderRequest req) {
        // 1. 创建订单（状态：待付款）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus(0);  // 待付款
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        orderMapper.insert(order);

        // 2. 发送延迟消息：30 分钟后触发超时检查
        rocketMQTemplate.syncSend(
            "ORDER_TIMEOUT",                    // Topic
            MessageBuilder.withPayload(order.getOrderNo()).build(),
            3000,                               // 发送超时 3 秒
            16                                  // 延迟级别 16 = 30 分钟
        );

        return convertToVO(order);
    }
}
```

### 5.2 延迟级别对照表

| 级别 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | ... | 16 | 17 | 18 |
|------|---|---|---|---|---|---|---|---|---|-----|----|----|-----|
| 时间 | 1s | 5s | 10s | 30s | 1m | 2m | 3m | 4m | 5m | ... | 30m | 1h | 2h |

---

## 六、消费者（接收消息）

### 6.1 订单超时监听器

```java
// ai-cs-order/src/main/java/com/aics/order/listener/OrderTimeoutListener.java
@Component
@RocketMQMessageListener(
    topic = "ORDER_TIMEOUT",              // 订阅的 Topic
    consumerGroup = "order-timeout-group"  // 消费者组
)
public class OrderTimeoutListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutListener.class);

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public void onMessage(String orderNo) {
        log.info("收到订单超时检查消息: {}", orderNo);

        // 1. 查询订单
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );

        if (order == null) return;

        // 2. 如果还是"待付款"状态 → 取消订单
        if (order.getStatus() == 0) {
            order.setStatus(4);  // 已取消
            orderMapper.updateById(order);
            
            // 3. 恢复库存
            // productClient.restoreStock(order.getProductId(), order.getQuantity());
            
            log.info("订单已自动取消: {}", orderNo);
        }
        // 如果已支付，忽略这条消息
    }
}
```

---

## 七、消息可靠性保障

### 7.1 三个环节

```
Producer → Broker → Consumer
   ①         ②        ③

① 发送可靠：同步发送 + 重试
② 存储可靠：同步刷盘 + 主从复制
③ 消费可靠：手动 ACK + 消费幂等
```

### 7.2 消费幂等（防止重复消费）

```java
@Override
public void onMessage(String orderNo) {
    // 幂等检查：用 Redis 记录已处理的消息
    String idempotentKey = "mq:consumed:ORDER_TIMEOUT:" + orderNo;
    Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);
    
    if (!Boolean.TRUE.equals(isNew)) {
        log.info("消息已处理过，跳过: {}", orderNo);
        return;
    }
    
    // 正常业务逻辑...
}
```

---

## 八、本项目测试示例

```java
// ai-cs-order/src/test/java/com/aics/order/listener/OrderTimeoutListenerTest.java
@ExtendWith(MockitoExtension.class)
class OrderTimeoutListenerTest {

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderTimeoutListener listener;

    @Test
    @DisplayName("订单超时 - 待付款订单应被取消")
    void onMessage_pendingOrder_shouldCancel() {
        Order order = new Order();
        order.setOrderNo("ORD20240101001");
        order.setStatus(0);  // 待付款

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateById(any())).thenReturn(1);

        listener.onMessage("ORD20240101001");

        verify(orderMapper).updateById(argThat(o -> o.getStatus() == 4));
    }

    @Test
    @DisplayName("订单超时 - 已支付订单不应被取消")
    void onMessage_paidOrder_shouldIgnore() {
        Order order = new Order();
        order.setOrderNo("ORD20240101002");
        order.setStatus(1);  // 已付款

        when(orderMapper.selectOne(any())).thenReturn(order);

        listener.onMessage("ORD20240101002");

        verify(orderMapper, never()).updateById(any());
    }
}
```

---

## 九、动手练习

1. 启动 RocketMQ：`docker-compose up -d rocketmq-namesrv rocketmq-broker`
2. 在订单服务中写一个 Producer，发送测试消息
3. 写一个 Consumer 接收并打印消息
4. 测试延迟消息：发送级别 1（1 秒后收到）
5. 模拟消费失败，观察重试机制

---

## 学习检查清单

- [ ] 理解 MQ 的核心价值：异步、解耦、削峰
- [ ] 理解 Producer → Topic → Consumer 的模型
- [ ] 会发送同步/异步/延迟消息
- [ ] 会编写 @RocketMQMessageListener 消费者
- [ ] 理解消费幂等的重要性及实现方式
- [ ] 理解消息丢失的三个环节及保障措施

---

## 下一步

→ [03-Elasticsearch搜索引擎](./03-Elasticsearch搜索引擎.md)
