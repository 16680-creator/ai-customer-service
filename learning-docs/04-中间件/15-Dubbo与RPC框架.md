# 15-Dubbo 与 RPC 框架：从零开始理解"方法调用怎么跨机器"（认知拓展，工程未落地）

> **定位**：本仓服务间调用是 **OpenFeign（chat 11 个 FeignClient）+ RestTemplate（order/pay 的 client）** 双轨（见 [02-Spring微服务/07-服务调用统一与SeataXID传播](../02-Spring微服务/07-服务调用统一与SeataXID传播.md)），Dubbo 未引入。但国内 Java JD 里 Dubbo 出现率极高，且它的 **SPI、集群容错、隐式传参** 三个概念能把你的既有知识串起来。本篇为认知拓展：学原理、建对照、不引入。
> 前置阅读：[01-Java基础/06-反射动态代理与SPI](../01-Java基础/06-反射动态代理与SPI.md)（SPI——Dubbo 的骨架）、[02-Spring微服务/02-SpringCloud微服务架构](../02-Spring微服务/02-SpringCloud微服务架构.md)（Feign 与负载均衡）、[04-中间件/16](16-ZooKeeper与etcd分布式协调.md)（注册中心的 CP 派）。

---

## ⚡ 30 秒速记卡

```text
① RPC = 让"调远程方法"像"调本地方法"：代理拦截 → 序列化 → 网络传输 → 反序列化 → 反射执行
② Dubbo 四角色：Provider(提供)/Consumer(消费)/Registry(注册中心)/Monitor(监控)
③ 骨架是 SPI：接口 + 自适应扩展（@SPI/@Adaptive），协议/注册中心/负载均衡全是可插拔实现
④ 集群容错六策：failover(默认,重试别的) / failfast / failsafe / failback / forking / broadcast
⑤ 隐式传参 RpcContext ≈ 你的 X-User-Id 头透传 / Seata TX_XID——"调用上下文跨进程传播"是共同命题
```

---

## 一、从 HTTP 调用到 RPC：差异到底在哪

你现在的调用（Feign）本质是：**接口代理 → 拼 HTTP 请求 → JSON 序列化 → 负载均衡 → 远端 Controller**。
Dubbo 做的是同一件事，但每一环都换了更"为 RPC 而生"的实现：

| 环节 | Feign（本仓） | Dubbo |
|---|---|---|
| 代理 | JDK/CGLIB 接口代理 | 同样是接口代理（`ReferenceBean`） |
| 协议 | HTTP/1.1 文本 | 默认 **dubbo 协议**：单一长连接 + NIO + 二进制紧凑头（也支持 Triple/REST） |
| 序列化 | JSON（Jackson） | Hessian2 / Protobuf / Kryo（更小更快） |
| 服务发现 | Nacos/注册中心 | Nacos/ZooKeeper 等（Registry 抽象） |
| 负载均衡 | Ribbon/LoadBalancer | 内建 4 种策略 + 可 SPI 扩展 |
| 容错 | 重试器 + Sentinel | 内建 6 种集群容错策略 |
| 服务治理 | 网关/配置中心 | 控制台（条件路由、动态配置、权重） |

> **一句话**：HTTP+JSON 胜在**通用、可调试、生态宽**；RPC 二进制协议胜在**性能与治理内建**。内部服务多、调用密集、Java 栈统一时 Dubbo 优势明显；对外开放/多语言混用时 HTTP/REST 依旧必留。

---

## 二、架构：四角色与一次调用的完整路径

```text
Provider 启动 ──register──► Registry（Nacos/ZK）
Consumer 启动 ◀─subscribe───┘  （拿到 Provider 地址列表并本地缓存）
Consumer ──invoke──► (负载均衡选一台) ──协议编解码──► Provider 线程池 ──反射调用实现──► 返回
        └──────── 统计上报 ────────────► Monitor
```

**关键设计：Consumer 本地缓存地址列表**——注册中心全挂了，存量调用仍能继续（只是发现不了新节点）。这个"数据本地副本"思想与你的 Nacos 客户端缓存一致。

---

## 三、骨架：SPI 与自适应扩展（衔接你学过的 Java SPI）

Java 原生 SPI（`ServiceLoader`）的问题：全量实例化、不能按条件挑、无 IOC/AOP。
Dubbo SPI（`@SPI` + `META-INF/dubbo/...`）补了三板斧：

```java
// 1. 按名字取一个实现（不是全量）
Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
        .getExtension("dubbo");           // 拿 DubboProtocol

// 2. @Adaptive 自适应：运行时按 URL 参数决定用哪个实现
//    例如 Registry 根据注册中心地址是 nacos:// 还是 zookeeper:// 自动切实现
// 3. 包装类（Wrapper）= SPI 层的 AOP：把 Protocol 层层包上 Filter/Listener
```

> 与 Spring 自动装配对照着记：**Spring 用"条件注解 + imports 清单"决定装哪个 Bean；Dubbo 用"SPI 文件 + URL 参数"决定用哪个扩展**。两套机制解决同一个问题：**可插拔**。

---

## 四、集群容错六策（面试必背，配场景记）

| 策略 | 行为 | 典型场景 |
|---|---|---|
| `failover`（默认） | 失败**换一台**重试（默认 2 次） | 读操作、幂等写 |
| `failfast` | 失败立即抛 | **非幂等写**（下单）——多试一次就重复扣款 |
| `failsafe` | 失败忽略，仅记日志 | 写审计日志、通知类旁路 |
| `failback` | 失败入队列，定时重发 | 消息最终一致（类似你 MQ 的重投） |
| `forking` | 并行调多台，取最快返回 | 实时性敏感、成本不敏感 |
| `broadcast` | 逐台全调，任一失败即异常 | 刷新所有节点的本地缓存 |

**对照本仓**：你的 Feign 默认不重试（`02-08` 记录了"重试只对幂等 GET"的取舍）+ Sentinel 熔断兜底——**等价于 failfast + 熔断降级**。所以面试被问 Dubbo 容错时，可以反手用项目语义回答。

---

## 五、隐式传参与链路上下文（与你的项目强相关）

跨服务要传"用户身份 / 链路 ID / 全局事务 ID"，RPC 框架都有"隐式上下文"：

| 框架 | 机制 | 本仓对应物 |
|---|---|---|
| Dubbo | `RpcContext.getContext().setAttachment(k,v)`，自动随 RPC 传播 | `X-User-Id/X-User-Roles` 头（网关透传）、`TX_XID`（Seata 02-07） |
| Feign | `RequestInterceptor` 手写 | 本仓 Seata 的 Feign XID 拦截器 |
| gRPC | `Context` + Metadata | 未使用 |

> 面试高频：**" attachment 在跨线程时会丢怎么办？"** —— 与你的 `TraceContextHolder.capture()/restore()` 同一答案：显式快照传递（Dubbo 3 提供 `Context` 异步传递支持）。

---

## 六、Dubbo 3 的两个关键演进（面试加分）

1. **应用级服务发现**：老版按"接口级"注册（一个应用 50 个接口 = 50 条注册记录，注册中心爆炸）；Dubbo 3 改为**应用级**（一个应用一条记录，接口列表存元数据）——数据量对齐 Spring Cloud，也让它能在 K8s 里与 Service 机制共存。
2. **Triple 协议**：基于 HTTP/2、兼容 gRPC，支持流式（Unary/Server-Stream/Bidi）——让"内部 Dubbo、外部 gRPC/REST"有望统一协议。

---

## 七、如果要引入本项目：评估结论（未落地）

| 维度 | 评估 |
|---|---|
| 收益 | 性能（二进制+长连接）、容错/路由治理内建、国内 JD 匹配 |
| 代价 | 11 个 FeignClient + 3 个 RestTemplate client 全部重写；py-chat（Python）不走 Dubbo，跨语言还得留 HTTP/gRPC；与现有 Sentinel/Seata 的适配要重验 |
| 结论 | **存量不动**；新学以"原理 + 面试口径"为目标。真要引入的信号：内部调用 QPS 高到 HTTP+JSON 成为瓶颈，或新团队以 Dubbo 技术栈为主 |

---

## 八、动手实验（可选，15 分钟）

```bash
# 官方示例最快路径：docker 起 Nacos + 跑 dubbo-samples 中的 api 示例
docker run -d --name nacos -p 8848:8848 nacos/nacos-server:v2.3.2
git clone https://github.com/apache/dubbo-samples && cd dubbo-samples/1-basic/dubbo-samples-api
# 按 README 用 zookeeper 或 nacos 作为注册中心跑 Provider/Consumer，观察控制台与 RpcContext
```

---

## 九、面试要点总结

```text
关键词：
RPC 四角色 + 本地地址缓存 · dubbo 协议（长连接/NIO/二进制头）· 序列化选型
Dubbo SPI：@SPI/@Adaptive/Wrapper（对照 Spring 条件装配）· 集群容错六策（配场景背）
RpcContext 隐式传参 ≈ 身份头/XID 传播 · Dubbo3 应用级发现 + Triple(HTTP/2,兼容gRPC)
项目锚点：Feign+RestTemplate 双轨（02-07）· Sentinel 熔断（02-08）· Seata XID 拦截器
结论口径：存量 Feign 不动；Dubbo 是"内部高密度调用+统一 Java 栈"场景的选项
```

## 学习检查清单

- [ ] 能画出四角色注册/订阅/调用时序
- [ ] 能解释 Dubbo SPI 相比 Java SPI 多了什么，并与 Spring 条件装配互译
- [ ] 能按场景给六个容错策略各配一个例子，并映射到本仓 Feign 的取舍
- [ ] 能说清隐式传参与本仓身份/XID 传播的对应关系
- [ ] 能给出"本项目为什么不引入 Dubbo"的工程口径

## 下一步

- [16-ZooKeeper 与 etcd](16-ZooKeeper与etcd分布式协调.md)：Dubbo 老搭档注册中心的 CP 派实现；
- [18-gRPC 与 Protobuf](18-gRPC与Protobuf跨语言调用.md)：跨语言方向的 RPC 答案。
