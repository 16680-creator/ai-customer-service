# 中间件（04 模块）

> 本专题覆盖本仓**实际使用**的中间件全家桶（01~12，全部有部署物与代码锚点），以及 **2026-09 认知拓展批**（13~22，多为"工程未落地/只学不引"的高价值组件——按仓库纪律，引入与否在每篇开头显式给决策）。
> 写法与全库一致：每篇绑定真实配置/代码锚点，未落地主题显式标注并给"如果引入，接在哪"的评估。

---

## 一、文档地图

### A. 工程在用（01~12，全部有部署物）

| 篇 | 主题 | 一句话定位 | 难度 |
|---|---|---|---|
| [01-Redis缓存实战](01-Redis缓存实战.md) | 缓存模式、一致性、热 key | 最常用的 L2 | ★★ |
| [02-RocketMQ消息队列](02-RocketMQ消息队列.md) | 基础模型、可靠投递 | 本仓消息主力 | ★★ |
| [03-Elasticsearch搜索引擎](03-Elasticsearch搜索引擎.md) | 倒排索引、DSL、混合检索 | 全文检索底座 | ★★★ |
| [04-MinIO对象存储](04-MinIO对象存储.md) | 对象存储、预签名 | 文档/图片存储 | ★★ |
| [05-SSE与WebSocket实时通信](05-SSE与WebSocket实时通信.md) | SSE 流式、WS 双工 | AI 对话与通知通道 | ★★★ |
| [06-Sentinel限流与熔断降级](06-Sentinel限流与熔断降级.md) | 流控/熔断/降级规则 | 接口级治理 | ★★★ |
| [07-Redisson分布式锁](07-Redisson分布式锁.md) | 看门狗、可重入、红锁辨析 | AP 派互斥 | ★★★ |
| [08-RocketMQ事务消息与死信队列](08-RocketMQ事务消息与死信队列.md) | 半消息/回查、DLQ | 一致性消息 | ★★★★ |
| [09-AI与治理中间件部署](09-AI与治理中间件部署.md) | Chroma/Neo4j/XXL-Job/Sentinel Dashboard | 部署缺口 P1 落地 | ★★ |
| [10-MongoDB对话审计归档](10-MongoDB对话审计归档.md) | 文档模型、TTL、归档模式 | 会话审计（最佳努力） | ★★ |
| [11-CanalCDC商品索引同步](11-CanalCDC商品索引同步.md) | binlog→Canal→MQ→ES | 实时同步链路 | ★★★★ |
| [12-Kafka与RocketMQ双栈切换](12-Kafka与RocketMQ双栈切换.md) | 消息抽象层+开关 | 可插拔 MQ 架构 | ★★★★ |

### B. 认知拓展批（13~22，2026-09 新增，逐篇标注引入决策）

| 篇 | 主题 | 引入决策 | 一句话定位 | 难度 |
|---|---|---|---|---|
| [13-Nginx与OpenResty](13-Nginx与OpenResty反向代理.md) | 反向代理/限流/静态卸载 | 未部署（可选 Ingress） | 入口层事实标准；三层限流分工 | ★★ |
| [14-SkyWalking链路追踪](14-SkyWalking链路追踪.md) | APM、字节码增强 | 未落地（与 Tempo 可并存） | 国内 JD 高频；对照手写 TraceInterceptor | ★★★ |
| [15-Dubbo与RPC框架](15-Dubbo与RPC框架.md) | SPI/容错/隐式传参 | 未引入（存量 Feign） | 对照 OpenFeign 的 RPC 坐标系 | ★★★ |
| [16-ZooKeeper与etcd](16-ZooKeeper与etcd分布式协调.md) | ZAB/Raft、临时节点/租约 | 不引入（K8s Lease 可替代选主） | CP 派协调双雄 + AP/CP 锁账本 | ★★★ |
| [17-Caffeine本地缓存](17-Caffeine本地缓存与两级缓存.md) | W-TinyLFU、两级缓存 | 未引入（给了 chat 两级化评估） | 把手写 LRU 升级成工程方案 | ★★★ |
| [18-gRPC与Protobuf](18-gRPC与Protobuf跨语言调用.md) | 编码/流模式/HTTP2 | 未引入（给了 py-chat 评估） | 跨语言低延迟 RPC；SSE 同构 | ★★★ |
| [19-Debezium与Canal对比](19-Debezium与Canal的CDC对比.md) | 两大家族 CDC | 只对比不引入 | 把 04-11 的选型收口成决策树 | ★★★ |
| [20-PostgreSQL与pgvector](20-PostgreSQL与pgvector.md) | PG 差异 + 向量扩展 | 未引入（Chroma→pgvector→Milvus 路径） | JD 第二关系库 + 向量免建库 | ★★★ |
| [21-RabbitMQ概念模型](21-RabbitMQ概念模型与AMQP协议.md) | AMQP/Exchange/confirm | **明确不引入**（C 类决策） | 只学不引；三套 MQ 总对照 | ★★ |
| [22-Eureka与Consul](22-Eureka与Consul注册中心对照.md) | AP/CP 注册中心 | 不引入（Nacos 在用） | 四轴对照 + K8s 双层发现 | ★★ |

## 二、与其他模块的分工边界（查重声明）

| 主题 | 在哪里讲 | 本模块讲不讲 |
|---|---|---|
| MySQL（索引/MVCC/锁/主从/备份/大表治理） | [03-数据库与ORM](../03-数据库与ORM/README.md) | 19/20 篇只做 CDC 与选型对照 |
| Nacos 注册与配置 | [02-Spring微服务/03](../02-Spring微服务/03-Nacos注册与配置中心.md) | 22 篇只做 AP/CP 对照，机制不重复 |
| Seata AT / XID 传播 | [02-Spring微服务/06、07](../02-Spring微服务/06-Seata分布式事务AT模式.md) | 不讲 |
| XXL-Job 分布式调度 | [07-运维部署/05](../07-运维部署/05-XXL-Job分布式调度.md) | 09 篇只讲 Admin 部署 |
| Prometheus/Grafana/Tempo/OTel | [07-运维部署/04](../07-运维部署/04-Prometheus可观测性.md) | 14 篇只做 SkyWalking 对比与接入评估 |
| Loki 日志聚合 | [07-运维部署/07](../07-运维部署/07-Loki日志聚合.md) | 不讲 |
| ClickHouse / OLAP / CDC 工程学 | [14-数据工程与OLAP](../14-数据工程与OLAP/README.md) | 19 篇只做 Cana/Debezium 对比 |
| 向量库原理/选型/运维 | [05-AI集成/05-向量数据库](../05-AI集成/05-向量数据库/README.md) | 20 篇只做 pgvector 落点评估 |
| Service Mesh 边界 | [16-云原生与GitOps/05](../16-云原生与GitOps/05-ServiceMesh与Istio边界.md) | 不讲；22 篇只提 Consul Connect 一句 |
| 消息抽象层实现 | [02-Spring微服务/10](../02-Spring微服务/10-自定义Starter与自动装配.md) + 本模块 12 | 12 篇与 02-10 互为镜像（工程视角/框架视角） |

## 三、学习路径

**新手（先把在用的跑通）**：01 → 02 → 04 → 05，每篇配合 `docker-compose up -d` 里的真实服务动手。

**进阶（讲清可靠性）**：07 → 08 → 11 → 12，四篇连读正好是"锁、一致性消息、实时同步、可插拔"四个工程难题。

**认知拓展（求职广度）**：13 → 14 → 15 → 16（入口/APM/RPC/协调四大件），再按目标岗位挑 17~22。

**验收**：每篇文末"学习检查清单"全勾 + 能对着本仓配置讲出至少 3 个真实锚点。

## 四、更新日志

- 2026-08：01~08 落地（随一二梯队与 P1~P3 工程）。
- 2026-08~09：09~12 落地（P1 部署补齐、MongoDB、Canal、Kafka 双栈）。
- 2026-09：第十二批认知拓展——13~22 十篇 + 本 README（T0 七篇 + T1 三篇，逐篇给引入决策）。
