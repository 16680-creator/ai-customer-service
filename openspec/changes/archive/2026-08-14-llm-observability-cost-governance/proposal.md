## Why

当前项目已有 RAG 离线评估（Recall@k / MRR / HitRate / LLM-as-Judge）与售后 Agent 轨迹审计（run/step 落库），但**线上一次请求到底发生了什么仍不可见**：模型、工具、检索、重排、费用链路无法按 `requestId` 还原，Token 消耗与估算费用没有统一计量，CI 门禁只有正确率没有延迟与成本维度（见 docs/15 第 3.3 节与"四、仍存在的技术缺口"：LLM 全链路可观测、Token 成本配额预算均标记为缺失 P0）。上线客服系统的前提是先能定位质量、延迟和费用问题。

## What Changes

- 在 `ai-cs-chat` 建立统一 LLM 观测层（Micrometer Observation + 可插拔 OTLP 导出），为每次对话请求生成 `requestId`，按调用链记录 intent / retrieval / rerank / llm / tools / answer 各环节的模型、Token、耗时、重试与状态。
- 新增 `model_usage` 计量链路：chat 侧按用户、场景（chat/rag/agent/summary/vision/eval…）、模型聚合 Token 与估算费用，经 Feign 落库到 `ai-cs-message`，并提供按用户/场景/模型/时间窗口的统计与配额查询接口。
- 将离线 RAG 评估扩展为线上采样评估：按采样率抽取线上请求，复用 LLM-as-Judge 评分并回写评估结果；新增用户反馈接口（点赞/点踩/评分）形成反馈闭环。
- 扩展评估门禁：在现有正确率门禁基础上，增加 P95 延迟与单请求平均 Token 上限；`model_usage` 统计可作为 CI 报告输入。
- 可观测数据采集与导出均需可配置开关（默认日志输出，OTLP 后端可选接入），且不得影响主链路（采集失败只告警不阻断）。

## Capabilities

### New Capabilities
- `chat/llm-observability`: 线上 LLM 调用全链路可观测——`requestId` 贯穿的 span 模型（intent/retrieval/rerank/llm/tools/answer）、Micrometer Observation 埋点、OTLP/日志导出与按 `requestId` 查询。
- `chat/model-usage`: Token 与估算费用的统一计量与治理——按用户/场景/模型聚合的 `model_usage` 记录、统计查询接口与配额/预算管控。
- `chat/online-evaluation`: 线上采样评估与用户反馈闭环——按采样率抽取线上请求并 LLM-as-Judge 评分、用户反馈接口、CI 门禁增加 P95 延迟与 Token 上限。

### Modified Capabilities
<!-- 暂无：openspec/specs/ 下仅存在 chat/history-management，本次不修改其需求。 -->

## Impact

- **ai-cs-chat**：新增观测模块（`com.aics.chat.observability`：TraceContext、Observation 配置、LLM/Tool 观测组件、OTLP 导出器）；改造 `ChatServiceImpl`、`ResilientAiService`、`VisionChatServiceImpl`、Agent 编排链路（`AfterSaleAgentService`/`IntentClassifierService`/工具执行）接入观测与用量采集；新增模型用量 Feign 客户端、线上采样评估服务与用户反馈接口；`SpringAiConfig`/`AgentProperties` 增加观测与采样配置。
- **ai-cs-message**：新增 `model_usage` 表与对应实体/Mapper/Service/Controller（参考 `AgentTraceController` 模式）；新增用量统计与配额查询接口。
- **API**：chat 侧新增 `GET /api/observability/traces/{requestId}`、`POST /api/chat/feedback`（用户反馈）；message 侧新增 `POST /api/model-usage/records`、`GET /api/model-usage/stats`、`GET/POST /api/model-usage/quota`。
- **依赖**：ai-cs-chat 新增 `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`（可选）与 `opentelemetry-api`；message 侧无新增依赖（MyBatis-Plus 已有）。
- **系统**：MySQL 新增 `model_usage`、`llm_eval_feedback` 表（deploy/mysql 初始化脚本）；配置项 `aics.observability.*`、`aics.usage.*`、`aics.eval.online.*`。
