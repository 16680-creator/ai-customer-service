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
# ---- 本段讲：FastAPI 的启动入口，相当于 Spring Boot 的 Application.java ----
# 关键差异：没有 @SpringBootApplication 的自动扫描，路由/中间件都要手动注册。
# 用工厂函数 create_app() 而非直接建全局 app，是为了测试时能创建独立实例。
from fastapi import FastAPI                                  # 应用核心类；≈ SpringApplication 创建的那个容器
from fastapi.middleware.cors import CORSMiddleware           # 跨域中间件；≈ WebMvcConfigurer#addCorsMappings

from app.api.chat import router as chat_router               # 导入路由对象；Java 靠 @ComponentScan 自动收集 @RestController
from app.config import get_settings                          # 配置读取函数（内部 lru_cache）；≈ 注入 @ConfigurationProperties


def create_app() -> FastAPI:                                 # 工厂函数；"-> FastAPI" 是类型注解，相当于 Java 的方法返回类型
    settings = get_settings()                                # 取配置单例；Java 里是 @Autowired 注入配置 Bean
    app = FastAPI(                                           # 实例化应用；这一段 ≈ @SpringBootApplication 启动类
        title="ai-cs-py-chat",                               # 只影响 /docs 页面标题，不影响路由
        description="简单 AI 对话服务(OpenAI 兼容协议,默认 DeepSeek)",  # 文档描述，会显示在 Swagger 顶部
        version="0.1.0",                                     # 文档版本号，纯展示用
    )

    origins = [o.strip() for o in settings.cors_origins.split(",") if o.strip()]
    # ↑ 把配置里的 "a,b,c" 拆成 ["a","b","c"]：列表推导式 ≈ Java stream().map(String::trim).filter(s->!s.isEmpty()).toList()
    if origins == ["*"]:                                     # 通配来源时不能同时开 allow_credentials（浏览器规范限制）
        app.add_middleware(                                  # 注册中间件；等价于 Java 里注册一个 Filter
            CORSMiddleware,                                  # 中间件类；后注册的先执行（洋葱模型）
            allow_origins=["*"], allow_methods=["*"], allow_headers=["*"],   # 全放开，仅开发环境合适
        )
    else:                                                    # 配置了具体域名时，允许携带凭证（Cookie）
        app.add_middleware(
            CORSMiddleware,
            allow_origins=origins, allow_credentials=True,   # 白名单 + 允许 Cookie
            allow_methods=["*"], allow_headers=["*"],        # 方法和请求头仍全放开
        )

    app.include_router(chat_router)                          # 挂载子路由；≈ @RequestMapping("/api") 的类级分组，但需手动 include
    return app                                               # 返回应用对象，交给下面模块级变量保存


app = create_app()                                           # 模块级单例；uvicorn "app.main:app" 导入的就是这个对象。
                                                             # import 只执行一次 ⇒ 天然的全局单例（Java 里由 IoC 容器保证）
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
# ---- 本段讲：工厂函数签名本身，重点看返回类型注解 ----
# 这里只保留签名，函数体用 ... 省略，代表"此处省略实现"（Python 的占位符，不是可运行语句）。
def create_app() -> FastAPI:   # -> FastAPI 是返回类型注解，供 IDE/mypy 检查；Java 中写作 public FastAPI createApp()
    ...                        # Ellipsis 占位：等价于 Java 接口里的方法体留空，正式代码里换成真正的实现
```

好处：**测试里可以创建独立实例**，不污染全局状态。对应 Spring 的 `@SpringBootTest` 每次起新上下文。

**② `include_router` 而不是把所有路由写在一个文件**

`APIRouter` 相当于 `@RequestMapping` 的类级分组 + 独立文件：

```python
# ---- 本段讲：APIRouter 的类级前缀，等价于 @RequestMapping("/api") ----
# APIRouter 把一组端点收拢到一个对象里，最后在 main.py 用 include_router 挂载。
router = APIRouter(prefix="/api")     # ← 相当于 @RequestMapping("/api")；
                                      #   之后 @router.get("/health") 实际路径就是 /api/health
```

**③ 中间件顺序**

`add_middleware` 的执行顺序是**后加的先执行**（洋葱模型），和 Spring 的 Filter 链顺序规则相反——这是容易搞混的点。

---

## 四、路由层：`api/chat.py`

```python
# app/api/chat.py（项目实际代码）
# ---- 本段讲：路由层，一个文件 ≈ 一个 @RestController ----
# 对应关系：APIRouter ↔ @RestController + @RequestMapping；
#          @router.get/post ↔ @GetMapping/@PostMapping；
#          参数类型注解 ↔ @RequestBody/@PathVariable 的自动绑定。
import json                                       # 标准库 JSON 序列化；≈ Jackson 的 ObjectMapper
import logging                                    # 标准库日志；≈ SLF4J 的 LoggerFactory

from fastapi import APIRouter                     # 路由分组对象；≈ @RestController 的类级容器
from fastapi.responses import StreamingResponse   # 流式响应；≈ 返回 Flux / SseEmitter

from app.schemas.chat import ChatRequest, ChatResponse   # 导入 Pydantic 模型；≈ 导入 DTO 类
from app.services import chat_service                    # 导入 service 模块（注意：导入的是模块，不是实例）

logger = logging.getLogger(__name__)              # 用当前模块名建 logger；≈ LoggerFactory.getLogger(ChatController.class)
router = APIRouter(prefix="/api")                 # 类级前缀；与此前 main.py 里的 include_router 配合生效


@router.get("/health")                            # 装饰器注册 GET /api/health；≈ @GetMapping("/health")
async def health() -> dict:                       # async def：协程函数，由事件循环调度；≈ 非阻塞 Controller 方法
    return {"status": "up"}                       # 直接返回 dict，FastAPI 自动序列化为 JSON；≈ return Map.of("status","up")


@router.post("/chat", response_model=ChatResponse)   # POST 路由；response_model 指定序列化/文档用的出参模型
async def chat(req: ChatRequest) -> ChatResponse:    # 参数是 Pydantic 模型 ⇒ 自动从 body 解析并校验；≈ @Valid @RequestBody
    return await chat_service.get_chat_service().reply(req)
    # ↑ 先拿 service 单例（模块级函数，非容器注入），再 await 异步调用。
    #   Java 写法：return chatService.reply(req);（由 @Autowired 字段提供 chatService）
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
# ---- 本段讲：FastAPI 如何自动绑定参数（没有 @PathVariable/@RequestParam）----
# 规则：名字出现在路径模板里 → 路径参数；否则 → query 参数；参数是 Pydantic 模型 → body。
@router.get("/orders/{order_id}")           # 路径参数
async def get_order(order_id: int, q: str | None = None):   # ← 自动绑定
    # order_id 与路径里的 {order_id} 同名 ⇒ 从 URL 取，并强制转 int（失败返回 422）
    # q: str | None = None ⇒ "可为 None 的字符串，默认 None"；Java 写作 @RequestParam(required=false) String q
    # Python 3.10+ 的 X | None 等价于 Optional[X]；q 不在路径模板里 ⇒ 从 ?q=... 取
    ...                                     # 函数体省略，真实实现会 return 订单对象
```

```text
order_id 出现在路径模板里      → 从路径取，并按注解转 int（转换失败自动 422）
q 不在路径模板里               → 从 query string 取
ChatRequest（Pydantic 模型）   → 从 body 取（自动 JSON 解析 + 校验）
```

**没有 `@PathVariable` / `@RequestParam` / `@RequestBody`**——全由"名字 + 类型 + 是否出现在路径"推断。

### 状态码与响应头

```python
# ---- 本段讲：自定义状态码与响应头，等价 @ResponseStatus 和 ResponseEntity ----
from fastapi import status                     # 状态码常量模块；≈ HttpStatus 枚举
from fastapi.responses import JSONResponse     # 需要精细控制响应时用它；≈ ResponseEntity<Map<String,Object>>

@router.post("/orders", status_code=status.HTTP_201_CREATED)   # 声明成功状态码 201；≈ @ResponseStatus(HttpStatus.CREATED)
async def create(...): ...                     # "..." 表示省略参数与函数体；真实项目写具体签名

@router.get("/x")                              # 注册 GET /x
async def x():
    return JSONResponse(                       # 返回显式响应对象（而不是让框架自动序列化）
        status_code=200,                       # 手动指定状态码
        content={"a": 1},                      # 响应体，会被 JSON 序列化
        headers={"X-Trace-Id": "abc"},         # 自定义响应头；≈ ResponseEntity.header("X-Trace-Id","abc")
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
# ---- 本段讲：Pydantic 模型 = DTO + 校验 + 序列化 + OpenAPI 文档，四合一 ----
# 一个 BaseModel 子类就顶掉 Java 里的 record DTO + Bean Validation 注解 + Jackson 配置。
# 注意：字段用类型注解声明，默认值写在等号右边；没有手写 getter/setter。
from pydantic import BaseModel, ConfigDict, Field      # BaseModel=模型基类；Field=字段级约束；ConfigDict=模型级配置


class ChatMessage(BaseModel):                          # 单条消息；≈ record ChatMessage(String role, String content)
    model_config = ConfigDict(populate_by_name=True)   # 允许用字段名(snake_case)或 alias(camelCase)两种写法赋值

    role: str = Field(..., description="消息角色:user / assistant / system")
    # role: str 声明必填字符串；Field(...) 里的 "..." 表示必填（无默认值），description 只进文档
    content: str = Field(..., min_length=1, description="消息内容")
    # min_length=1 ⇒ 空串直接 422；≈ @NotBlank，但不需要额外 @Valid


class ChatRequest(BaseModel):                          # 请求体；≈ @RequestBody 绑定的 DTO
    model_config = ConfigDict(populate_by_name=True)   # 配合下面的 alias 实现"外部 camelCase、内部 snake_case"

    message: str = Field(..., min_length=1, description="用户当前输入")   # 必填且非空
    session_id: str | None = Field(None, alias="sessionId", description="会话 ID")
    # 第一个参数 None = 默认值（可不传）；alias="sessionId" ⇒ 前端传 sessionId 也能接收；
    # 内部访问仍是 req.session_id（PEP 8 风格）。Java 写作 @JsonProperty("sessionId") String sessionId
    history: list[ChatMessage] = Field(default_factory=list)
    # 默认空列表；必须用 default_factory=list 而不是 = []（后者所有实例会共享同一个可变列表，经典坑）


class ChatResponse(BaseModel):                         # 响应体；由 response_model 声明后自动过滤多余字段
    model_config = ConfigDict(populate_by_name=True)

    session_id: str | None = Field(None, alias="sessionId")   # 可空；序列化时输出 sessionId
    reply: str = Field(..., description="AI 回复全文")         # 必填
    model: str = Field(..., description="实际使用的模型")      # 必填
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
# ---- 本段讲：alias + populate_by_name 实现了"对外 camelCase，对内 snake_case" ----
# 下面是三段用于说明数据形态的片段（JSON 字面量 / 属性访问 / 序列化结果），不是完整程序。
# 前端发 camelCase（符合前端/Java 习惯）
{"message": "hi", "sessionId": "s1"}      # 请求体原样：键名是 sessionId（camelCase），Pydantic 靠 alias 接收

# Python 内部用 snake_case（符合 PEP 8）
req.session_id        # "s1"             # 模型里字段名是 session_id，代码读起来符合 Python 命名规范

# 响应序列化时仍输出 camelCase
{"sessionId": "s1", "reply": "...", "model": "deepseek-chat"}
# ↑ 序列化默认按 alias 输出，所以前端拿到的仍是 sessionId；契约不变，内部风格也不妥协
```

`alias` + `populate_by_name` 一起用，实现了**跨语言契约统一 + Python 内部风格正确**。这个设计值得抄到自己的项目。

---

## 六、Service 层：没有容器怎么"注入"

```python
# app/services/chat_service.py（项目实际代码）
# ---- 本段讲：Service 层 + 没有 IoC 容器时的"依赖注入" ----
# Java：@Service + 构造器注入。Python：普通类 + 模块级单例函数，依赖通过参数传进来。
class ChatService:                              # ≈ @Service public class ChatService
    def __init__(self, llm: LLMClient | None = None):   # 构造器；llm 可选，便于测试注入 Mock
        self.llm = llm or get_llm_client()      # ← 构造注入 + 默认单例
        # "a or b"：a 为 None 时取 b；等价 Java 的 Optional.ofNullable(llm).orElseGet(this::getLlmClient)

    async def reply(self, req: ChatRequest) -> ChatResponse:   # 异步业务方法；≈ public ChatResponse reply(ChatRequest req)
        text = await self.llm.chat(req.history, req.message)   # await 等待 IO，不阻塞事件循环
        return ChatResponse(session_id=req.session_id, reply=text,   # 构造响应 DTO（关键字参数，顺序无关）
                            model=self.llm.settings.llm_model)       # 从客户端配置里取实际模型名


_service: ChatService | None = None             # 模块级变量充当"单例槽位"；下划线前缀表示内部使用

def get_chat_service() -> ChatService:          # 取单例的工厂函数；≈ 容器里 getBean(ChatService.class)
    global _service                             # 声明要改的是模块级变量（否则赋值会创建局部变量）
    if _service is None:                        # 惰性初始化：第一次调用才创建，避免导入时读配置
        _service = ChatService()                # 创建唯一实例
    return _service                             # 之后每次返回同一个对象
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
# ---- 本段讲：Python 里实现"单例 Bean"的三种常见写法 ----
# ① 模块级变量 + 惰性初始化（项目用法）
_service = None                                 # 全局槽位；模块只加载一次，所以天然唯一
def get_chat_service():
    global _service                             # 声明写入模块级变量
    if _service is None:                        # 第一次访问才创建（懒加载）
        _service = ChatService()
    return _service                             # 后续都返回同一对象

# ② lru_cache（config/llm 用的，更简洁）
@lru_cache(maxsize=1)                           # 用缓存装饰器实现单例：参数相同就复用上次结果
def get_llm_client() -> LLMClient:
    return LLMClient()                          # 只会在首次调用时真正执行一次

# ③ 模块级直接实例化（最简单，但导入时就执行）
_service = ChatService()                        # import 本模块的瞬间就建对象，配置没就绪会直接炸
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
# ---- 本段讲：Depends 是 FastAPI 提供的"依赖注入"，但本质是调用一个函数 ----
from fastapi import Depends                    # 依赖声明工具；≈ @Autowired 的显式版本

def get_repo() -> Repo:                        # 提供者函数；≈ @Bean 方法
    return Repo()                              # 每次请求都会调用一次（默认非单例）

@router.get("/orders")                         # GET /orders
async def list_orders(repo: Repo = Depends(get_repo)):
    # 参数默认值 Depends(get_repo) 告诉 FastAPI：调用本接口前先执行 get_repo()，把结果注入 repo。
    # 等价于 Spring 的构造器注入，但这里是"按参数注入"，且没有代理/AOP。
    return repo.all()                          # 使用注入的仓储
```

`Depends` 的价值在**生命周期管理**（每请求一个 DB session）：

```python
# ---- 本段讲：用生成器实现"请求级资源"的创建与释放 ----
# FastAPI 会：创建请求 → 执行到 yield（把 db 注入端点）→ 请求结束回到 yield 之后继续执行。
def get_db():
    db = SessionLocal()             # 前置：建立数据库会话
    try:
        yield db            # yield 之前 = 前置，之后 = 后置（自动清理）
    finally:                        # 放在 finally 里，即使端点抛异常也保证关闭
        db.close()                  # 后置：归还连接

@router.get("/x")
async def x(db = Depends(get_db)):     # 每请求一个新 session，用完自动关
    # 等价于 Spring 的 @Scope("request") + 事务/连接生命周期管理，但没有代理和 AOP
    ...                             # 函数体省略，真实实现用 db 查询
```

这相当于 Spring 的 `@Scope("request")` + `@Transactional` 的组合，但**没有代理、没有 AOP**——就是一个生成器。

---

## 七、外部客户端：`core/llm.py`

```python
# app/core/llm.py（项目实际代码，节选）
# ---- 本段讲：外部 HTTP 客户端封装（大模型调用），对应 Java 的 RestClient/WebClient ----
# 重点：一个 async 客户端 + 两个方法（一次性返回 / 流式返回）。
from openai import AsyncOpenAI                 # OpenAI 官方异步 SDK；也能调任何兼容协议的模型服务

class LLMClient:                               # ≈ @Component public class LlmClient
    def __init__(self, settings: Settings | None = None):   # 允许外部传配置，方便测试
        self.settings = settings or get_settings()          # 没传就用全局配置单例
        self._client = AsyncOpenAI(            # 创建异步 HTTP 客户端（只建一次，复用连接池）
            base_url=self.settings.llm_base_url,            # 服务地址；换成 DeepSeek/本地模型都行
            api_key=self.settings.llm_api_key or "not-configured",   # 没配 key 时给占位符，避免启动报错
            timeout=self.settings.llm_timeout,              # 单次请求超时（秒）
        )

    async def chat(self, history, message) -> str:    # 非流式：等完整回复
        resp = await self._client.chat.completions.create(   # await 发起真实网络请求
            model=self.settings.llm_model,                   # 模型名，如 deepseek-chat
            messages=self._build_messages(history, message), # 把历史+当前消息拼成 OpenAI 格式
            temperature=self.settings.llm_temperature,       # 采样温度，越低越稳定
            max_tokens=self.settings.llm_max_tokens,         # 最大输出长度
        )
        return resp.choices[0].message.content or ""         # 取第一个候选的文本；None 时兜底空串

    async def chat_stream(self, history, message):    # 流式：边生成边返回
        stream = await self._client.chat.completions.create(..., stream=True)
        # ... 表示省略与 chat() 相同的参数；stream=True 让服务端分块下发
        async for chunk in stream:                    # 异步迭代器，每来一小段就处理一次
            if chunk.choices and chunk.choices[0].delta.content:   # 有的块是空心跳，需判空
                yield chunk.choices[0].delta.content               # yield 把增量文本抛给调用方（生成器）
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
# ---- 本段讲：用"继承 + 手写循环"补齐超时/重试/降级，对应 Resilience4j 的 @TimeLimiter/@Retry/@Fallback ----
import asyncio                                       # 提供 wait_for / sleep 等异步原语
from collections.abc import AsyncIterator            # 异步迭代器类型，用于流式方法签名

class ResilientLLMClient(LLMClient):                 # 继承基础客户端，只增强可靠性
    async def chat(self, history, message) -> str:   # 覆盖父类方法
        last_exc = None                              # 记录最后一次异常，便于排查
        for attempt in range(1, self.settings.retry_times + 1):   # 1..N 次尝试
            try:
                return await asyncio.wait_for(       # wait_for 给协程加真实超时
                    super().chat(history, message),  # 调用父类实现
                    timeout=self.settings.llm_timeout,   # 超时秒数
                )
            except (asyncio.TimeoutError, ConnectionError) as e:   # 只重试"超时 + 连接"两类异常
                last_exc = e                         # 暂存异常
                if attempt < self.settings.retry_times:          # 最后一次失败就不再 sleep
                    await asyncio.sleep(0.5 * 2 ** (attempt - 1))   # 指数退避
        # 降级
        return "抱歉，服务暂时不可用，请稍后再试。"   # 全部失败返回兜底文案（≈ @Fallback）

    async def chat_stream(self, history, message) -> AsyncIterator[str]:   # 流式方法
        # 流式不重试（与 Java 侧一致：流不可重放）
        async for d in super().chat_stream(history, message):   # 直接转发父类的每一块
            yield d                                             # 原样抛给上层，不缓存不重放
```

> **注意**：`asyncio.wait_for` 的超时是**真超时**——不像 Java 里 `Mono.fromCallable` 忘了 `subscribeOn` 时计时器要等阻塞返回。因为 await 点是真正的协作让出点。这一点 Python 比 Java 的响应式编程少一个坑。

---

## 八、SSE 流式：对照 `Flux<ServerSentEvent>`

```python
# app/api/chat.py（项目实际代码）
# ---- 本段讲：SSE 流式端点，等价于 WebFlux 返回 Flux<ServerSentEvent<String>> ----
# 结构：外层是普通路由，内层是一个"异步生成器"，每次 yield 一帧 SSE 文本。
@router.post("/chat/stream")                    # POST /api/chat/stream
async def chat_stream(req: ChatRequest) -> StreamingResponse:   # 返回流式响应对象
    async def event_gen():                      # 内层异步生成器：真正的数据源
        try:
            async for delta in chat_service.get_chat_service().stream(req):   # 逐块消费模型输出
                yield f"data: {json.dumps({'delta': delta}, ensure_ascii=False)}\n\n"
                # ↑ 拼成一帧 SSE："data: {JSON}" + 两个换行分帧；
                #   ensure_ascii=False 保留中文，否则会输出 \uXXXX
        except Exception as exc:
            logger.exception("流式对话出错")     # 记录完整堆栈，便于排查
            yield f"data: {json.dumps({'error': str(exc)}, ensure_ascii=False)}\n\n"
            # 出错也要发一帧，让前端知道失败原因（响应头已发出，无法再改状态码）
        yield "data: [DONE]\n\n"                # 结束标记；无论成功失败都必须发

    return StreamingResponse(                   # 把生成器包成 HTTP 流式响应
        event_gen(),                            # 传入异步生成器（不是调用结果）
        media_type="text/event-stream",         # MIME 必须是它，浏览器才会按 SSE 解析
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},   # 禁缓存 + 禁 Nginx 缓冲
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
# ---- 本段讲：这两个响应头为何必须加，否则"流式"会退化成"一次性返回" ----
# Cache-Control: no-cache 让浏览器/代理不缓存这片数据；X-Accel-Buffering: no 让 Nginx 不要缓冲。
# 少了后者，Nginx 会攒满一个 buffer 才转发，前端看起来就像"流式失效"。
headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"}
# ↑ 响应头：Cache-Control 禁浏览器缓存；X-Accel-Buffering 禁 Nginx 缓冲，否则会攒满 buffer 才转发
```

**异常也要发一帧，然后正常收尾**

```python
# ---- 本段讲：流式端点的收尾约定——异常也要发一帧，且必须补上 [DONE] ----
# 对照 Java 的 Flux：出错时把错误也作为一条 SSE 事件推给前端，再 concatWith 一个 [DONE] 结束事件。
except Exception as exc:                                     # 捕获生成过程中的任何异常
    yield f"data: {json.dumps({'error': str(exc)})}\n\n"     # 把错误信息作为一帧发给前端
yield "data: [DONE]\n\n"     # ← 无论成功失败都要发；少这一帧前端会一直转圈
```

否则前端会一直等 `[DONE]`，界面永远停在"加载中"。

### 对比 Java 侧的项目经验

你的项目记忆里有一条：**流式不配 Retry（Flux 不可重放）**。Python 里同理——`AsyncIterator` 也**只能遍历一次**，重试必须重新发起请求，不能在同一个生成器上重来。

---

## 九、异常处理：`@app.exception_handler`

```python
# ---- 本段讲：全局异常处理，等价 @RestControllerAdvice + @ExceptionHandler ----
# 思路：自定义异常携带业务码，注册一个处理器把它翻译成统一 JSON 结构。
from fastapi import FastAPI, Request                 # Request 用于读取请求信息（如 URL、头）
from fastapi.responses import JSONResponse           # 需要自定义状态码/响应体时使用

app = FastAPI()                                      # 本段独立演示用的小应用

class AppException(Exception):                       # 自定义业务异常；≈ class BusinessException extends RuntimeException
    def __init__(self, code: str, message: str):     # 构造器：保存业务码和提示语
        self.code = code                             # 业务码，如 "INSUFFICIENT_STOCK"
        self.message = message                       # 给用户看的提示


@app.exception_handler(AppException)                 # 注册该异常类型的处理器；≈ @ExceptionHandler(BusinessException.class)
async def handle_app(request: Request, exc: AppException):   # 第一个参数是请求，第二个是异常实例
    return JSONResponse(status_code=200,             # 业务异常也返回 200，由 code 字段表达成败
                        content={"code": exc.code, "message": exc.message})   # 统一响应结构


@app.exception_handler(Exception)          # 兜底
async def handle_all(request: Request, exc: Exception):      # 捕获所有未处理异常
    logger.exception("未捕获异常 path=%s", request.url.path)   # 打印堆栈 + 路径；%s 是占位符（惰性格式化）
    return JSONResponse(status_code=500, content={"code": "INTERNAL", "message": "系统繁忙"})
    # 兜底统一 500，避免把内部堆栈泄露给前端
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
# ---- 本段讲：HTTP 中间件，等价 Servlet Filter / HandlerInterceptor ----
# 用途：统一注入 traceId、统计耗时，属于典型的横切关注点。
import time, uuid                              # time 用于计时；uuid 生成唯一请求 ID
from fastapi import Request                    # 请求对象类型注解

@app.middleware("http")                        # 注册 HTTP 中间件；≈ 实现 Filter 并注册到链上
async def add_trace_id(request: Request, call_next):
    # call_next 是"调用后续链路"的函数，返回响应；必须先 await 它，再加工 response。
    trace_id = request.headers.get("X-Trace-Id") or str(uuid.uuid4())
    # 优先沿用上游传的 traceId，没有就生成一个；or 在这里兼作空值兜底
    start = time.perf_counter()                # 高精度计时起点；≈ System.nanoTime()
    response = await call_next(request)        # 放行到下游处理，拿到最终响应
    response.headers["X-Trace-Id"] = trace_id  # 回写响应头，方便前端/网关串联日志
    response.headers["X-Cost-Ms"] = f"{(time.perf_counter()-start)*1000:.1f}"
    # ↑ 计算耗时（毫秒，保留 1 位小数），f-string 格式说明符 ≈ String.format("%.1f", ...)
    return response                            # 返回加工后的响应
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
# ---- 本段讲：FastAPI 接口测试全套写法，对照 @SpringBootTest + MockMvc ----
# 核心技巧：把 LLM 换成假实现（monkeypatch），测试不花钱、不联网、毫秒级启动。
import pytest                                   # 测试框架；≈ JUnit 5
from httpx import ASGITransport, AsyncClient    # 异步 HTTP 客户端 + 直接对接 ASGI 应用（不用起端口）

from app.main import create_app                 # 用工厂函数创建被测应用
from app.services import chat_service           # 用于替换其中的单例


@pytest.fixture                                # 定义夹具；≈ @BeforeEach 提供的测试上下文
async def client(monkeypatch):                 # monkeypatch 是 pytest 内置补丁工具；≈ @MockBean
    # 把 LLM 客户端替换掉，测试不花钱、不依赖网络
    class FakeLLM:                             # 假客户端：只需实现被调用的方法（鸭子类型）
        settings = type("S", (), {"llm_model": "fake"})()
        # 动态造一个只有 llm_model 属性的对象；≈ new Settings() 再 setLlmModel("fake")

        async def chat(self, history, message):        # 假的非流式调用
            return f"echo: {message}"                  # 原样回显，便于断言

        async def chat_stream(self, history, message): # 假的流式调用
            for ch in ["你", "好"]:                    # 固定吐两个字，测试可预期
                yield ch                               # 生成器逐个产出

    monkeypatch.setattr(chat_service, "_service", chat_service.ChatService(llm=FakeLLM()))
    # ↑ 直接把模块里的单例替换成注入了 FakeLLM 的实例；测试结束自动还原

    app = create_app()                         # 创建独立应用实例（不污染全局 app）
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as c:
        # ASGITransport 让 httpx 直接调用 app，无需 uvicorn；base_url 随便填
        yield c                                # 把 client 交给测试函数使用


@pytest.mark.asyncio                           # 标记为异步测试（需 pytest-asyncio 插件）
async def test_health(client):                 # 参数名 client 对应上面的夹具名
    r = await client.get("/api/health")        # 发起 GET；≈ mockMvc.perform(get("/api/health"))
    assert r.status_code == 200                # 裸 assert；pytest 会给出详细失败信息
    assert r.json() == {"status": "up"}        # 解析响应 JSON 并整体比较


@pytest.mark.asyncio
async def test_chat(client):
    r = await client.post("/api/chat", json={"message": "hi"})   # json= 自动设置 Content-Type 并序列化
    assert r.status_code == 200
    assert r.json()["reply"] == "echo: hi"                       # 验证 FakeLLM 的回显


@pytest.mark.asyncio
async def test_validation_422(client):
    r = await client.post("/api/chat", json={"message": ""})    # 违反 min_length=1
    assert r.status_code == 422                # 校验失败由 Pydantic 自动返回，无需自己写断言逻辑


@pytest.mark.asyncio
async def test_stream(client):
    async with client.stream("POST", "/api/chat/stream", json={"message": "hi"}) as r:
        # stream() 以流式方式读取响应，不一次性载入内存
        assert r.headers["content-type"].startswith("text/event-stream")   # 校验 SSE 响应头
        body = ""                              # 累积完整响应体
        async for chunk in r.aiter_text():     # 异步逐块读取文本
            body += chunk                      # 拼接
    assert "data: [DONE]" in body              # 确认结束帧存在，流正常收尾
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
# ---- 本段讲：配置类，等价 @ConfigurationProperties + application.yml ----
# 机制：环境变量 LLM_BASE_URL 会自动映射到字段 llm_base_url（大小写不敏感、下划线对应）。
from functools import lru_cache                     # 缓存装饰器，让配置对象只创建一次
from pydantic_settings import BaseSettings, SettingsConfigDict   # 配置基类与配置项


class Settings(BaseSettings):                       # 继承 BaseSettings ⇒ 自动从环境变量/.env 读取；≈ @ConfigurationProperties
    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
        # env_file 指定本地配置文件；extra="ignore" 表示忽略多余的环境变量，不报错
    )

    llm_base_url: str = "https://api.deepseek.com"  # 类型 + 默认值；环境变量会按名覆盖
    llm_api_key: str = ""                           # 密钥默认为空（真实值放 .env，绝不进代码库）
    llm_model: str = "deepseek-chat"                # 默认模型
    llm_temperature: float = 0.7                    # 采样温度
    llm_max_tokens: int = 2048                      # 最大输出 token 数
    llm_timeout: float = 60.0                       # 请求超时秒数

    system_prompt: str = "你是一个乐于助人的 AI 助手,请用简洁的中文回答。"   # 系统提示词

    app_host: str = "0.0.0.0"                       # 监听地址
    app_port: int = 8000                            # 监听端口
    cors_origins: str = "*"                         # 允许的跨域来源，逗号分隔


@lru_cache                                          # 无参函数 + 缓存 = 单例；≈ 容器里的配置 Bean
def get_settings() -> Settings:
    return Settings()                               # 只在第一次调用时真正解析环境变量
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
