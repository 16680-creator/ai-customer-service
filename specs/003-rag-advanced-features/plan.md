# 实施计划：RAG 进阶六件套

**分支**: `003-rag-advanced-features` | **日期**: 2026-08-12 | **规格**: [spec.md](spec.md)
**输入**: 来自 `/specs/003-rag-advanced-features/spec.md` 的功能规格

## 摘要

在现有 RAG（向量 + Rerank + 引用溯源 + 搜索侧混合检索 + NL2SQL）基础上新增六项能力：

1. **RAG 评估体系**（US1）：golden 集 + RetrievalMetrics + LLM-as-Judge，CI 门禁
2. **Hybrid RAG 接入对话**（US2）：chat 侧 RAG 链路接入 ES+向量+RRF 混合检索（Feign 调 ai-cs-search）
3. **查询改写 / HyDE**（US3）：LLM 多查询改写 + 假设性文档，RRF 融合
4. **GraphRAG 多跳检索**（US4）：三元组图谱 + 多跳 BFS，无图降级
5. **智能问数自然语言化与图表**（US5）：结论 + ECharts 配置自动生成
6. **知识库运营闭环**（US6）：提问聚类 + 缺口识别 + FAQ 反哺 + 向量更新

技术方案详见 [research.md](research.md)、[data-model.md](data-model.md)、[contracts/rest-api.md](contracts/rest-api.md)。

## 技术上下文

**语言/版本**：Java 17（本机编译 JDK 21）/ Spring Boot 3.2.5 / Spring Cloud 2023 / Spring AI 1.1.4
**主要依赖**：spring-ai（ChatClient/VectorStore/EmbeddingModel）、Feign（OpenFeign）、MyBatis-Plus、RocketMQ、Redis、Resilience4j；前端 Vue 3 + Element Plus + Vite + ECharts（新增）
**存储**：MySQL（kb_graph_triple / kb_faq 新表）、Chroma（向量）、Redis（会话）、ES（关键词）
**测试**：JUnit 5 + Mockito + JaCoCo（行覆盖率 ≥ 40%、分支 ≥ 30%，目标 60%/50%）
**目标平台**：Linux 服务器（微服务）+ 浏览器（前端）
**项目类型**：Web 微服务 + Vue SPA
**性能目标**：检索（不含 LLM）≤ 2s；改写 ≤ 3s；图表 ≤ 3s；评估 100 条 ≤ 5 分钟；聚类 1 万条 ≤ 5 分钟
**约束**：不新增微服务模块；对话侧默认纯向量；所有 LLM 调用复用弹性能力；新增代码强制 TDD
**规模/范围**：6 个用户故事，落在 ai-cs-chat / ai-cs-knowledge / ai-cs-frontend

## 宪法检查

*门禁：第 0 阶段研究前通过，第 1 阶段设计后复检。*

| 条款 | 结论 | 说明 |
|------|------|------|
| 第2条 SDD 流程 | ✅ | 经 /speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement 顺序推进 |
| 第2-1条 TDD | ✅ 计划中 | tasks 阶段为每个实现任务配置前置测试任务（Red→Green→Refactor） |
| 第12条 文档规范 | ✅ | 中文文档/注释、英文代码命名、SpringDoc 注解、Conventional Commits |
| 第13条 配置安全 | ✅ | 新增 API Key/开关全部走 Nacos（aics.rag.*），不硬编码 |
| 第16条 模块架构 | ✅ | 不新增模块；chat 调 search 走 Feign，不依赖内部类 |
| 第17条 技术优先 | ✅ | 评估/改写/图表优先复用 Spring AI + 现有组件；RRF/聚类/指标自实现并单测 |
| 第20条 编码规范 | ✅ | 构造器注入、Lombok、SLF4J 英文日志、Result<T> 统一响应 |
| 第21条 领域模型 | ✅ | 新 DTO/VO 命名遵循项目规范（*DTO/*VO/*Request） |
| 第22条 Git 工作流 | ✅ 计划中 | 按功能分类提交（feat(chat)/feat(knowledge)/feat(frontend)/docs/test） |

**门禁结论**：通过，无违规项，可进入 Phase 0。

## 项目结构

### 文档（本功能）

```text
specs/003-rag-advanced-features/
├── plan.md              # 本文件
├── research.md          # 第 0 阶段：技术调研与决策
├── data-model.md        # 第 1 阶段：数据模型设计
├── quickstart.md        # 第 1 阶段：快速启动与验证指南
├── contracts/
│   └── rest-api.md      # 第 1 阶段：REST API 契约
├── checklists/
│   └── requirements.md  # 规格质量检查清单
└── tasks.md             # 第 2 阶段输出（/speckit-tasks 生成）
```

### 源代码（仓库根目录）

```text
ai-cs-chat/src/main/java/com/aics/chat/
├── rag/
│   ├── eval/                    # US1 评估体系
│   │   ├── RagEvaluator.java            (接口)
│   │   ├── RagEvalServiceImpl.java
│   │   ├── RetrievalMetrics.java        (纯函数指标计算)
│   │   ├── GoldenCase.java / RagEvalReport.java / RagEvalCaseResult.java
│   │   ├── LlmJudgeService.java         (复用 ChatClient 打分)
│   │   └── RagEvalProperties.java
│   ├── retrieve/                # US2/US3 对话侧检索编排
│   │   ├── HybridRetriever.java / RetrieveResult.java / RetrievalMode.java
│   │   ├── MultiQueryMerger.java        (RRF 多路融合，纯函数)
│   │   └── RagRetrieveProperties.java
│   ├── rewrite/                 # US3 查询改写/HyDE
│   │   ├── QueryRewriteService.java / RewriteResult.java
│   │   └── QueryRewriteProperties.java
│   └── graph/                   # US4 图谱
│       ├── GraphKnowledgeService.java / GraphStore.java / GraphTriple.java / GraphHit.java
│       ├── InMemoryGraphStore.java / GraphRagService.java / GraphProperties.java
├── nl2sql/
│   └── chart/                   # US5 问数图表
│       ├── ChartAnswerGenerator.java / ChartAnswer.java / ChartType.java / ChartTypeDetector.java
│       └── ChartProperties.java
├── feign/
│   └── SearchFeignClient.java   # US2 调 ai-cs-search /search/hybrid
├── controller/
│   ├── RetrieveController.java  # /chat/retrieve/test
│   ├── ChartController.java     # /chat/chart
│   ├── GraphController.java     # /rag/graph/*
│   └── RagEvalController.java   # /rag/eval/run
└── config/
    └── RagAdvancedConfig.java   # 装配评估/改写/图谱/检索 Bean + Feign

ai-cs-chat/src/test/java/com/aics/chat/
├── rag/eval/RetrievalMetricsTest.java / RagEvalServiceImplTest.java
├── rag/retrieve/MultiQueryMergerTest.java / HybridRetrieverTest.java
├── rag/rewrite/QueryRewriteServiceTest.java
├── rag/graph/InMemoryGraphStoreTest.java / GraphRagServiceTest.java
├── nl2sql/chart/ChartTypeDetectorTest.java / ChartAnswerGeneratorTest.java
└── eval/RagEvaluationTest.java   # golden 集端到端（-Peval）

ai-cs-chat/src/test/resources/eval/golden-set.json   # US1 内置 golden 集（20 条）

ai-cs-knowledge/src/main/java/com/aics/knowledge/
├── ops/
│   ├── QuestionClusterService.java / ClusterReport.java / ClusterTopic.java / FaqSuggestion.java
│   ├── EmbeddingClusterService.java      # 向量贪心聚类（纯 Java）
│   ├── KnowledgeGapDetector.java         # 缺口分析
│   └── FaqService.java                   # FAQ 收录 → 触发向量化
├── controller/
│   └── KnowledgeOpsController.java       # /knowledge/ops/*
├── entity/
│   ├── KnowledgeGraphTriple.java
│   └── KnowledgeFaq.java
├── mapper/
│   ├── KnowledgeGraphTripleMapper.java
│   └── KnowledgeFaqMapper.java
└── config/
    └── OpsConfig.java

ai-cs-frontend/
├── package.json                 # 新增 echarts 依赖
├── src/views/
│   ├── ChatDashboardView.vue    # US5 问数图表页
│   └── KnowledgeOpsView.vue     # US6 聚类看板页
└── src/api/index.js             # 新增接口封装

deploy/sql/                      # kb_graph_triple / kb_faq 建表 SQL
```

**结构决策**：不新增微服务模块；按领域分包（eval/retrieve/rewrite/graph/chart/ops），各包自包含接口+实现+配置，符合宪法第16-1条包结构规范。

## 复杂度追踪

> 无需登记违规项；所有新增复杂度均有明确业务需求且无法用更简单方案替代（评估指标/图表判定/聚类为确定性逻辑，必须自实现）。

## 实施阶段（对应 tasks.md）

- **Phase 1**：依赖与配置（Feign、ECharts、Nacos 配置项）
- **Phase 2**：基础组件（DTO/指标计算/RRF 融合/图表判定/图存储）
- **Phase 3-8**：US1-US6 逐个实现（TDD：测试先行）
- **Phase 9**：前端（图表渲染 + 运营看板）
- **Phase 10**：CI 门禁与文档收尾
