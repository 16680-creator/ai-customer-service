# Kubernetes 入门：搞懂本项目的一整套部署逻辑

> 对应项目文件：`deploy/k8s/` 目录（全部部署清单）、各微服务的 `Dockerfile`
> 前置知识：先读 [01-Docker容器化](./01-Docker容器化.md)，K8s 是建立在 Docker 镜像之上的。
>
> 这篇文档的目标：**不讲空概念，用本项目真实存在的 YAML，把"代码怎么变成集群里能访问的服务"这条链路完整走一遍。**

---

## 零、一张图看懂整套部署逻辑（先建立全局观）

新手最常见的困惑是"概念很多但不知道它们怎么串起来"。先看这条完整链路：

```
【代码 → 用户可访问，总共 6 步】

 ① git push 代码
      │
 ② mvn package 打出 jar
      │
 ③ docker build（Dockerfile 多阶段构建）→ 生成镜像
      │
 ④ docker push → 推到私有镜像仓库 192.168.56.12:5000/aics/xxx:latest
      │
 ⑤ kubectl apply -f deploy/k8s/xxx.yaml   ← 清单里写的镜像地址就是 ④ 推的那个
      │        K8s 做了什么：
      │        API Server 校验并存入 etcd（集群的"数据库"）
      │           → Scheduler 挑一台合适的节点
      │           → 那台节点的 kubelet 拉镜像、启动容器
      │
 ⑥ Pod 跑起来了
      └─ Service 给它一个稳定的访问地址（服务名 + 虚拟 IP）
         api-gateway 用 NodePort 30080 暴露给外部
         → 浏览器访问 http://<节点IP>:30080 ✅
```

**记住一句话：K8s 不构建镜像，它只负责"拿着清单（YAML）描述的期望状态，让集群的真实状态无限逼近期望状态"。**
镜像怎么来是 Docker/CI 的事（见 01 和 03 两篇），集群里怎么跑、怎么恢复、怎么暴露是 K8s 的事。

### 部署顺序：谁先谁后有讲究

`deploy/k8s/` 里的文件不是随便排的，存在依赖关系（后启动的依赖先启动的）：

```
第 1 步  namespace.yaml                  ← 先有"隔离的文件夹"
第 2 步  middleware-secrets.example.yaml ← 数据库密码等敏感配置
第 3 步  mysql.yaml                      ← 最底层的存储，连 Nacos 都依赖它
第 4 步  nacos.yaml / redis.yaml         ← 注册中心 + 缓存
第 5 步  rocketmq / elasticsearch / neo4j / chroma / minio / xxl-job / sentinel
第 6 步  services/*.yaml（8 个微服务）    ← 最后上，因为依赖上面所有组件
```

顺序错了会怎样？比如先启动微服务再启动 Nacos：微服务注册不上，Pod 反复重启（CrashLoopBackOff）。K8s 会自动重试，所以最终还是会起来，但中间会经历大量报错——**理解依赖顺序 = 理解这套系统的架构**。

---

## 一、为什么需要 K8s？本项目其实有两套部署方案

项目里同时存在 `deploy/docker-compose/` 和 `deploy/k8s/`，这不是重复，是两个阶段：

```
Docker Compose：单机编排（学习/开发阶段）
  → 一条命令拉起 MySQL + Redis + Nacos + 8 个微服务
  → 缺点：一台服务器挂了全挂；机器不够要手动迁移；没有自愈

Kubernetes：集群编排（生产/进阶阶段）
  → 多台机器组成集群，K8s 决定每个容器跑在哪台
  → 容器挂了自动重启（自愈）、机器挂了自动迁移
  → 一条命令扩容到 N 个副本
```

| 能力 | Docker Compose | Kubernetes | 本项目的体现 |
|------|---------------|------------|-------------|
| 部署范围 | 单机 | 多机集群 | `deploy/k8s/` 清单 |
| 挂了怎么办 | 手动重启 | 自动重启/迁移 | K8s 检测容器退出自动拉起 |
| 扩容 | 手动改配置再 up | `kubectl scale` 一条命令 | `replicas` 字段 |
| 滚动更新 | 不支持 | 逐个替换，零停机 | Deployment 策略 |
| 服务发现 | Compose 网络的服务名 | 内置 DNS（CoreDNS） | 微服务里配的 `mysql`、`nacos` 就是 DNS 名 |
| 配置管理 | environment/volumes | ConfigMap + Secret | `mysql.yaml` 里的 cnf、密码 |
| 持久化 | named volume | PVC 动态分配存储 | `volumeClaimTemplates: 10Gi` |

---

## 二、核心概念：每个概念对应项目里的哪一行

不用死记定义，看它们在本项目里的"实体"：

| K8s 概念 | 一句话理解 | 本项目对应 |
|----------|-----------|-----------|
| Namespace | 隔离的"文件夹" | `namespace.yaml` 创建的 `ai-customer-service`，所有资源都在里面 |
| Pod | 最小部署单元 = 1 个运行中的容器 | `kubectl get pods` 看到的 `ai-chat-service-xxxxx` |
| Deployment | 管 Pod 的"班主任"：保证副本数、负责滚动更新 | 8 个微服务都用它（无状态） |
| StatefulSet | 管"有身份"的 Pod：有固定名字、有专属硬盘 | MySQL/Redis/ES/Neo4j/Chroma/MinIO 用它（有状态） |
| Service | 一组 Pod 的稳定访问入口（Pod 会死会换 IP，Service 地址不变） | `mysql-master:3306`、`nacos:8848` |
| ClusterIP | 只在集群内部访问的 Service 类型 | 中间件和微服务全是这种 |
| NodePort | 在每个节点开一个端口，让**外部**能访问 | api-gateway 的 `30080` |
| ConfigMap | 配置文件内容 | `mysql.yaml` 里的 master.cnf / slave.cnf、`redis.yaml` 里的 redis.conf |
| Secret | 敏感配置（Base64，可加密存储） | AI 的 `openai-api-key`、Neo4j 密码 |
| PVC（卷申请） | "我要 10Gi 硬盘"的申请单 | MySQL 的 `volumeClaimTemplates: storage: 10Gi` |
| 探针 | 健康检查：决定重启还是摘流量 | 每个服务都配了 `livenessProbe` / `readinessProbe` |
| 资源限额 | 给容器划定 CPU/内存边界 | `requests: 200m/512Mi`、`limits: 1000m/1Gi` |

### Deployment 和 StatefulSet：本项目为什么分开用？

这是新手最容易混的点，用本项目举例：

```
无状态（Deployment）—— ai-chat-service
  挂了重新起一个就行，新起的实例和原来的一模一样
  不需要记住任何"身份"，数据都在 MySQL/Redis 里

有状态（StatefulSet）—— mysql
  数据写在自己的硬盘（PVC）上，Pod 重建后必须还挂原来的硬盘
  Pod 名字稳定：mysql-master-0（永远叫这个，不会变成随机名）
  所以每个 StatefulSet 必须配一个 headless Service + volumeClaimTemplates
```

本项目 10 个中间件里，MySQL、Redis、Elasticsearch、Neo4j、Chroma、MinIO 全是 StatefulSet（都要存数据）；Nacos、RocketMQ、XXL-Job、Sentinel 是 Deployment（Nacos 数据放 MySQL，其余无状态）。

---

## 三、`deploy/k8s/` 全景图：项目里到底有哪些清单

```
deploy/k8s/
├── namespace.yaml                    ← 第 1 个 apply：创建命名空间
├── middleware-secrets.example.yaml   ← Secret 模板（复制成 middleware-secrets.yaml 再填真密码）
│
├── mysql.yaml                        ← 主从 MySQL：ConfigMap + 2×Service + 2×StatefulSet
├── redis.yaml                        ← ConfigMap + 2×Service（含 headless）+ StatefulSet
├── nacos.yaml                        ← 注册/配置中心：Service + Deployment
├── rocketmq.yaml                     ← NameServer + Broker：2×Service + 2×Deployment
├── elasticsearch.yaml                ← 搜索引擎：Service + StatefulSet（20Gi）
├── neo4j.yaml                        ← 图数据库：Service + StatefulSet
├── chroma.yaml                       ← 向量数据库（RAG 用）：Service + StatefulSet
├── minio.yaml                        ← 对象存储：Service + StatefulSet
├── xxl-job.yaml                      ← 调度中心：Service + Deployment（参数来自 Secret）
├── sentinel-dashboard.yaml           ← 流控看板：Service + Deployment
│
└── services/                         ← 8 个微服务，每个文件 = Service + Deployment
    ├── api-gateway.yaml              ← 8080，NodePort 30080（唯一对外的门）
    ├── user-service.yaml             ← 8081
    ├── knowledge-service.yaml        ← 8082
    ├── ai-chat-service.yaml          ← 8083（AI 对话）
    ├── search-service.yaml           ← 8084
    ├── message-service.yaml          ← 8085
    ├── notify-service.yaml           ← 8086
    └── product-service.yaml          ← 8088
```

一个文件里可以放多个资源，用 `---` 分隔（比如 `mysql.yaml` 一个文件里就有 5 个资源）。每个微服务文件都是"Service 在前、Deployment 在后"的结构。

---

## 四、逐行精读①：一个微服务清单是怎么工作的

以 `deploy/k8s/services/ai-chat-service.yaml` 为例（其他 7 个结构完全一样）。这是**最值得吃透的文件**，看懂它 = 会看本项目的所有微服务。

### 4.1 Service 部分：给 Pod 一个稳定门牌

```yaml
apiVersion: v1
kind: Service                       # 资源类型：Service
metadata:
  name: ai-chat-service             # Service 名字 → 会注册进集群 DNS
  namespace: ai-customer-service    # 属于哪个命名空间
spec:
  type: ClusterIP                   # 只在集群内访问，外部摸不到（安全）
  ports:
    - port: 8083                    # Service 自己监听的端口
      targetPort: 8083              # 转发到 Pod 容器的端口
      name: http
  selector:                         # ★ 关键：按 label 找 Pod
    app: ai-chat-service
```

**Service 和 Pod 是怎么"配对"的？** 靠 label（标签）：

```
Service 的 selector:  app=ai-chat-service
         ↑ 匹配
Deployment 模板里的 labels:  app=ai-chat-service
         ↓ 打在
每个 Pod 身上
```

改动任何一边导致对不上，Service 就会"找不到后端"，请求全部失败。这是新手必踩的坑，记牢这对关系。

### 4.2 Deployment 部分：声明"我要 1 个这样的 Pod"

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-chat-service
  namespace: ai-customer-service
spec:
  replicas: 1                       # 期望副本数。学习环境设 1 省资源；生产至少 2 个才谈得上高可用
  selector:
    matchLabels:
      app: ai-chat-service          # Deployment 靠它认领自己创建的 Pod（同样要和下面配对）
  template:                         # ===== Pod 模板：Pod 长什么样由这里决定 =====
    metadata:
      labels:
        app: ai-chat-service        # 给 Pod 打标签（上面两处 selector 都靠它）
    spec:
      containers:
        - name: ai-chat-service
          image: 192.168.56.12:5000/aics/ai-chat-service:latest
          #          └── 私有镜像仓库地址：docker push 推到哪，这里就写哪
          imagePullPolicy: IfNotPresent   # 节点上有这个镜像就不重新拉（本地学习快；生产建议 Always）

          ports:
            - containerPort: 8083    # 声明容器监听的端口

          env:                       # ===== 注入环境变量，应用启动时读取 =====
            - name: NACOS_ADDR       # 普通变量：直接写死
              value: "nacos:8848"    # ← 注意！这里写的是 K8s Service 的 DNS 名，不是 IP
            - name: MYSQL_HOST
              value: "mysql"         # 见 4.4 的"坑"
            - name: OPENAI_API_KEY   # 敏感变量：从 Secret 里取，不写在明文里
              valueFrom:
                secretKeyRef:
                  name: aics-secrets
                  key: openai-api-key
            - name: JAVA_OPTS        # Dockerfile 的 ENTRYPOINT 会用到它：java $JAVA_OPTS -jar app.jar
              value: "-Xms512m -Xmx512m"

          resources:                 # ===== 资源边界 =====
            requests:                # 最低保障：调度器按这个找"装得下"的节点
              cpu: "200m"            # 200m = 0.2 个 CPU 核
              memory: "512Mi"
            limits:                  # 上限：超过会被限制（内存超了直接 OOMKill）
              cpu: "1000m"
              memory: "1Gi"

          livenessProbe:             # 存活探针：失败 → 杀掉容器重启
            tcpSocket:               # 只测 8083 端口通不通
              port: 8083
            initialDelaySeconds: 60  # 启动后等 60s 才开始探（Java 应用启动慢，要给足时间）
            periodSeconds: 15        # 每 15s 探一次
          readinessProbe:           # 就绪探针：失败 → 不重启，只是不把流量给它
            tcpSocket:
              port: 8083
            initialDelaySeconds: 30
            periodSeconds: 10
```

### 4.3 一分钟看懂"创建 Pod 时 K8s 内部发生了什么"

```
kubectl apply（提交期望：1 个副本）
   ↓
API Server：校验 YAML → 存进 etcd（集群状态数据库）
   ↓
Scheduler：看 requests（0.2 核 / 512Mi），挑一个剩余资源够的节点
   ↓
该节点的 kubelet（节点上的"监工"）：
   → 向 192.168.56.12:5000 拉取镜像（IfNotPresent：本地有就跳过）
   → 用镜像启动容器，注入 env 环境变量
   ↓
kubelet 持续盯着：
   → livenessProbe 失败 → 重启容器
   → 容器退出          → 按策略重启（这就是"自愈"）
   ↓
Endpoints 控制器：发现新 Pod 就绪 → 把它的 IP 加入 ai-chat-service 这个 Service 的后端列表
```

### 4.4 本项目清单里的一个真实的"坑"

微服务清单里写了 `MYSQL_HOST=mysql`，但 `mysql.yaml` 里创建的 Service 实际叫 **`mysql-master`** 和 **`mysql-slave`**——并没有叫 `mysql` 的 Service。也就是说按这个名字连库会 DNS 解析失败。

这正是学习的好素材：**K8s 里环境变量里的主机名 = Service 名，写错一个字母就连不上**。可以自己动手修：

```bash
# 方案 A：把各服务清单里的 MYSQL_HOST 改成 mysql-master
# 方案 B：在 mysql.yaml 里补一个名字叫 mysql 的 Service（指向主库）
```

---

## 五、逐行精读②：中间件清单（MySQL）和"有状态"的落地

`deploy/k8s/mysql.yaml` 是最复杂的清单，一个文件包含 5 个资源，值得完整看一遍。

### 5.1 ConfigMap：配置文件和镜像解耦

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mysql-master-config
  namespace: ai-customer-service
data:
  master.cnf: |                     # | 表示多行文本
    [mysqld]
    server-id=1
    log-bin=mysql-bin               # 开启 binlog（主从复制的基础）
    ...
```

然后在 StatefulSet 里把它"挂"进容器，覆盖 MySQL 默认配置：

```yaml
          volumeMounts:
            - name: master-config
              mountPath: /etc/mysql/conf.d/master.cnf
              subPath: master.cnf    # 只挂这一个文件，而不是整个目录
      volumes:
        - name: master-config
          configMap:
            name: mysql-master-config  # ← 引用上面的 ConfigMap
```

**好处**：改配置不用重新打镜像，改 ConfigMap 再重启 Pod 即可。和 `application.yml` 一个道理——配置和代码分离。

### 5.2 两个 Service 把主从分开

```yaml
# Service mysql-master  → selector: app=mysql, role=master  → 永远指向主库（写）
# Service mysql-slave   → selector: app=mysql, role=slave   → 指向 2 个从库（读）
```

应用代码里"读写分离"就能直接用这两个稳定域名：写连 `mysql-master:3306`，读连 `mysql-slave:3306`。**这就是 Service 的价值：底下 Pod 换了多少轮，这两个名字永远不变。**

### 5.3 StatefulSet：数据怎么不丢

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql-master
spec:
  serviceName: mysql-master         # 关联 Service
  replicas: 1
  ...
  volumeClaimTemplates:             # ★ 每个 Pod 自动创建一张"硬盘申请单"（PVC）
    - metadata:
        name: mysql-master-data
      spec:
        accessModes: ["ReadWriteOnce"]   # 只能被一个节点挂载（块存储的典型模式）
        storageClassName: local-path     # 用哪个"存储供应商"
        resources:
          requests:
            storage: 10Gi                # 要 10Gi
```

**`storageClassName: local-path` 是什么？** 这是集群里提前装好的"存储供应商"（StorageClass）。Pod 说"我要 10Gi"，它就在节点磁盘上划一块出来自动供上。K8s 里存储的完整链条：

```
Pod（挂载）→ volumeClaimTemplates 生成 PVC（申请单）
           → StorageClass local-path 自动创建 PV（真实硬盘）→ 数据落在节点磁盘上
```

**为什么 Pod 删了数据还在？** 因为 PVC 的生命周期和 Pod 无关。Pod 重建后，StatefulSet 保证它重新挂上**同名 PVC**——数据还是原来那份。这就是"有状态"的全部含义。

### 5.4 探针的三种写法，本项目都出现了

| 写法 | 原理 | 本项目实例 |
|------|------|-----------|
| `exec` | 在容器里执行命令，退出码 0 = 健康 | MySQL：`mysqladmin ping` |
| `tcpSocket` | 只测端口能否建立 TCP 连接 | 8 个 Java 微服务 |
| `httpGet` | 请求一个 HTTP 路径，2xx/3xx = 健康 | Nacos：`/nacos/v1/console/health/readiness` |

> 思考题：微服务用 `tcpSocket` 的缺点是什么？——端口活着不代表应用活着（比如线程池打满、假死）。更严谨的做法是 `httpGet /actuator/health`（Spring Boot Actuator）。本项目选 tcpSocket 是为了学习环境简单可靠，你可以自己动手升级它。

---

## 六、流量从哪进、怎么走？（把网络链路搞明白）

### 6.1 外部请求进来：NodePort 是唯一的门

8 个微服务全是 ClusterIP（外部摸不到），**只有 api-gateway 是 NodePort**：

```yaml
# services/api-gateway.yaml
spec:
  type: NodePort
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30080    # 在每个节点上开 30080 端口（K8s 规定范围 30000-32767）
```

```
浏览器请求 http://<任意节点IP>:30080/api/chat
   ↓
节点的 30080 端口（kube-proxy 在转发）
   ↓
Service api-gateway（负载均衡到健康的网关 Pod）
   ↓
api-gateway Pod（Spring Cloud Gateway 按路由规则转发）
```

这就是"网关是系统唯一入口"在 K8s 层面的实现。

### 6.2 集群内部怎么互访？本项目有两条路径

**路径 A：微服务 → 中间件，走 K8s DNS。**
CoreDNS 把 Service 名解析成 ClusterIP：

```
ai-chat-service 容器里配 NACOS_ADDR=nacos:8848
  → K8s DNS 把 nacos 解析成 Service nacos 的 ClusterIP
  → kube-proxy 转发到某个 nacos Pod 的 8848
完整域名：nacos.ai-customer-service.svc.cluster.local（同命名空间内短名 nacos 就够）
```

对比 Docker Compose：也是"服务名互访"，但那是 Compose 内置的 DNS；K8s 里换成 CoreDNS，原理一样、机制更强。

**路径 B：微服务 → 微服务，走 Nacos 注册中心（项目现状）。**
每个微服务启动时把自身注册到 Nacos（`NACOS_ADDR=nacos:8848`），网关要从 user-service 转发请求时，先问 Nacos "user-service 的实例列表"，再从列表里挑一个 Pod IP 直连。

> 延伸思考：K8s 本身也提供服务发现（Service + DNS），理论上微服务间可以直接走 K8s Service。本项目沿用 Nacos 是 Spring Cloud Alibaba 体系的习惯，还能获得配置中心、权重路由等能力。两套机制并存正是真实项目的常态，理解各自分工即可。

---

## 七、动手实战：把整套系统部署起来（按顺序执行）

### 7.0 准备一个本地集群（三选一）

```bash
# 方案 A：Minikube（最常用）
minikube start --cpus=4 --memory=8g

# 方案 B：k3d（轻量，用容器模拟节点，推荐 8G 内存机器）
k3d cluster create aics

# 方案 C：三台 VirtualBox 虚拟机装 k3s（本项目镜像仓库 192.168.56.12 就是这么来的）
# k3s 自带 local-path 存储供应商，清单里的 storageClassName 直接可用
```

> 注意：清单里用了 `storageClassName: local-path`。k3s/k3d 默认就有；Minikube 需要另装 local-path-provisioner，或把清单里的 storageClassName 改成 `standard`。

### 7.1 构建并推送镜像

每个微服务目录下都有 Dockerfile（多阶段构建，见 [01-Docker容器化](./01-Docker容器化.md)）。注意 `docker build` 的上下文是**项目根目录**，因为 Dockerfile 里要 COPY 公共模块 `ai-cs-common`：

```bash
# 在项目根目录执行（以 ai-cs-chat 为例）
docker build -f ai-cs-chat/Dockerfile -t 192.168.56.12:5000/aics/ai-chat-service:latest .

# 推送到私有仓库
docker push 192.168.56.12:5000/aics/ai-chat-service:latest

# 8 个服务挨个来一遍（写个循环）
for svc in ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat \
           ai-cs-search ai-cs-message ai-cs-notify ai-cs-product; do
  docker build -f $svc/Dockerfile -t 192.168.56.12:5000/aics/${svc#ai-cs-}:latest .
done
```

私有 HTTP 仓库需要在 Docker 的 `insecure-registries` 里加 `192.168.56.12:5000`，K8s 节点同理。

### 7.2 按依赖顺序 apply（每步等上一步 Ready 再走下一步）

```bash
# ① 命名空间
kubectl apply -f deploy/k8s/namespace.yaml

# ② Secret（先复制模板再填真实值）
cp deploy/k8s/middleware-secrets.example.yaml deploy/k8s/middleware-secrets.yaml
vim deploy/k8s/middleware-secrets.yaml
kubectl apply -f deploy/k8s/middleware-secrets.yaml
# 另外微服务清单还引用了 aics-secrets（openai-api-key 等），也要创建：
kubectl create secret generic aics-secrets \
  --from-literal=openai-api-key=sk-xxx \
  -n ai-customer-service

# ③ MySQL（Nacos 依赖它的 nacos_config 库，要最先起）
kubectl apply -f deploy/k8s/mysql.yaml
kubectl rollout status statefulset/mysql-master -n ai-customer-service   # 等 Ready

# ④ 初始化数据库（把 deploy/mysql/ 下的 SQL 导进主库，nacos_config 库也在这一步建）

# ⑤ Nacos + Redis
kubectl apply -f deploy/k8s/nacos.yaml -f deploy/k8s/redis.yaml
kubectl rollout status deployment/nacos -n ai-customer-service

# ⑥ 其余中间件
kubectl apply -f deploy/k8s/rocketmq.yaml -f deploy/k8s/elasticsearch.yaml \
              -f deploy/k8s/neo4j.yaml -f deploy/k8s/chroma.yaml \
              -f deploy/k8s/minio.yaml -f deploy/k8s/xxl-job.yaml \
              -f deploy/k8s/sentinel-dashboard.yaml

# ⑦ 最后：8 个微服务
kubectl apply -f deploy/k8s/services/

# ⑧ 观察全员就绪（这个命令会实时刷新，等所有 Pod 都是 1/1 Running）
kubectl get pods -n ai-customer-service -w
```

### 7.3 验证部署成功

```bash
# 全部 Pod Running 且 READY 1/1
kubectl get pods -n ai-customer-service

# 服务都有了
kubectl get svc -n ai-customer-service

# 从外部访问网关（Minikube 下取节点 IP：minikube ip）
curl http://<节点IP>:30080/actuator/health

# 集群内部连通性测试（起个临时调试容器）
kubectl run tmp --rm -it --image=curlimages/curl -n ai-customer-service -- sh
# 容器里执行：curl http://user-service:8081/actuator/health
```

---

## 八、kubectl 日常操作手册（结合本项目场景）

### 8.1 排障四件套（90% 的问题靠它们）

```bash
# 1. 看状态：Pod 是不是 Running/Ready？
kubectl get pods -n ai-customer-service

# 2. 看事件：Pod 为什么起不来？Event 是关键线索
kubectl describe pod ai-chat-service-7d9f8b-x2v9p -n ai-customer-service

# 3. 看日志：应用自己报了什么错
kubectl logs -f ai-chat-service-7d9f8b-x2v9p -n ai-customer-service
kubectl logs -f --previous <pod名> -n ai-customer-service   # 看上一次崩溃前的日志（CrashLoopBackOff 神器）

# 4. 进容器：直接查
kubectl exec -it ai-chat-service-7d9f8b-x2v9p -n ai-customer-service -- /bin/sh
```

### 8.2 日常变更

```bash
# 扩容：把 ai-chat-service 扩到 3 个副本（K8s 自动创建 2 个新 Pod 并加入 Service 负载均衡）
kubectl scale deployment ai-chat-service --replicas=3 -n ai-customer-service

# 滚动更新：发布新版本镜像 → 逐个替换旧 Pod，全程不掉线
kubectl set image deployment/ai-chat-service \
  ai-chat-service=192.168.56.12:5000/aics/ai-chat-service:v2 -n ai-customer-service

# 盯着更新过程（成功/失败一目了然）
kubectl rollout status deployment/ai-chat-service -n ai-customer-service

# 回滚（新版本出问题，一条命令回到上一个版本）
kubectl rollout undo deployment/ai-chat-service -n ai-customer-service

# 看历史版本
kubectl rollout history deployment/ai-chat-service -n ai-customer-service

# 改完 YAML 重新生效
kubectl apply -f deploy/k8s/services/ai-chat-service.yaml

# 临时给某服务传新环境变量（验证用，正式改动要写回 YAML）
kubectl set env deployment/ai-chat-service OPENAI_MODEL=gpt-4o -n ai-customer-service
```

滚动更新为什么不会中断服务？Deployment 先建一个**新版本** Pod → 等 readinessProbe 通过 → 才杀一个旧 Pod → 循环。**readinessProbe 就是零停机更新的基石**——这也回应了 5.4：tcpSocket 探针太粗，升级成 `/actuator/health` 才能真正保证"新 Pod 真的就绪了才开始接流量"。

### 8.3 资源速查

```bash
kubectl get all -n ai-customer-service            # 命名空间里所有资源
kubectl get pods -o wide -n ai-customer-service   # 多显示 Pod IP 和所在节点
kubectl top pods -n ai-customer-service           # 实时 CPU/内存（需 metrics-server）
kubectl edit deployment ai-chat-service -n ai-customer-service  # 在线改清单
```

---

## 九、常见报错急救表（新手救命章）

先背下这条排障公式：**`kubectl describe pod` 看 Events 里的报错信息，比猜一百次都管用。**

| 现象 | 典型原因 | 处理 |
|------|---------|------|
| `ImagePullBackOff` | 镜像地址写错 / 私有仓库不可达 / 仓库没配 insecure | describe 看 Events 确认地址；`curl http://192.168.56.12:5000/v2/_catalog` 测仓库 |
| `CrashLoopBackOff` | 应用启动即崩：连不上 MySQL/Nacos、配置错、内存不够 | `kubectl logs --previous` 看崩溃前日志，通常是中间件没起或名字写错 |
| `Pending` | 没有节点满足 requests / 没有 StorageClass 供 PVC | describe 看 Events；检查 requests 是否开太大、storageClassName 是否存在 |
| Running 但 READY `0/1` | readinessProbe 一直失败 | describe 看 probe 失败详情；Java 启动慢就把 initialDelaySeconds 调大 |
| `OOMKilled` | 内存超 limits 被强杀 | 调大 limits；同时检查 JVM `-Xmx` 要小于 limits（本项目 512Mi 堆 + 1Gi limit 是安全的） |
| 服务名连不上 | Service 名拼错 / selector 和 label 不配对 | `kubectl get svc` 核对名字；`kubectl get endpoints <svc名>` 看后端是不是空的 |
| 数据丢了 | Pod 被重建后 PVC 没对上 | 确认 StatefulSet 的 volumeClaimTemplates 名字没改过 |

**本项目实景案例**：把 ai-chat-service 扩到 3 副本后突然报数据库连接失败？先 `kubectl describe` 看新 Pod 的 Events，再 `kubectl logs` 新 Pod——如果报 `UnknownHostException: mysql`，就是 4.4 讲的那个坑（清单里 `MYSQL_HOST=mysql` 但 Service 叫 `mysql-master`）。

---

## 十、学完这些之后，下一步去哪

| 主题 | 一句话说明 | 在本项目可练手的方向 |
|------|-----------|-------------------|
| HPA 自动扩缩容 | 根据 CPU/内存自动加减副本 | 给 ai-chat-service 配 HPA，压测观察扩容 |
| Ingress | 比 NodePort 更正式的外部入口（域名路由） | 用 Ingress 替代 30080，配 `api.aics.local` 域名 |
| Helm / Kustomize | 清单模板化，告别"8 个文件改 8 遍" | 把 services/ 里重复的 Deployment 抽成 Helm 模板 |
| Prometheus 监控 | 集群和应用指标可视化 | 见 [04-Prometheus可观测性](./04-Prometheus可观测性.md) |
| CI/CD 全自动部署 | git push 直接触发上面第 7.1、7.2 步 | 见 [03-JenkinsCICD流水线](./03-JenkinsCICD流水线.md) |

---

## 动手练习（强烈建议全部做完）

1. 装好 Minikube/k3d，只 apply `namespace.yaml`，用 `kubectl get ns` 确认创建成功
2. 完整走一遍第七节的部署顺序，体会"哪一步不等上一步会报什么错"
3. 故意把某个 Service 的 `selector` 改错再 apply，观察 `kubectl get endpoints` 的变化，然后改回来
4. `kubectl scale` 把 ai-chat-service 扩到 3 副本，`kubectl get pods -o wide` 看多个 Pod 怎么被负载均衡
5. 用 `kubectl set image` 发布一个不存在的镜像版本，观察滚动更新如何卡住，再 `kubectl rollout undo` 回滚
6. 删掉一个 MySQL Pod（`kubectl delete pod mysql-master-0`），观察 StatefulSet 如何自动重建并重新挂上原来的 PVC，数据还在

---

## 学习检查清单

- [ ] 能说清"代码到用户可访问"的 6 步链路，以及每一步谁负责（Docker / 镜像仓库 / K8s）
- [ ] 理解 K8s 的工作模式：声明期望状态 → 控制器逼近期望状态
- [ ] 知道本项目 8 个微服务用 Deployment、6 个存储类中间件用 StatefulSet 的原因
- [ ] 吃透 Service 和 Pod 通过 label/selector 配对的关系
- [ ] 理解 Service DNS 名（如 `nacos:8848`）在环境变量里的作用，并知道 `MYSQL_HOST` 那个坑
- [ ] 理解 ConfigMap 挂载配置、Secret 存密码、PVC 持久化数据的机制
- [ ] 分得清三种探针（exec/tcpSocket/httpGet）和 liveness/readiness 的不同后果
- [ ] 能按正确依赖顺序把整套系统部署起来并访问网关验证
- [ ] 遇到 Pod 异常，第一反应是 `kubectl describe pod` 看 Events
- [ ] 会扩容、滚动更新、回滚三件套

---

## 下一步

→ [03-JenkinsCICD流水线](./03-JenkinsCICD流水线.md)（把本文第 7 节的手工步骤全部自动化）
→ [04-Prometheus可观测性](./04-Prometheus可观测性.md)（部署起来之后，怎么看它运行得好不好）
