# 数据模型：RAG 检索质量升级

> 生成于 `/speckit-plan` 第 1 阶段（2026-08-12）

---

## 1. JSON 模型（不涉及新数据库表）

本功能不新增数据库表。ES 索引结构、MQ 消息体、代码 DTO 如下。

---

## 2. Elasticsearch 索引：`knowledge_docs`

### 映射（Mappings）

```json
{
  "mappings": {
    "properties": {
      "documentId":    { "type": "keyword" },
      "knowledgeBase": { "type": "keyword" },
      "title":         { "type": "text", "analyzer": "standard", "boost": 2.0 },
      "content":       { "type": "text", "analyzer": "standard" },
      "page":          { "type": "integer" },
      "docType":       { "type": "keyword" },
      "tags":          { "type": "keyword" },
      "categoryId":    { "type": "keyword" },
      "createdAt":     { "type": "date", "format": "epoch_millis" }
    }
  }
}
```

### 新建索引（幂等）

```http
PUT /knowledge_docs
{ 以上 mappings }
```

### 删除索引

```http
DELETE /knowledge_docs
```

---

## 3. RocketMQ 消息模型

### Topic
`knowledge-doc-sync-topic`

### Tags
`CREATE` / `UPDATE` / `DELETE`

### 消息体（KnowledgeSyncMessage）

| 字段 | 类型 | 说明 |
|------|------|------|
| action | String | CREATE / UPDATE / DELETE |
| documentId | Long | 知识文档 ID |
| knowledgeBase | String | 知识库标识（默认 "knowledge"） |
| title | String | 文档标题 |
| content | String | 文档文本内容（全文） |
| timestamp | Long | 消息产生时间（epoch millis） |

---

## 4. 代码 DTO

### ChatRagResponseDTO（ai-cs-chat.dto）

| 字段 | 类型 | 说明 |
|------|------|------|
| content | String | AI 回答文本 |
| citations | List\<CitationItemDTO> | 引用列表（无命中时空列表） |

### CitationItemDTO（ai-cs-chat.dto）

| 字段 | 类型 | 说明 |
|------|------|------|
| documentId | Long | 命中文档 ID |
| title | String | 文档标题 |
| page | Integer | 页码（PDF 有值，其他 null） |
| score | Double | 精排分数（rerank relevance_score） |
| content | String | 文档内容片段（前 200 字） |

### RerankRequest/RerankResponse（ai-cs-chat.rag.rerank，内部 DTO，不对外）

```java
// 请求
record RerankRequest(String model, String query, List<String> documents, int topN, boolean returnDocuments)

// 响应
record RerankResponse(List<RerankResult> results)
record RerankResult(int index, double relevanceScore, RerankDocument document)
record RerankDocument(String text)
```

### HybridSearchResult（ai-cs-search.hybrid）

| 字段 | 类型 | 说明 |
|------|------|------|
| documentId | String | 文档标识 |
| title | String | 文档标题 |
| content | String | 文档内容 |
| score | Double | RRF 融合分数 |
| esRank | Integer | ES 路排名（无此路为 -1） |
| vectorRank | Integer | 向量路排名（无此路为 -1） |
| knowledgeBase | String | 知识库标识 |
| page | Integer | 页码 |
| docType | String | 文档类型 |