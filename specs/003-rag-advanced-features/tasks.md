# 任务：RAG 进阶六件套

> **实施状态（2026-08-12）**：T001-T068 已全部执行完成（TDD：测试先行）。
> 后端 66 个单测全绿（chat 53 + knowledge 8 + common 5）、前端构建通过、`-Peval` 门禁实测通过、
> Nacos 配置已发布、Jenkinsfile 已接入门禁。剩余运行期验证依赖完整基础设施（MySQL/Redis/Chroma/ES），见 quickstart.md。

**输入**: 来自 `/specs/003-rag-advanced-features/` 的设计文档
**前置条件**: plan.md（必填）、spec.md（用户故事必填）、research.md、data-model.md、contracts/rest-api.md

**测试**: 采用 TDD 方式（宪法第2-1条），测试任务必选。每个用户故事先写测试并验证失败（Red），再实现使其通过（Green），再重构（Refactor）。

**组织方式**: 任务按用户故事分组（US1-US6），支持独立实施与测试。

## 格式：`[ID] [P?] [故事] 描述`

- **[P]**：可并行执行（不同文件，无依赖）
- **[故事]**：任务所属用户故事
- 描述含确切文件路径

---

## 阶段 1：Setup（依赖与配置）

- [x] T001 修改 ai-cs-chat/pom.xml：新增 openfeign 依赖（若未存在）用于调 ai-cs-search
- [x] T002 修改 ai-cs-frontend/package.json：新增 echarts 依赖
- [x] T003 新增 deploy/nacos/configs/ai-cs-chat.yml 配置项：aics.rag.*（hybrid/rewrite/graph/eval/cluster 开关与阈值，见 quickstart.md）
- [x] T004 新增 deploy/sql/kb_graph_triple.sql 与 kb_faq.sql（建表语句，见 data-model.md）

## 阶段 2：Foundational（基础组件，阻塞所有故事）

- [x] T005 [P] 新增 RetrievalMetrics 到 ai-cs-chat/rag/eval（纯函数：recallAtK/mrr/hitRate，空集处理）
- [x] T006 [P] 新增 MultiQueryMerger 到 ai-cs-chat/rag/retrieve（多路结果 RRF 融合去重，纯函数）
- [x] T007 [P] 新增 ChartTypeDetector 到 ai-cs-chat/nl2sql/chart（多行分类→PIE/BAR、时间列→LINE、单行/空→NONE）
- [x] T008 [P] 新增 InMemoryGraphStore 到 ai-cs-chat/rag/graph（三元组 CRUD + BFS 多跳查询）
- [x] T009 新增 DTO：GoldenCase/RagEvalReport/RagEvalCaseResult（eval）、RewriteResult/RetrieveResult/RetrievalMode（retrieve）、ChartAnswer/ChartType（chart）、GraphTriple/GraphHit（graph）

**检查点**：基础组件就绪，可并行实施 US1-US6。

---

## 阶段 3：US1 - RAG 质量可量化评估（P1）🎯 MVP

**目标**：golden 集一键评估 + 指标 + LLM-as-Judge + CI 门禁

**独立测试**：`mvn -pl ai-cs-chat test -Dtest=RagEvaluationTest -Peval` 运行内置 golden 集并断言通过

### US1 测试（TDD Red）

- [x] T010 [P] [US1] 编写 RetrievalMetricsTest：Recall@k/MRR/HitRate 计算、空期望文档、越界 k
- [x] T011 [P] [US1] 编写 RagEvalServiceImplTest：Mock 数据源与 LLM Judge，验证汇总指标/逐条明细/阈值门禁
- [x] T012 [US1] 编写 RagEvaluationTest：加载 classpath:eval/golden-set.json 端到端跑评估并断言 passed

### US1 实施

- [x] T013 [P] [US1] 实现 RetrievalMetrics（依赖 T005 接口定义与 T010）
- [x] T014 [P] [US1] 实现 GoldenCaseLoader（JSON → List<GoldenCase>，非法行跳过不崩溃）
- [x] T015 [P] [US1] 实现 LlmJudgeService（复用 ChatClient/ResilientAiService，输出 1-5 分，异常返回 null）
- [x] T016 [US1] 实现 RagEvalServiceImpl（逐条检索→指标→LLM 打分→汇总报告→阈值门禁）
- [x] T017 [US1] 实现 RagEvalProperties 与 RagEvalController（POST /rag/eval/run）
- [x] T018 [US1] 新增 src/test/resources/eval/golden-set.json（20 条内置样本）
- [x] T019 [US1] 配置 Maven profile `-Peval` 与 jacoco 排除（评估类不纳入常规覆盖率门禁）

**检查点**：评估命令可跑、报告可读、CI 可门禁。

---

## 阶段 4：US2 - Hybrid RAG 接入对话（P1）

**目标**：chat 侧 RAG 链路支持 ES+向量+RRF 混合检索，存量兼容

**独立测试**：`/chat/retrieve/test?mode=HYBRID` 返回混合结果；单路故障降级

### US2 测试（TDD Red）

- [x] T020 [P] [US2] 编写 MultiQueryMergerTest：RRF 融合计算、空单路、去重
- [x] T021 [P] [US2] 编写 HybridRetrieverTest：Mock SearchFeignClient + VectorStore，验证混合/降级/模式开关
- [x] T022 [US2] 编写 RetrieveControllerTest：/chat/retrieve/test 契约（mode 参数、响应结构）

### US2 实施

- [x] T023 [P] [US2] 新增 SearchFeignClient（GET /search/hybrid，参数 index/query/topK）
- [x] T024 [P] [US2] 新增 HybridSearchResult/HybridResultPageVO 映射（chat 侧 DTO）
- [x] T025 [US2] 实现 HybridRetriever（VECTOR 走本地向量库；HYBRID 走 Feign 混合；异常降级 VECTOR；结果转 Document 含 metadata）
- [x] T026 [US2] 实现 RetrieveController（GET /chat/retrieve/test）
- [x] T027 [US2] 修改 ChatServiceImpl.chatWithRag / chatStreamSse：支持 hybrid 参数（默认 false 保持纯向量），检索走 HybridRetriever

**检查点**：RAG 对话可开启 Hybrid，引用溯源基于混合结果。

---

## 阶段 5：US3 - 查询改写与 HyDE（P2）

**目标**：LLM 多查询改写 + 假设性文档，提升模糊问题召回

**独立测试**：`/chat/retrieve/test?mode=HYBRID_QUERY_REWRITE` 输出改写查询 + HyDE + 融合命中

### US3 测试（TDD Red）

- [x] T028 [P] [US3] 编写 QueryRewriteServiceTest：Mock ChatClient 返回 JSON 子查询/HyDE，解析、空结果降级、超时降级
- [x] T029 [US3] 编写 HybridRetriever 改写分支测试：多查询+HyDE 融合命中、单路降级

### US3 实施

- [x] T030 [P] [US3] 实现 QueryRewriteService（LLM 生成 ≥2 子查询 JSON + HyDE 文档；失败返回空→调用方用原问题）
- [x] T031 [US3] 实现 RewriteResult 解析与校验（去重、长度限制、非法 JSON 容错）
- [x] T032 [US3] 扩展 HybridRetriever：mode=HYBRID_QUERY_REWRITE 时先改写再对多查询+HyDE 分别检索，MultiQueryMerger 融合
- [x] T033 [US3] 新增 QueryRewriteProperties（开关/模型/超时）并接入 RagRetrieveProperties

**检查点**：模糊问题召回提升，可开关，失败自动降级。

---

## 阶段 6：US4 - GraphRAG 多跳检索（P2）

**目标**：三元组图谱 + 多跳查询，无图自动降级普通 RAG

**独立测试**：写入三元组后 `/rag/graph/query?depth=2` 返回多跳命中；未配置图谱时降级

### US4 测试（TDD Red）

- [x] T034 [P] [US4] 编写 InMemoryGraphStoreTest：CRUD、按 subject 多跳 BFS、空图、循环边防护
- [x] T035 [P] [US4] 编写 GraphRagServiceTest：命中注入上下文 / 未命中 / 未启用降级三分支
- [x] T036 [US4] 编写 GraphControllerTest：/rag/graph/triple POST 与 /rag/graph/query GET 契约

### US4 实施

- [x] T037 [P] [US4] 实现 InMemoryGraphStore（默认存储，进程内；含校验：subject/predicate/object 非空、长度限制）
- [x] T038 [P] [US4] 实现 GraphStore 接口与 GraphProperties（enabled 默认 false）
- [x] T039 [US4] 实现 GraphRagService（实体抽取（LLM 可选，MVP 用关键词匹配）+ 多跳查询 + 上下文注入；未启用/未命中返回空）
- [x] T040 [US4] 实现 GraphController（POST /rag/graph/triple、GET /rag/graph/query、GET /rag/graph/triples）
- [x] T041 [US4] 扩展 ChatServiceImpl：mode=GRAPH_RAG 时先图谱检索再普通检索（图谱命中补充上下文）

**检查点**：图谱链路可用且默认关闭；未配置时回答不中断。

---

## 阶段 7：US5 - 智能问数自然语言化与图表（P2）

**目标**：NL2SQL 结果 → 自然语言结论 + ECharts 配置

**独立测试**：`POST /chat/chart` 输入 rows 输出 conclusion + chartType + echartsOption

### US5 测试（TDD Red）

- [x] T042 [P] [US5] 编写 ChartTypeDetectorTest：分类维度→PIE、时间列→LINE、多行数值→BAR、单行/空→NONE
- [x] T043 [P] [US5] 编写 ChartAnswerGeneratorTest：Mock LLM 生成结论；LLM 失败降级模板结论；ECharts option 结构断言
- [x] T044 [US5] 编写 ChartControllerTest：/chat/chart 契约（question/rows → ChartAnswer）

### US5 实施

- [x] T045 [P] [US5] 实现 ChartTypeDetector（纯函数）
- [x] T046 [P] [US5] 实现 EChartsOptionBuilder（pie/bar/line option 构建，纯 Java）
- [x] T047 [US5] 实现 ChartAnswerGenerator（结论 LLM 生成 + 降级模板；图表类型判定 + option 构建）
- [x] T048 [US5] 实现 ChartController（POST /chat/chart）
- [x] T049 [US5] 扩展 Nl2Sql 工具返回：LLM 结果解析为 rows 结构供 ChartAnswerGenerator 消费（或提供 ChartService 适配）

**检查点**：问数结果可读、可图表化。

---

## 阶段 8：US6 - 知识库运营闭环（P2）

**目标**：提问聚类 + 缺口识别 + FAQ 反哺 + 向量更新

**独立测试**：`POST /knowledge/ops/cluster` 输出主题 + 缺口；`POST /knowledge/ops/faq` 收录并触发向量化

### US6 测试（TDD Red）

- [x] T050 [P] [US6] 编写 EmbeddingClusterServiceTest：Mock EmbeddingModel，验证贪心聚类分组、阈值、代表问题、最小数量
- [x] T051 [P] [US6] 编写 KnowledgeGapDetectorTest：Mock 检索命中率，验证缺口标记与阈值
- [x] T052 [US6] 编写 KnowledgeOpsControllerTest：/knowledge/ops/cluster、/knowledge/ops/cluster/report、/knowledge/ops/faq 契约

### US6 实施

- [x] T053 [P] [US6] 实现 EmbeddingClusterService（Embedding 向量 + 余弦相似度贪心聚类；<min-questions 返回 INSUFFICIENT_DATA）
- [x] T054 [P] [US6] 实现 KnowledgeGapDetector（主题内抽样问题走知识检索，命中率 < 阈值 → gapFlag）
- [x] T055 [P] [US6] 新增实体/表：KnowledgeGraphTriple、KnowledgeFaq（MyBatis-Plus）
- [x] T056 [US6] 实现 FaqService（FAQ 收录 → 生成 KnowledgeDocument → 触发向量化（复用 RocketMQ 链路））
- [x] T057 [US6] 实现 KnowledgeOpsController（POST /knowledge/ops/cluster、GET /knowledge/ops/cluster/report、POST /knowledge/ops/faq）
- [x] T058 [US6] 实现 QuestionClusterService 编排（导入问题→聚类→缺口→报告）

**检查点**：聚类看板可出报告、FAQ 一键收录并自动向量化。

---

## 阶段 9：前端（P2）

- [x] T059 [P] 新增 ChatDashboardView.vue：问数输入 + ECharts 图表渲染（/chat/chart 消费）
- [x] T060 [P] 新增 KnowledgeOpsView.vue：聚类主题列表 + 缺口标记 + FAQ 收录按钮（/knowledge/ops/* 消费）
- [x] T061 [P] 修改 ai-cs-frontend/src/api/index.js：新增 /chat/chart、/knowledge/ops/* 接口封装
- [x] T062 [P] 修改 ai-cs-frontend/src/router/index.js：注册 /chat-dashboard 与 /knowledge-ops 路由

---

## 阶段 10：CI 门禁与收尾

- [x] T063 [P] 在 Jenkinsfile 或 CI 脚本增加评估门禁步骤（mvn -Peval，失败即构建失败）
- [x] T064 [P] 更新 deploy/nacos/configs/ai-cs-chat.yml 与 ai-cs-knowledge.yml 的 Nacos 发布脚本
- [x] T065 全量编译与测试：mvn -pl ai-cs-chat,ai-cs-knowledge -am clean verify（含 JaCoCo 门禁）
- [x] T066 运行 /speckit-analyze 交叉一致性分析并修复发现的问题
- [x] T067 编写 Spec Kit 实战过程文档到 learning-docs/05-AI集成/04-规格驱动工具/04-SpecKit实战-RAG进阶六件套.md
- [x] T068 按功能分类提交代码（feat(chat)/feat(knowledge)/feat(frontend)/docs/test）

---

## 依赖图

```
Foundational (Phase 2)
   ├──→ US1 Eval (Phase 3) ──→ Phase 10 CI
   ├──→ US2 Hybrid (Phase 4) ──→ US3 Rewrite (Phase 5)
   ├──→ US4 Graph (Phase 6)
   ├──→ US5 Chart (Phase 7)
   └──→ US6 Ops (Phase 8) ──→ Phase 9 Frontend
```

## 并行机会

| 可并行组 | 任务 | 说明 |
|---------|------|------|
| 组 A | T010-T012（US1 测试） | 与组 B/C 无依赖 |
| 组 B | T020-T022（US2 测试） | 依赖 T006 MultiQueryMerger |
| 组 C | T028-T029（US3 测试） | 依赖 T006 |
| 组 D | T034-T036（US4 测试） | 依赖 T008 InMemoryGraphStore |
| 组 E | T042-T044（US5 测试） | 依赖 T007 ChartTypeDetector |
| 组 F | T050-T052（US6 测试） | 独立 |

## 建议 MVP

US1 评估体系（Phase 3）独立交付 → US2 Hybrid（Phase 4）→ 其余按优先级推进。
