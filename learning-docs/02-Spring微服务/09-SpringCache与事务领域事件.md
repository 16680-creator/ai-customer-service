# 09-Spring Cache 与事务领域事件（01-P2 落地记录）

> 2026-08 落地：product 的商品详情/分类列表接 Spring Cache + Redis；
> order 的超时关单、支付成功通知改为进程内领域事件，其中支付通知严格 AFTER_COMMIT。

## 一、Spring Cache：把缓存策略从业务代码中剥离

### 1.1 改造前后的对比

**旧方式**（业务逻辑手写 Redis）：

```java
String json = redisTemplate.opsForValue().get(key);
if (json == null) {
    value = mapper.selectById(id);
    redisTemplate.opsForValue().set(key, serialize(value), ttl);
}
return deserialize(json);
```

问题：命中/回源/序列化/TTL/失效逻辑散落业务方法，测试时要 mock Redis 细节。

**新方式**（声明式）：

```java
@Cacheable(cacheNames = PRODUCT_DETAIL, key = "#id", sync = true)
public ProductVO getProductDetail(Long id) { ... }

@CacheEvict(cacheNames = PRODUCT_DETAIL, key = "#id")
public ProductVO updateProduct(Long id, ProductUpdateDTO dto) { ... }
```

Spring AOP 代理在方法前查 Cache、命中直接返回，未命中执行目标方法并写入；
`@CacheEvict` 默认 `beforeInvocation=false`，方法**成功后**才删除（失败不删）。

### 1.2 本项目缓存边界

| cacheName | 方法 | TTL | 失效点 |
|-----------|------|-----|--------|
| `product:detail` | `getProductDetail(id)` | 30 分钟 | update/delete/deductStock/restoreStock |
| `product:categories` | `listCategories()` | 10 分钟 | createCategory（allEntries） |

**不缓存商品分页列表**：分页+关键词+分类+状态的 key 组合基数高，库存/销量又高频变化，
命中率低、驱逐面大——缓存不是越多越好。

`sync=true`：同一 JVM 同 key 并发 miss 时只允许一个线程回源，其他等待，降低缓存击穿；
它不跨网关实例，跨实例击穿仍需分布式锁/逻辑过期等更重方案（当前流量不需要）。

### 1.3 序列化与 TTL 配置

`ProductCacheConfig`：

- `GenericJackson2JsonRedisSerializer`：跨语言可读 JSON，带类型信息（避免 LinkedHashMap 反序列化问题）
- 禁止缓存 null：商品不存在抛业务异常，不把异常结果缓存（避免长时间假 404）
- 默认 TTL 20 分钟；按 cacheName 覆盖详情 30 / 分类 10 分钟

### 1.4 缓存一致性

采用**先更新数据库，成功后删除缓存**：

1. DB update 提交成功
2. AOP afterInvocation 执行 CacheEvict
3. 下一次读回源拿新值

极端窗口：DB 已更新但删缓存失败 → 短期脏读（最长 TTL）。生产增强路径：
`@TransactionalEventListener(AFTER_COMMIT)` 发缓存删除事件 + MQ 重试，或 CDC/binlog 订阅
（02 计划 P3 Canal）。本阶段流量与业务等级下 TTL + 主动驱逐足够。

## 二、领域事件：进程内解耦与事务边界

### 2.1 领域事件 vs RocketMQ

| 类型 | 适用边界 | 本项目例子 |
|------|----------|------------|
| Spring ApplicationEvent | 同一进程内模块解耦、事务同步回调 | OrderTimeoutEvent / OrderPaidEvent |
| RocketMQ | 跨服务、跨时间、需持久化重试 | order-timeout-topic 延迟消息 / notify-topic |

二者不是替代关系，而是串联：

```
RocketMQ 延迟消息（跨时间唤醒）
 → OrderTimeoutListener（协议适配）
 → publish OrderTimeoutEvent（进程内领域事实）
 → OrderTimeoutEventListener
 → OrderService.cancelExpiredOrder（业务）
```

这样消息协议适配层不再直接依赖关单实现，监听器单测与业务单测分开。

### 2.2 为什么支付通知必须 AFTER_COMMIT

旧代码在 `confirmPay @Transactional` 方法里先更新状态，随后直接发 notify-topic，
再清购物车。若「发完通知后清购物车失败」导致事务回滚，用户却已经收到支付成功通知——
典型的**事务内副作用泄漏**。

新链路：

```java
// confirmPay 的事务内：只记录领域事实
orderMapper.updateById(order);
eventPublisher.publishEvent(new OrderPaidEvent(orderNo, userId));
// 后续清购物车若失败 → 事务回滚

@TransactionalEventListener(phase = AFTER_COMMIT)
void handle(OrderPaidEvent event) {
    rocketMQTemplate.convertAndSend("notify-topic", payload);
}
```

原理：事件发布时，Spring 的 `TransactionalApplicationListener` 把 callback 注册到
`TransactionSynchronizationManager`；事务真正 commit 后执行 `afterCommit()`。
回滚则 callback 永不执行。`fallbackExecution=false`（默认）还确保**无事务发布时也不执行**。

### 2.3 四种 TransactionPhase

| phase | 时机 | 使用场景 |
|-------|------|----------|
| BEFORE_COMMIT | commit 前 | 同事务内最后校验（抛异常可阻止提交） |
| AFTER_COMMIT（默认） | commit 成功后 | 发通知、删缓存、审计记录等不可回滚副作用 |
| AFTER_ROLLBACK | 回滚后 | 补偿告警 |
| AFTER_COMPLETION | commit/rollback 都执行 | 资源清理 |

注意：AFTER_COMMIT 时主事务已提交，此时监听器里的 DB 写操作需要 `REQUIRES_NEW` 才能提交；
本项目监听器只发 MQ，不涉及新 DB 事务。

## 三、代码与验证

```
product/config/ProductCacheConfig.java                CacheManager + TTL
product/service/impl/ProductServiceImpl               @Cacheable / @CacheEvict
order/event/OrderTimeoutEvent(+Listener)               超时领域事件
order/event/OrderPaidEvent(+Listener)                  AFTER_COMMIT 通知
order/listener/OrderTimeoutListener                    MQ → 领域事件适配
order/service/impl/OrderServiceImpl                    事务内发布 OrderPaidEvent
```

验证结果：

- product：54 测试全绿（新增缓存契约测试 3），JaCoCo 门禁通过
- order：81 测试全绿（新增领域事件测试 7/重构超时测试），JaCoCo 门禁通过
- `OrderPaidEventListenerTest` 反射锁定 `phase=AFTER_COMMIT`、`fallbackExecution=false`
- `ProductCacheContractTest` 反射锁定 cacheNames/keys/sync/afterInvocation 契约

## 四、面试要点速记

- Spring Cache 三层抽象：CacheManager（管理多个 Cache）→ Cache（键值容器）→ 注解 AOP
- 缓存一致性为什么选先更库后删缓存；删失败如何用事务事件/MQ/CDC 增强
- `sync=true` 只防单 JVM 击穿，不是分布式锁
- `@TransactionalEventListener` 的四个 phase，AFTER_COMMIT 防副作用泄漏
- 领域事件与 MQ 的边界：同进程解耦 vs 跨进程可靠投递
