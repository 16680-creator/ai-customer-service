## Context

现有基础（见 proposal.md - Why）：

- `ai-cs-chat` 已有 `ResilientAiService`（统一 LLM 调用入口）、`HybridRetriever`/`SiliconFlowRerankService`（检索/重排）、`com.aics.chat.agent`（Agent 编排）、`rag/eval`（离线评估：Recall@k/MRR/HitRate + LLM-as-Judge）。
- `ai-cs-message` 已有 Agent 轨迹持久化模式：chat 侧 `AgentTraceRecorder` 摘要化后经 Feign（`AgentTraceFeignClient` → `AgentTraceController`）落库 `chat_db`，失败只告警不阻断。
- 项目规范：`specs/README.md` 之外采用统一 `Result<T>`、`chat_db` 库、幂等 SQL 初始化脚本、TDD（Red→Green→Refactor）、JaCoCo 门禁。
- Spring Boot 3.2 / Spring AI 1.0 / JDK 17；无 actuator 依赖；`CompletableFuture.supplyAsync` 与 SSE `Flux` 是异步边界。

## Goals / Non-Goals

**Goals:**
- 建立一套**默认零外部依赖、可平滑升级**的观测方案：核心用 Micrometer Observation，导出端可插拔（默认结构化日志，配置 OTLP 端点后走 OpenTelemetry）。
- 用量计量与配额走「chat 侧采集 → Feign → message 侧落库」的既有模式，与 Agent 轨迹审计同构，复用团队已熟悉的套路。
- 线上采样评估复用现有 `LlmJudgeService`，反馈闭环落库可查询。
- 所有新能力默认可开关，采集/落库失败不影响主链路。

**Non-Goals:**
- 不引入完整 APM 平台部署（Langfuse/Phoenix 仅作为可选的 OTLP 目标，不内嵌）。
- 不做多租户配额管理（配额按 userId 维度，租户模型属 P1 范围）。
- 不改造 RocketMQ 链路承载 trace（trace 走 Feign 直写，量级可控）。
- 不做基于 trace 的自动降级路由（属 3.4 P1 多模型路由范围）。

## Decisions

### D1: 观测 API —— Micrometer Observation + 可插拔导出器
**选择**：引入 `micrometer-tracing-bridge-otel` 与 `opentelemetry-exporter-otlp`（均仅在配置了 OTLP 端点时激活导出 Bean），并用 `ObservationRegistry` + 自定义 `ObservationHandler` 组装 span；同时提供 `TraceContext`（requestId/userId/sessionId/场景 + span 列表）作为统一载体。
**理由**：Micrometer Observation 是 Spring Boot 3.x 官方观测抽象，Spring AI 1.0 内部已基于 Observation 埋点（`ChatModel` 调用自带 span），可无缝衔接；OTLP 是 Langfuse/Phoenix 共同支持的协议，换后端零代码改动。
**备选**：直接依赖 Langfuse SDK —— 绑定单一厂商，弃；纯自研 ThreadLocal 日志 —— 无法接入标准追踪后端，弃。

### D2: trace 持久化与查询 —— message 侧 `llm_trace` 表（JSON spans）
**选择**：chat 侧 `TraceRecorder` 在请求结束时把整条调用链（spans JSON + 元数据）经 Feign 写入 `llm_trace` 表；查询接口 `GET /api/observability/traces/{requestId}` 由 chat 侧经 Feign 回读。
**理由**：与 `agent_run`/`agent_step` 模式一致，跨实例可查、可审计回放；单表存 JSON 足够支撑"按 requestId 还原调用链"，避免过度建模。
**备选**：Redis 缓存 trace —— 有 TTL 且不可审计，弃；只写日志 —— 无法提供查询接口，弃。

### D3: 用量计量 —— `model_usage` 单行记录 + 聚合统计 SQL
**选择**：每次 LLM 调用（含流式结束时）由 `ModelUsageRecorder` 生成一行 `model_usage`（requestId、userId、sessionId、scenario、provider、model、input/output tokens、估算费用、状态）；统计接口用聚合 SQL（GROUP BY 用户/场景/模型 + 时间窗口）。
**理由**：明细行便于审计与回放；聚合查询简单且可用索引覆盖。费用估算用配置化单价表 `aics.usage.pricing.<model>.{input,output}`（每百万 token 价格），未配置走默认单价并标记 `estimated=true`。
**备选**：Redis 计数器聚合 —— 无法审计明细，弃；message 侧定时任务从 trace 反推 —— 双写一致性问题，弃。

### D4: Token 来源 —— Spring AI `ChatResponse` 元数据
**选择**：非流式调用从 `ChatResponse.getMetadata().getUsage()` 取 token；流式调用在流结束时从最终 chunk 的 usage 元数据聚合（Spring AI 流式响应在最后一个 chunk 携带 token usage；取不到时记为 0 并标记）。`ResilientAiService` 是统一采集点，所有调用方自动获得计量。
**理由**：避免在每个调用方重复解析；Spring AI 1.0 的 OpenAiChatModel 已返回 usage 元数据。
**风险**：流式 usage 字段依赖模型供应商返回 —— 见 Risks。

### D5: requestId 传播 —— 显式上下文 + MDC
**选择**：`TraceContextHolder`（ThreadLocal）承载当前请求上下文；进入异步边界（`supplyAsync` 前、SSE `Flux` 通过 Reactor Context）时显式捕获并恢复；同时写入 MDC（`requestId`）保证日志可关联。
**理由**：Spring AI/Resilience4j 的回调链不会自动传播 ThreadLocal，显式传播最可控；MDC 让既有日志零成本带上 requestId。
**备选**：Micrometer 自动上下文传播 —— 依赖 `TaskDecorator`/Reactor 钩子全局配置，侵入面大且难测试，弃。

### D6: 线上采样评估 —— 请求后异步评分 + 反馈闭环
**选择**：`OnlineEvalSampler` 按 `aics.eval.online.sample-rate` 决定是否采样；采样请求在回答完成后异步执行 `LlmJudgeService.score()`，结果落 `online_eval_record`（含 question/answer 摘要、评分、状态）。用户反馈走 `POST /api/chat/feedback` → 落 `user_feedback` 表。两者均经 Feign 到 message 侧。
**理由**：完全复用现有 Judge 服务与 Feign 落库模式；异步执行不增加用户请求延迟。
**备选**：同步评分 —— 增加 P95 延迟，违背观测本身目标，弃。

### D7: CI 门禁扩展 —— 阈值配置化、不配置即跳过
**选择**：`EvalGateConfig`（p95LatencyMs、avgTokensPerRequest）并入 `RagEvalReport` 门禁判定；评估执行时同时采集每用例耗时与 token（来自 usage 记录），报告输出实测 P95/平均 token 与阈值对比；阈值未配置的维度只记录不判定。
**理由**：与现有 `passed` 门禁语义一致，向后兼容（现有 CI 不配阈值行为不变）。

### D8: 表结构与脚本 —— 新增 5 张表，幂等 SQL
**选择**：新增 `llm_trace`、`model_usage`、`model_usage_quota`、`online_eval_record`、`user_feedback`（均入 `chat_db`，索引覆盖查询维度），脚本 `deploy/mysql/llm-observability-init.sql` 全部 `IF NOT EXISTS` 幂等，并入 `all-init.sql`。
**理由**：与 `after-sales-agent-init.sql` 模式一致。

## Risks / Trade-offs

- **流式 token usage 可能缺失** → 取不到时记 0 并标记 `usage_estimated=true`；统计接口可按标记过滤。
- **ThreadLocal 传播遗漏导致 requestId 断裂** → 统一在 `ResilientAiService`/`TraceRecorder` 两个收口点传播；集成测试断言 spans 同 requestId。
- **OTLP 依赖引入体积/版本冲突** → 依赖 optional 化、导出 Bean 条件装配（`@ConditionalOnProperty`）；不配置端点时 classpath 中即使存在也不初始化 exporter。
- **高频采样放大成本**（Judge 调用本身花钱）→ 采样率默认 0.01；评分失败不计费重试。
- **Feign 落库失败** → 沿用"失败仅告警"模式，异步线程池隔离（`aics.usage.executor` 可配），不阻塞主线程。
- **`model_usage` 表增长** → 建索引（user_id+create_time、model+create_time）；保留策略后续用定时清理任务，本期仅文档说明。

## Migration Plan

1. 执行 `deploy/mysql/llm-observability-init.sql`（幂等，可重复执行）。
2. message 侧新增实体/Mapper/Service/Controller 并发布；chat 侧新增 Feign 客户端、观测与计量组件。
3. 配置默认值全部内置（`aics.observability.enabled=true` 但导出仅日志、`aics.usage.enabled=true`、`aics.eval.online.enabled=false`），滚动发布无需基础设施变更。
4. 回滚：配置关闭对应开关即可；表可保留（幂等脚本不破坏既有数据），如需彻底回滚执行 drop 语句。

## Open Questions

- 具体 OTLP 后端选型（Langfuse / Phoenix / 自建 collector）是部署决策，配置 `aics.observability.otlp.endpoint` 即可切换，不影响本设计与任务拆分，留待部署时确定。
