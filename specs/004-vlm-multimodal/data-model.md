# 数据模型设计：多模态图生文（VLM）

> 第 1 阶段输出（/speckit-plan）。实体分三类：对话请求/响应、商品图片描述。

## 一、对话实体（ai-cs-chat/dto）

### VisionChatRequest（图片对话请求）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 会话 ID（复用现有会话体系） |
| imageUrl | String | 是 | 图片 URL（MinIO 上传返回，需通过 SSRF 白名单校验） |
| message | String | 否 | 附带文字描述（可空，仅图片时为空） |
| hybrid | boolean | 否 | 默认 false；true 时检索走 Hybrid 混合检索 |
| rewrite | boolean | 否 | 默认 false；true 时检索前先查询改写/HyDE |

### VisionChatResponse（图片对话响应）

| 字段 | 类型 | 说明 |
|------|------|------|
| answer | String | 最终回答 |
| citations | List<CitationItemDTO> | 引用溯源（复用现有 `CitationItemDTO`） |
| imageDescription | String | 视觉模型生成的图片描述（可空，供前端展示"我看到了什么"） |
| degraded | boolean | 是否发生降级（视觉不可用→纯文本） |

### ImageDescriptionResult（视觉理解结果，内部）

| 字段 | 类型 | 说明 |
|------|------|------|
| description | String | 图片文本描述 |
| success | boolean | 是否识别成功 |
| errorMessage | String | 失败原因（可空） |

## 二、商品图片描述实体（ai-cs-product）

### Product.imageDescription（商品字段扩展）

| 字段 | 类型 | 说明 |
|------|------|------|
| imageDescription | String | 视觉模型生成的商品图描述（可空），供 ES 检索与后续向量化 |

> 复用现有 `Product` 实体，仅新增 `imageDescription` 字段；`ImageDescriptionService.describe(imageUrl)` 返回该描述文本。

## 三、存储设计

### MySQL 表变更（ai-cs-product 库，product 表）

```sql
-- 商品图片自动描述字段（幂等：列存在则跳过）
-- 输出至 deploy/mysql/vlm-multimodal-init.sql
ALTER TABLE product
    ADD COLUMN image_description VARCHAR(1024) NULL COMMENT '视觉模型生成的图片描述，用于增强检索'
    ;
```

> 采用 `ADD COLUMN` 幂等策略（配合 `IF NOT EXISTS` 语义或数据库版本判断），不破坏现有 `product-init.sql`。

### 图片对话/视觉理解不落库

- 图片本身存 MinIO（复用 `FileStorageService`，目录 `chat/images`）
- 视觉描述、对话回答均为临时计算，随响应返回，不新增对话表

## 四、关系说明

- VisionChatRequest → ImageDescriptionResult → RAG 检索 → VisionChatResponse：串行编排（理解 → 检索 → 回答）
- Product.imageDescription 与 Product 为 1 对 1（单字段，无独立表）
- 无新增实体间多对多关系，无状态机

## 五、字段校验规则

| 字段 | 规则 |
|------|------|
| imageUrl | 非空；必须是 http/https；必须通过 SSRF 白名单（MinIO 内网地址或配置白名单域名） |
| image 格式 | jpg/png/webp/gif（与商品图一致，上传阶段校验） |
| image 大小 | ≤ 5MB（上传阶段校验） |
| message | 长度 ≤ 2000 字符 |
| imageDescription | ≤ 1024 字符（超长截断） |
