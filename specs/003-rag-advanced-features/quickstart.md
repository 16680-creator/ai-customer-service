# 快速启动与验证指南：RAG 进阶六件套

> 第 1 阶段输出（/speckit-plan）。面向开发验证的最小路径。

## 前置条件

- JDK 17+（推荐 JDK 21）、Maven、MySQL/Redis/RocketMQ/Nacos/ES/Chroma（docker-compose 已覆盖）
- Nacos 已发布 `ai-cs-chat.yml`、`aics-shared.yml`（含 LLM/Embedding API Key）

## 编译与单元测试

```bash
mvn -pl ai-cs-chat,ai-cs-knowledge -am clean test -DskipTests=false
```

## 核心验证路径

### 1. RAG 评估（US1）

```bash
# 运行内置 golden 集评估测试（CI 门禁用）
mvn -pl ai-cs-chat test -Dtest=RagEvaluationTest -Peval
```

验证：测试类加载 `classpath:eval/golden-set.json` → 执行检索 → 输出指标 → 断言通过。

### 2. Hybrid 检索测试（US2/US3）

```bash
curl "http://localhost:8080/chat/retrieve/test?knowledgeBase=product-manual&query=ABC-123%20保修&mode=HYBRID&topK=5"
```

验证：`mode=HYBRID` 返回混合检索结果；`mode=HYBRID_QUERY_REWRITE` 先改写再检索；无 key 时降级。

### 3. 问数图表（US5）

```bash
curl -X POST http://localhost:8080/chat/chart -H "Content-Type: application/json" -d "{\"question\":\"各分类销量分布\",\"rows\":[{\"category\":\"手机\",\"sales\":1200},{\"category\":\"平板\",\"sales\":800}]}"
```

验证：返回 conclusion + chartType=PIE + echartsOption。

### 4. 图谱（US4）

```bash
curl -X POST http://localhost:8080/rag/graph/triple -H "Content-Type: application/json" -d "{\"subject\":\"退款政策\",\"predicate\":\"指向\",\"object\":\"申请入口\",\"knowledgeBase\":\"product-manual\"}"
curl "http://localhost:8080/rag/graph/query?entity=%E9%80%80%E6%AC%BE%E6%94%BF%E7%AD%96&depth=2&knowledgeBase=product-manual"
```

验证：多跳展开三元组；未配置图谱时接口返回空并提示降级。

### 5. 运营闭环（US6）

```bash
curl -X POST http://localhost:8080/knowledge/ops/cluster -H "Content-Type: application/json" -d "{\"period\":\"2026-08-01~2026-08-12\",\"questions\":[{\"id\":1,\"text\":\"怎么退款？\"}]}"
curl -X POST http://localhost:8080/knowledge/ops/faq -H "Content-Type: application/json" -d "{\"question\":\"怎么申请退款？\",\"answer\":\"进入订单详情页点击申请退款。\",\"knowledgeBase\":\"faq\"}"
```

验证：聚类输出主题 + 缺口标记；FAQ 收录后触发向量更新（MQ）。

## 前端验证

- `/chat-dashboard`：问数后渲染 ECharts 图表
- `/knowledge-ops`：聚类主题列表 + 缺口标记 + FAQ 收录按钮

## 环境变量/配置（新增）

| 配置 | 默认 | 说明 |
|------|------|------|
| `aics.rag.hybrid.enabled` | false | 对话侧 Hybrid 全局开关 |
| `aics.rag.rewrite.enabled` | false | 查询改写/HyDE 开关 |
| `aics.rag.graph.enabled` | false | 图谱开关（默认关闭） |
| `aics.rag.eval.hit-rate-threshold` | 0.6 | 评估命中率阈值 |
| `aics.rag.eval.llm-score-threshold` | 3.5 | 评估 LLM 分数阈值 |
| `aics.rag.cluster.similarity-threshold` | 0.82 | 聚类相似度阈值 |
| `aics.rag.cluster.gap-hit-rate-threshold` | 0.4 | 缺口命中率阈值 |
| `aics.rag.cluster.min-questions` | 20 | 聚类最小提问数 |
