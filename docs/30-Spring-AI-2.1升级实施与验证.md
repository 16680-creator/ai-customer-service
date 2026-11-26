# Spring AI 2.1 升级实施与验证

> 文档基线：2026-11-26，依据当前工作区父 POM、业务代码和配置核对。
> 当前版本：**Java 17 / Spring Boot 4.2.0 / Spring Cloud 2026.0.0 / Spring Cloud Alibaba 2025.1.0.0 / Spring AI 2.1.0**。
> 本文是**当前实施说明**；[12-SpringAI-2.0升级评估与迁移指南](../learning-docs/05-AI集成/01-SpringAI框架集成/12-SpringAI-2.0升级评估与迁移指南.md) 保留迁移前评估和演进过程。

## 1. 当前版本与依赖

父 `pom.xml` 通过 `spring-ai-bom:2.1.0` 统一版本，业务模块不自行声明 Spring AI 版本。

| 能力 | 当前 Maven 坐标 | 模块 |
|---|---|---|
| OpenAI 兼容对话/Embedding | `org.springframework.ai:spring-ai-starter-model-openai` | chat、knowledge、search、product |
| OpenAI Java SDK HTTP 实现 | `com.openai:openai-java-client-okhttp:4.101.0` | 上述手动创建客户端的模块 |
| 向量库抽象 | `org.springframework.ai:spring-ai-vector-store` | chat、knowledge |
| Chroma | `org.springframework.ai:spring-ai-starter-vector-store-chroma` | chat、knowledge、search |
| RAG Advisor | `org.springframework.ai:spring-ai-vector-store-advisor` | chat |
| 文档读取 | `spring-ai-pdf-document-reader` / `spring-ai-tika-document-reader` | chat |

不要再使用 `spring-ai-openai-spring-boot-starter` 或 `spring-ai-advisors-vector-store`。Spring AI 2.1 的 OpenAI 手动装配使用官方 `OpenAIClient` / `OpenAIOkHttpClient`，不是旧文档里的 `OpenAiApi` 构造示例。

## 2. 对话与多模型路由

`ChatModelRegistry` 按 `aics.model-router.models` 为启用的模型创建独立 `OpenAiChatModel` 和 `ChatClient`：

```java
OpenAIClient client = OpenAIOkHttpClient.builder()
        .baseUrl(definition.getBaseUrl())
        .apiKey(definition.getApiKey())
        .build();
OpenAiChatModel model = OpenAiChatModel.builder()
        .openAiClient(client)
        .openAiClientAsync(client.async())
        .options(OpenAiChatOptions.builder()
                .model(definition.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .build())
        .build();
```

同步与异步客户端必须共享同一供应商的 `baseUrl` 和凭据，避免 SSE/流式调用回退到另一供应商配置。

- `ChatClientCustomizer` 统一注入默认系统提示与 `QuestionAnswerAdvisor`。
- 只有声明 `tool-calling` 能力的模型挂载 `ToolCallbackProvider`。
- 注册表以不可变 Map 快照整体替换；刷新失败保留旧快照，首次启动失败则 fail-fast。
- 工具注解使用 `@Tool` / `@ToolParam`，工具对象由 `MethodToolCallbackProvider` 注册。
- 当前 `ChatModelRegistry` 仍显式调用 `defaultToolCallbacks(ToolCallbackProvider)`。不要按 2.0 评估稿机械替换成 `defaultTools(Object...)`：前者是 Provider 重载，后者接收工具对象，迁移时以编译和工具执行测试为准。

## 3. Embedding 与配置扁平化

对话模型和向量模型分离：DeepSeek 负责文本回答，默认 `BAAI/bge-m3` 负责知识库向量。chat/knowledge/search 通过 `aics.embedding.*` 创建 `OpenAiEmbeddingModel`：

```java
return new OpenAiEmbeddingModel(client, MetadataMode.EMBED,
        OpenAiEmbeddingOptions.builder().model(embeddingModel).build());
```

Java Builder 仍是 `OpenAiEmbeddingOptions`；**YAML 属性扁平化不代表 Java 类型改名**。

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.deepseek.com}
      chat:
        model: ${OPENAI_MODEL:deepseek-chat}
        temperature: ${OPENAI_TEMPERATURE:0.7}
```

OpenAI chat 的 YAML 属性不再嵌套 `options`。本项目自定义 Embedding 统一读 `aics.embedding.base-url/api-key/model`，不要把自定义键误写成另一套 Spring AI 自动配置键。

同一 Chroma 集合的写入和查询必须保持同一模型与向量维度。切换模型要重建向量或做版本化迁移，不能只改模型名。

## 4. RAG 与 Chroma 2.1 实现

`SpringAiConfig.ragAdvisor` 仍使用：

```java
QuestionAnswerAdvisor.builder(vectorStore)
        .searchRequest(SearchRequest.builder()
                .similarityThreshold(0.3d)
                .topK(5)
                .build())
        .build();
```

`QuestionAnswerAdvisor` 包名保持 `org.springframework.ai.chat.client.advisor.vectorstore`；变更的是 Advisor Maven 模块名。

`VectorStoreConfig` 当前不是空配置类：它显式构造 `ChromaVectorStore`，并用 `VectorStore` 接口代理延迟解析目标 Bean。原因是 Chroma 2.1 的生命周期初始化可能访问远端集合，即使 `initialize-schema=false` 也不能把它理解为“启动完全离线”。代理只推迟网络访问，**首次实际 RAG 入库/检索仍要求 Chroma v2 可达**。

```yaml
spring:
  ai:
    model:
      embedding: none
    vectorstore:
      chroma:
        initialize-schema: false
        collection-name: aics-knowledge
        client:
          host: ${CHROMA_HOST:http://localhost}
          port: ${CHROMA_PORT:8000}
```

chat 启动类和 `spring.autoconfigure.exclude` 均排除 `OpenAiEmbeddingAutoConfiguration`，配合 `spring.ai.model.embedding=none` 防止默认 Embedding 与手动 `@Primary` Bean 重复装配。

## 5. 多模态当前实现

`VisionModelClient` 使用独立 `OpenAIClient` 创建视觉 `OpenAiChatModel`，同步/异步客户端同样成对配置。消息构造保持：

```java
UserMessage message = UserMessage.builder()
        .text(instruction)
        .media(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl)))
        .build();
ChatResponse response = visionModel.call(new Prompt(List.of(message)));
```

图片 URL 先经过 `ImageUrlValidator`，视觉失败可退化为纯文本；是否真实识别成功需要模型联调，不能由 Bean 创建测试替代。

## 6. Boot 4 配套变化

- MyBatis-Plus 使用 `mybatis-plus-spring-boot4-starter`，分页相关功能显式引入 `mybatis-plus-jsqlparser`。新增批量 Mapper 重载后，测试中的 `insert/updateById(any())` 需要显式 `any(Product.class)`；Spring Data Redis 的 TTL 断言使用 `getTtlFunction().getTimeToLive(...)`。
- Tika 4 / POI 5.5.1 需要 Commons Compress 1.28.0。MinIO 传递的 1.26.0 会使 Tika ZIP 格式探测失败；父 POM 统一 1.28.0 后，`DocumentLoaderTest` 5 项（docx/xlsx/html/md/缺失文件降级）已通过。
- MockMvc / `@WebMvcTest` 使用 Boot 4 的独立测试 starter；`@MockBean` 迁移为 `@MockitoBean`。
- HTTP JSON 默认 Jackson 3；仍使用 `com.fasterxml.jackson.databind.ObjectMapper` 的业务代码由 common 的 `Jackson2AutoConfiguration` 提供独立兼容 Bean。
- 公共 Servlet 异常处理由 `CommonWebAutoConfiguration` 自动装配；MVC 切片验证异常映射时需包含该自动配置。
- 其他中间件在 Boot 4 下的兼容不能仅凭 Spring AI 测试推断，仍需各自链路和部署回归。

## 7. 验证范围

```bash
mvn -pl ai-cs-chat,ai-cs-knowledge,ai-cs-search,ai-cs-product -am test
```

该命令用于验证编译、单元测试、配置绑定与离线上下文测试。**2026-11-26 最终复测结果：通过。**

| 模块 | 通过数量 | 失败 | 错误 |
|---|---:|---:|---:|
| ai-cs-common | 38 | 0 | 0 |
| ai-cs-knowledge | 8 | 0 | 0 |
| ai-cs-chat | 289 | 0 | 0 |
| ai-cs-search | 14 | 0 | 0 |
| ai-cs-product | 54 | 0 | 0 |
| **合计** | **403** | **0** | **0** |

最终同一条 Reactor 命令于 2026-11-26 15:44 +08:00 返回 `BUILD SUCCESS`。其中包括 `VectorStoreConfigTest` 的 Chroma 离线启动接口代理测试、Tika 5 项解析测试、38 个 chat 安全 BDD 场景以及 product 缓存契约测试。

### 测试过程中修复的真实升级问题

1. 初次 chat 289 项中 Tika HTML/Markdown 2 项失败。依赖树证明 Tika 4 / POI 5.5.1 要求 Commons Compress 1.28.0，MinIO 传递的 1.26.0 被选中；父 POM 统一 1.28.0 后，Tika 5 项通过，common/MinIO 装配测试通过。
2. product 测试源码存在 MyBatis-Plus 批量重载导致的 Mockito `any()/argThat()` 歧义，以及 Redis Cache `getTtl()` 移除。显式限定 `Product` 类型并改用 `getTtlFunction().getTimeToLive(...)` 后，product 54 项通过。
3. 这些是 `mvn test` 的编译、单元和离线/模拟上下文验证，**不是全仓所有模块的 `mvn verify`，也不等于真实 DeepSeek/硅基流动调用、Chroma 读写、Nacos 刷新、RocketMQ/Seata/MySQL 链路或生产部署验收**。

历史 1.x 接口报告和故障结论不可直接沿用为 2.1 验收证据。

## 8. 文档维护规则

1. 当前版本以父 POM 和 `.claude/context/tech-stack.md` 为准。
2. 描述现行实现的代码示例必须与实际类名、包名、配置键一致。
3. 旧版本演进内容放入明确标注的历史章节，不删除、不伪装成当前状态。
4. “可运行”“已验证”必须区分编译、离线测试、真实外部服务和生产部署。
5. API Key、密码和内网凭据只能使用占位符；不得在教程或 Nacos 源文件中保存真实密钥。
