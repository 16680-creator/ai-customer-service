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

## 五、全链路 Tracing 铺开（2026-08 补，03-P5 落地记录）

### 5.1 改造内容

此前只有 chat 有 tracing（Tempo 里是单服务孤岛）；本次其余 10 个服务统一接入
Boot 3.2 标准自动装配（chat 保留手写的 Observation→OTel 桥接，因其含 LLM 专用 handler）：

```xml
<!-- 每个服务 pom：版本由 spring-boot-dependencies BOM 管理 -->
micrometer-tracing-bridge-otel
opentelemetry-exporter-otlp
```

```yaml
# 每个服务 application.yml 追加 management 块
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING:1.0}
  otlp:
    tracing:
      endpoint: ${OTLP_ENDPOINT:http://127.0.0.1:4318/v1/traces}
```

两个关键机制：

- **自动装配链**：`micrometer-tracing-bridge-otel` 在 classpath → Boot 注册
  `io.opentelemetry.api.trace.Tracer` 并把 micrometer Observation 桥接为 span；
  `management.otlp.tracing.endpoint` 存在 → `OtlpAutoConfiguration` 创建
  `OtlpHttpSpanExporter`，span 批量推给 OTel Collector → Tempo
- **上下文传播**：W3C `traceparent` 头。Feign/RestTemplate/restclient 由
  micrometer 的 client interceptor 自动注入；服务端 MVC/WebFlux 自动解析；
  Reactor（网关）靠 `Hooks.enableAutomaticContextPropagation()`（Boot 自动开启）

### 5.2 验证方法

1. 起 observability compose（otel-collector 4318 / Tempo 3200）
2. `curl 网关 → order → product` 走一遍下单查询
3. Tempo TraceQL：`{service.name="ai-cs-order"}` 应看到一条贯穿
   gateway→order→product 的完整 trace，三个服务的 span 同 traceId
4. MQ 异步段：支付成功消息的消费 span 应能接续生产侧 traceId
   （RocketMQ 消息属性携带 traceparent，micrometer instrumentation 自动处理）

### 5.3 采样策略

- 本地/测试全采样（1.0）便于排查；生产建议 0.1 起步，重要链路（支付）单独全采样
- 环境变量 `TRACING_SAMPLING` 一处配置，配合 Nacos 可动态调整

## 六、面试高频

1. **pull vs push**：Prometheus 拉取（服务发现 + 固定端点），OTLP 链路是推送——两种模型各自适合什么数据。
2. **指标标签基数爆炸**：`application` 这种低基数标签安全，userId/orderNo 绝不能做标签。
3. **Trace 上下文传播**：W3C traceparent 头跨服务传递，Feign/RestTemplate 需要 interceptor
   （micrometer-tracing 自动桥接）。
4. **为什么 traces 走推送、metrics 走拉取**：span 数量大、生命周期短、丢失可容忍；
   指标需要服务发现与统一抓取周期，pull 模型天然带健康检查语义。
5. **采样策略**：头部采样（写入时决定，省资源但可能丢关键 trace）vs 尾部采样
   （Collector 端按完整 trace 决定，能保留慢/错请求，成本高）。
