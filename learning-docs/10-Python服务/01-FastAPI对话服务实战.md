# 01-FastAPI 对话服务实战：从零开始理解 Python 微服务

> 本文面向只写过 Java、第一次写 Python 服务端代码的读者。
> 当前实现：`ai-cs-py-chat` 是一个独立于 Java 网关的轻量对话服务（FastAPI + AsyncOpenAI，端口 8000），提供非流式与 SSE 流式两种对话端点，pytest 全 mock 测试。
> 诚实说：该服务目前**未接入** docker-compose/K8s 编排，前端也尚未调用（前端 src 中无 8000 端口引用）——它是"多语言服务"的演练场，也是学习 Python 服务端的现成教材。

---

## 一、为什么项目里要有一个 Python 服务

| 问题 | 回答 |
|---|---|
| Java 侧已有 `ai-cs-chat`，为什么还要 Python 版？ | 一字不差地复刻"对话"这条最薄的链路，用来对照学习两种生态；同时验证 OpenAI 兼容协议的**跨语言通用性**——协议与语言无关，换语言只需要换 HTTP 客户端 |
| 它在生产链路里吗？ | 不在。独立运行、CORS 全开直连前端（不走网关），定位是独立服务/实验田 |
| 用了 LangChain 吗？ | 没有。只依赖 `openai` 官方客户端，与 Java 侧"纯 Spring AI 自研"的路线一致（见 [05-AI集成/01-SpringAI框架集成](../05-AI集成/01-SpringAI框架集成/)） |

## 二、FastAPI 是什么（对照 Spring Boot 学）

FastAPI 之于 Python，约等于 Spring Boot 之于 Java，但哲学不同：

| 概念 | Spring Boot | FastAPI |
|---|---|---|
| 入口 | `@SpringBootApplication` + 内嵌 Tomcat | `FastAPI()` 实例 + uvicorn（ASGI 服务器） |
| 声明路由 | `@GetMapping("/api/chat")` | `@router.post("/api/chat")`（装饰器） |
| 请求体校验 | `@Valid` + Bean Validation 注解 | **Pydantic 模型即校验**（类型注解自带校验，非法输入自动 422） |
| 依赖注入 | IoC 容器 | `Depends()`（轻量，按请求解析） |
| 配置 | `application.yml` + `@ConfigurationProperties` | pydantic-settings + `.env` |
| 并发模型 | 线程池 + 虚拟线程（Java 21） | asyncio 事件循环（协程） |
| 文档 | SpringDoc /swagger-ui | 自动生成（OpenAPI 原生，访问 `/docs`） |

最大的思维转变：**没有容器**。Spring 里 Bean 单例由容器管理；FastAPI 里就是你熟悉的普通模块级单例函数（本项目用 `@lru_cache` 实现）。

## 三、项目结构：薄到极致的四层

```text
ai-cs-py-chat/
├── requirements.txt          # 6 个运行依赖 + 2 个测试依赖，全文见下
├── .env.example              # 配置样例（含切硅基流动 Qwen 的示例）
├── scripts/run.ps1           # 一键建 venv + 装依赖 + 启动
├── app/
│   ├── main.py               # create_app 工厂：FastAPI 实例 + CORS + 挂路由
│   ├── config.py             # pydantic-settings 配置类
│   ├── api/chat.py           # 路由层：3 个端点
│   ├── core/llm.py           # LLM 客户端封装
│   ├── schemas/chat.py       # 请求/响应 Pydantic 模型
│   └── services/chat_service.py  # 业务编排 + 进程内单例
└── tests/
    └── test_chat_api.py      # pytest：health/多轮/422/SSE 分帧/CORS 预检
```

`requirements.txt` 全文——体会一下与 Java BOM 的体量差异：

```text
fastapi>=0.115.0
uvicorn[standard]>=0.30.0
openai>=1.40.0
pydantic>=2.7.0
pydantic-settings>=2.3.0
pytest>=8.2.0
httpx>=0.27.0
```

## 四、配置管理：pydantic-settings

`app/config.py`（`SettingsConfigDict(env_file=".env", extra="ignore")`）：

| 字段 | 默认值 | 说明 |
|---|---|---|
| `llm_base_url` | `https://api.deepseek.com` | OpenAI 兼容网关地址 |
| `llm_api_key` | `""` | `.env` 中注入 |
| `llm_model` | `deepseek-chat` | 默认 DeepSeek，可切硅基流动 Qwen |
| `llm_temperature` | `0.7` | |
| `llm_max_tokens` | `2048` | |
| `llm_timeout` | `60.0` | 客户端超时 |
| `system_prompt` | （客服人设提示词） | |
| `app_host` / `app_port` | `0.0.0.0` / `8000` | |
| `cors_origins` | `*` | 全放开（前端 5173 直连） |

单例套路（Python 版的"容器单例"）：

```python
@lru_cache
def get_settings() -> Settings:
    return Settings()
```

`@lru_cache` 使首次调用后全部复用同一实例——测试要换配置时，`monkeypatch` 替换工厂函数即可（见第七节）。

## 五、对话链路：AsyncOpenAI 客户端

`app/core/llm.py` 的 `LLMClient`：

```python
class LLMClient:
    def __init__(self, settings):
        self.client = AsyncOpenAI(base_url=settings.llm_base_url,
                                  api_key=settings.llm_api_key or "not-configured",
                                  timeout=settings.llm_timeout)

    def _build_messages(self, history, message):
        # system 提示词 + 历史多轮 + 本条用户输入
        ...

    async def chat(self, history, message):
        resp = await self.client.chat.completions.create(
            model=..., messages=self._build_messages(history, message))
        return resp.choices[0].message.content

    async def chat_stream(self, history, message):
        stream = await self.client.chat.completions.create(..., stream=True)
        async for chunk in stream:
            delta = chunk.choices[0].delta.content
            if delta:
                yield delta
```

与 Java 侧对照：`AsyncOpenAI` ≈ `OpenAiChatModel`；`chat_stream` 的 `async for ... yield` ≈ Java 的 `Flux<String>`；`_build_messages` 的 system+history+user 组装与 `ai-cs-chat` 的多轮记忆结构完全同构。**协议相同，语义零翻译**——这就是 OpenAI 兼容协议作为"行业方言"的价值。

## 六、SSE 流式实现：StreamingResponse + 异步生成器

`app/api/chat.py` 的流式端点（对比 [04-中间件/05-SSE与WebSocket实时通信](../04-中间件/05-SSE与WebSocket实时通信.md) 里 Java 的 `Flux` 写法）：

```python
@router.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    async def event_gen():
        async for delta in chat_service.get_chat_service().stream(req):
            yield f"data: {json.dumps({'delta': delta}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(event_gen(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})
```

三个协议要点（与 Java 版一致，属于 SSE 协议本身的规定）：

1. 每帧 `data: ` 前缀 + **两个换行** `\n\n` 分帧；
2. 自定义结束哨兵 `data: [DONE]`（OpenAI 生态惯例）；
3. `X-Accel-Buffering: no` 防止 Nginx 缓冲把流攒成一坨；`Cache-Control: no-cache` 防中间层缓存。

帧内 JSON 用 `ensure_ascii=False`：中文 delta 直接输出原文而非 `\uXXXX` 转义，前端拼接显示更省事。

## 七、pytest 测试：FakeLLM 替身与 monkeypatch

`tests/test_chat_api.py` 的替身思路——不 mock HTTP，直接**替换业务单例工厂**：

```python
class FakeLLM:
    """LLM 替身：回显用户输入，并记录每次调用的入参供断言。"""
    def __init__(self):
        self.settings = SimpleNamespace(llm_model="fake-model")
        self.calls = []
    async def chat(self, history, message):
        self.calls.append({"history": history, "message": message})
        return f"echo:{message}"
    async def chat_stream(self, history, message):
        for char in f"echo:{message}":
            yield char

# fixture：把 get_chat_service 换成返回注入 FakeLLM 的 ChatService
monkeypatch.setattr(chat_service, "get_chat_service", lambda: ChatService(llm=fake))
```

覆盖的五类断言，可与 Java 侧 `@MockitoBean` 替身思路一一对照：

| 测试点 | 断言方式 |
|---|---|
| `/api/health` 健康 | `resp.json() == {"status": "up"}` |
| 多轮历史透传 | `fake.calls[0]["history"]` 内容 |
| 空消息校验 | 断言 422（Pydantic 校验自动生效） |
| SSE 分帧格式 | 拼接 delta 还原全文 + 末帧 `data: [DONE]` |
| CORS 预检 | `Origin: http://localhost:5173` 的 OPTIONS 响应头 |

```bash
python -m pytest tests/ -v
```

对比 Java：无 Spring 上下文、无 `@SpringBootTest`、毫秒级启动——FastAPI 的 `TestClient`（基于 httpx）直接调用 ASGI app，这就是 Python 测试"轻"的直观感受。

## 八、运行与调用

```bash
# Windows 一键启动（建 venv + 装依赖 + uvicorn）
powershell -File scripts/run.ps1

# 手动等价操作
python -m venv .venv && .venv\Scripts\activate
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# 冒烟
curl http://localhost:8000/api/health
curl -X POST http://localhost:8000/api/chat -H "Content-Type: application/json" \
  -d '{"message":"你好"}'
# 自动文档: 浏览器打开 http://localhost:8000/docs
```

**诚实说（缺口即练习）**：该服务不在任何 compose/K8s 编排中、前端未接入、无 Dockerfile。把它产品化是绝佳练手：写一个多阶段 Dockerfile → 加进 `docker-compose.yml`（注意根 compose 的 8000 端口已被 chroma 占用，需改端口或改内部网络互通）→ 前端仿照 `api/index.js` 的 `createClient` 加一个 py 服务客户端。

## 九、与 ai-cs-chat 的能力对照

| 能力 | ai-cs-java-chat | ai-cs-py-chat |
|---|---|---|
| 基础对话 / 多轮记忆 | ✅ ChatClient | ✅ ChatService |
| SSE 流式 | ✅ Flux + Resilience4j（TimeLimiter/Retry/CircuitBreaker） | ✅ StreamingResponse（无弹性层） |
| RAG / Agent / NL2SQL / GraphRAG | ✅ 全量 | ❌（刻意保持薄） |
| 模型协议 | OpenAI 兼容（DeepSeek/SiliconFlow） | 相同 |
| 测试 | JUnit5 + Mockito + Cucumber | pytest + FakeLLM |

读懂这张表就明白它的定位：**Python 版不是功能对等的镜像，而是"最小对话服务"参照物**——用最少的代码展示同一协议栈在另一门语言里的样子。

## 十、面试要点总结

> 项目包含一个 FastAPI 实现的 Python 对话服务，与 Java 主链路走同一 OpenAI 兼容协议（DeepSeek/SiliconFlow），提供非流式与 SSE 流式端点；配置用 pydantic-settings + @lru_cache 单例，流式用 StreamingResponse + 异步生成器逐帧输出 data JSON 并以 [DONE] 收尾，测试用 FakeLLM 替身 + monkeypatch 替换业务工厂，实现无网络依赖的毫秒级测试；服务目前独立运行未入编排，Docker 化与前端接入是现成的演进项。

```text
关键词：FastAPI ≈ Spring Boot · Pydantic 即校验(422) · ASGI/uvicorn ≈ Servlet/Tomcat
SSE 三要点 = data: + \n\n 分帧 · [DONE] 哨兵 · X-Accel-Buffering: no
测试哲学 = 替换工厂函数(monkeypatch) ≈ Mockito 替身
```

## 学习检查清单

- [ ] 本地跑通 `scripts/run.ps1` 并在 `/docs` 手动调用三个端点
- [ ] 能对照说出 Java `Flux` 流式与 Python 异步生成器流式的三处对应关系
- [ ] 完成 8.1 的 Dockerfile + compose 接入练习
- [ ] 给 FakeLLM 加一个"抛异常"实现，写一个对话接口 500 的失败用例
