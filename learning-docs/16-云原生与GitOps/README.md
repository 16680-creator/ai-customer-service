# 云原生与GitOps

> 本专题是 `learning-docs` 的 **第十六个模块**（`16-云原生与GitOps`），主题一句话：**把"跑起来"升级为"可治理地交付"**。
> 前置阅读：[00-学习路线总览](../00-学习路线总览/README.md)、[07-运维部署/01-Docker容器化](../07-运维部署/01-Docker容器化.md)、[07-运维部署/02-Kubernetes入门](../07-运维部署/02-Kubernetes入门.md)、[07-运维部署/03-JenkinsCICD流水线](../07-运维部署/03-JenkinsCICD流水线.md)。
> 07-运维部署 已讲过 manifest 逐行精读、kubectl 日常操作、Jenkins 八阶段流水线——本模块**不重复**，只写增量：Helm 化、资源治理、GitOps、网络纵深、Mesh 决策、镜像供应链、配置漂移治理，以及一篇真实交付型的 manifest 补全文档。

---

## 一、为什么在 Docker/K8s/Jenkins 之后还要单独学这一层

07-运维部署 回答"这个项目怎么部署"；本模块回答"如果这是一个 20 人团队维护的生产系统，还缺什么"。

| 场景 | 只会 kubectl apply（本项目现状） | 掌握云原生交付之后 |
|---|---|---|
| 新增一个微服务 | 手写一份 70 行 YAML，从别的文件复制改 | 一套 chart + 一段 values（[01](./01-Helm从零到参数化.md)），或按 checklist 补清单（[08](./08-补全order-pay-mq的K8s清单.md)） |
| 内存 OOM / 被驱逐 | 不理解为什么 | 能从 requests/limits、QoS 等级、探针参数三条线定位（[02](./02-K8s资源治理与弹性伸缩.md)） |
| "集群上跑的到底是什么版本" | git 里是 `:latest`，真相在 Jenkins 构建号里 | GitOps：git 是唯一真相源，漂移可检测可自愈（[03](./03-GitOps与ArgoCD.md)） |
| 服务互访与入口 | 知道 NodePort 能通 | 能讲清 kube-proxy、CoreDNS、Ingress/Gateway API 的分层（[04](./04-K8s网络模型与Service.md)） |
| 限流熔断要不要上 Istio | 听说 Mesh 很厉害就上 | 先做重叠/互补矩阵再决策（[05](./05-ServiceMesh与Istio边界.md)） |
| 镜像越来越大、root 跑、有洞 | 不管 | jlink、非 root、只读 fs、Trivy/SBOM（[06](./06-镜像构建优化与安全扫描.md)） |
| Nacos 里有 `pay.sandbox` 死键 | 无人发现 | 键引用检查 + 漂移清单（[07](./07-多环境配置与漂移治理.md)） |
| order/pay/mq 上不了 K8s | 一直缺着 | 按 08 篇补齐 3 份清单（[08](./08-补全order-pay-mq的K8s清单.md)） |

一句话：**07-运维部署 是"能跑"，本模块是"敢上生产"。** 其中 [08](./08-补全order-pay-mq的K8s清单.md) 是真实交付型文档——补的正是本项目 B 类缺口里"K8s 部署缺口"（order/pay/mq 缺 manifest）那一行。

### 1.1 与 07-运维部署 的分工（避免学重复）

| 主题 | 07-运维部署 已讲 | 本模块只写增量 |
|---|---|---|
| manifest 逐行精读 | Service/Deployment/StatefulSet 逐行讲透（02 篇 §四/§五） | 不再讲字段含义，直接进共性与差异分析（[01](./01-Helm从零到参数化.md)） |
| 探针写法 | 三种探针的类型与本项目实例（02 篇 §5.4） | 参数陷阱、startupProbe、QoS（[02](./02-K8s资源治理与弹性伸缩.md)） |
| 流量入口 | NodePort 进网关、集群内两条互访路径（02 篇 §6） | kube-proxy/CoreDNS 纵深、Ingress/NetworkPolicy（[04](./04-K8s网络模型与Service.md)） |
| Jenkins 流水线 | 八 stage 逐个精讲 + 三个脚本（03 篇） | push/pull 模型批判、ArgoCD 共存方案（[03](./03-GitOps与ArgoCD.md)） |
| Dockerfile | 多阶段构建逐行精讲（01 篇 §二） | 层缓存纵深、jlink、非 root、扫描（[06](./06-镜像构建优化与安全扫描.md)） |
| 滚动更新 | 优雅停机三层防护（06 篇） | 不重复，仅在 PDB/探针处引用 |
| 中间件部署 | MySQL/Nacos 等清单解读（02 篇 §五） | 不涉及中间件清单 |

学法建议：07-运维部署 对应篇章若还没读，先读再进本模块；已读过的，本模块开头每篇的"对应项目/相关"块就是快速回链。

---

## 二、知识地图

```
                          云原生与 GitOps
                                │
         ┌──────────────────────┼──────────────────────┐
         │                      │                      │
    【交付形态】            【运行时治理】           【供应链与配置】
         │                      │                      │
   裸 manifest（现状）     资源与弹性               镜像构建优化
   ├ 8 份手写清单         ├ requests/limits/QoS    ├ 多阶段/层缓存（已会）
   └ 共性 87% 重复        ├ 探针三兄弟陷阱          ├ jlink 定制 JRE
         │                ├ HPA/VPA/KEDA           ├ 非 root / 只读 fs
   Helm（目标态）          └ PDB 驱逐预算           └ Trivy / SBOM
   ├ chart 结构                  │
   ├ values 覆盖链          网络                       配置漂移治理
   ├ _helpers/模板函数      ├ Pod 网络/CNI             ├ 漂移成因四类
   ├ 条件与循环             ├ Service/iptables/IPVS    ├ 本项目六处实证
   └ dev/prod overlay      ├ CoreDNS                  ├ 键引用检查
         │                 ├ Ingress/Gateway API      └ 治理路线
   GitOps（目标态）         └ NetworkPolicy
   ├ push vs pull                │
   ├ ArgoCD Application     Service Mesh（决策）
   ├ App of Apps            ├ sidecar vs ambient
   ├ 镜像 tag 回写          ├ mTLS/金丝雀/故障注入
   └ 漂移检测与自愈          └ 与 Sentinel/R4j/Nacos 边界
```

---

## 三、文档清单与学习路线

### 第一阶段：交付形态演进（1 周）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 01 | [Helm 从零到参数化](./01-Helm从零到参数化.md) | chart 结构、values 覆盖链、模板函数、_helpers、条件循环、dev/prod overlay；把 8 份 manifest 抽成 1 套 chart | ⭐⭐⭐ |
| 08 | [补全 order/pay/mq 的 K8s 清单](./08-补全order-pay-mq的K8s清单.md) | 真实交付：三份可用 manifest + Dockerfile 补齐方案 + 部署脚本扩容（可与 01 对调着学） | ⭐⭐ |

### 第二阶段：运行时治理与网络（1 周）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 02 | [K8s 资源治理与弹性伸缩](./02-K8s资源治理与弹性伸缩.md) | requests/limits 与 QoS、探针参数陷阱、HPA 指标选择、VPA/KEDA、PDB | ⭐⭐⭐ |
| 04 | [K8s 网络模型与 Service](./04-K8s网络模型与Service.md) | Pod 网络/CNI、Service 类型与 iptables vs IPVS、CoreDNS、Ingress vs Gateway API、NetworkPolicy | ⭐⭐⭐ |

### 第三阶段：GitOps 与供应链（1 周）

| 序号 | 文档 | 核心内容 | 难度 |
|---|---|---|---|
| 03 | [GitOps 与 ArgoCD](./03-GitOps与ArgoCD.md) | push/pull 模型、Application/AppProject、App of Apps、镜像 tag 回写、漂移自愈、与 Jenkins 共存 | ⭐⭐⭐⭐ |
| 06 | [镜像构建优化与安全扫描](./06-镜像构建优化与安全扫描.md) | 层缓存、jlink、非 root、只读根文件系统、Trivy/SBOM | ⭐⭐ |
| 07 | [多环境配置与漂移治理](./07-多环境配置与漂移治理.md) | 漂移成因、本项目六处实证（死键/死环境变量/双 Jenkinsfile）、键引用检查 | ⭐⭐⭐ |
| 05 | [ServiceMesh 与 Istio 边界](./05-ServiceMesh与Istio边界.md) | sidecar/ambient、mTLS、金丝雀；与 Sentinel/Resilience4j/Nacos 的重叠矩阵与引入决策清单 | ⭐⭐⭐⭐ |

---

## 四、本项目现场表（云原生要素 ↔ 项目文件 ↔ 对应文档）

> 先看清"工程现状"，再进各篇。`⚠️ 未落地` = 工程还没有，文档给目标态/预研方案。

| 云原生要素 | 项目现状（真实文件） | 对应文档 |
|---|---|---|
| 业务服务 manifest | `deploy/k8s/services/` 共 8 份（7 份同构 + product 异构：namespace `aics`、replicas 2、httpGet 探针） | [01](./01-Helm从零到参数化.md)、[02](./02-K8s资源治理与弹性伸缩.md) |
| resources/探针 | `ai-chat-service.yaml:80-98`（requests 200m/512Mi，tcpSocket 探针） | [02](./02-K8s资源治理与弹性伸缩.md) |
| 部署流水线 | `Jenkinsfile:119-132` 部署业务服务 → `deploy/scripts/k8s-deploy-services.sh:30-42`（kubectl apply + set image） | [03](./03-GitOps与ArgoCD.md) |
| 集群入口 | `api-gateway.yaml:10-15`（NodePort 30080，唯一入口；无 Ingress） | [04](./04-K8s网络模型与Service.md) |
| 限流/熔断 | `ai-cs-chat/pom.xml:203`（Sentinel）、`ai-cs-chat/pom.xml:116-125`（Resilience4j）、`ai-cs-gateway/.../filter/TokenBucketRateLimiter.java`（自研） | [05](./05-ServiceMesh与Istio边界.md) |
| 镜像构建 | `ai-cs-chat/Dockerfile:1-21`（多阶段，21 行）；`ai-cs-order/Dockerfile:21`（无 JAVA_OPTS 展开）；`ai-cs-pay/`、`ai-cs-mq/` **无 Dockerfile** | [06](./06-镜像构建优化与安全扫描.md)、[08](./08-补全order-pay-mq的K8s清单.md) |
| 配置中心 | `tools/nacos-config/`（aics-shared.yml 等 12 份；`ai-cs-pay.yml:18-19` 有死键 `pay.sandbox`） | [07](./07-多环境配置与漂移治理.md) |
| 端口契约 | order 8087 / pay 8089 / mq 8090（各服务 `application.yml:3`） | [08](./08-补全order-pay-mq的K8s清单.md) |
| 部署脚本映射 | `k8s-deploy-services.sh:9-20` 与 `k8s-build-push.sh:8-19` 的 case 表只有 7 个服务 | [08](./08-补全order-pay-mq的K8s清单.md) §五 |
| 镜像/流水线一致性 | 9 份 Dockerfile 两种 ENTRYPOINT 风格（order/product 无 JAVA_OPTS 展开）；两份 Jenkinsfile 参数默认值漂移 | [06](./06-镜像构建优化与安全扫描.md) §一、[07](./07-多环境配置与漂移治理.md) §2.6 |
| Helm / ArgoCD / Mesh / HPA / Ingress / PDB | ⚠️ 工程未落地（`deploy/k8s/` 下 grep 零命中） | [01](./01-Helm从零到参数化.md)、[03](./03-GitOps与ArgoCD.md)、[04](./04-K8s网络模型与Service.md)、[05](./05-ServiceMesh与Istio边界.md)、[02](./02-K8s资源治理与弹性伸缩.md) |

缺口总账见 [00-学习路线总览/05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md) B 类（Helm/GitOps、K8s 部署缺口两行）与文末两个"顺带发现"（`pay.sandbox` 死键、compose 与 k8s 服务清单不一致）——本模块 07/08 两篇正是对它们的落地回应。

---

## 五、常见误区对照表（每条都在本项目能找到原型）

| 误区 | 本项目原型 | 正解 |
|---|---|---|
| "manifest 注入了环境变量，服务就一定用上了" | `MYSQL_HOST` 注入 4 份清单，全仓零消费 | 以代码占位符为准，定期跑键引用检查（[07](./07-多环境配置与漂移治理.md) §四） |
| "git 里的清单就是集群在跑的东西" | git 是 `:latest`，集群跑的是构建号 tag | GitOps 把 git 变回真相源（[03](./03-GitOps与ArgoCD.md)） |
| "探针越敏感越好，liveness 挂了就重启很稳" | 7 份清单 tcpSocket + initialDelay 60 硬等 | liveness 要迟钝、readiness 要敏感，分工明确（[02](./02-K8s资源治理与弹性伸缩.md)） |
| "内存 limit=堆大小就行" | 堆 512m 配 limit 1Gi 是对的 | 堆只占 limit 一半，JVM 还有堆外开销（[02](./02-K8s资源治理与弹性伸缩.md) §2.3） |
| "听说 Istio 好，先上了再说" | Sentinel/R4j/自研限流已覆盖大半治理 | 先做重叠矩阵再决策（[05](./05-ServiceMesh与Istio边界.md)） |
| "镜像能跑就行，root 也没事" | 9 份 Dockerfile 全 root | 非 root + 只读 fs 是 K8s 安全基线（[06](./06-镜像构建优化与安全扫描.md)） |
| "新服务部署 = 复制一份 YAML 改改" | product 清单就是"复制后风格漂移"的产物 | 按 checklist 交付（[08](./08-补全order-pay-mq的K8s清单.md)）或 Helm 化（[01](./01-Helm从零到参数化.md)） |
| "Spring 宽松绑定能兜底任何变量名" | `MINIO_*` 注入清单，但消费前缀是 `aics.minio`（要 `AICS_MINIO_*`） | 宽松绑定只做 `-`/`_` 与大小写转换，不改前缀；前缀错了就是死变量（[07](./07-多环境配置与漂移治理.md) §二） |
| "docker-compose 里有的中间件，K8s 里就也有" | compose 12 个中间件服务与 `deploy/k8s/` 的中间件清单并不对齐 | 交付面漂移：两套环境要按同一份清单对账（[07](./07-多环境配置与漂移治理.md) §2） |

---

## 六、速查表

```bash
# ===== Helm（目标态，01 篇）=====
helm create aics-chart                     # 生成 chart 骨架
helm template ai-chat ./aics-chart -f values-dev.yaml   # 本地渲染不落集群（先看后装）
helm lint ./aics-chart                     # 语法检查
helm diff upgrade ai-chat ./aics-chart -f values-prod.yaml   # 预览变更（helm-diff 插件）
helm upgrade --install ai-chat ./aics-chart -f values-prod.yaml -n ai-customer-service

# ===== 资源与弹性（02 篇）=====
kubectl top pods -n ai-customer-service    # 实际用量（需 metrics-server）
kubectl get hpa,pdb -n ai-customer-service
kubectl describe pod <pod> | grep -A5 "Limits\|Last State"   # 看 OOMKilled

# ===== GitOps（03 篇）=====
argocd app list / argocd app diff ai-chat  # 漂移检测
argocd app sync ai-chat                    # 手动同步
argocd app get ai-chat --refresh           # 刷新状态

# ===== 网络（04 篇）=====
kubectl get svc -n ai-customer-service -o wide
kubectl run tmp --rm -it --image=curlimages/curl -- curl http://user-service:8081/actuator/health

# ===== 镜像供应链（06 篇）=====
trivy image 192.168.56.12:5000/aics/ai-chat-service:latest --severity HIGH,CRITICAL
trivy image --format cyclonedx -o sbom.json <image>          # SBOM

# ===== 配置漂移（07 篇）=====
grep -rn "pay.sandbox" tools/nacos-config/          # 死键定位
diff -r tools/nacos-config/ tools/nacos/data/tenant-config-data/   # 两份 Nacos 配置源比对

# ===== 新服务交付（08 篇，复制 manifest 后）=====
kubectl apply -f deploy/k8s/services/order-service.yaml
kubectl rollout status deploy/order-service -n ai-customer-service
kubectl logs -f deploy/order-service -n ai-customer-service --tail=100
kubectl get pods -n ai-customer-service -o wide
```

---

## 七、学习方法

1. **现状先行**：每篇都从真实文件出发（本模块共引用了 8 份 manifest、2 份 Dockerfile、2 个 Jenkinsfile、3 个部署脚本、12 份 Nacos 配置），先看"工程是什么样"，再学"业界怎么做"，最后回到"怎么改"。
2. **区分现状与目标态**：标注 ⚠️ 的章节是工程未落地的目标态设计——学习目标是"能设计、能答辩"，不是"项目里已有"。
3. **交付型文档动手验证**：08 篇的三份 manifest 写法与现有 8 份完全同构，学完后可亲手复制到 `deploy/k8s/services/` 并 `kubectl apply` 验证（验证步骤在 08 篇第五节）。
4. **决策题练习表达**：05 篇的 Mesh 决策、03 篇的 GitOps 演进，都是"要不要做"的开放题——练的是用证据链支撑结论，而非背概念。
5. **把漂移当教材**：07 篇的每一条实证（死键、变量名打错、双 Jenkinsfile）都是真实工程里天天发生的事，学会用 grep 立案、用清单收口。
6. **证据纪律**：引用项目文件前先 grep/Read 确认——本模块所有 file:line 引用都经过这一步，练习题也在训练同样的习惯。

### 7.1 学完本模块的自检清单

- [ ] 能说出 8 份 manifest 的共性骨架与 6 类差异字段（端口/Service 类型/副本/namespace/探针/env）
- [ ] 能解释 requests/limits/QoS，并判断本项目 8 份清单的 QoS 等级
- [ ] 能复述"git 里 :latest、集群跑构建号"的矛盾及两条 tag 回写路线
- [ ] 能画出外部请求经 NodePort 30080 → 网关 Pod 的完整路径（含 DNAT/SNAT）
- [ ] 能用"重叠/互补矩阵"论证本项目暂不引入 Istio，并说出至少 3 条触发条件
- [ ] 能写出一份带正确 env（非死键）与 httpGet 探针的新服务 manifest（08 篇验收）
- [ ] 能用三集合对账模型（代码消费键 / Nacos 键 / 注入变量）定位死配置
- [ ] 能说出本项目两个 git 仓库真相源问题：镜像 tag 不回写、Nacos 配置双份拷贝

---

> 返回 [学习路线总览](../00-学习路线总览/README.md)
