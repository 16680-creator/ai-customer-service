## Why

当前 `ai-cs-chat` 中所有 LLM 提示词均硬编码在 Java 代码里（如 `SpringAiConfig` 默认系统提示词、`IntentClassifierService.buildPrompt`、`QueryRewriteService` 改写 Prompt、`ChatServiceImpl` 摘要/RAG 提示词、`LlmJudgeService` / `ChartAnswerGenerator` / `VisionChatServiceImpl` 等），**且没有任何版本、灰度、回滚与效果关联能力**（见项目进度矩阵：Prompt 配置化 ⚠️ 部分实现，缺版本/灰度/回滚/效果关联）。

这带来三个问题：
1. **迭代慢、风险高**：改一句提示词就要改代码、走发版，且改坏了只能再次发版回滚。
2. **无灰度**：新提示词无法小流量验证，只能全量上线赌运气。
3. **无法归因**：已有 `LlmJudgeService` / `RagEvalService` 评估能力，但 Prompt 版本未进入 trace，无法判断"质量分波动是哪一版提示词导致的"。

目标：把 Prompt 变成**一等配置资源**，支持外置、版本、灰度、热回滚，并与现有观测/评估体系打通，形成 Prompt 迭代闭环。

## What Changes

- 在 `ai-cs-chat` 新增 Prompt 配置化模块（`com.aics.chat.prompt`）：`PromptProperties`（绑定 `aics.prompt.*`）、`PromptRegistry`（加载/渲染/版本管理）、`PromptRouter`（灰度策略与版本选定）。
- 将现有硬编码提示词外置到 `application-prompt.yml`，按 `scenario` 组织，保留 `{{var}}` 占位符语义，逐一对各调用点（intent / rewrite / rag / summary / judge / chart / vision / default-system）改造为从 `PromptRegistry` 取模板渲染。
- 配置支持版本与灰度：`scenario` 下可声明多 `versions` 与 `activeVersion`、灰度策略（`weights` / `userId-mod` / 全量指定）。
- 在现有 `TraceSpan`（已属 `chat/llm-observability`）中附加 `promptScenario` 与 `promptVersion` 字段；评估聚合按 `promptVersion` 维度统计。
- 提供 Prompt 版本查询/切换的管理接口（读取当前生效版本、列出版本、热切换），便于运维灰度与回滚。

## Capabilities

### New Capabilities
- `chat/prompt-config`: LLM 提示词外置配置化——YAML 按 scenario 组织、占位符渲染、`PromptRegistry`/`PromptRouter`、版本管理、灰度发布、热回滚、与观测/评估的效果关联。

### Modified Capabilities
- `chat/llm-observability`: 在 `TraceSpan` 观测数据中新增 `promptScenario` / `promptVersion` 字段（在现有 spec 上增补，关联 Prompt 版本与调用链）。

## Impact

- **ai-cs-chat**：新增 `com.aics.chat.prompt` 模块（`PromptProperties`、`PromptRegistry`、`PromptRouter`、`PromptRenderException`）；新增 `application-prompt.yml`；改造 `SpringAiConfig`、`IntentClassifierService`、`QueryRewriteService`、`ChatServiceImpl`、`LlmJudgeService`、`ChartAnswerGenerator`、`VisionChatServiceImpl`、`Nl2SqlQueryService` 等调用点为配置化渲染；`TraceContext`/`TraceSpan` 增加 `promptScenario`/`promptVersion`；新增 Prompt 管理 Controller（`GET /api/prompts`、`POST /api/prompts/{scenario}/active`）。
- **配置**：新增 `aics.prompt.*`（`enabled`、`source`、`scenarios.<scenario>.activeVersion`、`versions.<version>.system/user`、`rollout.<strategy>`）。
- **依赖**：无新增第三方依赖（复用 Spring Boot ConfigurationProperties + 已有 Micrometer/Observation）。
- **不改动既有 spec 的语义**：仅向 `TraceSpan` 增补两个字段，可观测性 spec 其余行为不变。
- **系统**：无新表（版本与灰度信息来自 YAML 配置，非持久化）；如需审计可后续扩展落库（本期不纳入）。
