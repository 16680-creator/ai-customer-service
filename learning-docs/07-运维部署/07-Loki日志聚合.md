# 07-Loki 日志聚合：补齐可观测性第三支柱

> 2026-09 落地记录：本项目可观测性已有指标（Prometheus，11 个服务 `/actuator/prometheus`）与链路（Tempo + OTel Collector），见 [04-Prometheus可观测性](04-Prometheus可观测性.md)；但**日志仍是每台机器的控制台文本**——全 deploy 目录 grep `loki` 零命中，OTel Collector 配置只有 traces 管道，应用侧没有 logback-spring.xml、无 JSON 日志。本篇把"日志聚合"这第三根支柱的落地方案写成可执行步骤。
> 诚实定位：这是**升级路线文档**（当前未落地），所有 yaml 为目标态配置。

---

## 一、三支柱盘点：缺的就是 logs

```text
deploy/docker-compose/docker-compose-observability.yml 现状：
┌──────────────┬────────────────────────────────┬──────────┐
│ 支柱         │ 链路                           │ 状态     │
├──────────────┼────────────────────────────────┼──────────┤
│ Metrics      │ actuator → Prometheus 9090     │ ✅ 已通  │
│ Traces       │ OTLP → Collector → Tempo 3200  │ ✅ 已通  │
│ Logs         │ 控制台文本，随容器销毁蒸发       │ ❌ 缺失  │
└──────────────┴────────────────────────────────┴──────────┘
```

`deploy/observability/otel-collector.yaml` 全文只有 20 行，管道一览：

```yaml
receivers: { otlp: { protocols: { grpc: 0.0.0.0:4317, http: 0.0.0.0:4318 } } }
exporters: { otlp/tempo: { endpoint: tempo:4317, tls: { insecure: true } } }
service: { pipelines: { traces: { receivers: [otlp], exporters: [otlp/tempo] } } }
```

没有 logs/metrics 管道、没有 processors。Grafana provisioning（`deploy/observability/grafana/provisioning/datasources/datasources.yaml`）也只有 Prometheus 与 Tempo 两个数据源。排查一次线上问题时你会立刻感到缺口：trace 告诉你 chat 服务慢在 Feign 调用，但要**看那次调用打的具体日志**，只能 `docker logs` 翻——容器一重建就没了。

## 二、为什么选 Loki（与 ELK 对比）

Loki 是 Grafana 系的日志聚合系统，核心思想：**不索引日志正文，只索引标签**（label：服务名、环境、trace_id 等元数据）。

| 维度 | ELK（ES 栈） | Loki |
|---|---|---|
| 索引对象 | 全文倒排索引 | 仅标签（正文压缩存储） |
| 存储成本 | 高（内存换全文检索） | 低（对象存储/本地盘即可） |
| 查询语言 | Query DSL / KQL | LogQL（像 PromQL） |
| 全文检索 | 强 | 一般（靠 grep 式扫描） |
| 与现有栈的关系 | 引入全新体系 | 与 Prometheus/Tempo/Grafana 同门，UI 共用 |

本项目已全押 Grafana 系（Prometheus + Tempo + Grafana 10.4），选 Loki 是阻力最小的拼图；代价是放弃全文检索的爽感——对我们"按服务 + 时间 + trace_id 找日志"的主场景够用。

## 三、两条接入路线

| 路线 | 链路 | 评价 |
|---|---|---|
| A：Promtail / Docker Loki driver | 容器 stdout → Promtail 抓取 → Loki | 经典方案，但要新增一个采集组件 |
| B：OTel Collector logs 管道 | 应用 → OTLP → Collector（loki exporter）→ Loki | **推荐**：复用现有 collector（镜像已是 contrib 0.97.0，含 loki exporter），不新增组件，且与 traces 同入口 |

选 B 还有个架构理由： traces 与 logs 走同一个 OTLP 入口，未来要加 metrics 管道也只是再拼一段 pipeline——collector 成为唯一采集面。

## 四、改造步骤

### 4.1 compose 增加 Loki 服务

`docker-compose-observability.yml` 增加（`grafana/loki:2.9.x`，与 Tempo 同属 2.x 时代版本线）：

```yaml
  loki:
    image: grafana/loki:2.9.8
    ports: ["3100:3100"]
    command: -config.file=/etc/loki/local-config.yaml
    networks: [aics-observe]
```

### 4.2 Collector 增加 logs 管道（核心改动）

`deploy/observability/otel-collector.yaml`：

```yaml
exporters:
  otlp/tempo: { endpoint: tempo:4317, tls: { insecure: true } }
  loki:
    endpoint: http://loki:3100/loki/api/v1/push

processors:
  resource:
    attributes:
      - key: service.name        # 确保标签存在，Loki 按它建流
        action: upsert

service:
  pipelines:
    traces: { receivers: [otlp], exporters: [otlp/tempo] }   # 原样保留
    logs:   { receivers: [otlp], processors: [resource], exporters: [loki] }   # 新增
```

### 4.3 应用侧：先有结构化日志，再谈聚合

当前各服务用 Spring Boot 默认 pattern 输出**纯文本控制台日志**（无 logback-spring.xml、无 logging 配置键）。Loki 对纯文本也能收（整行进正文），但想要 `trace_id` 标签关联，JSON 日志是前提。给每个服务加 `logback-spring.xml`（JSON 编码器用 `logstash-logback-encoder`）：

```xml
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <customFields>{"service":"ai-cs-chat"}</customFields>
      <!-- MDC 里放 traceId/spanId，与 micrometer-tracing 打通 -->
    </encoder>
  </appender>
  <root level="INFO"><appender-ref ref="CONSOLE"/></root>
</configuration>
```

配合 micrometer-tracing 的 MDC 桥接，每行日志自带 `trace_id`——这是"从 trace 跳到日志"的钥匙（4.5 节）。

### 4.4 Grafana 增加 Loki 数据源

`deploy/observability/grafana/provisioning/datasources/datasources.yaml` 追加：

```yaml
apiVersion: 1
datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    jsonData:
      derivedFields:            # 日志行里的 trace_id 变成可点链接跳 Tempo
        - name: TraceID
          matcherRegex: '"trace_id":"(\w+)"'
          datasourceUid: tempo
```

### 4.5 三支柱闭环：一次排障的完整路径

```text
Prometheus 告警(chat 5xx↑) → Grafana 面板 → 点告警关联的 Tempo trace
  → 看到慢在 ai-cs-search 的 ES 查询 → 复制 trace_id
  → Loki 查询 {service="ai-cs-search"} |= "<trace_id>" → 看到那几行具体日志
```

LogQL 上手三条即可覆盖主场景：

```logql
{service="ai-cs-chat"} |= "ERROR"                     # 该服务错误日志
{service=~"ai-cs-.*"} | json | level="WARN"           # 跨服务 WARN（JSON 解析后过滤）
sum(rate({service="ai-cs-gateway"} |= "5xx" [5m]))    # 错误日志速率（像 PromQL）
```

## 五、验证方式

```bash
# 1. 拉起观测栈
docker compose -f deploy/docker-compose/docker-compose-observability.yml up -d

# 2. 冒烟：Loki 就绪
curl http://localhost:3100/ready        # 期望 ready

# 3. 制造日志：调用任一接口触发业务日志，然后在 Grafana Explore 查询
{service="ai-cs-chat"}                  # 能看到 JSON 行 → logs 管道通

# 4. 关联验证：从 Tempo 某条 trace 复制 trace_id，查到对应日志行 → 三支柱闭环
```

## 六、踩坑与成本控制

1. **标签基数爆炸**：不要把 userId、orderNo 这类高基数字段做标签（每个组合是一条独立"流"，会把 Loki 内存打爆）——高基数信息放日志正文，用 `| json` 运行时过滤。
2. **保留期即成本**：loki 配置里按 7~30 天设 `retention`，别默认全存。
3. **控制台 JSON 会让本地开发难受**：用 logback 的 `<springProfile>` 区分——local 用彩色纯文本，非 local 用 JSON 编码器。
4. **Collector 单点**：当前观测栈全部单实例 compose，生产化时 collector/loki 的副本与持久卷要跟上（思路同 [02-Kubernetes入门](02-Kubernetes入门.md) 的 StatefulSet 讨论）。
5. **诚实提醒**：本文 yaml 未在本机执行过，落地时以 collector 0.97.0 文档核对 `loki` exporter 参数（该 exporter 在 contrib 镜像可用，但配置项随版本演进）。

## 七、面试要点总结

> 本项目可观测性已覆盖指标（Prometheus 抓 11 个服务）与链路（OTLP → Collector → Tempo），日志是缺口：应用为纯文本控制台输出、Collector 仅有 traces 管道。补齐方案是 Grafana 同门系 Loki——只索引标签不索引正文，成本低且与现有栈 UI 共用；接入复用 contrib 版 OTel Collector 增加 logs 管道（loki exporter 推送到 Loki push API），应用侧补 logback JSON 编码与 MDC traceId，Grafana derivedFields 让日志行中的 trace_id 可点击跳转 Tempo，形成"告警 → 链路 → 日志"的排障闭环；红线是标签基数控制与保留期成本。

```text
关键词：三支柱 = Metrics/Traces/Logs · Loki 只索引标签 · LogQL ≈ PromQL
复用 collector(contrib) 而非新增 Promtail · trace_id 打通 Tempo ↔ Loki
高基数字段不做标签 · 排障闭环 = 告警→trace→日志
```

## 学习检查清单

- [ ] 能画出三支柱当前状态与目标状态两张图
- [ ] 能说出选 Loki 而非 ELK 的两条理由与一条代价
- [ ] 按第四节完成落地并跑通第五节全部验证
- [ ] 能解释"从 trace 跳日志"依赖哪两个技术点（MDC traceId + derivedFields）
