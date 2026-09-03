# GitOps 与 ArgoCD

> 对应项目：`Jenkinsfile:119-132`（"部署业务服务"阶段）→ `deploy/scripts/k8s-deploy-services.sh:30-42`（kubectl apply + set image）。
> 相关：[07-运维部署/03-JenkinsCICD流水线](../07-运维部署/03-JenkinsCICD流水线.md)（八阶段流水线精讲与"离生产级还差什么"演进路线）、[01-Helm从零到参数化](./01-Helm从零到参数化.md)（GitOps 的 source 形态）、[07-多环境配置与漂移治理](./07-多环境配置与漂移治理.md)（漂移的更多实证）。
> ⚠️ **工程未落地**：全仓无 ArgoCD/Application/manifests（grep 零命中），本篇是目标态预研 + 与现有 Jenkins 的共存方案。立项目见 [05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md) B 类"Helm / GitOps"行。

---

## 一、push vs pull：本项目现状是典型 push

### 1.1 两种部署模型对比

| 维度 | push（CI 推，本项目现状） | pull（GitOps 拉，目标态） |
|---|---|---|
| 谁发起部署 | Jenkins 从集群**外**向内 kubectl | 集群**内**的控制器持续比对 git |
| 凭据暴露面 | 集群 kubeconfig 必须给 CI 保管 | CI 不需要集群凭据 |
| git 与集群关系 | **允许不一致**（跑完就忘） | git = 唯一真相源，不一致=漂移，可检测可自愈 |
| 部署审计 | 看 Jenkins 控制台日志 | git 提交历史 + ArgoCD 同步历史 |
| 故障恢复 | 人肉重跑流水线 | `argocd app sync`，或自愈自动回 |

### 1.2 现状链路精读（真实阶段与脚本）

```text
Jenkinsfile「部署业务服务」stage（Jenkinsfile:119-132）
  └─> bash deploy/scripts/k8s-deploy-services.sh
        ├─ kubectl apply -f deploy/k8s/namespace.yaml        # 脚本 :30
        ├─ kubectl apply -f deploy/k8s/services/             # :31 整目录 apply
        └─ for service in ${SERVICES}; do                    # :33-42
             kubectl set image deployment/<name> ...=192.168.56.12:5000/aics/<name>:${BUILD_NUMBER}
             kubectl rollout status ... --timeout=300s
           done
```

镜像版本来源：`Jenkinsfile:28` 的 `IMAGE_VERSION = "${params.VERSION ?: env.BUILD_NUMBER}"`，经 `kubectl set image` 在**流水线运行时**注入。

**这里埋着一个关键矛盾**：git 仓库里的 manifest 写的是 `image: 192.168.56.12:5000/aics/ai-chat-service:latest`（`ai-chat-service.yaml:37`），而集群里实际跑的是 `:23`、`:24` 这样的构建号 tag。也就是说——**看 git 不知道集群跑什么，看集群不知道该不该跑 git 里这份**。真相源事实上分裂在 git 和 Jenkins 里，这正是 push 模型的固有问题，也是本篇要解决的问题。

顺带一提：仓库里其实有**两份几乎相同的流水线**（`Jenkinsfile` 与 `Jenkinsfile-k8s`，仅参数默认值与 Maven 镜像源有差异，见 [07 篇 §2.6](./07-多环境配置与漂移治理.md)）——GitOps 化的另一个隐性收益是把"哪份是现行流水线"这类口头约定变成 git 里的唯一事实。

### 1.3 push 模型的三个真实痛点（都能在本项目找到原型）

| 痛点 | 本项目原型 |
|---|---|
| 集群被"手改"后无人知晓 | 谁手动 `kubectl set env` 了一下，下次 apply 就被静默覆盖 |
| 交付物不可追溯 | "上周三生产跑的哪个 tag？"——要翻 Jenkins 构建历史，git 里查不到 |
| 部署面漂移 | product-service 有 manifest 却不在 `SERVICES` 默认参数里（`Jenkinsfile:15` 只有 7 个服务），order/pay/mq 连 manifest 都没有（详见 [07 篇](./07-多环境配置与漂移治理.md) §二） |

---

## 二、ArgoCD 核心概念：Application 与 AppProject

```text
┌──────────┐   git pull    ┌─────────────┐   diff/apply   ┌──────────┐
│  Git 仓库 │ ────────────> │  ArgoCD     │ ─────────────> │ K8s 集群  │
│ manifests │               │ (集群内运行) │ <───────────── │ live 状态 │
└──────────┘               └─────────────§<─── watch ─── └──────────┘
                                │
                     desired state ═? live state
                     Synced/OutOfSync, Healthy/Degraded
```

- **Application**：一张"git 路径 → 集群目标"的声明。最小可用示例（直接指向本项目现有目录）：

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ai-chat-service
  namespace: argocd
spec:
  project: aics
  source:
    repoURL: https://git.example.com/aics/ai-customer-service.git
    targetRevision: main
    path: deploy/k8s/services        # 先复用现有目录，零改造起步
  destination:
    server: https://kubernetes.default.svc
    namespace: ai-customer-service   # 与 8 份清单一致（product 除外，见 07 篇）
  syncPolicy:
    automated:                       # 自动同步
      prune: false                   # 起步先关：git 删了资源集群别跟着删，观察期后再开
      selfHeal: false                # 同上：先只告警不自愈（见 §五）
    syncOptions: [CreateNamespace=false]
```

- **AppProject**：Application 的"权限与边界容器"——限定哪些 git repo、哪些 namespace、哪些资源类型允许被部署。给 `aics` 项目限定 `destinations: [{namespace: ai-customer-service}]`，就能防止手滑把服务部署到别的空间。

**起步建议**：source 直接用 `deploy/k8s/services/` 裸 manifest（零改造可跑通），随后切到 Helm chart（[01 篇](./01-Helm从零到参数化.md)），ArgoCD 原生支持 `source.helm.valueFiles`。

---

## 三、App of Apps：多应用的编排

11 个服务一个个建 Application 也繁琐——**App of Apps** 模式：一个根 Application 指向一个目录，目录里放 N 份子 Application 的 YAML：

```text
deploy/argocd/
├── root-app.yaml            # 只此一份手工 apply（或由 CI apply）
└── apps/                    # 根 Application 的 source.path 指向这里
    ├── api-gateway.yaml     # 每份就是一个 §二 的 Application
    ├── ai-chat-service.yaml
    ├── ...
    └── mq-service.yaml      # 补全清单后加入（见 08 篇）
```

好处：**加服务=往 git 目录加文件**，删除=删文件；根 app 的同步会级联创建/清理子 app。本项目 11 服务规模属于"可上可不上"——8 份 manifest 还能手动维护，加上 order/pay/mq 凑齐 11 份后建议启用。

### 3.1 Application 的两种状态字段（看板语言）

| 字段 | 取值 | 含义 | 本项目对应现场 |
|---|---|---|---|
| Sync Status | `Synced` / `OutOfSync` / `Unknown` | git 与集群是否一致 | 集群被手改 scale → OutOfSync |
| Health Status | `Healthy` / `Progressing` / `Degraded` / `Missing` | 资源实际运行质量 | replicas 拉不起来 → Degraded |

**两者的关系**：Synced 只说明"对象和 git 一样"，Healthy 才说明"跑得对"。典型组合是 `Synced + Degraded`——清单写得和集群一模一样，但镜像拉不下来；也意味着 GitOps 的告警要同时盯两条线，只告警 OutOfSync 会漏掉"同步成功的失败部署"。

> 顺带一提：ArgoCD 还有 **ApplicationSet**（模板化批量生成 Application，按 list/generator 枚举服务），本项目 11 服务枚举量小，App of Apps 的纯 YAML 形式更直白，ApplicationSet 留给多集群场景。

---

## 四、镜像 tag 回写：把"真相"写回 git

pull 模型的前提是 git 里**有真实 tag**。两条主流路线：

### 4.1 CI 回写（中间态，先跑通）

Jenkins 构建推送镜像后，顺手改 git 里的 tag 并提交：

```bash
# Jenkinsfile「构建并推送镜像」stage 之后追加（伪脚本示意）
sed -i "s|image: .*/ai-chat-service:.*|image: 192.168.56.12:5000/aics/ai-chat-service:${IMAGE_VERSION}|" \
  deploy/k8s/services/ai-chat-service.yaml
git commit -am "ci: bump ai-chat-service to ${IMAGE_VERSION}" && git push
# ArgoCD 检测到新 commit → 自动同步 → 集群变更
```

改造点小，但有两个代价：产生大量 "ci: bump" 提交污染历史；多服务并行构建会撞提交冲突。

### 4.2 ArgoCD Image Updater（终态，⚠️ 未落地）

专门控制器监听镜像仓库（本项目本地 Registry `192.168.56.12:5000` 需开 v2 API），发现新 tag 后**自动回写 Application 的参数**（write-back 模式写回 git）：

```yaml
metadata:
  annotations:
    argocd-image-updater.argoproj.io/image-list: ai-chat=192.168.56.12:5000/aics/ai-chat-service
    argocd-image-updater.argoproj.io/ai-chat.update-strategy: semver   # 或 latest
    argocd-image-updater.argoproj.io/write-back-method: git
```

选型建议：学习项目先用 4.1 理解"tag 回写"这个动作；团队规模上来、服务多了再上 4.2。

---

## 五、漂移检测与自愈

| 能力 | 含义 | 开启建议 |
|---|---|---|
| 漂移检测 | ArgoCD 持续 diff git 期望态 vs 集群实际态，OutOfSync 告警 | 默认就有，接 webhook/imail |
| `selfHeal` | 有人手改集群 → 控制器自动恢复成 git 的样子 | 观察期后开启 |
| `prune` | git 里删了资源 → 集群里同步删除 | 最后开（误删风险最大） |

```bash
argocd app diff ai-chat-service          # 手动看漂移：改了什么、谁改的
argocd app sync ai-chat-service          # 手动收敛
```

结合本项目的漂移实证体会价值：`05-技术缺口分析与补全计划.md` 记录的两个"顺带发现"——Nacos 里的死配置键 `pay.sandbox`（见 [07 篇](./07-多环境配置与漂移治理.md) §二）与 order/pay/mq 缺 manifest——本质都是**"声明与实际不一致且没人发现"**。GitOps 把"集群侧"的这类不一致变成了自动告警；"配置中心侧"的治理见 07 篇 §四。

ArgoCD 能捕获的漂移形态与本项目对照：

| 漂移形态 | ArgoCD 表现 | 本项目实例 |
|---|---|---|
| 集群资源被手改 | OutOfSync（diff 字段级展示） | 手动 `kubectl scale` 扩了副本，git 里还是 1 |
| git 有、集群没有 | `Missing` | order/pay/mq 三份清单 apply 前就是全员 Missing |
| 集群有、git 没有 | `OutOfSync`（prune 候选） | 某次调试 `kubectl apply` 的临时资源 |
| 配置内容不一致（Nacos/DB） | **管不到** | `pay.sandbox` 死键、Nacos 写死的 127.0.0.1:9876 |

**分寸**：Nacos/MySQL 里的**数据与配置内容**不是 K8s 资源，GitOps 管不到——不要指望 ArgoCD 解决全部漂移，它是交付层治理，不是配置层治理。

---

## 六、与现有 Jenkins 流水线如何共存

不是替换，是**分工**：

| 阶段（Jenkinsfile 现状） | 保留给 Jenkins（CI） | 迁移给 ArgoCD（CD） |
|---|---|---|
| 拉取代码 / 环境检查（:32-48） | ✅ | — |
| Maven 测试（:50-67） | ✅ | — |
| 写入 Kubernetes Secret（:69-87，创建 aics-secrets） | ✅ 暂留（后续可换 External Secrets） | — |
| 部署基础设施（:89-102，k8s-apply-infra.sh） | ✅ 暂留（中间件有状态、变更低频，最后再迁） | — |
| 构建并推送镜像（:104-117，k8s-build-push.sh） | ✅ | — |
| **部署业务服务（:119-132）** | ❌ **移除**（kubectl apply/set image 交出去） | ✅ 核心接管对象 |
| 部署验证（:134-148） | ✅ 改为触发 `argocd app wait` 断言 | — |

演进三步（与 `07-运维部署/03-JenkinsCICD流水线.md` §七 的演进路线衔接）：

1. **共存期**：Jenkins 保留到"推镜像"为止；新增 ArgoCD Application 指向 `deploy/k8s/services/`（手动 sync，先不动 Jenkins 的部署 stage，观察 diff 是否与 apply 结果一致）。
2. **切换期**：Jenkins 删掉"部署业务服务"stage，改为构建后回写 tag（§四）+ `argocd app sync --async`；开 selfHeal。
3. **终态**：业务服务全自动 pull；基础设施与 Secret 迁移评估 External Secrets + ArgoCD 管理（是否迁要看团队运维成熟度，不强求）。

---

## 面试高频问答

**Q1：GitOps 的核心原则是什么？**
A：① 声明式（期望状态以 YAML 存 git）；② 版本化不可变（git 历史=部署审计）；③ 自动拉取（集群内控制器对账）；④ 持续调和（漂移即告警/自愈）。一句话：git 是唯一真相源。

**Q2：push 和 pull 部署模型怎么选？**
A：push（CI kubectl）实现简单、但 CI 持有集群凭据且 git 与集群易失同步；pull（GitOps）凭据不出集群、漂移可检测，代价是多一个控制器与同步延迟。小团队起步 push 完全够，多环境/多集群/合规审计诉求出现时切 pull。

**Q3：你们项目 git 里 manifest 是 :latest，集群里跑的是构建号 tag，这说明什么？**
A：说明部署是 push 模式、真相源分裂——tag 在 Jenkins 运行时经 `kubectl set image` 注入，git 从未反映实际状态。GitOps 化的第一步就是"镜像 tag 回写 git"（CI sed 回写或 ArgoCD Image Updater）。

**Q4：Application 和 AppProject 的关系？**
A：Application 是一次"repo+路径 → 集群目标"的部署声明；AppProject 是一组 Application 的权限边界（允许哪些 repo、namespace、资源类型）。多团队场景必配 AppProject 做隔离。

**Q5：App of Apps 解决什么问题？**
A：用一个根 Application 管理一组子 Application 的声明文件，实现"加/删服务=加/删 git 文件"的级联编排，避免逐个 argocd app create。服务规模到两位数后收益明显。

**Q6：selfHeal 和 prune 分别什么时候开？**
A：先漂移告警（只检测）→ 开 selfHeal（手改被自动还原，安全）→ 最后开 prune（git 删除会真删集群资源，误删风险最大，需团队习惯稳定后再开）。

**Q7：GitOps 能覆盖 Nacos 里的配置漂移吗？**
A：不能直接覆盖。ArgoCD 只调和 K8s 资源，Nacos 配置内容不在其管辖内。需要配套"配置进 git + 发布工具推送 Nacos + 键引用检查"（见 07 篇），两者互补而非替代。

**Q8：迁移 GitOps 后 Jenkins 还有存在价值吗？**
A：有，且是必要的一半。构建、测试、镜像推送、安全扫描是 CI 职责，天然由 Jenkins 承担；GitOps 只接管"从 git 到集群"的交付段。完整链路=Jenkins 推镜像并回写 tag，ArgoCD 拉取并调和。

**Q9：ArgoCD 的 Application 应该放在哪个 git 仓库？**
A：两个选择：放进应用代码仓库（本项目起步零改造的做法）或独立的 config/deploy 仓库（职责清晰、CI/CD 权限分离）。规模小用前者够用，团队一多就拆后者——关键是仓库里始终有"集群应该长什么样"的唯一真相。

---

## 动手练习

1. 用 `grep -n "set image" deploy/scripts/k8s-deploy-services.sh` 确认 tag 注入点，再对比 `ai-chat-service.yaml:37` 的 `:latest`——写下"git 看不到集群真相"的三句论证（面试可直接用）。
2. 本地起一个 k3s + kind 运行的 ArgoCD（或用 ArgoCD 演示环境），创建指向本项目 `deploy/k8s/services/` 的 Application（destination 指向 ai-customer-service），观察 8 个资源的 Synced 状态。
3. 手动 `kubectl scale deploy/ai-chat-service --replicas=2` 制造漂移，用 `argocd app diff` 观察报告，再手动 sync 收敛——体会"检测在先、自愈在后"。
4. 给 Jenkinsfile 伪代码实现 §4.1 的 tag 回写（sed + git commit），思考：为什么建议回写后**让 ArgoCD 自动同步**而不是继续让 Jenkins 直接 apply？
5. 画出本项目 §六 共存方案的演进时序图（三个阶段，标出 Jenkinsfile 哪个 stage 在哪一步被移除），并回答：基础设施（mysql/nacos）为什么放在最后迁移？
6. 为 `product-service`（namespace 是 `aics` 而非 `ai-customer-service`，见 product-service.yaml:5）单独创建一个 Application：destination.namespace 填 `aics`，体会"一个 namespace 不一致就会 OutOfSync"的真实成本——这也是 01 篇主张 Helm 统一 namespace 的原因。
7. 用 `git log --oneline -- deploy/k8s/services/` 查看清单最近 10 次提交，回答：哪几次是"应发尽发"、哪几次是"救火手改"？后者就是 selfHeal 要拦下的操作。
8. 给 ArgoCD 配一条 Notifications 通知（webhook 到企业微信/钉钉），验证 OutOfSync 时能收到消息——GitOps 没有告警就等于没有检测。
9. 对比 `Jenkinsfile` 与 `Jenkinsfile-k8s` 的参数默认值（各自开头 env/parameters 段），列出漂移点，并回答：单仓库双流水线该合并还是明确分工？

---

> 上一篇：[02-K8s资源治理与弹性伸缩](./02-K8s资源治理与弹性伸缩.md) ｜ 下一篇：[04-K8s网络模型与Service](./04-K8s网络模型与Service.md)
