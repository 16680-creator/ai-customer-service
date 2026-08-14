## Purpose

对平台所有 LLM 调用进行统一的 Token 与费用计量：按用户、场景、模型、时间窗口聚合 Token 消耗与估算费用并落库持久化，提供统计查询与配额管控接口，为成本治理与预算控制提供数据基础。

## Requirements

### Requirement: 模型用量记录
系统 SHALL 在每次 LLM 调用完成后记录一条模型用量记录，包含 `requestId`、`userId`、`sessionId`、场景（chat/rag/agent/summary/vision/eval 等）、provider、model、输入/输出 Token 数与估算费用，并 MUST 持久化到 `model_usage` 表。

#### Scenario: 记录单次调用用量
- **WHEN** 一次 LLM 调用成功返回（含流式调用结束）
- **THEN** 系统生成一条包含 requestId、userId、场景、模型、输入/输出 Token 与估算费用的用量记录并落库

#### Scenario: 用量记录失败不阻断
- **WHEN** 用量记录写入失败（网络异常、服务不可用）
- **THEN** 系统仅记录告警日志，业务回复正常返回

#### Scenario: 场景归属
- **WHEN** 一次 LLM 调用属于 Agent 编排、摘要压缩或评估打分等非直接对话场景
- **THEN** 用量记录以对应场景标识（agent/summary/eval）记录，可区分统计

### Requirement: 用量统计查询
系统 SHALL 提供用量统计接口，支持按用户、场景、模型、时间窗口（日/周/月）聚合查询 Token 总量、调用次数与估算费用合计。

#### Scenario: 按维度聚合查询
- **WHEN** 调用方按用户或场景或模型或时间窗口查询用量统计
- **THEN** 系统返回对应维度的调用次数、输入/输出 Token 合计与估算费用合计

#### Scenario: 空数据查询
- **WHEN** 查询条件在 `model_usage` 中无匹配记录
- **THEN** 系统返回零值统计结果，不报错

### Requirement: 配额与预算管控
系统 SHALL 支持为「用户 × 场景 × 时间窗口」配置 Token 或费用配额，并在用量记录落库后检查配额；超过配额时 SHALL 返回可被调用方识别的超限结果，供上层决定降级或拒绝。

#### Scenario: 未超配额
- **WHEN** 当前用户在当前时间窗口内的累计用量未超过配置配额
- **THEN** 用量正常记录，不产生任何限制

#### Scenario: 超过配额
- **WHEN** 当前用户在当前时间窗口内的累计用量超过配置配额
- **THEN** 系统返回超限结果（含配额与当前用量），调用方可据此降级或拒绝后续请求

#### Scenario: 配额配置查询与更新
- **WHEN** 管理员查询或更新某用户某场景的配额配置
- **THEN** 系统返回当前配额配置，或将新配额持久化并生效于后续检查

### Requirement: 费用估算可配置
系统 SHALL 支持通过配置维护各模型单价（每百万 Token 输入/输出价格），费用估算 MUST 基于该单价计算；未配置单价的模型 SHALL 以默认值估算。

#### Scenario: 已配置单价
- **WHEN** `model_usage` 计量时命中配置的模型单价
- **THEN** 估算费用 = 输入 Token × 输入单价 + 输出 Token × 输出单价

#### Scenario: 未配置单价
- **WHEN** 模型单价未在配置中维护
- **THEN** 系统使用默认单价估算，并在记录中标记为估算值
