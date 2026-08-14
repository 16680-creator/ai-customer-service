# 功能规格：RAG 进阶六件套

**功能分支**: `003-rag-advanced-features`
**创建日期**: 2026-08-12
**状态**: 草稿
**模块**: `ai-customer-service:ai-cs-chat / ai-cs-search / ai-cs-knowledge / ai-cs-frontend`
**输入**: 需求描述: "RAG 进阶六件套：1) RAG 评估体系（RAGAS / LLM-as-Judge + golden 测试集回归 + CI 集成）；2) Hybrid RAG 接入对话（把 ai-cs-search 的 ES+向量+RRF 混合检索接入 chat 的 RAG 链路，目前对话侧只用纯向量）；3) 查询改写 / HyDE（LLM 把模糊问题改写为多个精确子查询；HyDE 假设性文档嵌入提升召回）；4) GraphRAG / 多跳检索（知识图谱 Neo4j + 实体关系检索）；5) 结构化问答增强（NL2SQL 结果自然语言化 + 自动生成图表 ECharts，AI 数据看板）；6) 知识库运营闭环（高频问题聚类反哺 FAQ、识别知识库缺口、自动更新向量）"

## 用户场景与测试 *（必填）*

### 用户故事 1 - RAG 质量可量化评估（优先级：P1）

AI 客服研发同学在改动检索链路（如新增 Rerank、Hybrid、查询改写）后，能运行一套离线评估脚本，用预置的 golden 测试集对"检索命中率"与"回答质量"打分，量化改动前后差异，防止 RAG 质量回退，并接入 CI 回归。

**此优先级的原因**：后续所有检索增强（Hybrid、改写、GraphRAG）都依赖"可量化的质量基线"，没有评估体系就无法证明改动有效，也无法防止回退。是其他用户故事的地基。

**独立测试**：提供评估 CLI/接口，输入 golden 测试集（JSON 文件：question + expected 文档/片段 + reference 答案），输出检索指标（Recall@k / MRR / HitRate）与回答质量分（LLM-as-Judge 1-5 分）报告，可在无对话服务的情况下独立运行。

**验收场景**：

1. **假设** 提供 20 条 golden 测试集（含 question、期望命中的文档 ID、参考答案），**当** 运行评估命令，**则** 输出 Recall@5、MRR、HitRate 三项检索指标与总体回答质量均分
2. **假设** golden 集中存在与知识库无关的问题，**当** 运行评估，**则** 该条不计入命中率且不导致评估崩溃
3. **假设** 评估脚本在 CI 中执行且质量分低于阈值，**当** 流水线运行，**则** 以失败/告警形式暴露，阻止低质量改动合入

---

### 用户故事 2 - Hybrid RAG 接入对话（优先级：P1）

用户在对话中问含精确型号/编号的问题（如"型号 ABC-123 的保修期"），系统不再只做向量语义检索，而是把 ES 关键词 + 向量语义的 RRF 混合检索结果注入 RAG 上下文，兼顾精确匹配与语义匹配。

**此优先级的原因**：客服场景大量问题是"型号 + 语义"混合表述，纯向量对型号/编号召回差，Hybrid 直接提升回答准确性；且 ai-cs-search 已实现混合检索组件，接入成本低、收益直接。

**独立测试**：调用 RAG 接口（同步 + SSE 流式）并携带 hybrid=true，校验返回回答与引用溯源均基于混合检索结果；与纯向量模式对比，含型号查询命中更准确。

**验收场景**：

1. **假设** 知识库含"型号 ABC-123 保修 1 年"，**当** 用户问"ABC-123 保修多久"，**则** RAG 回答基于混合检索命中的资料生成且引用含该资料
2. **假设** 用户问"怎么申请退款"（纯语义），**当** RAG 对话，**则** 语义命中的资料仍能进入上下文
3. **假设** ES 或向量库其中一路不可用，**当** RAG 对话，**则** 自动降级为可用的一路，回答不中断
4. **假设** 用户未开启 hybrid，**当** RAG 对话，**则** 保持原有纯向量行为，不影响存量调用

---

### 用户故事 3 - 查询改写与 HyDE 提升召回（优先级：P2）

用户提问模糊（如"那个功能怎么用""它支持吗"）时，系统先把问题交给 LLM 改写成多个更精确的子查询，并生成一条假设性回答文档（HyDE），用这些查询/文档去向量检索，扩大召回面后再融合去重，提高命中率。

**此优先级的原因**：客服提问口语化、指代模糊是常态，改写与 HyDE 是低成本高收益的召回增强；且与 Hybrid 正交，可叠加使用。

**独立测试**：提供独立检索测试接口，输入一个模糊问题，校验输出为：改写后的多查询列表 + HyDE 文档 + 融合后的 Top-N 命中；可关闭该能力保持原检索。

**验收场景**：

1. **假设** 用户问"那个功能怎么用"（无明确名词），**当** 开启查询改写，**则** 系统生成 ≥2 个精确子查询并检索，命中率不低于直接检索
2. **假设** 开启 HyDE，**当** 检索执行，**则** 系统先生成假设性文档再向量化检索，结果与多查询结果融合去重
3. **假设** 改写/HyDE 服务超时或失败，**当** 检索执行，**则** 降级为原始问题检索，不影响主流程

---

### 用户故事 4 - GraphRAG 多跳知识检索（优先级：P2）

知识库中存在跨文档关联知识（如"A 产品 → 配件 B → 兼容 C"），用户问"买 A 需要同时买什么"，系统基于知识图谱做多跳实体关系检索，把关联知识一并注入上下文，回答链路型问题。

**此优先级的原因**：客服知识常是网状关联（产品/配件/政策/流程互相引用），单文档 RAG 答不了跨文档多跳问题；GraphRAG 是 RAG 的进阶形态，价值高但依赖图基础设施。

**独立测试**：在测试环境中用少量三元组构建图谱，验证"实体抽取 → 图谱存储 → 多跳查询 → 上下文注入"完整链路；无图谱数据时自动退化为普通 RAG。

**验收场景**：

1. **假设** 知识库含"退款政策→申请入口→审核时效"的关联三元组，**当** 用户问"退款要多久到账"，**则** 系统通过图谱多跳检索把相关政策与流程片段一并注入上下文
2. **假设** 图谱服务（Neo4j）不可用或未配置，**当** 用户提问，**则** 自动降级为普通 RAG，回答不中断
3. **假设** 图谱为空或未命中实体，**当** 触发检索，**则** 不注入图谱上下文，回答基于普通检索

---

### 用户故事 5 - 智能问数自然语言化与图表（优先级：P2）

用户问"本月订单金额是多少、按商品分类怎么分布"，系统执行 NL2SQL 后，把 JSON 结果转成自然语言结论，并生成前端 ECharts 可直接渲染的图表配置（柱状/折线/饼图），在对话中展示"AI 数据看板"。

**此优先级的原因**：NL2SQL 已能查到数据，但返回的是 JSON 文本，用户看不懂；自然语言结论 + 图表能让问数真正可用，直接提升客服/运营效率。

**独立测试**：调用新增的图表生成接口，输入 SQL 查询结果 JSON 与用户问题，校验输出：自然语言结论 + 图表类型（自动判定）+ ECharts option JSON；前端组件可渲染该配置。

**验收场景**：

1. **假设** 查询返回多行分类聚合数据，**当** 用户问"各分类销量分布"，**则** 返回结论文本 + 饼图/柱状图 ECharts 配置
2. **假设** 查询返回单行汇总（如总数/总金额），**当** 用户问数，**则** 返回结论文本且不生成图表（无分布维度）
3. **假设** SQL 执行失败或无数据，**当** 用户问数，**则** 返回明确提示，不生成图表
4. **假设** 前端收到图表配置，**当** 渲染，**则** 使用 ECharts 正常展示且可导出

---

### 用户故事 6 - 知识库运营闭环（优先级：P2）

运营人员定期查看"用户高频问题聚类"看板：系统把一段周期内的用户提问聚类成主题，标注各主题频次与知识库命中缺口（高频但知识库命中率低的问题），反哺 FAQ 与知识库内容建设。

**此优先级的原因**：知识库质量需要数据驱动迭代，聚类 + 缺口识别让运营从"拍脑袋"变为"看数据"，是 RAG 长期保鲜的关键。

**独立测试**：用一组历史提问文本运行聚类任务，校验输出主题列表（主题名、成员、频次、代表问题）与缺口分析（高频低命中主题被标记）；提供运营看板 API 供前端展示。

**验收场景**：

1. **假设** 提供 100 条历史提问，**当** 运行聚类任务，**则** 输出 5-20 个主题，每个主题含代表问题与占比
2. **假设** 某主题下提问在知识库检索命中率低于阈值，**当** 生成缺口报告，**则** 该主题被标记为"知识库缺口"并给出示例问题
3. **假设** 无历史数据或数据过少，**当** 运行聚类，**则** 返回空结果并给出提示，不报错
4. **假设** 运营确认某主题为 FAQ，**当** 一键收录，**则** 自动生成 FAQ 条目并触发知识库向量更新

---

### 边界情况

- 外部依赖（Rerank / ES / Neo4j / 聚类服务）任一不可用时，所有能力必须降级可用，不得影响核心对话
- golden 测试集为空/格式非法时，评估命令输出明确错误，不崩溃
- 图表数据为空或只有一行时，不强制生成图表
- 查询改写结果为空时，使用原始问题检索
- 高频问题聚类数据量过小时（如 < 20 条），不产生聚类结果并提示
- 所有 AI 调用（改写/评估/聚类）必须复用 ResilientAiService 弹性能力，禁止裸调

## 技术背景

### 问题陈述

当前项目 RAG 已具备：向量检索 + Rerank 精排、引用溯源、ES+向量混合检索（搜索服务侧）、NL2SQL 智能问数。但存在六大缺口：

1. **无质量基线**：检索链路改动没有量化评估手段，无法证明改进、无法防回退；
2. **对话侧未用混合检索**：ai-cs-chat 的 RAG 只用纯向量，型号/编号类问题召回差；
3. **无查询理解**：模糊口语问题直接检索，召回面窄；
4. **无跨文档关联能力**：多跳/网状知识无法回答；
5. **问数结果不可读**：NL2SQL 返回裸 JSON，无自然语言结论、无图表；
6. **知识库无运营闭环**：高频问题、缺口无法数据化发现。

### 设计目标

- 建立 **RAG 质量评估基线**（golden 集 + 指标 + LLM-as-Judge），可离线跑、可进 CI
- 让对话侧 RAG 链路支持 **Hybrid（ES+向量+RRF）** 并默认兼容降级
- 引入 **查询改写 + HyDE** 提升模糊问题召回
- 引入 **GraphRAG** 支持跨文档多跳检索，无图谱时自动降级
- 让 **NL2SQL 结果自然语言化 + ECharts 图表化**
- 建立 **知识库运营闭环**：提问聚类 → 缺口识别 → FAQ 反哺 → 向量更新

## 技术能力规格

### 核心接口

| 接口名称 | 职责 | 关键方法 |
|---------|------|---------|
| `RagEvaluator` | 执行 golden 测试集评估，输出检索/回答质量指标 | `evaluate(RagEvalRequest) → RagEvalReport` |
| `RagEvalDataSource` | 抽象知识库检索数据源，供评估复用 | `retrieve(query, topK) → List<Document>` |
| `HybridRetriever` | 对话侧统一检索入口（向量 / Hybrid / 改写 / HyDE 可组合） | `retrieve(RetrieveRequest) → RetrieveResult` |
| `QueryRewriter` | 把原始问题改写为多个子查询 + HyDE 文档 | `rewrite(question) → RewriteResult` |
| `GraphKnowledgeService` | 知识图谱三元组管理与多跳查询 | `queryMultiHop(entity, depth) → List<GraphHit>` |
| `ChartSpecGenerator` | 把查询结果 JSON 转自然语言结论 + ECharts 配置 | `generate(question, rows) → ChartAnswer` |
| `QuestionClusterService` | 历史提问聚类 + 缺口分析 | `cluster(questions, period) → ClusterReport` |

### 核心类

| 类名 | 包 | 职责 | 关键字段/方法 |
|------|------|------|-------------|
| `RagEvalServiceImpl` | ai-cs-chat/rag/eval | 评估主流程：逐条检索→算指标→LLM Judge 打分→汇总 | `evaluate()` |
| `RetrievalMetrics` | ai-cs-chat/rag/eval | 检索指标计算（Recall@k/MRR/HitRate） | `compute()` |
| `LlmJudgeService` | ai-cs-chat/rag/eval | LLM-as-Judge 打分（复用 ResilientAiService） | `score(question, answer, reference)` |
| `HybridRagRetriever` | ai-cs-chat/rag/retrieve | 对话侧混合检索编排（调用 ai-cs-search Feign 或内嵌组件） | `retrieve()` |
| `QueryRewriteService` | ai-cs-chat/rag/rewrite | LLM 改写 + HyDE 生成 | `rewrite()` / `generateHyde()` |
| `GraphRagService` | ai-cs-chat/rag/graph | 图谱检索编排，无图时降级 | `retrieveWithGraph()` |
| `ChartAnswerGenerator` | ai-cs-chat/nl2sql | 结论 + 图表配置生成 | `generate()` |
| `QuestionClusterServiceImpl` | ai-cs-knowledge/ops | 聚类 + 缺口分析 | `cluster()` / `detectGaps()` |
| `FaqSuggestion` | ai-cs-knowledge/ops | FAQ 收录与向量更新触发 | `save()` |

### 核心枚举

- **`RetrievalMode`**：`VECTOR`（纯向量）/ `HYBRID`（混合）/ `HYBRID_QUERY_REWRITE`（混合+改写）/ `GRAPH_RAG`（图谱优先）
- **`ChartType`**：`PIE` / `BAR` / `LINE` / `NONE`
- **`ClusterStatus`**：`DRAFT` / `FAQ_ADOPTED` / `IGNORED`

### 数据模型

- **`GoldenCase`**：golden 测试集条目（id、question、expectedDocumentIds、referenceAnswer、knowledgeBase）
- **`RagEvalReport`**：评估报告（指标、LLM 分数、逐条明细、时间戳）
- **`RewriteResult`**：改写结果（原始问题、子查询列表、hydeDocument）
- **`GraphTriple`**：图谱三元组（subject、predicate、object、knowledgeBase）
- **`ChartAnswer`**：问数回答（conclusion、chartType、echartsOption、rows）
- **`ClusterTopic`**：聚类主题（topic、questionIds、count、ratio、gapFlag、representativeQuestions）
- **`ClusterReport`**：聚类报告（topics、gapTopics、period、totalQuestions）

## 需求 *（必填）*

### 功能需求

- **FR-001**：系统必须支持加载 golden 测试集（JSON）并执行 RAG 评估，输出 Recall@k、MRR、HitRate 与 LLM-as-Judge 回答质量均分
- **FR-002**：评估必须支持指定 knowledgeBase、检索模式（向量/Hybrid）与 Top-K，便于对比实验
- **FR-003**：评估报告必须包含逐条明细（问题、命中文档、指标、分数），并支持 CI 阈值门禁（质量分/命中率低于阈值即失败）
- **FR-004**：对话侧 RAG 必须支持 Hybrid 模式：ES 关键词 + 向量语义 + RRF 融合后注入上下文
- **FR-005**：Hybrid 模式单路失败必须降级为另一路，整体回答不中断
- **FR-006**：检索必须支持查询改写：LLM 生成 ≥2 个子查询，多查询结果融合去重
- **FR-007**：检索必须支持 HyDE：LLM 生成假设性文档 → 向量化检索 → 与子查询结果融合
- **FR-008**：改写/HyDE 服务异常时必须降级为原始问题检索
- **FR-009**：系统必须支持知识图谱三元组入库与多跳查询（深度可配置），命中后注入图谱上下文
- **FR-010**：图谱未配置/不可用/未命中时，必须自动降级为普通 RAG
- **FR-011**：NL2SQL 结果必须支持生成自然语言结论与 ECharts 图表配置（柱状/折线/饼图自动判定）
- **FR-012**：单行/无分布维度数据不得强制生成图表
- **FR-013**：系统必须支持对历史提问做聚类，输出主题、频次、占比与代表问题
- **FR-014**：聚类结果必须支持缺口分析：高频且知识库命中率低的问题标记为缺口
- **FR-015**：运营确认主题为 FAQ 后，系统必须自动生成 FAQ 条目并触发知识库向量更新

### 关键实体

- **GoldenCase / RagEvalReport / RetrievalMetrics**：评估体系实体
- **RewriteResult**：查询改写实体
- **GraphTriple / GraphHit**：图谱实体
- **ChartAnswer**：问数图表实体
- **ClusterTopic / ClusterReport**：运营闭环实体
- **FAQ 条目**：反哺知识库的 FAQ 实体（复用 KnowledgeDocument 或扩展）

## 非功能需求

### 性能要求

- 评估：100 条 golden 集全量评估 ≤ 5 分钟（含 LLM 调用）
- Hybrid 检索：检索阶段（不含 LLM 生成）≤ 2 秒
- 改写/HyDE：改写阶段 ≤ 3 秒，超时降级
- 图谱多跳查询：单次 ≤ 2 秒（深度 ≤ 3）
- 图表生成：≤ 3 秒（含 LLM 调用）
- 聚类任务：1 万条提问 ≤ 5 分钟

### 可观测性要求

- **响应时间**: 新增检索链路 P95 延迟 ≤ 5s（含 LLM 生成）
- **可用性**: 任一新增外部依赖不可用时自动降级，核心对话可用性不受影响
- **日志**: 检索/评估/聚类链路必须有结构化日志（模式、命中数、耗时、降级原因）
- **代码覆盖率**: 新增核心逻辑（指标计算、RRF 融合、图表判定、聚类）单元测试行覆盖率 ≥ 60%

### 安全要求

- 所有 LLM 调用复用 ResilientAiService（超时/重试/熔断），禁止裸调
- golden 测试集与 FAQ 数据入库必须做内容校验，禁止注入
- 图谱三元组写入必须走白名单校验（subject/predicate/object 非空、长度限制）
- 图表配置生成只读，不涉及数据写权限

### 可扩展性要求

- 检索模式通过枚举 + 策略组合，新增模式零侵入（RetrievalMode 扩展）
- 评估数据源抽象为接口，支持向量/Hybrid/图谱不同数据源复用
- 聚类算法可替换（Embedding 聚类 / 文本 TF-IDF / LLM 聚类），接口隔离
- 图谱存储抽象为接口（Neo4j 为默认实现），支持替换

## 集成规格

### 依赖模块

- **ai-cs-chat**：核心宿主（检索、评估、改写、图谱、问数图表）
- **ai-cs-search**：提供 ES+向量 RRF 混合检索能力（对话侧通过 Feign 或本地组件复用）
- **ai-cs-knowledge**：知识文档数据源、FAQ 落库、向量更新触发
- **ai-cs-frontend**：图表渲染（ECharts）、聚类看板、评估报告展示
- **ai-cs-common**：Result/ResultCode/BusinessException 复用

### 被依赖模块

- 无新增反向依赖；ai-cs-chat 不得依赖 ai-cs-search 内部类，必须通过 Feign/HTTP 契约

## 测试规格

### 单元测试

- RetrievalMetrics 指标计算（Recall@k/MRR/HitRate）边界与空集
- RrfMerger 复用测试（若新增多查询融合逻辑）
- QueryRewriteService 改写结果解析、空结果降级
- ChartAnswerGenerator 图表类型判定（分布/单行/空数据）
- QuestionClusterServiceImpl 聚类分组与缺口判定
- GraphRagService 命中/未命中/降级三分支

### 集成测试

- golden 评估端到端（Mock 检索数据源 + Mock LLM Judge）
- Hybrid RAG 对话（Mock ES + Mock 向量库）校验引用溯源
- 图谱多跳查询（嵌入式图存储或 Mock）校验上下文注入
- 聚类 → FAQ 收录 → 向量更新触发全链路

## 成功标准 *（必填）*

### 可衡量的结果

- **SC-001**：golden 测试集（≥20 条）可一键运行评估，输出 4 类指标（Recall@5/MRR/HitRate/LLM 分数），CI 可门禁
- **SC-002**：含精确型号的查询，Hybrid RAG 命中率 ≥ 纯向量模式（在 golden 集上对比）
- **SC-003**：开启查询改写/HyDE 后，模糊问题在 golden 集上的 Recall@5 不下降（允许持平）
- **SC-004**：图谱链路在未配置/不可用时 100% 降级为普通 RAG，回答不中断
- **SC-005**：NL2SQL 结果 ≥90% 可生成结论文本；有分布维度时生成正确图表类型
- **SC-006**：聚类任务在 ≥100 条提问上输出 ≥5 个主题，缺口主题被标记；FAQ 收录后向量自动更新

## 假设与约束

### 假设

- 硅基流动/DeepSeek 等 LLM API Key 已配置（Nacos），评估与改写复用现有模型
- ES 8.12 已在基础设施可用（ai-cs-search 已依赖）
- Neo4j 为可选依赖：未配置时 GraphRAG 自动禁用（默认关闭，避免阻塞主链路）
- golden 测试集由运营/研发人工维护，存放于 `deploy/eval/` 或 `ai-cs-chat/src/test/resources/eval/`

### 约束

- 不引入新的微服务模块，全部能力落在 ai-cs-chat / ai-cs-knowledge / ai-cs-frontend 现有模块
- 对话侧默认保持纯向量模式（hybrid 需显式开启），保证存量行为不变
- 图表前端使用 ECharts（ai-cs-frontend 现有技术栈）
- 评估 CLI 以独立可执行入口提供（Maven exec 或测试类），不依赖完整微服务启动

## 附录：相关代码位置（规划）

| 说明 | 路径 |
|---|---|
| 对话侧检索编排 | `ai-cs-chat/src/main/java/com/aics/chat/rag/retrieve/` |
| 评估体系 | `ai-cs-chat/src/main/java/com/aics/chat/rag/eval/` |
| 查询改写/HyDE | `ai-cs-chat/src/main/java/com/aics/chat/rag/rewrite/` |
| 图谱检索 | `ai-cs-chat/src/main/java/com/aics/chat/rag/graph/` |
| 问数图表 | `ai-cs-chat/src/main/java/com/aics/chat/nl2sql/` |
| 运营闭环 | `ai-cs-knowledge/src/main/java/com/aics/knowledge/ops/` |
| 前端图表/看板 | `ai-cs-frontend/src/views/` |
