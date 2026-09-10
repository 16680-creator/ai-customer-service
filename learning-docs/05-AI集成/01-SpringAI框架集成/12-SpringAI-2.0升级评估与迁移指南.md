# 12-SpringAI 2.0 升级评估与迁移指南（1.1.4 → 2.0）

> **文档状态**：评估完成（2026-09-10）。**决策：暂缓代码改造**，本文档作为后续实际升级时的行动指南与学习材料。
> 适用对象：`ai-customer-service` 全家桶（重点 `ai-cs-chat`、`ai-cs-knowledge`）。
> 结论速览：**Spring AI 侧是小手术（约 4 处代码 + 1 处 POM 改名 + 配置迁移），真正瓶颈在 Spring Boot 4.0 生态链**（Spring Cloud / SCA / Seata / RocketMQ / MyBatis-Plus 等 8+ 依赖同步跨版本），总工作量估算 6~10 人天。

---

## 1. 背景：为什么会有 2.0 这道坎

- Spring AI **2.0.0 GA** 于 2026-06-12 发布（随后 2.0.1 已跟进）；同期 1.1 线仍在维护（1.1.8，2026-06-12）。
- 2.0 的硬性基线：**Spring Boot 4.0 / 4.1 + Spring Framework 7.0**。也就是说：**不升 Boot 4 就用不了 Spring AI 2.0**，两个升级必须捆绑评估。
- 本项目现状（父 POM）：Spring Boot **3.2.5** / Spring Cloud 2023.0.1 / SCA 2023.0.1.0 / Spring AI **1.1.4** / Java 17。
- 时间压力：Spring Boot 3.5 的 OSS 支持到 **2026 年 11 月**。届时若还停在 Boot 3.x，将失去安全补丁。稳妥的中间态是「Boot 3.5.x + Spring AI 1.1.8」，但 Boot 4 切换建议不晚于 2026 Q4。

```
当前：Boot 3.2.5 ── Spring Cloud 2023.0.1 ── SCA 2023.0.1.0 ── Spring AI 1.1.4
目标：Boot 4.0.x ── Spring Cloud 2025.1.x ── SCA 2025.1.x   ── Spring AI 2.0.x
路径：3.2.5 → 3.5.x（必须经过，官方禁止 3.3 以下直跳 4.0）→ 4.0.x
```

---

## 2. 2.0 的版本地基：Boot 4 / Framework 7 带来了什么

Spring AI 2.0 的破坏性改动分两层：**地基层**（Boot 4 强加的）和**框架层**（Spring AI 自己重构的）。先记地基层：

| 地基变化 | 对本项目的影响 |
|---|---|
| 自动配置模块化：单体 `spring-boot-autoconfigure` 拆成 47 个轻量模块 | 引第三方自动配置时可能要补对应 starter/模块；`spring.autoconfigure.exclude` 按类全名排除的方式仍有效（FQN 需逐个验证） |
| Jackson 3（`tools.jackson`）成为默认 | 项目约 10+ 个主代码文件直接用 `com.fasterxml.jackson.databind.ObjectMapper`（chat/mq/user/pay/notify）。HTTP 层序列化会切到 Jackson 3，业务内 Jackson 2 需显式保留依赖做过渡 |
| JSpecify 空安全注解替代 `org.springframework.lang` | 编译期警告为主，可渐进处理 |
| 测试栈：JUnit 6 + Mockito 变化（`@MockBean` 移除 → `@MockitoBean`） | 项目 Cucumber 7.15 + junit-platform-suite 1.10.2 需升级对齐（BDD 安全用例受影响） |
| Jakarta EE 11（Servlet 6.1 / Tomcat 11） | 代码层基本无感，Docker 基镜像确认 JDK ≥ 17 即可（Java 17 仍是最低线，推荐 21） |

---

## 3. Spring AI 2.0 破坏性改动总览（框架层）

以下内容整理自官方 Upgrade Notes（1.1.x → 2.0.0 / 2.0.1），按主题分组。**加粗**标记的是本项目会命中的点。

### 3.1 工具调用（2.0 最核心的重构）

2.0 把"工具执行"从 ChatModel 内部搬到了 Advisor 层，术语全面统一为 Tool：

| 变化 | 1.x 行为 | 2.0 行为 | 迁移动作 |
|---|---|---|---|
| ChatModel 内部工具循环**移除** | `chatModel.call(prompt)` 遇到 tool_calls 会自动执行并循环 | 返回裸响应，不自动执行 | 走 ChatClient（自动注册 ToolCallingAdvisor）；直接用 ChatModel 的要自己驱动循环 |
| `internalToolExecutionEnabled` **移除** | 可在 ChatOptions 上关闭内部执行 | 编译错误 | 删掉该调用，改用 `AdvisorParams.toolCallingAdvisorAutoRegister(false)` |
| `ToolCallAdvisor` 改名 `ToolCallingAdvisor` | — | 编译错误 | 改引用 |
| FunctionCallback 家族彻底移除 | 已废弃的 legacy API | 编译错误 | 用 ToolCallback（本项目 1.0 起就是新 API，不受影响） |
| `toolNames()` / `SpringBeanToolCallbackResolver` **移除** | 按 Bean 名解析 Function | 编译错误 | 声明显式 ToolCallback Bean 或 @Tool 对象 |
| `tools(Consumer<ToolSpec>)` **移除** | ToolSpec 消费者式 API | 编译错误 | `tools(Object...)` + `toolContext(Map)` |
| `defaultToolCallbacks()` **废弃** | 挂载工具回调 | 仍可用但有废弃告警 | **改用 `defaultTools(Object...)`** ← 本项目命中 |
| ChatClient **自动注册 ToolCallingAdvisor** | 需要手动挂或靠模型内部循环 | 只要有工具，advisor 链自动补上 | 显式挂过 ToolCallingAdvisor 的会重复，需删除；本项目没显式挂过 → 无需动作 |
| ToolContext **不再自动带会话历史** | `TOOL_CALL_HISTORY` 常量/方法移除 | 编译错误（如引用） | 会话上下文归 advisor 层管 |
| 工具调用次数**默认上限** | 无上限 | 每工具 40 次 / 总计 150 次 | `ToolCallingManager.builder().maxCallsPerTool(...)` 或 `spring.ai.tools.limits.*` 可调 |
| Bean 工具解析 fallback 默认**关闭** | 未挂到请求的工具也会被解析执行 | 只执行请求上挂的工具 | 如有依赖加 `spring.ai.tools.resolution.fallback.enabled=true` |
| OpenAI strict 模式默认 **false** | 工具 schema 默认 strict(true)，带可选参数的工具会被 OpenAI 拒 400 | 默认非 strict | 需要时 `OpenAiChatOptions.builder().strict(true)` |

> 本项目工具调用形态：`@Tool/@ToolParam` 注解 + `MethodToolCallbackProvider` + `ChatClient.builder().defaultToolCallbacks(...)`，调用走 `chatClient.prompt()...call()/stream()`。**唯一要改的是 defaultToolCallbacks → defaultTools**，其余全部兼容。

### 3.2 Advisor 体系

| 变化 | 说明 |
|---|---|
| **模块改名：`spring-ai-advisors-vector-store` → `spring-ai-vector-store-advisor`** | ← 本项目 ai-cs-chat/pom.xml 命中，必须改 |
| `Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER` 默认值从 +1000 改为 +200 | 只影响自定义 advisor 排序的细节 |
| ToolCallingAdvisor 默认在工具循环内部管理会话历史 | 配合 MessageChatMemoryAdvisor 时注意 advisorOrder；不用记忆 advisor 则无感 |
| 新增 `ToolAdvisor` / `MemoryAdvisor` 标记接口 | 自定义 advisor 拥有工具/记忆职责时应实现，避免重复注册 |

`QuestionAnswerAdvisor` 本体 API（builder + SearchRequest）未列入 2.0 破坏性清单，编译期验证即可。

### 3.3 聊天记忆（ChatMemory）——本项目整体豁免

2.0 对记忆体系改动很大，但**本项目不用 Spring AI ChatMemory**（会话历史由 Redis 热缓存 + chat_message 表自管），以下全部不适用，仅作知识储备：

- conversation ID 变为**必填**：不传 `ChatMemory.CONVERSATION_ID` 直接抛 `IllegalArgumentException`；`DEFAULT_CONVERSATION_ID` 常量移除
- memory advisor 的 `.conversationId()` builder 方法移除（改为调用时通过 advisor context 传入）
- `PromptChatMemoryAdvisor` 移除 → 用 `MessageChatMemoryAdvisor`
- JDBC 记忆表新增 `sequence_id` 列（需迁移老表）；Redis 记忆模块改名（2.0.1：`...chat-memory-redis` → `...chat-memory-repository-redis`，属性前缀同步）
- 官方预告：`spring-ai-session` 社区项目将在 2.1 取代 ChatMemory

### 3.4 Options 与配置属性

| 变化 | 迁移动作 |
|---|---|
| Options 严格不可变：`copy()` / `fromOptions()` 移除 | 改用 `mutate()...build()` |
| **ChatClient 的 `.options()` / `.defaultOptions()` 只接受 Builder**（不再接受 build 好的实例） | `chatClient.prompt().options(OpenAiChatOptions.builder()....)`（注意：`OpenAiChatModel.builder().defaultOptions(实例)` 属于 ChatModel 路径，仍收实例） |
| **配置属性扁平化：去掉 `.options` 段** | `spring.ai.openai.embedding.options.model` → `spring.ai.openai.embedding.model`（旧键有过渡期兼容） |
| Options builder 的 `N()` 改名 `n()` | 编译错误点，全局替换 |
| 模型开关属性拆分 | `spring.ai.model.embedding` 现在支持 `embedding.text` / `embedding.multimodal` 细分；**本项目 `spring.ai.model.embedding.enabled: false` 建议改为 `spring.ai.model.embedding: none`** |
| **默认 temperature 移除** | 1.x 默认 0.7，2.0 交给供应商（DeepSeek 默认 1.0，会明显"更放飞"）。要保行为请显式配置 `spring.ai.openai.chat.temperature: 0.7` ← 本项目命中 |

### 3.5 MCP（本项目代码未集成，整体豁免）

2.0 的 MCP 变化很密集，仅列要点供后续集成时参考：MCP Java SDK 升到 2.0.0（服务端默认开启入参 schema 校验）；`@McpTool` 注解从 `org.springaicommunity.mcp.*` 收编进 `org.springframework.ai.mcp.annotation.*`；mcp-spring-webflux/webmvc 传输模块归属 Spring AI（groupId 变更）；elicitation 的 `TypeReference` → `ParameterizedTypeReference`；`McpSyncClientCustomizer/McpAsyncClientCustomizer` 合并为 `McpClientCustomizer<B>`。

### 3.6 OpenAI 模块：换官方 SDK 底座（好消息）

2.0 起 `spring-ai-openai` 内部改用**官方 openai-java SDK**（Chat/Embedding/Image/Audio/Moderation 全部），但官方声明：**`spring.ai.openai.*` 属性、builder、options 全部保留**。对本项目手动装配的 `OpenAiApi.builder()` / `OpenAiChatModel.builder()` / `OpenAiEmbeddingModel(...)` 构造方式预期零改动（编译期验证）。
同时移除的模块：`spring-ai-azure-openai`、`spring-ai-openai-sdk`、`spring-ai-oci-genai`（本项目未用）。

### 3.7 JSON 工具与结构化输出

- `ModelOptionsUtils` 的静态 JSON 方法移除、`JsonParser` 废弃 → 统一走 `JsonHelper` / `JacksonUtils.getDefaultJsonMapper()`（本项目未直接使用，不受影响）
- `BeanOutputConverter` 的 JSON Schema 生成对齐工具调用逻辑（可选字段不再进 required 数组、增加 OpenAPI 风格 format 提示）；`postProcessSchema` 扩展点移除 → 覆写 `generateSchema()`（本项目没用 `.entity()`/结构化输出，豁免）

### 3.8 向量库与其他

- 移除模块：hanadb store、Azure Cosmos DB（转外部维护）
- OpenSearch 客户端升 3.x（走 VectorStore 接口则透明）
- `AbstractFilterExpressionConverter.doSingleValue` 变抽象方法（自定义 filter converter 才受影响）
- **行为变化（不报错但结果变）**：
  1. 工具循环内 **Usage 累计**：`.call().chatResponse()` 拿到的 token 数从"最后一次调用"变为"整个循环累计" → 本项目 `ModelUsageRecorder` 的费用/用量统计数字会变大（口径更准确），看板基线要重置
  2. 可观测性 span：工具调用 `tool_call <name>` → `execute_tool <name>`，新增 `spring.ai.tool.type` / `spring.ai.tool.call.id` 属性 → Grafana/Tempo 查询条件同步

---

## 4. 对照本项目：命中 / 豁免清单

### 4.1 会命中的（升级时必改，共 6 处）

| # | 命中点 | 位置 | 动作 |
|---|---|---|---|
| 1 | RAG Advisor 模块改名 | ai-cs-chat/pom.xml | `spring-ai-advisors-vector-store` → `spring-ai-vector-store-advisor` |
| 2 | defaultToolCallbacks 废弃 | ChatModelRegistry.java | → `builder.defaultTools(toolCallbackProvider)` |
| 3 | Media 构造器兼容性待验证 | VisionModelClient.java | `new Media(MimeType, URI)` 若被移除 → `Media.builder().mimeType(...).data(URI).build()`（data(URI) 是 2.0 新增类型化重载） |
| 4 | Embedding 开关属性形态 | ai-cs-chat/application.yml | `spring.ai.model.embedding.enabled: false` → `spring.ai.model.embedding: none`；同步验证 `spring.autoconfigure.exclude` 里的 `OpenAiEmbeddingAutoConfiguration` FQN |
| 5 | temperature 默认值移除 | Nacos `ai-cs-chat.yml` | 显式配置 `spring.ai.openai.chat.temperature: 0.7` |
| 6 | Usage 口径 / span 名 | ModelUsageRecorder + Grafana | 统计基线重置 + 面板查询条件更新 |

### 4.2 天然豁免的（不用 Spring AI 对应能力）

ChatMemory 全家桶（conversationId 必填、PromptChatMemoryAdvisor 移除、JDBC sequence_id、Redis 模块改名）｜MCP 全部变化｜`toolNames()`/FunctionCallback legacy｜`.entity()`/BeanOutputConverter｜`internalToolExecutionEnabled`｜ChatClient `.options()` 传实例｜Ollama/Anthropic/Minimax/Google/Azure 模块。

### 4.3 编译期只需"验证签名"的（预期零改动）

`OpenAiApi.builder()`（ChatModelRegistry / SpringAiConfig / VisionModelClient 三处手动装配）、`OpenAiEmbeddingModel` 构造器、`QuestionAnswerAdvisor.builder().searchRequest(...)`、`MethodToolCallbackProvider.builder().toolObjects(...)`（仅异常类型 IllegalState→IllegalArgument，本项目无 catch 点）、`SearchRequest.builder()`、`TokenTextSplitter`。

---

## 5. Boot 3.2.5 → 4.0 生态兼容性（真正的瓶颈）

官方红线：**禁止 3.3 以下直跳 4.0**，必须先到 3.5.x 修完弃用警告。

| 依赖 | 当前 | Boot 4 目标 | 风险 | 说明 |
|---|---|---|---|---|
| Spring Boot | 3.2.5 | 4.0.x（经 3.5.x） | 🔴 主线 | MockBean→MockitoBean、自动配置模块化、JUnit 6 |
| Spring Cloud | 2023.0.1 | **2025.1.x (Oakwood)** | 🟢 确认 | 官方：2025.1.2 兼容 Boot 4.0.7 / 4.1.0 |
| Spring Cloud Alibaba | 2023.0.1.0 | **2025.1.x 线** | 🟢 确认 | SCA 双版本线之一，适配 Boot 4.0.x；Nacos/Sentinel starter 随升 |
| Spring AI | 1.1.4 | 2.0.x | 🟡 命中面小 | 见第 3、4 节 |
| Seata | 1.7.1 + seata-http | 随 SCA 带的 2.x | 🟠 需 PoC | 无 Boot 4 官方兼容声明；下单/支付分布式事务必须回归 |
| RocketMQ starter | 2.3.0 | 待验证 | 🟠 需 PoC | 无明确 Boot 4 适配声明 |
| MyBatis-Plus | 3.5.6 boot3-starter | **boot4-starter** | 🟡 有方案 | artifact 切换 |
| Redisson | 3.27.2 | **4.1.0+** | 🟡 有方案 | 社区 Boot 4 迁移实践采用 |
| springdoc | 2.3.0 | **3.1.x** | 🟢 确认 | springdoc FAQ：3.x 兼容 Boot 4 |
| ShardingSphere-JDBC | 5.5.0（driver 模式） | PoC | 🟢 低风险 | 非 starter，按 JDBC DataSource 用 |
| Resilience4j | 2.2.0 boot3 | 最新版 PoC | 🟠 需 PoC | ResilientAiService 弹性层依赖它 |
| spring-statemachine | 4.0.0 | PoC | 🟠 需 PoC | Framework 7 适配待验证（订单状态机） |
| xxl-job | 2.4.1 | 保持 | 🟢 低风险 | core 自带通信，弱依赖 Spring |
| Jackson | 2.x（BOM） | 3.x 默认 | 🟡 需处理 | 业务代码 Jackson 2 显式留依赖过渡或迁 `tools.jackson` |
| Cucumber / junit-platform-suite | 7.15 / 1.10.2 | 对齐 JUnit 6 | 🟠 需升级 | BDD 安全用例（SecurityGuardrailSteps）受影响 |

---

## 6. 升级路径（执行时照此走）

```
阶段 0（前置，~1 天）
  Boot 3.2.5 → 3.5.x ＋ Cloud 2025.0.x ＋ SCA 2023.0.3.4 ＋ Spring AI 1.1.8
  目的：先在不动大架构的前提下对齐到"最新 3.x 稳态"，清掉弃用警告
  （Spring AI 1.1.4 → 1.1.8 只是补丁位，含 Chroma 等修复）

阶段 1（Boot 生态跨版本，2~4 天）
  Boot 4.0.x ＋ Cloud 2025.1.x ＋ SCA 2025.1.x
  ＋ MyBatis-Plus boot4-starter / Redisson 4.1 / springdoc 3.1 / 测试栈升级
  注意：Spring AI 1.1 线不能配 Boot 4 → 本阶段末尾必须同步切 Spring AI 2.0.x
  （即阶段 1、2 在同一次切换里完成，但验证分开做）

阶段 2（Spring AI 2.0 代码适配，1~2 天）
  执行第 4.1 节 6 处改造 → 编译清零 → 各服务启动冒烟

阶段 3（回归与口径，2~3 天）
  · mvn -pl ai-cs-chat test -Peval   （RAG 评估门禁）
  · SecurityGuardrailSteps（BDD 安全用例）
  · ChatObservabilityIntegrationTest ＋ Grafana/Tempo 面板口径更新
  · Seata 下单事务 / RocketMQ 链路 / ShardingSphere 分片手工回归
```

**稳妥替代方案**：资源紧张的季度可停在「Boot 3.5.x + Spring AI 1.1.8」（1.1 线仍受官方维护），但 Boot 3.5 OSS 支持到 **2026-11**，Boot 4 切换不宜晚于 Q4。

**风险验证项（PoC 优先级排序）**：R1 Seata 事务链路（全流程下单→支付→取消）｜R2 RocketMQ 消费者兼容｜R3 Resilience4j/statemachine 在 Framework 7 下的 AOP 代理｜R4 Spring AI 三个手动装配 API 签名｜R5 Jackson 2/3 并存回归｜R6 `spring.autoconfigure.exclude` FQN 失效导致 Embedding 双 Bean（兜底：`spring.ai.model.embedding: none`）。

---

## 7. 与本项目代码的关联索引

| 主题 | 项目文件 |
|---|---|
| ChatClient/模型路由装配（defaultTools 改造点） | `ai-cs-chat/src/main/java/com/aics/chat/modelrouter/ChatModelRegistry.java` |
| RAG Advisor / Embedding 手动装配（签名验证点） | `ai-cs-chat/src/main/java/com/aics/chat/config/SpringAiConfig.java` |
| 多模态 Media 构造（兼容性验证点） | `ai-cs-chat/src/main/java/com/aics/chat/service/impl/VisionModelClient.java` |
| ChatClient 四种调用形态（call/stream，均兼容） | `ai-cs-chat/src/main/java/com/aics/chat/service/impl/ResilientAiService.java` |
| @Tool 工具对象（注解 API 不变） | `OrderQueryService.java` / `Nl2SqlQueryService.java` |
| Embedding 开关与 exclude 配置 | `ai-cs-chat/src/main/resources/application.yml` |
| Token/费用计量（Usage 口径变化） | `ai-cs-chat/src/main/java/com/aics/chat/observability/ModelUsageRecorder.java` |
| 知识库向量化（预期零改动） | `ai-cs-knowledge/.../KnowledgeVectorService.java` / `KnowledgeAiConfig.java` |
| 上一次版本升级的作业记录（1.0.0→1.1.4） | 根目录 `SPRING-AI-UPGRADE-TASKS.md` |

---

## 8. 已就地标注 2.0 提示的文档清单（2026-09-10）

> 为便于逐篇查阅，已在 `05-AI集成` 下 **35 篇**涉及 Spring AI / MCP 的文档顶部插入「可升级至 2.0」标注。
> 每篇标注内含**该文相关的具体变化要点**（而非通用模板），可据此快速定位影响面。

| 子目录 | 已标注文档 | 篇数 |
|---|---|---|
| `01-SpringAI框架集成` | 01-SpringAI入门 · 02-LLM工程化实践 · 03-VLM多模态 · 04-Agent编排 · 05-LLM可观测 · 06-AI安全网关 · 07-AI集成 · 08-缺口分析 · 09-接口测试报告 · 10-提示词灰度 · **11-MCP协议** | 11 |
| `02-RAG全栈实战` | 01-RAG检索增强 · 02-RAG开发 · 03-向量检索 · 05-混合检索 · 06-引用溯源 · 07-Tika多格式 · 08-增量同步 · 10-知识点汇总 | 8 |
| `面试题` | 02-Spring-AI框架 · 03-RAG · 04-向量库 · 05-Agent与工具调用 · 06-Prompt · 07-安全Guardrails · 08-架构 · 09-工程化 · 10-综合实战 · README | 10 |
| `05-向量数据库` | README · 01-向量与相似度 · 03-选型与容量规划 | 3 |
| `04-规格驱动工具` | 02-Superpowers实战 · 04-SpecKit-RAG六件套 | 2 |
| 根目录 | 代码注释与知识点解析 | 1 |
| **合计** | | **35** |

**模板差异**：`11-MCP协议实战` 采用 MCP 模板（标注 MCP Java SDK 0.14+ → 2.0.0，指向本文 §3.5），其余 34 篇采用 Spring AI 模板。

**未标注的文档**（不含本文所列 Spring AI / MCP API）：`03-AI编码方法论/`（6 篇，纯方法论）、`04-规格驱动工具/01、03`（讲工具用法）、`05-向量数据库/02、04、05`（讲索引算法与生产运维）、`02-RAG全栈实战/04-Rerank、09-接口测试报告`。

---

## 9. 参考资料

- Spring AI 官方 Upgrade Notes（1.1→2.0 全部破坏性改动）：https://docs.spring.io/spring-ai/reference/upgrade-notes.html
- Spring AI 2.0.0 GA 公告：https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now
- Tool Calling in Spring AI 2.0（架构解读）：https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling
- Spring Cloud 2025.1.x（Oakwood）兼容 Boot 4 公告：https://spring.io/blog/2026/01/29/spring-cloud-2025-1-1-aka-oakwood-has-been-released
- Spring Boot 4 升级实战（生态版本对照）：https://developer.aliyun.com/article/1712576
- springdoc 兼容性 FAQ（3.x ↔ Boot 4）：https://springdoc.org/faq.html
