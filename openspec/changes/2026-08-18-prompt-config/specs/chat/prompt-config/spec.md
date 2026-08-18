## Purpose

将散落在各服务代码中的 LLM 提示词（Prompt）统一外置为可配置资源，并为每个提示词建立**版本管理、灰度发布、热回滚与效果关联**能力。目标是让 Prompt 的迭代不再依赖代码发布，且每一次 Prompt 变更都能被追踪、可控灰度、可快速回滚，并能与线上/离线评估质量分关联，形成"改 Prompt → 灰度 → 看效果 → 全量或回滚"的闭环。

## ADDED Requirements

### Requirement: Prompt 外置配置化
系统 SHALL 将全部 LLM 提示词（system / user 模板）外置到 YAML 配置文件（如 `application-prompt.yml`），按 `scenario` 维度组织（如 `intent`、`rewrite`、`rag`、`judge`、`chart`、`vision`、`summary`、`default-system`），模板支持 `{{var}}` 占位符，运行时由统一的 `PromptRegistry` 加载并提供 `render(scenario, variables)` 渲染能力。

#### Scenario: 外置模板渲染
- **WHEN** 业务代码调用 `PromptRegistry.render("intent", Map.of("input", "..."))`
- **THEN** 系统返回用变量替换占位符后的完整提示词文本，等价于原硬编码输出

#### Scenario: 缺失 scenario 配置
- **WHEN** 请求一个未配置的 `scenario`
- **THEN** 系统记录告警并抛出明确的"未配置提示词"异常（或返回空，由调用方降级），不静默吞错

### Requirement: Prompt 版本管理
系统 SHALL 为每个 Prompt 维护版本（`version`，语义化或递增整数），同一 `scenario` 可同时存在多个版本，`PromptRegistry` 在加载时记录 `scenario + version + content + createdAt`，并提供按 `scenario` 列出所有版本及查询指定版本内容的能力。

#### Scenario: 同 scenario 多版本共存
- **WHEN** 配置中存在 `intent` 的 `v1` 与 `v2` 两个版本
- **THEN** 两者均被加载且可被独立查询，互不覆盖

#### Scenario: 版本元数据可查
- **WHEN** 调用方查询 `intent` 的可用版本
- **THEN** 返回版本号列表及各自的内容摘要与创建时间

### Requirement: Prompt 灰度发布
系统 SHALL 支持按策略将流量分配到 Prompt 的不同版本，灰度策略可在配置中声明，至少支持：**按比例权重**（如 `v1:0.9, v2:0.1`）、**按 userId 尾号分桶**、**按 scenario 全量指定版本**。每次渲染时由 `PromptRouter` 依据策略与请求上下文（`userId`/`scenario`）选定版本。

#### Scenario: 按比例灰度
- **WHEN** 配置 `intent` 灰度 `v1:0.9/v2:0.1` 且请求未带固定版本
- **THEN** 约 10% 请求使用 `v2`，其余使用 `v1`（统计上符合权重）

#### Scenario: 按 userId 固定分桶
- **WHEN** 配置按 `userId` 尾号命中 `v2` 且本次请求 `userId` 尾号命中
- **THEN** 该用户稳定命中 `v2`，跨请求一致（可复现）

### Requirement: Prompt 热回滚
系统 SHALL 支持通过配置 `activeVersion` 将某 `scenario` 的生效版本即时切换（热回滚），无需重新发布代码；切换后新请求立即使用目标版本，旧版本保留可供再次切回。

#### Scenario: 配置切换生效版本
- **WHEN** `intent.activeVersion` 从 `v2` 改回 `v1` 并重新加载配置
- **THEN** 后续 `intent` 请求统一使用 `v1`，无需重启应用

#### Scenario: 回滚后旧版本仍在
- **WHEN** 从 `v2` 回滚到 `v1`
- **THEN** `v2` 仍保留在 registry 中，可再次通过 `activeVersion` 切回

### Requirement: Prompt 效果关联
系统 SHALL 在每次 LLM 调用的观测数据（`TraceSpan`）中附加所用 Prompt 的 `scenario + version`，并 SHALL 将线上/离线评估（复用现有 `LlmJudgeService` / `RagEvalService`）结果与 `promptVersion` 关联聚合，支持按 `scenario × version` 查询质量分分布，为灰度决策与回滚提供依据。

#### Scenario: trace 携带 prompt 版本
- **WHEN** 一次 RAG 回答生成使用了 `rag` 的 `v2`
- **THEN** 该请求的 trace span 记录 `promptScenario=rag`、`promptVersion=v2`

#### Scenario: 按版本聚合评估分
- **WHEN** 查询 `rag` 在 `v1` 与 `v2` 下的平均 LLM-Judge 分数
- **THEN** 系统分别返回两个版本的质量分与样本量，可对比

## MODIFIED Requirements

<!-- 无：本 capability 为新增，不修改既有 spec 需求。 -->
