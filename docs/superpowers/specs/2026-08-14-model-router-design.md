# 多模型路由与模型降级设计

- 日期：2026-08-14
- 状态：已批准
- 影响模块：`ai-cs-chat`
- 关联缺口：[docs/15-AI功能与技术缺口分析.md](../../15-AI功能与技术缺口分析.md) 3.4 P1 多模型路由与模型降级

## 1. 背景与目标

当前 `ai-cs-chat` 只接入一个 DeepSeek `OpenAiChatModel`，所有 LLM 调用共用同一模型和同一个 Resilience4j `chatService` 熔断器。主模型故障时只能返回固定兜底文案，无法切换同能力模型；意图识别、摘要等低成本任务也使用同一模型，成本、能力和可用性都受限。

本设计实现：

- Nacos 驱动的多模型注册表，支持多个 OpenAI 兼容 provider/model。
- 确定性场景路由：按场景、能力、健康状态、成本和优先级选择模型。
- 同能力故障降级：主模型超时、429、5xx、熔断时切换到备选模型。
- 配额超限降档：用户 Token/费用配额超限时优先使用更便宜的模型。
- 可观测与成本联动：trace 和 `model_usage` 记录实际 provider/model、路由原因和降级链路。

## 2. 关键决策

1. 服务内 `ModelRouter`，不引入外部 LLM 网关；复用 Spring AI、Resilience4j、`TraceSpanObservationHandler` 和 `ModelUsageRecorder`。
2. 第一版供应商：DeepSeek 官方 API + 硅基流动 SiliconFlow Qwen。
3. 模型注册表和场景路由配置放在 Nacos `aics.model-router`，支持热更新。
4. 场景路由使用确定性规则，不使用 LLM 判断问题复杂度。
5. 每个模型有独立 CircuitBreaker，主模型故障不会连带熔断备选模型。
6. 流式调用在订阅前完成路由；流开始后不重放、不切换。
7. 配额超限时降档；若没有满足场景能力且更便宜的模型，保持原路由并记录原因。
8. 存量直接使用 `ChatClient` 的 `QueryRewriteService`、`ChartAnswerGenerator`、`LlmJudgeService` 迁移到路由入口，保留各自现有本地降级。

## 3. 配置模型

Nacos `ai-cs-chat.yml` 新增 `aics.model-router`：

```yaml
aics:
  model-router:
    enabled: true
    models:
      - id: deepseek-chat
        provider: deepseek
        base-url: https://api.deepseek.com
        api-key: ${DEEPSEEK_API_KEY}
        model: deepseek-chat
        enabled: true
        priority: 100
        tier: standard
        capabilities: [tool-calling]
        context-window: 65536
        timeout-ms: 30000
      - id: siliconflow-qwen3-32b
        provider: siliconflow
        base-url: https://api.siliconflow.cn
        api-key: ${SILICONFLOW_API_KEY}
        model: Qwen/Qwen3-32B
        enabled: true
        priority: 90
        tier: standard
        capabilities: [tool-calling]
        context-window: 32768
        timeout-ms: 30000
      - id: siliconflow-qwen3-8b
        provider: siliconflow
        base-url: https://api.siliconflow.cn
        api-key: ${SILICONFLOW_API_KEY}
        model: Qwen/Qwen3-8B
        enabled: true
        priority: 80
        tier: cheap
        capabilities: []
        context-window: 32768
        timeout-ms: 15000
    scenarios:
      chat:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      rag:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      agent:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      nl2sql:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      judge:
        primary: deepseek-chat
        fallbacks: [siliconflow-qwen3-32b]
      summary:
        primary: siliconflow-qwen3-8b
        fallbacks: [siliconflow-qwen3-32b]
      intent:
        primary: siliconflow-qwen3-8b
        fallbacks: [siliconflow-qwen3-32b]
      rewrite:
        primary: siliconflow-qwen3-8b
        fallbacks: [siliconflow-qwen3-32b]
      chart:
        primary: siliconflow-qwen3-8b
        fallbacks: [siliconflow-qwen3-32b]
    quota:
      enabled: true
      over-limit-fallback-tier: cheap
```

默认档位：

- `cheap`：`siliconflow-qwen3-8b`，用于 summary、intent、rewrite、chart。
- `standard`：`deepseek-chat`，用于 chat、rag、agent、nl2sql、judge。
- 标准场景备选：`siliconflow-qwen3-32b`，与主模型同能力。
- 所有模型名和场景映射均可通过 Nacos 调整；实际模型 ID 在实现阶段用供应商 API 验证。

## 4. 架构与组件

新增包 `com.aics.chat.modelrouter`：

### 4.1 ModelRouterProperties

- `@ConfigurationProperties(prefix = "aics.model-router")`。
- 字段：`enabled`、`List<ModelDefinition>`、`Map<ModelScenario, ScenarioRoute>`、`QuotaRouteProperties`。
- 启动和 Nacos 刷新时执行校验：模型 ID 唯一、primary/fallback 必须存在且 enabled、场景不能为空。
- 校验失败时保留旧配置并记录错误，不影响运行中的路由。

### 4.2 ChatModelRegistry

- 根据 `ModelDefinition` 构造独立的 `OpenAiApi`、`OpenAiChatModel` 和 `ChatClient`。
- 以 `modelId -> ModelClientHolder` 保存，`ModelClientHolder` 包含模型元数据和客户端。
- 支持 Nacos 刷新后重建变更模型；模型 ID 未变且配置未变时复用现有实例。

### 4.3 ModelHealthRegistry

- 使用编程式 `CircuitBreakerRegistry` 和 `RetryRegistry`，按 modelId 命名。
- 每个模型独立熔断配置：10 次滑动窗口、50% 失败率、30 秒 OPEN、3 次 HALF_OPEN 探测。
- 提供 `isAvailable(modelId)` 和 `recordSuccess/recordFailure` 给 Router 和调用层使用。

### 4.4 ModelRouter

- 输入：`ModelScenario`、`RouteRequest`（场景、必需能力、配额超限标志）。
- 输出：`RouteDecision`，包含 `selectedModelId`、`fallbackChain`、`reason`（枚举 `SCENARIO_DEFAULT`、`PRIMARY_UNAVAILABLE`、`QUOTA_DOWNGRADE`、`NO_ELIGIBLE_MODEL`）。
- 选择算法：
  1. 取场景 primary + fallbacks。
  2. 过滤 `enabled`、能力满足、健康可用。
  3. 若配额超限且启用了 quota 路由，优先选 `tier == cheap` 且满足能力的候选，同一档位内按 priority 降序；没有则保持原顺序并记录 `QUOTA_NO_CHEAPER_MODEL`。
  4. 其余按场景配置顺序，保证降级顺序可控；`priority` 仅在同一档位存在多个候选时作为同档排序依据，不跨场景覆盖显式顺序。
- 不把路由决策放到 LLM 调用内部，避免“选模型的调用”再次依赖模型。

### 4.5 ResilientAiService 改造

- 方法增加场景参数：`callChat(ModelScenario, List<Message>)`、`callRagChat(ModelScenario, String)`、`callSummary(ModelScenario, Prompt)`、`callSseStream(ModelScenario, List<Message>)`、`callSseRagStream(ModelScenario, String)`。
- 内部流程：
  1. 调用 `QuotaService` 获取配额结果（启用时）。
  2. 调用 `ModelRouter.select()` 得到候选链。
  3. 依次调用候选模型；每个候选使用自己的 TimeLimiter/Retry/CircuitBreaker。
  4. 每次尝试独立记录观测和 `model_usage`：实际 provider/model、`routeReason`、`fallbackFrom`、`attempt`。
  5. 候选全部失败才执行现有固定文案 fallback，并标记 `ALL_MODELS_FAILED`。
- 非流式：主模型瞬时网络错误最多重试 1 次；429/5xx/超时直接切换下一个候选；4xx 记录告警并切换。
- 流式：订阅前按 `ModelRouter.select()` 选择健康模型；不配置流内 Retry；流开始后失败只推送错误事件。

### 4.6 直接 ChatClient 调用迁移

- `QueryRewriteService`、`ChartAnswerGenerator`、`LlmJudgeService` 注入 `RoutedChatClientFactory`。
- `RoutedChatClientFactory.chatClientFor(ModelScenario)` 返回当前路由模型的 `ChatClient`。
- 迁移后这三个服务仍保留各自现有 catch/fallback：rewrite 降级原问题、chart 降级模板、judge 返回 null。

## 5. 数据流

```
Controller / Agent
  -> ChatServiceImpl / IntentClassifierService
  -> ResilientAiService.callX(ModelScenario, ...)
  -> QuotaService.check(...)              [quota 超限 -> 降档 hint]
  -> ModelRouter.select(scenario, capabilities, quota)
  -> RouteDecision(selected, fallbackChain, reason)
  -> per-model OpenAiChatModel / ChatClient
  -> success/failure -> 观测 + model_usage
  -> 候选失败 -> 下一个候选
  -> 全部失败 -> 固定兜底 / 本地规则降级
```

## 6. 可观测与成本

- `TraceSpan` 增加 `routeReason`、`fallbackFrom`、`attempt` 字段。
- `ModelUsageRecorder` 每次尝试记录实际 provider/model；失败尝试也记录。
- `ModelUsageProperties` 增加 `siliconflow-qwen3-8b`、`siliconflow-qwen3-32b` 单价，便于成本看板分账。
- 路由决策本身不作为独立 LLM span；路由原因作为调用 span 的 key-value 附加。

## 7. 测试策略

TDD，测试先行：

- `ModelRouterPropertiesTest`：配置校验、重复 ID、缺失 primary、非法 fallback。
- `ChatModelRegistryTest`：按配置构建客户端、禁用模型不构建、刷新复用。
- `ModelHealthRegistryTest`：独立熔断、熔断打开后 `isAvailable=false`、半开恢复。
- `ModelRouterTest`：场景选择、能力过滤、健康过滤、配额降档、无候选原因。
- `ResilientAiServiceTest`：主模型失败切备选、重试上限、4xx/429/5xx 行为、全部失败兜底、观测字段。
- 流式测试：熔断打开时订阅前选备选；流内失败不重放。
- 迁移服务测试：rewrite/chart/judge 路由后保留本地降级。
- 集成测试：Nacos 刷新后新配置生效，旧配置在刷新失败时保留。

## 8. 验收标准

1. 主模型 Key 失效或 base-url 不可达时，chat/rag/agent 自动切到 `siliconflow-qwen3-32b`，trace 显示实际模型和 `fallbackFrom`。
2. intent/summary 默认走 `siliconflow-qwen3-8b`，`model_usage` 按实际模型分账。
3. 配额超限时优先降档到 `cheap`；无满足能力便宜模型时保持原路由并记录原因。
4. 主模型熔断打开后，流式对话在首 token 前使用备选模型；开始后不重放。
5. 主模型故障不再连带熔断备选模型。
6. `mvn -pl ai-cs-chat -am test` 全部通过，覆盖率门禁通过。

## 9. 不做的事

- 不实现语义缓存（独立 P1）。
- 不引入外部 LLM 网关。
- 不做基于 trace 的自动降级路由。
- 不做多租户/按用户自定义模型路由。
- 不改变视觉模型链路；VLM 仍走 `VisionChatService`。
