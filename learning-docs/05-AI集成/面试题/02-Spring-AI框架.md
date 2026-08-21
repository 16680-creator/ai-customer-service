# 第2章：Spring AI 框架

> Spring AI 是 Java 生态接入 AI 能力的官方框架，是 Java AI 岗位的**核心考点**。

---

## 2.1 Spring AI 核心架构

### Q1：Spring AI 的核心抽象层有哪些？请画出整体架构。★★★★★

**参考答案：**

Spring AI 提供了统一的 AI 模型抽象层，屏蔽不同 AI 提供商的差异。

```
┌────────────────────────────────────────────────────────────────┐
│                     Spring AI Application                      │
├────────────────────────────────────────────────────────────────┤
│  ChatClient（高级 API，Builder 模式，链式调用）                   │
├────────────────────────────────────────────────────────────────┤
│  ChatModel / EmbeddingModel / ImageModel / ModerationModel     │
│  （核心接口层，统一不同提供商）                                    │
├────────────────────────────────────────────────────────────────┤
│  OpenAiChatModel │ OllamaChatModel │ DashScopeChatModel │ ...  │
│  （具体实现层）                                                  │
├────────────────────────────────────────────────────────────────┤
│  API Client（HTTP 通信，REST/流式）                               │
└────────────────────────────────────────────────────────────────┘
```

**核心抽象：**

| 抽象接口 | 功能 | 典型实现 |
|----------|------|----------|
| ChatModel | 对话生成 | OpenAiChatModel, OllamaChatModel |
| EmbeddingModel | 文本向量化 | OpenAiEmbeddingModel |
| VectorStore | 向量存储 | MilvusVectorStore, PgVectorStore |
| ChatClient | 高级对话 API | 包装 ChatModel，支持链式调用 |
| Advisor | 请求/响应拦截器 | MessageChatMemoryAdvisor, QuestionAnswerAdvisor |

**面试关键点：** Spring AI 的设计思想类似于 Spring Data——统一接口，切换实现。换模型提供商只需换配置，不改代码。

---

### Q2：ChatModel 和 ChatClient 有什么区别？什么时候用哪个？★★★★★

**参考答案：**

**ChatModel** 是底层接口，直接封装模型调用：
```java
// ChatModel：底层，直接调用
ChatResponse response = chatModel.call(
    new Prompt("你好，介绍一下自己")
);
String text = response.getResult().getOutput().getText();
```

**ChatClient** 是高层 API，提供 Builder 模式和链式调用：
```java
// ChatClient：高层，链式调用，功能更丰富
String text = ChatClient.create(chatModel)
    .prompt()
    .system("你是一个Java技术专家")
    .user("解释一下Spring的IoC容器")
    .advisors(messageChatMemoryAdvisor)  // 加记忆
    .call()
    .content();
```

**使用建议：**
- 简单调用、需要精细控制 → ChatModel
- 复杂对话流、需要 Advisor/记忆/工具 → ChatClient
- 实际项目中**优先使用 ChatClient**，更灵活

---

### Q3：Spring AI 如何实现流式输出（Streaming）？★★★★★

**参考答案：**

流式输出让 LLM 的响应逐 token 返回，用户无需等待完整响应。

**实现方式（基于 Flux 响应式流）：**

```java
// ChatModel 流式调用
Flux<ChatResponse> responseFlux = chatModel.stream(
    new Prompt("写一首关于春天的诗")
);
responseFlux.subscribe(resp -> {
    System.out.print(resp.getResult().getOutput().getText());
});

// ChatClient 流式调用
Flux<String> contentFlux = ChatClient.create(chatModel)
    .prompt()
    .user("写一首关于春天的诗")
    .stream()
    .content();
contentFlux.subscribe(System.out::print);
```

**在 Controller 中返回 SSE（Server-Sent Events）：**

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestParam String message) {
    return chatClient.prompt()
        .user(message)
        .stream()
        .content();
}
```

**底层原理：** Spring AI 使用 WebClient（或 RestClient）发起 HTTP 请求，响应格式为 `text/event-stream`，通过 SSE 协议逐块推送 token。

**面试追问 - 流式输出对系统设计的影响：**
- 不能用传统同步请求-响应模式，需要 SSE 或 WebSocket
- 需要考虑连接超时、客户端断连重连
- Token 计费需要记录完整响应（流式结束后聚合）

---

## 2.2 Function Calling（函数调用）

### Q4：什么是 Function Calling？Spring AI 如何实现？★★★★★

**参考答案：**

**Function Calling** 让 LLM 能够调用外部工具/函数，将自然语言意图转化为结构化操作。

**工作流程：**
```
用户："北京今天的天气怎么样？"
   ↓
LLM 判断：需要调用 getWeather 函数
   ↓
LLM 输出：{"function": "getWeather", "args": {"city": "北京"}}
   ↓
Spring AI 自动执行函数，获取结果
   ↓
LLM 基于函数结果生成自然语言回答
```

**Spring AI 实现方式：**

```java
// 方式1：@Bean + @Description 注册函数
@Bean
@Description("根据城市名查询天气信息")
public Function<WeatherRequest, WeatherResponse> getWeather() {
    return request -> weatherService.getWeather(request.city());
}

// 使用
String response = ChatClient.create(chatModel)
    .prompt()
    .user("北京今天天气怎么样？")
    .functions("getWeather")  // 指定可用函数
    .call()
    .content();

// 方式2：直接在 prompt 中注册
String response = ChatClient.create(chatModel)
    .prompt()
    .user("北京今天天气怎么样？")
    .functions(FunctionCallback.builder()
        .function("getWeather", (WeatherRequest req) -> 
            weatherService.getWeather(req.city()))
        .description("根据城市名查询天气信息")
        .inputType(WeatherRequest.class)
        .build())
    .call()
    .content();
```

**面试关键点：** Function Calling 不是 LLM 直接执行函数，而是 LLM 输出结构化的函数调用参数，由框架代为执行并将结果返回给 LLM。

---

### Q5：Function Calling 中如何处理多轮工具调用（Multi-turn Tool Use）？★★★★☆

**参考答案：**

复杂任务中，LLM 可能需要连续调用多个工具才能完成任务。

```
用户："帮我查一下北京明天的天气，如果下雨就帮我订一把伞"
   ↓
第1轮：LLM → getWeather("北京", "明天") → {"weather": "rain"}
   ↓
第2轮：LLM → orderUmbrella("北京") → {"orderId": "12345"}
   ↓
最终回答："北京明天有雨，已为您下单雨伞，订单号12345"
```

**Spring AI 处理多轮的方式：**
- Spring AI 框架会自动处理多轮工具调用循环
- 开发者只需注册好函数，框架负责：LLM 调用 → 执行函数 → 结果回传 LLM → 再调用 → ... 直到 LLM 给出最终回答
- 可设置 `maxToolCalls` 防止无限循环

**注意事项：**
- 每个函数应保持单一职责，粒度适中
- 函数描述要清晰，影响 LLM 的选择准确性
- 需要设置超时和最大调用次数兜底

---

## 2.3 记忆（Memory）与对话管理

### Q6：Spring AI 如何实现对话记忆（Chat Memory）？★★★★★

**参考答案：**

LLM 本身是无状态的，每次调用都是独立的。对话记忆通过在每次请求中注入历史消息来实现上下文连贯。

**Spring AI 的记忆实现：**

```java
// 1. 创建 ChatMemory 存储
ChatMemory chatMemory = new InMemoryChatMemory();
// 生产环境用持久化存储：
// ChatMemory chatMemory = new JdbcChatMemory(dataSource);

// 2. 创建 Memory Advisor
MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
    .chatMemoryRetrieveSize(20)  // 最近20条消息
    .build();

// 3. 应用到 ChatClient
String response = ChatClient.create(chatModel)
    .prompt()
    .advisors(memoryAdvisor)
    .user("我之前问的那个问题...")
    .call()
    .content();
```

**记忆类型：**

| 类型 | 实现方式 | 适用场景 |
|------|----------|----------|
| 短期记忆 | 直接拼接历史消息到 Prompt | 当前会话上下文 |
| 长期记忆 | 向量化存储 + 语义检索 | 跨会话知识记忆 |
| 摘要记忆 | LLM 对历史消息做摘要压缩 | 长对话场景 |

**面试追问 - 消息过多时如何处理？**
1. **滑动窗口**：只保留最近 N 条消息
2. **摘要压缩**：定期用 LLM 对旧消息做摘要，替换原始消息
3. **向量化检索**：将历史消息存入向量库，按需检索相关片段

---

### Q7：如何为每个用户维护独立的对话上下文？★★★★☆

**参考答案：**

通过 `conversationId` 区分不同用户/会话的记忆：

```java
@Bean
public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
    return ChatClient.builder(chatModel)
        .defaultAdvisors(
            MessageChatMemoryAdvisor.builder(chatMemory).build()
        )
        .build();
}

// 调用时传入 conversationId
public String chat(String userId, String message) {
    return chatClient.prompt()
        .user(message)
        .advisors(advisor -> advisor.param(
            ChatMemory.CONVERSATION_ID, userId  // 按用户隔离记忆
        ))
        .call()
        .content();
}
```

**生产环境注意事项：**
- `InMemoryChatMemory` 仅适用于开发，重启丢失数据
- 生产环境应使用 `JdbcChatMemory` 或自定义 Redis 持久化实现
- 需要设置 TTL 自动清理过期会话数据

---

## 2.4 Advisor（拦截器机制）

### Q8：什么是 Spring AI 的 Advisor？它有哪些应用场景？★★★★☆

**参考答案：**

**Advisor** 是 Spring AI 的拦截器机制，类似于 Spring MVC 的 Interceptor 或 Servlet 的 Filter，可以在请求发送到 LLM 前和响应返回后进行拦截处理。

**Advisor 执行链：**
```
用户输入 → Advisor1(前处理) → Advisor2(前处理) → LLM 调用 
         → Advisor2(后处理) → Advisor1(后处理) → 返回结果
```

**内置 Advisor：**

| Advisor | 功能 | 应用场景 |
|---------|------|----------|
| MessageChatMemoryAdvisor | 注入对话历史 | 多轮对话记忆 |
| QuestionAnswerAdvisor | 注入 RAG 检索结果 | 知识库问答 |
| PromptChatMemoryAdvisor | 在 system prompt 注入记忆 | 记忆放在 system 消息中 |
| SafeGuardAdvisor | 输入输出安全检查 | 内容安全过滤 |
| ContentMutationAdvisor | 修改消息内容 | 敏感词替换 |

**自定义 Advisor 示例：**
```java
public class LoggingAdvisor implements CallAroundAdvisor {
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        log.info("Request: {}", request.userText());
        AdvisedResponse response = chain.nextAroundCall(request);
        log.info("Response: {}", response.response().getResult().getOutput().getText());
        return response;
    }
    
    @Override
    public int getOrder() { return 0; }  // 执行顺序
}
```

---

## 2.5 结构化输出（Structured Output）

### Q9：如何让 LLM 返回结构化数据（JSON/Java 对象）？★★★★★

**参考答案：**

LLM 默认返回自由文本，但实际业务常需要结构化数据。Spring AI 提供了 `BeanOutputConverter` 直接将 LLM 输出映射为 Java 对象。

```java
// 定义目标类
record ProductInfo(String name, double price, List<String> features) {}

// 方式1：使用 entity() 直接映射
ProductInfo product = ChatClient.create(chatModel)
    .prompt()
    .user("分析这个产品：iPhone 16 Pro")
    .call()
    .entity(ProductInfo.class);

// 方式2：使用 list() 返回列表
List<ProductInfo> products = ChatClient.create(chatModel)
    .prompt()
    .user("列出三款热门手机")
    .call()
    .entity(new ParameterizedTypeReference<List<ProductInfo>>() {});

// 方式3：手动使用 OutputConverter
BeanOutputConverter<ProductInfo> converter = 
    new BeanOutputConverter<>(ProductInfo.class);

String response = ChatClient.create(chatModel)
    .prompt()
    .user("分析这个产品：iPhone 16 Pro. " + converter.getFormat())
    .call()
    .content();

ProductInfo info = converter.convert(response);
```

**底层原理：** `BeanOutputConverter` 会基于 Java 类的字段信息生成 JSON Schema，注入到 Prompt 中指导 LLM 输出格式，然后反序列化。

---

### Q10：LLM 输出的 JSON 格式不稳定怎么处理？★★★★☆

**参考答案：**

**常见问题：** LLM 可能输出多余文本、缺少字段、格式错误。

**解决方案：**

1. **Prompt 强化**：明确要求"只返回 JSON，不要有其他文本"
2. **JSON Mode**：部分模型（如 GPT-4）支持 `response_format: json_object` 强制 JSON 输出
3. **重试机制**：解析失败时重试，附加错误提示
4. **Schema 校验**：使用 `BeanOutputConverter` 自带校验
5. **Structured Output API**：OpenAI 的 Structured Output 保证 100% 符合 Schema

```java
// 重试机制示例
@Retryable(
    value = JsonParseException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
public ProductInfo parseProduct(String userInput) {
    String response = chatClient.prompt()
        .user(userInput + ". 请严格返回JSON格式。")
        .call()
        .content();
    return objectMapper.readValue(response, ProductInfo.class);
}
```

---

## 2.6 多模态支持

### Q11：Spring AI 如何处理图片等多模态输入？★★★☆☆

**参考答案：**

Spring AI 支持多模态（文本 + 图片）输入，用于视觉问答（VQA）场景。

```java
// 图片分析
String response = ChatClient.create(chatModel)
    .prompt()
    .user(u -> u
        .text("这张图片里有什么？")
        .media(MimeTypeUtils.IMAGE_PNG, 
               new UrlResource("https://example.com/product.jpg"))
    )
    .call()
    .content();

// 本地图片
String response = ChatClient.create(chatModel)
    .prompt()
    .user(u -> u
        .text("描述这张图片")
        .media(MimeTypeUtils.IMAGE_JPEG, 
               new FileSystemResource("/path/to/image.jpg"))
    )
    .call()
    .content();
```

**典型应用场景：**
- 商品图片识别与描述
- 图片内容审核
- 文档/票据 OCR + 理解
- 视觉问答（VQA）

---

## 2.7 Spring AI 与 Spring Boot 集成

### Q12：Spring AI 项目的依赖和配置如何组织？★★★★☆

**参考答案：**

**Maven 依赖：**
```xml
<!-- Spring AI BOM（版本管理） -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- OpenAI 集成 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>

<!-- 向量数据库 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
</dependency>
```

**配置文件（application.yml）：**
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-3-small
```

**多环境切换（开发用 Ollama，生产用 OpenAI）：**
```yaml
# application-dev.yml
spring.ai.ollama.chat.model: qwen2.5:7b

# application-prod.yml  
spring.ai.openai.api-key: ${OPENAI_API_KEY}
```

---

## 本章小结

**必背 TOP 5：**
1. ChatModel vs ChatClient 区别与选型
2. Function Calling 原理与实现
3. 对话记忆（ChatMemory）实现多轮对话
4. 结构化输出（BeanOutputConverter）
5. Advisor 拦截器机制与常用 Advisor
