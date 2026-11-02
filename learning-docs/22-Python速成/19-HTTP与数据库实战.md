# 19 · HTTP 与数据库实战

> 语法学会之后，服务端 80% 的工作是“调别人”和“存数据”。
> 这篇聚焦 `httpx` + `sqlite3`，并说明什么时候该上 SQLAlchemy。

---

## 一、HTTP 客户端：优先 httpx，不要只用 requests

| 能力 | requests | httpx |
|---|---|---|
| 同步调用 | ✅ | ✅ |
| 异步调用 | ❌ | ✅ |
| 连接池 | Session | Client / AsyncClient |
| 超时 | 支持 | 支持，粒度更清楚 |
| HTTP/2 | 不支持 | 可选 |

在 FastAPI 的 async 端点里：

```python
# ❌ 同步请求会阻塞事件循环
requests.get(url)

# ✅ 异步客户端
async with httpx.AsyncClient(timeout=5) as client:
    resp = await client.get(url)
```

---

## 二、httpx 的标准用法

### 同步

```python
import httpx

with httpx.Client(base_url="https://api.example.com", timeout=5.0) as client:
    resp = client.get("/orders/NO001", params={"include": "items"})
    resp.raise_for_status()
    data = resp.json()
```

### 异步与连接复用

```python
async with httpx.AsyncClient(
    base_url="https://api.example.com",
    timeout=httpx.Timeout(connect=2.0, read=10.0, write=5.0, pool=2.0),
    limits=httpx.Limits(max_connections=100, max_keepalive_connections=20),
    headers={"Authorization": "Bearer ..."},
) as client:
    responses = await asyncio.gather(*[
        client.get(f"/orders/{order_no}") for order_no in order_nos
    ])
```

**不要每个请求都新建 Client**。Client 负责连接池，每次新建会丢掉连接复用，并增加 TLS/连接开销。应用生命周期内复用，退出时关闭。

### 错误要分类处理

```python
try:
    resp = await client.get(url)
    resp.raise_for_status()
except httpx.TimeoutException:
    # 可重试，但必须控制次数与退避
    ...
except httpx.ConnectError:
    # 网络/域名/连接问题
    ...
except httpx.HTTPStatusError as e:
    # 4xx 通常不应盲目重试；5xx 视接口幂等性决定
    status = e.response.status_code
```

不要写成：

```python
except Exception:
    return retry()      # 参数错误、业务错误也重试，会放大故障
```

---

## 三、下载大文件必须流式

```python
async with client.stream("GET", "/exports/NO001") as resp:
    resp.raise_for_status()
    with open("export.zip", "wb") as f:
        async for chunk in resp.aiter_bytes(chunk_size=64 * 1024):
            f.write(chunk)
```

`resp.content` 会把整个响应载入内存；大文件下载不要这么做。

---

## 四、重试与幂等

可以重试的典型情况：

- 连接失败、部分超时
- 502/503/504，且接口幂等
- 明确的限流响应，遵守 `Retry-After`

不应该直接重试：

- 400/401/403/404/422
- 非幂等扣款/下单，没有幂等键
- 已经收到部分流式内容后，从中途简单重放

```python
async def get_with_retry(client, path, *, attempts=3):
    for attempt in range(1, attempts + 1):
        try:
            resp = await client.get(path)
            if resp.status_code in (502, 503, 504):
                raise httpx.HTTPStatusError("upstream", request=resp.request, response=resp)
            resp.raise_for_status()
            return resp.json()
        except (httpx.TransportError, httpx.HTTPStatusError):
            if attempt == attempts:
                raise
            await asyncio.sleep(0.2 * 2 ** (attempt - 1))
```

生产上还要加**抖动（jitter）**，避免所有实例同时重试。

---

## 五、sqlite3：学习和小型工具足够

```python
import sqlite3

conn = sqlite3.connect("app.db")
conn.row_factory = sqlite3.Row

with conn:  # 成功 commit，异常 rollback
    conn.execute("""
        CREATE TABLE IF NOT EXISTS notes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            text TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
    """)

with conn:
    cur = conn.execute("INSERT INTO notes(text) VALUES (?)", ("第一条",))
    note_id = cur.lastrowid

row = conn.execute("SELECT * FROM notes WHERE id = ?", (note_id,)).fetchone()
print(dict(row))
conn.close()
```

### 三条铁律

1. **永远参数化查询**：`WHERE id = ?`，不要 f-string 拼 SQL
2. **事务边界明确**：`with conn:`，异常自动回滚
3. **连接有所有者**：谁创建谁关闭，不在函数间随意传递无生命周期管理的连接

### SQLite 的并发边界

SQLite 适合：

- 本地工具、测试、小型单实例服务
- 低到中等写入、读多写少

不适合：

- 多实例高并发写入
- 跨机器共享数据库文件
- 高吞吐订单/支付主库

生产 MySQL/PostgreSQL 的连接池、事务和锁仍需按原方案设计，Python 不会改变数据库本身的限制。

---

## 六、FastAPI 中同步 DB 代码怎么写

```python
@app.post("/notes", status_code=201)
def create_note(req: NoteCreate):       # 普通 def，FastAPI 自动放线程池
    return repo.add(req.text)
```

这是合法的，而且比下面这种写法好：

```python
@app.post("/notes")
async def create_note(req: NoteCreate):
    return repo.add(req.text)            # ❌ 同步 DB 阻塞事件循环
```

如果端点必须是 `async def`：

```python
@app.post("/notes")
async def create_note(req: NoteCreate):
    return await asyncio.to_thread(repo.add, req.text)
```

同步连接不能无保护地被多个线程同时使用。SQLite 可以：

- 每次操作创建短连接（简单，但有开销）
- 或统一连接 + `check_same_thread=False` + 显式锁（教学/低并发可用）
- 正式项目用成熟 ORM 的连接池与 session 管理

---

## 七、什么时候上 SQLAlchemy

当你开始需要以下能力时：

- 多表关系与复杂查询
- 统一的 session / transaction 生命周期
- 连接池
- 迁移（Alembic）
- 模型与数据库表映射

典型层次：

```text
API（Pydantic 请求/响应）
  ↓
Service（业务规则、事务边界）
  ↓
Repository（SQLAlchemy 查询）
  ↓
Session / Engine
```

注意：**Pydantic 模型不是 ORM 模型**。前者是接口契约，后者是持久化结构，不要在 API 层直接返回 ORM 对象并让它随意序列化。

异步 SQLAlchemy 使用 `async_sessionmaker` + `AsyncSession`，不要拿同步 Session 在协程里直接跑。

---

## 八、事务应该放在哪一层

```python
def place_order(command):
    with session.begin():          # Service 层定义事务边界
        order_repo.insert(...)
        stock_repo.deduct(...)     # 任一步失败，整体回滚
        payment_repo.create(...)
```

不要每个 Repository 方法都独立 commit，否则“订单成功、库存失败”会留下不一致数据。Repository 提供操作，Service 决定事务范围。

---

## 九、示例代码

`code/09_webdb/` 提供一个带 SQLite 的 FastAPI 笔记服务：

- Pydantic 请求/响应模型
- Repository 参数化 SQL
- `with conn` 事务
- 同步端点由 FastAPI 自动放线程池
- lifespan 关闭连接
- TestClient + `tmp_path` 隔离测试

```bash
cd learning-docs/22-Python速成/code
python 09_webdb/demo.py
```

启动真实服务（需 fastapi/uvicorn）：

```bash
uvicorn 09_webdb.app:app --reload --port 8000
```

测试：

```bash
pytest 09_webdb/test_app.py -q
```

---

## 十、上线前检查

- [ ] 所有 DB/HTTP 调用有超时
- [ ] 重试只针对合适异常和幂等操作
- [ ] Client/连接池复用，退出时关闭
- [ ] SQL 全部参数化
- [ ] Service 层事务边界清晰
- [ ] 测试使用临时 DB，不连生产
- [ ] async 端点里没有未隔离的同步 IO
- [ ] 错误响应不泄露连接串、SQL、密钥和堆栈

---

**上一篇**：[18-测试进阶-pytest与可测性设计](18-测试进阶-pytest与可测性设计.md)　**下一篇**：[20-部署与生产运行](20-部署与生产运行.md)
