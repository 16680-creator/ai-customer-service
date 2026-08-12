# Spring AI 入门

> 本项目使用 **Spring AI 1.1.4** 集成大语言模型，实现 AI 智能对话。
> 对应项目文件：`ai-cs-chat` 模块、`application.yml` 中的 `spring.ai` 配置

---

## 一、Spring AI 是什么？

```
Spring AI = Spring 生态的 AI 开发框架

它提供统一的 API 来对接各种 LLM：
  • OpenAI（GPT-4）
  • DeepSeek（本项目使用）
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
      base-url: ${OPENAI_BASE_URL:https://api.deepseek.com}  # DeepSeek 的 API 地址
      chat:
        options:
          model: ${OPENAI_MODEL:deepseek-chat}   # 模型名称
          temperature: 0.7                     # 随机性（0=确定，1=创意）
          max-tokens: 2048                     # 最大回复长度
```

**关键理解**：
- Spring AI 用 OpenAI 兼容协议，所以 DeepSeek、通义等都能用 `spring-ai-starter-model-openai`
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
@RequestMapping("/chat")
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
@PostMapping(value = "/stream/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter chatStreamSse(@RequestParam String message) {
    SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);
    // 订阅 chatClient.prompt().user(message).stream().content() 返回的 Flux<String>
    // 逐段通过 emitter.send(...) 推送给前端；超时由 SSE_EMITTER_TIMEOUT（5 分钟）控制
    // （见 ChatController：超时 onTimeout 兜底，避免 SSE 连接挂起）
    return emitter;
}
```

前端接收 SSE：
```javascript
const eventSource = new EventSource(`/chat/stream/sse?message=${encodeURIComponent(msg)}`);
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

### 6.1 实战：AI 智能问数（NL2SQL）

> 本平台已实现：用户用自然语言提问，AI 自动组装 SQL → 查询业务库 → 返回结果。
> 无需任何额外操作，直接在对话页提问即可。对应代码：`ai-cs-chat/src/main/java/com/aics/chat/nl2sql/`

#### 效果示例

```
用户：帮我统计各订单状态的订单数量和支付金额汇总，并且列出销量最高的商品

AI 自动执行：
  1. executeReadOnlyQuery("order", "SELECT status, COUNT(*) cnt, SUM(pay_amount) ... FROM orders GROUP BY status")
  2. executeReadOnlyQuery("product", "SELECT name, sales FROM product ORDER BY sales DESC LIMIT 5")

AI 回复：
  | 订单状态 | 订单数量 | 支付金额汇总 |
  |---------|---------|------------|
  | 已取消   | 9 笔    | ¥2,736.00  |
  | 已支付   | 7 笔    | ¥1,596.00  |
  | 已退款   | 3 笔    | ¥2,239.00  |
```

#### 整体链路

```
用户提问（自然语言）
   │
   ▼
ChatClient（携带工具定义 + 数据库 schema 系统提示词）
   │  ① 模型判断需要查库
   ▼
executeReadOnlyQuery(database, sql)   ← @Tool 工具
   │  ② SQL 白名单校验（只读 SELECT）
   ▼
按库选择只读 JdbcTemplate（HikariCP 只读连接池）
   │  ③ 强制执行 LIMIT 100 + 查询超时 10s
   ▼
返回查询结果 JSON
   │  ④ 模型基于结果组织自然语言回复
   ▼
用户看到带表格/关键数字的回答
```

#### 核心实现

**① 工具服务 `Nl2SqlQueryService`（@Tool 入口）**

```java
@Service
public class Nl2SqlQueryService {

    private final Map<String, JdbcTemplate> jdbcTemplates; // 库标识 -> 只读连接池
    private final ObjectMapper objectMapper;

    @Tool(description = "执行只读SQL查询（仅支持SELECT），根据用户问题从指定业务库查询数据，返回查询结果。"
            + "database可选值：user/product/order/chat/knowledge")
    public String executeReadOnlyQuery(
            @ToolParam(description = "数据库标识") String database,
            @ToolParam(description = "只读SELECT查询语句") String sql) {

        JdbcTemplate jdbc = jdbcTemplates.get(database.trim().toLowerCase());
        if (jdbc == null) return "无效的数据库标识: " + database;

        String error = validateSql(sql);      // ② SQL 安全校验
        if (error != null) return "SQL 校验不通过：" + error;

        String finalSql = enforceLimit(sql);  // ③ 强制 LIMIT 100
        List<Map<String, Object>> rows = jdbc.query(finalSql, ps -> {
            ps.setQueryTimeout(10);           // 查询超时 10s
        }, (rs, rowNum) -> { /* ResultSetMetaData 转 Map */ });

        return "查询成功，共 " + rows.size() + " 条结果：\n"
                + objectMapper.writeValueAsString(rows);
    }
}
```

**② SQL 安全校验（只读 + 白名单）**

```java
private String validateSql(String sql) {
    if (sql == null || sql.isBlank()) return "SQL 不能为空";
    String s = sql.trim();
    if (COMMENTS.matcher(s).find())                 return "不允许包含 SQL 注释";   // -- # /* */
    if (s.replaceAll(";$", "").contains(";"))       return "不允许一次执行多条 SQL"; // 多语句
    if (!s.toUpperCase().startsWith("SELECT"))       return "仅允许 SELECT 查询语句";
    if (DANGEROUS_KEYWORDS.matcher(s).find())        return "检测到写操作或危险关键字";
    if (SYSTEM_SCHEMA.matcher(s).find())             return "不允许访问系统库";      // information_schema 等
    if (FUNC_ABUSE.matcher(s).find())                return "不允许使用危险函数";    // sleep/benchmark 等
    return null;
}
```

> 关键细节：危险关键字正则使用 `\b`（单词边界），避免误伤 `update_time`、`deleted` 等合法列名；
> `LIMIT` 上限通过 `enforceLimit()` 强制追加或改写为 `LIMIT 100`，防止 AI 拖库。

**③ 只读多数据源 `Nl2SqlDataSourceConfig`**

```java
@Configuration
@EnableConfigurationProperties(Nl2SqlProperties.class)
public class Nl2SqlDataSourceConfig {

    @Bean
    public Map<String, JdbcTemplate> nl2SqlJdbcTemplates(Nl2SqlProperties properties) {
        Map<String, JdbcTemplate> templates = new LinkedHashMap<>();
        properties.getUrls().forEach((key, url) -> {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(url + "&readOnly=true"); // 连接串追加只读提示
            ds.setUsername(properties.getUsername());
            ds.setPassword(properties.getPassword());
            ds.setMaximumPoolSize(3);
            templates.put(key, new JdbcTemplate(ds));
        });
        return templates;
    }
}
```

**④ 注册工具到 ChatClient（合并到已有工具）**

```java
@Bean
public ToolCallbackProvider toolCallbackProvider(OrderQueryService orderQueryService,
                                                 Nl2SqlQueryService nl2SqlQueryService) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(orderQueryService, nl2SqlQueryService) // 订单查询 + 智能问数
            .build();
}
```

**⑤ 数据源配置（Nacos `ai-cs-chat.yml`）**

```yaml
aics:
  nl2sql:
    username: root
    password: ${DB_PASSWORD}
    urls:
      user:      jdbc:mysql://123.60.31.79:3306/user_db?...&readOnly=true
      product:   jdbc:mysql://123.60.31.79:3306/product_db?...&readOnly=true
      order:     jdbc:mysql://123.60.31.79:3306/ai_customer_service?...&readOnly=true
      chat:      jdbc:mysql://123.60.31.79:3306/chat_db?...&readOnly=true
      knowledge: jdbc:mysql://123.60.31.79:3306/knowledge_db?...&readOnly=true
```

**⑥ 数据库 schema 注入系统提示词**

把各库的表名/列名精简版拼到 `defaultSystem`，AI 组装 SQL 时才不会乱猜列名：

```
数据库表结构参考（编写 SQL 时必须使用真实表名/列名）：
【order 订单支付库】
orders(id, order_no, user_id, total_amount, discount_amount, pay_amount, ..., status, pay_time, create_time)
order_item(id, order_id, order_no, product_id, product_name, product_price, quantity, subtotal)
coupon(id, user_id, coupon_name, amount, min_order_amount, status, expire_time, ...)
...
```

#### 踩坑记录

| 问题 | 现象 | 解决 |
|------|------|------|
| 引入 `spring-boot-starter-jdbc` 后启动失败 | `Failed to configure a DataSource` | chat 无业务主库，在 `application.yml` 排除 `DataSourceAutoConfiguration`，数据源由 `Nl2SqlDataSourceConfig` 手动创建 |
| AI 乱猜列名导致 SQL 报错 | 用不存在的列 | 系统提示词注入真实 schema，列名/枚举（如 status=PENDING_PAY）给全 |
| 一次问"订单统计+商品销量" | 只查了第一个 | 模型多轮工具调用（先查 order 再查 product），工具定义描述写清楚用途即可 |

#### 前端调用

复用现有 SSE 对话接口即可，无需前端改动：

```bash
curl -X POST -H "Authorization: Bearer <token>" -H "Accept: text/event-stream" \
  "http://localhost:8080/api/chat/stream/sse?sessionId=test&message=统计一下已支付订单的总金额&knowledgeBase="
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
2. 用 Postman 调用：`POST http://localhost:8083/chat/send?sessionId=1&message=你好`
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
