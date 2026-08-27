# RAG 全栈实战 —— 知识点汇总

> 本文将本目录下 9 篇文档按知识点重新组织，作为快速复习与查阅的总纲。
> 原文索引：[01-理论](01-RAG检索增强生成.md) · [02-开发总纲](02-RAG检索增强开发.md) · [03-向量检索](03-RAG向量检索实战.md) · [04-Rerank](04-RAG进阶实战-Rerank重排序.md) · [05-混合检索](05-混合检索实战-ES-BM25与向量RRF融合.md) · [06-引用溯源](06-引用溯源实战-带出处回答.md) · [07-Tika多格式](07-文档格式扩展实战-Tika多格式解析.md) · [08-RocketMQ增量同步](08-知识库增量同步实战-RocketMQ驱动向量化.md) · [09-接口测试报告](09-ai-cs-knowledge-search接口测试报告.md)

---

## 目录

1. [RAG 基础理论](#一rag-基础理论)
2. [整体架构：三服务读写分离](#二整体架构三服务读写分离)
3. [知识入库链路：加载 → 分块 → 向量化 → 存储](#三知识入库链路加载--分块--向量化--存储)
4. [向量检索与 VectorStore 抽象](#四向量检索与-vectorstore-抽象)
5. [检索优化之一：两阶段检索（宽召回 + Rerank 精排）](#五检索优化之一两阶段检索宽召回--rerank-精排)
6. [检索优化之二：混合检索（ES BM25 + 向量 RRF 融合）](#六检索优化之二混合检索es-bm25--向量rrf-融合)
7. [引用溯源：带出处回答](#七引用溯源带出处回答)
8. [文档格式扩展：Tika 多格式解析](#八文档格式扩展tika-多格式解析)
9. [知识库增量同步：RocketMQ 驱动向量化](#九知识库增量同步rocketmq-驱动向量化)
10. [配置项汇总](#十配置项汇总)
11. [测试验证与踩坑记录](#十一测试验证与踩坑记录)
12. [常见问题与调优速查](#十二常见问题与调优速查)

---

## 一、RAG 基础理论

### 1.1 RAG 解决什么问题

- **幻觉问题**：无 RAG 时，LLM 只凭训练数据回答，会编造答案（如虚构"7天无理由退货"）；有 RAG 时基于真实知识库文档回答（如"15天无理由退货"），保证有据可依。
- **私有知识**：大模型不知道企业内部数据，RAG 通过"先检索再生成"注入私有知识。
- **上下文限制**：LLM 上下文窗口有限（如 8K/64K Token），50 页 PDF 无法整体塞入 Prompt，必须切块后只检索最相关的几块。

### 1.2 RAG 两阶段流程

```text
【离线·入库】PDF/TXT → 文档加载 → 分块(Chunking) → Embedding向量化 → 存入 VectorStore
【在线·问答】用户提问 → 向量化 → Top-K 相似度检索 → 组装 Prompt(问题+上下文) → LLM 生成
```

### 1.3 分块策略对比

| 策略 | 优点 | 缺点 | 说明 |
|------|------|------|------|
| 固定大小 | 实现简单 | 可能切断语义 | — |
| 按段落 | 语义完整 | 大小不均 | — |
| 按页 | 页码可溯源 | 粒度受排版影响 | 本项目 PDF 方案 |
| 递归分割 | 兼顾语义和大小 | 实现稍复杂 | 推荐方案 |

本项目使用 `TokenTextSplitter`（默认 800 Token/块）。

### 1.4 Embedding 原理

文本经模型转为高维浮点向量（如 1024 维），**语义相近的文本在向量空间中距离相近**（"蓝牙耳机"与"无线耳机"余弦相似度高），以此实现"以意搜"。传统关键词检索无法理解这种语义相似性。

### 1.5 RAG 优化方向速查

| 问题 | 优化手段 |
|------|----------|
| 检索不准 | 调分块大小 / 引入混合检索 |
| 上下文太长 | 减小 topK |
| 出现幻觉 | 强化 Prompt 约束 + 降低 temperature |
| 知识更新延迟 | 增量索引（RocketMQ 异步同步） |

---

## 二、整体架构：三服务读写分离

RAG 能力横跨三个微服务，写入与读取链路解耦：

```text
┌─────────────────┐   RocketMQ    ┌──────────────────────┐
│ ai-cs-knowledge │ ────────────▶ │ Chroma 向量库          │
│ (知识后台:8082)  │   异步向量化    │ collection:           │
│ 文档CRUD+投递消息 │               │ aics-knowledge        │
└─────────────────┘               └──────────┬───────────┘
                                             │ 相似度检索
┌─────────────────┐    Feign    ┌────────────▼───────────┐
│ ai-cs-chat      │ ◀────────── │ ai-cs-search           │
│ (RAG问答:8083)   │             │ (检索底座:8084, ES+向量) │
└─────────────────┘             └────────────────────────┘
```

- **离线写路径**：知识后台 DB 落库 `kb_document` 表 → 投递 RocketMQ 消息（CREATE/UPDATE/DELETE）→ `KnowledgeSyncConsumer` 消费 → `KnowledgeVectorService.vectorize()` 切块向量化 → 写 Chroma。
- **在线读路径四种模式**：
  1. 纯向量：宽召回 Top-20 → Rerank 精排 Top-N
  2. 混合检索：ES BM25 + 向量 + RRF 融合（`HybridRetriever`）
  3. 查询改写：`QueryRewriteService` 的 LLM 改写 / HyDE
  4. 知识图谱：`GraphRagService` 优先，未命中降级纯向量
- **统一编排**：`retrieveRagDocs(...)` 编排四种模式，增强失败自动降级纯向量，保证主链路可用。

---

## 三、知识入库链路：加载 → 分块 → 向量化 → 存储

### 3.1 文档加载（DocumentLoader）

三种加载器按扩展名路由：

```java
// KnowledgeBaseService.addFile 格式路由核心
if (isPdf(file)) {
    documents = documentLoader.loadPdf(resource);      // PDF 按页读，metadata 带 page_number
} else if (isTika(file)) {                              // docx/xlsx/html/htm
    documents = documentLoader.loadTika(resource);
} else {
    documents = documentLoader.loadText(resource);      // txt/md 等纯文本
}
return addChunks(knowledgeBase, documents);
```

- PDF 用 `PagePdfDocumentReader` 按页分割（每页一个 Document），保留页码供溯源：

```java
PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
    PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build());
```

- 所有加载方法 catch 异常返回空列表——坏文件不会让上传接口整体失败（500）。

### 3.2 分块（TokenTextSplitter）

```java
TokenTextSplitter splitter = new TokenTextSplitter(
    800,    // chunkSize：每块最大 Token 数
    350,    // minChunkSizeChars：最小字符长度
    5,      // minChunkLengthToEmbed：低于此长度不向量化
    10000,  // maxNumChunks：最多块数
    true);  // keepSeparator
// 默认构造 new TokenTextSplitter() 即为上述默认值
List<Document> chunks = new TokenTextSplitter().split(new Document(text));
```

要点：
- chunkSize 是 **Token 数量**而非字符数，实际每块大小受换行、空格等自然边界影响。
- 改切块策略后旧数据不会自动重切，需重新向量化入库。
- 切分后的片段继承原始 Document 元数据。

### 3.3 打元数据 + 写向量库（addChunks）

```java
chunks.forEach(chunk -> {
    Map<String, Object> meta = chunk.getMetadata();
    meta.put("knowledgeBase", knowledgeBase);   // 多知识库隔离过滤
    meta.put("documentId", ...);                // 溯源定位
    meta.put("title", title);                   // 前端引用展示
});
vectorStore.add(chunks);   // 内部自动调用 EmbeddingModel 向量化后写入
```

元数据作用：
- `knowledgeBase`：检索时 filterExpression 过滤，防止多库串扰；
- `documentId`：溯源与删除定位（注意：当前实现存的是分块自身 ID，若要整篇文档级删除需从调用方传入统一 ID）；
- `page_number`：由 PDF Reader 自动写入，用于分页溯源。

---

## 四、向量检索与 VectorStore 抽象

### 4.1 四个核心角色

| 角色 | 实现 | 说明 |
|------|------|------|
| EmbeddingModel | `siliconFlowEmbeddingModel`（@Primary） | 硅基流动 BAAI/bge-m3；DeepSeek 不支持 `/v1/embeddings` 故单独装配 |
| VectorStore | `ChromaVectorStore`（starter 自动装配） | 持久化，collection `aics-knowledge` |
| 业务服务 | `KnowledgeBaseService` | 分块/入库/检索/拼上下文 |
| RAG Advisor | `QuestionAnswerAdvisor` | 让所有 ChatClient 对话自动带检索 |

### 4.2 关键一致性约束：入库与查询必须同一向量模型

不同 Embedding 模型的向量空间互不兼容——即使维度相同，各维语义也不同。本项目通过 @Primary Bean + Nacos 统一配置，保证 `ai-cs-knowledge`（入库）与 `ai-cs-chat`（查询）都用 bge-m3。**更换模型后必须清空或新建 Collection 并全量重新入库。**

bge-m3 检索技巧：查询加官方指令前缀 `"为这个句子生成表示以用于检索相关文章："` 可提升查询向量质量。

### 4.3 VectorStore 抽象价值

SimpleVectorStore（内存测试）/ Chroma / ES / Qdrant / Milvus / PGVector 实现同一接口，切换只需换依赖 + 配置，业务代码零改动。

### 4.4 Chroma 版本兼容（重要踩坑）

- 远端 Chroma 只支持 v2 API，Spring AI 1.0.0 走 v1 会报 410 deprecated；另有集合存在性判断的字符串比对 bug（"does not exists" vs "does not exist"）导致误报 404。
- 解决：父 POM 升级 `spring-ai.version` 至 **1.1.4**，实测与 Spring Boot 3.2.5 兼容。
- 依赖坐标注意是 `spring-ai-starter-vector-store-chroma`（不是 `...-chroma-store-spring-boot-starter`）。

### 4.5 全局 RAG Advisor

```java
@Bean
public QuestionAnswerAdvisor ragAdvisor(VectorStore vectorStore) {
    return QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder()
                    .similarityThreshold(0.3d)
                    .topK(5)
                    .build())
            .build();
}
// 通过 defaultAdvisors(ragAdvisor) 挂到每个 ChatClient
```

---

## 五、检索优化之一：两阶段检索（宽召回 + Rerank 精排）

### 5.1 为什么需要两阶段

纯向量检索三个痛点：
1. **召回噪声**：话题相近但答案不同的片段分数都高；
2. **关键词失灵**：精确型号（如 "AICS-X200"）排不到前面；
3. **分数不可比**：余弦相似度跨知识库无统一阈值。

两阶段思路："**向量检索负责找得到，Rerank 负责找得对**"——先宽召回 Top-20（快、召回全），再交叉编码器精排 Top-5（慢、判得准）。这是生产级 RAG 标准做法。

### 5.2 Cross-Encoder vs Bi-Encoder

| 类型 | 编码方式 | 特点 | 适用 |
|------|----------|------|------|
| Bi-Encoder（双塔） | 问题、文档各自独立编码 | 可离线预计算，但看不到词级交互 | 全库召回 |
| Cross-Encoder（交叉编码器） | "问题+文档"拼接整体编码 | 词级交互精度高但慢 | 几十条候选精排 |

Rerank 使用硅基流动 API：`POST {baseUrl}/v1/rerank`，模型 `BAAI/bge-reranker-v2-m3`（中文效果好、8192 token 上下文），响应 `results[]` 含 `index`/`relevance_score`(0~1)。

### 5.3 index 桥梁机制

Rerank 只返回输入列表下标，编排层通过 `recallDocs.get(item.getIndex())` 取回原 Document（含完整 metadata）：

```java
List<RerankResultItem> reranked = rerankService.rerank(query, recallDocs, topK).block();
if (reranked != null && !reranked.isEmpty()) {
    for (RerankResultItem item : reranked) {
        result.add(recallDocs.get(item.getIndex()));  // 按下标回取原文档
    }
    return result;
}
// reranked 为 null → 降级：直接用已按相似度降序的宽召回结果 subList 取 Top-N
```

### 5.4 四层降级策略（核心设计）

1. apiKey 为空 → 不发请求直接降级；
2. `.timeout()` 超时兜底（默认 5s）；
3. `.onErrorResume()` 捕获网络/HTTP 异常返回空；
4. `ObjectProvider<RerankService>` 可选注入，Bean 缺失自动跳过。

```java
// SiliconFlowRerankService.rerank：同步调用包进响应式管道
return Mono.fromCallable(() -> doRerank(query, documents, topN))
        .subscribeOn(Schedulers.boundedElastic())  // 必须切弹性线程池，timeout 计时才生效
        .timeout(Duration.ofMillis(properties.getTimeoutMs()))
        .onErrorResume(e -> { log.warn(...); return Mono.empty(); });
```

精排过滤"宁缺毋滥"：低于 minScore 的丢弃，按相关度降序输出。

### 5.5 成本控制

单次问答成本 ≈ `recall-top-k` × Rerank 单价，20 条召回是性价比平衡点。

---

## 六、检索优化之二：混合检索（ES BM25 + 向量 RRF 融合）

### 6.1 为什么需要双路互补

- 向量检索擅长语义/同义改写，但对精确型号、订单号等实体不敏感；
- BM25 擅长字面精确匹配，但同义改写会漏；
- 客服场景两类问题都有，需双路互补。

### 6.2 BM25 核心直觉

- **词频饱和**（k1 默认 1.2）：词出现次数越多分数越高但有上限；
- **逆文档频率**：罕见词更有区分度（"退货"比"我们"值钱）；
- **长度归一化**（b 默认 0.75）：惩罚长文档；
- **字段加权**：标题命中比正文值钱（本项目 `title^2`）。

ES 的 match/multi_match 默认走 BM25，无需自己实现。

### 6.3 RRF 倒数排名融合

```text
RRFScore(d) = Σ 1/(k + r)    k 为平滑常数（标准值 60）
```

只看排名不看分数——因为向量分数（0~1）与 BM25 分数（可上千）量纲不同无法直接加权。双路都命中的文档分数天然叠加排最前（单路第 1 名仅 0.0164，双路命中可达 0.03+）。相比加权线性融合无需调 α/β、无量纲、鲁棒。

```java
// RrfMerger.addScores：RRF 核心公式
scores.merge(item.getId(), 1.0 / (k + rank), Double::sum);
```

**关键前提：两路 ID 体系一致**——ES 路用 `_id`，向量路用 `metadata.documentId`，靠入库双写保证一致。

### 6.4 双路编排与降级

```java
List<RankedItem> esItems = esSearch(knowledgeBase, query);
List<RankedItem> vectorItems = vectorSearch(knowledgeBase, query);
if (esItems.isEmpty()) return toResults(vectorItems);   // ES 路 → 仅向量
if (vectorItems.isEmpty()) return toResults(esItems);   // 向量路 → 仅 ES
List<RankedItem> merged = RrfMerger.merge(esItems, vectorItems, topK, RRF_K);
```

每路方法内部 try/catch 吞掉自己的异常返回空列表——单路故障自动降级为另一路结果。

ES 关键词路查询（filter 不参与打分，must 参与 BM25）：

```java
.query(q -> q.bool(b -> b
    .filter(f -> f.term(t -> t.field("knowledgeBase").value(knowledgeBase)))
    .must(m -> m.multiMatch(mm -> mm.fields("title^2", "content").query(query)))))
```

融合结果条数有限，采用内存分页：取前 `page*size` 条后 subList 切页。

### 6.5 与 Rerank 的关系

混合检索解决"召回来源单一"，Rerank 解决"排序不精确"；生产级 RAG 通常串联两者：**双路召回 + RRF → 候选集 → Rerank Top-5**。

### 6.6 调参建议

| 参数 | 建议值 | 说明 |
|------|--------|------|
| RETRIEVE_TOP_K | 20 起步 | 大库可到 50 |
| RRF_K | 60 | 想让双路命中更突出可调小到 30 |
| title^N | 2 | 标题质量高可提到 3 |

---

## 七、引用溯源：带出处回答

### 7.1 合规价值

客服场景四大需求：审计追踪（答复可回溯政策原文）、责任界定（区分"检索错"还是"模型编造"）、信任建立（用户看到出处更信）、质量评估（用 citations 分数量化 RAG 效果）。

### 7.2 数据来源设计原则

**从检索结果构建而非 LLM 标注**——LLM 标注来源容易编造（幻觉来源）；检索结果的 metadata 是真实入库数据天然可信，且回答正是基于这些片段生成的，天然对应。全部字段无需二次查库：

| 字段 | 来源 |
|------|------|
| documentId / title | 入库时 `addChunks` 写入 |
| page_number | `PagePdfDocumentReader` 按 PDF 页自动写入 |
| score | 检索时 VectorStore 计算的相似度 |
| content | 分块原文 |

```java
// buildCitations：metadata → DTO，防御性转换
Object docId = meta.get("documentId");
if (docId != null) {
    try { item.setDocumentId(Long.valueOf(String.valueOf(docId))); }
    catch (NumberFormatException ignore) { }   // UUID 等非数字忽略
}
if (meta.get("page_number") instanceof Number pageNum) item.setPage(pageNum.intValue());
item.setScore(doc.getScore() != null ? doc.getScore().doubleValue() : null);
item.setContent(doc.getText());
```

### 7.3 流式场景关键做法

检索是一次性完成的，引用不能混入流式内容推送——**检索阶段先构建并缓存 citations，流式结束后在 done 事件里一次性携带 content + citations**：

```java
citations = buildCitations(docs);          // 检索阶段先构建好
...
doneEvent.put("content", response);
doneEvent.put("citations", citations);
emitter.send(SseEmitter.event().data(doneEvent));
```

前端 SSE 解析要点（ChatView.vue）：

```js
if (obj.content) assistant.content += obj.content     // \n\n 分隔事件，增量拼接打字机效果
if (obj.citations) assistant.citations = obj.citations // 整体覆盖，不能追加（防重复）
```

### 7.4 前端引用卡片 UI 要点

- 相关度进度条颜色分级：≥0.7 绿 / ≥0.5 黄 / 其余红；
- 原文预览 line-clamp 3 行截断；
- 空值兜底：`cit.title || '未命名文档'`、`v-if="cit.page"`；
- 未命中知识库时 docs 为空 → citations 为空数组 → 卡片不渲染，如实告知"暂无资料"。

---

## 八、文档格式扩展：Tika 多格式解析

### 8.1 背景

客服资料多为 Word 话术、Excel 价格表、HTML 导出 FAQ；只支持 PDF/TXT 意味着人工转格式或放弃入库。

### 8.2 Apache Tika

Java 生态最成熟的内容检测与文本提取库（Apache 顶级项目）：
- 格式检测靠 Magic Bytes，不依赖扩展名；
- 底层支持 300+ 格式（Office 全家桶、PDF、HTML/XML、RTF、OpenDocument、EPUB 等）；
- Spring AI 通过 `spring-ai-tika-document-reader` 提供 `TikaDocumentReader`，与向量化链路无缝衔接。

### 8.3 格式路由策略

`.pdf` → PagePdfDocumentReader（引用溯源需要按页+页码能力，故不走 Tika）；`.docx/.xlsx/.html/.htm` → TikaDocumentReader；其余（txt/md）→ TextReader。

扩展新格式只需在 `isTika()` 补扩展名分支（如 .doc/.ppt/.pptx/.odt），零额外代码。

### 8.4 注意事项

- xlsx 解析：Tika 把单元格文本按行拼接，适合文本型表格；复杂公式/图表可能有噪声，建议入库前人工整理；
- ⚠️ 项目未配置 multipart 上限，Spring Boot 默认单文件 1MB，办公文档常超限，建议追加：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 50MB
```

- 中文编码 Tika 自动检测（UTF-8/GBK），一般无需干预。

---

## 九、知识库增量同步：RocketMQ 驱动向量化

### 9.1 解决的问题

"数据库改了、向量库没跟上"的一致性问题——MySQL 中文档 CRUD 后异步驱动 Chroma 同步更新，实现最终一致。

**为何必须异步**：一次入库 = 分块 → 每块调 Embedding API（网络往返）→ 写 Chroma；100 块的文档要 100 次 HTTP 调用。同步执行会导致接口响应慢、DB 连接池被拖满、Embedding API 抖动引发保存操作回滚。

### 9.2 整体链路

```text
KnowledgeServiceImpl(CRUD 后投递) → RocketMQ(topic: knowledge-doc-sync-topic, tag: CREATE/UPDATE/DELETE)
  → KnowledgeSyncConsumer 按 action 分发
      ├─ CREATE/UPDATE → KnowledgeVectorService.vectorize() 切块入库
      └─ DELETE        → deleteByDocumentId() 按 metadata 过滤删除
```

```java
// Producer：syncSend 同步确认落盘 ack，topic:tag 语法，失败仅告警（DB 已成功，不让 MQ 问题影响主流程）
rocketMQTemplate.syncSend(TOPIC + ":" + action, message);

// Consumer：一个消费者处理全部 action
@RocketMQMessageListener(topic = "knowledge-doc-sync-topic",
        consumerGroup = "knowledge-sync-group", selectorExpression = "*")
public void onMessage(KnowledgeSyncMessage message) {
    if ("DELETE".equals(action)) {
        knowledgeVectorService.deleteByDocumentId(message.getDocumentId());
    } else {
        knowledgeVectorService.vectorize(doc);   // CREATE/UPDATE 向量化
    }
}

// 按 metadata 删除该文档所有分块
vectorStore.delete("documentId == '" + documentId + "'");
```

### 9.3 消息设计决策

- 单一 topic + tag 区分动作；
- 消息携带**全量 content** 而非只带 ID 回查 DB——消费端不依赖数据库，可独立部署且天然幂等；
- DELETE 只带 documentId（内容已无意义）。

### 9.4 幂等性三手段

1. 消费端不查 DB、消息语义自包含；
2. 向量以 documentId 分组管理（每个分块 metadata 带 documentId）；
3. UPDATE 语义为"删旧建新"（进阶：消费 UPDATE 时先 deleteByDocumentId 再 vectorize）。

### 9.5 失败处理取舍

- 当前实现：Consumer catch 异常后不抛（消息被 ACK），代价是丢一次同步机会；
- 生产推荐：抛异常 → RocketMQ 自动重试 16 次（间隔递增）→ 死信队列 `%DLQ%knowledge-sync-group` → 人工/Dashboard 重投/定时补偿。

### 9.6 已知边界问题

- **乱序风险**：CREATE 重试期间 DELETE 先消费，CREATE 重投会把已删文档向量写回 → 对策：按 documentId 串行消费或消费前校验 DB；
- **消费积压**：调小 consumeThreadMax 防压垮 Embedding API、改批量 API、批量消费；
- 建议补充同步状态字段（如 `sync_status`）做补偿对账。

---

## 十、配置项汇总

### 10.1 Nacos `ai-cs-chat.yml`（RAG 核心）

```yaml
aics:
  rag:
    recall-top-k: 20       # 宽召回条数（文档量大可加至 30~50）
    recall-threshold: 0.3  # 召回相似度阈值（宽召回，非 0.5；过高漏召回、过低引噪声）
    hybrid: true           # 是否启用混合检索
    rewrite: false         # 是否启用查询改写（默认关闭）
  rerank:
    base-url: https://api.siliconflow.cn
    api-key: ${SILICONFLOW_API_KEY}   # 环境变量注入，Embedding 与 Rerank 共用
    model: BAAI/bge-reranker-v2-m3
    top-n: 5          # 精排返回条数（上下文充裕可到 8~10）
    min-score: 0.7    # 相关度阈值（库质量高可 0.8，模糊问题多降到 0.6）
    timeout-ms: 5000  # 中文长文本实测 3~8s，5s 是折中
```

### 10.2 Chroma 向量库连接

```yaml
spring:
  ai:
    model:
      embedding:
        enabled: false          # 禁用内置 Embedding 自动装配（手动提供 bge-m3）
    vectorstore:
      chroma:
        initialize-schema: true # 首次启动自动建集合
        collection-name: aics-knowledge
        client:
          host: http://localhost   # 复用 MySQL 主机保证同机部署
          port: ${CHROMA_PORT:8000}
```

本地启动 Chroma：

```bash
docker run -d --name chroma -p 8000:8000 chromadb/chroma
# 或 pip install chromadb && chroma run --host 0.0.0.0 --port 8000
```

### 10.3 Elasticsearch（ai-cs-search）

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200   # 开启认证时配 username/password
```

ES 索引首次写入时由 `createEsIndexIfNeeded(...)` 幂等自动创建，索引名必须小写。

### 10.4 RocketMQ（ai-cs-knowledge）

- `aics-shared.yml`：`rocketmq.name-server: 127.0.0.1:9876`
- `ai-cs-knowledge.yml`：`rocketmq.producer.group: knowledge-producer-group`
- 可选参数：consumeThreadMax（默认 64）、retryTimesWhenConsumeFailed（默认 16，可配 3~5 减少 DLQ 堆积）

### 10.5 环境与服务端口

| 项 | 值 |
|----|----|
| JDK | 21 |
| Spring Boot | 3.2.5 |
| Spring AI | 1.1.4 |
| SILICONFLOW_API_KEY | Embedding 与 Rerank 共用环境变量 |
| ai-cs-chat | 8083 |
| ai-cs-knowledge | 8082 |
| ai-cs-search | 8084 |
| 网关 | 8080（/api/knowledge、/api/search） |
| Chroma | 8000，collection `aics-knowledge` |
| ES | 9200（本地 8.12.2，xpack.security.enabled=false） |

---

## 十一、测试验证与踩坑记录

> 摘自 [09-接口测试报告](09-ai-cs-knowledge-search接口测试报告.md)：23/23 用例通过，覆盖服务直连与网关两种链路。

发现并修复的 6 个问题：

| # | 服务 | 问题 | 根因 | 修复 |
|---|------|------|------|------|
| 1 | search | SearchController 混入 knowledge 模块代码，编译报错 | 版本库文件污染 | 重写 Controller |
| 2 | search | 全部接口 500：argument not available via reflection | @PathVariable/@RequestParam 未显式命名且未开 -parameters | 显式命名参数 |
| 3 | search | IndexCoordinates cannot be resolved | spring-data-elasticsearch 5.x 中该类移到 core.mapping 包 | 修正 import 并 clean 重编译 |
| 4 | knowledge | 列表 500：表不存在 | 实体 @TableName 与实际表名 kb_document 不一致 | 修正实体表名 |
| 5 | knowledge | 创建 500：create_time cannot be null | 缺 MetaObjectHandler | 新增 MybatisPlusConfig（分页插件+自动填充） |
| 6 | knowledge | 列表 500 反射取参问题 | @RequestParam 未显式命名 | 显式命名 |

负向用例约定：查询不存在资源返回 500 是 BusinessException 包装的合理处理；删除不存在的 ES 索引返回 200 是幂等语义。

---

## 十二、常见问题与调优速查

| 现象/需求 | 处理方式 |
|-----------|----------|
| 检索结果为空 | 多为阈值过高，降低 similarityThreshold |
| Chroma 数据会丢吗 | Chroma 落盘持久化不丢；只有 SimpleVectorStore（内存）重启丢 |
| 启动连不上 Chroma | 检查端口 / CHROMA_PORT 环境变量 |
| 更换 Embedding 模型 | 清空/新建 Collection 后全量重新入库（新旧向量空间不兼容） |
| 调整分块大小 | 需全量重新向量化，旧数据不会自动重切 |
| 新增检索模式 | 在枚举扩展并保证可降级 |
| 接新向量库 | 实现 VectorStore 接口或扩展 search 服务，业务代码零改动 |
| 上传大文件失败 | 配置 spring.servlet.multipart 上限（默认 1MB/文件） |
| MQ 消息乱序 | 按 documentId 串行消费或消费前校验 DB |
| MQ 消费失败丢失 | 生产改为抛异常走重试 + 死信队列补偿 |

---

## 附：原文档与知识点对照

| 原文档 | 主要知识点 |
|--------|-----------|
| 01-RAG检索增强生成 | RAG 理论、两阶段流程、分块策略、Embedding 原理 |
| 02-RAG检索增强开发 | 三服务架构、读写路径总纲、四模式检索编排 |
| 03-RAG向量检索实战 | 四核心角色、Chroma 接入、两阶段检索升级、metadata 过滤 |
| 04-Rerank重排序 | Cross-Encoder、硅基流动 Rerank API、四层降级、成本控制 |
| 05-混合检索 | BM25 原理、RRF 公式、双路编排降级、内存分页 |
| 06-引用溯源 | citations 构建、流式 done 事件、前端卡片 UI |
| 07-Tika多格式解析 | Tika 能力、格式路由、multipart 配置 |
| 08-RocketMQ增量同步 | 异步链路、消息设计、幂等、死信补偿 |
| 09-接口测试报告 | 23 用例验证结论、6 个修复记录 |
