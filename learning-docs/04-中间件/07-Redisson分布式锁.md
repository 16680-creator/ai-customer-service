# 07-Redisson 分布式锁

> 2026-08 落地记录：order 服务引入 Redisson，下单接口用户维度防重锁。

## 一、为什么手写 SETNX 不够

| 能力 | 手写 SETNX | Redisson |
|---|---|---|
| 自动续期（看门狗） | ❌ 业务超时后锁过期，并发穿透 | ✅ 默认 30s 持有，每 10s 续期 |
| 可重入 | ❌（需自己记持有者） | ✅ Hash 结构记录持有线程与计数 |
| 安全解锁 | ❌ 可能解掉别人的锁（锁已过期被他人抢占） | ✅ Lua 脚本校验持有者 |
| 等待/通知 | ❌ 自旋 | ✅ Redis Pub/Sub 唤醒 |

## 二、代码落点

```
ai-cs-order/
├── pom.xml                                  # redisson-spring-boot-starter（复用 spring.data.redis 配置）
└── src/main/java/com/aics/order/lock/
    ├── OrderCreateLockService.java          # withCreateLock(userId, Supplier)
    └── OrderServiceImpl                     # createOrder 入口包锁
```

关键实现（`OrderCreateLockService`）：

```java
RLock lock = redissonClient.getLock("lock:order:create:" + userId);
acquired = lock.tryLock(0, TimeUnit.SECONDS);   // waitTime=0：同用户并发下单快速失败
// 不指定 leaseTime → 看门狗生效
...
finally { if (acquired && lock.isHeldByCurrentThread()) lock.unlock(); }
```

设计决策：**锁在全局事务外层**（`createOrder` 入口加锁、拿到锁才进 `@GlobalTransactional`），
避免「拿不到锁的请求空占事务/数据库连接」。

## 三、锁的粒度选择（面试高频）

| 粒度 | 例子 | 优缺点 |
|---|---|---|
| 用户级（本项目） | `lock:order:create:{userId}` | 防单用户重复提交，不同用户互不影响 |
| 订单级 | `lock:order:pay:{orderNo}` | 支付回调幂等加固 |
| 全局 | `lock:order:create` | 简单但吞吐骤降，仅用于极少见的全局资源 |

## 四、验证方式

- 单测：`OrderCreateLockServiceTest`（4 用例，纯 Mockito 不连 Redis）
  - 竞争失败快速拒绝且不执行业务
  - 成功后释放锁
  - 业务异常 finally 兜底释放
  - waitTime=0（看门狗语义）
- 集成冒烟：并发两个相同用户下单请求，后者返回「您有订单正在创建中，请勿重复提交」

## 五、注意事项

1. `unlock` 前必须 `isHeldByCurrentThread()`，否则锁过期被他人抢占后解锁抛 `IllegalMonitorStateException`。
2. Redisson 客户端复用 `spring.data.redis` 配置；本地测试禁用 Redis 时，含 Redisson 的模块测试用 Mockito mock `RedissonClient`，不要连真实 Redis。
3. 分布式锁只解决「互斥」，不解决「数据一致性」——一致性靠 Seata/事务消息（见其他篇）。
