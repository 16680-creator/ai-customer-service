# 技术调研报告：RAG 检索质量升级五件套

> 生成于 `/speckit-plan` 第 0 阶段（2026-08-12）
> 目的：解决 plan.md 技术上下文中的全部决策点，为设计与实现提供依据。

---

## 1. Rerank 重排序服务选型

### 决策
采用**硅基流动（SiliconFlow）`BAAI/bge-reranker-v2-m3`**，通过 Spring `RestClient` 直调 HTTP API，在 `ai-cs-chat` 模块自研 `RerankService` 接口 + `SiliconFlowRerankService` 实现。

### Rationale（理由）
- 项目已使用硅基流动的 bge-m3 embedding（`aics.embedding.base-url=https://api.siliconflow.cn`），同一供应商、同一 API Key 体系，接入成本最低
- Spring AI 1.1.4 无内置 Reranker 抽象，自定义接口最轻量且便于替换
- bge-reranker-v2-m3 是中文 RAG 场景成熟精排模型，与 bge-m3 embedding 同族，语义空间一致

### 关键技术点（已确认 API 契约）
- 端点：`POST https://api.siliconflow.cn/v1/rerank`
- 认证：`Authorization: Bearer <API_KEY>`
- 请求体：`{"model": "BAAI/bge-reranker-v2-m3", "query": "...", "documents": ["..."], "top_n": 5, "return_documents": true}`
- 响应体：`{"results": [{"index": 0, "relevance_score": 0.98, "document": {"text": "..."}}], "usage": {...}}`
- 超时：连接/读取均设 5s；失败或超时 → 捕获异常降级为向量原始排序（FR-002）

### Alternatives considered（备选）
| 方案 | 结论 | 原因 |
|------|------|------|
| Cohere Rerank | 否决 | 需新供应商、新 Key，中文效果不如 bge-reranker |
| 本地 ONNX 模型 | 否决 | 引入 Python/ONNX Runtime 运维负担，与 Java 单体部署冲突 |
| 用 LLM 打分排序 | 否决 | 成本高、延迟高，精排质量不如专用 reranker |

---

## 2. 混合检索（ES BM25 + 向量 + RRF）

### 决策
在 `ai-cs-search` 模块新增 **ES BM25 关键词检索 + Chroma 向量语义检索双路召回**，用 **RRF（Reciprocal Rank Fusion）** 融合排序。ES 使用 `co.elastic.clients:elasticsearch-java` 8.12.2（父 POM 已管版本）。

### Rationale
- 项目曾用 ES 全文检索（后切 Chroma），ES 8.12 仍在基础设施中（docker-compose 已部署），elasticsearch-java 依赖已在父 POM dependencyManagement
- BM25 对型号/订单号等精确串命中强，向量对语义查询强，RRF 无需训练、无需调权重即可融合异构排序
- Chroma 是 Spring AI VectorStore 抽象，ES 侧只需维护一个关键词索引，两者数据同源（知识文档）

### 关键技术点
- **ES 索引**：`knowledge_docs`，字段：`documentId`(keyword)、`knowledgeBase`(keyword)、`title`(text)、`content`(text, analyzer=standard)、`page`(integer)；查询用 `multi_match`（title 权重 2、content 权重 1）
- **写入链路**：`SearchServiceImpl.indexDocument()` 在写 Chroma 的同时写 ES（先删同 documentId 旧文档再插入，保证幂等）；`deleteIndex()` 同时清 ES
- **RRF 公式**：`score(d) = Σ 1/(k + rank(d))`，k=60（标准默认，可配置）；每路取 Top-20，融合后取 Top-10
- **降级**：ES 异常 → 仅返回向量结果；Chroma 异常 → 仅返回 ES 结果（FR-004）
- **ES 连接**：`spring.elasticsearch.uris` 走 Nacos（aics-shared.yml 或 ai-cs-search.yml），本地兜底 `http://localhost:9200`

### Alternatives considered
| 方案 | 结论 | 原因 |
|------|------|------|
| 仅向量 + filter 硬匹配 | 否决 | 无法做模糊关键词召回，精确串弱匹配场景覆盖不足 |
| 向量库原生混合（如 Chroma 无 BM25） | 否决 | Chroma 不提供 BM25，必须引入外部全文检索引擎 |
| 权重线性加权融合 | 否决 | 需调权重、跨异构分数不可比；RRF 免调参且鲁棒 |

---

## 3. 引用溯源（Citation）

### 决策
后端在 RAG 回答响应中附带 `citations` 数组（引用项：documentId/title/page/score/content），**流式接口在 `done` 事件中携带**，前端 ChatView 渲染引用卡片。

### Rationale
- 客服回答必须可查证；引用元数据在检索阶段即可获得，成本极低
- SSE 流式已是前端主链路，`done` 事件已有 `content` 字段，扩展 `citations` 字段对现有解析逻辑零破坏
- `PagePdfDocumentReader` 分页读取时 metadata 已含 `page_number`（0-based，展示 +1）；Tika/文本块无页码则置 null

### 关键技术点
- `ChatServiceImpl.chatWithRag()` 返回类型 `Result<String>` → `Result<ChatRagResponseDTO>`（content + citations），Controller 同步调整
- `chatStreamSse()` RAG 分支：检索后缓存命中文档列表，`done` 事件发送 `{content, done:true, citations:[...]}`
- 引用项只保留精排 Top-5 中通过阈值过滤的文档

---

## 4. 文档格式扩展（Apache Tika）

### 决策
`ai-cs-chat` 引入 `spring-ai-tika-document-reader`（Spring AI 官方 DocumentReader 实现，封装 Apache Tika 2.x），`DocumentLoader` 新增 `loadTika()`，`KnowledgeBaseService.addFile()` 按扩展名路由解析器。

### Rationale
- 宪法第17条"技术优先"：Spring AI 官方能力优先于自研解析
- Tika 自动探测文件类型，docx/xlsx/html/md 统一解析为纯文本，新增格式零代码
- 保留 PDF 走 `PagePdfDocumentReader`（保留页码 metadata，服务引用溯源），TXT/MD 走 `TextReader`（轻量），其余走 Tika

### 关键技术点
- artifact：`org.springframework.ai:spring-ai-tika-document-reader`（spring-ai-bom 1.1.4 已管版本，无需声明版本号）
- 类：`org.springframework.ai.reader.tika.TikaDocumentReader`，实现 `AutoCloseable`，必须 try-with-resources
- 扩展名路由：`.pdf` → loadPdf；`.txt`/`.md` → loadText；`.docx`/`.xlsx`/`.html`/`.htm` → loadTika；其他 → BusinessException 明确提示（FR-009）
- xlsx 表格内容由 Tika 转为文本（单元格按行拼接），入库后以文本形式检索

---

## 5. 知识库增量同步（RocketMQ）

### 决策
`ai-cs-knowledge` 引入 `rocketmq-spring-boot-starter`，文档 CRUD 后由 `KnowledgeSyncProducer` 发送变更消息（topic: `knowledge-doc-sync-topic`，tag: `CREATE`/`UPDATE`/`DELETE`），同模块 `KnowledgeSyncConsumer` 异步消费执行向量化/清理。

### Rationale
- 同模块生产+消费实现"DB 事务与向量化解耦"：createDocument 的 DB insert 不再被向量化耗时阻塞（大文档 embedding 可达数秒）
- RocketMQ 消息具备重试能力（消费失败自动重投），天然提供补偿机制
- 消息体携带文档全文（content），消费端无需回源 DB，降低耦合（spec 假设已确认）

### 关键技术点
- 消息体：`KnowledgeSyncMessage{action, documentId, knowledgeBase, title, content}`（JSON 序列化）
- 幂等：CREATE/UPDATE 采用"先按 documentId 删除旧向量，再写入新向量"；DELETE 重复执行无副作用（FR-011）
- `KnowledgeVectorService.vectorize()` 改造：metadata 补充 `documentId`、`title`（当前仅有 knowledgeBase），新增 `deleteByDocumentId()`
- 发送失败不阻断主流程：捕获异常记日志告警（FR-012 相关，DB 操作先行）
- 消费组：`knowledge-sync-group`；`@RocketMQMessageListener(topic = "knowledge-doc-sync-topic", consumerGroup = "knowledge-sync-group")`
- 删除文档时 KnowledgeServiceImpl 需回填 documentId 用于向量清理：先查实体再删（selectById → delete → 发 DELETE 消息）

---

## 6. 降级与失败策略汇总

| 场景 | 行为 |
|------|------|
| Rerank API 超时/5xx/解析失败 | 记录 warn 日志，返回向量召回原始排序（Top-5） |
| ES 不可用 | 混合检索仅返回向量结果（记录 warn） |
| Chroma 不可用 | 混合检索仅返回 ES 结果（记录 warn） |
| MQ 发送失败 | 文档 CRUD 正常返回，告警日志，向量同步缺失可后续补偿 |
| Tika 解析失败 | 返回明确错误信息，不影响其他文件 |

---

## 7. 配置项清单（新增，全部走 Nacos/环境变量）

```yaml
# ai-cs-chat.yml
aics:
  rerank:
    base-url: https://api.siliconflow.cn
    api-key: ${aics.openai.api-key:}      # 与 embedding 同 Key
    model: BAAI/bge-reranker-v2-m3
    top-n: 5
    min-score: 0.7                          # 精排后阈值（宪法第20-1条）
    timeout-ms: 5000
  rag:
    recall-top-k: 20                        # 召回候选池
    recall-threshold: 0.5                   # 召回阶段阈值（保证候选充足）

# ai-cs-search.yml
spring:
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
aics:
  hybrid:
    index-name: knowledge_docs
    es-top-k: 20
    vector-top-k: 20
    fusion-top-k: 10
    rrf-k: 60
```
