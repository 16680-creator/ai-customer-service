# RAG 检索增强生成

> 本项目 `ai-cs-chat` 模块实现了 RAG（Retrieval-Augmented Generation），让 AI 基于知识库回答。
> 对应项目文件：`ai-cs-chat/src/main/java/com/aics/chat/rag/DocumentLoader.java`

---

## 一、什么是 RAG？

### 问题：AI 会"幻觉"

```
用户问：你们的退货政策是什么？
AI（没有 RAG）：7天无理由退货，运费我们承担。（编造的！）
AI（有 RAG）：根据知识库，我们支持15天无理由退货，运费由买家承担。（基于真实文档）
```

### RAG 流程

```
┌─────────────────────────────────────────────────────────────┐
│                    RAG 完整流程                               │
│                                                              │
│  【离线阶段：构建知识库】                                      │
│  PDF/TXT → 分块(Chunk) → 向量化(Embedding) → 存入向量数据库   │
│                                                              │
│  【在线阶段：回答问题】                                        │
│  用户提问 → 向量化 → 向量检索(Top-K) → 取出相关文档片段        │
│           → 组装 Prompt(问题+上下文) → LLM 生成回答           │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、文档加载（本项目已实现）

```java
// ai-cs-chat/src/main/java/com/aics/chat/rag/DocumentLoader.java
@Component
public class DocumentLoader {

    /**
     * 加载 PDF 文档（按页分割）
     */
    public List<Document> loadPdf(Resource resource) {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
            resource,
            PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageBottomMargin(0)
                .withPagesPerDocument(1)   // 每页一个 Document
                .build()
        );
        return pdfReader.get();
    }

    /**
     * 加载纯文本文档
     */
    public List<Document> loadText(Resource resource) {
        TextReader textReader = new TextReader(resource);
        return textReader.get();
    }
}
```

### Spring AI 支持的文档格式

| 格式 | Reader 类 | 依赖 |
|------|-----------|------|
| PDF | PagePdfDocumentReader | spring-ai-pdf-document-reader |
| TXT | TextReader | spring-ai-core（内置） |
| HTML | JsoupDocumentReader | spring-ai-jsoup-document-reader |
| Word | — | 需自行解析 |
| Markdown | — | 可用 TextReader |

---

## 三、文档分块（Chunking）

### 为什么要分块？

```
一个 PDF 有 50 页 → 50000 字
LLM 上下文窗口有限（如 8K tokens）
→ 不能把整个文档塞进 Prompt
→ 需要切成小块，只检索最相关的几块
```

### 分块策略

```java
// 使用 Spring AI 的 TokenTextSplitter
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

TokenTextSplitter splitter = new TokenTextSplitter(
    800,    // 每块最大 token 数
    350,    // 最小块大小
    5,      // 最小块大小（字符）
    10000,  // 最大块数
    true    // 保留段落边界
);

List<Document> chunks = splitter.apply(documents);
// 50 页 PDF → 可能切成 100+ 个 chunks
```

### 分块策略对比

| 策略 | 优点 | 缺点 | 适用 |
|------|------|------|------|
| 固定大小 | 简单 | 可能切断语义 | 通用 |
| 按段落 | 语义完整 | 块大小不均 | 文章 |
| 按页 | 简单（本项目 PDF） | 一页可能太长 | PDF |
| 递归分割 | 兼顾语义和大小 | 配置复杂 | 推荐 |

---

## 四、向量化（Embedding）

### 原理

```
文本 → Embedding 模型 → 高维向量（如 1536 维浮点数组）

"蓝牙耳机" → [0.12, -0.34, 0.56, ..., 0.78]  (1536个数)
"无线耳机" → [0.11, -0.33, 0.55, ..., 0.77]  (很接近！)
"今天天气" → [0.89, 0.22, -0.44, ..., 0.01]  (差很远)
```

### 使用 Spring AI 的 EmbeddingModel

```java
@Autowired
private EmbeddingModel embeddingModel;

// 将文本转为向量
float[] vector = embeddingModel.embed("无线蓝牙耳机");

// 批量向量化
List<float[]> vectors = embeddingModel.embed(chunks.stream()
    .map(Document::getContent)
    .toList());
```

---

## 五、向量存储与检索

### 5.1 VectorStore 接口

```java
// Spring AI 提供统一的 VectorStore 抽象
public interface VectorStore {
    void add(List<Document> documents);           // 存入
    List<Document> similaritySearch(String query); // 检索
    List<Document> similaritySearch(SearchRequest request); // 高级检索
}
```

### 5.2 可选实现

| VectorStore | 说明 | 适用场景 |
|-------------|------|---------|
| SimpleVectorStore | 内存存储 | 开发测试 |
| ElasticsearchVectorStore | 用 ES 8.x 的 KNN | 本项目（已有 ES） |
| PgVectorStore | PostgreSQL 扩展 | 中小规模 |
| MilvusVectorStore | 专业向量数据库 | 大规模生产 |
| RedisVectorStore | Redis Stack | 低延迟 |

### 5.3 使用示例

```java
@Service
public class KnowledgeService {

    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private DocumentLoader documentLoader;

    /**
     * 导入知识库文档
     */
    public void importDocument(Resource file) {
        // 1. 加载文档
        List<Document> documents = documentLoader.loadPdf(file);
        
        // 2. 分块
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);
        
        // 3. 向量化 + 存储（VectorStore 内部自动调用 EmbeddingModel）
        vectorStore.add(chunks);
    }

    /**
     * 检索相关文档
     */
    public List<Document> search(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(topK)              // 返回最相似的 K 个
                .similarityThreshold(0.7) // 相似度阈值
                .build()
        );
    }
}
```

---

## 六、RAG 对话（完整流程）

```java
@Service
public class RagChatService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;
    @Autowired
    private VectorStore vectorStore;

    public String chatWithRag(String sessionId, String question, String knowledgeBase) {
        // 1. 检索相关文档
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(question)
                .topK(5)
                .filterExpression("kb == '" + knowledgeBase + "'")
                .build()
        );

        // 2. 组装上下文
        String context = relevantDocs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n---\n\n"));

        // 3. 构建 Prompt
        String systemPrompt = """
            你是AI客服助手。请基于以下知识库内容回答用户问题。
            
            ## 知识库内容
            %s
            
            ## 回答规则
            - 只基于上述内容回答
            - 如果内容中没有相关信息，回答"抱歉，我没有找到相关信息，建议联系人工客服"
            - 引用具体内容时标注来源
            """.formatted(context);

        // 4. 调用 LLM
        return chatClientBuilder.build()
            .prompt()
            .system(systemPrompt)
            .user(question)
            .call()
            .content();
    }
}
```

### 使用 Spring AI 的 Advisor（更优雅）

```java
// Spring AI 内置了 RAG Advisor
ChatClient chatClient = chatClientBuilder
    .defaultAdvisors(
        QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder().topK(5).build())
            .build()
    )
    .build();

// 调用时自动：检索 → 注入上下文 → 生成回答
String reply = chatClient.prompt()
    .user(question)
    .call()
    .content();
```

---

## 七、RAG 优化技巧

| 问题 | 优化方案 |
|------|---------|
| 检索不准 | 调整分块大小、使用混合检索（向量+关键词） |
| 上下文太长 | 减少 topK、压缩文档 |
| 回答幻觉 | 强化 Prompt 约束、降低 temperature |
| 多语言 | 使用多语言 Embedding 模型 |
| 更新延迟 | 增量索引、定时同步 |

---

## 八、动手练习

1. 准备一个 PDF（如产品手册），用 DocumentLoader 加载
2. 分块后打印每个 chunk 的内容和长度
3. 用 SimpleVectorStore 存入，执行相似度搜索
4. 实现完整的 RAG 对话接口
5. 对比有/无 RAG 时 AI 回答的准确性

---

## 学习检查清单

- [ ] 理解 RAG 的完整流程（加载→分块→向量化→检索→生成）
- [ ] 理解 Embedding 的原理（文本→向量→相似度）
- [ ] 会使用 DocumentLoader 加载不同格式文档
- [ ] 理解分块策略的选择
- [ ] 会使用 VectorStore 存取向量
- [ ] 能实现完整的 RAG 对话
- [ ] 知道 RAG 的常见优化方向

---

## 下一步

→ [03-LLM工程化实践](./03-LLM工程化实践.md)
