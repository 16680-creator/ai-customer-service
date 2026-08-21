# 第4章：向量数据库与 Embedding

> 向量数据库是 RAG 系统的核心基础设施，面试中会重点考察原理理解和选型能力。

---

## 4.1 Embedding（文本向量化）

### Q1：什么是 Embedding？文本 Embedding 的原理是什么？★★★★★

**参考答案：**

**Embedding** 是将高维离散数据（如文本）映射到低维连续向量空间的过程。语义相近的文本在向量空间中距离更近。

```
"苹果手机发热" → [0.23, -0.45, 0.67, ..., 0.12]  (1536维)
"iPhone温度过高" → [0.21, -0.43, 0.65, ..., 0.11]  (距离很近)
"今天天气不错"   → [-0.87, 0.33, -0.12, ..., 0.55]  (距离很远)
```

**Embedding 模型原理：**
1. 输入文本经过 Tokenization
2. Token 序列输入 Transformer Encoder
3. 对最后一层隐藏状态做 Pooling（通常是 Mean Pooling）
4. 输出固定维度的向量（通常 768~3072 维）

**Spring AI 中使用 Embedding：**
```java
// 注入 EmbeddingModel
@Autowired
private EmbeddingModel embeddingModel;

// 文本转向量
float[] vector = embeddingModel.embed("苹果手机发热");

// 文档批量向量化
List<Document> docs = ...;
embeddingModel.embed(docs);  // 自动设置每个文档的 embedding
```

---

### Q2：常见的 Embedding 模型有哪些？如何选择？★★★★★

**参考答案：**

| 模型 | 提供商 | 维度 | 中文效果 | 价格 | 最大 Token |
|------|--------|------|----------|------|-----------|
| text-embedding-3-small | OpenAI | 1536 | 好 | $0.02/1M tokens | 8191 |
| text-embedding-3-large | OpenAI | 3072 | 很好 | $0.13/1M tokens | 8191 |
| bge-large-zh-v1.5 | BAAI（开源） | 1024 | 优秀 | 免费（本地部署） | 512 |
| bge-m3 | BAAI（开源） | 1024 | 优秀 | 免费 | 8192 |
| text2vec-large-chinese | 开源 | 1024 | 好 | 免费 | 512 |
| voyage-3 | Voyage AI | 1024 | 好 | $0.06/1M tokens | 32000 |
| m3e-base | 开源 | 768 | 好 | 免费 | 512 |

**选型决策：**
- **预算充足 + 简单集成** → text-embedding-3-large
- **中文效果优先 + 本地部署** → bge-large-zh-v1.5 或 bge-m3
- **长文本** → voyage-3（32K tokens）或 bge-m3（8K tokens）
- **成本敏感** → text-embedding-3-small 或 开源模型

**面试追问 - 如何评估 Embedding 模型？**
- MTEB（Massive Text Embedding Benchmark）排行榜
- 关注指标：检索召回率、语义相似度准确率
- 中文场景参考 C-MTEB 排行榜

---

### Q3：什么是向量相似度？余弦相似度、欧氏距离、点积有什么区别？★★★★★

**参考答案：**

| 度量方式 | 公式 | 特点 | 适用场景 |
|----------|------|------|----------|
| 余弦相似度 | cos(θ) = A·B / (‖A‖·‖B‖) | 只看方向不看大小，[-1,1] | 文本语义相似度（最常用） |
| 欧氏距离 | d = √Σ(a_i - b_i)² | 考虑向量大小，[0,∞) | 数值特征距离 |
| 点积（内积） | A·B = Σ a_i·b_i | 同时考虑方向和大小 | 归一化向量等同余弦 |

**为什么 RAG 最常用余弦相似度？**
- 文本 Embedding 通常做过归一化，此时余弦相似度 = 点积
- 不受向量绝对大小影响，更关注语义方向
- 值域固定 [-1, 1]，便于设置阈值

**Spring AI 中的使用：**
```java
// 大多数 VectorStore 默认使用余弦相似度
SearchRequest request = SearchRequest.builder()
    .query("手机发热")
    .topK(5)
    .similarityThreshold(0.7)  // 余弦相似度阈值
    .build();
```

---

## 4.2 向量数据库

### Q4：主流向量数据库对比，如何选型？★★★★★

**参考答案：**

| 数据库 | 类型 | 最大规模 | 性能 | 生态集成 | 适用场景 |
|--------|------|----------|------|----------|----------|
| Milvus | 专用向量数据库 | 十亿级 | 极高 | Spring AI 支持 | 大规模生产环境 |
| PGVector | PG 扩展 | 百万级 | 中高 | Spring AI 支持 | 已有 PG 的团队 |
| Chroma | 轻量级 | 十万级 | 中 | Spring AI 支持 | 原型/小规模 |
| Weaviate | 专用向量数据库 | 十亿级 | 高 | Spring AI 支持 | 多模态检索 |
| Qdrant | 专用向量数据库 | 十亿级 | 高 | Spring AI 支持 | 过滤查询强 |
| Elasticsearch 8.x | 搜索引擎+向量 | 十亿级 | 高 | Spring AI 支持 | 混合检索 |
| Redis | 内存数据库+向量 | 百万级 | 极高 | Spring AI 支持 | 低延迟场景 |

**选型决策树：**
```
已有 PostgreSQL？
├── 是，数据量 < 100万 → PGVector（零额外运维）
└── 否
    ├── 数据量 > 1000万 → Milvus / Qdrant
    ├── 需要全文检索+向量 → Elasticsearch 8.x
    ├── 快速原型/小规模 → Chroma
    └── 极低延迟 → Redis Stack
```

---

### Q5：向量数据库的索引算法有哪些？各自的优缺点？★★★★☆

**参考答案：**

向量检索的核心挑战是：在海量向量中快速找到最相似的 Top-K，暴力遍历不可行。

| 算法 | 原理 | 精度 | 速度 | 内存 | 适用规模 |
|------|------|------|------|------|----------|
| Flat（暴力） | 逐一计算距离 | 100% | 慢 | 低 | <10万 |
| IVF（倒排文件） | 聚类后只搜索最近的几个簇 | 高 | 快 | 中 | 百万级 |
| HNSW（分层小世界图） | 多层图结构，逐层缩小搜索范围 | 很高 | 很快 | 高 | 百万~千万级 |
| PQ（乘积量化） | 将向量压缩为短码 | 中 | 很快 | 很低 | 十亿级 |
| ScaNN | Google 优化的 ANN 算法 | 很高 | 很快 | 中 | 大规模 |

**HNSW 详解（面试最常考）：**
```
第3层（最稀疏）：    A -------- D            （快速跳转）
第2层：            A --- B --- D --- F       （中等精度）
第1层（最密集）：  A-B-C-D-E-F-G-H-I-J      （精确搜索）
```
1. 从最高层开始，贪心搜索找到最近节点
2. 下降到下一层，从对应位置继续搜索
3. 在最底层做精细搜索

**参数：**
- `M`：每个节点的最大连接数（越大精度越高，内存越大）
- `efConstruction`：构建索引时的搜索宽度（越大索引质量越高）
- `efSearch`：查询时的搜索宽度（越大精度越高，速度越慢）

---

### Q6：PGVector 的实际使用经验和注意事项？★★★★☆

**参考答案：**

PGVector 是 PostgreSQL 的向量扩展，对已有 PG 技术栈的团队非常友好。

**优势：**
- 无需额外部署和维护向量数据库
- 向量检索和关系查询在同一事务中
- 支持混合查询（SQL + 向量）
- IVFFlat 和 HNSW 两种索引

**Spring AI 集成：**
```java
// 配置
spring:
  ai:
    vectorstore:
      pgvector:
        dimensions: 1536
        distance-type: COSINE_DISTANCE
        index-type: HNSW

// 使用
@Autowired
private VectorStore vectorStore;

// 存储
vectorStore.add(documents);

// 检索
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query("手机发热怎么办")
        .topK(5)
        .similarityThreshold(0.7)
        .build()
);
```

**生产注意事项：**
- 大数据量（>100万）需要创建 HNSW 索引
- 定期 VACUUM 维护
- 连接池配置（向量查询较耗连接）
- 考虑读写分离（写入向量库不影响业务查询）

---

### Q7：什么是向量数据库的 Metadata Filtering（元数据过滤）？★★★★☆

**参考答案：**

Metadata Filtering 是在向量检索时，通过附加条件过滤文档，提高检索精度。

```java
// 只搜索特定类别和时间的文档
SearchRequest request = SearchRequest.builder()
    .query("退货政策")
    .topK(5)
    .filterExpression(
        "category == 'after-sales' AND created_time > '2024-01-01'"
    )
    .build();

List<Document> results = vectorStore.similaritySearch(request);
```

**典型使用场景：**
- 多租户隔离：`tenant_id == 'tenant_123'`
- 文档分类过滤：`category == 'product-manual'`
- 时间范围过滤：`created_time > '2024-01-01'`
- 权限控制：`access_level <= 3`

**底层实现：** Pre-filtering（先过滤再搜索）vs Post-filtering（先搜索再过滤）
- Pre-filtering：准确性高但可能结果不足
- Post-filtering：速度快但可能过滤掉好的结果
- 大多数数据库支持两种，可按需选择

---

## 4.3 Embedding 进阶

### Q8：Embedding 模型的维度越高越好吗？★★★☆☆

**参考答案：**

**不是**，维度需要平衡效果和成本：

| 维度 | 效果 | 存储成本 | 检索速度 | 典型模型 |
|------|------|----------|----------|----------|
| 256~512 | 一般 | 低 | 快 | 小模型 |
| 768~1024 | 好 | 中 | 中 | BGE、M3E |
| 1536 | 很好 | 中高 | 中 | OpenAI small |
| 3072 | 最好 | 高 | 慢 | OpenAI large |

**OpenAI 的维度缩减特性：**
text-embedding-3 系列支持 Matryoshka Representation Learning，可以将 3072 维向量截断为任意低维度（如 256/512/1024），效果平滑下降。

```java
// 将 3072 维截断为 1024 维，存储成本降低 3 倍，效果仅降 ~2%
float[] truncated = Arrays.copyOf(fullVector, 1024);
```

---

### Q9：如何处理 Embedding 模型的跨语言问题？★★★☆☆

**参考答案：**

**问题：** 中英文文本在向量空间中可能距离较远，即使语义相同。

**解决方案：**

1. **使用多语言模型**：bge-m3、multilingual-e5 等支持 100+ 语言
2. **统一语言检索**：将文档翻译为英文后 Embedding，查询也翻译为英文
3. **语言对齐微调**：用平行语料（中英文对照）微调 Embedding 模型
4. **查询扩展**：同时用中英文查询，合并结果

---

## 本章小结

**必背 TOP 5：**
1. Embedding 原理和常见模型选型
2. 余弦相似度 vs 欧氏距离 vs 点积
3. 向量数据库选型（尤其 PGVector vs Milvus）
4. HNSW 索引算法原理
5. Metadata Filtering 应用场景
