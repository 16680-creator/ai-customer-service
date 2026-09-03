# 补全 order/pay/mq 的 K8s 清单

> 对应项目：`deploy/k8s/services/user-service.yaml`（风格模板）、`ai-cs-order/src/main/resources/application.yml`、`ai-cs-pay/src/main/resources/application.yml`、`ai-cs-mq/src/main/resources/application.yml`。
> 相关：[07-多环境配置与漂移治理](./07-多环境配置与漂移治理.md) §2.1（缺口立案）、[01-Helm从零到参数化](./01-Helm从零到参数化.md)（清单同构性的根因）、[02-K8s资源治理与弹性伸缩](./02-K8s资源治理与弹性伸缩.md)（资源档位依据）、[07-运维部署/02-Kubernetes入门](../07-运维部署/02-Kubernetes入门.md) §7（apply 顺序与验证）。
> **本文档性质：真实交付型**——文中的 YAML/脚本片段是给 `deploy/k8s/services/` 的待交付物，**本文档不直接改动仓库**；按 §六 复制落地并用 `kubectl apply` 验证。全部字段（端口/健康检查/环境变量）均已 grep 核对真实来源。

---

## 一、动笔前的事实核对（全部 file:line）

### 1.1 三个服务的硬事实

| 核对项 | ai-cs-order | ai-cs-pay | ai-cs-mq |
|---|---|---|---|
| 端口（`server.port`） | **8087**（`application.yml:3`） | **8089**（`application.yml:3`） | **8090**（`application.yml:3`） |
| actuator 依赖 | ✅ `ai-cs-order/pom.xml:163` | ✅ `ai-cs-pay/pom.xml:88` | ✅ `ai-cs-mq/pom.xml:55` |
| `/actuator/health` 可用 | ✅（`aics-shared.yml:32` 统一暴露 `health,info,prometheus`） | ✅ 同左 | ✅ 同左 |
| Nacos 注册/配置 | ✅ namespace `aics`（`application.yml:14`） | ✅ 同左（:14） | ✅ 同左（:14） |
| Dockerfile | ✅ 有（`EXPOSE 8087`，但 `:21` 无 JAVA_OPTS 展开） | ❌ **无** | ❌ **无** |
| 现存 K8s 清单 | ❌ 缺 | ❌ 缺 | ❌ 缺 |

缺口立案见 [07 篇 §2.1](./07-多环境配置与漂移治理.md)：8 份清单对 11 个服务，交付面漂移。本篇交付 order/pay/mq 三份（第 8~10 份；py-chat 为 Python 服务不适用本模板，见 [10-Python服务/01](../10-Python服务/01-FastAPI对话服务实战.md)）。

### 1.2 各服务真实消费的环境变量（决定 manifest 的 env 该写什么）

对三服务 src/main 与其 Nacos 配置逐项 grep 的结论：

| 服务 | 必需 env | 来源依据 | 可选 env |
|---|---|---|---|
| order | `NACOS_ADDR`、`DB_PASSWORD` | Nacos 配置 `tools/nacos-config/ai-cs-order.yml:11` 的 `${DB_PASSWORD}` 占位；`application.yml:13` 的 `${NACOS_ADDR:127.0.0.1:8848}` | `SEATA_ADDR`（`application.yml:43`，默认 127.0.0.1:8091）、`XXL_JOB_ENABLED`（:54，默认 false）、`XXL_JOB_ADMIN`（:55） |
| pay | `NACOS_ADDR`、`DB_PASSWORD` | Nacos 配置 `ai-cs-pay.yml:5` 的 `${DB_PASSWORD}`；`application.yml:13` 同上 | `ROCKETMQ_ADDR`（Nacos 配置 `ai-cs-pay.yml:23` 的 `${ROCKETMQ_ADDR:127.0.0.1:9876}`——**pay 是三家里唯一支持 MQ 地址覆盖的**）、`TRACING_SAMPLING`、`OTLP_ENDPOINT` |
| mq | `NACOS_ADDR` | 仅 Nacos 注册/配置 + 追踪 | `TRACING_SAMPLING`、`OTLP_ENDPOINT`。**无 DB**：mq 的 pom 只有 rocketmq-tools，无数据源 |

**两个必须诚实标注的前置缺口**（不修则清单"可 apply 但功能异常"）：

1. **RocketMQ 地址写死**：`tools/nacos-config/aics-shared.yml:14` 与 `ai-cs-mq.yml:2` 都是 `name-server: 127.0.0.1:9876`——Pod 里 127.0.0.1 是 Pod 自己，K8s 里必然连不上。修法（Nacos 侧，非本篇交付）：改成 `${ROCKETMQ_ADDR:rocketmq-namesrv:9876}` 或直接 `rocketmq-namesrv:9876`。
2. **Seata 无 Server**：order 的 `application.yml:36` `seata.enabled: true`，但 compose 与 K8s 都没有 Seata Server（`docker-compose.yml` grep `seata` 零命中）。部署后 order 单机事务可用、**全局事务（AT 分支）不可用**；清单保留 `SEATA_ADDR` 注入点，值暂填 `seata:8091` 占位。

---

## 二、模板选择与风格约定（对齐现有 8 份）

以 `user-service.yaml` 为骨架基准（7 份同构清单的代表），仅探针一处采用 `product-service.yaml` 的 httpGet 先例：

| 决策点 | 取值 | 依据 |
|---|---|---|
| namespace | `ai-customer-service` | 7/8 清单一致（product 的 `aics` 是漂移，见 07 篇 §2.2，不再复制错误） |
| 对象顺序 | Service 先、Deployment 后（`---` 分隔） | 与 user-service.yaml:1-17 同构 |
| 镜像 | `192.168.56.12:5000/aics/order-service:latest` | 与 `ai-chat-service.yaml:37` 的 registry/tag 形态一致 |
| imagePullPolicy | `IfNotPresent` | 同上 |
| 探针 | **httpGet `/actuator/health`** | 1.1 已核实三服务均暴露 health；`product-service.yaml:47-58` 有同款先例；比 tcpSocket 强（[02 篇](./02-K8s资源治理与弹性伸缩.md) §三） |
| 资源档 | order 用 ai-chat 档（事务发起方+Seata+Feign+MQ 多依赖），pay/mq 用标准档 | 与 [02 篇](./02-K8s资源治理与弹性伸缩.md) §一 现状画像对齐 |
| 命名 | k8s 资源名 `order-service` / `pay-service` / `mq-service` | 延续 `*-service` 命名法（与映射表 §五 一致） |

---

## 三、前置交付：Dockerfile（写在文档里，待复制）

### 3.1 `ai-cs-pay/Dockerfile` 与 `ai-cs-mq/Dockerfile`（新建，模板同一份）

两份文件除两处参数外完全一致（与 chat 形式对齐：支持 JAVA_OPTS），**参数替换表**：

| 参数 | pay 版 | mq 版 |
|---|---|---|
| 模块目录与 `-pl` | `ai-cs-pay` | `ai-cs-mq` |
| `EXPOSE` | 8089 | 8090 |

```dockerfile
# 多阶段构建 - <支付服务|MQ管理服务>（按上表替换两处参数）
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY ai-cs-common/pom.xml ai-cs-common/
COPY ai-cs-<pay|mq>/pom.xml ai-cs-<pay|mq>/
RUN mvn dependency:go-offline -B -pl ai-cs-<pay|mq> -am
COPY ai-cs-common/ ai-cs-common/
COPY ai-cs-<pay|mq>/ ai-cs-<pay|mq>/
RUN mvn package -pl ai-cs-<pay|mq> -am -DskipTests -B

# 阶段2: JRE运行
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/ai-cs-<pay|mq>/target/*.jar app.jar
EXPOSE <8089|8090>
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
```

### 3.2 `ai-cs-order/Dockerfile:21` 升级（补 JAVA_OPTS 展开点）

现状 `ENTRYPOINT ["java", "-jar", "app.jar"]` 会**静默忽略** JAVA_OPTS（9 份镜像里 order/product 两份如此，见 [06 篇](./06-镜像构建优化与安全扫描.md) §一）。改为：

```dockerfile
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
```

> 镜像安全加固（非 root / 只读 fs / jlink）见 [06 篇](./06-镜像构建优化与安全扫描.md) §三~§五，本篇只求"能部署"，加固项不混入本次交付。

---

## 四、三份 K8s 清单（核心交付物）

### 4.1 `deploy/k8s/services/order-service.yaml`

```yaml
# 订单服务部署（端口 8087：ai-cs-order/src/main/resources/application.yml:3）
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: ai-customer-service
  labels:
    app: order-service
spec:
  type: ClusterIP
  ports:
    - port: 8087
      targetPort: 8087
      name: http
  selector:
    app: order-service
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: ai-customer-service
  labels:
    app: order-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: 192.168.56.12:5000/aics/order-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8087
              name: http
          env:
            - name: NACOS_ADDR
              value: "nacos:8848"
            # DB_PASSWORD：Nacos 配置 ai-cs-order.yml:11 的 ${DB_PASSWORD} 占位所需；
            # 明文仅为与现有清单风格一致，生产应换 secretKeyRef（07 篇 §2.7）
            - name: DB_PASSWORD
              value: "root"
            # SEATA_ADDR：compose/K8s 均无 Seata Server（grep 实证），全局事务暂不可用，
            # 占位指向未来 Service 名，部署 Seata 后无需改清单
            - name: SEATA_ADDR
              value: "seata:8091"
            - name: XXL_JOB_ENABLED   # application.yml:54 默认 false，显式写出防歧义
              value: "false"
            - name: JAVA_OPTS
              value: "-Xms256m -Xmx512m"
          resources:
            requests:
              cpu: "200m"
              memory: "512Mi"
            limits:
              cpu: "1000m"
              memory: "1Gi"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8087
            initialDelaySeconds: 60
            periodSeconds: 20
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8087
            initialDelaySeconds: 30
            periodSeconds: 10
```

### 4.2 `deploy/k8s/services/pay-service.yaml`

```yaml
# 支付服务部署（端口 8089：ai-cs-pay/src/main/resources/application.yml:3）
apiVersion: v1
kind: Service
metadata:
  name: pay-service
  namespace: ai-customer-service
  labels:
    app: pay-service
spec:
  type: ClusterIP
  ports:
    - port: 8089
      targetPort: 8089
      name: http
  selector:
    app: pay-service
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pay-service
  namespace: ai-customer-service
  labels:
    app: pay-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: pay-service
  template:
    metadata:
      labels:
        app: pay-service
    spec:
      containers:
        - name: pay-service
          image: 192.168.56.12:5000/aics/pay-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8089
              name: http
          env:
            - name: NACOS_ADDR
              value: "nacos:8848"
            # DB_PASSWORD：Nacos 配置 ai-cs-pay.yml:5 占位所需（治理提示同 order）
            - name: DB_PASSWORD
              value: "root"
            # pay 的 Nacos 配置支持 ${ROCKETMQ_ADDR}（ai-cs-pay.yml:23），注入即覆盖默认值
            - name: ROCKETMQ_ADDR
              value: "rocketmq-namesrv:9876"
            - name: JAVA_OPTS
              value: "-Xms256m -Xmx256m"
          resources:
            requests:
              cpu: "100m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8089
            initialDelaySeconds: 60
            periodSeconds: 20
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8089
            initialDelaySeconds: 30
            periodSeconds: 10
```

### 4.3 `deploy/k8s/services/mq-service.yaml`

```yaml
# MQ管理服务部署（端口 8090：ai-cs-mq/src/main/resources/application.yml:3）
# 前置：Nacos 配置 ai-cs-mq.yml:2 的 rocketmq.name-server 写死 127.0.0.1:9876，
#       需改为 rocketmq-namesrv:9876 或参数化，否则 MQ 功能连不上（见 §1.2）。
apiVersion: v1
kind: Service
metadata:
  name: mq-service
  namespace: ai-customer-service
  labels:
    app: mq-service
spec:
  type: ClusterIP
  ports:
    - port: 8090
      targetPort: 8090
      name: http
  selector:
    app: mq-service
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mq-service
  namespace: ai-customer-service
  labels:
    app: mq-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mq-service
  template:
    metadata:
      labels:
        app: mq-service
    spec:
      containers:
        - name: mq-service
          image: 192.168.56.12:5000/aics/mq-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8090
              name: http
          env:
            - name: NACOS_ADDR
              value: "nacos:8848"
            - name: JAVA_OPTS
              value: "-Xms256m -Xmx256m"
          resources:
            requests:
              cpu: "100m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8090
            initialDelaySeconds: 60
            periodSeconds: 20
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8090
            initialDelaySeconds: 30
            periodSeconds: 10
```

---

## 五、配套交付：脚本映射与流水线参数扩容

只写文档，不动仓库。现有映射表**只有 7 个服务**（`deploy/scripts/k8s-build-push.sh:8-19`、`k8s-deploy-services.sh:9-20`），各加三行：

```bash
# k8s-build-push.sh 与 k8s-deploy-services.sh 的 case 表各追加：
    ai-cs-order) echo "order-service" ;;
    ai-cs-pay) echo "pay-service" ;;
    ai-cs-mq) echo "mq-service" ;;
```

`Jenkinsfile:15` 的 SERVICES 默认值同步扩容：

```groovy
string(name: 'SERVICES', defaultValue: 'ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify ai-cs-order ai-cs-pay ai-cs-mq', ...)
```

> product（第 11 个服务）是否入列是独立决策：它有清单（`product-service.yaml`）却不在映射表——按 [07 篇 §三](./07-多环境配置与漂移治理.md) 清单先"核对/修正"再入列，避免把 namespace `aics` 的漂移一起带上线。

---

## 六、落地与验证步骤（"待复制后 kubectl apply 验证"）

> 再次声明：本篇 YAML 尚未进仓库。以下步骤就是"复制后怎么验"。

```bash
# 0. 前置确认
ls deploy/k8s/services/          # 放入三份 yaml 后应为 11 份
ls ai-cs-pay/Dockerfile ai-cs-mq/Dockerfile   # §三 的两份镜像构建文件已就位

# 1. 构建（依赖 §五 的映射扩容；REGISTRY/VERSION 沿用流水线参数）
REGISTRY=192.168.56.12:5000 VERSION=1 SERVICES="ai-cs-order ai-cs-pay ai-cs-mq" \
  bash deploy/scripts/k8s-build-push.sh

# 2. apply（与现有 8 份同目录，整目录生效）
kubectl apply -f deploy/k8s/services/order-service.yaml
kubectl apply -f deploy/k8s/services/pay-service.yaml
kubectl apply -f deploy/k8s/services/mq-service.yaml

# 3. 观察（对照 07-运维部署/02 §7.2 的做法）
for d in order-service pay-service mq-service; do
  kubectl rollout status deployment/$d -n ai-customer-service --timeout=300s
done

# 4. 健康验证（探针同路径，集群内直测；三行分别改端口 8087/8089/8090）
kubectl run tmp --rm -it --image=curlimages/curl -n ai-customer-service -- sh
#   curl http://order-service:8087/actuator/health   → {"status":"UP"}   （pay/mq 同理改名字与端口）

# 5. Nacos 控制台确认三服务已注册（namespace aics；与服务名 ai-cs-order/ai-cs-pay/ai-cs-mq 对应，
#    注意 k8s Service 名 order-service 与 Nacos 注册名是两回事，调用方走 Feign 用的是 Nacos 名）
```

**预期故障速查**（与 [07 篇](./07-多环境配置与漂移治理.md) 实证一一对应）：

| 现象 | 根因 | 处置 |
|---|---|---|
| order/pay Pod CrashLoop，日志含 `Could not resolve placeholder 'DB_PASSWORD'` | env 未注入或值错误 | 核对 env 名精确为 `DB_PASSWORD` |
| mq 服务 UP 但发消息超时 | Nacos 里 rocketmq 地址写死 127.0.0.1:9876（§1.2 前置缺口 1） | 改 Nacos `ai-cs-mq.yml:2` 与 `aics-shared.yml:14` |
| order 日志反复报 Seata 连接失败 | 无 Seata Server（§1.2 前置缺口 2） | 预期内：单机事务可用；或 Nacos 配置临时 `seata.enabled: false` |
| 镜像拉取失败 `ErrImagePull` | 未先跑 §六第 1 步构建，或 containerd 未配 HTTP 私仓（Jenkinsfile:166 的故障提示同源） | 先构建推送 / 配 insecure registry |

---

## 面试高频问答

**Q1：手写一份新的 K8s manifest 前，你核对什么？**
A：五件事：① 服务端口（读 application.yml 的 server.port，不猜）；② 健康检查能力（actuator 是否在依赖里、health 端点是否暴露）；③ 真实消费的环境变量（grep 占位符，不做"想象的注入"——本项目 8 份清单里约 3/4 的 env 是死键）；④ Dockerfile 是否存在、入口是否支持 JAVA_OPTS；⑤ namespace/命名/资源档与现有清单一致。

**Q2：为什么你的探针用 httpGet /actuator/health 而不是照抄大多数清单的 tcpSocket？**
A：tcpSocket 只证明端口在监听，线程池打满时照样通（假活）；三服务均已引入 actuator 且 aics-shared.yml 统一暴露 health 端点，product-service 已有 httpGet 先例——既有能力就该用对。

**Q3：k8s Service 名（order-service）和 Nacos 注册名（ai-cs-order）什么关系？**
A：两套独立体系。K8s Service 是集群内网络对象（探针、跨 namespace 访问）；Nacos 名是应用层注册发现（Feign 调用方用的是它）。清单里两个名字都要写对，别混用——调用排障时先确认流量走的是哪套。

**Q4：为什么 order 的资源档给到 1C1G，pay/mq 只给 0.5C0.5G？**
A：按依赖与职责分档：order 是 Seata 全局事务发起方 + Feign 消费方 + RocketMQ 生产者，堆给 512m（与 ai-chat 同档对账：堆/limit=50%）；pay 虽是资金链路但无重计算，标准档即可；mq 是管理面服务更轻。档位依据全部来自现有 8 份清单的真实画像。

**Q5：DB_PASSWORD 为什么明文写在清单里？正确做法是什么？**
A：是与现有清单风格（MYSQL_PASSWORD: root）保持一致的学习环境取值，且 Nacos 配置的 ${DB_PASSWORD} 占位必需。正确做法是 secretKeyRef（项目里 aics-secrets 已有先例：Jenkinsfile:79-84），并在漂移治理篇 §2.7 的密钥面治理中统一收口。

**Q6：交付型文档和教程文档最大的区别是什么？**
A：交付型文档的每个字段必须可追溯到来源（file:line），且要写清"前置缺口、验证步骤、预期故障"三件套——读者要能照着执行并判断结果对不对；教程文档重在讲原理，可以有省略。本篇的三个前置缺口（Dockerfile×2、RocketMQ 地址、Seata）就是"不给这三段说明，清单就是坑"的部分。

**Q7：这三份清单上完线，项目的 K8s 部署缺口就算关了吗？**
A：manifest 数量上 8→11 关了"缺清单"子项，但映射表扩容（§五）、product 清单修正（namespace 漂移）、pay/mq Dockerfile 落盘、Nacos 地址参数化这四个配套动作没做完，验收标准"k8s 全量 11 服务可部署"还不能勾。验收要看端到端健康检查通过，不是文件存在。

---

## 动手练习

1. 核对端口契约：`grep -n "port:" ai-cs-order/src/main/resources/application.yml ai-cs-pay/src/main/resources/application.yml ai-cs-mq/src/main/resources/application.yml`，确认 8087/8089/8090，再与本篇三份清单的 `containerPort` 逐一对照。
2. 按 §三 落盘三份 Dockerfile（两份新建 + order 的 ENTRYPOINT 升级），本地 `docker build` 并 `docker run -e JAVA_OPTS=-Xmx256m` 验证参数确实生效（对照 [06 篇](./06-镜像构建优化与安全扫描.md) 练习 1）。
3. 按 §六 完整走一遍：构建 → apply → rollout status → 集群内 curl 三次 health，把结果（含遇到的故障与处置）记录成一份"部署纪要"。
4. 制造一次故障并排障：把 order 清单的 `DB_PASSWORD` 改名 `DB_PASSWD` 后重新 apply，观察 CrashLoop 与占位符报错，用 `kubectl describe pod` + `kubectl logs` 定位——体会 §六 速查表第一行。
5. 进阶：把三份清单的差异字段（端口/env/资源档）抽进 [01 篇](./01-Helm从零到参数化.md) 的 values 结构里，验证"加一个服务只需 10 行 values"的结论对 pay 也成立。

---

> 上一篇：[07-多环境配置与漂移治理](./07-多环境配置与漂移治理.md)
