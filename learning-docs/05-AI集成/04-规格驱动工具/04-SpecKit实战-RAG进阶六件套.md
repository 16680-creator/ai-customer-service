# 04 - SpecKit 实战：RAG 进阶六件套（003-rag-advanced-features）

> 本文档是使用 **Spec Kit 规格驱动开发（SDD）** 落地「RAG 进阶六件套」的完整实战记录。
> 日期：2026-08-12 ｜ 分支：`003-rag-advanced-features` ｜ 规格目录：`specs/003-rag-advanced-features/`

---

## 一、背景与目标

项目 `ai-customer-service` 已具备基础 RAG（向量 + Rerank + 引用溯源 + 搜索侧混合检索 + NL2SQL），
本次用 Spec Kit 流程再落地六项 RAG 进阶能力：

| # | 能力 | 对应用户故事 |
|---|------|-------------|
| 1 | RAG 评估体系（golden 集 + 指标 + LLM-as-Judge + CI 门禁） | US1 |
| 2 | Hybrid RAG 接入对话（ES+向量+RRF 混合检索） | US2 |
| 3 | 查询改写 / HyDE（多查询 + 假设性文档） | US3 |
| 4 | GraphRAG / 多跳检索（图谱 + BFS，无图降级） | US4 |
| 5 | 智能问数自然语言化 + ECharts 图表 | US5 |
| 6 | 知识库运营闭环（提问聚类 + 缺口识别 + FAQ 反哺） | US6 |

---

## 二、Spec Kit 流程与产物

本仓库 `.specify/` 已初始化（Spec Kit 0.8.1），宪法见 `.specify/memory/constitution.md`。
流程严格按 **specify → clarify(默认决策) → plan → tasks → implement → analyze** 推进。

### 2.1 第 1 步：/speckit-specify —— 创建功能规格

**操作**：运行 before_specify 钩子（git extension）自动创建功能分支
`003-rag-advanced-features`，然后创建规格目录与 `spec.md`。

**产物**：

- `specs/003-rag-advanced-features/spec.md` —— 6 个用户故事 + 边界情况 + 15 条功能需求 + 非功能需求 + 6 条成功标准
- `specs/003-rag-advanced-features/checklists/requirements.md` —— 规格质量检查清单

**关键设计**：每个用户故事都定义了「独立测试」方式与「验收场景（假设/当/则）」，
确保可单独交付、单独验证。

### 2.2 第 2 步：/speckit-plan —— 实施计划

**操作**：运行 `setup-plan.ps1` 复制计划模板 → 生成研究/数据模型/契约/快速启动 → 填写计划。

**产物**：

| 文件 | 内容 |
|------|------|
| `research.md` | 15 项技术决策（D1-D15）：评估形态、golden 集存放、指标自实现、LLM-Judge 复用、Feign 接入 Hybrid、默认开关、RRF 融合、图存储抽象、图表判定规则、聚类算法等 |
| `data-model.md` | 评估/检索/图表/运营四类实体 + 新增表 `kb_faq` / `kb_graph_triple` |
| `contracts/rest-api.md` | 新增/变更 REST 契约（检索测试、RAG 扩展、图表、图谱、评估、运营） |
| `quickstart.md` | 前置条件 + 验证命令 + 新增配置项清单 |
| `plan.md` | 技术上下文 + 宪法检查（全部通过）+ 项目结构 |

**关键决策示例**：

- **评估形态**：不新增服务，ai-cs-chat 内嵌离线评估包 + Maven profile（`-Peval`）做 CI 门禁
- **Hybrid 接入**：ai-cs-chat 新增 `SearchFeignClient` 调 ai-cs-search `/search/hybrid`，遵守宪法禁止跨模块依赖内部类
- **GraphRAG**：抽象 `GraphStore` 接口，默认 `InMemoryGraphStore`，`aics.rag.graph.enabled=false` 默认关闭，未配置/未命中 100% 降级

### 2.3 第 3 步：/speckit-tasks —— 任务清单

**产物**：`specs/003-rag-advanced-features/tasks.md`

- 共 68 个任务，按 10 个阶段组织（Setup → Foundational → US1-US6 → 前端 → CI/收尾）
- 每个用户故事严格 **TDD**：先列「测试（Red）」任务，再列「实施（Green）」任务
- 附依赖图与 6 组并行机会表

### 2.4 第 4 步：/speckit-implement —— TDD 实施

**实施范围**（本记录交付时已完成并验证）：

| 模块 | 新增代码 | 测试 |
|------|---------|------|
| ai-cs-chat/rag/eval | GoldenCaseLoader、RetrievalMetrics、RagEvalServiceImpl、LlmJudgeService、RagEvalController、golden-set.json（20 条） | RetrievalMetricsTest、RagEvalServiceImplTest、RagEvaluationTest（CI 门禁） |
| ai-cs-chat/rag/retrieve | HybridRetriever、RetrievalMode、RetrieveResult、MultiQueryMerger、RagRetrieveProperties、RetrieveController | MultiQueryMergerTest、HybridRetrieverTest、RetrieveControllerTest |
| ai-cs-chat/rag/rewrite | QueryRewriteService、RewriteResult、QueryRewriteProperties | QueryRewriteServiceTest |
| ai-cs-chat/rag/graph | GraphStore、InMemoryGraphStore、GraphRagService、GraphProperties、GraphController | InMemoryGraphStoreTest |
| ai-cs-chat/nl2sql/chart | ChartType、ChartTypeDetector、EChartsOptionBuilder、ChartAnswerGenerator、ChartController | ChartTypeDetectorTest、ChartAnswerGeneratorTest、ChartControllerTest |
| ai-cs-chat | SearchFeignClient、ChatHybridPageVO/ChatHybridSearchResult、ChatServiceImpl/ChatController 接入 hybrid/rewrite | — |
| ai-cs-knowledge/ops | EmbeddingClusterService、KnowledgeGapDetector、QuestionClusterService、FaqService、KnowledgeFaq、KnowledgeOpsController | EmbeddingClusterServiceTest、QuestionClusterServiceTest、KnowledgeOpsControllerTest |
| ai-cs-frontend | ChatDashboardView（ECharts 图表）、KnowledgeOpsView（聚类看板）、路由/API 封装、echarts 依赖 | 构建通过 |

**验证结果**：

- ai-cs-chat 全量单测：**53 个全部通过**（含存量测试），`BUILD SUCCESS`
- ai-cs-knowledge 单测：**8 个全部通过**，`BUILD SUCCESS`
- ai-cs-frontend：`npm run build` 成功，产出 ChatDashboardView 等 chunk

### 2.5 第 5 步：/speckit-analyze —— 交叉一致性分析

分析结论（见下文「五、一致性自查」），并据此修复了测试 mock 与实现格式不一致等 3 处问题。

---

## 三、实战中的关键问题与解法

### 3.1 中文编码：PowerShell 写文件产生 BOM 导致编译失败

**现象**：用 `Set-Content -Encoding UTF8` 写 Java 文件后，javac 报 `非法字符: '\ufeff'`。
**根因**：Windows PowerShell 5.1 的 UTF8 编码带 BOM，javac 不认。
**解法**：改用 `[System.IO.File]::WriteAllText(path, content, UTF8Encoding($false))` 写无 BOM 文件，并批量把已生成的 Java 文件重编码。

### 3.2 Spring AI 1.1.4 API 变化：Document 无 setScore

**现象**：`doc.setScore(...)` 编译报错。
**根因**：Spring AI 1.1.4 的 `Document` 只有 `getScore()`，没有 setter，需用 Builder。
**解法**：改用 `Document.builder().id(...).text(...).metadata(...).score(...).build()`。

### 3.3 存量实现与测试不一致：SiliconFlow Rerank 响应格式

**现象**：`SiliconFlowRerankServiceTest` 2 个用例失败。
**根因**：实现已按硅基流动真实响应（`document` 为对象 `{"text":"..."}`）更新，但旧测试仍 mock 成字符串。
**解法**：修正测试 mock 为对象格式，保持实现为准（实现反映真实 API 行为）。

### 3.4 评估报告时间格式

**现象**：`DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(Instant.now())` 抛 `UnsupportedTemporalType`。
**解法**：`.withZone(ZoneId.systemDefault())` 后格式化。

### 3.5 TDD 测试桩逻辑

**现象**：`RagEvalServiceImplTest` 命中率不符合预期。
**根因**：mock 数据源对所有问题返回同一文档，未按 golden 集期望文档命中。
**解法**：改为按 golden 集 question → expectedDocumentIds 的桩数据源（与 `RagEvaluationTest` 一致），保证指标断言确定。

---

## 四、Spec Kit 实战要点总结

1. **规格先行是最大的价值**：6 个能力在写代码前先定「独立测试」与「验收场景」，实施时有明确边界，避免大方法失控。
2. **TDD 对纯函数收益最高**：指标计算、RRF 融合、图表判定、聚类、图谱 BFS 全是确定性逻辑，单测覆盖后重构无忧。
3. **降级是 AI 客服的命门**：每个增强（Hybrid/改写/图谱）都设计「失败降级为纯向量」，测试显式覆盖降级分支，保证核心对话可用性。
4. **默认关闭、显式开启**：新能力全部默认 false，存量调用零破坏，符合「渐进增强」原则。
5. **AI 依赖项与框架 API 版本要实测**：Spring AI 1.1.4 的 Document/EmbeddingModel API 与 1.0 有差异，实施前用 javap 确认。
6. **Windows 下写代码文件的编码陷阱**：Java 源文件必须无 BOM，否则编译失败。

---

## 五、一致性自查（/speckit-analyze 视角）

| 检查项 | 结论 |
|--------|------|
| spec ↔ plan 用户故事一致（US1-US6） | ✅ |
| plan ↔ tasks 阶段/任务一致 | ✅ |
| tasks 每个故事有测试前置任务（TDD） | ✅ |
| contracts ↔ 控制器路径/参数一致（/chat/retrieve/test、/chat/chart、/rag/eval/run、/rag/graph/*、/knowledge/ops/*） | ✅ |
| data-model ↔ 实体/表一致（kb_faq、kb_graph_triple） | ✅ |
| 宪法门禁（不新增模块、Feign 跨服务、配置走 Nacos、复用 Result） | ✅ |
| 测试全绿 + 前端构建通过 | ✅ |

---



---

## 八、运行期 E2E 验证（2026-08-12 补充）

在真实基础设施（远程 MySQL/Redis/Chroma 123.60.31.79 + 本地 Nacos/RocketMQ）上重启
chat(8083)/knowledge(8082) 新 jar，逐项实测：

| 能力 | 接口 | 实测结果 |
|------|------|---------|
| US1 评估 | POST /rag/eval/run | ✅ 加载 golden 集（20 条）→ 检索 → 指标(recall/mrr/hitRate) → LLM-Judge 均分 → 门禁判定；product-manual 库无数据时 hitRate=0、passed=false（行为正确） |
| US2 Hybrid | GET /chat/retrieve/test?mode=HYBRID | ✅ 全链路实测：启动 ai-cs-search(8084) + 临时开启 hybrid 开关 → chat Feign → search 混合检索 → 返回 5 条命中（mode=HYBRID, degraded=false，top1 为刚收录的 FAQ）；ES 未部署时 search 侧自动走向量路；开关关闭时自动降级纯向量 |
| US3 改写/HyDE | GET /chat/retrieve/test?mode=HYBRID_QUERY_REWRITE | ✅ 临时开启开关后实测成功路径：LLM 改写→多查询+HyDE→向量检索→RRF 融合返回 5 条命中、未降级；关闭后自动降级 |
| US4 GraphRAG | POST /rag/graph/triple + GET /rag/graph/query | ✅ 三元组入库（id 1/2）→ 多跳查询 depth=2 命中「退款政策→申请入口→审核时效」2 条 |
| US5 图表 | POST /chat/chart | ✅ LLM 生成结论 + chartType=PIE 自动判定 + ECharts option 返回 |
| US6 聚类 | POST /knowledge/ops/cluster | ✅ 24 条提问聚成 1 主题、占比正确、缺口检测（命中率 1.0、无缺口） |
| US6 FAQ | POST /knowledge/ops/faq | ✅ 收录成功（faqId=1）→ 创建知识文档 → 触发向量化 |

### E2E 过程中发现并修复的问题

1. **Nacos 配置 BOM 污染**：publish 脚本把带 BOM 的 yml 上传，客户端解析失败导致
   `spring.ai.openai.api-key` 未解析、服务启动失败。修复：去除所有配置源文件 BOM 后重发布。
2. **knowledge jar 无主清单**：knowledge pom 未配置 boot repackage execution，
   补上 `<goal>repackage</goal>` 后 jar 可运行。
3. **knowledge 缺 RocketMQ 配置**：全量发布覆盖了 Nacos 中原有 `rocketmq.name-server`，
   补回 `deploy/nacos/configs/ai-cs-knowledge.yml` 并重发布。
4. **golden 集未打包进运行时 jar**：golden-set.json 原在 test resources，运行期 `/rag/eval/run`
   找不到。修复：复制一份到 `src/main/resources/eval/golden-set.json`（测试仍走 test 资源）。
5. **远程 MySQL 缺新表**：用 JDK 单文件程序执行 `rag-advanced-init.sql` 的 DDL（kb_faq / kb_graph_triple）。

### 环境限制（已登记）

- Hybrid 双路 RRF 完整成功路径需 Elasticsearch（本环境 9200 未部署），当前已验证 chat→search 全链路（ES 降级为向量路）；Rerank/Embedding API Key 走 Nacos，无硬编码
- GraphRAG 默认 InMemory 存储实测通过；Neo4j 版需 Neo4j 实例（单测基于 Mock 驱动）

## 六、后续待办（跟踪）

### 已完成（2026-08-12 续）

- [x] Neo4j 图存储实现（`Neo4jGraphStore` + `Neo4jConfig` + 驱动依赖；`aics.rag.graph.storage=neo4j` 切换，5 个单测通过）
- [x] CI 流水线接入 `-Peval` 评估门禁（Jenkinsfile 新增「RAG 评估门禁」阶段）
- [x] Nacos 配置发布（`publish-to-nacos.ps1` 执行成功 11/11，已读回验证 `aics.rag.*` 生效）
- [x] 任务清单 T001-T068 全部勾选，文档与实现一致

### 待办（依赖完整基础设施）

- [x] 运行期 E2E 验证：已在远程 MySQL/Redis/Chroma + 本地 Nacos/RocketMQ 实测 6 项能力（见第八节）；Hybrid 成功路径与 Neo4j 版受环境限制待补
- [ ] 评估 golden 集生产样本（`deploy/eval/`）运营维护
- [ ] GraphRAG 实体抽取 LLM 增强（当前为关键词子串匹配）
- [ ] Neo4j 实例联调（当前单测基于 Mock 驱动）
- [ ] /speckit-clarify 阶段形式化澄清（本次用默认决策）

---

## 七、相关链接

- 规格：`specs/003-rag-advanced-features/spec.md`
- 计划：`specs/003-rag-advanced-features/plan.md`
- 任务：`specs/003-rag-advanced-features/tasks.md`
- 研究：`specs/003-rag-advanced-features/research.md`
- 契约：`specs/003-rag-advanced-features/contracts/rest-api.md`
- 快速启动：`specs/003-rag-advanced-features/quickstart.md`
- 建表 SQL：`deploy/mysql/rag-advanced-init.sql`
- 系列文档：本目录 `01-OpenSpec实战-会话历史持久化.md`、`02-Superpowers（SpecKit）实战：LLM调用工程化.md`、`03-SpecKit-OpenSpec-Superpowers-对比与实战指南.md`