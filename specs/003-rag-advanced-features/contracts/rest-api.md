# REST API 契约：RAG 进阶六件套

> 第 1 阶段输出（/speckit-plan）。所有响应统一包装 `Result<T>`（code/data/message）。

## 一、ai-cs-chat（端口 8083，网关前缀 /chat）

### 1.1 检索测试接口（US2/US3 独立测试用）

`GET /chat/retrieve/test`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| knowledgeBase | String | 是 | 知识库标识 |
| query | String | 是 | 检索问题 |
| mode | String | 否 | VECTOR / HYBRID / HYBRID_QUERY_REWRITE，默认 VECTOR |
| topK | int | 否 | 默认 5 |

响应 `data`：

```json
{
  "query": "ABC-123 保修多久",
  "mode": "HYBRID",
  "degraded": false,
  "documents": [
    { "id": "doc-1", "text": "型号 ABC-123 保修 1 年...", "score": 0.89,
      "metadata": { "knowledgeBase": "product-manual", "documentId": "1", "title": "保修政策", "page_number": 3 } }
  ]
}
```

### 1.2 RAG 对话扩展（US2）

`POST /chat/rag` 与 `POST /chat/stream/sse` 新增可选参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| hybrid | boolean | 否 | 默认 false；true 时检索走 ES+向量 RRF 混合 |
| rewrite | boolean | 否 | 默认 false；true 时先查询改写（含 HyDE）再检索 |

行为：hybrid=false 时保持现有纯向量行为（存量兼容）；任一增强降级时 degraded=true 但回答不中断。

### 1.3 问数图表生成（US5）

`POST /chat/chart`

请求体：

```json
{
  "question": "各分类销量分布",
  "rows": [
    { "category": "手机", "sales": 1200 },
    { "category": "平板", "sales": 800 }
  ]
}
```

响应 `data`：

```json
{
  "question": "各分类销量分布",
  "conclusion": "手机销量最高（1200），其次为平板（800）。",
  "chartType": "PIE",
  "echartsOption": { "series": [{ "type": "pie", "data": [{ "name": "手机", "value": 1200 }] }] },
  "rows": [ { "category": "手机", "sales": 1200 } ],
  "degraded": false
}
```

### 1.4 图谱三元组管理（US4）

- `POST /rag/graph/triple`：新增三元组
  请求体：`{ "subject": "退款政策", "predicate": "指向", "object": "申请入口", "knowledgeBase": "product-manual", "sourceDocumentId": 1 }`
  响应：`{ "id": 1 }`
- `GET /rag/graph/query?entity=退款政策&depth=2&knowledgeBase=product-manual`
  响应：`{ "entity": "退款政策", "triples": [...], "depth": 2 }`
- `GET /rag/graph/triples?knowledgeBase=product-manual`：列出三元组（分页）

### 1.5 评估接口（US1）

`POST /rag/eval/run`

请求体：

```json
{
  "goldenSetPath": "classpath:eval/golden-set.json",
  "knowledgeBase": "product-manual",
  "mode": "VECTOR",
  "topK": 5,
  "llmScoreThreshold": 3.5,
  "hitRateThreshold": 0.6
}
```

响应 `data`（RagEvalReport 摘要）：

```json
{
  "evalId": "20260812-100000",
  "retrievalMode": "VECTOR",
  "topK": 5,
  "metrics": { "recallAtK": 0.75, "mrr": 0.8, "hitRate": 0.85 },
  "avgLlmScore": 4.1,
  "passed": true,
  "caseResults": [ ... ]
}
```

> CI 门禁：`passed=false` 时 HTTP 200 但 data.passed=false；Maven 测试 profile `-Peval` 运行评估测试类，失败即构建失败。

## 二、ai-cs-knowledge（端口 8082，网关前缀 /knowledge）

### 2.1 聚类与缺口分析（US6）

`POST /knowledge/ops/cluster`

请求体：

```json
{
  "period": "2026-08-01~2026-08-12",
  "questions": [ { "id": 1, "text": "怎么退款？" } ],
  "gapHitRateThreshold": 0.4
}
```

响应 `data`（ClusterReport）：topics[]（topic/count/ratio/gapFlag/representativeQuestions）+ gapTopics[] + status。

### 2.2 最新聚类报告（US6 看板）

`GET /knowledge/ops/cluster/report?period=2026-08-01~2026-08-12` → ClusterReport

### 2.3 FAQ 收录（US6）

`POST /knowledge/ops/faq`

请求体：

```json
{
  "question": "怎么申请退款？",
  "answer": "进入订单详情页点击申请退款...",
  "knowledgeBase": "faq",
  "clusterTopicId": "topic-3"
}
```

响应：`{ "faqId": 1, "vectorized": true }`（复用知识文档链路异步向量化）

## 三、ai-cs-frontend

| 页面 | 路由 | 说明 |
|------|------|------|
| AI 数据看板 | /chat-dashboard | 问数结果 + ECharts 图表渲染（echarts 依赖） |
| 知识库运营看板 | /knowledge-ops | 聚类主题列表、缺口标记、FAQ 一键收录 |

## 四、契约约束

- 所有新增参数均有默认值，存量调用零破坏
- 检索/评估/图表/聚类接口均返回 Result<T>，异常由 GlobalExceptionHandler 统一处理
- 前端图表仅消费 `echartsOption` 标准配置，不感知后端生成细节
