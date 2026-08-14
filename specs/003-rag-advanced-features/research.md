# 技术调研与决策：RAG 进阶六件套

> 第 0 阶段输出（/speckit-plan），解决 spec 中所有 NEEDS CLARIFICATION 与技术选型。

## 决策总览

| # | 决策点 | 结论 | 理由 |
|---|--------|------|------|
| D1 | 评估体系形态 | ai-cs-chat 内嵌离线评估包（rag/eval），以 JUnit 测试 + Maven profile 作为 CI 门禁 | 不新增服务；评估本质是"跑 golden 集 → 出报告"，复用现有检索与 LLM 组件最省成本；CI 门禁用测试失败表达最直接 |
| D2 | golden 集存放 | `ai-cs-chat/src/test/resources/eval/golden-set.json`（测试用内置 20 条）+ `deploy/eval/golden-set-prod.json`（生产样本） | 测试资源随代码走、CI 天然可用；生产样本走部署目录便于运营维护 |
| D3 | 检索指标 | 自实现 RetrievalMetrics：Recall@k、MRR、HitRate（纯函数） | 指标计算简单确定，无外部依赖，符合宪法 TDD 单元测试范围 |
| D4 | LLM-as-Judge | 复用现有 ChatClient（DeepSeek），prompt 打分 1-5，输出 JSON | 与现有模型一致，不引入新供应商；复用 ResilientAiService 弹性能力 |
| D5 | Hybrid 接入对话方式 | ai-cs-chat 新增 `SearchFeignClient` 调 ai-cs-search `/search/hybrid`；HybridRetriever 编排 | 宪法禁止跨模块直接依赖内部类；ai-cs-search 已暴露混合检索 HTTP 契约 |
| D6 | Hybrid 默认行为 | 默认 `VECTOR`，显式传 `hybrid=true` 才启用 | 保持存量行为不变（spec 约束），降级路径天然兼容 |
| D7 | 查询改写 / HyDE | QueryRewriteService：LLM 输出 JSON 子查询数组 + HyDE 假设文档；多路结果用 RRF 融合 | RRF 已在 ai-cs-search 验证过；chat 内新建轻量 `MultiQueryMerger`（含测试），避免跨模块依赖 |
| D8 | 改写/HyDE 降级 | 任何异常/超时 → 用原始问题检索 | spec FR-008；LLM 不可用不影响主链路 |
| D9 | GraphRAG 存储 | 抽象 `GraphStore` 接口；默认 `InMemoryGraphStore`（进程内，测试/开发用）+ 可选 Neo4j 实现（配置开关） | Neo4j 非必备基础设施；默认关闭不阻塞主链路，满足 FR-010 降级要求 |
| D10 | 图谱构建方式 | 三元组由运营/脚本显式写入（REST API），LLM 实体抽取为可选增强 | MVP 先保证确定性；实体抽取放后续迭代 |
| D11 | 图表类型判定 | ChartTypeDetector 纯函数：多行 + 分类维度 → PIE/BAR；含时间列 → LINE；单行/空 → NONE | 确定性逻辑可单测；结论文本由 LLM 生成（失败降级为模板摘要） |
| D12 | ECharts 配置生成 | Java 侧拼标准 ECharts option JSON（不含 LLM），前端直接渲染 | option 结构确定，避免 LLM 幻觉出非法 JSON |
| D13 | 聚类算法 | 基于 Embedding 向量余弦相似度的贪心聚类（阈值配置）+ 代表问题取频次最高；缺口 = 主题内检索命中率低于阈值 | 无外部 ML 依赖，可单测；Embedding 复用 bge-m3 |
| D14 | FAQ 反哺 | 复用 ai-cs-knowledge 的 KnowledgeDocument 创建链路（→ RocketMQ → 自动向量化） | 已有增量同步能力（002），零新增链路 |
| D15 | 运营看板 API | ai-cs-knowledge 暴露 `/knowledge/ops/cluster`、`/knowledge/ops/faq`；前端新增聚类看板页 | 数据源在 knowledge 模块（历史提问经 message 服务或上传导入） |

## 需要澄清项（已用默认决策解决）

| 澄清点 | 默认决策 | 影响 |
|--------|---------|------|
| golden 集来源 | 内置测试样本 + 部署目录样本 | 运营后续可替换 |
| Neo4j 是否必须 | 否，默认 InMemory + 开关 | 无图时 100% 降级 |
| 图表前端技术 | ECharts（项目已有 Element Plus 生态） | 新增 echarts 依赖 |
| 聚类数据源 | ai-cs-knowledge 提供导入接口（CSV/JSON）+ message 表查询两种方式 | MVP 先支持导入 |

## 架构约束确认

- 不新增微服务模块；能力落在 ai-cs-chat / ai-cs-knowledge / ai-cs-frontend
- ai-cs-chat 不得依赖 ai-cs-search 内部类（走 Feign HTTP 契约）
- 所有 LLM 调用走 ResilientAiService 或同配置 ChatClient
- 新增代码遵循 TDD：先写失败测试（Red）→ 最小实现（Green）→ 重构（Refactor）
