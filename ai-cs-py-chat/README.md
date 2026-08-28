# ai-cs-py-chat

简单 AI 对话服务(Python):**FastAPI + OpenAI 兼容协议**,默认对接 **DeepSeek**,改 `.env` 即可切换硅基流动(Qwen)等兼容服务。

服务独立运行在 `http://localhost:8000`,开发环境 CORS 全放开,现有前端 `ai-cs-frontend` 可直接跨域调用,**不需要改动 Java 网关**。

## 目录结构

```
ai-cs-py-chat/
├── requirements.txt          # 依赖清单
├── .env.example              # 配置模板(复制为 .env 使用)
├── app/
│   ├── main.py               # FastAPI 入口:CORS + 路由注册
│   ├── config.py             # pydantic-settings 读取 .env
│   ├── api/chat.py           # 路由:/api/health、/api/chat、/api/chat/stream
│   ├── core/llm.py           # LLM 客户端(OpenAI 兼容,流式/非流式)
│   ├── schemas/chat.py       # 请求/响应模型(camelCase 契约)
│   └── services/chat_service.py  # 业务编排:系统提示词 + 多轮历史组装
├── tests/test_chat_api.py    # 接口测试(LLM 全 mock,不花钱)
└── scripts/run.ps1           # 一键:建 venv + 装依赖 + 启动
```

## 快速开始

```powershell
# 0. 前置:安装 Python 3.10+(推荐 3.12),本机当前没有 Python
winget install -e --id Python.Python.3.12
#    装完重新打开终端

# 1. 启动(自动建 venv、装依赖、生成 .env)
cd ai-cs-py-chat
.\scripts\run.ps1

# 2. 配置密钥:编辑 ai-cs-py-chat\.env,填入
#    LLM_API_KEY=sk-xxxx        (DeepSeek 控制台申请)

# 3. 验证
#    浏览器打开 http://localhost:8000/docs (Swagger 调试页)
#    或:curl http://localhost:8000/api/health  ->  {"status":"up"}
```

## 配置说明(.env)

| 变量 | 默认值 | 说明 |
|---|---|---|
| `LLM_BASE_URL` | `https://api.deepseek.com` | OpenAI 兼容服务地址 |
| `LLM_API_KEY` | 空 | API 密钥(必填才能真正对话) |
| `LLM_MODEL` | `deepseek-chat` | 模型名,如 `Qwen/Qwen3-8B` |
| `LLM_TEMPERATURE` / `LLM_MAX_TOKENS` | `0.7` / `2048` | 生成参数 |
| `SYSTEM_PROMPT` | 助手提示词 | 系统提示词 |
| `CORS_ORIGINS` | `*` | 允许的前端来源,生产环境务必收敛 |

## API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/health` | 健康检查 |
| POST | `/api/chat` | 普通对话,JSON 进 JSON 出 |
| POST | `/api/chat/stream` | 流式对话,SSE 输出 |

**普通对话:**

```bash
curl -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"s-001","message":"你好","history":[{"role":"user","content":"我叫小明"},{"role":"assistant","content":"你好,小明"}]}'
```

```json
{"sessionId": "s-001", "reply": "...", "model": "deepseek-chat"}
```

**流式对话(SSE 分帧):**

```
data: {"delta":"你"}

data: {"delta":"好"}

data: [DONE]
```

## 前端接入

前端请求地址直接指向本服务(不经网关),复用 `ChatView.vue` 现有的 `fetch + ReadableStream` 解析逻辑:

```js
const PY_CHAT = 'http://localhost:8000'   // 也可放 import.meta.env

const resp = await fetch(`${PY_CHAT}/api/chat/stream`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ sessionId, message, history })
})
// 后续按空行分帧、取 data: 行的解析方式与 /api/chat/stream/sse 完全一致
```

多轮上下文:把本轮之前的消息放进 `history`(不含当前 `message`),由前端按会话维护。

## 测试

```powershell
cd ai-cs-py-chat
.venv\Scripts\python -m pytest tests/ -v
```

LLM 全部使用替身(FakeLLM),测试不产生任何真实 API 调用与费用。

## 后续扩展点

- 会话历史服务端持久化(接 MySQL / Redis)
- RAG:复用 `ai-cs-knowledge` 的向量检索
- 工具调用(Function Calling)、接入网关统一鉴权
