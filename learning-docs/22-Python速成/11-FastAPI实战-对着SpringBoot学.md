# 11 · FastAPI 实战：对着 Spring Boot 学

> 本篇用你仓库里的真实服务 [`ai-cs-py-chat`](../../ai-cs-py-chat/) 作为教材，**每一段代码都来自实际文件**。
> 目标：让你能把 Spring Boot 的每一层映射过来，然后独立写出一个同等结构的 Python 服务。

---

## 一、整体对照（先建立全景）

```text
Spring Boot                            FastAPI
──────────────────────────────────────────────────────────
@SpringBootApplication          ↔    app = FastAPI()
@RestController                 ↔    APIRouter + @router.get/post
@GetMapping/@PostMapping        ↔    @router.get("/path")/@router.post
@RequestBody + @Valid           ↔    函数参数: Pydantic 模型
@PathVariable                   ↔    @router.get("/{id}") + 函数参数
@RequestParam                   ↔    函数参数 (非路径参数即为 query)
ResponseEntity<T>               ↔    直接 return 对象 / JSONResponse
@RestControllerAdvice           ↔    @app.exception_handler(...)
@ConfigurationProperties        ↔    pydantic-settings BaseSettings
@Service + @Autowired           ↔    模块级单例函数（无容器！）
@Transactional                  ↔    @contextmanager 封装事务
springdoc-openapi               ↔    内置（/docs /redoc /openapi.json）
SseEmitter / Flux<SSE>          ↔    StreamingResponse(media_type="text/event-stream")
Filter / HandlerInterceptor     ↔    Middleware / Depends
Tomcat（内嵌）                   ↔    uvicorn（外部 ASGI 服务器）
```

**最需要转变的一点**：没有 IoC 容器。"依赖注入"变成"传参"或"调单例函数"。

---

## 二、项目结构：五层，一一对应

```text
ai-cs-py-chat/
├── requirements.txt                    ← pom.xml
├── .env.example                        ← application.yml.example
├── app/
│   ├── main.py                         ← Application.java（启动类）
│   ├── config.py                       ← @ConfigurationProperties
│   ├── api/chat.py                     ← @RestController
│   ├── schemas/chat.py                 ← DTO + Bean Validation
│   ├── services/chat_service.py        ← @Service
│   └── core/llm.py                     ← 外部客户端（RestClient）
└── tests/test_chat_api.py              ← @SpringBootTest + MockMvc
```

```java
// Java 版同构结构
com.example.chat/
├── ChatApplication.java
├── config/LlmProperties.java
├── controller/ChatController.java
├── dto/ChatRequest.java
├── service/ChatService.java
└── client/LlmClient.java
```

---

## 三、启动类：`main.py`

```python
# app/main.py（项目实际代码）
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.chat import router as chat_router
from app.config import get_settings


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(
        title="ai-cs-py-chat",
        description="简单 AI 对话服务(OpenAI 兼容协议,默认 DeepSeek)",
        version="0.1.0",
    )

    origins = [o.strip() for o in settings.cors_origins.split(",") if o.strip()]
    if origins == ["*"]:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=["*"], allow_methods=["*"], allow_headers=["*"],
        )
    else:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=origins, allow_credentials=True,
            allow_methods=["*"], allow_headers=["*"],
        )

    app.include_router(chat_router)
    return app


app = create_app()
```

对照 Spring Boot：

```java
@SpringBootApplication
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
    }
}
```

### 三个值得学的设计点

**① `create_app()` 工厂函数**（不是直接 `app = FastAPI(...)`）

```python
def create_app() -> FastAPI:
    ...
```

好处：**测试里可以创建独立实例**，不污染全局状态。对应 Spring 的 `@SpringBootTest` 每次起新上下文。

**② `include_router` 而不是把所有路由写在一个文件**

`APIRouter` 相当于 `@RequestMapping` 的类级分组 + 独立文件：

```python
router = APIRouter(prefix="/api")     # ← 相当于 @RequestMapping("/api")
```

**③ 中间件顺序**

`add_middleware` 的执行顺序是**后加的先执行**（洋葱模型），和 Spring 的 Filter 链顺序规则相反——这是容易搞混的点。

---

## 四、路由层：`api/chat.py`

```python
# app/api/chat.py（项目实际代码）
import json
import logging

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from app.schemas.chat import ChatRequest, ChatResponse
from app.services import chat_service

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api")


@router.get("/health")
async def health() -> dict:
    return {"status": "up"}


@router.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest) -> ChatResponse:
    return await chat_service.get_chat_service().reply(req)
```

```java
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;   // 构造注入

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "up");
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        return chatService.reply(req);
    }
}
```

### 关键差异：参数绑定

```python
@router.get("/orders/{order_id}")           # 路径参数
async def get_order(order_id: int, q: str | None = None):   # ← 自动绑定
    ...
```

```text
order_id 出现在路径模板里      → 从路径取，并按注解转 int（转换失败自动 422）
q 不在路径模板里               → 从 query string 取
ChatRequest（Pydantic 模型）   → 从 body 取（自动 JSON 解析 + 校验）
```

**没有 `@PathVariable` / `@RequestParam` / `@RequestBody`**——全由"名字 + 类型 + 是否出现在路径"推断。

### 状态码与响应头

```python
from fastapi import status
from fastapi.responses import JSONResponse

@router.post("/orders", status_code=status.HTTP_201_CREATED)
async def create(...): ...

@router.get("/x")
async def x():
    return JSONResponse(
        status_code=200,
        content={"a": 1},
        headers={"X-Trace-Id": "abc"},
    )
```

```java
// Java
@PostMapping("/orders")
@ResponseStatus(HttpStatus.CREATED)
public ...
```

---

## 五、Schema 层：Pydantic 模型（**这里是最大的效率来源**）

```python
# app/schemas/chat.py（项目实际代码）
from pydantic import BaseModel, ConfigDict, Field


class ChatMessage(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    role: str = Field(..., description="消息角色:user / assistant / system")
    content: str = Field(..., min_length=1, description="消息内容")


class ChatRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    message: str = Field(..., min_length=1, description="用户当前输入")
    session_id: str | None = Field(None, alias="sessionId", description="会话 ID")
    history: list[ChatMessage] = Field(default_factory=list)


class ChatResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    session_id: str | None = Field(None, alias="sessionId")
    reply: str = Field(..., description="AI 回复全文")
    model: str = Field(..., description="实际使用的模型")
```

```java
// Java 等价的 DTO + 校验注解
public record ChatRequest(
    @NotBlank String message,
    @JsonProperty("sessionId") String sessionId,
    List<@Valid ChatMessage> history
) {}

public record ChatMessage(
    @NotBlank String role,
    @NotBlank String content
) {}
```

### 一个模型 = DTO + 校验 + 序列化 + 文档

| 能力 | Spring Boot | FastAPI |
|---|---|---|
| 反序列化 | Jackson | Pydantic 自动 |
| 字段校验 | `@NotBlank` + `@Valid` | `Field(min_length=1)` 直接写在类型上 |
| 错误响应 | `MethodArgumentNotValidException` → 自己写 handler | **自动 422 + 详细错误定位** |
| API 文档 | springdoc 扫注解 | 直接扫模型生成 schema |
| 命名风格转换 | `@JsonProperty` | `alias="sessionId"` |

**自动 422 的返回示例**（零配置）：

```json
{
  "detail": [
    {
      "type": "string_too_short",
      "loc": ["body", "message"],
      "msg": "String should have at least 1 character",
      "input": ""
    }
  ]
}
```

Java 里要实现同样精度，得写一个 `@RestControllerAdvice` 遍历 `FieldError`。**这是 FastAPI 最省事的地方**。

### `populate_by_name=True` 的用途

```python
# 前端发 camelCase（符合前端/Java 习惯）
{"message": "hi", "sessionId": "s1"}

# Python 内部用 snake_case（符合 PEP 8）
req.session_id        # "s1"

# 响应序列化时仍输出 camelCase
{"sessionId": "s1", "reply": "...", "model": "deepseek-chat"}
```

`alias` + `populate_by_name` 一起用，实现了**跨语言契约统一 + Python 内部风格正确**。这个设计值得抄到自己的项目。

---

## 六、Service 层：没有容器怎么"注入"

```python
# app/services/chat_service.py（项目实际代码）
class ChatService:
    def __init__(self, llm: LLMClient | None = None):
        self.llm = llm or get_llm_client()      # ← 构造注入 + 默认单例

    async def reply(self, req: ChatRequest) -> ChatResponse:
        text = await self.llm.chat(req.history, req.message)
        return ChatResponse(session_id=req.session_id, reply=text,
                            model=self.llm.settings.llm_model)


_service: ChatService | None = None

def get_chat_service() -> ChatService:
    global _service
    if _service is None:
        _service = ChatService()
    return _service
```

```java
@Service
public class ChatService {
    private final LlmClient llm;
    public ChatService(LlmClient llm) { this.llm = llm; }
    ...
}
```

### 三种"单例"写法，本项目选了最朴素的一种

```python
# ① 模块级变量 + 惰性初始化（项目用法）
_service = None
def get_chat_service():
    global _service
    if _service is None:
        _service = ChatService()
    return _service

# ② lru_cache（config/llm 用的，更简洁）
@lru_cache(maxsize=1)
def get_llm_client() -> LLMClient:
    return LLMClient()

# ③ 模块级直接实例化（最简单，但导入时就执行）
_service = ChatService()
```

| 写法 | 优点 | 缺点 |
|---|---|---|
| ① 惰性 + global | 可测试替换（`global _service = Mock()`） | 代码略啰嗦 |
| ② `lru_cache` | 极简 | 无法清空（除非 `cache_clear()`） |
| ③ 直接实例化 | 最简 | 导入即执行，配置未就绪会炸 |

### 没有 DI 会不会很痛苦？

**90% 的 Python 服务不需要 DI 框架。** 原因：

- 模块本身就是单例作用域（`import` 只执行一次）
- 需要替换时，直接改模块变量或用 `pytest` 的 `monkeypatch`
- 需要多实现时，用参数传入（如 `ChatService(llm=MockLLM())`）

真的需要时再上 `dependency-injector` 或 `FastAPI` 的 `Depends`：

```python
from fastapi import Depends

def get_repo() -> Repo:
    return Repo()

@router.get("/orders")
async def list_orders(repo: Repo = Depends(get_repo)):
    return repo.all()
```

`Depends` 的价值在**生命周期管理**（每请求一个 DB session）：

```python
def get_db():
    db = SessionLocal()
    try:
        yield db            # yield 之前 = 前置，之后 = 后置（自动清理）
    finally:
        db.close()

@router.get("/x")
async def x(db = Depends(get_db)):     # 每请求一个新 session，用完自动关
    ...
```

这相当于 Spring 的 `@Scope("request")` + `@Transactional` 的组合，但**没有代理、没有 AOP**——就是一个生成器。

---

## 七、外部客户端：`core/llm.py`

```python
# app/core/llm.py（项目实际代码，节选）
from openai import AsyncOpenAI

class LLMClient:
    def __init__(self, settings: Settings | None = None):
        self.settings = settings or get_settings()
        self._client = AsyncOpenAI(
            base_url=self.settings.llm_base_url,
            api_key=self.settings.llm_api_key or "not-configured",
            timeout=self.settings.llm_timeout,
        )

    async def chat(self, history, message) -> str:
        resp = await self._client.chat.completions.create(
            model=self.settings.llm_model,
            messages=self._build_messages(history, message),
            temperature=self.settings.llm_temperature,
            max_tokens=self.settings.llm_max_tokens,
        )
        return resp.choices[0].message.content or ""

    async def chat_stream(self, history, message):
        stream = await self._client.chat.completions.create(..., stream=True)
        async for chunk in stream:
            if chunk.choices and chunk.choices[0].delta.content:
                yield chunk.choices[0].delta.content
```

### 与 Java 侧 `ResilientAiService` 的对照

你项目里 Java 侧用 Resilience4j 做了四件事，Python 侧目前**只做了超时**：

| 能力 | Java（ResilientAiService） | Python（当前） | 怎么补 |
|---|---|---|---|
| 超时 | `@TimeLimiter` + `CompletableFuture.get(timeout)` | `AsyncOpenAI(timeout=)` + `asyncio.wait_for` | ✅ 已有 |
| 重试 | `@Retry`（只配网络异常） | 无 | 自己写 `@retry` 装饰器（见 04 篇） |
| 熔断 | `@CircuitBreaker` | 无 | `pybreaker` 库 |
| 降级 | `@Fallback` | 无 | `except` + 返回兜底值 |
| 流式不重试 | 独立实例，不配 Retry | 天然（Flux 不可重放同理） | — |

```python
# 补齐版的思路（可以直接抄）
import asyncio
from collections.abc import AsyncIterator

class ResilientLLMClient(LLMClient):
    async def chat(self, history, message) -> str:
        last_exc = None
        for attempt in range(1, self.settings.retry_times + 1):
            try:
                return await asyncio.wait_for(
                    super().chat(history, message),
                    timeout=self.settings.llm_timeout,
                )
            except (asyncio.TimeoutError, ConnectionError) as e:
                last_exc = e
                if attempt < self.settings.retry_times:
                    await asyncio.sleep(0.5 * 2 ** (attempt - 1))   # 指数退避
        # 降级
        return "抱歉，服务暂时不可用，请稍后再试。"

    async def chat_stream(self, history, message) -> AsyncIterator[str]:
        # 流式不重试（与 Java 侧一致：流不可重放）
        async for d in super().chat_stream(history, message):
            yield d
```

> **注意**：`asyncio.wait_for` 的超时是**真超时**——不像 Java 里 `Mono.fromCallable` 忘了 `subscribeOn` 时计时器要等阻塞返回。因为 await 点是真正的协作让出点。这一点 Python 比 Java 的响应式编程少一个坑。

---

## 八、SSE 流式：对照 `Flux<ServerSentEvent>`

```python
# app/api/chat.py（项目实际代码）
@router.post("/chat/stream")
async def chat_stream(req: ChatRequest) -> StreamingResponse:
    async def event_gen():
        try:
            async for delta in chat_service.get_chat_service().stream(req):
                yield f"data: {json.dumps({'delta': delta}, ensure_ascii=False)}\n\n"
        except Exception as exc:
            logger.exception("流式对话出错")
            yield f"data: {json.dumps({'error': str(exc)}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_gen(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
```

```java
// Java WebFlux 等价
@PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> stream(@RequestBody ChatRequest req) {
    return chatService.stream(req)
        .map(d -> ServerSentEvent.builder(d).build())
        .concatWith(Mono.just(ServerSentEvent.builder("[DONE]").build()));
}
```

### SSE 协议的三条硬规则

```text
① 每条消息以 "data: " 开头
② 消息之间用 "\n\n" 分隔（空行分帧）
③ 响应头 Content-Type: text/event-stream
```

**任何一条错了，前端就收不到数据**。项目里的 `\n\n` 和 `media_type` 都是必须的。

### 两个易错点

**`X-Accel-Buffering: no`** — 让 Nginx 不要缓冲。不加的话，Nginx 会攒够 buffer 才转发，看起来就像"流式失效"。

```python
headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"}
```

**异常也要发一帧，然后正常收尾**

```python
except Exception as exc:
    yield f"data: {json.dumps({'error': str(exc)})}\n\n"
yield "data: [DONE]\n\n"     # ← 无论成功失败都要发
```

否则前端会一直等 `[DONE]`，界面永远停在"加载中"。

### 对比 Java 侧的项目经验

你的项目记忆里有一条：**流式不配 Retry（Flux 不可重放）**。Python 里同理——`AsyncIterator` 也**只能遍历一次**，重试必须重新发起请求，不能在同一个生成器上重来。

---

## 九、异常处理：`@app.exception_handler`

```python
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

app = FastAPI()

class AppException(Exception):
    def __init__(self, code: str, message: str):
        self.code = code
        self.message = message


@app.exception_handler(AppException)
async def handle_app(request: Request, exc: AppException):
    return JSONResponse(status_code=200,
                        content={"code": exc.code, "message": exc.message})


@app.exception_handler(Exception)          # 兜底
async def handle_all(request: Request, exc: Exception):
    logger.exception("未捕获异常 path=%s", request.url.path)
    return JSONResponse(status_code=500, content={"code": "INTERNAL", "message": "系统繁忙"})
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result handle(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }
}
```

**注意 `@app.exception_handler(Exception)` 的局限**：它抓不到 `StreamingResponse` 内部生成器抛的异常（因为响应头已经发出去了）。所以流式端点必须**自己在生成器里 try/except**——这就是项目代码里那段 `try ... except ... finally yield [DONE]` 的原因。

---

## 十、中间件与横切关注点

```python
import time, uuid
from fastapi import Request

@app.middleware("http")
async def add_trace_id(request: Request, call_next):
    trace_id = request.headers.get("X-Trace-Id") or str(uuid.uuid4())
    start = time.perf_counter()
    response = await call_next(request)
    response.headers["X-Trace-Id"] = trace_id
    response.headers["X-Cost-Ms"] = f"{(time.perf_counter()-start)*1000:.1f}"
    return response
```

```java
// Java 等价：HandlerInterceptor 或 Filter
public class TraceFilter implements Filter {
    public void doFilter(...) { ... }
}
```

> **中间件里不能 return 已经 `call_next` 之后的响应体内容**——`response` 是 `StreamingResponse` 时无法再修改 body。这是与 Servlet Filter 的一个差别。

---

## 十一、测试：`tests/test_chat_api.py`

```python
# 项目模式（简化示意）
import pytest
from httpx import ASGITransport, AsyncClient

from app.main import create_app
from app.services import chat_service


@pytest.fixture
async def client(monkeypatch):
    # 把 LLM 客户端替换掉，测试不花钱、不依赖网络
    class FakeLLM:
        settings = type("S", (), {"llm_model": "fake"})()

        async def chat(self, history, message):
            return f"echo: {message}"

        async def chat_stream(self, history, message):
            for ch in ["你", "好"]:
                yield ch

    monkeypatch.setattr(chat_service, "_service", chat_service.ChatService(llm=FakeLLM()))

    app = create_app()
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as c:
        yield c


@pytest.mark.asyncio
async def test_health(client):
    r = await client.get("/api/health")
    assert r.status_code == 200
    assert r.json() == {"status": "up"}


@pytest.mark.asyncio
async def test_chat(client):
    r = await client.post("/api/chat", json={"message": "hi"})
    assert r.status_code == 200
    assert r.json()["reply"] == "echo: hi"


@pytest.mark.asyncio
async def test_validation_422(client):
    r = await client.post("/api/chat", json={"message": ""})    # 违反 min_length=1
    assert r.status_code == 422


@pytest.mark.asyncio
async def test_stream(client):
    async with client.stream("POST", "/api/chat/stream", json={"message": "hi"}) as r:
        assert r.headers["content-type"].startswith("text/event-stream")
        body = ""
        async for chunk in r.aiter_text():
            body += chunk
    assert "data: [DONE]" in body
```

### 与 Java 测试对照

| Spring Boot | FastAPI + pytest |
|---|---|
| `@SpringBootTest` | 不需要（`create_app()` 就够，秒级启动） |
| `@AutoConfigureMockMvc` + `MockMvc` | `httpx.AsyncClient` + `ASGITransport` |
| `@MockBean` | `monkeypatch.setattr` |
| `@ParameterizedTest` | `@pytest.mark.parametrize` |
| `@BeforeEach` | `@pytest.fixture` |
| JUnit 断言 | 裸 `assert`（pytest 会重写提供详细 diff） |

**最大优势**：启动一个 FastAPI 测试 app 只要毫秒级，不用起 Spring 上下文。所以 Python 项目里"每个测试都起完整 app"是完全可接受的。

---

## 十二、配置：`.env` + pydantic-settings

```python
# app/config.py（项目实际代码）
from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )

    llm_base_url: str = "https://api.deepseek.com"
    llm_api_key: str = ""
    llm_model: str = "deepseek-chat"
    llm_temperature: float = 0.7
    llm_max_tokens: int = 2048
    llm_timeout: float = 60.0

    system_prompt: str = "你是一个乐于助人的 AI 助手,请用简洁的中文回答。"

    app_host: str = "0.0.0.0"
    app_port: int = 8000
    cors_origins: str = "*"


@lru_cache
def get_settings() -> Settings:
    return Settings()
```

```java
@ConfigurationProperties(prefix = "")     // 或自定义 prefix
@Validated
public record AppProperties(
    String llmBaseUrl, String llmApiKey, String llmModel, ...
) {}
```

**机制**：环境变量名 `LLM_BASE_URL` → 字段 `llm_base_url`（大小写不敏感 + 下划线映射）。所以 `.env` 里写 `LLM_BASE_URL=...` 就能覆盖。

优先级：**环境变量 > .env 文件 > 字段默认值**。这和 Spring 的 `环境变量 > application.yml > @Value 默认` 一致。

---

## 十三、完整开发流程（从零建一个同构服务）

```bash
# 1. 建项目
mkdir my-service && cd my-service
uv init
uv add fastapi "uvicorn[standard]" pydantic-settings python-dotenv
uv add --dev pytest pytest-asyncio httpx

# 2. 建目录
mkdir -p app/api app/schemas app/services app/core tests
touch app/__init__.py app/api/__init__.py app/schemas/__init__.py \
      app/services/__init__.py app/core/__init__.py tests/__init__.py

# 3. 写代码（按 main → config → schemas → services → api 顺序）

# 4. 跑
uv run uvicorn app.main:app --reload --port 8000

# 5. 打开 http://localhost:8000/docs 调接口

# 6. 测试
uv run pytest -v
```

**开发顺序建议**（和 Java 相反）：先写 `schemas`（契约），再写 `services`（逻辑），最后写 `api`（路由）。因为 Pydantic 模型一旦定了，路由和文档都自动生成。

---

## 十四、动手任务（验收用）

在 `ai-cs-py-chat` 上加一个端点，验证你学会了：

```text
需求：POST /api/chat/count
请求体：{"message": "你好世界"}
响应体：{"chars": 4, "words": 2}
```

要求：

1. 在 `app/schemas/chat.py` 加 `CountResponse` 模型（用 `Field(description=...)`）
2. 在 `app/services/chat_service.py` 加 `count` 方法
3. 在 `app/api/chat.py` 加路由（`response_model=CountResponse`）
4. 在 `tests/test_chat_api.py` 加一条测试
5. `pytest -v` 通过
6. 打开 `/docs` 看到新端点的文档

做到这五步，说明你已经能独立开发 FastAPI 服务了。

---

**上一篇**：[10-标准库必会清单](10-标准库必会清单.md)　**下一篇**：[12-Java开发者踩坑清单](12-Java开发者踩坑清单.md)
