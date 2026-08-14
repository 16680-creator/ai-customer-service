## 1. 数据库与依赖准备

- [x] 1.1 新增 `deploy/mysql/llm-observability-init.sql`：5 张表（`llm_trace`、`model_usage`、`model_usage_quota`、`online_eval_record`、`user_feedback`，均入 `chat_db`，含查询索引，`IF NOT EXISTS` 幂等），并并入 `all-init.sql`
- [x] 1.2 `ai-cs-chat/pom.xml` 增加 `micrometer-tracing-bridge-otel`、`opentelemetry-exporter-otlp`（optional），以及 spring-boot-starter-actuator（如需 actuator 指标端点）

## 2. chat 侧观测核心（com.aics.chat.observability）

- [x] 2.1 新增 `TraceContext`（requestId/userId/sessionId/scenario/startTime/span 列表）与 `TraceContextHolder`（ThreadLocal + MDC 写入），提供跨异步边界的显式捕获/恢复方法
- [x] 2.2 新增 `ObservabilityProperties`（enabled、sample-rate、otlp.endpoint、log-export）并绑定 `aics.observability.*`
- [x] 2.3 新增 `ObservationConfig`：注册 `ObservationRegistry`、`ObservationHandler`（组装 span 到 TraceContext）、`@ConditionalOnProperty` 激活的 `OtlpHttpSpanExporter` Bean（未配置端点时不创建）
- [x] 2.4 新增 `TraceRecorder`：请求结束时把 spans JSON + 元数据经 `TraceFeignClient` 落 `llm_trace`，失败仅告警
- [x] 2.5 新增 `TraceFeignClient`（chat 侧）与 `GET /api/observability/traces/{requestId}` 查询接口（chat 侧 controller，经 Feign 回读 message 侧）
- [x] 2.6 单元测试：TraceContext 传播（含 supplyAsync 边界）、采样开关、导出降级、TraceRecorder 失败不阻断

## 3. message 侧 trace 持久化

- [x] 3.1 新增 `LlmTrace` 实体 + `LlmTraceMapper` + 建表脚本对应字段
- [x] 3.2 新增 `LlmTraceService`/`LlmTraceServiceImpl`（写入 + 按 requestId 查询 + 分页查询）与 `LlmTraceController`（`POST /api/observability/traces`、`GET /api/observability/traces/{requestId}`、`GET /api/observability/traces?userId=&scenario=&page=`），统一 `Result<T>`
- [x] 3.3 单元测试：写入幂等/查询缺失返回空、controller 委托正确性

## 4. 埋点接入现有调用链

- [x] 4.1 `ResilientAiService` 各调用方法（chat/rag/summary/sse/vision）接入：请求开始创建/获取 TraceContext，成功/失败记录 LLM span（provider/model/tokens/首 token/总耗时/重试/状态），MDC 关联 requestId
- [x] 4.2 `ChatServiceImpl`（普通对话/RAG 对话/流式）接入 retrieval 与 rerank 环节 span（query、召回数、文档 ID、耗时；rerank 模型、排序前后、耗时）
- [x] 4.3 Agent 编排链（`AfterSaleAgentService`/`IntentClassifierService`/`AgentToolRegistry` 工具执行）接入 intent span 与 tools span（工具名、参数摘要、结果状态、耗时）
- [x] 4.4 `VisionChatServiceImpl`、`Nl2SqlQueryService`、`LlmJudgeService` 接入 LLM span（场景 vision/nl2sql/eval）
- [x] 4.5 answer 环节 span：引用数、安全检测结果（`SafetyGuardService` 结果）、回答长度；请求结束统一由 `TraceRecorder` 落库
- [x] 4.6 集成测试：一次 RAG 对话产生完整链路（intent→retrieval→rerank→llm→answer 同 requestId）；一次 Agent 会话产生 intent+tools+llm 链路

## 5. 用量计量与配额（chat 采集 → message 落库）

- [x] 5.1 message 侧：`ModelUsage` 实体 + `ModelUsageMapper` + `ModelUsageService`/`ModelUsageServiceImpl`（写入、按 user/scenario/model/时间窗口聚合统计）+ `ModelUsageController`（`POST /api/model-usage/records`、`GET /api/model-usage/stats`）
- [x] 5.2 message 侧：`ModelUsageQuota` 实体 + `ModelUsageQuotaMapper` + 配额读写服务 + Controller（`GET/POST /api/model-usage/quota`）
- [x] 5.3 message 侧单元测试：统计聚合 SQL 正确性（Mock 场景）、配额查询/更新、controller 委托
- [x] 5.4 chat 侧：`ModelUsageProperties`（enabled、pricing `<model>.input/output`、default pricing、executor 配置）绑定 `aics.usage.*`
- [x] 5.5 chat 侧：`ModelUsageRecorder`（从 ChatResponse/流式末尾 usage 取 token，计算估算费用，异步经 `ModelUsageFeignClient` 落库，失败仅告警）+ `ModelUsageFeignClient`
- [x] 5.6 chat 侧：`QuotaService`（按 userId×scenario×时间窗口累计用量校验，超限返回 `QuotaCheckResult`，供调用方降级）
- [x] 5.7 单元测试：费用计算（含未配置单价走默认）、配额超限判定、记录失败不阻断、场景归属（agent/summary/eval）

## 6. 线上采样评估与反馈闭环

- [x] 6.1 message 侧：`OnlineEvalRecord` 实体 + Mapper + Service + Controller（`POST /api/eval/online-records`、`GET /api/eval/online-records/stats`）；`UserFeedback` 实体 + Mapper + Service + Controller（`POST /api/eval/feedback`、`GET /api/eval/feedback`）
- [x] 6.2 chat 侧：`OnlineEvalProperties`（enabled、sample-rate、judge model）绑定 `aics.eval.online.*`
- [x] 6.3 chat 侧：`OnlineEvalSampler`（采样判定）+ `OnlineEvalService`（采样请求回答完成后异步复用 `LlmJudgeService` 评分，经 Feign 落 `online_eval_record`，评分失败标记不重试）
- [x] 6.4 chat 侧：`POST /api/chat/feedback` 用户反馈接口（点赞/点踩/1-5 分/补充文本，关联 requestId/sessionId，经 Feign 落 `user_feedback`）
- [x] 6.5 单元测试：采样判定边界（rate=0/1/0.5）、评分失败不影响主链路、反馈参数校验
- [x] 6.6 集成测试：采样请求完成评分并落库；反馈闭环可查询

## 7. CI 门禁扩展

- [x] 7.1 新增 `EvalGateConfig`（p95LatencyMs、avgTokensPerRequest，可空）+ 评估执行时采集每用例耗时与 token
- [x] 7.2 `RagEvalReport` 增加 p95 延迟/平均 token 字段；门禁判定扩展：任一超限 passed=false，阈值未配置的维度只记录不判定
- [x] 7.3 单元测试：阈值超限/未配置/边界值三种门禁行为；既有门禁（hitRate/llmScore）行为不变

## 8. 文档与收尾

- [x] 8.1 更新 `docs/15-AI功能与技术缺口分析.md`：3.3 标记为已实现，更新 2.4/四 章节状态
- [x] 8.2 新增实现文档（docs/25）：配置项说明（aics.observability.* / aics.usage.* / aics.eval.online.*）、表结构、OTLP 接入步骤、查询接口示例
- [x] 8.3 全量回归：`mvn -pl ai-cs-chat,ai-cs-message verify`（含 JaCoCo 门禁）与 `mvn -pl ai-cs-chat,ai-cs-message -am install -DskipTests` 通过（全量 clean install 因本地运行中服务锁定 jar 而跳过，与本次变更无关）
