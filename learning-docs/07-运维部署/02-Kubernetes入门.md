# Kubernetes 入门

> 本项目 `deploy/k8s/` 目录包含完整的 K8s 部署清单，用于生产环境编排。
> 对应项目文件：`deploy/k8s/` 目录

---

## 一、为什么需要 K8s？

```
Docker Compose：适合单机
  → 一台服务器挂了，所有服务都挂

Kubernetes：适合集群
  → 多台服务器，自动调度、自动恢复、自动扩缩
```

| 能力 | Docker Compose | Kubernetes |
|------|---------------|------------|
| 部署 | 单机 | 多机集群 |
| 故障恢复 | 手动 | 自动重启/迁移 |
| 扩容 | 手动改配置 | 一条命令/自动 |
| 滚动更新 | 不支持 | 零停机更新 |
| 服务发现 | 服务名 | 内置 DNS |

---

## 二、核心概念

```
Kubernetes 集群
├── Master（控制平面）
│   ├── API Server     ← 所有操作的入口
│   ├── Scheduler      ← 决定 Pod 跑在哪台机器
│   └── etcd           ← 存储集群状态
│
└── Worker（工作节点）× N
    ├── Pod            ← 最小部署单元（一个或多个容器）
    ├── Deployment     ← 管理 Pod 的副本数和更新策略
    ├── Service        ← 提供稳定的访问入口
    └── Ingress        ← 外部流量入口（类似 Gateway）
```

### 概念对照

| K8s 概念 | 类比 | 本项目对应 |
|----------|------|-----------|
| Namespace | 文件夹（隔离环境） | `aics` 命名空间 |
| Pod | 一个进程 | 一个微服务实例 |
| Deployment | 进程管理器 | 确保 N 个副本运行 |
| Service | 负载均衡器 | 服务间访问 |
| ConfigMap | 配置文件 | application.yml |
| Secret | 加密配置 | 数据库密码、API Key |

---

## 三、本项目的 K8s 清单

### 3.1 命名空间

```yaml
# deploy/k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: aics
  labels:
    project: ai-customer-service
```

### 3.2 部署一个微服务（示例）

```yaml
# deploy/k8s/services/ai-cs-chat.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-cs-chat
  namespace: aics
spec:
  replicas: 2                    # 2 个副本（高可用）
  selector:
    matchLabels:
      app: ai-cs-chat
  template:
    metadata:
      labels:
        app: ai-cs-chat
    spec:
      containers:
        - name: ai-cs-chat
          image: ai-cs-chat:latest
          ports:
            - containerPort: 8083
          env:
            - name: OPENAI_API_KEY
              valueFrom:
                secretKeyRef:
                  name: aics-secrets
                  key: openai-api-key
            - name: SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR
              value: "nacos:8848"
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:          # 存活探针（挂了自动重启）
            httpGet:
              path: /actuator/health
              port: 8083
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:         # 就绪探针（没准备好不接流量）
            httpGet:
              path: /actuator/health
              port: 8083
            initialDelaySeconds: 20
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: ai-cs-chat
  namespace: aics
spec:
  selector:
    app: ai-cs-chat
  ports:
    - port: 8083
      targetPort: 8083
  type: ClusterIP              # 集群内部访问
```

### 3.3 基础设施（MySQL 示例）

```yaml
# deploy/k8s/mysql.yaml（简化版）
apiVersion: apps/v1
kind: StatefulSet              # 有状态服务用 StatefulSet
metadata:
  name: mysql
  namespace: aics
spec:
  serviceName: mysql
  replicas: 1
  template:
    spec:
      containers:
        - name: mysql
          image: mysql:8.0
          ports:
            - containerPort: 3306
          env:
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: aics-secrets
                  key: mysql-password
          volumeMounts:
            - name: mysql-data
              mountPath: /var/lib/mysql
  volumeClaimTemplates:
    - metadata:
        name: mysql-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 10Gi      # 10GB 持久化存储
```

---

## 四、常用 kubectl 命令

```bash
# 查看所有 Pod
kubectl get pods -n aics

# 查看 Pod 详情（排查问题）
kubectl describe pod ai-cs-chat-xxx -n aics

# 查看日志
kubectl logs -f ai-cs-chat-xxx -n aics

# 进入 Pod
kubectl exec -it ai-cs-chat-xxx -n aics -- /bin/sh

# 扩容
kubectl scale deployment ai-cs-chat --replicas=3 -n aics

# 滚动更新（换镜像版本）
kubectl set image deployment/ai-cs-chat ai-cs-chat=ai-cs-chat:2.0 -n aics

# 查看更新状态
kubectl rollout status deployment/ai-cs-chat -n aics

# 回滚
kubectl rollout undo deployment/ai-cs-chat -n aics

# 部署清单
kubectl apply -f deploy/k8s/
```

---

## 五、探针（健康检查）

```yaml
# 三种探针
livenessProbe:     # 存活：失败 → 重启容器
readinessProbe:    # 就绪：失败 → 从 Service 摘除（不接新流量）
startupProbe:      # 启动：给慢启动服务更多时间

# 本项目 Spring Boot 的 Actuator 端点
httpGet:
  path: /actuator/health    # Spring Boot 健康检查
  port: 8083
```

---

## 六、学习路径建议

```
第一周：理解概念
  → Pod、Deployment、Service、Namespace

第二周：本地实践
  → 安装 Minikube/Kind，部署一个简单服务

第三周：部署本项目
  → 用 deploy/k8s/ 清单部署完整系统

第四周：进阶
  → HPA 自动扩缩、Ingress、Helm Chart
```

---

## 学习检查清单

- [ ] 理解 K8s 解决的核心问题（编排、自愈、扩缩）
- [ ] 理解 Pod / Deployment / Service 的关系
- [ ] 会写基本的 Deployment + Service YAML
- [ ] 理解 ConfigMap 和 Secret 的用途
- [ ] 理解 liveness / readiness 探针
- [ ] 会用 kubectl 基本命令
- [ ] 了解 StatefulSet（有状态服务）

---

## 下一步

→ [03-JenkinsCICD流水线](./03-JenkinsCICD流水线.md)
