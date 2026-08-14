# 快速启动与验证指南：多模态图生文（VLM）

> 第 1 阶段输出（/speckit-plan）。面向开发验证的最小路径。

## 前置条件

- JDK 17+（推荐 JDK 21）、Maven、MinIO/MySQL/Redis/Nacos（docker-compose 已覆盖）
- Nacos 已发布 `aics.vision.*` 配置（base-url、api-key、model），及共享 `aics-shared.yml`
- 硅基流动账号已开通多模态视觉模型权限

## 编译与单元测试

```bash
mvn -pl ai-cs-chat,ai-cs-product -am clean test
```

## 核心验证路径

### 1. 图片上传（US1 前置）

```bash
curl -X POST http://localhost:8080/chat/upload-image \
  -F "file=@/path/to/error-screenshot.png"
```

验证：返回 MinIO 图片 URL；超大/非法格式被拒绝。

### 2. 图片对话（US1/US2）

```bash
curl -X POST http://localhost:8080/chat/vision \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "sessionId=s1&imageUrl=<上一步URL>&message=这个怎么解决&hybrid=true"
```

验证：返回 answer + citations + imageDescription；`imageDescription` 体现图片关键信息（错误码/型号）。

### 3. 图片对话 SSE 流式（US1）

```bash
curl -N -X POST http://localhost:8080/chat/vision/sse \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "sessionId=s1&imageUrl=<URL>"
```

验证：SSE 逐 token 推送，首 token ≤ 5 秒。

### 4. 降级验证（US2）

- 临时把 `aics.vision.api-key` 置空 → 仅图片无文字时返回 `VISION_SERVICE_UNAVAILABLE` 提示；带文字时 `degraded=true` 走纯文本。

### 5. 商品图描述（US3）

```bash
# 上传商品图后，触发描述生成（随商品创建/更新流程）
curl -X POST http://localhost:8080/product \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"降噪耳机\",\"image\":\"<商品图URL>\"}"
```

验证：商品详情返回 `imageDescription` 非空；视觉不可用时为 null 且创建成功。

## 前端验证

- `/chat` 聊天页：输入框出现图片上传按钮；发送带图消息后气泡显示缩略图，助手流式回答

## 环境变量/配置（新增）

| 配置 | 默认 | 说明 |
|------|------|------|
| `aics.vision.base-url` | `https://api.siliconflow.cn` | 视觉模型端点 |
| `aics.vision.api-key` | （空） | 视觉模型 API Key（Nacos 管理，不硬编码） |
| `aics.vision.model` | `Qwen/Qwen2.5-VL-72B-Instruct` | 视觉模型名 |
| `aics.vision.enabled` | true | 视觉能力总开关（false 时全部降级） |
| `aics.vision.timeout` | 5s | 视觉调用超时 |
| `aics.vision.allowed-image-host` | （MinIO 内网地址） | 图片 URL 白名单（SSRF 防护） |
