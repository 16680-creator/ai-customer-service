# 04-Prometheus 可观测性：指标 + 链路 + 看板

> 对应项目文件：`deploy/docker-compose/docker-compose-observability.yml`、`deploy/observability/`（prometheus.yml / tempo.yaml / otel-collector.yaml / grafana 数据源）、各服务 `application.yml` 的 management 配置
> 定位：前面几篇解决"部署上去"，这一篇解决"**跑起来之后，怎么知道它好不好**"。

---

## 零、先定位：可观测性在整套部署逻辑里的位置

```
部署链路（前面几篇）：
  git push → CI/CD → 镜像仓库 → K8s 运行 → 用户可访问 ✅
                                              │
                                    然后呢？服务跑得怎么样？
                                              ▼
可观测性（本篇）回答三类问题：
  ① 死活/资源  → 指标 Metrics：QPS 多少？内存涨没涨？接口慢不慢？（Prometheus）
  ② 一次请求   → 链路 Tracing：这笔下单在 11 个服务里走了一遍？卡在哪个服务？（Tempo）
  ③ 出了什么事 → 日志 Logs：（本项目暂用文件日志，Loki/ELK 为演进方向）
```

一句话区分：**指标告诉你"有问题"，链路告诉你"问题出在哪一步"，日志告诉你"那一步的具体报错"。**

---

## 一、两条数据管道全景图（先记住这张图）

本项目观测栈有两条完全独立的管道，方向还不一样（pull vs push）：

```
【管道 A：指标 —— Prometheus 主动"拉"】

 11 个微服务                         Prometheus              Grafana
 每个都暴露                          每 15s 主动来抓           查询展示
 /actuator/prometheus  ◄───pull────  prometheus.yml        ◄──PromQL──  浏览器
 （打开是纯文本指标）                 里面列了 11 个目标          │
                                    8080~8090                  │
                            http://localhost:9090 ◄────────────┘ 自查

【管道 B：链路 —— 应用主动"推"】

 11 个微服务                OTel Collector            Tempo
 请求带上 traceId           (中转/缓冲站)             (存 24h，可查询)
 micrometer 自动生成 span ──push──▶ OTLP:4317 ──push──▶ Tempo:3200
                          http 4318                TraceQL 查询
```

**为什么指标是拉、链路是推？**（高频面试题，第 5.3 节有完整答案）

---

## 二、指标管道精读：从一行配置到一张图

### 2.1 应用侧：3 个依赖 + 一段统一配置

每个微服务引入（版本由 Spring Boot BOM 管）：

```
spring-boot-starter-actuator            # 暴露 /actuator/* 端点
micrometer-registry-prometheus          # 把内部指标转成 Prometheus 文本格式
```

统一配置（Nacos 共享配置 `aics-shared.yml`，所有服务生效）：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus   # 只开放这 3 个端点，不裸奔全部
  metrics:
    tags:
      application: ${spring.application.name}
      # 每条指标都盖上服务名标签 —— Grafana 才能按服务聚合/筛选
```

### 2.2 验证：不启动任何观测组件也能看

```bash
curl http://localhost:8083/actuator/prometheus | grep jvm_memory_used
# 输出 OpenMetrics 文本：
# jvm_memory_used_bytes{area="heap",application="ai-cs-chat",...} 2.348E8
```

这行命令是理解指标管道的最好入口——Prometheus 抓的就是这个文本。

### 2.3 抓取侧：prometheus.yml 里写死 11 个目标

```yaml
# deploy/observability/prometheus.yml
global:
  scrape_interval: 15s            # 每 15s 抓一轮（= 数据粒度）
scrape_configs:
  - job_name: 'ai-cs-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'host.docker.internal:8080'   # gateway
          - 'host.docker.internal:8081'   # user
          - 'host.docker.internal:8082'   # knowledge
          - 'host.docker.internal:8083'   # chat
          - 'host.docker.internal:8084'   # search
          - 'host.docker.internal:8085'   # message
          - 'host.docker.internal:8086'   # notify
          - 'host.docker.internal:8087'   # order
          - 'host.docker.internal:8088'   # product
          - 'host.docker.internal:8089'   # pay
          - 'host.docker.internal:8090'   # mq
```

- 观测栈跑在 Docker 里、服务跑在宿主机，所以用 `host.docker.internal`（Linux 宿主机要在 compose 里加 `extra_hosts: host.docker.internal:host-gateway`，文件里有注释）
- `static_configs` 是静态写死的，适合学习；服务多了要换成**服务发现**（如 K8s SD，让 Prometheus 自己发现新 Pod）

### 2.4 本项目常用指标族（看监控先看这几个）

| 指标族 | 看什么 | 出问题的典型表现 |
|--------|--------|----------------|
| `jvm_memory_*` | JVM 堆内存 | 只涨不跌 → 内存泄漏 |
| `hikaricp_connections*` | 数据库连接池 | active 顶满 pending > 0 → 慢 SQL |
| `http_server_requests_seconds_*` | 接口延迟分布 | 0.99 分位暴涨 → 尾部延迟 |
| `rocketmq_producer_*` | 消息发送 | 失败计数增长 → MQ 抖动 |

PromQL 上手示例（在 Grafana 或 9090 页面直接试）：

```promql
# 网关每秒请求数（按路由聚合）
sum(rate(http_server_requests_seconds_count{application="ai-cs-gateway"}[1m]))

# 各服务接口 P99 延迟
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))

# 哪个服务堆内存用得最多
topk(3, jvm_memory_used_bytes{area="heap"})
```

---

## 三、链路管道精读：一次请求怎么被"全程录像"

### 3.1 Trace 的基本单位（30 秒建立概念）

```
一条 trace（完整链路）= 一个全局 traceId + 若干 span（每段工作）

用户下单查询的 trace 长这样（在 Tempo 里看到的样子）：
  traceId: 4bf92f3577b34da6a3ce929d0e0e4736
  ├─ span: api-gateway /GET /api/order/1        12ms
  │         └─ span: ai-cs-order /GET /1        9ms
  │               └─ span: ai-cs-product ...    4ms   ← 调下游
  │               └─ span: mysql SELECT         2ms   ← DB 客户端
  同一个 traceId 贯穿所有服务 —— 这就是"链路"
```

### 3.2 traceId 怎么跨服务传？——W3C traceparent 头

```
api-gateway ── HTTP 请求 ──────────────────► ai-cs-order
  请求头带:
    traceparent: 00-4bf92f...4736-01    ▲
                     └ traceId    │ 服务端收到请求，MVC 拦截器自动解析该头
                                  │ 新 span 接到同一个 trace 上
                                  │ 再调 ai-cs-product 时，client 拦截器自动把头带下去
```

本项目各环节的落点：

| 环节 | 谁负责 | 本项目实现 |
|------|--------|-----------|
| 生成 span / traceId | micrometer-tracing | `micrometer-tracing-bridge-otel`（全部 11 个服务） |
| 服务间 HTTP 调用自动注入/解析 traceparent | micrometer 拦截器 | Feign / RestTemplate / restclient 自动 |
| 网关（Reactor 异步流）上下文传递 | Reactor Hook | Boot 自动开启 `Hooks.enableAutomaticContextPropagation()` |
| MQ 异步段接续 | RocketMQ instrumentation | traceparent 放进消息属性，消费侧自动接续 |
| 导出 | OTLP HTTP | `opentelemetry-exporter-otlp` → `OTLP_ENDPOINT` |

chat 服务额外保留了一份手写的 Observation→OTel 桥接（带 LLM 专用 handler，能记录模型调用），其余服务都是 Boot 3.2 标准自动装配——**依赖在 classpath + 一个 endpoint 配置，自动装配链就通了**：

```
micrometer-tracing-bridge-otel 在 classpath → Boot 注册 Tracer，Observation 桥接为 span
management.otlp.tracing.endpoint 有值     → OtlpAutoConfiguration 创建 OtlpHttpSpanExporter
                                          → span 批量推给 OTel Collector → Tempo
```

### 3.3 采样：不是每条请求都要录

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING:1.0}   # 1.0 = 全采样（本地/测试环境）
```

- 本地全采样便于排查；生产建议 0.1 起步（录 10%），支付等关键链路单独全采样
- 一处环境变量控制，配合 Nacos 可动态调整

---

## 四、一键拉起观测栈 + 端到端验证

### 4.1 启动

```bash
docker compose -f deploy/docker-compose/docker-compose-observability.yml up -d

# 四个组件：
#   Prometheus    http://localhost:9090   指标库 + PromQL 自查
#   Grafana       http://localhost:3000   看板（admin/admin，数据源已自动注入）
#   Tempo         http://localhost:3200   链路库（本地存储，留 24h）
#   OTel Collector  4317/4318             链路中转站（grpc/http）
```

### 4.2 端到端验证清单

```bash
# ① 指标通了没：Prometheus 页面 Status → Targets，11 个 target 应全 UP
#    或命令行：curl 'http://localhost:9090/api/v1/targets'

# ② 链路通了没：走一笔真实请求
curl http://localhost:8080/api/order/1        # 网关 → order → product

# ③ Tempo 查询（TraceQL）
{service.name="ai-cs-order"}
# 应看到一条贯穿 gateway → order → product 的完整 trace，三个 span 同 traceId

# ④ MQ 异步段：支付成功消息的消费 span 应接续生产侧 traceId
#    （traceparent 随消息属性传递，micrometer 自动处理）

# ⑤ Grafana：Add visualization → 选 Prometheus 数据源 → 粘 2.4 节的 PromQL
```

无 Docker 时的替代验证：直接 curl `/actuator/prometheus`（指标）；把 `OTLP_ENDPOINT` 临时指向任意 OTLP 兼容端点（链路）；`/actuator/health`、`/actuator/metrics` 兜底。

---

## 五、面试高频（附本项目答案素材）

1. **pull vs push，为什么指标拉、链路推？**
   指标是聚合值、需要统一的抓取周期做时序对比，pull 模型天然带"目标失联=挂了"的健康语义，还方便 Prometheus 做服务发现；span 数量大、单条生命周期短、偶尔丢失可容忍，批量推送对应用影响最小。两条管道在本项目分别落地为 Prometheus 抓 15s 一次、OTLP 推给 Collector。

2. **指标标签设计**：`application`（服务名）这类低基数标签安全；userId、orderNo 这类**高基数**值绝不能做标签——每个唯一值都是一个时间序列，基数爆炸会打爆 Prometheus 内存（本项目靠 `metrics.tags.application` 固定低基数维度）。

3. **Trace 上下文传播**：W3C `traceparent` 头；HTTP 调用靠 client interceptor 自动注入、server 自动解析；Reactor 异步流靠 `Hooks.enableAutomaticContextPropagation()`；MQ 靠消息属性携带。

4. **头部采样 vs 尾部采样**：本项目用头部采样（写入时按概率决定，省资源但可能恰好丢掉出问题的那条）；尾部采样在 Collector 端按完整 trace 决定保留谁（能保住慢/错请求，成本高）。生产可以 Collector 加尾部采样规则保关键错误链路。

---

## 动手练习

1. 起观测栈，Prometheus Targets 页面截图，找出哪个服务没 UP 并排查（多半是服务没起）
2. 写 3 条 PromQL：网关 QPS、chat 服务堆内存、全系统 P99 延迟，在 Grafana 画出来
3. 走一笔 `curl localhost:8080/api/order/1`，在 Tempo 用 TraceQL 找到这条 trace，展开看 gateway→order→product 的 span 树
4. 把 `TRACING_SAMPLING` 设成 0，重启服务再请求，验证 Tempo 里不再有新 trace
5. 思考题：如果给指标加了 `userId` 标签，压测 10 分钟会发生什么？（基数爆炸，内存飙升——自己动手在本地 Prometheus 上观察 series 增长）

---

## 学习检查清单

- [ ] 能画出指标（pull）和链路（push）两条管道的完整数据流
- [ ] 知道 `/actuator/prometheus` 输出的是 OpenMetrics 文本，且不依赖任何观测组件就能看
- [ ] 理解 `metrics.tags.application` 低基数标签的作用
- [ ] 能说出 traceId/span/traceparent 的关系，以及跨服务、跨 MQ 怎么传播
- [ ] 知道采样概率配置在哪、本地为什么全采样
- [ ] 会用 Prometheus Targets 页面和 TraceQL 做连通性验证
- [ ] 能回答"为什么指标 pull、链路 push"

---

## 下一步

→ [05-XXL-Job分布式调度](./05-XXL-Job分布式调度.md)（部署与监控之外的第三块拼图：定时任务治理）
