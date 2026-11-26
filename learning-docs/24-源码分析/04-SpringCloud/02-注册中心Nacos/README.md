# 02 · 注册中心 Nacos

> 一句话结论：注册中心是微服务的"通讯录"——服务启动时把自己写进去（注册），调用方按名字查出来（发现）；本目录把 Spring Cloud 抽象层、`com.alibaba.cloud` 适配层、`nacos-client` 通信层三层的源码串起来讲。
> 阅读前置：读完 [`../01-总览与版本/01-springcloud-体系与版本矩阵.md`](../01-总览与版本/01-springcloud-体系与版本矩阵.md)，知道本项目用的是 Spring Cloud 子项目 `5.1.0`（release train `2026.0.0`）+ Boot `4.2.0` + Nacos `3.3.0`。
> 预计阅读：20 分钟（本页）｜源码：`com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0`、`com.alibaba.nacos:nacos-client:3.3.0`、`org.springframework.cloud:spring-cloud-commons:5.1.0`

---

## 这个目录要解决什么问题

零基础同学第一次接触注册中心，通常有三个困惑：

1. **"服务已经启动成功了，为什么网关还是 503？"** —— 服务启动了 ≠ 注册成功了。注册发生在 `WebServerInitializedEvent` 之后，失败时**默认不抛异常**（`fail-fast=false`），启动日志一片祥和。
2. **"Nacos 控制台的实例列表是谁在维护？"** —— 是服务端的实例表，由客户端心跳 + 服务端健康检查共同维护；客户端本地还各存了一份缓存和磁盘快照。
3. **"我把实例停了，调用方为什么还在报连接它？"** —— 三层缓存叠加的延迟，不是 bug。

本目录用两篇文章回答：**实例怎么注册上去**、**列表怎么刷新到调用方**。

---

## 学习目标

读完本目录，你应该能做到：

- 说出 `AbstractAutoServiceRegistration#start()` 的完整动作序列，并指出注册失败时日志长什么样。
- 区分**临时实例（ephemeral）**与**持久实例**，并解释 Nacos 2.x 之后为什么临时实例只能走 gRPC。
- 说清 `NacosServiceRegistry#getNacosInstanceFromRegistration` 里 7 个字段各自来自哪个配置项。
- 画出"服务端推送 → `ServiceInfoHolder` → `Spring Cloud LoadBalancer` 缓存"的三层刷新链路，并估算"下线到生效"的时间。
- 找到本地快照目录 `~/nacos/naming/<namespace>/`，并解释 `namingLoadCacheAtStart` 的作用与风险。
- 用日志关键词（`nacos registry`、`current ips`、`Subscribe from Server failed`）在 10 分钟内定位注册/发现类故障。

---

## 文章顺序

```mermaid
flowchart LR
    A["01-总览与版本<br/>版本与六大件"] --> B["02-01<br/>服务注册与发现源码"]
    B --> C["02-02<br/>服务实例列表如何更新"]
    C --> D["03-负载均衡与调用<br/>（下一篇）"]
```

> 读图说明：
> 1. 先读 `02-01`，它负责"**实例怎么进去**"：`AbstractAutoServiceRegistration` → `NacosServiceRegistry` → `nacos-client` → `/nacos/v1/ns/instance`（或 gRPC `InstanceRequest`）。
> 2. 再读 `02-02`，它负责"**列表怎么出来**"：`NacosDiscoveryClient` → `ServiceInfoHolder` → `ServiceInstanceListSupplier`。
> 3. 两篇合起来才是一条闭环；只看注册不看发现，就会误以为"注册成功=调用成功"。

---

## 本目录文章

| # | 文章 | 一句话内容 |
| --- | --- | --- |
| 01 | [`01-nacos-服务注册与发现源码.md`](./01-nacos-服务注册与发现源码.md) | 从 `AbstractAutoServiceRegistration#start` 到 `NamingGrpcClientProxy#registerService`，讲清注册时机、失败策略、心跳、临时 vs 持久、三层隔离 |
| 02 | [`02-服务发现与拉取-服务实例列表如何更新.md`](./02-服务发现与拉取-服务实例列表如何更新.md) | 从 `NacosDiscoveryClient#getInstances` 到 `CachingServiceInstanceListSupplier`，讲清推送、缓存、本地快照与"下线后还有流量" |

---

## 依赖与边界

- **依赖**：`spring-cloud-commons:5.1.0`（`ServiceRegistry` / `AutoServiceRegistration` 抽象）、`spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0`（对接层）、`com.alibaba.nacos:nacos-client:3.3.0`（通信层）、`spring-cloud-loadbalancer:5.1.0`（消费实例列表）。
- **不在这里讲**：
  - Nacos **配置中心**、`@RefreshScope`、Bus 广播 → 见 [`../05-配置中心与Bus/README.md`](../05-配置中心与Bus/README.md)。
  - Nacos **进阶**（命名空间治理、鉴权、`NacosLoadBalancer` 权重/就近、集群拓扑） → 见 [`../08-Alibaba生态详解/README.md`](../08-Alibaba生态详解/README.md)。
  - 熔断降级（注册失败后的业务兜底） → 见 [`../06-熔断限流与容错/README.md`](../06-熔断限流与容错/README.md)。
- **版本提醒**：本仓库基线是 Spring Cloud 子项目 `5.1.0`（release train `2026.0.0`）、Boot `4.2.0`。网上大量教程基于 `2021.0.x` + Nacos 1.4，**抓包位置、配置文件内容、starter 坐标都可能不一样**，请以本文引用的源码为准。

---

## 学完能做到什么

- 能对着日志判断"注册到底成功没有"，并说出下一步该看哪一行日志。
- 能解释 `service.port has not been set`、`No service to register for nacos client`、`Subscribe from Server failed` 三条报错各自的原因。
- 能设计一次**优雅下线**（先 `deregister`、等待大于缓存时延、再停进程），把发布期间的 `ConnectException` 降到 0。
- 能读懂 `nacos-client` 里 `NamingService` → `NamingClientProxyDelegate` → `NamingGrpcClientProxy` 的分层，遇到通信类问题知道该调哪个日志级别。

---

## 学习检查点

1. `AbstractAutoServiceRegistration` 什么时候被触发？为什么是 `WebServerInitializedEvent` 而不是 `ApplicationReadyEvent`？
2. `NacosAutoServiceRegistration#register()` 里 port `< 0` 的那段在补什么？
3. `fail-fast=true` 与 `false` 在注册失败时的行为差异？生产该选哪个？
4. 临时实例的心跳由谁发、间隔多少、超时与删除延迟各是多少？
5. `ServiceInfoHolder` 与 `CachingServiceInstanceListSupplier` 各存了什么、TTL 多长？
6. 本地快照目录怎么算出来？什么情况下调用方会用上它？
