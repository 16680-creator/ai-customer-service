## Purpose

为线上 LLM 对话提供全链路可观测能力：每次请求生成 `requestId`，贯穿意图识别、检索、重排、模型调用、工具调用与回答生成各环节，记录耗时、Token、重试与状态，可按 `requestId` 还原一次请求的完整调用链，并支持导出到 OTLP 兼容追踪后端。

## ADDED Requirements

### Requirement: 请求级 trace 标识
系统 SHALL 为每次对话请求生成全局唯一 `requestId`，并 MUST 使其贯穿该请求的意图识别、检索、重排、LLM 调用、工具调用与回答生成全链路。

#### Scenario: 新请求生成 requestId
- **WHEN** 用户发起一次对话请求（普通对话、RAG 对话、流式对话或 Agent 对话）
- **THEN** 系统生成全局唯一 `requestId`，并关联 `userId` 与 `sessionId`

#### Scenario: requestId 贯穿各环节
- **WHEN** 请求经过 intent / retrieval / rerank / llm / tools / answer 任一环节
- **THEN** 该环节产生的观测数据均携带同一 `requestId`，且按调用顺序关联

### Requirement: 各环节观测数据采集
系统 SHALL 在以下环节采集观测数据：意图识别（类型、置信度）、检索（query、召回数、文档 ID、耗时）、重排（模型、排序前后、耗时）、LLM 调用（provider、model、token 数、首 Token 延迟、总耗时、重试次数）、工具调用（工具名、参数摘要、结果状态、耗时）、回答（引用数、安全检测结果、用户反馈）。

#### Scenario: LLM 调用观测
- **WHEN** 一次 LLM 调用成功返回
- **THEN** 记录 provider、model、prompt/输出 token、首 Token 延迟、总耗时与重试次数

#### Scenario: 调用失败观测
- **WHEN** 一次 LLM 调用失败或触发重试/熔断降级
- **THEN** 观测数据记录失败状态、错误摘要与重试次数，且不阻断主链路

#### Scenario: 检索与重排观测
- **WHEN** RAG 请求执行检索或重排
- **THEN** 记录检索 query、召回文档数与耗时；重排记录使用的模型、排序前后 top 文档与耗时

### Requirement: 观测数据导出
系统 SHALL 支持将 trace 数据导出到 OTLP 兼容的追踪后端（如 Langfuse / Phoenix）；未配置后端或导出失败时，SHALL 降级为结构化日志输出，且采集/导出不得影响主业务链路。

#### Scenario: 配置 OTLP 后端
- **WHEN** 系统配置了 `aics.observability.otlp.endpoint`
- **THEN** trace span 通过 OTLP 导出到该端点

#### Scenario: 未配置后端或导出失败
- **WHEN** 未配置 OTLP 端点，或导出过程抛异常
- **THEN** 观测数据以结构化日志输出，业务请求正常完成，仅记录告警

### Requirement: Trace 查询接口
系统 SHALL 提供按 `requestId` 查询一次请求完整 trace 详情的接口，返回各环节的观测数据与总耗时。

#### Scenario: 查询存在的 trace
- **WHEN** 调用方以存在的 `requestId` 查询 trace
- **THEN** 系统返回该请求各环节按顺序排列的观测明细（环节、模型、耗时、Token、状态）

#### Scenario: 查询不存在的 trace
- **WHEN** 调用方以不存在的 `requestId` 查询 trace
- **THEN** 系统返回空结果或明确的未找到提示，不报错

### Requirement: 观测配置开关
系统 SHALL 支持通过配置开启/关闭观测采集、设置采样率与导出目标，默认采集开启但导出为日志。

#### Scenario: 关闭采集
- **WHEN** 配置 `aics.observability.enabled=false`
- **THEN** 系统不采集与导出任何 trace 数据，业务功能不受影响

#### Scenario: 采样率控制
- **WHEN** 配置了采样率（0~1）
- **THEN** 系统按采样率抽取请求采集 trace，未命中的请求仅保留最小日志
