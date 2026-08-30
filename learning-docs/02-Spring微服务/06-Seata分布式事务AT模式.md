# 06-Seata 分布式事务（AT 模式）

> 2026-08 落地记录：下单扣库存从「先扣后用 + 手工回补」改造为 Seata AT 全局事务
> （order 发起 ↔ product 分支），seata-server 1.7.1 本地 file 模式。

## 一、改造前后的对比

**旧方案（尽力一致）**：

```java
try {
    for (item : cartItems) productStockClient.deductStock(...);  // 同步 Feign 扣库存
} catch (Exception e) {
    for (已扣项) productStockClient.restoreStock(pid, qty);      // 手工回补
    throw new BusinessException(ORDER_STOCK_INSUFFICIENT);
}
```

问题：回补本身可能失败（网络抖动）→ 需要人工核查；代码复杂且每一单都要背这个心智负担。

**新方案（Seata AT）**：

```java
@GlobalTransactional(rollbackFor = Exception.class, name = "order-create")
@Transactional(rollbackFor = Exception.class)
public OrderVO doCreateOrder(...) {
    for (item : cartItems) productStockClient.deductStock(...);  // 分支事务：XID 随请求头传播
    ... 落订单（本地事务）
    // 任一环节失败 → TC 协调全部分支按 undo_log 反向补偿
}
```

## 二、AT 模式原理（面试高频）

两阶段提交，但对业务**无感**：

```
一阶段：拦截业务 SQL → 查询前后镜像 → 生成 undo_log 与业务 SQL 同一本地事务提交 → 分支注册到 TC
二阶段：
  全局提交 → 异步批量删除 undo_log（极快）
  全局回滚 → 按 undo_log 后镜像生成反向 SQL 补偿 → 删除 undo_log
```

关键机制：
- **XID 传播**：全局事务发起后，XID 放进请求头；Seata 对 Feign 有自动拦截器把 XID 传给下游
- **数据源代理**：`enable-auto-data-source-proxy: true` 自动把 DataSource 包成 Seata 代理，
  SQL 解析、镜像记录全自动
- **全局锁**：AT 在 TC 侧维护行级全局锁，防止两个全局事务同时改同一行（写隔离）

## 三、代码与配置落点

```
ai-cs-order/    pom.xml + application.yml（seata 配置块）+ OrderServiceImpl（@GlobalTransactional）
ai-cs-product/  pom.xml + application.yml + ProductServiceImpl（deductStock/restoreStock 加 @Transactional）
deploy/mysql/seata-undo-log.sql                 # order/product 两库各建 undo_log
tools/seata/seata/                              # seata-server 1.7.1（file 模式，TC 端口 8091）
```

客户端配置（两个服务相同）：

```yaml
seata:
  application-id: ${spring.application.name}
  tx-service-group: aics_tx_group
  service:
    vgroup-mapping:
      aics_tx_group: default      # 事务组 → TC 集群名映射
    grouplist:
      default: 127.0.0.1:8091     # file 注册模式直连 TC
  registry:
    type: file
  data-source-proxy-mode: AT
  enable-auto-data-source-proxy: true
```

## 四、非 Seata 资源的注意事项

MQ 不是 Seata 资源：`doCreateOrder` 里的**延迟消息**（超时关单）在全局回滚时已发出。
安全的原因：消费侧 `cancelExpiredOrder` 查不到订单直接返回（状态机幂等）。
→ 混合编排原则：**全局事务内的 MQ 消息，消费侧必须幂等**。

## 五、验证方式

- 启动 seata-server（`tools/seata/seata/bin/seata-server.bat`），控制台 http://localhost:7091（seata/seata）
- 下单成功：order/product 的 undo_log 表一阶段插入、事务提交后被清理
- 库存不足下单：product 分支抛异常 → TC 全局回滚 → 订单不落库、已扣库存被镜像补偿回来

## 六、常见问题

1. **undo_log 表缺失** → 分支提交报表不存在；每个参与分支事务的库都要建（表名固定）。
2. **vgroup-mapping 不一致** → 客户端找不到 TC；`tx-service-group` 与 `service.vgroup-mapping` 的 key 必须对应。
3. **分支事务方法必须能被 Spring 代理**（public + 由容器调用），否则 undo_log 不生成、二阶段无法回滚。
4. AT 模式适合**短事务**；长事务/跨 MQ 的最终一致场景改用事务消息（见 08-RocketMQ 事务消息）。
