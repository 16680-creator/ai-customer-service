# ServiceMesh 与 Istio 边界

> 对应项目：`ai-cs-chat/pom.xml:203`（spring-cloud-starter-alibaba-sentinel）、`ai-cs-chat/pom.xml:116-125`（resilience4j-spring-boot3 + timelimiter）、`ai-cs-chat/.../config/Resilience4jConfig.java`、`ai-cs-gateway/.../filter/TokenBucketRateLimiter.java`（自研令牌桶）。
> 相关：[05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md) §3.3 C 类"有意不引入"（本篇延续该决策记录格式）、[04-K8s网络模型与Service](./04-K8s网络模型与Service.md)（Mesh 建立在 CNI 之上）、[11-数据结构与算法/10-限流算法](../11-数据结构与算法/10-限流算法-四大算法与网关实现.md)（自研限流的算法底座）。
> ⚠️ **工程未落地，本篇为决策记录**：项目无 Istio/Istiod/sidecar 任何踪迹（grep 零命中），本篇回答"要不要引入"，结论是**暂不引入并给出触发条件**——与缺口文档 C 类决策风格一致，防止反复横跳。

---

## 一、ServiceMesh 是什么：sidecar vs ambient

Mesh 把"服务间通信"（路由、重试、mTLS、观测）从应用进程挪到基础设施层：

| 维度 | sidecar 模式（Istio 经典） | ambient 模式（Istio 1.22+） |
|---|---|---|
| 形态 | 每 Pod 注入一个 envoy 容器 | 每节点 ztunnel（L4）+ 按需 waypoint（L7） |
| 资源开销 | 每 Pod 常驻 ~0.1C/128Mi 量级 | 节点级共享，Pod 无感 |
| 升级 | 跟随 Pod 重启注入新 sidecar | 节点级升级，业务无感 |
| 适用 | 需要完整 L7 能力（路由/故障注入/鉴权细分） | 先要 mTLS+遥测、L7 渐进开启 |

Istio 四个招牌能力速览：

| 能力 | 做什么 | 一句话理解 |
|---|---|---|
| mTLS | 服务间双向认证 + 加密，零代码 | 网络层"自动 HTTPS" |
| 金丝雀 | 按 header/权重把流量分到 v2 | 发布不靠回滚靠灰度 |
| 流量镜像 | 生产流量复制一份发给新版本（不影响响应） | 用真实流量做验证 |
| 故障注入 | 人为注入延迟/5xx 测韧性 | 混沌工程的流量层实现 |

### 1.3 Istio 的最小概念集（够决策即可）

```text
┌───────────────────────────────────────────────┐
│ 控制面 istiod（单实例）                        │
│  - 下发证书（SPIFFE 身份）与路由配置（xDS）     │
└──────────────┬────────────────────────────────┘
               │ gRPC 下发
┌──────────────▼────────────────────────────────┐
│ 数据面：每个注入的 Pod = 业务容器 + envoy 容器  │
│  入站拦截 → 出站拦截 → 按 VirtualService 转发  │
└───────────────────────────────────────────────┘
```

| CRD | 管什么 | 本项目若引入的对应物 |
|---|---|---|
| VirtualService | 路由规则（按 header/权重分流、超时、重试、故障注入） | ai-chat-service 的灰度分流规则 |
| DestinationRule | 目标子集划分（v1/v2）与负载均衡策略 | 按 Deployment 版本打 label 划 subset |
| Gateway | Mesh 边缘入口（可与 Ingress 共存或替代） | 现状由 api-gateway NodePort 承担 |
| PeerAuthentication | mTLS 模式（STRICT/PERMISSIVE/DISABLE） | PERMISSIVE 起步灰度切 STRICT |
| AuthorizationPolicy | L7 服务间鉴权（谁可以调谁） | 对应 NetworkPolicy 的 L7 加强版 |

**记忆口径**：VirtualService 定"流量怎么走"，DestinationRule 定"目的地有哪几版"，两者成对出现；PeerAuthentication/AuthorizationPolicy 定"谁能进"，是安全面。

### 1.4 mTLS 的三个模式与切换策略

| 模式 | 行为 | 用途 |
|---|---|---|
| `PERMISSIVE` | 明文与 mTLS 都接受 | **灰度期**：未注入 sidecar 的服务照常通信 |
| `STRICT` | 只接受 mTLS | 全量注入后的终态 |
| `DISABLE` | 关闭 | 排障/对接外部系统 |

切换路径永远是 PERMISSIVE → 观察（哪些服务还在明文）→ 逐 namespace 收紧 STRICT——一次到位 STRICT 会把所有未注入的业务瞬间打断。

---

## 二、先盘点：本项目已经有什么治理能力

### 2.1 治理栈全景（一张图看清三层各管什么）

```text
                ┌─────────────────────────────────────────────┐
   南北向入口    │  api-gateway（Spring Cloud Gateway）         │
                │  AuthFilter / RateLimitFilter（自研令牌桶、   │
                │  滑动窗口）                                  │
                └──────────────────┬──────────────────────────┘
                                   │
                ┌──────────────────▼──────────────────────────┐
   应用内治理    │  各服务进程内                                │
                │  chat: Sentinel（pom:203）+ Resilience4j     │
                │       （pom:116-125，模型路由熔断/重试/超时）  │
                │  order: Feign CircuitBreaker（yml:26-31）     │
                └──────────────────┬──────────────────────────┘
                                   │
                ┌──────────────────▼──────────────────────────┐
   基础设施层    │  Nacos 注册发现/负载均衡（8 份清单 + 8 个服务  │
                │  application.yml 各 2 处，全项目 24 处注入点）  │
                │  ⚠️ 本层目前几乎空缺：无 mTLS、无实例级摘除、  │
                │  无流量层灰度 —— 这正是 Mesh 若要填的位置      │
                └─────────────────────────────────────────────┘
```

逐项核实过的现状（file:line 均真实）：

| 能力 | 实现 | 位置 | 生效范围 |
|---|---|---|---|
| 限流 | 自研令牌桶/滑动窗口 | `ai-cs-gateway/.../filter/TokenBucketRateLimiter.java`、`SlidingWindowRateLimiter.java`（经 `RateLimitFilter` 挂网关责任链） | 网关层，IP/路径维度 |
| 限流/熔断 | Alibaba Sentinel | `ai-cs-chat/pom.xml:203`（spring-cloud-starter-alibaba-sentinel）；dashboard 在 `docker-compose.yml:256`（sentinel-dashboard） | chat 应用内 |
| 熔断/重试/超时 | Resilience4j | `ai-cs-chat/pom.xml:116-125` + `ai-cs-chat/.../config/Resilience4jConfig.java`（COUNT_BASED 窗口 10/失败率 50%/OPEN 30s） | chat 对模型路由的调用 |
| Feign 熔断开关 | OpenFeign + CircuitBreaker | `ai-cs-order/src/main/resources/application.yml:26-31`（注释明确"所有 Feign 调用自动包裹 Resilience4j CircuitBreaker"，生效于 productClient/payClient） | order 的跨服务调用 |
| 注册发现/负载均衡 | Nacos | `NACOS_ADDR` 注入：8 份 K8s 清单各 1 处 + 8 个服务 application.yml 各 2 处（discovery+config），共 24 处 | 全部 Java 服务 |
| 鉴权 | 网关 AuthFilter + JWT | `ai-cs-gateway/.../filter/AuthFilter.java` | 入口层 |

**结论先行**：限流、熔断、重试、超时、发现这五样，应用层/网关层都已各自有一套实现——Mesh 最常见的卖点本项目**大半已经有了**。

一句话：**Mesh 不是"更高级的 Sentinel"**，它接管的是本项目三层治理图（§2.1）里最下面那层——基础设施层的通信治理。判断要不要引入，本质是判断"最下面那层空着"是不是当前的真痛点。

---

## 三、职责边界矩阵：重叠还是互补

| 能力 | Sentinel/R4j（应用内） | 网关自研限流 | Nacos | Mesh 能补什么 | 判定 |
|---|---|---|---|---|---|
| 限流 | ✅ 方法/资源维度 | ✅ 入口维度 | — | 入口限流（envoy local rate limit） | **重叠**，不必换 |
| 熔断 | ✅（R4j/Feign） | — | — | outlier detection（实例级摘除） | 部分互补（Mesh 摘实例，R4j 熔调用） |
| 重试/超时 | ✅（R4j TimeLimiter、Feign 配置） | — | — | ✅ 路由级重试 | **重叠** |
| 服务发现 | — | — | ✅ | ✅（基于 K8s Service） | **重叠**，且双注册体系要防打架（见 04 篇 §3.2） |
| mTLS | ❌ 应用内明文 HTTP | ❌ | ❌ | ✅ **核心增量** | 互补，唯一硬缺口 |
| 金丝雀/流量镜像 | ❌ | ❌（无按版本路由） | ❌ | ✅ **核心增量** | 互补（缺口文档 B 类"灰度发布"行的另一条实现路径） |
| 故障注入 | ❌ | ❌ | ❌ | ✅ | 互补（演练工具，缺口文档 B 类"故障演练"行） |
| 全链路遥测 | 部分埋点 | 部分埋点 | — | ✅ 自动（L7 span） | 互补，但项目已有 OTel+Tempo（部分重复） |

两个矩阵之外的细节：① order 的 Feign 熔断（`application.yml:26-31`）覆盖了 productClient/payClient 两个跨服务调用——这正是 Mesh outlier detection 会"接管"的位置，若引入 Mesh 必须先决定听谁的；② Sentinel 的规则目前主要围绕模型路由场景（chat 对 LLM 的调用），Mesh 完全管不到 LLM 出口流量（那是南北向出站），不要指望 Mesh 覆盖。

**边界原则**：Mesh 管东西向（服务间）流量的基础设施层；Sentinel/R4j 管应用内调用语义；网关管南北向入口。三者可共存，但同一能力重复上两遍（比如网关限流 + Mesh 限流叠加）会变成排障噩梦——**每层只留一个权威实现**。

---

## 四、Mesh 能补什么，代价是什么

### 4.1 三个真实增量（如果引入，价值在这）

1. **mTLS**：本项目 8 份清单里 MySQL 密码曾是明文 env（`ai-chat-service.yaml:55-56`）、Nacos 无认证——内部流量全是明文 HTTP。Mesh 能零代码补上传输加密，但**它治不了配置治理问题**（那是 [07 篇](./07-多环境配置与漂移治理.md)的事）。
2. **金丝雀**：缺口文档 B 类"灰度发布与网关高级路由"行的 Mesh 实现路径——不用改网关路由代码，VirtualService 按 header 分流。但注意该行的立项方案是"Nacos 元数据 + 网关路由断言"，**先用已有组件实现**，Mesh 是备选而非必选。
3. **故障注入/镜像**：给 B 类"故障演练"行提供流量层工具。

### 4.2 目标态示例：ai-chat-service 的金丝雀（若引入 Mesh，规则长这样）

```yaml
# DestinationRule：把 ai-chat-service 的实例分成 stable / canary 两个子集
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: ai-chat-service
  namespace: ai-customer-service          # 与 ai-chat-service.yaml:6 同名空间
spec:
  host: ai-chat-service
  subsets:
    - name: stable
      labels: { version: stable }
    - name: canary
      labels: { version: canary }
---
# VirtualService：带灰度头走新版本，其余按 90/10 权重分流
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ai-chat-service
  namespace: ai-customer-service
spec:
  hosts: [ai-chat-service]
  http:
    - match:
        - headers: { x-canary: { exact: "true" } }
      route: [{ destination: { host: ai-chat-service, subset: canary } }]
    - route:
        - { destination: { host: ai-chat-service, subset: stable }, weight: 90 }
        - { destination: { host: ai-chat-service, subset: canary }, weight: 10 }
```

对照思考：同样的灰度，不用 Mesh 的实现是网关路由断言 + Nacos 元数据打标（缺口文档 B 类"灰度发布"行的立项方案），要在 `ai-cs-gateway/.../filter/` 里新增一个灰度 Filter；Mesh 方式的优势是**规则是配置不是代码**，劣势是多一套要运维的体系。这正是 §五 决策的实质权衡。

### 4.3 三个真实代价

| 代价 | 对本项目的具体影响 |
|---|---|
| 运维复杂度 +1 | 要养 Istiod/ztunnel/envoy 排障能力；8 份清单的 tcpSocket 探针（见 [02 篇](./02-K8s资源治理与弹性伸缩.md)）在 sidecar 下还要考虑 holdApplicationUntilProxyReady，否则启动竞态 |
| 资源 +1 | 11 个服务 × sidecar 常驻开销，学习集群（三台虚拟机）扛不扛得住要先算账 |
| 排障链路 +1 | 一次失败调用要横跨 业务代码 → 应用内熔断 → envoy → kube-proxy/CNI 四层日志，学习曲线陡 |
| 双注册体系冲突 | Nacos 发现 + Mesh（K8s Service）发现并存，摘流/灰度语义要重理（04 篇 §3.2 的双轨问题被放大） |
| CNI 前置 | NetworkPolicy/mTLS 体验完整需要 Calico/Cilium（04 篇 §1.2），当前学习集群默认 CNI 不满足 |

---

## 五、决策清单：本项目什么时候才值得引入 Mesh

延续 [05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md) §3.3 的格式，落一条 C 类决策：

> **决策：暂不引入 ServiceMesh。** 依据：限流/熔断/重试/发现已有四套应用层实现覆盖（§二）；服务全部同构 Java（11 个 Spring 服务 + 1 个薄 Python 服务），无多语言治理痛点；无 mTLS 合规要求；学习集群资源有限。收益的三项（§4.1）各有更轻的替代路径。
>
> 决策日期 2026-09；复核触发：下表任一条件出现两条，或第六条单独出现。

**触发条件清单（满足任意两条再评估）**：

| # | 触发条件 | 对应信号 |
|---|---|---|
| 1 | 合规/安全要求服务间 mTLS | 安全审计报告、等保测评 |
| 2 | 服务数 >20 且跨 ≥2 种语言栈 | 新语言服务占比上升，SDK 治理成本失控 |
| 3 | 金丝雀成为常态发布需求 | 每周多版本发布、需要按 header/权重自动分流 |
| 4 | 出现专职平台团队 | 有人对 Mesh 控制面负责（否则 Istio 必然荒废） |
| 5 | 故障演练制度化 | B 类"故障演练"行升级为例行机制，需要流量层注入 |
| 6 | Spring Cloud Alibaba 治理组件弃用 | Sentinel/R4j 退场，治理能力出现真空需要承接 |

**替代路径（比 Mesh 便宜，先做）**：mTLS 缺口先靠"中间件认证 + NetworkPolicy 默认拒"（[04 篇](./04-K8s网络模型与Service.md) §五）；灰度先按缺口文档立项的"Nacos 元数据 + 网关路由断言"；故障演练先写脚本（缺口文档 B 类行）。

**决策复核节奏**：本决策不是"永久关闭"，建议每半年按上表重新打分一次，或在触发条件 6（Spring Cloud Alibaba 治理组件退场）出现时立即复核。复核时重读 §三 的矩阵——若"Mesh 能补什么"一栏的增量仍只有 mTLS 一项硬需求，结论大概率还是"再等等"；若金丝雀/多语言/合规同时命中，则启动 PoC（先 ambient 后 sidecar，先 PERMISSIVE 后 STRICT）。

---

## 面试高频问答

**Q1：什么是 ServiceMesh？解决什么问题？**
A：把服务间通信（路由、重试、mTLS、遥测）下沉到基础设施层。sidecar 模式给每个 Pod 注入 envoy 代理，应用只管业务逻辑；收益是语言无关、零侵入，代价是多一层要运维的基础设施。

**Q2：sidecar 和 ambient 模式的区别？**
A：sidecar 每 Pod 常驻代理，资源随 Pod 线性增长；ambient 用节点级 ztunnel 做 L4（mTLS/遥测），需要 L7 时按 namespace 挂 waypoint。ambient 大幅降低小流量的资源开销，升级也无需重启业务 Pod。

**Q3：Mesh 的限流熔断和 Sentinel/Resilience4j 什么关系？**
A：能力重叠、层次不同。应用内组件（Sentinel/R4j）管方法级调用语义、与应用生命周期一致；Mesh 管实例级/路由级（outlier detection、路由重试）。同时上两套要明确每层的权威实现，否则同一故障两处都在"修"，排障互相干扰。

**Q4：mTLS 解决什么问题，和应用层加密什么关系？**
A：mTLS 在传输层做双向证书认证+加密，零代码防窃听与伪造来源；应用层加密（如字段加密、JWT 鉴权）解决的是内容与身份语义问题，两者互补不替代。mTLS 是 Mesh 最难用其他方式廉价补齐的能力。

**Q5：金丝雀发布怎么用 Mesh 实现？不用 Mesh 行不行？**
A：Mesh 方式是 VirtualService 按 header/权重分流到两个版本 subset，配合流量镜像验证。不用 Mesh 也行：网关路由断言 + Nacos 元数据打标（本项目缺口文档的立项方案）、或 K8s 原生双 Deployment + Service 选择器切换。Mesh 的优势是灰度规则与代码彻底解耦、支持按比例精细控制。

**Q6：引入 Istio 前应该先评估什么？**
A：① 治理能力是否已有等价实现（本项目 Sentinel/R4j/自研限流已覆盖大半）；② 服务规模与语言异构度；③ CNI 是否支持策略（NetworkPolicy/mTLS 体验）；④ 团队是否有专职运维控制面；⑤ 资源开销账。评估结果是"暂缓"也没关系——有明确的触发条件即可。

**Q7：Mesh 和 API 网关（如本项目的 Spring Cloud Gateway）什么关系？**
A：网关管南北向（外部→集群）流量，Mesh 管东西向（服务间）流量，典型架构是两者叠加。Mesh 不能替代网关的业务鉴权/限流逻辑，网关也不该承担服务间治理。

**Q8：为什么说"双注册体系"（Nacos + K8s Service）上 Mesh 要特别小心？**
A：Mesh 基于 K8s Service 的 endpoints 做路由与摘流，而本项目业务流量走 Nacos 发现——摘流、灰度按哪套实例列表生效会出现两套语义。引入 Mesh 前必须先统一注册体系（收敛到 K8s Service 或让 Mesh 感知 Nacos），否则灰度状态不可信。

**Q9：sidecar 注入是怎么发生的？能不能只给部分服务注入？**
A：namespace 打 `istio-injection=enabled` 标签（或 Pod 模板注解 sidecar.istio.io/inject）后由 webhook 自动注入，也可全关、逐 Pod 注解开启——所以灰度注入完全可行：先给 ai-chat-service 一个服务注入（PERMISSIVE 下不影响其他服务），观察稳定再扩散。注入是数据面动作，istiod 不参与运行时数据转发，控制面挂了已有连接仍通（新配置不下发而已）。

---

## 动手练习

1. 画出本项目治理能力分布图：把 §二 表格的六项能力标到"网关层/应用层/基础设施层"三栏图上，回答"哪一层最薄"（提示：基础设施层）。
2. 用 Istio 演示环境（或 istioctl install --set profile=demo 到 k3d 集群）给一个 demo 服务注入 sidecar，`istioctl proxy-config` 查看 envoy 配置——直观感受"路由规则变成了 xDS 下发"。
3. 用 VirtualService 给 ai-chat-service 写一份"header `X-Canary: true` 走新版本"的分流规则（目标态练习，不必真实两版本），并回答：没有 Mesh 时，同样的分流要在本项目哪个组件里实现？改多少代码？
4. 写一份"Mesh 触发条件评估报告"：对照 §五 六条触发条件逐条给出本项目的当前信号（有/无/证据），形成可存档的决策记录。
5. 反向练习：假设 Sentinel 被弃用（触发条件 6 成立），列出需要从 Sentinel 迁移到 Mesh 的具体配置项（限流规则、熔断窗口），估算迁移工作量，写进决策记录的"预案"栏。
6. 用 `kubectl get pods -n istio-system` 观察 demo profile 装完后的组件（istiod、ingressgateway），估算这套控制面 + 每Pod 一个 sidecar 的内存开销，对照 §4.3 的代价清单形成体感。

---

> 上一篇：[04-K8s网络模型与Service](./04-K8s网络模型与Service.md) ｜ 下一篇：[06-镜像构建优化与安全扫描](./06-镜像构建优化与安全扫描.md)
