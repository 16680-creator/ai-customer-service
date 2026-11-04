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
# ---- 本段讲：为什么 async 端点里不能用 requests ----
# FastAPI 的 async def 端点跑在唯一的事件循环线程上，调度方式是"协作式"：
# 只有遇到 await 才会把执行权让给别的协程。同步 IO 期间不让出，整个线程上所有请求一起卡住。
# 对照 Java：相当于把 Tomcat 的一根工作线程用 synchronized + 阻塞 socket 按死，线程池很快耗尽。
# ❌ 同步请求会阻塞事件循环
requests.get(url)   # requests 是纯同步库，没有 await；调用期间 DNS、建连、等响应全占着事件循环

# ✅ 异步客户端
async with httpx.AsyncClient(timeout=5) as client:   # async with 退出时自动 await client.aclose()，等价 Java 的 try-with-resources
    resp = await client.get(url)                     # await 让出控制权，等待网络期间事件循环可继续处理其他请求
```

---

## 二、httpx 的标准用法

### 同步

```python
# ---- 本段讲：httpx 同步客户端 + 连接池的标准写法 ----
# Java 对照：效果等同 RestTemplate/OkHttpClient 复用同一个连接池，而不是每次 new。
import httpx   # 第三方库；标准库对应 urllib.request，但缺少连接池和便捷的 json/超时配置

with httpx.Client(base_url="https://api.example.com", timeout=5.0) as client:
    # with 是 try/finally 语法糖：离开代码块自动 close()，归还所有连接；
    # base_url 相当于 Feign 的 @FeignClient(url=...)，之后只写相对路径
    # timeout=5.0 表示"总超时"；生产环境建议拆成 connect/read/write/pool 四段（见下一段异步示例）
    resp = client.get("/orders/NO001", params={"include": "items"})
    # params 字典自动做 URL 编码并拼成 ?include=items，不用自己拼字符串；
    # 等价 Java 里 UriComponentsBuilder.queryParam(...).build().encode()
    resp.raise_for_status()
    # 4xx/5xx 统一抛 httpx.HTTPStatusError；不写这一行，404 也会被当成正常响应继续解析
    data = resp.json()
    # 反序列化为 dict/list（无类型约束）；Java 对照：ObjectMapper.readTree() 或映射成 DTO
```

### 异步与连接复用

```python
# ---- 本段讲：AsyncClient 的四段超时、连接池上限与并发请求 ----
# 关键点：Client 对象要复用（模块级/应用生命周期级），它才是连接池的持有者。
async with httpx.AsyncClient(
    base_url="https://api.example.com",          # 统一前缀，避免每处写全 URL
    timeout=httpx.Timeout(connect=2.0, read=10.0, write=5.0, pool=2.0),
    # 四段超时分别对应：建连、读响应、写请求体、从连接池借连接排队；Java HttpClient 需分别配 connectTimeout 与 HttpRequest.timeout
    limits=httpx.Limits(max_connections=100, max_keepalive_connections=20),
    # max_connections=并发上限；max_keepalive_connections=保活空闲连接数；对应 OkHttp 的 connectionPool 与 maxRequests
    headers={"Authorization": "Bearer ..."},      # 默认请求头，省去每次调用重复传 token；真实值应从配置/密钥管理读取
) as client:                                      # 退出 with 时自动关闭连接池，不泄露 socket
    responses = await asyncio.gather(*[
        # 列表推导式生成一批协程对象（此时还没执行），* 解包成 gather 的位置参数
        client.get(f"/orders/{order_no}") for order_no in order_nos
    ])
    # asyncio.gather 并发等待全部完成，结果顺序与入参一致；
    # Java 对照：CompletableFuture.allOf(...) 或 Reactor 的 Flux.merge，但这里没有线程池开销
```

**不要每个请求都新建 Client**。Client 负责连接池，每次新建会丢掉连接复用，并增加 TLS/连接开销。应用生命周期内复用，退出时关闭。

### 错误要分类处理

```python
# ---- 本段讲：按异常类型分流，决定"能否重试" ----
# Python 的 except 按顺序匹配，子类要写在父类前面，否则会被父类先吞掉。
try:
    resp = await client.get(url)          # 网络调用放在 try 内；注意 await 也可能抛异常
    resp.raise_for_status()               # 主动把 4xx/5xx 转成异常，避免错误码被静默忽略
except httpx.TimeoutException:
    # 可重试，但必须控制次数与退避
    ...
    # 超时属于"结果未知"，重试前要确认接口幂等（GET/PUT 通常可以，POST 下单不行）
except httpx.ConnectError:
    # 网络/域名/连接问题
    ...
    # 建连失败通常可重试；但要区分是 DNS 配置错还是对端没起来，前者重试无意义
except httpx.HTTPStatusError as e:
    # 4xx 通常不应盲目重试；5xx 视接口幂等性决定
    status = e.response.status_code
    # e.response 是原始响应对象；Java 对照：捕获 HttpClientErrorException / HttpServerErrorException 后取 getStatusCode()
```

不要写成：

```python
# ---- 本段讲：反例——过宽的 except 会把业务错误一起重试 ----
# except Exception 相当于 Java 的 catch (Throwable/Exception)，连参数校验错、JSON 解析错都兜住，
# 于是"请求本来就不合法"被当成"网络抖动"，重试三次只是把错误放大三倍。
except Exception:
    return retry()      # 参数错误、业务错误也重试，会放大故障
```

---

## 三、下载大文件必须流式

```python
# ---- 本段讲：流式下载大文件，避免把整个响应读进内存 ----
# client.stream 返回"响应头已到、body 还没读"的上下文，按块拉取；
# Java 对照：HttpClient 的 BodyHandlers.ofInputStream() + 循环 read(buffer)，而不是 bodyToBytes()。
async with client.stream("GET", "/exports/NO001") as resp:
    # async with 保证无论成功失败都会关闭连接、释放未读完的 body
    resp.raise_for_status()                       # 错误码要在开始写文件之前检查，避免落一个空文件
    with open("export.zip", "wb") as f:            # "wb" = 二进制写；Python 的 open 不需要声明编码，二进制模式禁止文本转换
        async for chunk in resp.aiter_bytes(chunk_size=64 * 1024):
            # aiter_bytes 按 64KB 分片产出 bytes，内存占用恒定；chunk 是 bytes，不是 str
            f.write(chunk)                         # 同步写本地文件；若写盘也慢，可再套 asyncio.to_thread
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
# ---- 本段讲：带次数上限与指数退避的重试封装 ----
# 核心约束：只重试"传输层异常 + 502/503/504"，且次数固定、退避递增。
# Java 对照：Resilience4j 的 Retry 配置（maxAttempts + exponentialBackoff），这里手写一遍看清原理。
async def get_with_retry(client, path, *, attempts=3):
    # 末尾的 * 表示 attempts 只能按关键字传（get_with_retry(c, p, attempts=5)），避免位置参数误传
    for attempt in range(1, attempts + 1):
        # range(1, 4) 产出 1、2、3；Python 的 for 是"遍历可迭代对象"，不是 Java 的 for(;;) 三段式
        try:
            resp = await client.get(path)        # 每次重试都重新发一次请求（GET 幂等，才敢这么做）
            if resp.status_code in (502, 503, 504):
                # 502/503/504 表示"网关或上游暂时不可用"，属于可重试范围；raise_for_status 不会区分，所以先手动拦
                raise httpx.HTTPStatusError("upstream", request=resp.request, response=resp)
            resp.raise_for_status()              # 其余 4xx/5xx 也抛异常；本示例把 HTTPStatusError 一并纳入重试，生产应按状态码细分
            return resp.json()                   # 成功即返回，函数结束；Python 无需声明返回类型
        except (httpx.TransportError, httpx.HTTPStatusError):
            # TransportError 覆盖超时/建连/读写中断；HTTPStatusError 本次实现里也统一进重试分支
            if attempt == attempts:
                raise                            # 最后一次仍失败就向上抛，让调用方决定降级；裸 raise 保留原始栈
            await asyncio.sleep(0.2 * 2 ** (attempt - 1))
            # 指数退避：0.2s → 0.4s → 0.8s；** 是幂运算，等价 Java 的 Math.pow
```

生产上还要加**抖动（jitter）**，避免所有实例同时重试。

---

## 五、sqlite3：学习和小型工具足够

```python
# ---- 本段讲：sqlite3 的建表、插入、查询与事务 ----
# 标准库自带，无需 JDBC 驱动；Connection ≈ java.sql.Connection，Cursor ≈ PreparedStatement/ResultSet。
import sqlite3

conn = sqlite3.connect("app.db")
# 建立连接；文件不存在会自动创建。参数化占位符是 "?"（MySQL 驱动也支持 ?，PostgreSQL 用 %s）

conn.row_factory = sqlite3.Row
# 让查询结果的行支持按列名取值（row["text"]），而不是只能按下标 row[1]；
# Java 对照：ResultSet.getString("text")，这里是给整个连接设默认行工厂

with conn:  # 成功 commit，异常 rollback
    # 注意：with conn 管理的是"事务"，不是关闭连接；等价 Spring 的 @Transactional 边界
    conn.execute("""
        CREATE TABLE IF NOT EXISTS notes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            text TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
    """)
    # 三列含义：id 是自增主键（对应 MySQL AUTO_INCREMENT）；text 为必填文本；
    # created_at 用库函数默认值填充；SQLite 没有独立 DATETIME 类型，时间以 ISO 字符串存
    # 三引号字符串保留换行与缩进，SQL 更可读，等价 Java 里的 text block """..."""

with conn:
    # 第二个独立事务：DDL 与 DML 分开，避免某些数据库的隐式提交差异
    cur = conn.execute("INSERT INTO notes(text) VALUES (?)", ("第一条",))
    # 用 ? 参数化，值放在元组里；"第一条" 后的逗号是单元素元组的语法要求，不能省
    # 严禁 f"INSERT ... VALUES ('{text}')"：那是 SQL 注入，等价 Java 里拼字符串而不用 PreparedStatement
    note_id = cur.lastrowid
    # 自增主键回填；Java 对照：Statement.RETURN_GENERATED_KEYS + getGeneratedKeys()

row = conn.execute("SELECT * FROM notes WHERE id = ?", (note_id,)).fetchone()
# fetchone() 取第一行（没有则 None），等价 ResultSet.next() + 读取一行；注意 SQL 仍是参数化
print(dict(row))
# Row 转普通 dict 才能直接打印/序列化；Java 对照：手动映射成 DTO
conn.close()
# 显式关闭；生产应放 finally 或 with closing(conn)，避免异常路径泄漏连接
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
# ---- 本段讲：用普通 def 写同步 DB 端点，交给 FastAPI 的线程池 ----
# FastAPI 的规则：async def 在事件循环里跑；普通 def 会被自动丢进 anyio 线程池执行。
# 所以"网关 + 同步 DB 驱动"这种组合，用 def 反而是正确写法。
@app.post("/notes", status_code=201)
# 装饰器等价 Spring 的 @PostMapping + @ResponseStatus(CREATED)；status_code 决定成功响应码
def create_note(req: NoteCreate):       # 普通 def，FastAPI 自动放线程池
    # req 由 Pydantic 从 JSON body 反序列化并校验，类似 @RequestBody + @Valid
    return repo.add(req.text)
    # 返回 dict/模型会被自动序列化成 JSON；同步 repo 调用在线程池线程里阻塞，不影响事件循环
```

这是合法的，而且比下面这种写法好：

```python
# ---- 本段讲：反例——async def 里直接跑同步 DB ----
# async def 让 FastAPI 认为"这个函数不会阻塞"，于是不做线程池隔离；
# 结果 repo.add 的同步 IO 卡住整个事件循环，并发能力骤降为一个请求。
@app.post("/notes")
async def create_note(req: NoteCreate):
    # 注意：这里没有 await，说明函数体内其实是纯同步逻辑——那就不该声明成 async
    return repo.add(req.text)            # ❌ 同步 DB 阻塞事件循环
```

如果端点必须是 `async def`：

```python
# ---- 本段讲：async def 端点里如何安全地跑同步 DB 代码 ----
# asyncio.to_thread 把同步函数丢到线程池执行并返回可 await 的结果，
# Java 对照：CompletableFuture.supplyAsync(() -> repo.add(text), dbExecutor)，只是这里不用显式声明线程池。
@app.post("/notes")
async def create_note(req: NoteCreate):   # 端点本身保持 async，不阻塞事件循环
    return await asyncio.to_thread(repo.add, req.text)
    # await 等待线程池里的同步 DB 调用完成；期间事件循环可继续服务其他请求，
    # 阻塞被隔离在工作线程里，相当于把阻塞 IO 划分到专用线程池
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
# ---- 本段讲：事务边界应放在 Service 层，而不是各个 Repository 内部 ----
# session.begin() 开启一个事务块，离开时自动 commit、抛异常时自动 rollback；
# Java 对照：Spring 的 @Transactional 标在 Service 方法上，而不是每个 Dao 方法各自提交。
def place_order(command):                      # command 是入参对象，类似 Spring 的 ApplicationEvent/命令 DTO
    with session.begin():                      # Service 层定义事务边界
        order_repo.insert(...)                 # 写订单表；此时尚未提交，处于同一事务
        stock_repo.deduct(...)                 # 扣库存；任一步失败，整体回滚
        payment_repo.create(...)               # 创建支付单；三个仓库共享同一个 session/事务
        # 对应 Java 里 @Transactional 方法内调用多个 Mapper/Repository：要么全成功，要么全回滚
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
