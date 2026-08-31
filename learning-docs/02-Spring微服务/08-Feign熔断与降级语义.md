# 08-Feign 熔断与降级语义（03-P2 落地记录）

> 2026-08 落地：order 交易链路接入 Resilience4j 熔断，ProductClient/PayClient 配
> FallbackFactory。核心是**按方法性质区分降级语义**——这也是本文最重要的一节。

## 一、改造前的问题

- 11 个 FeignClient（chat）与 order 的 2 个新 FeignClient 都**没有降级**：
  下游一挂，异常直接冒泡，线程被拖死（默认连接/读超时内每个请求都在傻等）
- 熔断只存在于 chat（Resilience4j 修饰业务方法），交易主链路（order→product/pay）裸奔

## 二、熔断接入：一行开关自动包裹

```yaml
# ai-cs-order application.yml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```

配 `spring-cloud-starter-circuitbreaker-resilience4j` 依赖后，
OpenFeign 的调用处理器自动换成 `FeignCircuitBreakerInvocationHandler`——
**每个 FeignClient 的调用都包上 CircuitBreaker**（实例名 = contextId：`productClient`/`payClient`），
无需手写 `@CircuitBreaker` 注解或 decorator。

> 网关层熔断（03-P3，粗粒度/服务级）与调用层熔断（细粒度/接口级）是两层防线：
> 网关挡住整服务不可用，调用层按接口熔断并降级。

## 三、降级语义红线（面试重点）

FallbackFactory 不是"包一层 try-catch 返回默认值"这么简单，
**每个方法的降级策略由其失败后果决定**：

| 方法 | 性质 | 降级策略 | 理由 |
|------|------|----------|------|
| `ProductClient.deductStock` | 关键写 | **抛业务异常**快速失败 | 库存没扣成功下单必须失败，静默吞掉=超卖；Seata 全局事务随之回滚 |
| `ProductClient.getProduct` | 读 | 抛业务异常 | 购物车路径已有"商品服务暂不可用"提示兜底；库存校验退化为 Redis 镜像值 |
| `ProductClient.restoreStock` | 尽力而为写 | **记告警 + 返回 fail，不抛** | 关单主流程（退券/关渠道订单）不应因回补失败中断；回补缺失靠对账任务兜底 |
| `PayClient.closeOrder` | 尽力而为通知 | 记告警 + 返回 fail，不抛 | 关单优先完成；渠道订单残留由支付服务超时兜底关闭 |

三条铁律：

1. **绝不假成功**：降级结果必须是"显式失败"（异常或 fail Result），绝不能返回 success
2. **读类可降级到兜底数据，写类要么快速失败要么显式告警**
3. **尽力而为类降级不阻断主流程**，但要留下可追溯的告警日志（对账/人工介入依据）

## 四、代码落点

```
ai-cs-order/pom.xml                          + spring-cloud-starter-circuitbreaker-resilience4j
ai-cs-order/application.yml                  + spring.cloud.openfeign.circuitbreaker.enabled: true
client/fallback/ProductClientFallbackFactory  新增（三类降级语义）
client/fallback/PayClientFallbackFactory      新增（尽力而为语义）
client/ProductClient / PayClient              + fallbackFactory 属性
```

FallbackFactory 的 `create(Throwable cause)` 里先记一条统一降级日志
（cause 是熔断 open 或底层异常），再返回匿名实现。

## 五、验证

- `mvn -pl ai-cs-order verify`：78 测试全绿（含新增 4 个降级语义测试），
  JaCoCo 门禁通过
- 降级测试断言：deductStock/getProduct 抛 `BusinessException(GATEWAY_SERVICE_UNAVAILABLE)`；
  restoreStock/closeOrder 返回 fail 且不抛
- ⏳ 真实故障演练（起两个服务断开 product）后补——验收点：product 宕机时下单 3s 内
  快速失败、预占释放、CB open 后请求不再打到下游

## 六、面试要点速记

- `feign.circuitbreaker.enabled` 的原理：OpenFeign 用
  `FeignCircuitBreakerInvocationHandler` 替换默认 InvocationHandler，
  按 client name 维护 CircuitBreaker
- 熔断三态（closed → open → half-open）与失败率/慢调用率触发条件
- 降级语义分类是系统设计题的高频考点：**读降级、写熔断、尽力而为告警**
- 为什么网关层与调用层要两层熔断：故障域不同（服务级 vs 接口级），阈值与恢复策略也不同
