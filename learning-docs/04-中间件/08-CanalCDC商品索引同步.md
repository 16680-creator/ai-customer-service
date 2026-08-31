# 08-Canal CDC 商品索引同步（02-P3 分阶段落地）

> 2026-08 实施记录。已落地 MySQL binlog 前置配置与 search 消费端；Canal Server/Adapter
> 运行联调受本机无 Docker CLI 阻塞，Adapter 版本配置在有容器环境后执行并验收。

## 一、为什么需要 CDC

原先商品更新后没有任何 ES 写路径：search 只有知识库索引，商品改价/下架不会实时反映到搜索。
离线 `sync-kb-to-es.py` 也无法处理商品秒级变化。

目标链路：

```text
MySQL product 表 ROW binlog
  → Canal Server（伪装 MySQL slave）
  → Canal Adapter RocketMQ topic: c-product-sync
  → ai-cs-search ProductCdcConsumer
  → Elasticsearch product_catalog（_id = product.id）
```

## 二、MySQL 前置配置（已落地）

`deploy/mysql/mysql.cnf`：

```ini
server-id=1
log-bin=mysql-bin
binlog-format=ROW
binlog-row-image=FULL
expire-logs-days=7
```

- ROW：记录每行的变更，而不是 SQL 文本，避免 statement 非确定性
- FULL：UPDATE/DELETE 保留完整前后行镜像，消费端可可靠得到 id/删除标记
- `canal` 用户只给 `SELECT, REPLICATION SLAVE, REPLICATION CLIENT`，不授业务写权限

`deploy/mysql/canal-init.sql` 已挂到根 docker-compose MySQL 初始化目录。

## 三、search 消费端（已落地，默认关闭）

`ProductCdcConsumer`：

- `@RocketMQMessageListener(topic="c-product-sync", consumerGroup="search-product-cdc-consumer")`
- 仅处理 `table=product`
- ES `_id = product.id`：INSERT/UPDATE 都是覆盖 upsert，天然幂等
- DELETE 或 `deleted=1`：删除 ES 文档；不存在忽略（重复删除幂等）
- ES 异常**抛出**让 RocketMQ 重试，绝不静默丢索引事件
- `CANAL_PRODUCT_CDC_ENABLED=false` 默认关闭，未部署 Canal 时不消费副作用

依赖和配置：

```yaml
rocketmq.name-server: ${ROCKETMQ_NAME_SERVER:127.0.0.1:9876}
aics.cdc.product.enabled: ${CANAL_PRODUCT_CDC_ENABLED:false}
```

验证：`ProductCdcConsumerTest` 覆盖默认关闭/非 product 表忽略，2 tests 全绿；
`mvn -pl ai-cs-search -DskipTests compile` 成功。

## 四、Canal Adapter 配置模板（待 Docker 联调）

Canal 分两层：

- **Canal Server**：连 MySQL binlog，instance filter 只匹配 `aics_product\\.product`
- **Canal Adapter**：把 Server event 转成 RocketMQ JSON，写 topic `c-product-sync`

生产必须锁定 Canal Server/Adapter 同一版本（建议 1.1.7），不要混用网上不同年代的
`canal.properties` 字段。启动后先用一条 UPDATE 验证 Adapter 消息结构是否匹配：

```json
{"database":"aics_product","table":"product","type":"UPDATE",
 "data":[{"id":"1001","name":"...","status":"1","deleted":"0"}]}
```

若 Adapter 输出结构不同，先改 `CanalChangeEvent` 反序列化 DTO，再开生产开关，不能靠猜。

## 五、最终一致性三板斧

1. **幂等**：ES 固定 document id upsert
2. **乱序**：当前 MVP 依赖 Canal 单 instance 顺序；扩容多分区后须带 binlog offset/updatedAt，
   丢弃旧版本
3. **对账**：定时比较 MySQL `product where deleted=0` 数量与 ES `product_catalog` 数量，
   差异告警并用全量脚本重建

CDC 不是强一致事务：商品 DB 提交到 ES 可见有秒级延迟；用户下单库存仍只信 product DB，
搜索是可延迟的读模型。

## 六、运行验收（待 Docker）

1. 起 MySQL/RocketMQ/ES/Canal Server/Adapter/search
2. `UPDATE product SET price=188.00 WHERE id=1001`
3. 消费日志看到 `CDC 商品索引 upsert: id=1001`
4. `GET product_catalog/_doc/1001` 价格为 188
5. 删除/逻辑删除商品，文档消失
6. 重放同一消息，ES 结果不重复

## 七、面试要点

- binlog STATEMENT/ROW/MIXED 区别，CDC 选 ROW+FULL 的理由
- Canal 模拟 slave 协议读取 binlog；Server（采集）与 Adapter（投递）职责分离
- CDC 读模型的幂等、乱序、对账三板斧
- 为什么不是业务代码双写 MySQL+ES：双写失败窗口更大，CDC 订阅数据库事实变更更可靠
