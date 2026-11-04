# 18 · 测试进阶：pytest 与可测性设计

> 会写 `assert` 只是开始。真正有价值的是：**测试快、稳定、能定位问题，并且不依赖真实 LLM/网络/数据库**。
> 本篇以 `ai-cs-py-chat/tests/test_chat_api.py` 的 FakeLLM 策略为基础，补齐工程测试能力。

---

## 一、测试分层：不要所有测试都起完整服务

| 层级 | 测什么 | 依赖 | 速度 | 占比建议 |
|---|---|---|---|---|
| 单元测试 | 金额、分组、状态机、重试/熔断 | 无 | 毫秒 | 多 |
| 组件测试 | Repository + 临时 SQLite、Service + Fake LLM | 本地临时资源 | 毫秒~秒 | 中 |
| 接口测试 | FastAPI + TestClient | 不起真实端口 | 秒级 | 中 |
| 端到端 | 真网关、真 DB、真下游 | 完整环境 | 慢、易波动 | 少 |

**优先在最低层抓住 bug**。能用纯函数测试验证的规则，不要写成 HTTP 测试。

---

## 二、可测性首先来自代码结构

### 反例：业务逻辑和外部依赖写死

```python
# ---- 本段讲：业务逻辑直接创建外部依赖为何难以测试 ----
# 这个反例在业务函数内部连接生产数据库并构造真实大模型客户端，使单元测试无法隔离外部资源。
# Java 中类似于在 Service 方法里直接 new JDBC Connection 和 OpenAIClient，而不是由 Spring 构造器注入。
# 这种写法迫使测试访问真实环境，也无法方便地用 Mockito 替换依赖。

# 定义异步下单函数；async def 类似 Java 返回 CompletableFuture 的异步方法，但调用时需要 await。
async def create_order(req):
    # 直接连接固定的 prod.db；测试无法替换数据库，类似 Java 业务代码硬编码 DriverManager.getConnection。
    conn = sqlite3.connect("prod.db")
    # 直接从环境变量取密钥并创建真实客户端；类似 Java 方法内部 new SDK Client，Mockito 无法注入 mock。
    llm = AsyncOpenAI(api_key=os.environ["KEY"])
    # 查询、计算、调用全揉在一起
```

### 改进：依赖从构造函数传入

```python
# ---- 本段讲：用构造函数依赖注入提升可测性 ----
# OrderService 只声明自己需要仓库和大模型客户端，不负责创建具体实现，从而可在测试中替换依赖。
# 这与 Java 的构造器注入一致，也对应 Spring 推荐的 final 字段构造器注入方式。
# 测试可传入 Fake 或 Mockito mock，生产环境则由装配层传入真实实现。

# 定义订单服务类；类似 Java 中承载业务用例的 @Service 类。
class OrderService:
    # 构造函数接收两个抽象依赖；类型标注类似 Java 构造器参数类型，但 Python 运行时默认不强制检查。
    def __init__(self, repo: OrderRepository, llm: LlmClient):
        # 保存仓库依赖供业务方法使用；对应 Java 的 this.repo = repo。
        self.repo = repo
        # 保存 LLM 依赖而非在方法内 new；测试时因此能注入 FakeLLM 或 AsyncMock。
        self.llm = llm

    # 定义异步创建订单用例，并标注输入、返回类型；对应 Java Service 的异步业务方法。
    async def create(self, command: CreateOrder) -> Order:
        # 省略实际业务实现；省略号是 Python 的 Ellipsis 对象，在示例中充当占位符。
        ...
```

生产环境传真实实现，测试传 Fake：

```python
# ---- 本段讲：测试中注入内存仓库和 Fake LLM ----
# 这里在装配 Service 时选择无外部副作用的测试替身，因此测试不需要真实数据库、网络或密钥。
# Java 中相当于手动 new Service(new InMemoryRepository(), new FakeLlmClient())，或在 Spring 测试配置中提供测试 Bean。
# Fake 具有可工作的简化实现；它与 Mockito mock 的“按调用配置行为”用途不同。

# 使用关键字参数创建服务，明确 repo 与 llm 各自接收哪个替身；类似 Java 构造器注入测试双对象。
service = OrderService(repo=MemoryRepo(), llm=FakeLLM())
```

这就是本项目 `ChatService(llm=...)` 的可测性来源。**不是为了抽接口而抽接口，而是为了隔离变化与外部依赖**。

---

## 三、fixture：准备环境，而不是复制粘贴

```python
# ---- 本段讲：用 pytest fixture 复用测试准备并自动解析依赖 ----
# fixture 把测试数据和对象装配从测试方法中抽离，每个用例按参数名声明自己需要什么。
# pytest 会按依赖关系先创建 order 再创建 service，类似 JUnit 5 的 @BeforeEach，但可组合且能返回值。
# Java 可将其理解为“带依赖图的测试工厂”；Spring 测试的 Bean 注入也有相似的按类型或名称装配体验。

# 导入 pytest，后续使用其 fixture 装饰器注册测试资源提供函数。
import pytest

# 将下面的 order 函数注册为 fixture；默认 scope=function，即每个测试方法重新创建一次。
@pytest.fixture
# 定义名为 order 的 fixture；测试函数写入同名参数即可请求它，无需手动调用。
def order():
    # 返回一份订单字典作为固定测试数据；类似 JUnit @BeforeEach 中构造测试对象后赋给字段。
    return {"order_no": "NO001", "amount": "100.00"}

# 注册 service fixture；它可以继续依赖其他 fixture，pytest 会自动按顺序解析。
@pytest.fixture
# 参数 order 不是普通调用方传值，而是请求上面名为 order 的 fixture。
def service(order):
    # 用 order 初始化内存仓库并注入 FakeLLM；每个测试获得隔离的 Service 实例。
    return OrderService(repo=MemoryRepo(seed=[order]), llm=FakeLLM())
```

### fixture 作用域

```python
# ---- 本段讲：fixture scope 控制资源实例的生命周期 ----
# scope 决定 fixture 结果被缓存和复用多久；范围越大，创建成本越低，但共享状态导致测试互相污染的风险越高。
# function 对应 JUnit 5 默认的每方法测试实例与 @BeforeEach，module/session 更接近 @BeforeAll 或全局测试容器。
# 这三行是作用域写法示意，实际使用时每个装饰器应分别放在对应 fixture 函数上。

# function 是默认作用域，每个测试函数各创建一份，隔离性最好；类似 JUnit 5 @BeforeEach。
@pytest.fixture(scope="function")   # 默认，每测试一个新实例
# module 让同一测试文件只创建一次，适合只读且构建较贵的资源；近似该类中的 @BeforeAll。
@pytest.fixture(scope="module")     # 每个文件一次
# session 在整个 pytest 进程中只创建一次；类似测试套件级单例，不应承载跨用例可变状态。
@pytest.fixture(scope="session")    # 整个测试进程一次
```

规则：

- 默认 `function`，隔离最好
- `session` 只放只读、昂贵、无状态资源
- 可变数据库/连接不要共享 session fixture
- fixture 有清理逻辑就用 `yield`

```python
# ---- 本段讲：用 yield fixture 成对执行资源准备与清理 ----
# yield 之前是 setup，yield 交出的对象供测试使用，测试结束后无论成功或异常都会继续执行 finally 清理。
# 这相当于 JUnit 5 的 @BeforeEach 创建资源与 @AfterEach 关闭资源，但准备和清理写在同一个 fixture 中不易遗漏。
# tmp_path 也是 pytest 内置 fixture，每个测试获得独立临时目录，避免并行测试争用同一数据库文件。

# 注册默认 function 作用域的仓库 fixture，确保每个测试独立创建并关闭仓库。
@pytest.fixture
# 通过参数名请求 pytest 内置 tmp_path；类似 JUnit 的 @TempDir 注入 Path。
def repo(tmp_path):
    # 在当前测试专属临时目录创建 SQLite 仓库，避免连接生产库或污染其他用例。
    repo = SqliteRepo(tmp_path / "test.db")
    # try/finally 保证测试主体即使抛异常，清理逻辑仍会执行。
    try:
        # 暂停 fixture 并把仓库交给测试；不同于 return，yield 后面的代码会在测试结束时恢复执行。
        yield repo
    # finally 对应稳定的 teardown 阶段，语义类似 JUnit 5 @AfterEach。
    finally:
        # 关闭数据库连接并释放文件句柄，避免 Windows 上临时文件被占用。
        repo.close()
```

---

## 四、参数化：一组规则，多组数据

```python
# ---- 本段讲：用 parametrize 让同一规则覆盖多组输入与期望值 ----
# 装饰器会为参数表中的每个元组独立生成一次测试执行，某一组失败不会阻止 pytest 报告其他组结果。
# 对应 JUnit 5 的 @ParameterizedTest 配合 @CsvSource 或 @MethodSource；raw、expected 类似参数化测试的方法形参。
# 表驱动写法减少重复测试代码，同时清楚覆盖正常、格式化、空值、非法值和 None 边界。

# 声明两个参数名，并提供六组“原始输入、期望整数”；每个元组对应一个独立测试用例。
@pytest.mark.parametrize("raw,expected", [
    # 普通数字字符串应直接转换为整数 100。
    ("100", 100),
    # 带千位分隔符的字符串应移除逗号后得到 1234。
    ("1,234", 1234),
    # 带首尾空格的字符串应清理空白后得到 42。
    (" 42 ", 42),
    # 空字符串按业务约定安全降级为 0，而不是抛 ValueError。
    ("", 0),
    # 非数字字符串同样验证容错分支返回 0。
    ("abc", 0),
    # None 覆盖缺失值边界，对应 Java 测试中传入 null。
    (None, 0),
])
# pytest 把当前参数组按名称注入测试函数；类似 JUnit 参数化测试逐次调用测试方法。
def test_safe_int(raw, expected):
    # 断言转换结果与当前组期望值一致；类似 JUnit Assertions.assertEquals(expected, safeInt(raw))。
    assert safe_int(raw) == expected
```

参数化时给每组数据清晰含义；失败时 pytest 会显示具体参数。**不要把多个不相关断言塞进一个测试**，否则第一处失败会遮住后面的错误。

---

## 五、monkeypatch 与 mock 怎么选

### `monkeypatch`：替换模块级依赖

```python
# ---- 本段讲：用 monkeypatch 临时替换被测模块查找的依赖 ----
# monkeypatch fixture 会在当前测试结束后自动恢复原属性，避免替换泄漏到其他用例。
# 这里替换服务工厂，使接口代码拿到 fake_service；Java 中近似 Mockito.mockStatic，或 Spring 测试中的 @MockBean 替换 Bean。
# 应替换“被测代码实际查找该名称的位置”，而不是盲目替换依赖最初定义的位置。

# client 和 monkeypatch 都由 pytest 按参数名注入；前者调用接口，后者负责安全地修改并恢复属性。
def test_health_uses_fake_service(client, monkeypatch):
    # 将 chat_service 模块的工厂函数替换为返回 fake_service 的 lambda，阻止测试创建真实服务。
    monkeypatch.setattr(chat_service, "get_chat_service", lambda: fake_service)
    # 省略使用 client 发请求并断言响应的代码；测试结束时 monkeypatch 自动撤销替换。
    ...
```

适合：环境变量、当前时间、模块函数、单例工厂。

### `unittest.mock`：检查调用行为

```python
# ---- 本段讲：用 AsyncMock 模拟异步依赖并验证 await 调用 ----
# AsyncMock 专门模拟 async 函数：调用后返回可等待对象，await 时再产生配置好的结果。
# Java 中可对照 Mockito.mock 配合 CompletableFuture.completedFuture，但 AsyncMock 还能直接断言是否被 await。
# 测试同时检查业务结果所依赖的返回值，以及 LLM 调用次数和关键参数是否正确。

# 从标准库 mock 模块导入异步 Mock；普通 Mock 不能正确表达协程被 await 的行为。
from unittest.mock import AsyncMock

# 创建异步替身；其方法会自动形成可记录调用与 await 信息的 AsyncMock。
llm = AsyncMock()
# 指定 await llm.chat(...) 的返回值，前后空格可用于验证业务层是否执行清理逻辑。
llm.chat.return_value = " mocked "
# 把替身注入服务，避免测试访问真实 LLM；对应 Java 构造器注入 Mockito mock。
service = ChatService(llm=llm)

# 调用并等待被测异步方法；此时会触发上面配置的 llm.chat 返回值。
result = await service.reply(request)

# 断言 chat 恰好被 await 一次；比 assert_called_once 更准确地约束异步调用已真正执行。
llm.chat.assert_awaited_once()
# await_args 保存最近一次 await 的参数；args[1] 校验第二个位置参数确实是用户文本。
assert llm.chat.await_args.args[1] == "你好"
```

| 需求 | 工具 |
|---|---|
| 替换一个函数/变量 | `monkeypatch.setattr` |
| 验证调用了几次、参数是什么 | `Mock` / `AsyncMock` |
| 模拟返回值 | `return_value` |
| 模拟抛异常 | `side_effect = TimeoutError()` |
| 连续多次不同结果 | `side_effect = [exc1, value2]` |

**不要过度验证内部调用顺序**。测试应尽量断言“外部可观察行为”，否则重构内部实现就会导致大量测试无意义失败。

---

## 六、时间、随机与外部网络必须可控

### 时间

不要依赖“等 100ms 后应该成功”这种脆弱测试。把时间抽象成可注入函数，或 monkeypatch：

```python
# ---- 本段讲：用 monkeypatch 固定并推进单调时间来测试熔断器 ----
# 测试把 time.monotonic 替换为可控时钟，无需真实 sleep 31 秒，因此执行快速且不会因机器调度而抖动。
# Java 中类似注入 java.time.Clock 并在测试里推进固定 Clock；若代码写死静态时间，也可用 Mockito 静态 mock。
# 字典保存可变时间值，使 lambda 每次调用都能读取推进后的最新值。

# pytest 按参数名注入 monkeypatch，并在测试结束后自动恢复真实 time.monotonic。
def test_breaker_opens(monkeypatch):
    # 设置虚拟单调时钟的初始秒数；单调时间只关心经历时长，不受系统时钟回拨影响。
    now = {"value": 100.0}
    # 替换被测代码读取的时间函数；每次调用都返回当前字典中的虚拟时间。
    monkeypatch.setattr(time, "monotonic", lambda: now["value"])
    # 此处省略触发连续失败并断言熔断器打开的步骤。
    ...
    # 直接推进 31 秒以跨过熔断恢复窗口，不产生真实等待。
    now["value"] += 31
```

### 随机

```python
# ---- 本段讲：固定随机种子以复现伪随机测试结果 ----
# 相同算法和调用顺序下，固定 seed 会生成相同伪随机序列，使失败能够稳定复现。
# Java 中对应测试前调用 new Random(42) 或把带固定种子的 Random 注入被测对象。
# 这只适合控制测试内的非安全随机行为；生产密钥或令牌不能依赖可预测种子。

# 将模块级伪随机生成器重置到确定状态；42 是任意固定值，不代表期望随机结果本身。
random.seed(42)          # 仅适合测试内可控的伪随机
```

更好的方式是把随机函数作为参数传入。

### 网络

单元/组件测试**不访问真实网络**：

- LLM：FakeLLM
- HTTP：注入 fake fetcher / transport
- SMTP：Fake sender
- 支付：Mock channel

需要验证真实 HTTP 契约时，单独放集成测试，并用标记隔离：

```python
# ---- 本段讲：用 integration marker 标记并隔离真实集成测试 ----
# integration 标记把会访问真实提供方的慢测试与默认快速测试分组，便于通过 pytest -m 选择或排除。
# Java 中类似 JUnit 5 的 @Tag("integration")，再由 Maven/Gradle 按标签筛选测试。
# marker 只负责分类，不会自动 mock 网络；运行此用例仍需显式准备凭据、网络和测试环境。

# 给下面的测试附加 integration 元数据；通常还应在 pytest 配置中注册该 marker，避免拼写错误。
@pytest.mark.integration
# 测试命名说明它验证真实 provider 契约，而不是隔离的单元逻辑。
def test_real_provider():
    # 省略调用真实服务并校验响应契约的实现。
    ...
```

```bash
pytest -m "not integration"
```

---

## 七、异步测试配置

安装：

```bash
python -m pip install pytest pytest-asyncio
```

`pytest.ini`：

```ini
[pytest]
asyncio_mode = auto
testpaths = 06_exercises 09_webdb 10_ai
addopts = -q
```

测试：

```python
# ---- 本段讲：用 pytest-asyncio 在事件循环中执行异步流测试 ----
# pytest-asyncio 识别 asyncio marker，为 async def 测试提供并管理事件循环，测试体内可直接使用 await 与 async for。
# Java 中可类比 JUnit 测试等待 CompletableFuture，或用 Reactor StepVerifier 驱动并断言异步数据流。
# 本例收集异步生成器的全部分片，再验证拼接后的外部可观察内容。

# 标记该测试由 pytest-asyncio 运行；缺少插件时 pytest 无法原生执行 async def 测试。
@pytest.mark.asyncio
# 异步测试函数会在插件管理的事件循环中执行。
async def test_stream():
    # async for 逐个等待流式分片，并用异步列表推导式收集结果；类似消费 Java Publisher 的全部元素。
    chunks = [c async for c in service.stream(req)]
    # 将分片按到达顺序拼接，断言最终文本内容而不是内部发送次数。
    assert "".join(chunks) == "你好"
```

也可以不用插件，在同步测试里 `asyncio.run(...)`，但**同一个测试里不能嵌套已有事件循环**。团队统一一种方式即可。

> 上次题库 4 个异步用例失败，不是业务代码错，而是环境缺 `pytest-asyncio`。看到 `async def functions are not natively supported`，先查插件与配置。

---

## 八、FastAPI 接口测试

```python
# ---- 本段讲：用 FastAPI TestClient 在进程内验证接口与 lifespan ----
# TestClient 不启动真实监听端口，而是把 HTTP 风格请求直接交给 ASGI 应用，适合验证路由、校验和序列化。
# Java 中对应 Spring MockMvc：都能在测试进程内发送请求并断言状态码和 JSON，无需部署服务器。
# 临时数据库隔离持久化状态，with 上下文则确保应用启动与关闭生命周期完整执行。

# 导入 FastAPI/Starlette 提供的同步测试客户端，测试代码无需手动管理异步事件循环。
from fastapi.testclient import TestClient

# tmp_path 是 pytest 内置 fixture，为该测试提供独立的临时目录。
def test_create_note(tmp_path):
    # 将临时数据库路径传给应用工厂，避免读写开发或生产数据库。
    app = create_app(db_path=tmp_path / "notes.db")
    # 上下文进入和退出会触发应用 lifespan，相当于 MockMvc 测试环境的初始化与资源清理。
    with TestClient(app) as client:
        # 以 JSON 请求体调用创建接口；TestClient 会完成编码并设置相应 Content-Type。
        resp = client.post("/notes", json={"text": "第一条"})
        # 断言 HTTP 201，确认资源创建语义而不只是请求未抛异常。
        assert resp.status_code == 201
        # 解析 JSON 响应并验证新记录的标识。
        assert resp.json()["id"] == 1

        # 使用同一应用和数据库调用查询接口，验证创建结果已持久化并可读取。
        resp = client.get("/notes")
        # 提取每条记录的 text 字段，断言列表内容和顺序符合接口契约。
        assert [n["text"] for n in resp.json()] == ["第一条"]
```

要点：

- `with TestClient(app)` 会触发 lifespan，负责资源关闭
- 用 `tmp_path` 隔离数据库文件
- 测试校验 422、404、业务错误码，不只测 happy path
- SSE 要断言分帧格式、增量内容和 `[DONE]`
- 文件/网络/数据库不要指向生产配置

---

## 九、覆盖率检查与门禁

### 运行覆盖率测试

```bash
python -m pip install pytest-cov
pytest tests/ --cov=app --cov-report=html --cov-report=term-missing

# 输出：
# app/services/order.py:42  100%
# app/services/order.py:45   50%  # 缺失这一行的分支
```

看 `Missing` 行号，重点补：
- 异常分支
- 参数为空/边界值
- 重试、超时、熔断恢复
- 事务回滚
- 并发冲突

**100% 覆盖不等于没有 bug**。一行代码被执行过，不代表结果被正确断言，也不代表并发与外部契约正确。

### JaCoCo 同等标准：门禁配置

在 `pyproject.toml` 中：

```toml
[tool.pytest.ini_options]
addopts = “--cov=app --cov-fail-under=60 --cov-report=term-missing”
```

**与 Java 对标：**

| Java | Python |
|------|--------|
| Maven Surefire 收集覆盖率 | pytest + pytest-cov |
| JaCoCo 门禁 `<limit>` | `--cov-fail-under` |
| IDE 显示绿/红行 | `--cov-report=html` + 浏览器 |

---

## 十、性能测试集成

### pytest-benchmark

```bash
pip install pytest-benchmark
```

```python
# ---- 本段讲：用 pytest-benchmark 重复测量代码并识别性能回归 ----
# benchmark fixture 会预热并多轮调用目标函数，统计耗时分布、吞吐量和离群值，而不是只测一次墙钟时间。
# Java 中对应 JMH 的 @Benchmark；两者都处理重复执行与统计，但仍应在稳定环境中比较基线。
# 被测函数保持很小，只包住推荐查询本身，避免把 fixture 准备时间误计入结果。

# benchmark 由 pytest-benchmark 插件按参数名注入，测试名称会出现在基准报告中。
def test_recommendations_performance(benchmark):
    “””
    性能回归测试（确保不会无故变慢）
    
    对标 Java: @Benchmark (JMH)
    “””
    # 定义一次基准操作；插件将重复调用它以自动选择轮数并形成统计样本。
    def recommendation():
        # 固定 user_id 和 limit，避免输入变化干扰不同版本之间的耗时比较。
        return service.get_recommendations(user_id=1, limit=5)
    
    # 把可调用对象交给 benchmark 执行；返回值是其中一次基准调用的业务结果，可继续做正确性断言。
    result = benchmark(recommendation)
    # pytest-benchmark 自动计算统计、检测回归
```

运行：

```bash
pytest tests/test_perf.py --benchmark-only
```

输出：

```
test_recommendations_performance:
  Name (time in ms): 15.45 ± 2.34
  Median: 14.89 ms
  IQR: 2.10 ms
  Outliers: 1 (1.2%)
  OPS/sec: 64.73
  Rounds: 100
  Iterations: 1
```

---

## 十一、CI 集成与缺陷检测

### GitHub Actions 测试工作流

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: [“3.11”, “3.12”]
    
    steps:
      - uses: actions/checkout@v3
      
      - uses: actions/setup-python@v4
        with:
          python-version: ${{ matrix.python-version }}
      
      - name: Install dependencies
        run: |
          pip install -e “.[dev]”
      
      - name: Run tests
        run: |
          pytest tests/ \
            --cov=app \
            --cov-fail-under=60 \
            --cov-report=xml \
            --junitxml=test-results.xml
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./coverage.xml
```

### 失败时自动打印详细日志

```bash
pytest -vv --tb=long --capture=no
```

---

## 十二、稳定测试的十条纪律

1. 不访问互联网
2. 不依赖执行顺序
3. 不共享可变的全局状态
4. 不依赖真实当前时间
5. 不写无必要的 `sleep`
6. 不只断言”没抛异常”
7. 不用生产数据库和生产密钥
8. 临时文件用 `tmp_path`
9. 失败信息能看出输入、输出、期望
10. 每修一个 bug，优先补最小回归测试

---

## 十三、一个接口测试的最小清单

```text
✓ 正常输入返回正确数据
✓ 缺字段 / 类型错 / 长度不合法 -> 422
✓ 资源不存在 -> 404 或业务码
✓ 重复请求 -> 幂等或冲突行为明确
✓ 下游超时 -> 有超时、重试、降级
✓ 数据库异常 -> 回滚，不留半条数据
✓ 流式响应 -> 分帧、异常帧、结束帧正确
✓ 多并发 -> 不超卖、不重复扣款
```

---

## 十四、实战练习：为 ai-cs-py-recommend 补测试

```bash
cd ai-cs-py-recommend

# 1. 运行现有测试
pytest tests/ -v

# 2. 生成覆盖率报告
pytest tests/ --cov=app --cov-report=html

# 3. 打开报告
open htmlcov/index.html

# 4. 在红线处补充测试
# 重复直到覆盖率 ≥ 60%
```

**检查清单：**
- [ ] 所有 API 端点都有 happy path + error path 测试
- [ ] Service 层的所有公共方法都有单元测试
- [ ] 参数校验（422）都测过
- [ ] 缓存命中/未命中都测过
- [ ] 并发场景测试通过
- [ ] 覆盖率 ≥ 60%

---

**上一篇**：[17-性能优化与基准测试](17-性能优化与基准测试.md)　**下一篇**：[19-HTTP与数据库实战](19-HTTP与数据库实战.md)
