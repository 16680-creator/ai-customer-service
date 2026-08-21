# 第3章：RAG 检索增强生成

> RAG 是当前 Java AI 岗位面试的**最高频考点**，几乎所有涉及 AI 的 Java 岗位都会深入考察。

---

## 3.1 RAG 基础原理

### Q1：什么是 RAG？它解决了 LLM 的什么问题？★★★★★

**参考答案：**

**RAG（Retrieval-Augmented Generation，检索增强生成）** 是一种结合外部知识检索与 LLM 生成的技术架构。

**核心流程：**
```
用户提问
   ↓
[1] 检索（Retrieval）：从知识库中检索相关文档片段
   ↓
[2] 增强（Augmented）：将检索结果注入到 Prompt 中
   ↓
[3] 生成（Generation）：LLM 基于检索到的上下文生成回答
```

**RAG 解决的核心问题：**

| 问题 | 说明 | RAG 如何解决 |
|------|------|-------------|
| 知识时效性 | LLM 训练数据有截止日期 | 实时检索最新文档 |
| 幻觉问题 | LLM 会编造不存在的信息 | 基于真实文档回答 |
| 私有知识 | LLM 不知道企业内部信息 | 检索企业知识库 |
| 可溯源 | 不知道答案来源 | 返回引用文档 |
| 成本 | 全量微调成本高 | 只需更新知识库 |

---

### Q2：RAG vs Fine-Tuning，什么时候用哪个？★★★★★

**参考答案：**

| 维度 | RAG | Fine-Tuning |
|------|-----|-------------|
| 知识更新 | 更新知识库即可，实时 | 需要重新训练 |
| 成本 | 低（无需 GPU 训练） | 高（需要 GPU + 标注数据） |
| 幻觉控制 | 好（有据可查） | 一般（模型仍可能编造） |
| 可溯源 | 是（可返回引用来源） | 否 |
| 知识容量 | 受检索质量限制 | 受模型容量限制 |
| 延迟 | 多一步检索延迟 | 无额外延迟 |
| 适用场景 | 知识库问答、文档搜索 | 特定领域风格、格式对齐 |

**最佳实践：两者结合**
- Fine-Tuning：让模型学会"怎么说话"（领域术语、回答风格）
- RAG：提供"说什么"（实时知识、私有数据）

---

## 3.2 文档处理流水线

### Q3：RAG 的文档处理流水线（Pipeline）包括哪些步骤？★★★★★

**参考答案：**

```
原始文档（PDF/Word/HTML/MD）
   ↓
[1] 文档读取（Document Reader）
   ├── PDF → Apache Tika / PyPDF
   ├── Word → Apache Tika / POI
   ├── HTML → Jsoup
   └── Markdown → 直接读取
   ↓
[2] 文档清洗
   ├── 去除页眉页脚、水印、广告
   ├── 统一编码格式
   └── 处理特殊字符
   ↓
[3] 文档分块（Chunking）★★★★★
   ├── 固定大小分块（TokenTextSplitter）
   ├── 按语义分块（按段落、标题）
   └── 递归分块（按层级分隔符）
   ↓
[4] 向量化（Embedding）
   └── 将文本块转为向量表示
   ↓
[5] 存储（Vector Store）
   └── 存入向量数据库（Milvus/PGVector/Chroma）
```

**Spring AI 实现：**
```java
// 1. 读取文档
var reader = new TikaDocumentReader(new FileSystemResource("docs/product.pdf"));
List<Document> documents = reader.get();

// 2. 文本分块
var splitter = new TokenTextSplitter(
    800,    // chunkSize：每块最大 token 数
    200,    // minChunkSize：最小块大小
    5,      // minChunkLengthChars：最小字符数
    10000,  // maxNumChunks：最大块数
    true    // keepSeparator：保留分隔符
);
List<Document> chunks = splitter.split(documents);

// 3. 存入向量数据库
vectorStore.add(chunks);
```

---

### Q4：文本分块（Chunking）策略有哪些？如何选择合适的分块大小？★★★★★

**参考答案：**

分块策略直接影响 RAG 的检索质量，是 RAG 工程中最关键的环节之一。

**常见分块策略：**

| 策略 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| 固定大小分块 | 按字符数/Token 数切割 | 简单高效 | 可能切断语义 |
| 递归字符分块 | 按分隔符层级递归切分 | 尽量保持语义完整 | 实现复杂 |
| 语义分块 | 基于语义相似度判断切割点 | 语义最完整 | 计算成本高 |
| 按结构分块 | 按标题/章节/段落切分 | 天然保持结构 | 依赖文档结构 |
| Markdown 分块 | 按 Markdown 标题层级切分 | 适合技术文档 | 仅限 Markdown |

**分块大小的选择（面试高频追问）：**

```
太小（< 200 tokens）：
├── 检索精度高（噪音少）
├── 但上下文不足（语义不完整）
└── LLM 生成质量差

太大（> 2000 tokens）：
├── 上下文充足
├── 检索精度低（噪音多）
├── 占用更多上下文窗口
└── Token 成本更高

推荐大小：
├── 一般场景：500~1000 tokens
├── FAQ/问答：200~500 tokens
├── 技术文档：800~1500 tokens
└── 法律合同：1000~2000 tokens
```

**重叠（Overlap）策略：**
- 相邻块保留 10%~20% 的重叠，避免关键信息被切断
- 例如 chunkSize=800, overlap=100

---

## 3.3 检索优化

### Q5：什么是混合检索（Hybrid Search）？为什么比纯向量检索效果好？★★★★★

**参考答案：**

**混合检索** = 向量检索（语义匹配）+ 关键词检索（BM25 精确匹配），通过融合排序获得更好的结果。

**为什么需要：**
- **向量检索**擅长语义理解："手机发热" 能匹配到 "温度过高"
- **关键词检索**擅长精确匹配：型号 "iPhone 16 Pro"、人名、专有名词
- 两者互补，单独用任何一种都有盲区

**Spring AI 实现：**
```java
// 混合检索配置
SearchRequest searchRequest = SearchRequest.builder()
    .query(userQuery)
    .topK(10)
    .similarityThreshold(0.5)
    .build();

// PGVector 支持混合检索
// 底层：向量余弦相似度 + BM25 全文检索 → RRF 融合排序
List<Document> results = vectorStore.similaritySearch(searchRequest);
```

**融合排序算法 - RRF（Reciprocal Rank Fusion）：**
\[
\text{RRF Score}(d) = \sum_{i} \frac{1}{k + \text{rank}_i(d)}
\]
其中 k 通常取 60，rank_i(d) 是文档 d 在第 i 个检索结果中的排名。

---

### Q6：什么是 Query Rewriting / Query Expansion？★★★★☆

**参考答案：**

用户的原始查询可能不适合直接用于检索（太模糊、太口语化、缺少关键词），需要改写。

**常见策略：**

```
原始查询："手机不行了"
   ↓
策略1：Query Rewriting（改写）
→ "手机故障排查 常见问题"

策略2：Query Expansion（扩展）
→ "手机故障" + "手机维修" + "手机问题"

策略3：HyDE（假设文档嵌入）
→ 先用 LLM 生成一个"假设性答案"
→ 用假设答案去检索（因为答案和文档的语义更接近）
```

**HyDE 实现示例：**
```java
// HyDE：Hypothetical Document Embeddings
String hypotheticalAnswer = chatClient.prompt()
    .user("请回答以下问题（不需要确认准确性）：" + userQuery)
    .call()
    .content();

// 用假设答案去检索，而不是用原始查询
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.builder().query(hypotheticalAnswer).topK(5).build()
);
```

---

### Q7：什么是 Rerank（重排序）？在 RAG 中如何使用？★★★★★

**参考答案：**

**Rerank** 是对初步检索结果进行二次精排序的过程。

**为什么需要：**
- 向量检索是"粗排"，可能包含不相关结果
- Rerank 模型（Cross-Encoder）对每对（query, document）做精细打分
- 精度远高于向量检索（但速度慢，所以只能对少量结果重排）

**流程：**
```
用户查询
   ↓
向量检索 → Top 20 结果（粗排，快）
   ↓
Rerank 模型 → Top 5 结果（精排，慢但准确）
   ↓
注入 Prompt → LLM 生成
```

**Spring AI 集成 Rerank：**
```java
// 使用 Cohere Rerank 或本地 Rerank 模型
RerankModel rerankModel = ...;

List<Document> initialResults = vectorStore.similaritySearch(
    SearchRequest.builder().query(query).topK(20).build()
);

// 重排序
List<Document> rerankedResults = rerankModel.rerank(
    initialResults, query, 5  // 取 top 5
);
```

**Rerank 模型选择：**
- Cohere Rerank API（效果好，需付费）
- BGE-Reranker（开源，中文效果好）
- Cohere Rerank 3（最新，效果最佳）

---

## 3.4 高级 RAG 技术

### Q8：什么是 Parent-Child Chunking（父子分块）？★★★★☆

**参考答案：**

**核心思想：** 用小块（Child）做检索，返回大块（Parent）给 LLM。

```
原始文档
   ↓
大块（Parent）：500~2000 tokens（提供完整上下文）
   ↓  ↓  ↓
小块（Child）：100~300 tokens（精确检索）
```

**优势：**
- 小块检索：精度高，噪声少
- 大块返回：上下文完整，LLM 生成质量好

**实现：**
```java
// 1. 创建大块
List<Document> parentDocs = new TokenTextSplitter(1500, 300, 5, 1000, true)
    .split(documents);

// 2. 为每个大块创建小块
List<Document> childDocs = new ArrayList<>();
for (Document parent : parentDocs) {
    List<Document> children = new TokenTextSplitter(300, 50, 5, 100, true)
        .split(List.of(parent));
    // 给小块标记 parent id
    children.forEach(child -> 
        child.getMetadata().put("parent_id", parent.getId()));
    childDocs.addAll(children);
}

// 3. 小块存入向量库
vectorStore.add(childDocs);

// 4. 检索时：匹配小块 → 返回对应大块
```

---

### Q9：如何处理表格、图片等非结构化内容？★★★☆☆

**参考答案：**

**表格处理：**
- 直接文本化：将表格转为 Markdown 表格或 CSV 格式
- 表格拆分：大表格按行/列拆分成多个小块
- 表格摘要：用 LLM 生成表格的自然语言描述
- 混合存储：表格原文 + 摘要同时存储

**图片处理：**
- OCR + 理解：先用 OCR 提取文字，再用 VLM 理解图片内容
- 图片描述：用 VLM（如 GPT-4V）生成图片的文本描述，存入知识库
- 多模态检索：使用 CLIP 等模型同时支持文本和图片检索

**代码处理：**
- 按函数/类为单位分块
- 保留 import 和注释作为上下文
- 使用代码专用 Embedding 模型（如 Voyage Code）

---

### Q10：RAG 系统的评估指标有哪些？如何评估 RAG 效果？★★★★★

**参考答案：**

RAG 评估分为**检索评估**和**生成评估**两部分。

**检索评估指标：**

| 指标 | 含义 | 计算方式 |
|------|------|----------|
| Recall@K | Top-K 结果中包含了多少正确答案 | 相关文档命中数 / 总相关文档数 |
| Precision@K | Top-K 结果中有多少是相关的 | 相关文档命中数 / K |
| MRR | 第一个相关结果的排名倒数 | 1 / rank_of_first_relevant |
| NDCG@K | 考虑排名位置的评估 | 加权相关度 / 理想排名 |

**生成评估指标：**

| 指标 | 含义 | 评估方式 |
|------|------|----------|
| Faithfulness（忠实度） | 回答是否基于检索到的文档 | 用 LLM 评估 |
| Answer Relevancy（答案相关性） | 回答是否回答了用户的问题 | 用 LLM 评估 |
| Context Relevancy（上下文相关性） | 检索的文档是否与问题相关 | 用 LLM 评估 |
| Hallucination Rate（幻觉率） | 回答中有多少内容是编造的 | 对比检索文档 |

**评估框架：**
- **RAGAS**（最主流）：自动化评估 RAG 的多个维度
- **TruLens**：可观测性 + 评估
- **LangSmith**：LangChain 生态的评估工具

---

## 3.5 RAG 生产实践

### Q11：RAG 系统上线后，效果不好怎么排查和优化？★★★★★

**参考答案：**

**排查框架（由上游到下游）：**

```
效果不好
├── 1. 检索质量问题（最常见）
│   ├── 分块不合理 → 调整 chunk size / overlap
│   ├── Embedding 模型不好 → 换模型（如 text-embedding-3-large）
│   ├── 查询和文档语义空间不同 → Query Rewriting / HyDE
│   ├── 缺少关键词匹配 → 加 BM25 混合检索
│   └── 排序不准 → 加 Rerank
│
├── 2. 上下文组装问题
│   ├── 检索到了但顺序不对 → 按原始顺序排列
│   ├── 信息分散在多块中 → 增大 topK 或用 Parent-Child
│   └── 太多不相关内容 → 降低 topK 或提高阈值
│
├── 3. 生成质量问题
│   ├── 幻觉 → 增强 system prompt 约束
│   ├── 不回答 → 调整 Temperature 或换模型
│   └── 格式不对 → 用结构化输出
│
└── 4. 数据质量问题
    ├── 文档质量差 → 清洗预处理
    ├── 知识库覆盖不足 → 补充文档
    └── 知识冲突 → 去重 + 版本管理
```

---

### Q12：如何设计一个支持增量更新的 RAG 知识库？★★★★☆

**参考答案：**

生产环境的知识库需要支持文档的增删改，而非每次全量重建。

**设计方案：**

```java
// 文档元数据记录来源和版本
Map<String, Object> metadata = new HashMap<>();
metadata.put("source", "product_manual_v2.pdf");
metadata.put("page", 3);
metadata.put("upload_time", LocalDateTime.now());
metadata.put("doc_id", "doc_12345");

// 增量更新流程：
// 1. 删除旧文档的所有 chunk
vectorStore.delete(filterExpression("doc_id == 'doc_12345'"));

// 2. 重新处理并添加新版本文档
List<Document> newChunks = processAndChunk(newDocument);
vectorStore.add(newChunks);

// 3. 记录更新日志
auditLog.record("doc_12345", "updated", LocalDateTime.now());
```

**关键设计：**
- 每个 chunk 的 metadata 中标记 `doc_id`，支持按文档删除
- 文件变更监听（RocketMQ 消息通知 / 文件 watcher）
- 定期清理孤立向量（文档已删除但向量还在）

---

## 本章小结

**必背 TOP 5：**
1. RAG 完整流程和解决的问题
2. 文本分块策略（Chunking）选型
3. 混合检索（向量 + BM25）+ Rerank
4. RAG 评估指标（Recall/Faithfulness/Hallucination Rate）
5. RAG 效果排查框架
