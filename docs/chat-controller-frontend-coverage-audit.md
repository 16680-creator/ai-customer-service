# ai-cs-chat Controller 前端覆盖率审计报告

> 审计日期: 2026-08-21  
> 审计范围: `ai-cs-chat/src/main/java/com/aics/chat/controller/` 下所有 Controller  
> 前端范围: `ai-cs-frontend/src/` 下所有 Vue 组件与 API 定义

---

## 〇、实现记录（2026-08-21 已修复/补全）

> 说明：审计报告中的部分 Controller 路径（如 `/api/observability`、`/api/prompts`、`/api/chat/feedback`）
> 与实际后端代码不一致。实际控制器映射为 `/observability`、`/prompts`、`/chat/feedback`，
> 因此网关路由与前端 API 均按**真实端点**对齐，确保不出现 404。

| 项 | 改动 | 状态 |
|----|------|------|
| P0-1 反馈路由 | `ChatFeedbackController` 类前缀 `/api/chat` → `/chat`，匹配网关 `PATH_CHAT` 转发 | ✅ |
| P0-2 可观测性网关 | `RouteConfig` 新增 `ai-cs-observability`：`/api/observability/**` → `lb://ai-cs-chat` | ✅ |
| P0-3 Prompt 网关 | `RouteConfig` 新增 `ai-cs-prompt`：`/api/prompts/**` → `lb://ai-cs-chat` | ✅ |
| Agent 网关 | `RouteConfig` 新增 `ai-cs-agent`：`/api/agent/**` → `lb://ai-cs-chat` | ✅ |
| 前端 API | `api/index.js` 新增 `feedbackApi`/`observabilityApi`/`promptApi`/`agentApi` 及封装 | ✅ |
| P1-4 反馈按钮 | `ChatView.vue` AI 回答下方新增点赞/点踩/评分/补充工具条 | ✅ |
| P1-5 链路追踪 | 新增 `TraceView.vue`（甘特图 + span 明细，调用 `/observability/traces/{requestId}`） | ✅ |
| P1-6 Prompt 管理 | 新增 `PromptView.vue`（场景列表 + 版本热切换） | ✅ |
| P1-7 检索测试 | `VectorKbView.vue` 检索卡片新增 VECTOR/HYBRID/RERANK 模式 + TopK，调用 `/retrieve/test` | ✅ |
| P2-8 Agent 对话 | 新增 `AgentView.vue`（意图/确认写操作/转人工，调用 `/chat/agent`、`/chat/agent/confirm`） | ✅ |
| P2-9 知识图谱 | 新增 `GraphView.vue`（三元组增删查 + 多跳检索，调用 `/graph/*`） | ✅ |
| P2-10 RAG 评估 | 新增 `RagEvalView.vue`（golden 集 + LLM-as-Judge 门禁，调用 `/eval/run`） | ✅ |
| 路由 + 入口 | `router/index.js` 注册 6 个路由；`Dashboard.vue` 新增入口按钮 | ✅ |

`npx vite build` 已通过（注：构建前缺失 `echarts` 依赖，与本次改动无关，已 `npm install echarts` 补齐）。

---

## 一、审计结论

| 指标 | 数值 |
|------|------|
| Controller 总数 | 10 个 |
| 后端 API 端点总数 | 25 个 |
| 前端已调用的端点 | 10 个 |
| 前端已定义但无页面调用的端点 | 2 个 |
| 完全无前端支持的端点 | 13 个 |
| **前端覆盖率** | **40%** |
| 网关路由缺陷 | 3 个 |

---

## 二、网关路由缺陷（P0 必须修复）

### 缺陷 #1：ChatFeedbackController 路由不通

| 项目 | 内容 |
|------|------|
| **严重程度** | 🔴 P0 — 功能完全不可用 |
| **Controller** | `ChatFeedbackController` |
| **Controller 路径** | `@RequestMapping("/api/chat")` |
| **网关路由** | `/api/chat/**` → `stripPrefix(1)` → 转发到 `/chat/**` |
| **实际效果** | 前端请求 `/api/chat/feedback` → 网关去掉 `/api` → 转发 `/chat/feedback` → 但 Controller 期望 `/api/chat/feedback` → **404** |
| **影响端点** | `POST /api/chat/feedback`（提交反馈）、`GET /api/chat/feedback`（查询反馈） |
| **修复建议** | 将 `ChatFeedbackController` 的 `@RequestMapping` 从 `/api/chat` 改为 `/chat`，与网关 `stripPrefix(1)` 行为一致 |

### 缺陷 #2：ObservabilityController 无网关路由

| 项目 | 内容 |
|------|------|
| **严重程度** | 🟡 P1 — 功能不可从外部访问 |
| **Controller** | `ObservabilityController` |
| **Controller 路径** | `@RequestMapping("/api/observability")` |
| **网关路由** | 未配置 `/api/observability/**` |
| **影响端点** | `GET /api/observability/traces/{requestId}` |
| **修复建议** | 在 `RouteConfig` 中添加路由：`/api/observability/**` → `stripPrefix(1)` → `lb://ai-cs-chat` |

### 缺陷 #3：PromptController 无网关路由

| 项目 | 内容 |
|------|------|
| **严重程度** | 🟡 P1 — 功能不可从外部访问 |
| **Controller** | `PromptController` |
| **Controller 路径** | `@RequestMapping("/api/prompts")` |
| **网关路由** | 未配置 `/api/prompts/**` |
| **影响端点** | `GET /api/prompts`、`GET /api/prompts/{scenario}`、`POST /api/prompts/{scenario}/active` |
| **修复建议** | 在 `RouteConfig` 中添加路由：`/api/prompts/**` → `stripPrefix(1)` → `lb://ai-cs-chat` |

---

## 三、后端端点与前端支持对照表

### 3.1 ChatController (`/chat`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `POST /chat/send` | 普通同步对话 | ❌ 无调用 | ⚠️ 前端用 SSE 替代（可接受） |
| 2 | `POST /chat/rag` | RAG 同步对话 | ❌ 无调用 | ⚠️ 前端用 SSE 替代（可接受） |
| 3 | `POST /chat/stream` | 流式对话占位接口 | ❌ 无调用 | ⚠️ 仅返回提示信息 |
| 4 | `POST /chat/stream/sse` | SSE 流式对话 | ✅ SSE fetch | ChatView.vue:340 |
| 5 | `GET /chat/history` | 查询会话历史 | ✅ `chatApi.get('/history')` | ChatView.vue:173 |
| 6 | `POST /chat/upload-image` | 上传图片 | ✅ `visionApi.uploadImage()` | ChatView.vue:292 |
| 7 | `POST /chat/vision` | 图片同步对话 | ❌ 无调用 | ⚠️ 前端用 SSE 替代（可接受） |
| 8 | `POST /chat/vision/sse` | 图片 SSE 对话 | ✅ SSE fetch | ChatView.vue:340 |

### 3.2 ChatFeedbackController (`/api/chat`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `POST /api/chat/feedback` | 提交用户反馈 | ❌ 无调用 | 🔴 无页面入口 + 路由缺陷 |
| 2 | `GET /api/chat/feedback` | 查询用户反馈 | ❌ 无调用 | 🔴 无页面入口 + 路由缺陷 |

### 3.3 KnowledgeBaseController (`/rag/knowledge-base`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `POST /rag/knowledge-base/text` | 文本入库 | ✅ `ragApi.post(...)` | VectorKbView.vue:105 |
| 2 | `POST /rag/knowledge-base/upload` | 文件入库 | ✅ `ragApi.post(...)` | VectorKbView.vue:133 |
| 3 | `GET /rag/knowledge-base/search` | 语义检索测试 | ✅ `ragApi.get(...)` | VectorKbView.vue:155 |

### 3.4 ObservabilityController (`/api/observability`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `GET /api/observability/traces/{requestId}` | LLM 调用链详情 | ❌ 无调用 | 🔴 无页面入口 + 无网关路由 |

### 3.5 PromptController (`/api/prompts`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `GET /api/prompts` | 列出所有 Prompt 场景版本 | ❌ 无调用 | 🔴 无页面入口 + 无网关路由 |
| 2 | `GET /api/prompts/{scenario}` | 场景 Prompt 版本详情 | ❌ 无调用 | 🔴 无页面入口 + 无网关路由 |
| 3 | `POST /api/prompts/{scenario}/active` | 热切换生效版本 | ❌ 无调用 | 🔴 无页面入口 + 无网关路由 |

### 3.6 RagEvalController (`/rag/eval`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `POST /rag/eval/run` | 运行 RAG 评估 | ⚠️ API 已定义无页面 | ragEvalApi.run() 已封装但无 Vue 页面调用 |

### 3.7 RetrieveController (`/chat`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `GET /chat/retrieve/test` | 检索测试（多模式） | ❌ 无调用 | 🔴 无页面入口 |

### 3.8 ChartController (`/chat`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `POST /chat/chart` | 问数图表生成 | ✅ `chatApi.post('/chart')` | ChatDashboardView.vue:72 |

### 3.9 AgentController (`/chat/agent`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `POST /chat/agent` | Agent 多轮对话 | ❌ 无调用 | 🔴 无页面入口 |
| 2 | `POST /chat/agent/confirm` | 写操作确认 | ❌ 无调用 | 🔴 无页面入口 |
| 3 | `GET /chat/agent/runs/{runId}` | 执行轨迹查询 | ❌ 无调用 | 🔴 无页面入口 |

### 3.10 GraphController (`/rag/graph`)

| # | 端点 | 说明 | 前端支持 | 状态 |
|---|------|------|---------|------|
| 1 | `POST /rag/graph/triple` | 新增三元组 | ⚠️ API 已定义无页面 | graphApi.addTriple() 已封装但无 Vue 页面调用 |
| 2 | `GET /rag/graph/query` | 多跳图谱检索 | ⚠️ API 已定义无页面 | graphApi.query() 已封装但无 Vue 页面调用 |
| 3 | `GET /rag/graph/retrieve` | 图谱检索（供 RAG 编排） | ❌ 无调用 | 🔴 无页面入口 |
| 4 | `GET /rag/graph/triples` | 列出三元组 | ❌ 无调用 | 🔴 无页面入口 |

---

## 四、修复优先级建议

### P0 — 立即修复（功能不可用）

| # | 问题 | 修复方案 |
|---|------|---------|
| 1 | ChatFeedbackController 路由不通 | `@RequestMapping("/api/chat")` → `@RequestMapping("/chat")` |
| 2 | ObservabilityController 无网关路由 | `RouteConfig` 添加 `/api/observability/**` 路由 |
| 3 | PromptController 无网关路由 | `RouteConfig` 添加 `/api/prompts/**` 路由 |

### P1 — 本期实现（核心功能缺失）

| # | 功能 | 修复方案 |
|---|------|---------|
| 4 | 用户反馈页面 | 在 ChatView 中添加反馈按钮（点赞/点踩/评分），调用 `POST /api/chat/feedback` |
| 5 | LLM 调用链追踪页面 | 新建 `TraceView.vue`，输入 requestId 查询调用链详情 |
| 6 | Prompt 管理页面 | 新建 `PromptView.vue`，查看/切换 Prompt 版本 |
| 7 | 检索测试页面 | 在 VectorKbView 中增加检索模式选择，调用 `GET /chat/retrieve/test` |

### P2 — 后续迭代（Agent 与图谱功能）

| # | 功能 | 修复方案 |
|---|------|---------|
| 8 | Agent 对话页面 | 新建 `AgentView.vue`，对接 Agent 多轮对话与确认流程 |
| 9 | 图谱管理页面 | 新建 `GraphView.vue`，管理三元组与多跳检索 |
| 10 | RAG 评估页面 | 新建 `RagEvalView.vue`，运行评估并展示报告 |

---

## 五、网关路由对照表

| 前端请求路径 | 网关路由 | stripPrefix | 转发路径 | Controller 路径 | 是否匹配 |
|------------|---------|-------------|---------|----------------|---------|
| `/api/chat/send` | `/api/chat/**` | 1 | `/chat/send` | `/chat` | ✅ |
| `/api/chat/stream/sse` | `/api/chat/**` | 1 | `/chat/stream/sse` | `/chat` | ✅ |
| `/api/chat/vision/sse` | `/api/chat/**` | 1 | `/chat/vision/sse` | `/chat` | ✅ |
| `/api/chat/upload-image` | `/api/chat/**` | 1 | `/chat/upload-image` | `/chat` | ✅ |
| `/api/chat/feedback` | `/api/chat/**` | 1 | `/chat/feedback` | `/api/chat` | ❌ |
| `/api/rag/knowledge-base/text` | `/api/rag/**` | 1 | `/rag/knowledge-base/text` | `/rag/knowledge-base` | ✅ |
| `/api/rag/knowledge-base/upload` | `/api/rag/**` | 1 | `/rag/knowledge-base/upload` | `/rag/knowledge-base` | ✅ |
| `/api/rag/knowledge-base/search` | `/api/rag/**` | 1 | `/rag/knowledge-base/search` | `/rag/knowledge-base` | ✅ |
| `/api/rag/eval/run` | `/api/rag/**` | 1 | `/rag/eval/run` | `/rag/eval` | ✅ |
| `/api/rag/graph/triple` | `/api/rag/**` | 1 | `/rag/graph/triple` | `/rag/graph` | ✅ |
| `/api/rag/graph/query` | `/api/rag/**` | 1 | `/rag/graph/query` | `/rag/graph` | ✅ |
| `/api/observability/traces/*` | **无路由** | — | — | `/api/observability` | ❌ |
| `/api/prompts` | **无路由** | — | — | `/api/prompts` | ❌ |
| `/api/prompts/*` | **无路由** | — | — | `/api/prompts` | ❌ |

---

## 六、前端 API 封装使用情况

| API 客户端 | 封装位置 | 已封装端点 | 实际被页面调用 |
|-----------|---------|-----------|-------------|
| `chatApi` | api/index.js:62 | 默认客户端 | ✅ history, upload-image, chart |
| `ragApi` | api/index.js:72 | 默认客户端 | ✅ knowledge-base/* |
| `visionApi` | api/index.js:102 | uploadImage | ✅ ChatView.vue |
| `ragEvalApi` | api/index.js:88 | run | ❌ 无页面调用 |
| `graphApi` | api/index.js:93 | addTriple, query | ❌ 无页面调用 |
| `opsApi` | api/index.js:80 | cluster, adoptFaq | ✅ KnowledgeOpsView.vue |
