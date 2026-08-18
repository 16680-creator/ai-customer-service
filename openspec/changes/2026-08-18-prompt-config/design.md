# 设计文档：Prompt 配置化

## 1. 目标与边界

将 `ai-cs-chat` 中散落的 LLM 提示词外置为 YAML 配置，建立 **配置化 + 版本 + 灰度 + 回滚 + 效果关联** 闭环。

**本期边界**：
- 版本/灰度/回滚信息以 YAML 配置为单一事实来源（不持久化到 DB，重启即恢复配置态）。
- 灰度策略支持三种：按比例权重、按 userId 尾号分桶、scenario 全量指定。
- 效果关联复用现有 `LlmJudgeService` / `RagEvalService` 与 `TraceSpan`，仅增补 `promptScenario`/`promptVersion` 字段，不做新的评估算法。

## 2. 配置模型（application-prompt.yml）

```yaml
aics:
  prompt:
    enabled: true                 # 关闭时回退原硬编码（过渡期双写用）
    scenarios:
      intent:
        activeVersion: v1
        rollout:
          strategy: weights        # weights | userId-mod | pinned
          weights: { v1: 0.9, v2: 0.1 }
          # userId-mod: { version: v2, mod: 10, remainder: 3 }  # userId%10==3 命中 v2
          # pinned: v1
        versions:
          v1:
            system: "你是智能客服的意图识别器……"
            user: "用户输入：{{input}}"
          v2:
            system: "你是智能客服的意图识别器（增强版）……"
            user: "用户输入：{{input}}"
      rewrite:
        activeVersion: v1
        rollout:
          strategy: pinned
          pinned: v1
        versions:
          v1:
            system: "你是检索查询优化专家，只输出指定 JSON。"
            user: "请把下面的用户问题改写成 {{count}} 个子查询……\n用户问题：{{question}}"
      rag:
        activeVersion: v1
        rollout: { strategy: pinned, pinned: v1 }
        versions:
          v1:
            system: "你是AI客服平台的智能助手……"
            user: "参考知识：{{context}}\n用户问题：{{message}}"
      summary:
        activeVersion: v1
        rollout: { strategy: pinned, pinned: v1 }
        versions:
          v1:
            user: "请将以下对话历史压缩为简洁的摘要……\n{{history}}"
      judge:
        activeVersion: v1
        rollout: { strategy: pinned, pinned: v1 }
        versions:
          v1:
            system: "你是严谨的 RAG 质量评估员，只输出 1-5 的整数分数。"
            user: "问题：{{question}}\n回答：{{answer}}\n参考答案：{{reference}}"
      chart:
        activeVersion: v1
        rollout: { strategy: pinned, pinned: v1 }
        versions:
          v1:
            system: "你是数据分析师，只输出结论。"
            user: "问题：{{question}}\n数据：{{rows}}"
      vision:
        activeVersion: v1
        rollout: { strategy: pinned, pinned: v1 }
        versions:
          v1:
            user: "{{instruction}}"
      default-system:
        activeVersion: v1
        rollout: { strategy: pinned, pinned: v1 }
        versions:
          v1:
            system: "你是AI客服平台的智能助手，代表平台为用户提供专业、友好的服务……"
```

## 3. 核心类设计（com.aics.chat.prompt）

### 3.1 PromptProperties
`@ConfigurationProperties("aics.prompt")`，映射上述 YAML 结构：
- `enabled`
- `Map<String, ScenarioConfig> scenarios`
- `ScenarioConfig`：`activeVersion`、`RolloutConfig rollout`、`Map<String, VersionConfig> versions`
- `RolloutConfig`：`strategy`（枚举）、`Map<String,Double> weights`、`PinnedConfig pinned`、`UserIdModConfig userIdMod`
- `VersionConfig`：`String system`、`String user`

### 3.2 PromptRegistry
- 启动时校验：每个 `scenario` 的 `activeVersion` 必须存在于 `versions`；`weights` 之和约为 1。
- `render(String scenario, Map<String,Object> vars)`：经 `PromptRouter` 选定版本 → 用 `StrSubstitutor`（或简单 `{{key}}` 正则替换）渲染 system/user → 返回 `RenderedPrompt(system, user)`。
- `listVersions(scenario)` / `getVersion(scenario, version)` / `getActiveVersion(scenario)`：版本查询。
- `setActiveVersion(scenario, version)`：热回滚（修改内存态 `activeVersion`，供管理接口调用）。
- `resolveVersion(scenario, userId)`：委托 `PromptRouter`。

### 3.3 PromptRouter
依据 `RolloutConfig` 策略选版本：
- `weights`：用 `ThreadLocalRandom` 按权重抽样（可注入 `Supplier<Double>` 便于测试）。
- `userId-mod`：`Long.parseLong(userId) % mod == remainder` 命中目标版本，否则 `activeVersion`。
- `pinned`：固定返回 `pinned` 版本。
- 命中版本不存在时回退 `activeVersion`。

### 3.4 渲染占位符
- 采用 `{{var}}` 语法；未提供变量时报 `PromptRenderException`（缺参），不静默留空。
- system 为空时返回 null（兼容仅 user 的模板，如 `intent`）。

## 4. 调用点改造清单

| 调用点 | 原硬编码位置 | scenario | 改造方式 |
|---|---|---|---|
| 默认系统提示词 | `SpringAiConfig.java:255` | `default-system` | `defaultSystem(...)` 改为 `promptRegistry.render("default-system", Map.of()).system()` |
| 意图识别 | `IntentClassifierService.java:121` | `intent` | `buildPrompt(input)` → `render("intent", Map.of("input", input)).user()` |
| 查询改写 | `QueryRewriteService.java:113/124` | `rewrite` | system+user 改为 `render("rewrite", Map.of("count",3,"question",q))` |
| RAG 对话 | `ChatServiceImpl.java:384/497` | `rag` | `ragPrompt` → `render("rag", Map.of("context",..,"message",..))` |
| 摘要 | `ChatServiceImpl.java:252` | `summary` | → `render("summary", Map.of("history",..))` |
| 评估打分 | `LlmJudgeService.java:65/77` | `judge` | → `render("judge", Map.of(...))` |
| 图表结论 | `ChartAnswerGenerator.java:104/114` | `chart` | → `render("chart", Map.of(...))` |
| 视觉 | `VisionChatServiceImpl` | `vision` | → `render("vision", Map.of("instruction",..))` |

## 5. 效果关联（复用 observability）

- `TraceSpan` 新增字段 `promptScenario`、`promptVersion`（在 `chat/llm-observability` spec 增补）。
- `ResilientAiService` / 各调用点在发起 LLM 调用前，将本次 `render` 得到的 `scenario+version` 写入 `TraceContext`，由现有 Observation 埋点带入 span。
- 现有 `RagEvalService` / `LlmJudgeService` 评分结果落库时携带 `promptVersion`（经 `TraceContext` 透传），聚合查询按 `scenario × version` 分组。
- 管理接口 `GET /api/prompts/stats?scenario=rag` 返回各版本平均质量分（对接 `online_eval_record` / `rag_eval`）。

## 6. 管理接口

- `GET /api/prompts`：列出全部 scenario 的 activeVersion 与版本数。
- `GET /api/prompts/{scenario}`：列出该 scenario 所有版本及内容摘要。
- `POST /api/prompts/{scenario}/active?version=v1`：热切换生效版本（回滚/灰度收敛）。

## 7. 过渡策略（降低风险）

- `aics.prompt.enabled=false` 时，`PromptRegistry.render` 回退到各服务内保留的"原硬编码模板副本"（仅过渡期，最终移除）。
- 逐 scenario 灰度迁移：先 `intent`/`rewrite` 等低风险场景，再 `rag`/默认系统。
- 单元测试覆盖：占位符渲染、缺参异常、灰度权重抽样、userId 分桶稳定性、热回滚切换、版本缺失告警。
