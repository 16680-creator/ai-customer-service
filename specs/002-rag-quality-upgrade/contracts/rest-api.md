# REST API 契约：RAG 检索质量升级

> 仅列出本功能变更/新增的接口。未列出的接口保持不变。
> 所有接口前缀经网关：`{GATEWAY}/api/`

---

## 1. 知识库管理：`/rag/knowledge-base/upload`

### 变更说明
支持 docx/xlsx/html/md 文件格式上传（原有仅 PDF/TXT）。

### 请求

`POST /rag/knowledge-base/upload`

- `knowledgeBase` (String, query) — 知识库标识
- `file` (MultipartFile, body) — 上传文件（支持.pdf/.txt/.md/.docx/.xlsx/.html/.htm）

### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": { "knowledgeBase": "...", "fileName": "...", "chunks": 12 }
}
```

### 错误
- `400` 不支持的格式（如 .png）

---

## 2. RAG 对话：`/chat/rag`

### 变更说明
返回类型从纯文本改为含引用元数据的对象。

### 请求

`POST /chat/rag`
- `sessionId` (String, query, required) — 会话 ID
- `message` (String, query, required) — 用户消息
- `knowledgeBase` (String, query, required) — 知识库标识
- `X-User-Id` (header, optional) — 用户 ID

### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": "根据您提供的资料，退款流程如下...",
    "citations": [
      {
        "documentId": 1,
        "title": "售后政策手册",
        "page": 3,
        "score": 0.95,
        "content": "退款申请需在收货后7天内提出..."
      }
    ]
  }
}
```

### 流式 SSE：`/chat/stream/sse`

**变更说明**：`done` 事件增加 `citations` 字段。

### 事件流

```
data: {"content":"根据"}
data: {"content":"您"}
...
data: {"content":"资料"}
data: {"done":true, "content":"根据...资料。", "citations":[...]}
```

---

## 3. 混合检索：`/search/hybrid`

### 新增接口

`GET /search/hybrid`
- `index` (String, query, required) — 知识库标识（即 knowledgeBase）
- `query` (String, query, required) — 查询内容
- `page` (int, query, default=1) — 页码
- `size` (int, query, default=10) — 每页条数

### 响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "page": 1,
    "size": 10,
    "results": [
      {
        "documentId": "1",
        "title": "售后政策手册",
        "content": "...",
        "score": 0.85,
        "knowledgeBase": "knowledge",
        "page": 3,
        "docType": "pdf"
      }
    ]
  }
}
```

### 错误
- `503` 查询失败（双路都不可用时）