# 任务清单：RAG 检索质量升级五件套

> 生成于 `/speckit-tasks`（2026-08-12）
> 基于 [plan.md](plan.md)、[spec.md](spec.md)、[research.md](research.md)、[data-model.md](data-model.md)、[contracts/rest-api.md](contracts/rest-api.md)

---

## 依赖图

```
Foundational (Phase 2)
    ├──→ US1 Rerank (Phase 3) ──→ US3 Citation (Phase 5)
    ├── [P] US4 Tika (Phase 3) ──→ ↑ US7 前端引用卡片 (Phase 7)
    ├── [P] US2 Hybrid (Phase 4)
    └── [P] US5 MQ Sync (Phase 6)
```

## 并行机会

| 可并行组 | 包含任务 | 说明 |
|---------|---------|------|
| 组 A | US1 + US4 | Rerank 与 Tika 无依赖关系，可并行开发 |
| 组 B | US2 | 与 A/B 组均无依赖 |
| 组 C | US5 | 与 A/B 组均无依赖 |

## 独立测试准则

| 故事 | 独立测试方式 |
|------|-------------|
| US1 Rerank | 注入 Mock RerankService，验证向量召回→Rerank→过滤流程；超时/失败验证降级 |
| US2 Hybrid | 注入 Mock ES client + Mock VectorStore，验证 RRF 融合；单路异常验证降级 |
| US3 Citation | 依赖 US1 组件就绪后，验证 chat/rag 和 SSE 的 citations 字段完整性 |
| US4 Tika | 分别上传 docx/xlsx/html/md 文件验证解析、分块、入库 |
| US5 MQ | 发消息验证消费者异步执行向量化/删除；MQ 不可用时验证 DB 操作正常 |

---

## Phase 1：Setup（依赖与配置变更）

- [ ] T001 修改 ai-cs-chat/pom.xml：新增 spring-ai-tika-document-reader 依赖
- [ ] T002 修改 ai-cs-search/pom.xml：新增 elasticsearch-java 依赖
- [ ] T003 修改 ai-cs-knowledge/pom.xml：新增 rocketmq-spring-boot-starter 依赖

## Phase 2：Foundational（基础组件与 DTO）

- [ ] T004 [P] 新增 CitationItemDTO 到 ai-cs-chat/dto（documentId/title/page/score/content）
- [ ] T005 [P] 新增 ChatRagResponseDTO 到 ai-cs-chat/dto（content + citations）
- [ ] T006 [P] 新增 RerankProperties 到 ai-cs-chat/rag/rerank（base-url/model/api-key/top-n/min-score/timeout-ms，@ConfigurationProperties("aics.rerank")）
- [ ] T007 [P] 新增 RerankService 接口及 RerankRequest/RerankResult/RerankResultItem 模型到 ai-cs-chat/rag/rerank
- [ ] T008 [P] 新增 ElasticsearchConfig 到 ai-cs-search/config（ElasticsearchClient Bean，uris 走 @Value("${spring.elasticsearch.uris}")）
- [ ] T009 新增 HybridSearchResult VO 到 ai-cs-search/hybrid

## Phase 3：US1 Rerank + US4 Tika（可并行）

### US1 Rerank 重排序

- [ ] T010 [US1] 编写 RerankServiceTest：MockRestServiceServer 模拟硅基流动 API 响应，验证正常返回/超时/5xx 降级
- [ ] T011 [US1] 实现 SiliconFlowRerankService（RestClient 调 POST /v1/rerank，try-catch 降级为 null→调用方回退原始排序）
- [ ] T012 [US1] 修改 KnowledgeBaseService.search() 为两阶段检索：① searchRaw() 召回 Top-20；② rerankIfAvailable() 精排 Top-5；③ 阈值过滤；④ 降级逻辑
- [ ] T013 [US1] 修改 KnowledgeBaseService.buildContext()：保持引用编号【资料N】一致，metadata 补充 documentId/title（入库时）
- [ ] T014 [US1] 修改 KnowledgeBaseService.addChunks()：写入 Chroma 时 metadata 补充 documentId（从 chunk.getId()）和 title

### US4 Tika 文档格式扩展

- [ ] T015 [P] [US4] 编写 DocumentLoaderTest：Mock Resource 模拟 docx/xlsx/html/md 输入，验证 loadTika 返回非空 Document 列表；验证 pdf/txt/md 仍走原路径
- [ ] T016 [P] [US4] 实现 DocumentLoader.loadTika(Resource)：TikaDocumentReader try-with-resources 解析，返回 List<Document>
- [ ] T017 [US4] 修改 KnowledgeBaseService.addFile()：扩展名路由（.pdf→loadPdf，.txt/.md→loadText，.docx/.xlsx/.html/.htm→loadTika，其他→BusinessException）
- [ ] T018 [US4] 修改 KnowledgeBaseController.upload() 的 JavaDoc/OpenAPI 注解说明支持的格式

### US1 + US4 阶段验证

- [ ] T019 [US1] 运行 KnowledgeBaseService 测试，确认 Rerank 降级逻辑覆盖
- [ ] T020 [US4] 运行 DocumentLoader 测试，确认 6 种格式覆盖

## Phase 4：US2 Hybrid 混合检索（可独立于 Phase 3）

- [ ] T021 [P] [US2] 编写 RrfMergerTest：模拟两路排名列表，验证 RRF 公式计算正确（k=60）、空列表/单路异常场景
- [ ] T022 [P] [US2] 实现 RrfMerger（纯函数 merge(List<RankedItem> esResults, List<RankedItem> vectorResults, int fusionTopK, int rrfK)）
- [ ] T023 [P] [US2] 编写 HybridSearchServiceImplTest：Mock EsClient.search + Mock VectorStore.similaritySearch，验证双路召回→RRF→Top-10 融合流程
- [ ] T024 [P] [US2] 实现 HybridSearchServiceImpl（EsClient BM25 multi_match → 转 RankedItem；VectorStore similaritySearch → 转 RankedItem；RrfMerger.merge → 取 Top-10）
- [ ] T025 [US2] 编写 SearchController 集成测试：Mock HybridSearchService，验证 /search/hybrid 接口返回结构符合契约
- [ ] T026 [US2] 实现 SearchController.hybridSearch()：GET /search/hybrid，参数 index/query/page/size
- [ ] T027 [US2] 修改 SearchServiceImpl.indexDocument()：写 Chroma 的同时同步写入 ES（先按 documentId 删除旧文档再插入，幂等）
- [ ] T028 [US2] 修改 SearchServiceImpl.deleteIndex()：同时清理 ES 索引数据
- [ ] T029 [US2] 新增 SearchServiceImpl.createEsIndexIfNeeded()：启动时校验 ES index 是否存在，不存在则按 mappings 创建

### Phase 4 验证

- [ ] T030 [US2] 运行全部 Hybrid 搜索测试，确认双路召回+RRF+降级覆盖

## Phase 5：US3 Citation 引用溯源（依赖 US1）

- [ ] T031 [P] [US3] 编写 ChatServiceImpl 测试（chatWithRag）：注入 Mock KnowledgeBaseService（返回带 documentId/score 的文档），验证响应包含 citations 数组
- [ ] T032 [P] [US3] 编写 ChatServiceImpl 测试（chatStreamSse）：验证 done 事件包含 citations 字段
- [ ] T033 [US3] 修改 ChatServiceImpl.chatWithRag()：从检索结果提取 CitationItemDTO 列表，连同 content 封装为 ChatRagResponseDTO 返回
- [ ] T034 [US3] 修改 ChatController.chatWithRag()：返回类型 Result<ChatRagResponseDTO>
- [ ] T035 [US3] 修改 ChatServiceImpl.chatStreamSse() 的 RAG 分支：检索后缓存 citation 列表，done 事件发送 {done:true, content, citations:[...]}
- [ ] T036 [US3] 修改 KnowledgeBaseService.buildContext() 确保上下文文本与 citations 数据来源一致（同一份 Top-5 文档）

### Phase 5 验证

- [ ] T037 [US3] 运行 ChatServiceImpl 全部测试，验证 citations 在非流式和流式接口中均正常工作

## Phase 6：US5 MQ 知识库增量同步

- [ ] T038 [P] [US5] 新增 KnowledgeSyncMessage DTO 到 ai-cs-knowledge/mq（action/documentId/knowledgeBase/title/content/timestamp）
- [ ] T039 [P] [US5] 新增 KnowledgeSyncProducer 到 ai-cs-knowledge/mq（RocketMQTemplate.convertAndSend("knowledge-doc-sync-topic", message, tag)）
- [ ] T040 [P] [US5] 编写 KnowledgeSyncConsumerTest：Mock KnowledgeVectorService，分别接收 CREATE/UPDATE/DELETE 消息验证处理逻辑
- [ ] T041 [P] [US5] 实现 KnowledgeSyncConsumer（@RocketMQMessageListener，CREATE/UPDATE→vectorize，DELETE→deleteByDocumentId）
- [ ] T042 [US5] 修改 KnowledgeVectorService.vectorize()：metadata 补充 documentId、title
- [ ] T043 [US5] 新增 KnowledgeVectorService.deleteByDocumentId()：按 Chroma filterExpression "documentId == X" 删除
- [ ] T044 [US5] 修改 KnowledgeServiceImpl.createDocument()：DB insert 后改发 CREATE 消息替代同步 vectorize
- [ ] T045 [US5] 修改 KnowledgeServiceImpl.updateDocument()：DB update 后改发 UPDATE 消息
- [ ] T046 [US5] 修改 KnowledgeServiceImpl.deleteDocument()：先 selectById 回填 documentId，DB delete 后发 DELETE 消息

### Phase 6 验证

- [ ] T047 [US5] 运行 KnowledgeServiceImpl 测试，验证 DB 操作正常、MQ 发送正常、MQ 不可用不阻断

## Phase 7：前端引用溯源 UI

- [ ] T048 [P] [US3] 编写 ChatView SSE 解析逻辑：done 事件新增 citations 字段解析（现有代码只解析 content 和 error）
- [ ] T049 [P] [US3] 修改 ChatView.vue：消息气泡下方增加引用卡片区域（el-card，列表渲染 citations，每项卡片含文档名/页码/相似度/内容预览）

### Phase 7 验证

- [ ] T050 [US3] 前端测试：Mock SSE 事件流包含 citations，确认 UI 正确渲染引用卡片

## Phase 8：端到端验证 & 文档

- [ ] T051 运行 `mvn clean install -DskipTests` 确认全量编译通过
- [ ] T052 运行 `mvn -pl ai-cs-chat,ai-cs-search,ai-cs-knowledge verify` 确认测试通过且 JaCoCo 覆盖率达标
- [ ] T053 验证各模块 application.yml 及 Nacos 配置模板中新增配置项（aics.rerank.*、spring.elasticsearch.uris）

---

## 统计

| 类别 | 任务数 |
|------|--------|
| 全部任务 | 53 |
| 测试任务（TDD Red 阶段） | 10 |
| 实现任务 | 33 |
| 验证任务 | 8 |
| 配置/文档任务 | 2 |

## 建议 MVP 范围

Phase 1 + 2 + Phase 3（US1 Rerank + US4 Tika）= **20 个任务**，覆盖精度提升和格式扩展两个高价值点，可独立上线。后续 Phase 逐一追加。