# RAG 向量检索实战（Embedding + VectorStore + Advisor）

> 本文档针对本项目 `ai-cs-chat` 模块，讲解"真正的 RAG 向量检索"是如何落地的。
> 与 [01-RAG检索增强生成.md](01-RAG检索增强生成.md) 的理论篇不同，本文是**代码篇**，逐行讲解本项目实现。
>
> **核心目标**：让 `chatWithRag` 名副其实——先做**语义向量检索**，再把命中的文档注入上下文，最后让大模型基于私有知识作答。

---

> **当前基线（2026-11-26）**：Spring AI **2.1.0** / Spring Boot **4.2.0**。本文保留通用原理；涉及项目 API、配置和结果的段落以当前源码及验证记录为准。
> 2.x 迁移注意事项：RAG Advisor 模块改名 + `SearchRequest.builder()` / `TokenTextSplitter` 签名待编译验证；向量库 filter converter 的 `doSingleValue` 变抽象方法（自定义才受影响）。
> 完整破坏性改动清单与迁移路径见 [12-SpringAI-2.0升级评估与迁移指南](../01-SpringAI框架集成/12-SpringAI-2.0升级评估与迁移指南.md)。

## 一、整体架构

```
【离线：构建知识库】
  上传文本/PDF ──► 分块(Chunk) ──► Embedding向量化 ──► 写入 VectorStore
             (TokenTextSplitter)   (EmbeddingModel)       (Chroma/其他)

【在线：RAG 问答】
  用户问题 ──► Embedding向量化 ──► VectorStore 相似度检索(Top-K) ──► 命中片段
                 │                                                      │
                 └──────────────► 组装 RAG Prompt(上下文+问题) ──► LLM 回答
```

对应到本项目，四个核心角色：

| 角色 | 类 / Bean | 说明 |
|------|-----------|------|
| 向量化模型 | `EmbeddingModel`（@Primary `siliconFlowEmbeddingModel`，见 SpringAiConfig） | 硅基流动 BAAI/bge-m3（DeepSeek 不支持 /v1/embeddings） |
| 向量存储 | `ChromaVectorStore`（starter 自动装配） | 当前 Chroma（持久化），曾用内存版 `SimpleVectorStore`，生产可选 ES/Qdrant/Milvus |
| 检索+上下文 | `KnowledgeBaseService` | 分块、入库、语义检索、拼装上下文 |
| RAG 助手 | `SpringAiConfig.ragAdvisor` | `QuestionAnswerAdvisor`，让所有对话自动带检索 |

---

## 二、新增依赖（pom.xml）

```xml
<!-- Spring AI VectorStore 抽象（Document/SearchRequest/SimilaritySearch 接口） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vector-store</artifactId>
</dependency>

<!-- 提供 QuestionAnswerAdvisor（检索 + 上下文注入 + 回答 的 RAG 助手） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vector-store-advisor</artifactId>
</dependency>
```

> 注意：当前项目使用 Spring AI `2.1.0`，`QuestionAnswerAdvisor` 位于
> `org.springframework.ai.chat.client.advisor.vectorstore` 子包。
> 变化的是 Maven 坐标（`spring-ai-advisors-vector-store` → `spring-ai-vector-store-advisor`），不是这个包名。

---

## 三、向量存储配置（VectorStoreConfig）

> 本项目当前使用 **Chroma**，`VectorStoreConfig` 显式创建目标 `ChromaVectorStore`，再提供 `VectorStore` 接口懒加载代理。
> 这是 Spring AI 2.1 生命周期初始化需要访问远端集合时的隔离措施，不是空配置类。

文件：`ai-cs-chat/src/main/java/com/aics/chat/config/VectorStoreConfig.java`

```java
@Bean
@Primary
VectorStore vectorStore(@Lazy ChromaVectorStore chromaVectorStore) {
    // 使用 LazyInitTargetSource 的接口代理；首次 RAG 调用才解析目标。
    // 完整实现见源码，不能只给构造器加 @Lazy 就认为接口注入也被延迟。
}

@Bean
@Lazy
ChromaVectorStore chromaVectorStore(EmbeddingModel embeddingModel,
                                    ChromaApi chromaApi,
                                    ChromaVectorStoreProperties properties,
                                    BatchingStrategy batchingStrategy) {
    return ChromaVectorStore.builder(chromaApi, embeddingModel)
            .collectionName(properties.getCollectionName())
            .databaseName(properties.getDatabaseName())
            .tenantName(properties.getTenantName())
            .initializeSchema(properties.isInitializeSchema())
            .batchingStrategy(batchingStrategy)
            .build();
}
```

依赖（BOM 管理的坐标是 `spring-ai-starter-vector-store-chroma`，注意不是 `...-chroma-store-spring-boot-starter`）：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-chroma</artifactId>
</dependency>
```

连接配置（本项目写在 `ai-cs-chat/src/main/resources/application.yml` 的 `spring.ai.vectorstore.chroma.*`，
可由 Nacos `ai-cs-chat.yml`（源文件 `tools/nacos-config/ai-cs-chat.yml`）覆盖）：
```yaml
spring:
  ai:
    vectorstore:
      chroma:
        initialize-schema: false        # 本地默认不建集合；首次真实调用仍需 Chroma v2 可达
        collection-name: aics-knowledge # 向量集合名
        client:
          host: http://${aics.mysql.host:localhost}   # 复用 MySQL 主机（在 aics-shared.yml 定义），保证同机部署一致
          port: ${CHROMA_PORT:8000}                # Chroma 服务端口
```

> 本地 `application.yml` 已含上述占位符做兜底；改 Chroma 地址优先改 Nacos 的 `ai-cs-chat.yml`
> （源文件 `tools/nacos-config/ai-cs-chat.yml`），无需发布脚本。

### Chroma v2 API 兼容性说明（Spring AI 1.0.0 → 1.1.4）

> 背景：本项目最初的 `spring-ai.version=1.0.0` 无法连接远端 Chroma，原因与解决如下，全部已在 1.1.4 落地。

1. **远端 Chroma 只支持 v2 API**：v1 API 会返回 `410 deprecated`。Spring AI `1.0.0` 的
   `ChromaVectorStore` 走 v1 API，因此无法连接；`1.1.4` 已改为走 v2 API。

### 启动 Chroma（本地）
```bash
# 方式一：Docker
docker run -d --name chroma -p 8000:8000 chromadb/chroma
# 方式二：Python
pip install chromadb && chroma run --host 0.0.0.0 --port 8000
```

### 曾用的内存版（SimpleVectorStore，本地零依赖）
```java
@Bean
public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return SimpleVectorStore.builder(embeddingModel).build();
}
```

### 生产环境切换为 Elasticsearch（保持抽象，改动最小）
同样只需换依赖 + 配置，业务代码（`KnowledgeBaseService`）一行不改：

**① 加依赖**
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-elasticsearch</artifactId>
</dependency>
```

**② 加配置（application.yml）**
```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: your-password
  ai:
    vectorstore:
      elasticsearch:
        initialize-schema: true   # 首次运行自动建索引
```

**③ 换实现**：es starter 同样自动装配 `ElasticsearchVectorStore`，无需手写 Bean。

> 无论内存 / Chroma / ES / Qdrant，都实现同一个 `VectorStore` 接口，这就是"抽象"的价值。

---

## 四、知识库服务（KnowledgeBaseService）—— RAG 核心

文件：`ai-cs-chat/src/main/java/com/aics/chat/service/KnowledgeBaseService.java`

### 1. 文本入库 `addText`

```java
public int addText(String knowledgeBase, String text) {
    // 1. 把原始文本包成一个 Document
    // 2. 打上知识库归属 metadata（knowledgeBase 字段），检索时按它过滤
    // 3. TokenTextSplitter 按 token 数把长文本切成小块，便于精准检索
    // 4. addChunks 内部会调用 EmbeddingModel 把每块转成向量后存储
    List<Document> chunks = new TokenTextSplitter().split(new Document(text));
    return addChunks(knowledgeBase, chunks);
}
```

### 2. 文件入库 `addFile`

```java
public int addFile(String knowledgeBase, MultipartFile file) {
    // MultipartFile → Spring Resource → DocumentLoader 读取
    Resource resource = file.getResource();
    List<Document> documents = isPdf(file)
            ? documentLoader.loadPdf(resource)   // 按页切分
            : documentLoader.loadText(resource); // 整篇一个 Document
    return addChunks(knowledgeBase, documents);
}
```

### 3. 分块入库通用实现 `addChunks`

```java
private int addChunks(String knowledgeBase, List<Document> documents) {
    // 1. 分块：每个 Document 再切分为更小的片段，便于精准检索
    List<Document> chunks = new TokenTextSplitter().apply(documents);

    // 2. 给每个块打上知识库归属的 metadata，检索时按 knowledgeBase 过滤，避免跨库串扰
    chunks.forEach(chunk -> chunk.getMetadata().put("knowledgeBase", knowledgeBase));

    // 3. 写入向量库（内部会调用 EmbeddingModel 把文本转成向量后存储）
    vectorStore.add(chunks);
    log.info("知识库[{}]入库完成, 共{}个分块", knowledgeBase, chunks.size());
    return chunks.size();
}
```

### 4. 语义检索 `search`（在线阶段核心）

```java
public List<Document> search(String knowledgeBase, String query, int topK, double threshold) {
    // 构造检索请求：
    //   - query：会被 EmbeddingModel 向量化后做余弦相似度检索
    //   - topK：返回最相似的几条
    //   - similarityThreshold：相似度阈值，低于它视为"无相关内容"
    //   - filterExpression：只检索当前知识库（metadata 过滤）
    SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(threshold)
            .filterExpression("knowledgeBase == '" + knowledgeBase + "'")
            .build();

    List<Document> results = vectorStore.similaritySearch(searchRequest);
    log.info("知识库[{}]检索完成: 命中{}条", knowledgeBase, results.size());
    return results;
}
```

### 5. 拼装上下文 `buildContext`

```java
public String buildContext(List<Document> docs) {
    if (docs == null || docs.isEmpty()) {
        return "";  // 未命中，返回空串，由调用方提示"暂无资料"
    }
    // 把命中的文档片段拼接成一段可读文本，作为大模型的"参考资料"
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < docs.size(); i++) {
        sb.append("【资料").append(i + 1).append("】");
        sb.append(docs.get(i).getText()).append("\n\n");
    }
    return sb.toString().trim();
}
```

---

## 五、注册 RAG Advisor（SpringAiConfig）

文件：`ai-cs-chat/src/main/java/com/aics/chat/config/SpringAiConfig.java`

```java
/**
 * 注册 RAG 检索增强 Advisor。
 * QuestionAnswerAdvisor 的作用：用户提问时，先到 VectorStore 里做语义检索，
 * 把命中的片段自动注入用户消息，让大模型基于检索到的私有知识作答（减少幻觉）。
 */
@Bean
public QuestionAnswerAdvisor ragAdvisor(VectorStore vectorStore) {
    return QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder()
                    .similarityThreshold(0.3d)  // 相似度低于0.3视为无相关内容
                    .topK(5)                    // 返回最相关5条
                    .build())
            .build();
}

@Bean
public ChatClient chatClient(OpenAiChatModel chatModel, ToolCallbackProvider orderToolCallbackProvider,
                             QuestionAnswerAdvisor ragAdvisor) {
    return ChatClient.builder(chatModel)
            .defaultSystem("...")
            .defaultToolCallbacks(orderToolCallbackProvider)
            .defaultAdvisors(ragAdvisor)   // 默认启用 RAG 检索增强
            .build();
}
```

> `defaultAdvisors(ragAdvisor)` 让所有通过该 `ChatClient` 的对话都自动带上 RAG 检索能力。

---

## 六、当前 RAG 对话编排

文件：`ai-cs-chat/src/main/java/com/aics/chat/service/impl/ChatServiceImpl.java`

当前 `chatWithRag(...)` 已不再是简单的 `Result<String>` 包装，也不直接使用一个全局 ChatClient。它返回 `Result<ChatRagResponseDTO>`，并支持 Hybrid、查询改写、缓存、ACL、引用和弹性路由：

```java
public Result<ChatRagResponseDTO> chatWithRag(
        String sessionId, String message, String knowledgeBase,
        boolean hybrid, boolean rewrite) {
    // 输入 Guardrail；按 knowledgeBase + 用户隔离的缓存查询。
    List<Document> docs = retrieveRagDocs(
            knowledgeBase, message, 5, 0.5, hybrid, rewrite);
    docs = ragAclFilter.filter(knowledgeBase, docs, ChatUserContext.getUserId());
    String context = knowledgeBaseService.buildContext(docs);

    // 实际提示词由 PromptRegistry 的 rag 场景渲染。
    String ragPrompt = promptRegistry.render("rag", Map.of(
            "context", context.isBlank() ? "（未检索到相关资料）" : context,
            "message", message)).text();

    String response = resilientAiService
            .callRagChat(ModelScenario.RAG, ragPrompt).get();
    response = guardOutput(cleanResponse(response));
    List<CitationItemDTO> citations = buildCitations(docs);
    return Result.success(new ChatRagResponseDTO()
            .setContent(response)
            .setCitations(citations));
}
```

以上是核心链路的教学简化，不包含真实源码中的缓存回填、观测、输出拦截和异常转换。检索命中文档需经过 ACL 后才能进入 Prompt 和引用；`Document` 正文使用 `getText()`。

---

## 七、知识库管理接口（KnowledgeBaseController）

文件：`ai-cs-chat/src/main/java/com/aics/chat/controller/KnowledgeBaseController.java`

| 方法 | 接口 | 作用 |
|------|------|------|
| POST | `/rag/knowledge-base/text` | 上传纯文本入库 |
| POST | `/rag/knowledge-base/upload` | 上传 PDF/TXT 文件入库 |
| GET  | `/rag/knowledge-base/search` | 检索测试，查看命中片段 |

---

## 八、本地快速验证（跑通完整链路）

当前默认用 **Chroma**（持久化向量库）+ 硅基流动 **BAAI/bge-m3** Embedding（经 SiliconFlow API），需先启动 Chroma 并配置 Embedding 服务的 API Key：

```bash
# 0. 启动 Chroma（任选其一）
docker run -d --name chroma -p 8000:8000 chromadb/chroma
# 或：pip install chromadb && chroma run --host 0.0.0.0 --port 8000

# 1. 启动服务（编译目标 JDK 17，开发推荐 JDK 21；按本机实际 JAVA_HOME 设置）
# Windows PowerShell 示例：
$env:JAVA_HOME = "C:\\path\\to\\jdk-21"
$env:Path = "$env:JAVA_HOME\\bin;$env:Path"

# 2. 文本入库
curl -X POST "http://localhost:8083/rag/knowledge-base/text" \
     -d "knowledgeBase=product-manual" \
     -d "text=我们支持15天无理由退货，运费由买家承担。"

# 3. 检索测试
curl "http://localhost:8083/rag/knowledge-base/search?knowledgeBase=product-manual&query=退货政策"

# 4. RAG 对话（命中知识库会基于资料回答；未命中会如实说"暂无资料"）
#    POST /chat/rag  body: { "sessionId": "s1", "message": "退货政策是什么?", "knowledgeBase": "product-manual" }
```

> 注：服务端口是 `8083`（见 application.yml）。若不想启动 Chroma，可临时把 `VectorStoreConfig`
> 换回内存版 `SimpleVectorStore`（见上文"曾用的内存版"），零依赖直接跑。

---

## 九、常见问题

**Q1：本项目 Embedding 用的是什么？**
本项目用 **硅基流动 BAAI/bge-m3**（`SpringAiConfig` 中 `@Primary siliconFlowEmbeddingModel`），
因为 DeepSeek 不支持 `/v1/embeddings`，故单独接 SiliconFlow 的 Embedding 服务。生产也可换 OpenAI/通义
等语义更强的模型，只需替换 `EmbeddingModel` Bean（并改 `aics.embedding.model` 配置）。

**Q2：`QuestionAnswerAdvisor` 找不到？**
Spring AI 2.1 当前在 `org.springframework.ai.chat.client.advisor.vectorstore`
子包，使用 `spring-ai-vector-store-advisor` 坐标。若找不到类，先检查依赖模块，不要按旧版本经验随意改 import。

**Q3：检索命中为空？**
多为 `similarityThreshold`（相似度阈值）设置过高，或知识库根本没有相关内容。可调低阈值（如 0.3）再试。

**Q4：Chroma 数据会不会丢？**
Chroma 是持久化服务，数据落盘，应用重启不丢（除非删容器/清目录）。只有此前内存版
`SimpleVectorStore` 才会重启即清空。

**Q5：切换 Chroma 后启动报连接失败？**
多半是 Chroma 没启动，或端口不对。确认 `docker ps` 中 chroma 在 8000 端口运行，或调整
`spring.ai.vectorstore.chroma.client.port` / 环境变量 `CHROMA_PORT`。

**Q6：Chroma 报 410 / “API is deprecated“？**
远端 Chroma 只支持 v2 API；当前项目使用 Spring AI `2.1.0`。历史 1.0.0 的 v1/410 问题属于演进背景。
若仍报错，确认当前 `spring.ai.vectorstore.chroma.client.host/port` 与远端 Chroma 实例一致，并核对 tenant/database/collection。懒加载代理只能推迟连接，不能修复远端协议或网络故障。

---

## 十、总结

| 环节 | Chroma（当前） | 内存版（本地零依赖） | 生产可选 |
|------|---------------|-------------------|---------|
| 存储 | `ChromaVectorStore` | `SimpleVectorStore` | ES / Qdrant / Milvus |
| 向量模型 | 硅基流动 BAAI/bge-m3（SiliconFlow） | 硅基流动 BAAI/bge-m3 | OpenAI/通义 Embedding |
| 依赖 | `spring-ai-starter-vector-store-chroma` | 无 | 对应 store starter |
| 持久化 | 落盘，重启不丢 | 内存，重启即清 | 持久化 |
| 代码改动 | 显式目标 Bean + 懒加载接口代理 | 手写 Bean | 需重新核对坐标、配置、元数据和生命周期 |

当前 `chatWithRag` 已包含真正的语义检索、私有知识增强和引用。`VectorStore` 抽象能降低存储切换成本，但生产换 ES/Qdrant 仍需验证 starter、过滤表达式、向量维度、初始化和检索行为，不能承诺业务代码永远零改动。

### 历史：1.0.0 → 1.1.4 的 Chroma 修复
2. **集合存在判断 bug 已修复**：1.0.0 对集合不存在的错误消息做字符串比对时写的是
   `“does not exists“`，而 Chroma 实际返回 `“does not exist“`，导致集合明明已创建仍误报 404。
   该 bug 在 1.1.4 已修复。
3. **版本与兼容性**：父 POM `spring-ai.version` 已由 `1.0.0` 改为 `1.1.4`；实测
   Spring AI `1.1.4` + Spring Boot `3.2.5` 编译通过。这是当时结果；当前已经升级到 Spring AI 2.1.0 / Boot 4.2.0，不适用旧结论。

---

## 十一、两阶段检索升级（召回 + Rerank）

> 背景：早期 `search()` 是**单次向量检索**直接取 Top-5。向量相似度只能反映"整体语义接近"，
> 容易出现：字面上相关但语义偏差的片段混入、真正精确的片段被挤出前 5。
> 升级后改为 **宽召回（Top-20）→ Rerank 精排（Top-5）** 的两阶段链路，检索精度显著提升。

### 1. 链路变化对比

```
【旧】单次检索
  用户问题 ──► Embedding 向量化 ──► VectorStore 相似度检索(Top-5, 阈值0.5) ──► 直接作为上下文

【新】两阶段检索
  用户问题 ──► Embedding 向量化 ──► 向量宽召回(Top-20, 低阈值0.5) ──► Rerank 精排(Top-5, minScore 0.7)
                                      │                                    │
                                      └── Rerank 服务异常/无 Key 时 ──► 回退相似度排序取 Top-5（降级）
```

**为什么先宽召回？** 检索质量 = 召回率 × 精排准确率。第一阶段把阈值放宽、条数放大
（Top-20），宁可多召回一些候选，把"是否真相关"的判断交给更聪明的 Rerank 模型。

### 2. 核心实现（KnowledgeBaseService.search）

文件：`ai-cs-chat/src/main/java/com/aics/chat/service/KnowledgeBaseService.java`

```java
/** 第一阶段宽召回的 Top-K（配置 aics.rag.recall-top-k） */
@Value("${aics.rag.recall-top-k:20}")
private int recallTopK = 20;

/** 第一阶段宽召回的相似度阈值（配置 aics.rag.recall-threshold） */
@Value("${aics.rag.recall-threshold:0.5}")
private double recallThreshold = 0.5;

/** Rerank 精排服务（可选注入：bean 不存在时返回 null，检索退化为基础相似度排序） */
private final ObjectProvider<RerankService> rerankServiceProvider;

public List<Document> search(String knowledgeBase, String query, int topK, double threshold) {
    // 阶段一：宽召回（Top-20，低阈值，保证召回率）
    List<Document> recallDocs = searchRaw(knowledgeBase, query, recallTopK, recallThreshold);
    if (recallDocs.isEmpty()) {
        return recallDocs;
    }

    // 阶段二：Rerank 精排（可选，bean 不存在或调用失败时退化）
    RerankService rerankService = rerankServiceProvider.getIfAvailable();
    if (rerankService != null) {
        try {
            List<RerankResultItem> reranked = rerankService.rerank(query, recallDocs, topK).block();
            if (reranked != null && !reranked.isEmpty()) {
                // 按 Rerank 返回的 index 回取原文档，即得到精排后的 Top-N
                List<Document> result = new ArrayList<>(reranked.size());
                for (RerankResultItem item : reranked) {
                    int idx = item.getIndex();
                    if (idx >= 0 && idx < recallDocs.size()) {
                        result.add(recallDocs.get(idx));
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("知识库[{}]Rerank调用异常，降级为相似度排序: {}", knowledgeBase, e.getMessage());
        }
    }

    // 退化路径：向量结果已按相似度降序，直接取 Top-N
    List<Document> fallback = recallDocs.size() <= topK ? recallDocs : recallDocs.subList(0, topK);
    return fallback;
}
```

> 使用 `ObjectProvider<RerankService>` 注入：Rerank 是可选的增强组件，
> 即使服务没起 / Bean 不存在，检索链路也能正常运行（降级为相似度排序），不破坏主流程。

### 3. 宽召回细节（searchRaw）

```java
private List<Document> searchRaw(String knowledgeBase, String query, int topK, double threshold) {
    // bge-m3 检索建议：查询文本加指令前缀，提升语义检索质量
    String retrievalQuery = "为这个句子生成表示以用于检索相关文章：" + query;
    SearchRequest searchRequest = SearchRequest.builder()
            .query(retrievalQuery)
            .topK(topK)
            .similarityThreshold(threshold)
            .filterExpression("knowledgeBase == '" + knowledgeBase + "'") // 只检索当前知识库
            .build();
    return vectorStore.similaritySearch(searchRequest);
}
```

> 技巧：本项目 Embedding 使用 **bge-m3**，其官方检索建议给查询加指令前缀
> `"为这个句子生成表示以用于检索相关文章："`（英文模型对应 `"Represent this sentence for searching relevant passages:"`），
> 能显著提升查询向量质量。

### 4. 配置说明（Nacos ai-cs-chat.yml）

```yaml
aics:
  # RAG 两阶段检索配置
  rag:
    recall-top-k: 20        # 第一阶段宽召回条数（越大召回率越高，但 Rerank 开销越大）
    recall-threshold: 0.5   # 宽召回相似度阈值（低于它直接丢弃）
```

调参建议：
- 知识库文档量大 / 分块细 → `recall-top-k` 可加到 30~50，保证候选覆盖；
- 知识库小 / 主题集中 → 20 足够，减少 Rerank 的 API 开销；
- `recall-threshold` 不建议设太高（0.5 左右），精排阶段由 Rerank 的 `min-score` 把关。

### 5. Rerank 详细内容

Rerank 原理、硅基流动 `bge-reranker-v2-m3` API 调用、`SiliconFlowRerankService` 完整代码、
降级策略与全部配置项，详见：[04-RAG进阶实战-Rerank重排序.md](04-RAG进阶实战-Rerank重排序.md)。

### 6. 验证方法

```bash
# 1. 确保 Nacos 中已配置 aics.rag.* 与 aics.rerank.*（见 14 号文档）
# 2. 启动服务后，检索测试接口返回的条数与顺序应体现 Rerank 效果：
curl "http://localhost:8083/rag/knowledge-base/search?knowledgeBase=product-manual&query=退货政策"

# 3. 观察日志中的两阶段链路：
#    知识库[product-manual]宽召回完成: 命中20条
#    知识库[product-manual]Rerank精排完成: 召回20条 -> 返回5条

# 4. 降级验证：临时把 aics.rerank.api-key 置空后重启，
#    日志出现 "Rerank降级: apiKey为空或无待重排文档"，检索仍正常返回（相似度排序）。
```
