# REST API 契约：多模态图生文（VLM）

> 第 1 阶段输出（/speckit-plan）。所有响应统一包装 `Result<T>`（code/data/message）。

## 一、ai-cs-chat（网关前缀 /chat）

### 1.1 图片上传（US1 前置）

`POST /chat/upload-image`

请求：`multipart/form-data`，字段 `file`（图片文件）

响应 `data`：图片 URL（MinIO 返回的访问地址）

```json
{
  "code": 0,
  "message": "上传成功",
  "data": "http://minio.internal/aics/chat/images/2026/08/13/uuid.png"
}
```

校验：格式 jpg/png/webp/gif；大小 ≤ 5MB；超限返回业务异常（复用商品图错误码思路）。

### 1.2 图片对话（US1/US2）

`POST /chat/vision`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 会话 ID |
| imageUrl | String | 是 | 图片 URL（须通过 SSRF 白名单校验） |
| message | String | 否 | 附带文字（可空） |
| hybrid | boolean | 否 | 默认 false；true 时 RAG 走 Hybrid 混合检索 |
| rewrite | boolean | 否 | 默认 false；true 时检索前查询改写/HyDE |

响应 `data`：

```json
{
  "answer": "根据您截图中的错误码 E10086，这是下单失败...",
  "citations": [
    { "documentId": "1", "title": "下单失败排查手册", "content": "..." }
  ],
  "imageDescription": "截图显示：下单失败，错误码 E10086，页面停留在收银台",
  "degraded": false
}
```

降级行为：
- 视觉模型不可用且 `message` 非空 → `degraded=true`，走纯文本对话
- 视觉模型不可用且 `message` 为空 → 返回业务错误码（如 `VISION_SERVICE_UNAVAILABLE`），message 提示"当前无法识别图片，请文字描述"
- `imageUrl` 校验失败 → 返回 `IMAGE_URL_INVALID`

### 1.3 图片对话 SSE 流式（US1）

`POST /chat/vision/sse`（`produces = text/event-stream`）

参数同 1.2。响应为 SSE 逐 token 推送：

```
data: {"content":"根据"}
data: {"content":"您截图"}
...
data: [DONE]
```

首 token 时限 ≤ 5 秒（视觉理解 + 检索 + 首 token 生成），复用 `sseChatService` 弹性策略。

## 二、ai-cs-product（内部服务能力）

### 2.1 商品图片描述生成（US3）

商品图片上传/更新时，服务内部调用 `ImageDescriptionService.describe(imageUrl)` 生成描述，写入 `product.image_description` 字段，不单独暴露对外接口。

- 视觉模型不可用或识别失败 → 返回 null，商品主流程不中断
- 描述非空时随商品详情返回，供前端展示与后续检索

## 三、ai-cs-frontend

| 交互 | 位置 | 说明 |
|------|------|------|
| 聊天输入框图片上传按钮 | ChatView.vue 输入区 | 点击选图 → 调 `/chat/upload-image` 拿 URL → 消息气泡显示缩略图 |
| 消息气泡图片渲染 | ChatView.vue 消息区 | 用户消息内嵌图片；助手消息可选展示 `imageDescription` |
| 图片对话发送 | ChatView.vue | 带图消息走 `/chat/vision/sse` 流式渲染 |

## 四、契约约束

- 所有新增参数均有默认值，存量文本对话零破坏
- 图片对话复用现有会话体系（sessionId）与引用溯源（CitationItemDTO）
- 视觉不可用降级为明确错误码或纯文本，不抛 5xx
- 图片 URL 必须过 SSRF 白名单，禁止直连任意外网地址
