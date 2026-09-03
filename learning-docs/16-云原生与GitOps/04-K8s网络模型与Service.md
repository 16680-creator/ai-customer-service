# K8s 网络模型与 Service

> 对应项目：`deploy/k8s/services/api-gateway.yaml:10-15`（全项目唯一 NodePort 入口，nodePort 30080）及其余 7 份 ClusterIP 清单。
> 相关：[07-运维部署/02-Kubernetes入门](../07-运维部署/02-Kubernetes入门.md) §6（外部请求进 NodePort、集群内两条互访路径——本篇不再重复，只写 CNI/kube-proxy/CoreDNS/Ingress/NetworkPolicy 的纵深）、[03-GitOps与ArgoCD](./03-GitOps与ArgoCD.md)（网络配置也是交付物）。
> ⚠️ Ingress/Gateway API/NetworkPolicy 部分**工程未落地**（`deploy/k8s/` 下 grep `kind: Ingress|NetworkPolicy` 零命中），为目标态设计；Service/入口部分为现状盘点。

---

## 一、Pod 网络：扁平模型与 CNI

### 1.1 K8s 网络三条基本原则

```text
① 每个 Pod 一个独立 IP（Pod 内容器共享网络栈，localhost 互访）
② 所有 Pod 之间无需 NAT 直接互通（跨节点也通）
③ Node 与 Pod 之间也无需 NAT 互通
```

**谁来实现"跨节点 Pod 互通"？——CNI 插件**。kubelet 只负责调用 CNI 二进制给 Pod 挂网，不通不通它不管。

### 1.2 CNI 概览（怎么选）

| 插件 | 模式 | 特点 |
|---|---|---|
| Flannel | overlay（VXLAN） | 最简单，无网络策略；k3s 默认 |
| Calico | BGP 路由（或 overlay） | 性能好，NetworkPolicy 支持完整 |
| Cilium | eBPF | 高性能 + L7 策略 + 可观测，云原生新贵 |

本项目本地集群三个候选（`07-运维部署/02-Kubernetes入门.md` §7.0：Minikube / k3d / 三台 VirtualBox 装 k3s）默认分别来自内置 CNI 与 Flannel——学习环境够用；**若做 [05 篇](./05-ServiceMesh与Istio边界.md) 的 Istio 预研，建议换 Calico 或 Cilium**（NetworkPolicy 语义完整，Istio 与 eBPF CNI 配合也最顺）。

选型一句话：**先问自己要不要 NetworkPolicy/mTLS 的内核级支持**——要就 Calico/Cilium，不要就 Flannel（最省心）。CNI 是集群装完最难换的组件之一，起步前想清楚。

---

## 二、Service 与 kube-proxy：iptables vs IPVS

### 2.1 为什么需要 Service

Pod IP 随重建而变、副本是多个——需要**稳定 VIP + 负载均衡**，这就是 Service（ClusterIP）。实现者不是 Service 对象本身，而是每台节点的 **kube-proxy** 把 VIP 规则写进内核。

### 2.2 四种类型对号入座（本项目全部命中前两种）

| 类型 | 本项目用例 | 访问范围 |
|---|---|---|
| ClusterIP | 7 份清单（user/knowledge/ai-chat/search/message/notify/product） | 仅集群内 |
| NodePort | `api-gateway.yaml:10-15`（8080 → **30080**） | 集群外经 `<任意节点IP>:30080` 进 |
| LoadBalancer | 未用 | 云厂商 LB 自动绑定 |
| ExternalName | 未用 | DNS CNAME 转发 |

现状流量拓扑：

```text
外部用户 ──> 节点IP:30080 (NodePort, iptables DNAT)
              └─> api-gateway Pod:8080        # 唯一入口，网关内再做路由
集群内服务 ──> user-service:8081 (ClusterIP)   # 7 份 ClusterIP 供内部互访/探针
```

以现有 8 份清单为参照的 Service 字段速查（写新清单时对着抄）：

| 字段 | 现状取值 | 说明 |
|---|---|---|
| `spec.type` | ClusterIP（7 份）/ NodePort（网关） | 内部互访 vs 外部入口 |
| `ports.port` / `targetPort` | 相同（8080-8088） | VIP 端口与容器端口一致是本项目的简化写法 |
| `ports.nodePort` | 30080（仅网关，`api-gateway.yaml:14`） | 范围默认 30000-32767 |
| `ports.name` | `http` | 命名端口，探针/协议识别可引用 |
| `selector` | `app: <服务名>` | 与 Deployment 模板标签严格一致，不一致则 endpoints 为空 |

**endpoints 排障口诀**：Service 通不通，先看 `kubectl get endpoints <svc> -n ai-customer-service`——为空说明 selector 与 Pod 标签没对上，或 readiness 还没放行；本模块 [08 篇](./08-补全order-pay-mq的K8s清单.md) 的新清单就靠这条口诀验证。

### 2.3 iptables vs IPVS（kube-proxy 两种模式）

| 维度 | iptables 模式（默认） | IPVS 模式 |
|---|---|---|
| 实现 | 线性遍历规则链做 DNAT | 内核态哈希表 + 负载均衡算法 |
| 规模 | 服务多了 O(n) 遍历，规则更新慢（全量刷） | O(1) 查找，增量更新，数千服务无压力 |
| 负载均衡 | 随机/概率 | rr、lc、sh（源地址会话保持）等 8 种 |
| 排障 | `iptables-save \| grep <svc>` | `ipvsadm -Ln` |

**判断依据**：服务 <100 个用默认 iptables 足够；本项目 11 服务 → 无需切换，但面试要能讲出"为什么大集群要 IPVS"。切换方式：k3s 加 `--kube-proxy-arg proxy-mode=ipvs`（仅作了解）。

**一个常被问到的细节**：NodePort 的流量会不会再绕一层？——外部流量 DNAT 到 Pod 后**直接回给客户端**（不经 Service VIP 回程），这叫 externalTrafficPolicy 语义；设成 `Local` 可避免二次跳转（保留源 IP），代价是只在有该 Service Pod 的节点上开 NodePort。

一份数据包的完整路径（外部请求 `curl 节点IP:30080` 为例）：

```text
客户端
  └─> 节点网卡 :30080（NodePort 监听，kube-proxy 写入的 iptables/IPVS 规则）
        └─ DNAT: 目的改写为某个 api-gateway Pod IP:8080
              └─> Pod 内 envoy?（本项目无）→ 直接是网关容器
                    └─ 响应包源地址仍是 节点IP:30080（SNAT 回写），客户端无感
集群内 curl http://user-service:8081 时同理：
  └─ DNS 解析 ClusterIP → iptables/IPVS 按 endpoints 概率/哈希选中一个 Pod → DNAT → Pod
```

---

## 三、CoreDNS：Service 怎么变成名字

### 3.1 集群内 DNS 规则

```text
<svc>.<namespace>.svc.cluster.local
例：user-service.ai-customer-service.svc.cluster.local
同 namespace 内可简写：http://user-service:8081/actuator/health
```

CoreDNS 以 `ClusterIP` 形式部署在 kube-system，每个 Pod 的 `/etc/resolv.conf` 指向它。验证：

```bash
kubectl run tmp --rm -it --image=curlimages/curl -n ai-customer-service -- sh
# 容器内：
nslookup user-service
curl http://user-service:8081/actuator/health    # 同 07-运维部署/02 §7.3 的验证方式
```

### 3.2 本项目的"双轨寻址"（特色，也是考点）

服务间调用其实有**两套并行的机制**：

| 机制 | 依赖 | 本项目角色 |
|---|---|---|
| K8s Service + CoreDNS | kube-proxy/CoreDNS | 基础设施互访（pod 连 mysql/nacos）、探针目标、无 SDK 的组件 |
| Nacos 注册发现 | 应用内 SDK，`NACOS_ADDR` 注入（7 份清单全有，如 `ai-chat-service.yaml:43-46`） | 业务 Feign/负载均衡：服务列表来自 Nacos，不经过 K8s Service |

**推论**：某个业务服务即使没有 ClusterIP 也能被别的服务调到（走 Nacos）；但没有 ClusterIP 就过不了探针/连不上中间件。反过来，**K8s Service 的 endpoints 摘流（readiness）只对"走 Service 的流量"生效**——走 Nacos 的流量摘流靠 Nacos 自身的心跳下线，这正是 [02 篇](./02-K8s资源治理与弹性伸缩.md) readiness 语义在本项目的适用边界，两个注册体系要分开治理。

---

## 四、Ingress vs Gateway API（⚠️ 未落地，目标态）

### 4.1 现状：没有 Ingress，入口全靠 NodePort + 网关自身路由

`deploy/k8s/` 下无任何 Ingress 资源（grep 零命中）。所有外部流量从 `:30080` 直进网关，路由/鉴权/限流都在 Spring Cloud Gateway 里做（`ai-cs-gateway/.../filter/AuthFilter.java`、`RateLimitFilter.java`）。**这没问题**——单集群单入口时，L4 直通 + 网关应用层路由是最简单的可用架构；Ingress 要解决的是"L7 入口从应用里解耦出来"。

### 4.2 Ingress 是什么（目标态示例）

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: aics-ingress
  namespace: ai-customer-service
spec:
  ingressClassName: nginx                  # 依赖安装 ingress-nginx controller
  rules:
    - host: aics.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-gateway          # 仍然把流量交给网关，Ingress 只做 L7 前置
                port: { number: 8080 }
```

价值排序（对本项目）：TLS 终结（证书集中管理）> 域名化入口（摆脱 `节点IP:30080`）> 多入口路由。**不要**用 Ingress 取代网关业务路由——限流/鉴权仍在 Gateway 过滤器里，Ingress 只做前置。

### 4.3 Gateway API 一句话

Ingress 的换代标准（SIG-Network）：角色拆分（GatewayClass/Gateway/HTTPRoute）、跨 namespace 引用、更丰富的路由语义。本项目暂无多团队/多租户入口诉求，**学习关注即可，不必迁移**——Ingress 生态更成熟，够用十年。

---

## 五、NetworkPolicy：从"默认全通"到"默认拒"（⚠️ 未落地，目标态）

K8s 网络默认**无任何隔离**：任何 Pod 可访问任何 Pod。NetworkPolicy 用标签选择器收紧，且是**加法叠加**：一旦有 policy 选中某 Pod，未明确允许的流量全部拒绝。

### 5.1 目标态第一步：默认拒出 namespace，再逐条放行

```yaml
# ① 默认拒：本 namespace 内未声明的入口全拒
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
  namespace: ai-customer-service
spec:
  podSelector: {}          # 选中 namespace 内全部 Pod
  policyTypes: [Ingress]
---
# ② 放行：网关 → 各业务服务
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-gateway
  namespace: ai-customer-service
spec:
  podSelector: {}
  policyTypes: [Ingress]
  ingress:
    - from:
        - podSelector:
            matchLabels: { app: api-gateway }    # 对应 api-gateway.yaml:8 的标签
```

### 5.2 收益与前提

| 收益 | 说明 |
|---|---|
| 爆炸半径收敛 | 就算某个 Pod 被攻破，也摸不到非授权服务（与 [09-安全与设计模式](../09-安全与设计模式/04-设计模式在微服务中的实战.md) 的应用层防护形成纵深） |
| 拓扑即文档 | 谁能调谁，写在 YAML 里而不是口头约定 |

### 5.3 别忘了出口方向：egress 策略

本项目服务大量依赖**集群外出口**——调用 OpenAI 兼容 API（`OPENAI_BASE_URL` 经 Secret 注入，`ai-chat-service.yaml:66-70`）、Embedding 服务（`aics-shared.yml:22` 的 `https://api.siliconflow.cn`）。默认拒入口（ingress）不影响出口；但若做出口收敛（egress 默认拒），要把这些 FQDN/IP 加白名单，否则 LLM 调用全部超时——ingress 收紧先行、egress 梳理流量后再动，是稳妥顺序。

**前提**：CNI 必须支持 NetworkPolicy（Flannel 不支持，Calico/Cilium 支持）——这就是 §1.2 说"预研 Mesh 前建议换 CNI"的原因之一。落地顺序：先 observe（Cilium Hubble 看真实流量）→ 再定策略 → 最后默认拒，一步到位默认拒会把业务打断。

---

## 面试高频问答

**Q1：K8s 网络模型的三条基本原则是什么？**
A：每 Pod 一个 IP；Pod 间（跨节点）无 NAT 互通；Node 与 Pod 间无 NAT 互通。实现"跨节点互通"的是 CNI 插件，kube-proxy 只负责 Service VIP 的转发规则。

**Q2：Service 的 ClusterIP 是怎么实现负载均衡的？**
A：ClusterIP 是 iptables/IPVS 里的虚拟 IP 规则，kube-proxy 在每个节点把它 DNAT 到某个后端 Pod。iptables 模式随机概率选择、线性匹配规则；IPVS 模式内核哈希表 + 多种均衡算法，大规模集群性能更好。

**Q3：iptables 和 IPVS 模式怎么选？**
A：小规模（<100 服务）iptables 默认即可；大规模用 IPVS（O(1) 查找、增量更新、支持会话保持）。本项目 11 服务无需切换。

**Q4：Pod 崩了重启后 IP 变了，调用方怎么找到它？**
A：两条路：集群内 DNS（CoreDNS 解析 Service 名，endpoints 自动跟随 Pod 变化）；或应用层注册中心（本项目 Nacos，服务上线/下线靠心跳与 SDK 拉取）。本项目两者并存——K8s Service 管基础设施互访与探针，Nacos 管业务调用。

**Q5：readiness 摘流对 Nacos 调用方生效吗？**
A：不生效。摘流只修改 Service 的 endpoints，影响"走 Service VIP"的流量；走 Nacos 的业务调用靠 Nacos 心跳下线。双注册体系下，摘流语义要按流量路径分别治理。

**Q6：Ingress 和 NodePort 什么关系？**
A：Ingress 是 L7 规则（按 host/path 分流、TLS 终结），它自己不能进流量，需要一个 Controller（如 ingress-nginx）落地，而 Controller 的流量入口通常恰恰是 LoadBalancer/NodePort。所以是"NodePort 进门、Ingress 分诊"，不冲突。

**Q7：NetworkPolicy 是默认允许还是默认拒绝？**
A：默认全允许；一旦有 NetworkPolicy 的 podSelector 选中某个 Pod，未在 policy 里明确允许的流量即被拒绝（策略是加法叠加，"选中即收紧"）。

**Q8：Flannel 集群里建了 NetworkPolicy 为什么不生效？**
A：Flannel 不实现 NetworkPolicy API，规则只是存着没人执行。需要 Calico/Cilium 这类支持策略的 CNI。排查第一步永远是确认 CNI 能力，而不是怀疑 YAML。

---

## 动手练习

1. 在集群里起临时容器（`kubectl run tmp --rm -it --image=curlimages/curl`），分别用 `user-service`、`user-service.ai-customer-service.svc.cluster.local`、Pod IP 三种方式访问 `/actuator/health`，验证 §3.1 的解析规则。
2. 把 api-gateway 的 Service 改成 `externalTrafficPolicy: Local`，从集群外 curl 观察：有网关 Pod 的节点能通、没有的节点超时——理解 Local 语义的代价。
3. 查看当前 kube-proxy 模式（`kubectl get cm kube-proxy -n kube-system -o yaml \| grep mode`），写一段 200 字论证本项目要不要切 IPVS。
4. 安装 ingress-nginx（`kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml` 的思路），给 api-gateway 建 §4.2 的 Ingress，用 `curl -H "Host: aics.local" http://<节点IP>/` 验证——对比 NodePort 直连体验。
5. 选 CNI 支持 NetworkPolicy 的集群（或装 Calico），只给 `user-service` 建"仅允许来自 api-gateway"的 ingress 策略，再用临时容器分别以网关 Pod 与裸 Pod 身份访问——亲测"选中即收紧"。

---

> 上一篇：[03-GitOps与ArgoCD](./03-GitOps与ArgoCD.md) ｜ 下一篇：[05-ServiceMesh与Istio边界](./05-ServiceMesh与Istio边界.md)
