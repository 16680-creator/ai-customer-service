# Spring AI 入门

> 本项目使用 **Spring AI 1.1.4** 集成大语言模型，实现 AI 智能对话。
> 对应项目文件：`ai-cs-chat` 模块、`application.yml` 中的 `spring.ai` 配置

---

## 一、Spring AI 是什么？

```
Spring AI = Spring 生态的 AI 开发框架

它提供统一的 API 来对接各种 LLM：
  • OpenAI（GPT-4）
  • MiniMax（本项目使用）
  • 通义千问、文心一言
  • Ollama（本地模型）

你只需要换配置，不用改代码！
```

---

## 二、本项目的 AI 配置

```yaml
# ai-cs-chat/src/main/resources/application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:你的密钥}
      base-url: ${OPENAI_BASE_URL:https://api.minimaxi.com}  # MiniMax 的 API 地址
      chat:
        options:
          model: ${OPENAI_MODEL:MiniMax-M3}   # 模型名称
          temperature: 0.7                     # 随机性（0=确定，1=创意）
          max-tokens: 2048                     # 最大回复长度
```

**关键理解**：
- Spring AI 用 OpenAI 兼容协议，所以 MiniMax、通义等都能用 `spring-ai-starter-model-openai`
- `base-url` 指向不同厂商的 API 地址即可
- `api-key` 从环境变量读取（安全！不要硬编码）

---

## 三、依赖引入

```xml
<!-- ai-cs-chat/pom.xml -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>

<!-- PDF 文档读取（RAG 用） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pdf-document-reader</artifactId>
</dependency>
```

父 POM 中通过 BOM 管理版本：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.1.4</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

---

## 四、基本对话（ChatClient）

### 4.1 最简单的调用

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @PostMapping("/send")
    public Result<String> chat(@RequestParam String sessionId,
                               @RequestParam String message) {
        ChatClient chatClient = chatClientBuilder.build();
        
        String reply = chatClient.prompt()
            .user(message)                    // 用户消息
            .call()                           // 同步调用
            .content();                       // 获取回复文本

        return Result.success(reply);
    }
}
```

### 4.2 带 System Prompt

```java
String reply = chatClient.prompt()
    .system("""
        你是一个专业的AI客服助手，服务于XX电商平台。
        
        规则：
        1. 回答简洁专业，不超过200字
        2. 不确定的问题引导联系人工客服
        3. 不要编造商品信息
        """)
    .user(message)
    .call()
    .content();
```

### 4.3 流式输出（SSE）

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestParam String message) {
    ChatClient chatClient = chatClientBuilder.build();
    
    return chatClient.prompt()
        .user(message)
        .stream()              // 流式调用
        .content();            // 返回 Flux<String>，逐字输出
}
```

前端接收 SSE：
```javascript
const eventSource = new EventSource(`/api/chat/stream?message=${encodeURIComponent(msg)}`);
eventSource.onmessage = (event) => {
    // event.data 是每次返回的一小段文字
    appendToChat(event.data);
};
```

---

## 五、多轮对话（对话记忆）

### 5.1 手动管理历史

```java
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public Result<String> chat(String sessionId, String message) {
        // 1. 从 Redis 读取历史消息
        List<Message> history = getHistory(sessionId);
        
        // 2. 构建完整 Prompt
        ChatClient chatClient = chatClientBuilder
            .defaultSystem("你是AI客服助手")
            .build();

        String reply = chatClient.prompt()
            .messages(history)     // 历史消息
            .user(message)         // 当前消息
            .call()
            .content();

        // 3. 保存本轮对话到 Redis
        saveHistory(sessionId, message, reply);
        
        return Result.success(reply);
    }

    private List<Message> getHistory(String sessionId) {
        String key = "chat:history:" + sessionId;
        List<String> jsonList = redisTemplate.opsForList().range(key, -20, -1);
        // 反序列化为 Message 对象...
        return messages;
    }

    private void saveHistory(String sessionId, String userMsg, String aiReply) {
        String key = "chat:history:" + sessionId;
        redisTemplate.opsForList().rightPush(key, toJson("user", userMsg));
        redisTemplate.opsForList().rightPush(key, toJson("assistant", aiReply));
        redisTemplate.expire(key, 24, TimeUnit.HOURS);  // 24 小时过期
    }
}
```

### 5.2 Spring AI 内置记忆（ChatMemory）

```java
// 使用 Spring AI 的 MessageChatMemoryAdvisor
ChatClient chatClient = chatClientBuilder
    .defaultAdvisors(
        MessageChatMemoryAdvisor.builder(chatMemory)
            .conversationId(sessionId)
            .build()
    )
    .build();

// 之后每次调用自动带上历史
String reply = chatClient.prompt()
    .user(message)
    .call()
    .content();
```

---

## 六、Function Calling（工具调用）

让 AI 能调用你的 Java 方法：

```java
// 定义工具函数
@Bean
@Description("查询订单状态")  // AI 通过这个描述决定何时调用
public Function<OrderQueryRequest, OrderQueryResponse> queryOrder() {
    return request -> {
        Order order = orderMapper.selectByOrderNo(request.orderNo());
        return new OrderQueryResponse(order.getStatus(), order.getPayTime());
    };
}

// 记录定义
record OrderQueryRequest(String orderNo) {}
record OrderQueryResponse(int status, LocalDateTime payTime) {}

// 对话时启用工具
String reply = chatClient.prompt()
    .user("我的订单 ORD20240101001 到哪了？")
    .functions("queryOrder")   // 注册可用工具
    .call()
    .content();

// AI 会自动：
// 1. 识别需要查订单
// 2. 调用 queryOrder("ORD20240101001")
// 3. 拿到结果后组织自然语言回复
```

---

## 七、Prompt 工程技巧

### 7.1 结构化 Prompt

```java
String prompt = """
    ## 角色
    你是{company}的AI客服。
    
    ## 上下文
    用户ID: {userId}
    用户等级: {level}
    
    ## 知识库参考
    {context}
    
    ## 用户问题
    {question}
    
    ## 回答要求
    - 基于知识库回答，不要编造
    - 如果知识库没有相关信息，说"建议您联系人工客服"
    - 回答不超过 300 字
    """;
```

### 7.2 Temperature 参数

| 值 | 效果 | 适用场景 |
|----|------|---------|
| 0.0 | 完全确定，每次一样 | 数据提取、分类 |
| 0.3 | 较确定 | 客服回答（本项目 0.7） |
| 0.7 | 平衡 | 通用对话 |
| 1.0 | 很有创意 | 文案生成、头脑风暴 |

---

## 八、动手练习

1. 配置好 API Key，启动 `ai-cs-chat`
2. 用 Postman 调用：`POST http://localhost:8083/api/chat/send?sessionId=1&message=你好`
3. 修改 temperature 为 0 和 1，对比回复差异
4. 写一个带 System Prompt 的客服对话
5. 实现流式输出接口，前端用 SSE 接收

---

## 学习检查清单

- [ ] 理解 Spring AI 的统一抽象（ChatClient）
- [ ] 会配置不同 LLM 提供商（换 base-url）
- [ ] 理解 temperature、max-tokens 参数
- [ ] 会实现同步/流式对话
- [ ] 理解多轮对话的记忆管理
- [ ] 了解 Function Calling 的原理
- [ ] 会写结构化的 System Prompt

---

## 下一步

→ [01-RAG检索增强生成](../02-RAG全栈实战/01-RAG检索增强生成.md)
