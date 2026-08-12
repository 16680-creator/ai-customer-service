# 实施计划：RAG 检索质量升级五件套

**分支**: `002-rag-quality-upgrade` | **日期**: 2026-08-12 | **规格**: [spec.md](spec.md)
**输入**: 来自 `specs/002-rag-quality-upgrade/spec.md` 的功能规格

## 摘要

对现有 RAG 检索链路实施五项升级：① **Rerank 重排序**——`ai-cs-chat` 知识库检索由"单次向量 Top-5"升级为"向量召回 Top-20 → 硅基流动 bge-reranker-v2-m3 精排 → 阈值过滤取 Top-5"，失败自动降级为原始排序；② **混合检索**——`ai-cs-search` 增加 ES BM25 关键词检索与 Chroma 向量检索双路召回，RRF 算法融合排序，解决型号/订单号精确查询短板；③ **引用溯源**——RAG 回答（含流式）返回命中文档元数据（文档名/页码/相似度/片段），前端 ChatView 以卡片展示出处；④ **文档格式扩展**——`ai-cs-chat` 引入 spring-ai-tika-document-reader（Apache Tika），支持 docx/xlsx/html/md 解析入库；⑤ **知识库增量同步**——`ai-cs-knowledge` 文档创建/更新/删除后发 RocketMQ 消息，消费者异步执行向量化/清理，解耦 DB 操作与向量化。

技术方案：全部基于现有模块改造，不新增微服务。Rerank 用 Spring RestClient 直调硅基流动 HTTP API；混合检索用 `co.elastic.clients:elasticsearch-java`（父 POM 已管版本 8.12.2）+ 现有 Chroma VectorStore；增量同步复用 `rocketmq-spring-boot-starter`（父 POM 已管版本 2.3.0）。

## 技术上下文

**语言/版本**：Java 17、Spring Boot 3.2.5、Spring AI 1.1.4
**主要依赖**：spring-ai-starter-model-openai、spring-ai-starter-vector-store-chroma、spring-ai-tika-document-reader（新增）、elasticsearch-java 8.12.2（ai-cs-search 新增）、rocketmq-spring-boot-starter 2.3.0（ai-cs-knowledge 新增）
**存储**：Chroma（向量）、Elasticsearch 8.12（BM25 关键词索引，新增）、MySQL（知识文档表，已有）、RocketMQ（同步消息，已有）
**测试**：JUnit 5 + Mockito（单元）、Spring Boot Test（集成）；MockRestServiceServer 模拟 Rerank HTTP
**目标平台**：Linux 服务器（Docker 容器化）
**项目类型**：微服务（Web 服务）改造
**性能目标**：RAG 检索（召回+精排）< 2s；混合检索 < 2s；同步消息消费延迟 < 5s
**约束**：Rerank/ES 不可用时降级不阻断；API Key 走 Nacos/环境变量；RAG 最终回答阈值 ≥ 0.7（宪法第20-1条）
**规模/范围**：改造 3 个后端模块（chat/search/knowledge）+ 1 个前端页面，约 15 个新类、8 个改动类

## 宪法检查

*门禁：必须在第 0 阶段研究前通过。第 1 阶段设计后重新检查。*

| 宪法条款 | 合规状态 | 说明 |
|----------|----------|------|
| 第2条 SDD流程 | ✅ 通过 | 按 specify → plan → tasks → implement 顺序推进 |
| 第2-1条 TDD | ✅ 计划中 | tasks 阶段为每个实现任务配置前置测试任务（Red→Green→Refactor） |
| 第12条 文档规范 | ✅ 通过 | 中文文档/注释/commit、英文代码命名、SpringDoc 注解、Conventional Commits |
| 第13条 配置安全 | ✅ 通过 | Rerank API Key 通过 Nacos（aics.rerank.api-key）注入，占位符本地兜底 |
| 第13-2条 公共复用 | ✅ 通过 | 复用 ai-cs-common 的 Result/ResultCode/BusinessException/GlobalExceptionHandler；混合检索放 ai-cs-search 自身模块 |
| 第16条 模块架构 | ✅ 通过 | 不新增模块、不破坏依赖方向；chat/knowledge/search 均只依赖 common + Spring AI |
| 第16-1条 包结构 | ✅ 通过 | 新类放入现有模块既有包结构（service/config/rag/mq/dto），见"项目结构"包结构映射表 |
| 第17条 技术优先 | ✅ 通过 | 优先 Spring AI 官方能力（Tika DocumentReader、VectorStore）；Rerank/RRF 无官方能力才自定义 |
| 第20条 编码规范 | ✅ 通过 | 构造器注入、Lombok、SLF4J 英文日志、Result<T> 统一响应 |
| 第20-1条 Spring AI规范 | ✅ 通过 | 最终回答 Top-5、精排后相似度阈值 ≥ 0.7（配置化）；召回 Top-20 仅为候选池（pipeline 内部步骤，不直接用于回答）；temperature ≤ 0.3 保持现状 |
| 第21条 领域模型 | ✅ 通过 | CitationItemDTO、RerankedHitDTO、HybridHitVO 命名符合 DTO/VO 规范 |
| 第22条 Git工作流 | ✅ 计划中 | 实现完成后按功能分类提交（feat(chat)/feat(search)/feat(knowledge)/feat(frontend)/docs/test） |

**门禁结论**：全部通过，无违规项，可进入 Phase 0。

## 项目结构

### 文档（本功能）

```text
specs/002-rag-quality-upgrade/
├── plan.md              # 本文件
├── research.md          # 第 0 阶段：技术调研与决策
├── data-model.md        # 第 1 阶段：数据模型设计（ES 索引 + MQ 消息 + 引用 DTO）
├── quickstart.md        # 第 1 阶段：快速启动与验证指南
├── contracts/           # 第 1 阶段：API 契约
│   └── rest-api.md      # REST API 契约（改动接口 + 新增接口）
├── checklists/
│   └── requirements.md  # 规格质量检查清单
└── tasks.md             # 第 2 阶段输出（/speckit-tasks 生成）
```

### 源代码（仓库根目录）

```text
ai-cs-chat/src/main/java/com/aics/chat/
├── config/
│   └── SpringAiConfig.java              ← 修改：注入 RerankService（可选）
├── rag/
│   ├── DocumentLoader.java              ← 修改：新增 loadTika() 多格式解析
│   └── rerank/
│       ├── RerankService.java           ← 新增：重排序服务接口
│       ├── SiliconFlowRerankService.java← 新增：硅基流动 bge-reranker-v2-m3 实现（RestClient）
│       └── RerankProperties.java        ← 新增：rerank 配置属性（base-url/model/api-key/top-n/阈值）
├── dto/
│   ├── CitationItemDTO.java             ← 新增：引用溯源 DTO（documentId/title/page/score/content）
│   └── ChatRagResponseDTO.java          ← 新增：RAG 响应 DTO（content + citations）
├── service/
│   ├── KnowledgeBaseService.java        ← 修改：两阶段检索（召回 Top-20 → Rerank → Top-5）；metadata 带 documentId/title/page
│   └── ChatService.java                 ← 修改：chatWithRag 返回 ChatRagResponseDTO
├── service/impl/
│   ├── ChatServiceImpl.java             ← 修改：chatWithRag/chatStreamSse 携带引用元数据（done 事件含 citations）
│   └── ResilientAiService.java          ← 修改：RAG Prompt 拼接引用编号【资料N】保持一致
└── mq/
    └── KnowledgeSyncConsumer.java       ← 新增：消费 knowledge-doc-sync-topic，异步向量化/删除（复用 KnowledgeBaseService）

ai-cs-chat/pom.xml                       ← 修改：新增 spring-ai-tika-document-reader

ai-cs-search/src/main/java/com/aics/search/
├── config/
│   └── ElasticsearchConfig.java         ← 新增：ElasticsearchClient Bean（uris 走 Nacos 配置）
├── hybrid/
│   ├── HybridSearchService.java         ← 新增：混合检索服务（ES BM25 + Chroma 向量 → RRF 融合）
│   └── RrfMerger.java                   ← 新增：RRF 融合排序工具（纯函数，便于单测）
├── service/
│   └── impl/
│       ├── SearchServiceImpl.java       ← 修改：indexDocument/deleteIndex 同步维护 ES 关键词索引
│       └── HybridSearchServiceImpl.java ← 新增：实现 HybridSearchService
├── controller/
│   └── SearchController.java            ← 修改：新增 /hybrid 混合检索接口
└── SearchApplication.java               ← 修改：排除 ES 自动配置或按需配置

ai-cs-search/pom.xml                     ← 修改：新增 elasticsearch-java

ai-cs-knowledge/src/main/java/com/aics/knowledge/
├── mq/
│   ├── KnowledgeSyncProducer.java       ← 新增：文档变更消息生产者（CREATE/UPDATE/DELETE）
│   └── KnowledgeSyncConsumer.java       ← 新增：消费消息，异步 vectorize / deleteByDocumentId
├── service/
│   ├── KnowledgeVectorService.java      ← 修改：metadata 补 documentId/title；新增 deleteByDocumentId()
│   └── impl/KnowledgeServiceImpl.java   ← 修改：DB 操作后改发 MQ（替代同步 vectorize）
└── config/
    └── KnowledgeAiConfig.java           ← 修改（如需）：RocketMQ 消费组/主题常量

ai-cs-knowledge/pom.xml                  ← 修改：新增 rocketmq-spring-boot-starter

ai-cs-frontend/src/views/
└── ChatView.vue                         ← 修改：解析 done 事件 citations，渲染引用卡片（el-card + 折叠面板）

deploy/nacos/configs/
├── ai-cs-chat.yml                       ← 修改：新增 aics.rerank.* 配置
└── ai-cs-search.yml                     ← 修改：新增 spring.elasticsearch.uris + aics.hybrid.* 配置

learning-docs/05-AI集成/                 ← 文档交付（用户指定目录）
└── 02-RAG全栈实战/
    ├── 03-RAG向量检索实战.md             ← 更新：两阶段检索链路（召回+Rerank）
    ├── 04-RAG进阶实战-Rerank重排序.md    ← 新增
    ├── 05-混合检索实战-ES-BM25与向量RRF融合.md ← 新增
    ├── 06-引用溯源实战-带出处回答.md      ← 新增
    ├── 07-文档格式扩展实战-Tika多格式解析.md ← 新增
    └── 08-知识库增量同步实战-RocketMQ驱动向量化.md ← 新增
```

### 包结构映射表（宪法第16-1条）

| 新类 | 所在模块 | 包路径 | 依据 |
|------|----------|--------|------|
| RerankService / SiliconFlowRerankService / RerankProperties | ai-cs-chat | com.aics.chat.rag.rerank | rag 子包已存在（DocumentLoader），重排序属 RAG 检索能力 |
| CitationItemDTO / ChatRagResponseDTO | ai-cs-chat | com.aics.chat.dto | dto 包已存在 |
| KnowledgeSyncConsumer | ai-cs-chat | com.aics.chat.mq | mq 包已存在（ChatMessageProducer） |
| ElasticsearchConfig / HybridSearchService / RrfMerger | ai-cs-search | com.aics.search.config / com.aics.search.hybrid / com.aics.search.service | config/service 包已存在，hybrid 为新增子包 |
| KnowledgeSyncProducer / KnowledgeSyncConsumer | ai-cs-knowledge | com.aics.knowledge.mq | 新增 mq 子包（模块内新建，符合模块边界） |

**结构决策**：所有改动在既有模块内完成，不新增微服务、不破坏依赖方向。Rerank 与 RRF 为自定义算法组件（Spring AI 1.1.4 无内置），放置于独立子包便于测试与替换。

## 复杂度追踪

> **仅在第20-1条处有必须说明的设计决策**

| 违规项 | 为何需要 | 拒绝更简单替代方案的原因 |
|--------|----------|--------------------------|
| 第20-1条：召回阶段 topK=20 超出 5-10 | 两阶段检索需要足够候选池供精排，否则 Rerank 无意义 | 直接 topK=5 无精排空间，回答精度无法提升 |
| 第20-1条：召回阶段阈值低于 0.7 | 候选池需保证召回率（recall），高阈值导致候选不足 | 最终回答阶段精排分数阈值 ≥ 0.7（配置化），回答质量满足宪法要求 |
