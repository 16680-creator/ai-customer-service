# 快速启动与验证指南：RAG 检索质量升级

> 用于开发者验证五项功能是否正常工作。

---

## 前置条件

1. **基础设施就绪**：MySQL + Redis + Nacos + RocketMQ + Elasticsearch + Chroma 均已启动（`docker-compose up -d`）
2. **服务启动顺序**：Nacos → MySQL → Redis → RocketMQ → ES → Chroma → ai-cs-chat + ai-cs-search + ai-cs-knowledge
3. **Nacos 配置已发布**：部署最新的 `ai-cs-chat.yml`（含 aics.rerank.*）和 `ai-cs-search.yml`（含 spring.elasticsearch.uris）

---

## 验证 1：Rerank 重排序

```bash
# 1. 上传测试知识库
curl -X POST "http://localhost:8080/api/rag/knowledge-base/text?knowledgeBase=test-rerank&text=退款规则：收货后7天内可申请退款，需保持商品完好。"

# 2. 提问（自动触发两阶段检索）
curl -s "http://localhost:8080/api/chat/rag?sessionId=test1&message=怎么退款&knowledgeBase=test-rerank" | jq .
# 期望：回答基于知识库，data.citations 非空，citations[0].score >= 0.7
```

---

## 验证 2：混合检索

```bash
# 1. 通过知识库上传接口入库（自动同步到 ES + Chroma）
curl -X POST "http://localhost:8080/api/rag/knowledge-base/text?knowledgeBase=test-hybrid&text=型号ABC-123打印机保修期2年"

# 2. 混合检索
curl -s "http://localhost:8080/api/search/hybrid?index=test-hybrid&query=ABC-123" | jq .
# 期望：结果中包含精确匹配 ABC-123 的资料
```

---

## 验证 3：引用溯源

```bash
# 发起 RAG 对话
curl -s "http://localhost:8080/api/chat/rag?sessionId=test2&message=保修多久&knowledgeBase=test-hybrid" | jq '.data.citations'
# 期望：citations 数组包含 documentId/title/page/score/content
```

---

## 验证 4：文档格式扩展

```bash
# 上传各格式文件（需准备测试文件）
# Word
curl -X POST "http://localhost:8080/api/rag/knowledge-base/upload?knowledgeBase=test-tika" -F "file=@test.docx"
# Excel
curl -X POST "http://localhost:8080/api/rag/knowledge-base/upload?knowledgeBase=test-tika" -F "file=@test.xlsx"
# HTML
curl -X POST "http://localhost:8080/api/rag/knowledge-base/upload?knowledgeBase=test-tika" -F "file=@test.html"
# Markdown
curl -X POST "http://localhost:8080/api/rag/knowledge-base/upload?knowledgeBase=test-tika" -F "file=@test.md"
# 期望：均返回 chunks > 0

# 验证检索
curl -s "http://localhost:8080/api/rag/knowledge-base/search?knowledgeBase=test-tika&query=关键字" | jq '.data | length'
# 期望：命中 > 0
```

---

## 验证 5：知识库增量同步

```bash
# 1. 通过知识库服务创建文档（自动发 MQ→异步向量化）
curl -X POST "http://localhost:8080/api/knowledge" \
  -H "Content-Type: application/json" \
  -d '{"title":"测试同步","content":"这是异步同步测试内容","tags":"test"}'

# 2. 等待 2 秒后验证向量库已更新
curl -s "http://localhost:8080/api/rag/knowledge-base/search?knowledgeBase=knowledge&query=异步同步" | jq '.data | length'
# 期望：命中 > 0（显示异步同步成功）
```

---

## 验证降级

```bash
# 1. 停止 Rerank 服务（关闭 SiliconFlow API 可达性）
# 2. 发起 RAG 对话
curl -s "http://localhost:8080/api/chat/rag?sessionId=test-degrade&message=退款&knowledgeBase=test-rerank" | jq .
# 期望：回答仍正常返回（降级为向量原始排序），日志包含 "Rerank不可用，降级为原始排序"
```