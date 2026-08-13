# 多模态图生文实战（VLM 视觉理解）

> 让 AI 客服"看懂"用户上传的图片，再结合知识库回答。
> 对应项目文件：`ai-cs-chat` 模块的 `VisionModelClient`、`VisionChatServiceImpl`、`ImageUrlValidator`、`PiiMasker`；`ai-cs-product` 模块的 `SiliconFlowImageDescriptionService`
> 功能分支：`004-vlm-multimodal`

---

## 一、什么是多模态图生文（VLM）？

```
传统 LLM（如 DeepSeek-chat）：只吃文字 —— "这个报错怎么解决"（纯文本）
VLM（视觉语言模型）      ：吃图片 + 文字 —— "（截图）+ 这个报错怎么解决"

VLM = Vision Language Model（视觉语言模型）
能同时理解图片内容和文字指令。
```

**客服场景为什么需要图生文？**

大量用户问题天然以图片为载体：

| 场景 | 用户发的 | 纯文本的局限 |
|------|---------|-------------|
| 报错排查 | 报错截图（错误码 E10086） | 用户不会描述错误码 |
| 订单核对 | 订单页面截图 | 金额/状态抄写易错 |
| 商品咨询 | 商品实拍照片 | "这个"指代不清 |
| 售后凭证 | 故障照片 | 无法文字描述外观 |

**核心价值**：让 AI 客服从"只能读文字"升级为"能看图"，覆盖更多真实问题。

---

## 二、为什么 DeepSeek 不能直接看图？

DeepSeek 的 `deepseek-chat` 模型**只支持文本输入**，不支持图片。所以"看图"必须换一个支持视觉的模型。

```
DeepSeek（本项目回答模型）：只吃文本，不吃图片
硅基流动 Qwen2.5-VL       ：吃图片 + 文字，但回答能力弱于 DeepSeek
```

**解决方案**：两者分工 —— Qwen2.5-VL 负责"看图"，DeepSeek 负责"回答"。这就是下面的两段式架构。

关键点：硅基流动聚合的 Qwen2.5-VL 走 **OpenAI 兼容协议**，和项目里对接 DeepSeek 的方式完全一致，所以 Spring AI 一行配置就能接入，零新增 SDK。

---

## 三、两段式架构：看图 → 回答

```
用户上传图片 + 可选文字
        │
        ▼
┌─────────────────────────────────────────────┐
│ 第一段【看图】Qwen2.5-VL（硅基流动）           │
│   图片 URL → 视觉模型 → 文本描述              │
│   "截图显示：下单失败，错误码 E10086"          │
└─────────────────────────────────────────────┘
        │ 文本描述 + 用户文字
        ▼
┌─────────────────────────────────────────────┐
│ 第二段【回答】复用现有 RAG + DeepSeek          │
│   描述文本 → RAG 检索知识库 → 生成回答         │
│   "根据错误码 E10086，请重试下单..."           │
└─────────────────────────────────────────────┘
```

**为什么必须两段式？**

1. DeepSeek 无视觉，看图只能用 VLM；
2. 回答要复用 003 已建好的 RAG 链路（Hybrid/改写/GraphRAG），不能重造；
3. 两段式让"看图"和"回答"解耦，视觉模型可替换，RAG 能力可复用。

---

## 四、核心实现

### 4.1 视觉模型装配（VisionModelClient）

`VisionModelClient` 是视觉模型的封装。**为什么不注册成 `OpenAiChatModel` Bean？**

因为容器里已有 DeepSeek 的 `OpenAiChatModel`（自动装配），再注册一个同类型的视觉模型 Bean 会**类型歧义**。所以在 `@PostConstruct` 阶段手动构造，与文本模型解耦：

```java
@PostConstruct
void init() {
    // 未启用或未配 API Key → 视觉模型不初始化，图片对话降级
    if (!visionProperties.isEnabled() || !StringUtils.hasText(visionProperties.getApiKey())) {
        return;
    }
    // OpenAiApi：OpenAI 兼容协议，baseUrl 指向硅基流动
    OpenAiApi api = OpenAiApi.builder()
            .baseUrl(visionProperties.getBaseUrl())   // https://api.siliconflow.cn
            .apiKey(visionProperties.getApiKey())
            .build();
    // OpenAiChatModel：视觉模型，model 用 Qwen2.5-VL
    this.visionModel = OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder().model(visionProperties.getModel()).build())
            .build();
}
```

**关键理解**：和 DeepSeek 的对接方式一模一样（OpenAI 兼容协议），只是换了 `baseUrl` + `apiKey` + `model`。这就是 OpenAI 兼容协议的价值 —— **换模型只改配置，不改代码**。

### 4.2 多模态消息构造（图片怎么塞给模型）

Spring AI 用 `Media` 类型表示图片，用 `UserMessage.builder()` 构造多模态消息：

```java
// 多模态消息：文本指令 + 图片
UserMessage userMessage = UserMessage.builder()
        .text("请描述这张图片中的关键信息...")                    // 指令
        .media(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl)))  // 图片 URL
        .build();

// 调用视觉模型，逐层取回文本描述
String description = visionModel.call(new Prompt(List.of(userMessage)))
        .getResult()   // 完整结果
        .getOutput()   // 消息输出
        .getText();    // 纯文本
```

**关键理解**：

- `Media(MimeType, URI)`：把图片 URL 包装成媒体对象。`OpenAiChatModel` 会自动把它转成 OpenAI 协议的 `image_url` 字段。
- 图片用 **URL 而非 base64**：图片存在 MinIO，传 URL 即可，视觉模型会自己拉取。
- `call(...).getResult().getOutput().getText()` 是 Spring AI 的三层取文本链，逐层剥洋葱。

### 4.3 两段式编排（VisionChatServiceImpl）

`VisionChatServiceImpl` 负责把"看图"和"回答"串起来：

```java
public Result<VisionChatResponse> chatWithVision(VisionChatRequest request) {
    // 1. 校验图片 URL（SSRF 白名单）
    if (!imageUrlValidator.isValid(request.getImageUrl())) {
        throw new BusinessException(ResultCode.CHAT_IMAGE_URL_INVALID, "图片地址无效或不允许访问");
    }

    // 2. 第一段【看图】：图片 → 文本描述（失败返回 null）
    String description = describeImage(request.getImageUrl());
    if (description == null) {
        return degradeToText(request);   // 视觉不可用 → 降级
    }

    // 3. 第二段【回答】：组合查询 → 复用 RAG 对话 → DeepSeek 回答
    String query = buildQuery(request.getMessage(), description);
    Result<ChatRagResponseDTO> rag = chatService.chatWithRag(
            request.getSessionId(), query, request.getKnowledgeBase(),
            request.isHybrid(), request.isRewrite());
    // ... 包装成 VisionChatResponse 返回
}
```

**关键理解**：第二段直接复用 `chatService.chatWithRag(...)`，把视觉描述当普通查询喂给现有 RAG 链路。这就是"复用"的体现 —— 图片理解只是给 RAG 提供更好的查询文本。

### 4.4 降级策略（视觉是增强项，不可用不影响核心对话）

视觉模型可能超时、限流、未配置。降级策略保证图片对话**永不抛 5xx**：

```
视觉模型不可用
  ├── 有文字 → 降级为纯文本对话（degraded=true）
  └── 仅图片 → 返回明确提示"当前无法识别图片，请文字描述"
```

```java
private Result<VisionChatResponse> degradeToText(VisionChatRequest request) {
    if (StringUtils.hasText(request.getMessage())) {
        // 有文字 → 走纯文本对话，degraded=true 标记降级
        Result<String> text = chatService.chat(request.getSessionId(), request.getMessage());
        // ...
    }
    // 仅图片 → 明确错误码提示，不抛 5xx
    throw new BusinessException(ResultCode.CHAT_VISION_SERVICE_UNAVAILABLE, "当前无法识别图片，请文字描述");
}
```

### 4.5 安全：SSRF 防护 + PII 脱敏

图片 URL 是外部输入，有两类安全风险需要处理：

**① SSRF 防护（ImageUrlValidator）**

图片 URL 可被恶意利用发起 SSRF 攻击（探测内网、读云元数据）。防护手段：**白名单校验**。

```java
// 只允许 http/https 协议
if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
    return false;   // 拒绝 file/ftp 等协议
}
// 主机名必须命中白名单（精确匹配或子域匹配）
for (String allowed : allowedHosts) {
    if (host.equalsIgnoreCase(allowed) || host.endsWith("." + allowed)) {
        return true;
    }
}
return false;
```

**② PII 脱敏（PiiMasker）**

视觉描述可能含截图中的手机号、身份证号。注入 Prompt 前先脱敏：

```java
// 身份证：110101199001011234 → 110101********1234
ID_CARD.matcher(text).replaceAll("$1********$2");
// 手机号：13812345678 → 138****5678
PHONE.matcher(masked).replaceAll("$1****$2");
```

**关键理解**：先脱敏身份证（更长更具体）再脱敏手机号，避免手机号正则误匹配身份证里的数字片段。

---

## 五、商品图描述（顺带增强商品检索）

`ai-cs-product` 的 `SiliconFlowImageDescriptionService` 让商品图片也能被"看懂"：

```
商品图片 → 视觉模型 → 文本描述 → 落库 image_description 字段 → 向量检索复用
```

原来 `ImageDescriptionService` 是 Noop 占位（返回 null），现在接入视觉模型后，商品"以图搜文/相似商品"能识别图片外观特征。

**降级**：视觉不可用返回 null，商品创建/更新流程不中断。

---

## 六、学习要点总结

| 技术点 | 一句话理解 |
|--------|-----------|
| **VLM 多模态** | 模型同时吃图片 + 文字，用 `Media` 类型传图 |
| **OpenAI 兼容协议** | 换模型只改配置（baseUrl/key/model），不改代码 |
| **两段式架构** | 看图（VLM）与回答（LLM）解耦，各用所长 |
| **类型歧义规避** | 两个同类型模型 Bean 会冲突，用组件内部构造规避 |
| **弹性治理复用** | 视觉调用走 Resilience4j（超时/重试/熔断），失败降级 |
| **SSRF 白名单** | 图片 URL 只允许命中白名单主机，防内网探测 |
| **PII 脱敏** | 视觉描述注入 Prompt 前，先脱敏手机号/身份证 |

---

## 七、延伸思考

- **多图**：当前支持单图，`Media` 列表天然支持多图，扩展只需传多个 `Media`。
- **视频帧**：VLM 也能理解视频帧，截帧后走同样链路即可。
- **本地模型**：Ollama 的 LLaVA 等也可替换硅基流动，同样走 OpenAI 兼容协议。
