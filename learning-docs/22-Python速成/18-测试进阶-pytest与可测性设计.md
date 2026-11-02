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
async def create_order(req):
    conn = sqlite3.connect("prod.db")
    llm = AsyncOpenAI(api_key=os.environ["KEY"])
    # 查询、计算、调用全揉在一起
```

### 改进：依赖从构造函数传入

```python
class OrderService:
    def __init__(self, repo: OrderRepository, llm: LlmClient):
        self.repo = repo
        self.llm = llm

    async def create(self, command: CreateOrder) -> Order:
        ...
```

生产环境传真实实现，测试传 Fake：

```python
service = OrderService(repo=MemoryRepo(), llm=FakeLLM())
```

这就是本项目 `ChatService(llm=...)` 的可测性来源。**不是为了抽接口而抽接口，而是为了隔离变化与外部依赖**。

---

## 三、fixture：准备环境，而不是复制粘贴

```python
import pytest

@pytest.fixture
def order():
    return {"order_no": "NO001", "amount": "100.00"}

@pytest.fixture
def service(order):
    return OrderService(repo=MemoryRepo(seed=[order]), llm=FakeLLM())
```

### fixture 作用域

```python
@pytest.fixture(scope="function")   # 默认，每测试一个新实例
@pytest.fixture(scope="module")     # 每个文件一次
@pytest.fixture(scope="session")    # 整个测试进程一次
```

规则：

- 默认 `function`，隔离最好
- `session` 只放只读、昂贵、无状态资源
- 可变数据库/连接不要共享 session fixture
- fixture 有清理逻辑就用 `yield`

```python
@pytest.fixture
def repo(tmp_path):
    repo = SqliteRepo(tmp_path / "test.db")
    try:
        yield repo
    finally:
        repo.close()
```

---

## 四、参数化：一组规则，多组数据

```python
@pytest.mark.parametrize("raw,expected", [
    ("100", 100),
    ("1,234", 1234),
    (" 42 ", 42),
    ("", 0),
    ("abc", 0),
    (None, 0),
])
def test_safe_int(raw, expected):
    assert safe_int(raw) == expected
```

参数化时给每组数据清晰含义；失败时 pytest 会显示具体参数。**不要把多个不相关断言塞进一个测试**，否则第一处失败会遮住后面的错误。

---

## 五、monkeypatch 与 mock 怎么选

### `monkeypatch`：替换模块级依赖

```python
def test_health_uses_fake_service(client, monkeypatch):
    monkeypatch.setattr(chat_service, "get_chat_service", lambda: fake_service)
    ...
```

适合：环境变量、当前时间、模块函数、单例工厂。

### `unittest.mock`：检查调用行为

```python
from unittest.mock import AsyncMock

llm = AsyncMock()
llm.chat.return_value = " mocked "
service = ChatService(llm=llm)

result = await service.reply(request)

llm.chat.assert_awaited_once()
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
def test_breaker_opens(monkeypatch):
    now = {"value": 100.0}
    monkeypatch.setattr(time, "monotonic", lambda: now["value"])
    ...
    now["value"] += 31
```

### 随机

```python
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
@pytest.mark.integration
def test_real_provider():
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
@pytest.mark.asyncio
async def test_stream():
    chunks = [c async for c in service.stream(req)]
    assert "".join(chunks) == "你好"
```

也可以不用插件，在同步测试里 `asyncio.run(...)`，但**同一个测试里不能嵌套已有事件循环**。团队统一一种方式即可。

> 上次题库 4 个异步用例失败，不是业务代码错，而是环境缺 `pytest-asyncio`。看到 `async def functions are not natively supported`，先查插件与配置。

---

## 八、FastAPI 接口测试

```python
from fastapi.testclient import TestClient

def test_create_note(tmp_path):
    app = create_app(db_path=tmp_path / "notes.db")
    with TestClient(app) as client:
        resp = client.post("/notes", json={"text": "第一条"})
        assert resp.status_code == 201
        assert resp.json()["id"] == 1

        resp = client.get("/notes")
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
def test_recommendations_performance(benchmark):
    “””
    性能回归测试（确保不会无故变慢）
    
    对标 Java: @Benchmark (JMH)
    “””
    def recommendation():
        return service.get_recommendations(user_id=1, limit=5)
    
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
