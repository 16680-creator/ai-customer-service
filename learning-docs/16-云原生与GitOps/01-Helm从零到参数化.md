# Helm 从零到参数化

> 对应项目：`deploy/k8s/services/`（现存 8 份手写 manifest，共 621 行，7 份结构同构）。
> 相关：[07-运维部署/02-Kubernetes入门](../07-运维部署/02-Kubernetes入门.md)（manifest 逐行精读）、[03-GitOps与ArgoCD](./03-GitOps与ArgoCD.md)（Helm 之后的下一站）。
> ⚠️ **工程未落地，本篇为目标态设计**：项目当前没有 Helm chart（全仓无 Chart.yaml），下文 chart 是基于 8 份真实 manifest 抽象出的改造方案，立项目见 [05-技术缺口分析与补全计划](../00-学习路线总览/05-技术缺口分析与补全计划.md) B 类"Helm / GitOps"行（验收标准："一套 chart 参数化 8 个服务的部署差异"）。

---

## 一、为什么需要 Helm：先量化现状的重复度

对 8 份 manifest 逐份比对（全部读过，非抽查）：

| 服务 | 端口 | Service 类型 | 副本 | namespace | 探针类型 | resources |
|---|---|---|---|---|---|---|
| api-gateway | 8080 | **NodePort 30080** | 1 | ai-customer-service | tcpSocket | 100m/256Mi → 500m/512Mi |
| user-service | 8081 | ClusterIP | 1 | ai-customer-service | tcpSocket | 同上 |
| knowledge-service | 8082 | ClusterIP | 1 | ai-customer-service | tcpSocket | 同上 |
| ai-chat-service | 8083 | ClusterIP | 1 | ai-customer-service | tcpSocket | **200m/512Mi → 1000m/1Gi** |
| search-service | 8084 | ClusterIP | 1 | ai-customer-service | tcpSocket | 100m/256Mi → 500m/512Mi |
| message-service | 8085 | ClusterIP | 1 | ai-customer-service | tcpSocket | 同上 |
| notify-service | 8086 | ClusterIP | 1 | ai-customer-service | tcpSocket | 同上 |
| product-service | 8088 | ClusterIP | **2** | **aics** ⚠️ | **httpGet** ⚠️ | 250m/512Mi（顺序还不同）⚠️ |

结论：**7 份清单的骨架完全一样**（Service 先、Deployment 后、`image: 192.168.56.12:5000/aics/<name>:latest`、`imagePullPolicy: IfNotPresent`、tcpSocket 探针 initialDelay 60/30），差异只有上表 6 类字段 + 每家不同的 env。product-service 是"另一个 team 写的"风格（namespace、镜像名 `aics/ai-cs-product:latest` 无 registry 前缀、探针用 httpGet）——这本身就是**手写清单必然漂移**的现场证据（详见 [07-多环境配置与漂移治理](./07-多环境配置与漂移治理.md)）。

**Helm 解决的正是这个问题**：把"骨架"做成模板，把"差异"做成 values。

| 维度 | 手写 8 份 manifest（现状） | 1 套 chart + values（目标态） |
|---|---|---|
| 新增服务 | 复制 70 行 YAML 改 6 处 | 加 10 行 values |
| 改全局项（如 registry 地址） | 8 个文件挨个改 | 改 1 行 |
| 环境差异（dev/prod） | 复制整套目录或 kubectl 手改 | 两份 values 文件 |
| 升级/回滚 | `kubectl set image` + `rollout undo` | `helm upgrade` / `helm rollback`（版本化 release） |
| 模板校验 | apply 了才知道错 | `helm template` / `helm lint` 本地渲染 |

---

## 二、Chart 目录结构（目标态）

```text
deploy/helm/aics-platform/
├── Chart.yaml                  # chart 元数据（name/version/apiVersion）
├── values.yaml                 # 默认值（所有 8 个服务的公共骨架）
├── values-dev.yaml             # dev 环境覆盖（可不放仓库，见 §六）
├── values-prod.yaml            # prod 环境覆盖
└── templates/
    ├── _helpers.tpl            # 模板片段：命名、标签、镜像、探针
    ├── service.yaml            # 一份模板渲染出 N 个 Service（range）
    └── deployment.yaml         # 一份模板渲染出 N 个 Deployment
```

`Chart.yaml` 最小内容：

```yaml
apiVersion: v2
name: aics-platform
description: AI 客户服务平台（11 服务合集 chart）
version: 0.1.0        # chart 自身版本
appVersion: "1.0"     # 应用版本（可选）
```

**设计取舍**：8 个服务放**一个 chart**（用 range 循环渲染）而不是 8 个 chart——因为骨架 87% 相同，拆 8 个 chart 会把重复再犯一遍。等某服务长出独立诉求（如 mq 需要大量 volume）再拆子 chart。

---

## 三、values 覆盖链：差异收敛成数据

### 3.1 覆盖优先级（从低到高）

```text
chart 内 values.yaml  →  -f values-prod.yaml  →  -f 更后指定的文件  →  --set 单键
```

后加载的覆盖先加载的；`--set` 优先级最高（适合 CI 里注入镜像 tag，见 [03-GitOps与ArgoCD](./03-GitOps与ArgoCD.md) §四）。查最终生效值用 `helm get values <release> -a`。

### 3.2 用真实差异设计 values（核心一步）

差异字段全量收进 values，键名直接对应 §一 的表：

```yaml
# values.yaml（骨架默认值 = 7 份同构清单的最大公约数）
global:
  registry: 192.168.56.12:5000
  imagePullPolicy: IfNotPresent
  namespace: ai-customer-service
  imageTag: latest            # 生产建议改为 BUILD_NUMBER

services:
  api-gateway:
    port: 8080
    serviceType: NodePort
    nodePort: 30080           # 对应 api-gateway.yaml:14 的真实值
    javaOpts: "-Xms256m -Xmx256m"
    env: []                   # 网关清单只有 NACOS_ADDR，无业务 env
  user-service:
    port: 8081
    javaOpts: "-Xms256m -Xmx256m"
    env:
      - { name: MYSQL_HOST, value: mysql }
      - { name: MYSQL_PORT, value: "3306" }
      - { name: MYSQL_DB, value: user_db }
  ai-chat-service:
    port: 8083
    javaOpts: "-Xms512m -Xmx512m"
    resources:                # 只有大模型服务单独一档（ai-chat-service.yaml:80-86）
      requests: { cpu: 200m, memory: 512Mi }
      limits:   { cpu: 1000m, memory: 1Gi }
    env:
      - { name: MYSQL_HOST, value: mysql }
      - { name: MYSQL_DB, value: chat_db }
      - { name: REDIS_HOST, value: redis }     # chat 代码真实消费：application.yml:51
    secrets:                  # OPENAI_* 走 aics-secrets（ai-chat-service.yaml:61-77）
      - { envName: OPENAI_API_KEY, secretName: aics-secrets, key: openai-api-key }
      - { envName: OPENAI_BASE_URL, secretName: aics-secrets, key: openai-base-url, optional: true }
  # ...search(8084)/message(8085)/notify(8086)/knowledge(8082) 同理，各 10 行左右
```

> 诚实备注：`MYSQL_*`/`MINIO_*` 等 env 在现状 manifest 里注入了但代码并不消费（死键，见 [07 篇 §二](./07-多环境配置与漂移治理.md)）。目标态 chart 正是清理它们的机会——迁移时先只保留有消费的 env（如 chat 的 `REDIS_HOST`），其余列为"待核实再删"。

---

## 四、_helpers.tpl 与模板函数

`templates/_helpers.tpl` 定义可复用片段。以下全部以真实字段为例：

```yaml
{{/* 服务名：release 名 + 服务键，如 aics-ai-chat-service */}}
{{- define "aics.name" -}}
{{ .Values.global.releasePrefix | default "aics" }}-{{ .key }}
{{- end }}

{{/* 镜像全名：对应现状 192.168.56.12:5000/aics/ai-chat-service:latest */}}
{{- define "aics.image" -}}
{{ .Values.global.registry }}/aics/{{ .key }}:{{ .Values.global.imageTag }}
{{- end }}

{{/* 公共标签 */}}
{{- define "aics.labels" -}}
app: {{ .key }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}
```

常用模板函数速查（都在本项目会用到）：

| 函数 | 作用 | 本项目用例 |
|---|---|---|
| `quote` / `default` | 加引号 / 兜底 | `MYSQL_PORT "3306"` 必须是字符串，`"3306"` 手写易漏 quote |
| `upper` / `lower` | 大小写 | 标签值规范化 |
| `nindent` | 换行缩进（配合 include） | include "aics.labels" . \| nindent 4 |
| `toYaml` | 结构体转 YAML | `resources: {{ toYaml .resources \| nindent 10 }}` |
| `required` | 缺值直接报错 | `required "port 必填" .port` 防止渲染出空端口 |
| `eq` / `default` | 条件判断 | `if eq .serviceType "NodePort"` |

---

## 五、条件与循环：一份模板渲染 8 份清单

### 5.1 range 循环渲染所有服务

```yaml
# templates/service.yaml
{{- range $key, $svc := .Values.services }}
---
apiVersion: v1
kind: Service
metadata:
  name: {{ $key }}
  namespace: {{ $.Values.global.namespace }}    # 注意：range 内取根作用域要用 $
  labels:
    app: {{ $key }}
spec:
  type: {{ $svc.serviceType | default "ClusterIP" }}
  ports:
    - port: {{ $svc.port }}
      targetPort: {{ $svc.port }}
      name: http
      {{- if eq ($svc.serviceType | default "ClusterIP") "NodePort" }}
      nodePort: {{ $svc.nodePort }}             # 只有网关有 30080
      {{- end }}
  selector:
    app: {{ $key }}
{{- end }}
```

### 5.2 Deployment 模板（骨架 + 条件差异）

```yaml
{{- range $key, $svc := .Values.services }}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ $key }}
  namespace: {{ $.Values.global.namespace }}
spec:
  replicas: {{ $svc.replicas | default 1 }}
  selector:
    matchLabels: { app: {{ $key }} }
  template:
    metadata:
      labels: { app: {{ $key }} }
    spec:
      containers:
        - name: {{ $key }}
          image: {{ $.Values.global.registry }}/aics/{{ $key }}:{{ $.Values.global.imageTag }}
          imagePullPolicy: {{ $.Values.global.imagePullPolicy }}
          ports:
            - containerPort: {{ $svc.port }}
              name: http
          env:
            - { name: NACOS_ADDR, value: "nacos:8848" }        # 8 份清单人人都有
            {{- range $svc.env }}
            - name: {{ .name }}
              value: {{ .value | quote }}
            {{- end }}
            {{- range $svc.secrets }}                           # secretKeyRef 分支
            - name: {{ .envName }}
              valueFrom:
                secretKeyRef:
                  name: {{ .secretName }}
                  key: {{ .key }}
                  optional: {{ .optional | default false }}
            {{- end }}
            - { name: JAVA_OPTS, value: {{ $svc.javaOpts | quote }} }
          {{- with $svc.resources }}
          resources:
            {{- toYaml . | nindent 12 }}
          {{- else }}
          resources:                                            # 默认档 = 7 份清单的共同值
            requests: { cpu: 100m, memory: 256Mi }
            limits:   { cpu: 500m, memory: 512Mi }
          {{- end }}
          livenessProbe:
            tcpSocket: { port: {{ $svc.port }} }                # 现状写法，§六升级
            initialDelaySeconds: 60
            periodSeconds: 15
            timeoutSeconds: 5
          readinessProbe:
            tcpSocket: { port: {{ $svc.port }} }
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
{{- end }}
```

**两个高频坑**：① `range` 内 `.` 已变成循环变量，取 `.Values` 根作用域要写 `$.Values`；② 空值渲染出 `resources:` 空行会让 apply 报错，用 `with` + `default` 双保险。

---

## 六、多环境 overlay：dev 与 prod 的 values

环境差异只该体现在 values，不该复制模板。以本项目真实差异设计两份 overlay：

```yaml
# values-dev.yaml（学习/本地集群：保持现状行为）
global:
  imageTag: latest                      # dev 图省事
services:
  ai-chat-service:
    probes: tcpSocket                   # 与现状一致
  api-gateway: {}

# values-prod.yaml（生产：三处升级）
global:
  imageTag: required-version            # 由 CI 注入 BUILD_NUMBER，禁用 latest
services:
  ai-chat-service:
    replicas: 2                         # 大模型服务先扩
    probes: httpGet                     # tcpSocket → /actuator/health
  user-service:
    replicas: 2
    probes: httpGet
```

prod 升级探针的依据：`tools/nacos-config/aics-shared.yml:32` 已统一暴露 `health,info,prometheus` 端点，且 `product-service.yaml:47-58` 已有 httpGet `/actuator/health` 先例——所以这不是新约定，是把已有事实推广到全部服务。

渲染与验证（**先看后装**）：

```bash
# 本地渲染比对：新旧产物 diff，确认骨架一致
helm template aics ./deploy/helm/aics-platform -f values-dev.yaml > rendered-dev.yaml
diff <(kubectl get deploy ai-chat-service -n ai-customer-service -o yaml) rendered-dev.yaml

# 语法与最佳实践检查
helm lint ./deploy/helm/aics-platform -f values-prod.yaml

# 试运行（不落集群，打印服务端校验结果）
helm install aics ./deploy/helm/aics-platform -f values-dev.yaml --dry-run

# 正式安装 / 升级 / 回滚（release 版本化，取代 kubectl set image）
helm upgrade --install aics ./deploy/helm/aics-platform -f values-dev.yaml -n ai-customer-service
helm rollback aics 1 -n ai-customer-service
```

迁移验收（对齐缺口文档 B 类验收标准）：**8 份手写 manifest 删除后，`helm template` 渲染产物与原集群 live 状态 diff 为空（除有意修正项）**。迁移完成后再接 GitOps（[03 篇](./03-GitOps与ArgoCD.md)）。

---

## 面试高频问答

**Q1：Helm 解决什么问题？没有 Helm 行不行？**
A：解决"模板重复 + 参数散落 + 版本管理缺失"。理论上 Kustomize（patch 叠加）也能做，但本项目场景是"骨架同构、差异字段多"，values 全量参数化比 patch 叠加直观；两者也可混用。

**Q2：values 的覆盖优先级是什么？**
A：从低到高：chart 默认 values.yaml → `-f` 各环境文件（后者覆盖前者）→ `--set`。排查用 `helm get values <release> -a` 看合并结果，不要猜。

**Q3：_helpers.tpl 里的 define/include 是什么机制？**
A：`define` 定义命名模板片段，`include "name" .` 渲染并返回字符串（常配 `nindent` 缩进）；区别于 `template`（直接输出、不能做管道）。命名约定用 chart 前缀（`aics.xxx`）避免跨 chart 冲突。

**Q4：range 里为什么取不到 .Values？**
A：`range` 把作用域切到循环元素上。要取根作用域需用 `$.Values`（`$` 是模板根），或在 range 前把值存入变量。

**Q5：`--set` 传复杂结构有什么坑？**
A：类型退化（数字变字符串）、转义地狱（逗号/方括号都要转义）、不可版本化。复杂值一律走 `-f` 文件；`--set` 留给 CI 注入单个镜像 tag 这类原子值。

**Q6：helm upgrade 与 kubectl apply 的本质区别？**
A：apply 是"把这份 YAML 应用到集群"，无版本概念；helm upgrade 是"从 release v(n-1) 计算到 v(n) 的变更"，带版本历史（helm history/rollback），且渲染期就能 `helm template/diff` 预检。

**Q7：你们的 chart 为什么 8 个服务放一个 chart 而不是每服务一个？**
A：8 份真实 manifest 骨架 87% 相同（7 份完全同构），拆开只是把复制粘贴换成复制粘贴。单 chart + per-service values 把重复收敛进模板；未来某服务长出独立诉求再拆子 chart，成本远低于现在拆 8 个。

**Q8：Helm 和 GitOps 是什么关系？**
A：Helm 是"打包与参数化"工具，GitOps 是"以 git 为真相源的交付模型"，二者正交。ArgoCD 原生支持 Helm source（git 里存 chart+values，ArgoCD 负责同步），本项目路线就是先 Helm 化再接 ArgoCD。

---

## 动手练习

1. 用 `wc -l deploy/k8s/services/*.yaml` 复核 621 行总量，再对 ai-chat-service 与 user-service 做 `diff`，列出全部差异行——验证 §一"骨架同构"的结论。
2. 手写最小 chart：只含 `api-gateway` 一个服务的 Service+Deployment 模板，`helm template` 渲染结果与 `deploy/k8s/services/api-gateway.yaml` 逐行比对（重点：nodePort 30080、JAVA_OPTS 256m）。
3. 把 §五 的模板跑通后，给 `mq-service`（端口 8090，见 [08 篇](./08-补全order-pay-mq的K8s清单.md)）只加 5 行 values 就能渲染出完整清单——体验"新增服务=加数据"。
4. 故意把 values 里 `port` 删掉，观察渲染产物，再用 `required` 改造模板使其 fail-fast。
5. 写 `values-prod.yaml` 时验证一个细节：把 `imageTag` 设为空串渲染，想想生产环境为什么必须 fail-fast（提示：`:latest` + `imagePullPolicy: IfNotPresent` 在节点已有旧镜像时会**不拉取**，升级静默失败）。

---

> 下一篇：[02-K8s资源治理与弹性伸缩](./02-K8s资源治理与弹性伸缩.md)
