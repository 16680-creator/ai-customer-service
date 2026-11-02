# 07-运维部署

> 从代码到生产的完整链路。不只讲怎么部署，讲怎么让部署"零故障"。

---

## 📚 章节导航

| 章节 | 核心内容 | 难度 | 适用场景 |
|------|---------|------|---------|
| Docker 容器化 | 镜像构建 / 多阶段 / 优化 / 安全 | ⭐⭐ | 本地开发 / CI/CD |
| Kubernetes 编排 | Pod / Service / Deployment / 调度 | ⭐⭐⭐ | 云原生部署 |
| CI/CD 流水线 | GitHub Actions / GitLab CI / 自动化测试 | ⭐⭐ | 交付自动化 |
| 监控告警 | Prometheus / Grafana / ELK | ⭐⭐⭐ | 生产可观测性 |
| 日志系统 | ELK Stack / Loki / 中心化日志 | ⭐⭐ | 故障排查 |
| 配置管理 | Nacos / Apollo / 环境管理 | ⭐⭐ | 多环境治理 |
| 灾备与恢复 | 备份策略 / RTO/RPO / 演练 | ⭐⭐⭐⭐ | 高可用设计 |

---

## 🎯 学习路线

### 快速路线（1周）
```
Docker → CI/CD → 基础监控
容器化 → 自动化 → 可观测性
```

### 完整路线（3周）
```
Docker → K8s → CI/CD → 监控告警 → 日志系统 → 灾备恢复
```

### 按角色选路

**开发者路线**
- Docker（本地开发 / 测试）
- CI/CD（自动化测试 + 部署）
- 监控（性能基线）

**SRE/DevOps 路线**
- K8s（生产编排）
- 监控告警（主动发现问题）
- 灾备恢复（业务连续性）
- 日志系统（事后分析）

---

## 💡 核心知识点

### Docker 多阶段构建（对标 22-Python速成/20 篇）

```dockerfile
# ❌ 臃肿镜像（包含所有编译依赖）
FROM maven:3.8-openjdk-17
WORKDIR /app
COPY . .
RUN mvn clean package
CMD ["java", "-jar", "target/app.jar"]
# 结果: 1.2GB

# ✅ 精简镜像（只保留运行时）
FROM maven:3.8-openjdk-17 as builder
WORKDIR /app
COPY . .
RUN mvn clean package

FROM openjdk:17-slim
COPY --from=builder /app/target/app.jar /app/
CMD ["java", "-jar", "/app/app.jar"]
# 结果: 350MB （减少 71%）
```

### K8s 三大核心概念

| 概念 | 作用 | 对标传统 |
|------|------|---------|
| Pod | 最小调度单位（1 或多容器） | Docker 容器 |
| Service | 稳定网络入口（LB + DNS） | Nginx 反向代理 |
| Deployment | 有状态应用编排与滚动更新 | 进程管理器（Supervisor） |

### 监控告警三层

```
应用层（Prometheus）
  ↓ 指标采集
基础设施层（Node Exporter）
  ↓ CPU / 内存 / 磁盘
告警规则（AlertManager）
  ↓ 通知 / 升级 / 自愈
```

### 日志中心化（ELK vs Loki）

| 方案 | 成本 | 查询速度 | 适用规模 |
|------|------|---------|---------|
| ELK | 中等（磁盘密集） | 快（Lucene 索引） | PB 级日志 |
| Loki | 低（仅索引标签） | 慢（全表扫描） | 百GB-TB |

---

## 🔧 实战场景

### 场景 1：灰度发布
**目标**：新版本从 5% → 50% → 100% 的流量

**K8s 实现**（参考金丝雀部署）：
```yaml
# 旧版本 95% 流量
Deployment replicas: 19

# 新版本 5% 流量
Deployment replicas: 1

# 监控指标 10 min → 50% 流量
# 监控指标 20 min → 100% 流量
```

### 场景 2：紧急回滚
**问题**：新版本发现严重 bug，需秒级回滚

**K8s 实现**：
```bash
# 查看发布历史
kubectl rollout history deployment/myapp

# 秒级回滚到上一版本
kubectl rollout undo deployment/myapp

# 验证
kubectl get rs   # 新旧两个 ReplicaSet 的 Pod 数翻转
```

### 场景 3：成本优化
**目标**：削减 50% 云计算成本

**优化清单**：
1. 右调容器资源限制（不是一步到位 2C4G）
2. 用 HPA 自动扩缩容（而不是固定副本数）
3. 用 Spot 实例跑无状态服务
4. 合理设置 Pod Disruption Budget（保证可用性）

---

## 📊 部署流程对比

### 传统部署（手工+脚本）
```
开发 commit
  ↓ (手工触发)
测试环境编译、部署、测试
  ↓ (人工审批)
生产环境部署
  ↓ (监控 + 告警)
故障通知 → 人工排查 → 紧急回滚
```
**问题**：易出错、慢、依赖人

### 现代部署（CI/CD + K8s）
```
开发 commit
  ↓ (自动)
测试 / 构建 / 代码扫描
  ↓ (自动)
构建镜像 / 推送仓库
  ↓ (自动或一键)
K8s 滚动更新 / 灰度发布
  ↓ (自动)
监控 / 日志 / 告警 → 自动降级 / 回滚
```
**优势**：快速、可靠、自愈

---

## ⚡ 常见问题速查

### Q1：镜像为什么这么大？
**诊断**：
```bash
docker images | grep myapp
# myapp:latest  1.2GB

docker history myapp:latest
# 看哪一层最大
```

**修复**：多阶段构建 / 清理 apt 缓存 / 用 slim 基础镜像

### Q2：Pod 一直 CrashLoopBackOff？
**诊断**：
```bash
kubectl logs <pod-name>       # 查看日志
kubectl describe pod <pod-name>  # 查看事件
```

**常见原因**：
- 配置缺失（ConfigMap / Secret）
- 资源不足（内存溢出）
- 健康检查失败

### Q3：部署后流量没有流向新版本？
**诊断**：
```bash
kubectl rollout status deployment/myapp
kubectl get pods -o wide   # 看 Pod 分布

# 检查 Service selector
kubectl get svc myapp -o yaml | grep selector
```

**常见原因**：标签（label）选择错误 / Deployment 副本数没更新

---

## 📈 部署检查清单

```
□ 镜像大小 < 500MB （云原生 SLA）
□ 启动时间 < 30s （K8s 探针超时）
□ 内存上限 = 运行时 peak + 20% 缓冲
□ CPU 限制 = 基准 + 突刺容量
□ 健康检查配置正确（readiness / liveness）
□ 日志输出到 stdout （K8s 日志采集）
□ 优雅关闭（SIGTERM 处理）
□ 灾备方案文档化（RTO / RPO）
□ 灰度发布流程验证过
□ 监控告警已配置
□ 回滚计划已演练
```

---

## 🔗 与其他模块的关联

```
07-运维部署
  ├── ← 01-Java基础（JVM 启动参数）
  ├── ← 02-Spring微服务（Nacos 配置）
  ├── ← 16-云原生与GitOps（K8s / Helm）
  ├── ← 22-Python速成/20 篇（Docker / 部署）
  └── → 12-性能工程（基准测试验证部署前后）
```

---

## 📚 推荐资源

- [K8s 官方文档](https://kubernetes.io/docs/)
- [Docker Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Prometheus 告警规则](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)
- [22-Python速成/20 篇 部署与生产运行](../22-Python速成/20-部署与生产运行.md)
