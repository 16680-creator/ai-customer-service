# RAG 全栈实战 —— 知识点汇总

> 本文由两部分材料合并：本目录 01~09 专题原文（代码与实现细节）+ Word 讲稿《RAG 全栈实战：知识点汇总》（工程决策、验证清单与踩坑边界）。
> 定位是快速复习与查阅的总纲：每个知识点同时给出"怎么做"和"为什么这样取舍"。
> 原文索引：[01-理论](01-RAG检索增强生成.md) · [02-开发总纲](02-RAG检索增强开发.md) · [03-向量检索](03-RAG向量检索实战.md) · [04-Rerank](04-RAG进阶实战-Rerank重排序.md) · [05-混合检索](05-混合检索实战-ES-BM25与向量RRF融合.md) · [06-引用溯源](06-引用溯源实战-带出处回答.md) · [07-Tika多格式](07-文档格式扩展实战-Tika多格式解析.md) · [08-RocketMQ增量同步](08-知识库增量同步实战-RocketMQ驱动向量化.md) · [09-接口测试报告](09-ai-cs-knowledge-search接口测试报告.md)

---

## 目录

1. [RAG 基础理论](#一rag-基础理论)
2. [整体架构：三服务读写分离](#二整体架构三服务读写分离)
3. [知识入库链路：加载 → 分块 → 向量化 → 存储](#三知识入库链路加载--分块--向量化--存储)
4. [向量检索与 VectorStore 抽象](#四向量检索与-vectorstore-抽象)
5. [检索优化之一：两阶段检索（宽召回 + Rerank 精排）](#五检索优化之一两阶段检索宽召回--rerank-精排)
6. [检索优化之二：混合检索（ES BM25 + 向量 RRF 融合）](#六检索优化之二混合检索es-bm25--向量-rrf-融合)
7. [引用溯源：带出处回答](#七引用溯源带出处回答)
8. [文档格式扩展：Tika 多格式解析](#八文档格式扩展tika-多格式解析)
9. [知识库增量同步：RocketMQ 驱动向量化](#九知识库增量同步rocketmq-驱动向量化)
10. [配置项汇总](#十配置项汇总)
11. [验证与测试](#十一验证与测试)
12. [常见问题定位与调优速查](#十二常见问题定位与调优速查)
13. [生产化检查清单](#十三生产化检查清单)
14. [推荐学习顺序](#十四推荐学习顺序)

---

## 一、RAG 基础理论

### 1.1 RAG 解决什么问题

RAG（Retrieval-Augmented Generation，检索增强生成）先从私有知识库检索相关资料，再把资料与用户问题一起交给大模型。它主要解决三类问题：

- **幻觉问题**：无 RAG 时，LLM 只凭训练数据回答，会编造答案（如虚构"7天无理由退货"）；有 RAG 时基于真实知识库文档回答（如"15天无理由退货"），保证有据可依。
- **私有知识**：大模型不知道企业内部数据，RAG 通过"先检索再生成"注入私有知识。
- **上下文限制**：LLM 上下文窗口有限（如 8K/64K Token），50 页 PDF 无法整体塞入 Prompt，必须切块后只检索最相关的几块。

对应到工程口径就是三句话：模型不知道企业私有知识、模型知识可能过时、回答缺乏可追溯依据。典型效果：没有 RAG 时模型可能编造"7 天无理由退货"；接入 RAG 后，模型可依据知识库回答"支持 15 天无理由退货，运费由买家承担"，并返回原文出处。

### 1.2 RAG 两阶段流程

```text
【离线·入库】PDF/TXT → 文档加载 → 分块(Chunking) → Embedding向量化 → 存入 VectorStore
【在线·问答】用户提问 → 向量化 → Top-K 相似度检索 → 组装 Prompt(问题+上下文) → LLM 生成
```

把两阶段展开成完整链路（含异步同步与降级点）：

```text
离线写路径
文件/文本 -> 格式解析 -> 分块 -> 元数据补全 -> Embedding -> VectorStore
    DB CRUD -> RocketMQ CREATE/UPDATE/DELETE -> 异步向量同步

在线读路径
用户问题 -> 查询改写(可选) -> 宽召回 -> RRF/Rerank -> 上下文组装
        -> Prompt 约束 -> LLM 生成 -> 引用溯源 -> 前端展示
```

### 1.3 分块策略对比

| 策略 | 优点 | 风险 | 适用场景 |
|------|------|------|----------|
| 固定大小 | 实现简单、稳定 | 容易截断语义 | 通用基线 |
| 按段落/标题 | 语义较完整 | 块大小不均 | 文章、制度文档 |
| 按页 | 天然保留页码、可溯源 | 单页可能过长 | PDF 引用溯源（本项目方案） |
| 递归分割 | 兼顾结构与大小 | 参数更多、实现稍复杂 | 生产推荐 |

分块的目标不是单纯把文本切小，而是在**上下文预算、语义完整性和检索粒度**之间取平衡。本项目使用 `TokenTextSplitter`（默认 800 Token/块），要点：

- 改块大小后**必须重新向量化**，旧数据不会自动重切；
- 块过大 → 召回内容泛、Prompt 成本高；块过小 → 上下文碎片化、答案缺少完整条件；
- 生产中通常还要设置适量 overlap，避免关键句跨块断裂。

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

### 2.1 服务边界与关键组件

| 服务 | 核心职责 | 关键组件 |
|------|----------|----------|
| `ai-cs-knowledge` | 文档 CRUD、解析、分块、向量化、增量同步 | `KnowledgeServiceImpl`、`KnowledgeVectorService`、`KnowledgeSyncConsumer`、`DocumentLoader` |
| `ai-cs-chat` | 检索编排、上下文拼装、LLM 生成、引用返回 | `ChatServiceImpl`、`KnowledgeBaseService`、`HybridRetriever`、`RerankService` |
| `ai-cs-search` | ES 全文检索、向量检索和混合检索底座 | Search API、BM25、RRF |

整体采用读写分离：知识后台负责写入与同步，对话服务负责在线检索和生成，搜索服务提供独立检索能力。

---

## 三、知识入库链路：加载 → 分块 → 向量化 → 存储

### 3.1 文档加载与格式路由

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

| 文件类型 | 加载方式 | 说明 |
|----------|----------|------|
| PDF | `PagePdfDocumentReader` | 可按页生成 `Document`，便于保留页码 |
| TXT、Markdown | `TextReader` | 纯文本直接读取 |
| DOC/DOCX、XLS/XLSX、PPT/PPTX、HTML/HTM | `TikaDocumentReader` | Apache Tika 统一抽取正文和元数据 |

推荐路由：`pdf -> loadPdf`，Office/HTML -> `loadTika`，其他文本 -> `loadText`。

- 路由时既看扩展名，也应校验 MIME 类型，避免伪装扩展名。
- PDF 单独走 `PagePdfDocumentReader` 而不是并入 Tika，是为了"按页切分 + 页码"能力，直接服务引用溯源：

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

### 3.3 元数据设计

元数据决定后续能否过滤、删除、引用和排障。每个 chunk 至少携带：

```java
chunks.forEach(chunk -> {
    Map<String, Object> meta = chunk.getMetadata();
    meta.put("knowledgeBase", knowledgeBase);   // 多知识库隔离过滤
    meta.put("documentId", ...);                // 溯源定位
    meta.put("title", title);                   // 前端引用展示
});
vectorStore.add(chunks);   // 内部自动调用 EmbeddingModel 向量化后写入
```

- `knowledgeBase`：知识库隔离和检索过滤；检索时 filterExpression 依赖它，防止多库串扰。
- `documentId`：文档级删除、更新和幂等处理。
- `title`：引用展示。
- `page_number`：由 `PagePdfDocumentReader` 按 PDF 页自动写入，用于页码引用（可获取时写入）。

可选：文件名、来源 URL、版本、租户、ACL、更新时间、chunk 序号。

三条硬约束：

1. 元数据键应在**写入端、检索端和前端 DTO 中保持统一**。缺少 `documentId` 会导致旧分块难以清理；缺少标题和页码会让引用只能展示无意义的片段。
2. ⚠️ 当前实现的 `documentId` 存的是分块自身 ID，若要整篇文档级删除需从调用方传入统一 ID。
3. 切分后的片段继承原始 Document 元数据，因此新增字段只需在父 Document 上写入一次。

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

### 4.3 VectorStore 抽象价值与可替换存储

项目通过 Spring AI 的 `EmbeddingModel` 与 `VectorStore` 抽象把**模型与存储解耦**：SimpleVectorStore（内存测试）/ Chroma / Elasticsearch / PgVector / Qdrant / Milvus / Redis 实现同一接口，切换只需换依赖 + 配置，业务代码零改动。

- `VectorStore.add(chunks)` 内部完成批量向量化与写入。
- `VectorStore.similaritySearch(SearchRequest)` 支持 `topK`、相似度阈值和 metadata 过滤。

替换存储时必须保持一致的四项语义：**向量维度、距离度量、过滤表达式、删除语义**。任一项变化都会让既有向量失效或过滤条件静默失配。

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

### 4.6 基础检索链路与上下文拼装

基础链路是：问题向量化 -> 按 `knowledgeBase` 过滤 -> Top-K 相似度检索 -> 拼装上下文 -> LLM 回答。

项目默认 `recall-threshold` 为 `0.3`。阈值过低会引入噪声，过高会漏掉相关片段。**不要孤立调阈值**，应结合块大小、Embedding 模型、Top-K 和实际查询集一起评估。

上下文拼装要做到五点：保留来源标识；控制总 token；按最终相关度排序；去重相邻或高度重复片段；明确分隔不同来源。

### 4.7 Prompt 约束

RAG Prompt 至少包含三部分：角色、检索上下文、回答规则。关键规则包括：

1. 只依据给定资料回答，不把模型常识伪装成知识库事实。
2. 资料不足时明确说未找到，不补写具体政策或数字。
3. 回答结论与引用片段一致。
4. 防止检索文档中的指令覆盖系统规则，即把知识内容视为数据而非指令（Prompt Injection 防线）。

Spring AI 的 `QuestionAnswerAdvisor` 可自动完成检索和上下文注入；项目的复杂链路仍需要业务编排层，以接入混合检索、Rerank、ACL、Guardrail、弹性调用与引用组装。

---

## 五、检索优化之一：两阶段检索（宽召回 + Rerank 精排）

### 5.1 为什么需要两阶段

纯向量检索三个痛点：
1. **召回噪声**：话题相近但答案不同的片段分数都高；
2. **关键词失灵**：精确型号（如 "AICS-X200"）排不到前面；
3. **分数不可比**：余弦相似度跨知识库无统一阈值。

根因在编码方式：向量检索使用双塔模型，文档向量可预计算，速度快，适合从大规模语料中召回候选；但它对问题与文档的细粒度交互建模不足。Rerank 使用交叉编码器同时读取 query 和 document，判断更精细，但成本更高。

两阶段思路："**向量检索负责找得到，Rerank 负责找得对**"——先宽召回 Top-20（快、召回全），再由 Rerank 精排并返回 Top-5（慢、判得准）。这是生产级 RAG 标准做法。

### 5.2 Cross-Encoder vs Bi-Encoder

| 类型 | 编码方式 | 特点 | 适用 |
|------|----------|------|------|
| Bi-Encoder（双塔） | 问题、文档各自独立编码 | 可离线预计算，但看不到词级交互 | 全库召回 |
| Cross-Encoder（交叉编码器） | "问题+文档"拼接整体编码 | 词级交互精度高但慢 | 几十条候选精排 |

Rerank 使用硅基流动 API：`POST {baseUrl}/v1/rerank`，模型 `BAAI/bge-reranker-v2-m3`（中文效果好、8192 token 上下文），响应 `results[]` 含 `index`/`relevance_score`(0~1)。

分工：`KnowledgeBaseService.search` 负责宽召回；`SiliconFlowRerankService` 调用硅基流动 Rerank API；`RerankProperties` 管理 baseUrl/apiKey/model/topN/minScore/timeoutMs 参数。

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

覆盖的失败场景：API Key 为空、待排序文档为空、超时、远程 5xx/网络异常。共同底线是**降级为原向量相似度排序，不能让问答接口整体失败**。

精排过滤"宁缺毋滥"：低于 minScore 的丢弃，按相关度降序输出。

### 5.5 成本控制

单次问答成本 ≈ `recall-top-k` × Rerank 单价，20 条召回是性价比平衡点。

### 5.6 调参顺序

建议顺序：**先保证召回集合包含正确答案，再调 Rerank 的 `top-n` 和 `min-score`**。

- 若正确答案没有进入宽召回，Rerank 无法补救；
- 盲目提高精排阈值还会把弱相关但有用的证据过滤掉；
- 因此召回端用低阈值（0.3）保量，质量交给精排端（`min-score: 0.7`）把关。

---

## 六、检索优化之二：混合检索（ES BM25 + 向量 RRF 融合）

### 6.1 为什么需要双路互补

- 向量检索擅长语义/同义改写，但对精确型号、订单号等实体不敏感；
- BM25 擅长字面精确匹配（精确词、型号、编号、人名、专有名词），但同义改写会漏；
- 客服知识库同时存在这两类查询，单一路径不够稳定，需双路互补。

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

只看排名不看分数——因为向量分数（0~1）与 BM25 分数（可上千）量纲不同无法直接加权。`k` 的作用是减小头部名次差距的过度影响：文档在多路都排名靠前时会获得更高融合分。双路都命中的文档分数天然叠加排最前（单路第 1 名仅 0.0164，双路命中可达 0.03+）。相比加权线性融合，RRF 无需调 α/β、无量纲、鲁棒。

```java
// RrfMerger.addScores：RRF 核心公式
scores.merge(item.getId(), 1.0 / (k + rank), Double::sum);
```

**关键前提：两路 ID 体系一致**——ES 路用 `_id`，向量路用 `metadata.documentId`，靠入库双写保证一致。响应中保留 `esRank`、`vectorRank` 便于解释和排障。

### 6.4 双路编排与降级

```java
List<RankedItem> esItems = esSearch(knowledgeBase, query);
List<RankedItem> vectorItems = vectorSearch(knowledgeBase, query);
if (esItems.isEmpty()) return toResults(vectorItems);   // ES 路 → 仅向量
if (vectorItems.isEmpty()) return toResults(esItems);   // 向量路 → 仅 ES
List<RankedItem> merged = RrfMerger.merge(esItems, vectorItems, topK, RRF_K);
```

每路方法内部 try/catch 吞掉自己的异常返回空列表——单路故障自动降级为另一路结果；两路都失败才上抛业务错误。

ES 关键词路查询（filter 不参与打分，must 参与 BM25）：

```java
.query(q -> q.bool(b -> b
    .filter(f -> f.term(t -> t.field("knowledgeBase").value(knowledgeBase)))
    .must(m -> m.multiMatch(mm -> mm.fields("title^2", "content").query(query)))))
```

融合结果条数有限，采用内存分页：取前 `page*size` 条后 subList 切页。

工程策略清单：

- 并行执行 ES 与向量召回，降低总延迟。
- ES 索引 `_id` 与向量 metadata 的 `documentId` 统一。
- 融合后仍可接 Rerank，形成"多路宽召回 -> RRF -> 精排"。
- 分别用精确查询和语义查询验证两路能力，不能只测普通关键词。

### 6.5 与 Rerank 的关系

混合检索解决"召回来源单一"，Rerank 解决"排序不精确"；两者不互相替代，生产级 RAG 通常串联：**双路召回 + RRF → 候选集 → Rerank Top-5**。

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

边界认知：引用能提高可验证性，但**不能自动保证结论正确**。相关度是检索信号，不应被展示成"答案正确率"；还要验证答案中的数字、条件和例外是否真的由引用片段支持。

### 7.2 数据来源设计原则

**从检索结果构建而非 LLM 标注**——LLM 标注来源容易编造（幻觉来源）；检索结果的 metadata 是真实入库数据天然可信，且回答正是基于这些片段生成的，天然对应。全部字段无需二次查库：

| 字段 | 来源 |
|------|------|
| documentId / title | 入库时 `addChunks` 写入 |
| page_number | `PagePdfDocumentReader` 按 PDF 页自动写入 |
| score | 检索时 VectorStore 计算的相似度 |
| content | 分块原文 |

完整数据链路——引用不是生成完成后临时猜出来的，而是从入库 metadata 一直贯穿到响应：

```text
chunk metadata(documentId/title/page_number)
 -> 检索命中文档
 -> buildCitations
 -> ChatRagResponseDTO(content, citations)
 -> SSE done 事件
 -> 前端引用卡片
```

单条引用通常包含 `documentId`、`title`、`page`、`score` 和原文 `content`。同步接口直接返回回答与引用；流式接口在 token 流结束后的 `done` 事件中返回完整内容和引用列表。

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

### 7.4 去重与前端引用卡片 UI 要点

引用应按文档、页码和片段去重，保留最终相关度顺序；前端展示标题、页码、相关度和原文摘要，并对长片段折叠。

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

Tika 的价值是统一多格式解析，减少逐格式接入成本；但它**不等于完美结构化**——复杂表格、扫描 PDF、图片文字和版式语义仍需要 OCR 或专用解析器补充。

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

**为何必须异步**：解析、分块、Embedding 和向量写入可能耗时数秒甚至更久，不应放在文档 CRUD 的 HTTP/数据库事务链路中。一次入库 = 分块 → 每块调 Embedding API（网络往返）→ 写 Chroma；100 块的文档要 100 次 HTTP 调用。同步执行会导致接口响应慢、DB 连接池被拖满、Embedding API 抖动引发保存操作回滚。

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

- 单一 topic + tag（action）区分动作；
- 消息携带**全量 content** 而非只带 ID 回查 DB——消费端不依赖数据库，可独立部署且天然幂等；
- DELETE 只带 documentId（内容已无意义）。

代价：消息体会变大，需要关注 RocketMQ 单消息大小限制；大文档应考虑只传引用ID + 分片，或改走对象存储中转。

### 9.4 幂等与更新语义

每个 chunk 都带 `documentId`，删除时按 metadata 清理该文档全部向量。三个手段：

1. 消费端不查 DB、消息语义自包含；
2. 向量以 documentId 分组管理（每个分块 metadata 带 documentId）；
3. UPDATE 语义为"删旧建新"——生产环境应明确采用"先 `deleteByDocumentId`，再 `vectorize`"，否则文档内容或分块数变化后会残留旧片段。

仅依赖"相同内容重复写不会重复"不足以覆盖内容变化、乱序与不同 ID 生成策略。更稳妥的方案是增加文档版本或更新时间，消费时拒绝旧版本消息。

### 9.5 失败处理的关键事实

⚠️ 容易误解的一点：当前消费者 `catch` 异常后**不重抛**，消息会被 ACK，因此**不会触发 RocketMQ 自动重试和 DLQ**。只有抛出异常，RocketMQ 才会按策略重试（默认 16 次、间隔递增），耗尽后进入死信队列 `%DLQ%knowledge-sync-group`，再由人工/Dashboard 重投或定时补偿。

生产推荐：

1. 可重试异常继续抛出，让 MQ 重试并进入 DLQ。
2. 不可重试的数据错误记录失败原因并告警。
3. 增加同步状态/待同步表和定时对账任务，补偿 Producer 投递失败。
4. 处理 CREATE/UPDATE/DELETE 乱序，可使用顺序消息、版本校验或消费前检查当前状态。
5. 控制消费并发，避免打爆 Embedding API。

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

> `min-score` 以仓库实际配置为准（`tools/nacos-config/ai-cs-chat.yml` 与 `RerankProperties` 默认值均为 **0.7**）。Word 讲稿与 [02-开发总纲](02-RAG检索增强开发.md) 中出现的 `min-score: 0.0` 是早期示例，会关闭精排过滤，不要照抄。

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

### 10.5 环境与运行基线

| 项 | 值 |
|----|----|
| JDK | 21（材料中明确提醒本机默认 JDK 8 不可用） |
| Spring Boot | 3.2.5 |
| Spring AI | 1.1.4 |
| SILICONFLOW_API_KEY | Embedding 与 Rerank 共用环境变量 |
| ai-cs-knowledge | 8082 |
| ai-cs-chat | 8083 |
| ai-cs-search | 8084 |
| 网关 | 8080（/api/knowledge、/api/search） |
| Chroma | `localhost:8000`，集合 `aics-knowledge` |
| ES | `localhost:9200`（本地 8.12.2，xpack.security.enabled=false） |
| RocketMQ NameServer | `127.0.0.1:9876` |
| RAG 召回阈值 | 0.3 |
| Rerank | 宽召回约 20，精排返回约 5，min-score 0.7，超时 5000 ms |

### 10.6 配置口径

配置以 Nacos 为主，本地 `application.yml` 作为兜底。外部 API Key 必须通过环境变量或密钥服务注入，**不写入文档、源码或日志**。

---

## 十一、验证与测试

### 11.1 最小闭环验证（8 步）

1. 启动 Chroma、ES、RocketMQ、Nacos 及相关服务。
2. 创建一篇带唯一事实的文档，例如"支持 15 天无理由退货"。
3. 确认 DB 写入、MQ 投递/消费、分块数和向量写入日志。
4. 直接调用知识库检索，确认命中正确 chunk 与 metadata。
5. 调用 `/chat/rag`，确认答案只使用知识内容并返回引用。
6. 更新为"30 天"，确认旧片段消失、新答案生效。
7. 删除文档，确认检索不再命中。
8. 分别停止 ES、Rerank 服务或清空 API Key，验证降级链路。

### 11.2 接口测试基线与踩坑记录

> 摘自 [09-接口测试报告](09-ai-cs-knowledge-search接口测试报告.md)：2026-08-07 覆盖 `ai-cs-knowledge` 5 个接口与 `ai-cs-search` 4 个接口，共 23 个自动化 HTTP 用例，结果 23/23 符合预期，同时覆盖服务直连与网关两种链路。

发现并修复的 6 个问题：

| # | 服务 | 问题 | 根因 | 修复 |
|---|------|------|------|------|
| 1 | search | SearchController 混入 knowledge 模块代码，编译报错 | 版本库文件污染 | 重写 Controller |
| 2 | search | 全部接口 500：argument not available via reflection | @PathVariable/@RequestParam 未显式命名且未开 -parameters | 显式命名参数 |
| 3 | search | IndexCoordinates cannot be resolved | spring-data-elasticsearch 5.x 中该类移到 core.mapping 包 | 修正 import 并 clean 重编译 |
| 4 | knowledge | 列表 500：表不存在 | 实体 @TableName 与实际表名 kb_document 不一致 | 修正实体表名 |
| 5 | knowledge | 创建 500：create_time cannot be null | 缺 MetaObjectHandler | 新增 MybatisPlusConfig（分页插件+自动填充） |
| 6 | knowledge | 列表 500 反射取参问题 | @RequestParam 未显式命名 | 显式命名 |

负向用例约定与一处待改语义：删除不存在的 ES 索引返回 200 属于幂等语义；但"查询不存在资源返回 500"虽被原测试视为符合当前预期（`BusinessException` 包装），从 API 语义看更适合映射为 404 或明确业务错误码，避免监控把正常负向查询误判为服务故障。

---

## 十二、常见问题定位与调优速查

### 12.1 按现象定位（优先检查项）

| 现象 | 优先检查 |
|------|----------|
| 检索不到 | knowledgeBase 元数据、阈值、分块、Embedding 模型/维度、集合名 |
| 命中但回答错误 | Prompt 约束、上下文顺序、片段截断、模型是否忽略证据 |
| 更新后仍命中旧内容 | UPDATE 是否先删后建、documentId 类型/过滤表达式、消息乱序 |
| 引用无标题/页码 | 入库 metadata 是否写入并在 DTO 映射时保留 |
| 精确编号搜不到 | ES BM25 路、分词器、字段 mapping、RRF 对齐 ID |
| Rerank 导致接口失败 | 超时与异常降级是否覆盖、API Key 与模型名 |
| MQ 没有重试 | Consumer 是否 catch 后吞掉异常 |
| Office 文件为空/乱码 | Tika 依赖、MIME、损坏文件、扫描件是否需要 OCR |

### 12.2 处置速查

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

## 十三、生产化检查清单

- **文档解析**：限制文件大小和类型；检测 MIME；隔离恶意文件；扫描件有 OCR 方案。
- **分块质量**：按真实问答集评估块大小和 overlap；变更后全量重建。
- **数据隔离**：`knowledgeBase`、tenant、ACL 在检索前强制过滤，不能只靠 Prompt。
- **检索质量**：同时维护精确查询、语义查询、无答案查询和对抗查询测试集。
- **可靠性**：ES、向量库、Rerank 任一故障都有清晰降级；超时预算逐层收敛。
- **一致性**：UPDATE 先删后建；版本防乱序；Producer 失败可补偿；Consumer 失败可重试/DLQ。
- **可观测性**：记录各阶段耗时、召回数、过滤数、最终引用、降级原因和 traceId。
- **安全**：防 Prompt Injection；敏感 metadata 不进入模型；API Key 不落日志。
- **引用**：答案结论能被片段直接支持；来源可访问；页码和标题真实准确。
- **验收**：CRUD、索引、检索、RAG、更新、删除、网关和故障演练形成自动化回归。

---

## 十四、推荐学习顺序

1. 先掌握 RAG 离线/在线两阶段和 Embedding、VectorStore、Top-K、阈值。
2. 跑通文本入库 -> 向量检索 -> RAG 回答的最小闭环。
3. 加入 Tika 和 metadata，解决多格式与引用问题。
4. 加入宽召回 + Rerank，提高排序精度并实现降级。
5. 加入 BM25 + 向量 + RRF，覆盖精确词与语义查询。
6. 用 RocketMQ 解耦知识 CRUD 与向量同步，并补齐幂等、重试、DLQ 和对账。
7. 建立真实查询集和自动化接口测试，以指标驱动分块、阈值和排序调优。

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

本汇总在上述专题原文之外，另合并了 Word 讲稿独有的四块内容：服务边界与职责表（§2.1）、按现象定位的排障表（§12.1）、生产化检查清单（§十三）与推荐学习顺序（§十四）。需要逐行代码时，回看对应专题原文。
