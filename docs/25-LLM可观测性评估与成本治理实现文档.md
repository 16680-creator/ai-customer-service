# 25 - LLM 可观测性、评估与成本治理实现文档

> 适用范围：`ai-cs-chat`、`ai-cs-message`<br>
> 实现日期：2026-08-14<br>
> 对应规格：OpenSpec 变更 `2026-08-14-llm-observability-cost-governance`（docs/15 第 3.3 节 P0）

---

## 一、目标

解决 docs/15 第 3.3 节指出的关键缺口：**线上一次请求到底发生了什么不可见**。本功能交付：

1. **LLM 全链路可观测**：`requestId` 贯穿 intent / retrieval / rerank / llm / tools / answer 各环节，可按 `requestId` 还原调用链，支持 OTLP 导出（Langfuse / Phoenix）。
2. **Token 与成本治理**：`model_usage` 统一计量，按用户、场景、模型统计 Token 与估算费用，支持配额管控。
3. **线上采样评估与反馈闭环**：按采样率对线上真实回答做 LLM-as-Judge 评分；用户点赞/点踩/评分回流。
4. **CI 门禁扩展**：在正确率门禁之外增加 P95 延迟与单请求平均 Token 上限。

---

## 二、架构总览

```text
ai-cs-chat                                    ai-cs-message
┌─────────────────────────────┐              ┌──────────────────────────┐
│ TraceInterceptor（请求入口） │              │ llm_trace 表             │
│   ├─ TraceContextHolder     │   Feign      │ model_usage 表           │
│   ├─ TraceSpanObservation…  │ ───────────► │ model_usage_quota 表     │
│   ├─ TraceRecorder ─────────┼─ TraceFeign  │ online_eval_record 表    │
│   ├─ ModelUsageRecorder ────┼─ ModelUsage  │ user_feedback 表         │
│   ├─ QuotaService ──────────┼─ Feign      │                          │
│   └─ OnlineEvalService ─────┼─ OnlineEval │                          │
│                             │  Feign       │                          │
│ Observation(埋点) + OTLP ───┼──► Langfuse / Phoenix（可选）          │
└─────────────────────────────┘              └──────────────────────────┘
```

- **埋点统一入口**：Micrometer Observation（`Observation.createNotStarted`），`TraceSpanObservationHandler` 在 `onStop` 时把 observation 组装为 `TraceSpan` 挂到当前 `TraceContext`。
- **异步传播**：`TraceContextHolder.capture()/restore()` 显式跨线程传播（LLM 调用的 `supplyAsync`、SSE 订阅回调、Rerank 的弹性线程池）。
- **审计尽力而为**：trace/用量/评估落库失败只告警，绝不阻断主业务链路。

---

## 三、配置项

### 3.1 观测（ai-cs-chat，前缀 `aics.observability`）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `aics.observability.enabled` | `true` | 观测总开关，关闭后不采集 trace |
| `aics.observability.sample-rate` | `1.0` | 采样率（0~1），生产可调低控制存储 |
| `aics.observability.otlp-endpoint` | 空 | OTLP 兼容后端地址（如 Langfuse/Phoenix），**配置后才创建 exporter** |
| `aics.observability.log-export` | `true` | 未配置 OTLP 时的结构化日志导出通道 |

### 3.2 用量计量（ai-cs-chat，前缀 `aics.usage`）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `aics.usage.enabled` | `true` | 计量总开关 |
| `aics.usage.pricing.<model>.input` | `0` | 模型输入单价（元/百万 Token） |
| `aics.usage.pricing.<model>.output` | `0` | 模型输出单价（元/百万 Token） |
| `aics.usage.default-pricing.input` / `.output` | `0` | 未配置单价的模型兜底单价 |
| `aics.usage.executor-size` | `2` | 异步上报线程池大小 |

示例：

```yaml
aics:
  usage:
    pricing:
      deepseek-chat:
        input: 1.0     # 元/百万 Token（示例值，请按实际合同价配置）
        output: 2.0
    default-pricing:
      input: 0.5
      output: 0.5
```

### 3.3 线上评估（ai-cs-chat，前缀 `aics.eval.online`）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `aics.eval.online.enabled` | `false` | 线上评估总开关（默认关闭，开启后才采样评分） |
| `aics.eval.online.sample-rate` | `0.01` | 采样率，控制 Judge 调用成本 |
| `aics.eval.online.judge-model` | 空 | Judge 模型名（空=默认模型） |

### 3.4 CI 门禁（ai-cs-chat，前缀 `aics.eval.gate`）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `aics.eval.gate.p95-latency-ms` | 空（不校验） | P95 延迟上限，超限门禁不通过 |
| `aics.eval.gate.avg-tokens-per-request` | 空（不校验） | 单请求平均 Token 上限，超限门禁不通过 |

---

## 四、数据表（chat_db，脚本 `deploy/mysql/llm-observability-init.sql`，幂等可重复执行）

| 表 | 用途 | 关键字段 |
|---|---|---|
| `llm_trace` | 调用链追踪 | `request_id`(PK)、`user_id`、`session_id`(VARCHAR，兼容 sessionKey/会话ID)、`scenario`、`status`、`total_duration_ms`、`spans_json` |
| `model_usage` | Token/费用计量 | `request_id`、`user_id`、`scenario`、`provider`、`model`、`input_tokens`、`output_tokens`、`total_tokens`、`estimated_cost`、`estimated` |
| `model_usage_quota` | 配额配置 | `user_id`、`scenario`、`window_type`(DAILY/WEEKLY/MONTHLY)、`quota_tokens`、`quota_cost`（唯一键 `user_id+scenario`） |
| `online_eval_record` | 线上采样评估 | `request_id`、`question_digest`、`answer_digest`、`llm_score`、`judge_status`(SUCCESS/FAILED/SKIPPED) |
| `user_feedback` | 用户反馈闭环 | `request_id`、`feedback_type`(LIKE/DISLIKE)、`score`(1-5)、`comment` |

---

## 五、接口清单

### 5.1 ai-cs-chat（对外）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/observability/traces/{requestId}` | 查询一次请求的完整调用链（经 Feign 回读） |
| POST | `/api/chat/feedback` | 提交用户反馈（点赞/点踩/1-5 分/补充文本） |
| GET | `/api/chat/feedback?requestId=` | 查询用户反馈 |

### 5.2 ai-cs-message（服务间）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/observability/traces` | 上报调用链（幂等：requestId 已存在返回原值） |
| GET | `/api/observability/traces/{requestId}` | 查询调用链 |
| GET | `/api/observability/traces?userId=&scenario=&page=&size=` | 分页查询调用链 |
| POST | `/api/model-usage/records` | 上报单次用量 |
| GET | `/api/model-usage/stats?userId=&scenario=&model=&startTime=&endTime=` | 聚合统计 |
| GET/POST | `/api/model-usage/quota` | 配额查询/设置（upsert） |
| POST | `/api/eval/online-records` | 上报线上评估记录 |
| GET | `/api/eval/online-records/stats?startTime=&endTime=` | 评估+反馈统计 |
| POST | `/api/eval/feedback` | 上报用户反馈 |
| GET | `/api/eval/feedback?requestId=&startTime=&endTime=` | 查询用户反馈 |

---

## 六、OTLP 接入步骤（可选）

1. 部署 OTLP 兼容后端（如 Langfuse：`docker run -p 3000:3000 langfuse/langfuse`，或自建 collector）。
2. 在 ai-cs-chat 配置：

```yaml
aics:
  observability:
    otlp-endpoint: http://langfuse-host:4318/v1/traces
```

3. 重启 ai-cs-chat：`ObservationConfig` 检测到端点后创建 `OtlpHttpSpanExporter` + `Tracer`，`OtlpObservationHandler` 把 observation 同步导出为 OTel span。
4. 未配置端点时零开销：不创建 exporter，trace 走结构化日志导出（`log-export`）。

> 说明：trace 持久化（llm_trace 落库）与 OTLP 导出相互独立，即使未接入后端，`GET /api/observability/traces/{requestId}` 仍可查询。

---

## 七、CI 门禁用法

现有 RAG 评估门禁（`-Peval` profile）不变，新增两个可选阈值：

```yaml
aics:
  eval:
    gate:
      p95-latency-ms: 5000        # 可选：P95 延迟上限（毫秒）
      avg-tokens-per-request: 2000  # 可选：单请求平均 Token 上限
```

- 阈值**未配置**的维度只记录指标值，不参与判定（既有 CI 行为不变）；
- 任一配置维度超限 → `RagEvalReport.passed=false`，构建失败。

---

## 八、关键代码位置

| 能力 | 路径 |
|---|---|
| 调用链上下文与传播 | `ai-cs-chat/src/main/java/com/aics/chat/observability/TraceContext*.java` |
| 观测配置与 OTLP 导出 | `.../observability/ObservationConfig.java`、`OtlpObservationHandler.java` |
| 请求入口拦截器 | `.../observability/TraceInterceptor.java`、`config/ObservabilityWebConfig.java` |
| 调用链落库 | `.../observability/TraceRecorder.java`、`feign/TraceFeignClient.java` |
| Token/费用计量 | `.../observability/ModelUsageRecorder.java`、`ModelUsageProperties.java` |
| 配额管控 | `.../observability/QuotaService.java` |
| 线上采样评估 | `.../observability/OnlineEvalService.java`、`OnlineEvalSampler.java`、`OnlineEvalProperties.java` |
| 用户反馈接口 | `ai-cs-chat/.../controller/ChatFeedbackController.java` |
| 埋点接入点 | `service/impl/ResilientAiService.java`、`ChatServiceImpl.java`、`VisionModelClient.java`、`rag/rerank/SiliconFlowRerankService.java`、`rag/eval/LlmJudgeService.java`、`nl2sql/Nl2SqlQueryService.java`、`agent/*` |
| CI 门禁扩展 | `rag/eval/EvalGateConfig.java`、`RagEvalServiceImpl.java`、`RagEvalReport.java` |
| message 侧持久化 | `ai-cs-message/.../controller/LlmTraceController.java`、`ModelUsageController.java`、`ModelUsageQuotaController.java`、`OnlineEvalController.java` |

---

## 九、测试策略

| 层 | 覆盖 |
|---|---|
| 单元测试（chat） | TraceContext 传播/采样/MDC、Observation→span 组装、TraceRecorder 失败降级、费用计算、配额判定、采样边界、反馈参数校验、门禁三种行为 |
| 单元测试（message） | 五张表服务幂等/聚合/查询、controller 委托（58 个用例） |
| 集成测试 | RAG 对话产生 retrieval+answer 链路（同 requestId）；Agent 会话产生 intent+tools 链路 |
| 回归 | `mvn -pl ai-cs-chat,ai-cs-message verify`（含 JaCoCo 门禁） |
