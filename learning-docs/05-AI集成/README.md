# 05-AI集成

> 企业应用中的 AI 集成完整指南：从对话、RAG 到生产级部署。

---

## 📚 内容导航

### 核心模块

| 模块 | 内容 | 难度 | 适用场景 |
|------|------|------|---------|
| [01-SpringAI框架集成](01-SpringAI框架集成/) | Spring AI 完整实现与最佳实践 | ⭐⭐ | Java 应用集成 LLM |
| [02-RAG全栈实战](02-RAG全栈实战/) | 向量检索、Rerank、引用溯源 | ⭐⭐⭐ | 知识库问答系统 |
| [03-AI编码方法论](03-AI编码方法论/) | Prompt 工程、Agent 编排 | ⭐⭐⭐ | AI 辅助开发 |
| [04-规格驱动工具](04-规格驱动工具/) | SDD/TDD/OpenSpec 流程 | ⭐⭐⭐ | 工程化开发 |
| [05-向量数据库](05-向量数据库/) | Elasticsearch/Milvus/Pinecone | ⭐⭐⭐ | 向量存储与检索 |

### 辅助内容

- [代码注释与知识点解析.md](代码注释与知识点解析.md) — 源码注释规范
- [面试题/](面试题/) — AI 相关面试问题汇总

---

## 🎯 学习路线

### 快速路线（1周）
```
Day 1-2: SpringAI 框架 → ai-cs-chat 源码
Day 3-4: RAG 实战 → ai-cs-knowledge 源码
Day 5:   向量数据库选型 → Elasticsearch 集成
Day 6-7: 部署上线 → 监控优化
```

### 完整路线（3周）
```
Week 1: SpringAI + RAG 基础
Week 2: Agent 编排 + Function Calling
Week 3: 生产部署 + 成本优化 + 评估体系
```

---

## 💡 核心概念

### AI 应用三层架构

```
用户层 (UI / API)
  ↓
编排层 (LangChain / Spring AI)
  ├── 意图识别
  ├── RAG 检索
  ├── Function Calling
  └── 对话管理
  ↓
基础层 (LLM / Embedding / Vector DB)
  ├── OpenAI / DeepSeek
  ├── BGE-M3 / text-embedding-ada-002
  └── Elasticsearch / Milvus
```

### RAG 标准链路

```
离线（写路径）:
文档 → 分块 → Embedding → 向量库

在线（读路径）:
问题 → 向量检索 TopK → Rerank → 上下文拼装 → LLM → 答案 + 引用
```

### Spring AI 核心组件

| 组件 | 作用 | 对标 LangChain |
|------|------|---------------|
| ChatClient | LLM 调用统一接口 | ChatOpenAI |
| VectorStore | 向量存储抽象 | VectorStore |
| EmbeddingClient | Embedding 统一接口 | Embeddings |
| ChatMemory | 对话历史管理 | ConversationBufferMemory |
| Function Calling | 外部工具调用 | Tools / Agent |

---

## 🔧 实战场景

### 场景 1：企业知识库问答

**需求**：基于内部文档回答员工问题

**技术方案**：
```java
// 1. 文档入库（KnowledgeService）
@Service
public class KnowledgeService {
    @Autowired
    private VectorStore vectorStore;
    
    public void ingestDocument(String docId, String content) {
        // 分块
        List<String> chunks = TextSplitter.split(content, 500);
        
        // 向量化并存储
        List<Document> docs = chunks.stream()
            .map(chunk -> new Document(chunk, Map.of("docId", docId)))
            .toList();
        
        vectorStore.add(docs);
    }
}

// 2. 检索与回答（ChatService）
@Service
public class ChatService {
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private VectorStore vectorStore;
    
    public String answer(String question) {
        // 检索相关文档
        List<Document> docs = vectorStore.similaritySearch(question, 5);
        
        // 构造上下文
        String context = docs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));
        
        // 调用 LLM
        String prompt = String.format("""
            基于以下资料回答问题，资料不足时说"我不知道"。
            
            资料：
            %s
            
            问题：%s
            """, context, question);
        
        return chatClient.call(prompt);
    }
}
```

### 场景 2：Function Calling（工具调用）

```java
@Service
public class AgentService {
    
    @Bean
    public ChatClientCustomizer functionCallingCustomizer() {
        return builder -> builder.defaultFunctions(
            "getWeather",  // 查天气
            "searchOrders" // 查订单
        );
    }
    
    @Tool("""
        Get current weather for a city.
        @param city The city name
        @return Weather description
        """)
    public String getWeather(String city) {
        // 调用天气 API
        return weatherApi.get(city);
    }
    
    @Tool("""
        Search orders by user ID.
        @param userId User ID
        @return List of orders
        """)
    public List<Order> searchOrders(int userId) {
        return orderRepo.findByUserId(userId);
    }
}

// 使用
String response = chatClient.call("北京今天天气怎么样？顺便帮我查一下我的订单");
// LLM 自动调用 getWeather("北京") 和 searchOrders(currentUserId)
```

---

## ⚡ 常见问题

### Q1：RAG 检索不准确？
**诊断**：
1. Embedding 模型不匹配（中文用 BGE-M3，不用 text-embedding-ada-002）
2. 分块策略不合理（块太大或太小）
3. TopK 太少（尝试 10-20）
4. 没用 Rerank

**解法**（参考 22-Python速成/21 篇）：
- 调整分块大小（300-800 中文字）
- 加 Rerank（检索 50 条 → Rerank Top 10）
- 监控召回率/准确率

### Q2：LLM 响应太慢？
**诊断**：
```java
long start = System.currentTimeMillis();
String response = chatClient.call(prompt);
long duration = System.currentTimeMillis() - start;
log.info("LLM call took {}ms", duration);
```

**优化**：
- 用流式响应（SSE）给用户更快反馈
- 缓存常见问题答案
- 减少上下文长度
- 选更快的模型（DeepSeek vs GPT-4）

### Q3：成本太高？
**成本构成**：
- LLM 调用（按 token 计费）
- Embedding 调用（量大时成本显著）
- 向量数据库存储

**优化策略**：
```java
// 1. Embedding 缓存
@Cacheable("embeddings")
public List<Float> embed(String text) {
    return embeddingClient.embed(text);
}

// 2. 压缩上下文
String context = docs.stream()
    .map(doc -> doc.getContent().substring(0, 200))  // 只取前 200 字
    .collect(Collectors.joining("\n\n"));

// 3. 选便宜的模型
chatClient.withModel("deepseek-chat");  // ¥0.001/1K tokens
```

---

## 📊 评估指标

| 指标 | 计算 | 目标 |
|------|------|------|
| **准确率** | 正确回答 / 总问题数 | ≥ 85% |
| **召回率** | 检索到相关文档 / 应检索文档数 | ≥ 90% |
| **响应时间** | P99 延迟 | < 3s（流式首字< 500ms） |
| **成本** | 每千次问答的费用 | < ¥10 |

---

## 🔗 与其他模块关联

```
05-AI集成
  ├── → 02-Spring微服务（Spring AI 框架）
  ├── → 04-中间件（Elasticsearch 向量检索）
  ├── → 10-Python服务（ai-cs-py-chat 对标）
  └── → 22-Python速成/21篇（RAG 实战）
```

---

## 📚 推荐资源

### 内部文档
- [ai-cs-chat 源码](../../ai-cs-chat/) — Spring AI 完整实现
- [ai-cs-knowledge 源码](../../ai-cs-knowledge/) — 向量化与检索
- [22-Python速成/21篇](../22-Python速成/21-AI应用实战-从对话到RAG.md)

### 外部资源
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [LangChain 文档](https://python.langchain.com/)
- [OpenAI Function Calling](https://platform.openai.com/docs/guides/function-calling)
- [RAG 论文](https://arxiv.org/abs/2005.11401)

---

## ✅ 实施检查清单

```
基础集成
□ Spring AI 依赖配置
□ LLM 客户端初始化
□ Embedding 客户端初始化
□ 向量数据库连接

RAG 链路
□ 文档分块策略确定
□ Embedding 模型选择（中文 BGE-M3）
□ 向量索引创建
□ 检索 TopK 调优
□ Rerank 集成（可选）
□ 引用溯源实现

生产就绪
□ 超时与重试配置
□ 降级兜底文案
□ 日志记录（model/tokens/耗时）
□ Prometheus 指标
□ 成本监控告警
□ 质量评估体系
```

---

**下一步**：从 01-SpringAI框架集成 开始，跟随 ai-cs-chat 源码学习完整链路。
