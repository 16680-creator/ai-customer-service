# RAG 向量检索实战（Embedding + VectorStore + Advisor）

> 本文档针对本项目 `ai-cs-chat` 模块，讲解"真正的 RAG 向量检索"是如何落地的。
> 与 [02-RAG检索增强生成.md](02-RAG检索增强生成.md) 的理论篇不同，本文是**代码篇**，逐行讲解本项目实现。
>
> **核心目标**：让 `chatWithRag` 名副其实——先做**语义向量检索**，再把命中的文档注入上下文，最后让大模型基于私有知识作答。

---

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
| 向量化模型 | `EmbeddingModel`（SPRING_AI 自动装配） | 本地用 HashEmbedding，生产可换 OpenAI |
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
    <artifactId>spring-ai-advisors-vector-store</artifactId>
</dependency>
```

> 注意：本项目使用 Spring AI `1.1.4`，`QuestionAnswerAdvisor` 位于
> `org.springframework.ai.chat.client.advisor.vectorstore` 子包（1.0.0 与 1.1.x 包名一致，未变化），
> 不是 `...advisor` 根包。

---

## 三、向量存储配置（VectorStoreConfig）

> 本项目当前已切换为 **Chroma**（持久化向量库），由 starter 自动装配，无需手动定义 Bean。

文件：`ai-cs-chat/src/main/java/com/aics/chat/config/VectorStoreConfig.java`

```java
// 本类仅是"切换说明"文档类，不声明 @Bean。
// spring-ai-starter-vector-store-chroma 已依据 application.yml 的
//   spring.ai.vectorstore.chroma.* 自动装配 ChromaVectorStore。
@Configuration
public class VectorStoreConfig {
    // 无需手动创建 Bean
}
```

依赖（BOM 管理的坐标是 `spring-ai-starter-vector-store-chroma`，注意不是 `...-chroma-store-spring-boot-starter`）：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-chroma</artifactId>
</dependency>
```

连接配置（**统一收口到 Nacos**，`dataId: ai-cs-chat.yml`，对应源文件 `deploy/nacos/configs/ai-cs-chat.yml`）：
```yaml
spring:
  ai:
    vectorstore:
      chroma:
        initialize-schema: true         # 首次启动自动建集合
        collection-name: aics-knowledge # 向量集合名
        tenant-name: default_tenant     # Chroma v2 tenant（Spring AI 1.1.x 新增，须与远端 Chroma 一致）
        database-name: default          # Chroma v2 database（Spring AI 1.1.x 新增）
        client:
          host: ${aics.mysql.host:localhost}   # 复用 MySQL 主机（在 aics-shared.yml 定义），保证同机部署一致
          port: ${CHROMA_PORT:8000}            # Chroma 服务端口
```

> 本地 `application.yml` 只保留同样的占位符做兜底。改 Chroma 地址只需改 Nacos 一处，
> 发布脚本：`powershell -ExecutionPolicy Bypass -File deploy/nacos/publish-to-nacos.ps1`。

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
    // 4. vectorStore.add(chunks) 内部会调用 EmbeddingModel 把每块转成向量后存储
    return addChunks(knowledgeBase, List.of(new Document(text)));
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
        sb.append("【片段").append(i + 1).append("】\n");
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
                    .similarityThreshold(0.5d)  // 相似度低于0.5视为无相关内容
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

## 六、改造 chatWithRag（真正向量检索）

文件：`ai-cs-chat/src/main/java/com/aics/chat/service/impl/ChatServiceImpl.java`

```java
@Override
public Result<String> chatWithRag(String sessionId, String message, String knowledgeBase) {
    log.info("RAG对话请求: sessionId={}, knowledgeBase={}", sessionId, knowledgeBase);

    try {
        // ===== 真正的 RAG 检索增强生成 =====
        // 1. 语义检索：在指定知识库中，用向量相似度找出与问题最相关的 Top-5 片段
        List<Document> docs = knowledgeBaseService.search(knowledgeBase, message, 5, 0.5);

        // 2. 把命中的片段拼装成上下文文本
        String context = knowledgeBaseService.buildContext(docs);

        // 3. 将上下文注入 Prompt：让大模型"基于检索到的私有知识"作答，而不是凭空编造
        String ragPrompt = """
                请严格基于下面的【知识库资料】回答用户问题。
                重要规则：
                1. 如果资料中没有相关信息，请如实告知："我暂时没有这方面的资料"，不要编造内容
                2. 回答时优先引用资料中的内容，不要提及"根据资料/检索结果"之类的表述

                【知识库资料】
                %s

                【用户问题】
                %s
                """.formatted(context.isBlank() ? "（未检索到相关资料）" : context, message);

        String response = chatClient.prompt()
                .user(ragPrompt)
                .call()
                .content();

        response = cleanResponse(response);   // 过滤思考过程
        log.info("RAG对话完成: sessionId={}, 检索命中{}条", sessionId, docs.size());
        return Result.success(response);
    } catch (Exception e) {
        log.error("RAG对话异常: sessionId={}", sessionId, e);
        throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + e.getMessage());
    }
}
```

**关键点**：`search` 拿到的是**真实命中的文档片段**，而不是简单地把知识库名塞进 Prompt。这就是"真正的 RAG"。

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

当前默认用 **Chroma**（持久化向量库）+ `HashEmbeddingModel`（本地方言），不依赖外部 Embedding 服务，但**需先启动 Chroma**：

```bash
# 0. 启动 Chroma（任选其一）
docker run -d --name chroma -p 8000:8000 chromadb/chroma
# 或：pip install chromadb && chroma run --host 0.0.0.0 --port 8000

# 1. 启动服务（需 JDK 17+，本机默认是 JDK8，请用 JDK21 启动）
set JAVA_HOME=D:\Tools\IT\enviroment\jdk\jdk-21.0.11+10
set Path=%JAVA_HOME%\bin;%Path%

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

**Q1：为什么本地用 HashEmbeddingModel？**
生产应该用语义更强的模型（OpenAI/通义等），但本地图"零依赖、秒启动"，用 `HashEmbeddingModel`
（词袋哈希向量，中文也能用，只是语义精度一般）。切换成 OpenAI 只需在配置里替换 `EmbeddingModel` Bean。

**Q2：`QuestionAnswerAdvisor` 找不到？**
Spring AI 版本差异导致包名不同。本项目 `1.1.4`（与 `1.0.0` 一致）在 `...advisor.vectorstore`
子包，更旧版本在 `...advisor` 根包。换其他版本时注意核对 import 路径。

**Q3：检索命中为空？**
多为 `similarityThreshold`（相似度阈值）设置过高，或知识库根本没有相关内容。可调低阈值（如 0.3）再试。

**Q4：Chroma 数据会不会丢？**
Chroma 是持久化服务，数据落盘，应用重启不丢（除非删容器/清目录）。只有此前内存版
`SimpleVectorStore` 才会重启即清空。

**Q5：切换 Chroma 后启动报连接失败？**
多半是 Chroma 没启动，或端口不对。确认 `docker ps` 中 chroma 在 8000 端口运行，或调整
`spring.ai.vectorstore.chroma.client.port` / 环境变量 `CHROMA_PORT`。

**Q6：Chroma 报 410 / “API is deprecated“？**
远端 Chroma 只支持 v2 API。Spring AI `1.0.0` 走 v1 API 会报 410，本项目已升级到 `1.1.4`（支持 v2）。
若仍报错，确认 `spring.ai.vectorstore.chroma` 的 `tenant-name` / `database-name` 与远端实例一致。

---

## 十、总结

| 环节 | Chroma（当前） | 内存版（本地零依赖） | 生产可选 |
|------|---------------|-------------------|---------|
| 存储 | `ChromaVectorStore` | `SimpleVectorStore` | ES / Qdrant / Milvus |
| 向量模型 | `HashEmbeddingModel` | `HashEmbeddingModel` | OpenAI/通义 Embedding |
| 依赖 | `spring-ai-starter-vector-store-chroma` | 无 | 对应 store starter |
| 持久化 | 落盘，重启不丢 | 内存，重启即清 | 持久化 |
| 代码改动 | 自动装配，无需改 | 手写 Bean | 仅换依赖+配置 |

至此，`chatWithRag` 从"名称占位"变为**真正的语义向量检索 + 私有知识增强**，且通过 `VectorStore`
抽象，本地用 Chroma、生产换 ES/Qdrant 都只需改依赖和配置，业务代码零改动。
2. **集合存在判断 bug 已修复**：1.0.0 对集合不存在的错误消息做字符串比对时写的是
   `“does not exists“`，而 Chroma 实际返回 `“does not exist“`，导致集合明明已创建仍误报 404。
   该 bug 在 1.1.4 已修复。
3. **新增 `tenant-name` / `database-name` 配置**：对应 Chroma v2 的 tenant/database 概念，
   必须与远端 Chroma 实例一致（默认 `default_tenant` / `default`）。本项目的 Nacos 配置与本地
   兜底 `application.yml` 均已补充这两个字段。
4. **版本与兼容性**：父 POM `spring-ai.version` 已由 `1.0.0` 改为 `1.1.4`；实测
   Spring AI `1.1.4` + Spring Boot `3.2.5` 编译通过，无需升级 Boot。
