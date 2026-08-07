# 16 - ai-cs-chat 接口测试报告

> 测试日期：2026-08-07
> 测试方式：本地启动服务 + 自动化 HTTP 用例（17 个）
> 结论：**17/17 用例符合预期（含 6 个参数校验负向用例）**

---

## 一、测试目标与环境

验证 `ai-cs-chat` 服务的全部 HTTP 接口（基础对话、工具调用、RAG 对话、流式对话、知识库入库、语义检索、参数校验），并修复测试中发现的缺陷。

| 项目 | 说明 |
|---|---|
| 服务 | ai-cs-chat（端口 8083） |
| JDK / 构建 | JDK 21 / Maven 3.9.12 / Spring Boot 3.2.5 / Spring AI 1.1.4 |
| 注册/配置中心 | Nacos 127.0.0.1:8848（namespace: aics，9 个配置已加载） |
| 向量库 | Chroma 123.60.31.79:8000（v2 API，collection: aics-knowledge） |
| 对话模型 | DeepSeek deepseek-chat（base-url: https://api.deepseek.com） |
| Embedding 模型 | 硅基流动 BAAI/bge-m3（base-url: https://api.siliconflow.cn） |
| 服务注册 | ai-cs-chat 192.168.10.102:8083 已注册 Nacos |

---

## 二、需启动的工程清单（测试 AI 对话的前提）

| 工程/组件 | 端口 | 是否必需 | 说明 |
|---|---|---|---|
| **Nacos** | 8848 | ✅ 必需 | 注册中心 + 配置中心，加载 `ai-cs-chat.yml` / `aics-shared.yml` |
| **ai-cs-chat** | 8083 | ✅ 必需 | 被测服务本体 |
| **Chroma 向量库** | 8000 | ✅ 必需 | RAG 向量存储（远程 v2 API） |
| DeepSeek API | - | ✅ 必需 | 对话/工具调用（需外网） |
| 硅基流动 API | - | ✅ 必需 | Embedding（需外网） |
| ai-cs-order/product/user 等 | - | ❌ 非必需 | ai-cs-chat 使用 mock 订单数据，不 Feign 调用其他服务 |

> 即：**只需启动 `ai-cs-chat` 一个应用服务**，配合 Nacos + Chroma + 外部 AI API 即可完成全部接口测试。

---

## 三、测试接口清单（6 个端点）

| 序号 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | POST | /chat/send | 基础对话（含工具调用） |
| 2 | POST | /chat/rag | RAG 增强对话 |
| 3 | POST | /chat/stream | 流式对话（当前为桩实现） |
| 4 | POST | /rag/knowledge-base/text | 文本入库 |
| 5 | POST | /rag/knowledge-base/upload | 文件入库（PDF/TXT） |
| 6 | GET | /rag/knowledge-base/search | 语义检索测试 |

---

## 四、测试结果明细（17/17 通过）

### 4.1 知识库入库

| 用例 | 结果 | 耗时 | 响应摘要 |
|---|---|---|---|
| 文本入库 `/rag/knowledge-base/text` | ✅ 200 | 0.48s | `{"knowledgeBase":"test-kb","chunks":1}` |
| 文件入库 `/rag/knowledge-base/upload` | ✅ 200 | 0.40s | `{"knowledgeBase":"test-kb-upload","fileName":"test-kb.txt","chunks":1}` |

### 4.2 基础对话 / 工具调用

| 用例 | 结果 | 耗时 | 响应摘要 |
|---|---|---|---|
| 基础问候 | ✅ 200 | 1.97s | AI 正常自我介绍并列出订单查询能力 |
| 按订单号查订单（工具调用） | ✅ 200 | 2.55s | 正确返回 ORD20260720001 状态/商品/物流（顺丰 SF1234567890） |
| 按用户 ID 查订单（工具调用） | ✅ 200 | 3.43s | 正确返回 user_10086 的 2 个订单详情 |
| 查询不存在的订单 | ✅ 200 | 1.72s | AI 正确回复"系统中没有找到该订单"并引导 |

### 4.3 RAG 对话

| 用例 | 结果 | 耗时 | 响应摘要 |
|---|---|---|---|
| 基于已入库知识库提问 | ✅ 200 | 2.42s | 正确引用知识库回答"7 天无理由退货、1-3 个工作日退款" |
| 未入库知识库提问 | ✅ 200 | 1.38s | 检索未命中时仍基于模型知识合理回复，无报错 |

### 4.4 流式对话

| 用例 | 结果 | 耗时 | 响应摘要 |
|---|---|---|---|
| 流式对话（桩实现） | ✅ 200 | 0.02s | 返回 `{"status":"streaming","message":"流式对话已启动，请通过SSE端点接收响应"}`（当前为占位，非真实 SSE） |

### 4.5 语义检索

| 用例 | 结果 | 耗时 | 响应摘要 |
|---|---|---|---|
| 语义检索 test-kb | ✅ 200 | 0.33s | 命中 4 条，最高相似度 0.7433，内容与"退货"相关 |

### 4.6 参数校验（负向用例，预期 400）

| 用例 | 结果 | 响应摘要 |
|---|---|---|
| 空 message | ✅ 400 | `消息内容不能为空` |
| 空 sessionId | ✅ 400 | `会话ID不能为空` |
| 空 knowledgeBase | ✅ 400 | `知识库标识不能为空` |
| 缺 message 参数 | ✅ 400 | `缺少请求参数: message` |
| 缺 knowledgeBase 参数 | ✅ 400 | `缺少请求参数: knowledgeBase` |
| 缺 query 参数 | ✅ 400 | `缺少请求参数: query` |

---

## 五、测试中发现并修复的问题

| # | 问题 | 根因 | 修复 |
|---|---|---|---|
| 1 | `/chat/send` 等对话接口 500 | Spring AI OpenAiApi 自动拼接 `/v1/embeddings`，而硅基流动 base-url 误配为 `.../v1`，实际请求 `.../v1/v1/embeddings` → 404 | `SpringAiConfig` 的 EMBEDDING_BASE_URL 改为 `https://api.siliconflow.cn`（去掉 `/v1`），并同步修正 `deploy/nacos/configs/` 两个 yml |
| 2 | 知识库入库/检索 500 | `KnowledgeBaseController` 的 `@RequestParam` 未显式命名参数，编译未开 `-parameters` 时反射拿不到参数名 | 显式命名 `@RequestParam("knowledgeBase"/"text"/"query")` |
| 3 | 参数校验返回 500（应为 400） | `GlobalExceptionHandler`（ai-cs-common）未被 `ChatApplication` 组件扫描，且缺少 `ConstraintViolationException` 处理器 | `ChatApplication` 增加 `scanBasePackages={"com.aics.chat","com.aics.common"}`；`GlobalExceptionHandler` 新增 `ConstraintViolationException` 处理器返回 400 |

---

## 六、遗留说明与建议

1. **流式对话为桩实现**：`/chat/stream` 仅返回占位响应，未实现真实 SSE/WebSocket 流式输出，需后续迭代。
2. **会话历史为内存存储**：`ChatServiceImpl` 使用 `ConcurrentHashMap`，重启即失、多实例不同步，生产建议落 Redis/MySQL（ai-cs-message 已有表结构）。
3. **RAG 对话不携带历史**：`chatWithRag` 为单轮检索注入，追问场景上下文不连续。
4. **安全**：`SpringAiConfig` 中硬编码了硅基流动 API Key（测试便利，上线前应改为 Nacos 配置注入）；订单数据为 mock，未接真实 `ai-cs-order`。
5. 测试用临时文件（测试脚本、启动脚本、测试知识库文件）已清理；本报告及原始结果 `logs/chat-test/test-results.json` 保留备查。

---

## 附：接口测试证据

原始自动化测试结果：`logs/chat-test/test-results.json`（17 条用例，含请求参数/HTTP 状态/耗时/响应全文）。