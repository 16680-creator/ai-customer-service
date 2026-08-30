# 04-Prometheus 可观测性（指标 / 链路 / 看板）

> 2026-08 落地记录：全服务暴露 `/actuator/prometheus`；提供完整观测栈编排
> （Prometheus + Grafana + Tempo + OTel Collector）。

## 一、三个支柱在本项目的落点

| 支柱 | 数据源 | 后端 | 现状 |
|---|---|---|---|
| 指标 Metrics | Micrometer + Actuator（`/actuator/prometheus`） | Prometheus | ✅ 全服务可验证 |
| 链路 Tracing | micrometer-tracing-bridge-otel（chat 已接 OTLP 导出） | Tempo（经 OTel Collector） | ✅ 编排就绪（需 Docker） |
| 日志 Logs | 文件（tools/logs/*.out.log） | Loki/ELK（可演进） | 规划中 |

## 二、指标暴露（已验证，无 Docker 即可用）

各模块引入 `spring-boot-starter-actuator` + `micrometer-registry-prometheus`，
`aics-shared.yml` 统一暴露端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: ${spring.application.name}   # 每条指标带上服务名，Grafana 按 app 聚合
```

验证：

```bash
curl http://localhost:8083/actuator/prometheus | grep jvm_memory_used
# 输出 OpenMetrics 文本，含 jvm_/http_server_/hikaricp_ 等指标族
```

常用指标族：`jvm_memory_*`（内存）、`hikaricp_connections*`（连接池）、
`http_server_requests_seconds_*`（接口延迟分布）、`rocketmq_producer_*`（消息）。

## 三、观测栈编排（Docker 环境一键拉起）

```
deploy/docker-compose/docker-compose-observability.yml   # prometheus+grafana+tempo+collector
deploy/observability/prometheus.yml                      # 抓取 11 个服务的 /actuator/prometheus
deploy/observability/tempo.yaml                          # Tempo（OTLP 接收 + 本地存储 24h）
deploy/observability/otel-collector.yaml                 # OTLP → Tempo 转发
deploy/observability/grafana/provisioning/datasources/   # 数据源自动注入
```

```bash
docker compose -f deploy/docker-compose/docker-compose-observability.yml up -d
# Grafana:  http://localhost:3000 (admin/admin)  数据源已自动装配
# Prometheus: http://localhost:9090              PromQL 自查
# Tempo:    http://localhost:3200               TraceQL 查链路
```

注意：Prometheus 抓宿主机服务用 `host.docker.internal`（Linux 需加
`extra_hosts: host.docker.internal:host-gateway`，文件内有注释说明）。

## 四、本机无 Docker 的替代验证

- 指标：直接 curl `/actuator/prometheus`（已验证）
- 链路：chat 已配 OTLP 导出（micrometer-tracing + opentelemetry-exporter-otlp），
  可临时把 exporter 指向任意 OTLP 兼容端点（如本地跑的 zipkin-all-in-one jar + OTLP 转换）
- 兜底：Actuator 的 `/actuator/health`、`/actuator/metrics` 纯 JVM 方式自查

## 五、面试高频

1. **pull vs push**：Prometheus 拉取（服务发现 + 固定端点），OTLP 链路是推送——两种模型各自适合什么数据。
2. **指标标签基数爆炸**：`application` 这种低基数标签安全，userId/orderNo 绝不能做标签。
3. **Trace 上下文传播**：W3C traceparent 头跨服务传递，Feign/RestTemplate 需要 interceptor
   （micrometer-tracing 自动桥接）。
