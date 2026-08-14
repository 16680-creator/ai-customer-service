# 数据模型设计：RAG 进阶六件套

> 第 1 阶段输出（/speckit-plan）。实体分三类：评估、检索、运营。

## 一、评估体系实体（ai-cs-chat/rag/eval）

### GoldenCase（golden 测试集条目，JSON 文件）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 是 | 用例 ID |
| question | String | 是 | 用户问题 |
| knowledgeBase | String | 是 | 知识库标识 |
| expectedDocumentIds | List<String> | 否 | 期望命中的文档 ID（用于 Recall/MRR） |
| referenceAnswer | String | 否 | 参考答案（用于 LLM Judge） |
| expectedKeywords | List<String> | 否 | 期望包含的关键词（回答质量辅助判据） |

### RetrievalMetrics（检索指标，纯计算）

| 字段 | 类型 | 说明 |
|------|------|------|
| recallAtK | double | Recall@k：命中期望文档数 / 期望文档总数 |
| mrr | double | 首个命中期望文档的倒数排名 |
| hitRate | double | 至少命中一条期望文档的用例占比 |
| retrievedCount | int | 检索返回条数 |

### RagEvalCaseResult（单条评估结果）

| 字段 | 类型 | 说明 |
|------|------|------|
| goldenCaseId | String | 用例 ID |
| question | String | 问题 |
| retrievedDocumentIds | List<String> | 检索命中文档 ID |
| recallAtK / mrr / hit | double/boolean | 单条指标 |
| llmScore | Integer | LLM-as-Judge 分数（1-5，可为空） |
| answer | String | 生成的回答（可为空） |

### RagEvalReport（评估报告）

| 字段 | 类型 | 说明 |
|------|------|------|
| evalId | String | 报告 ID（时间戳） |
| retrievalMode | String | 检索模式（VECTOR/HYBRID/...） |
| topK | int | 检索 Top-K |
| metrics | RetrievalMetrics | 汇总检索指标 |
| avgLlmScore | Double | LLM 均分（可为空） |
| caseResults | List<RagEvalCaseResult> | 逐条明细 |
| passed | boolean | 是否通过阈值门禁 |
| executedAt | Instant | 执行时间 |

## 二、检索增强实体（ai-cs-chat/rag/retrieve、rewrite、graph）

### RewriteResult（查询改写结果）

| 字段 | 类型 | 说明 |
|------|------|------|
| originalQuery | String | 原始问题 |
| subQueries | List<String> | 改写后的子查询列表 |
| hydeDocument | String | 假设性文档（HyDE），可为空 |

### RetrieveResult（统一检索结果）

| 字段 | 类型 | 说明 |
|------|------|------|
| query | String | 实际执行的查询 |
| documents | List<Document> | 命中文档（含 metadata.knowledgeBase/documentId/title/page） |
| mode | String | 实际执行的检索模式 |
| degraded | boolean | 是否发生降级 |
| degradeReason | String | 降级原因 |

### GraphTriple（图谱三元组）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键（InMemory 自增 / DB 自增） |
| subject | String | 主体实体（如"退款政策"） |
| predicate | String | 关系（如"指向"、"依赖"） |
| object | String | 客体实体（如"申请入口"） |
| knowledgeBase | String | 知识库标识 |
| sourceDocumentId | Long | 来源文档 ID（可空） |

### GraphHit（图谱命中）

| 字段 | 类型 | 说明 |
|------|------|------|
| entity | String | 命中实体 |
| triples | List<GraphTriple> | 多跳展开的三元组 |
| depth | int | 展开深度 |

## 三、问数图表实体（ai-cs-chat/nl2sql）

### ChartAnswer（问数回答）

| 字段 | 类型 | 说明 |
|------|------|------|
| question | String | 原始问题 |
| conclusion | String | 自然语言结论 |
| chartType | ChartType | PIE/BAR/LINE/NONE |
| echartsOption | Map<String,Object> | ECharts option（chartType=NONE 时为空） |
| rows | List<Map<String,Object>> | 原始数据行 |
| degraded | boolean | 结论是否降级为模板生成 |

## 四、运营闭环实体（ai-cs-knowledge/ops）

### ClusterTopic（聚类主题）

| 字段 | 类型 | 说明 |
|------|------|------|
| topic | String | 主题名（代表问题或 LLM 归纳） |
| questionIds | List<Long> | 成员提问 ID |
| count | int | 成员数 |
| ratio | double | 占比 |
| representativeQuestions | List<String> | 代表问题（Top-3 高频） |
| gapFlag | boolean | 是否知识库缺口 |
| hitRate | Double | 主题内知识库命中率（可空） |

### ClusterReport（聚类报告）

| 字段 | 类型 | 说明 |
|------|------|------|
| period | String | 统计周期 |
| totalQuestions | int | 总提问数 |
| topics | List<ClusterTopic> | 主题列表 |
| gapTopics | List<ClusterTopic> | 缺口主题 |
| status | String | OK / INSUFFICIENT_DATA |

### FaqSuggestion（FAQ 收录建议）

| 字段 | 类型 | 说明 |
|------|------|------|
| question | String | FAQ 问题（代表问题） |
| answer | String | FAQ 答案（取自命中知识片段或运营填写） |
| knowledgeBase | String | 知识库标识（默认 faq） |
| clusterTopicId | String | 来源主题 ID |

## 五、存储设计

### MySQL 新增表（ai-cs-knowledge 库）

```sql
-- 知识图谱三元组（GraphRAG）
CREATE TABLE IF NOT EXISTS kb_graph_triple (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    predicate VARCHAR(128) NOT NULL,
    object VARCHAR(255) NOT NULL,
    knowledge_base VARCHAR(64) NOT NULL,
    source_document_id BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_kb (knowledge_base),
    KEY idx_subject (subject)
) COMMENT='知识图谱三元组';

-- FAQ 条目（反哺知识库）
CREATE TABLE IF NOT EXISTS kb_faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(512) NOT NULL,
    answer TEXT NOT NULL,
    knowledge_base VARCHAR(64) DEFAULT 'faq',
    topic_id VARCHAR(64) NULL,
    status VARCHAR(16) DEFAULT 'DRAFT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='FAQ 条目';
```

### 评估/改写/HyDE 数据不落库（临时计算），图表配置不落库（随响应返回）

## 六、关系说明

- GoldenCase → RagEvalCaseResult：1 对 1（评估时逐条）
- ClusterTopic → FaqSuggestion：1 对 1（运营采纳后生成）
- FaqSuggestion → KnowledgeDocument：1 对 1（复用知识文档链路向量化）
- GraphTriple 通过 subject/object 连接构成图；多跳查询按 subject → object BFS 展开
