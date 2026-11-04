"""FastAPI + SQLite 最小服务：参数化 SQL、事务、连接生命周期

运行真实服务（需要 fastapi/uvicorn）:
    uvicorn 09_webdb.app:app --reload --port 8000

无端口演示:
    python 09_webdb/demo.py
测试:
    pytest 09_webdb/test_app.py -q
"""

# ============================================================
# 本模块演示：用 FastAPI + 标准库 sqlite3 写一个最小 CRUD 服务。
# 三个重点，都能对上 Spring Boot 的日常关注点：
#   1) Pydantic 模型做入参校验   <-> DTO + @Valid
#   2) 参数化 SQL + 事务上下文    <-> JdbcTemplate 的 ? 占位符 + @Transactional
#   3) lifespan 管理连接生命周期 <-> @PostConstruct / @PreDestroy
# 读法建议：先从 create_app() 看路由，再回头读 NoteRepository。
# ============================================================

# sqlite3：Python 标准库自带的关系型数据库驱动，无需安装 <-> JDBC 驱动 + DataSource
import sqlite3
# threading：提供互斥锁，用来保护"单连接"不被多线程同时使用 <-> ReentrantLock / synchronized
import threading
# AsyncIterator：仅用于类型标注，表示 lifespan 是异步生成器 <-> 近似 Closeable
from collections.abc import AsyncIterator
# asynccontextmanager：把 async 生成器装饰成 async 上下文管理器 <-> 近似 @PostConstruct/@PreDestroy 组合
from contextlib import asynccontextmanager

# FastAPI：Web 框架本体，同时也是应用对象的类型 <-> Spring Web 的 ApplicationContext 载体
from fastapi import FastAPI
# BaseModel：所有请求/响应模型的基类（自带解析与校验） <-> DTO 基类
# Field：给字段附加约束（长度、默认值等） <-> @Size / @NotNull 等 Bean Validation 注解
# field_validator：字段级自定义校验钩子 <-> 自定义 ConstraintValidator
from pydantic import BaseModel, Field, field_validator


# ------------------------------------------------------------
# 入参模型：只声明"创建笔记"允许客户端提交的字段。
# Java 对照：带 @NotBlank + @Size(max = 200) 的 CreateNoteRequest DTO。
# 特意不含 id / created_at —— 这些由服务端生成，绝不信任客户端传入。
# ------------------------------------------------------------
class NoteCreate(BaseModel):
    # text 必填；长度约束由 Pydantic 在进入路由函数之前完成校验 <-> @Size(min = 1, max = 200)
    text: str = Field(min_length=1, max_length=200)

    # @field_validator("text")：声明"下面这个方法专门负责 text 字段的校验/清洗"
    # <-> 自定义校验注解；校验不过时抛异常由框架转成错误响应
    # @classmethod：Pydantic v2 的校验器规范写法，第一个参数是类本身（这里命名为 cls）
    @field_validator("text")
    @classmethod
    def strip_and_reject_blank(cls, value: str) -> str:
        # 先去掉首尾空白：这样 "  有内容  " 最终会存成 "有内容"
        value = value.strip()
        # 全是空白（如 "   "）时，原始长度 > 0 能骗过 min_length，但业务上无效，故手动拦下
        # 抛 ValueError 后 FastAPI 会自动返回 422 <-> 校验失败抛 MethodArgumentNotValidException
        if not value:
            raise ValueError("内容不能只包含空白")
        # 返回处理后的值：Pydantic 会用返回值替换原值，这是"清洗型"校验器的关键
        return value


# ------------------------------------------------------------
# 出参模型：定义返回给前端的 JSON 结构。
# Java 对照：NoteResponse DTO，字段名即 JSON 键名。
# 配合 response_model 使用时，多余字段会被过滤，避免内部数据泄漏。
# ------------------------------------------------------------
class Note(BaseModel):
    # 数据库自增主键 <-> Long id
    id: int
    # 笔记正文
    text: str
    # 创建时间：由 SQLite 的 CURRENT_TIMESTAMP 生成，这里用 str 接收 <-> LocalDateTime
    created_at: str


# ------------------------------------------------------------
# 数据访问层（仓储）：把所有 SQL 收敛在类里，路由层只调用方法。
# Java 对照：@Repository 标注的 NoteRepository，内部持有一个 JdbcTemplate。
#
# 为什么加锁：sqlite3 连接默认不允许跨线程使用（这里用 check_same_thread=False 放开），
# 而 FastAPI 会把"普通 def"路由丢进线程池执行，多个请求可能同时用到这一条连接，
# 用一把锁串行化即可保证安全。生产高并发应换成连接池。
# ------------------------------------------------------------
class NoteRepository:
    """SQLite 仓储。

    教学/低并发实现：单连接 + 显式锁，避免 FastAPI 线程池中并发使用连接。
    生产高并发场景应换成成熟连接池与 session 管理。
    """

    # db_path 默认 ":memory:"：内存库，进程结束即消失，适合测试/演示；
    # 传文件路径则数据落盘，可跨应用实例重启保留。
    def __init__(self, db_path: str = ":memory:"):
        # 建立连接；check_same_thread=False 允许其它线程使用该连接（安全由下面的锁保证）
        # Java 对照：DriverManager.getConnection(url)
        self._conn = sqlite3.connect(db_path, check_same_thread=False)
        # row_factory=sqlite3.Row：查询结果支持按列名访问（row["id"]），默认只能按数字下标
        # Java 对照：把 ResultSet 映射成 Map/POJO，而不是裸数组
        self._conn.row_factory = sqlite3.Row
        # 一把互斥锁，保护"单连接"的并发访问 <-> ReentrantLock
        self._lock = threading.Lock()
        # with self._conn: 是 SQLite 连接自带的"事务上下文"：正常退出 commit，异常 rollback
        # <-> @Transactional 的自动提交/回滚语义
        with self._conn:
            # IF NOT EXISTS：表已存在不报错，实现"启动即建表"，省掉独立迁移脚本
            self._conn.execute(
                """
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
            )

    # 新增一条笔记，返回完整行（含数据库生成的 id 与 created_at）
    def add(self, text: str) -> dict:
        # with conn: 正常结束 commit，异常 rollback。
        # 同时 with self._lock 保证同一时刻只有一个线程操作这条连接（按从左到右获取）
        with self._lock, self._conn:
            # 关键：用 ? 占位符 + 参数元组，绝不拼接字符串 —— 这是防 SQL 注入的根本
            # 即使 text 是 "'); DROP TABLE notes; --"，也只会被当作普通字符串存进去
            # Java 对照：jdbcTemplate.update("INSERT INTO notes(text) VALUES (?)", text)
            cursor = self._conn.execute("INSERT INTO notes(text) VALUES (?)", (text,))
            # cursor.lastrowid：刚插入行的自增主键 <-> KeyHolder.getKey()
            # 再查一次是为了拿到数据库自动填的 created_at（INSERT 语句里没有显式赋值）
            row = self._conn.execute(
                "SELECT id, text, created_at FROM notes WHERE id = ?",
                (cursor.lastrowid,),
            ).fetchone()
        # Row 转成普通 dict，可直接作为 JSON 返回 <-> POJO 序列化为 Map
        return dict(row)

    # 查询全部笔记，按 id 升序（即创建顺序）
    def list(self) -> list[dict]:
        # 只读操作：加锁即可，不需要事务上下文
        with self._lock:
            rows = self._conn.execute(
                "SELECT id, text, created_at FROM notes ORDER BY id"
            ).fetchall()
        # 列表推导式把每行 Row 转成 dict <-> rows.stream().map(...).collect(toList())
        return [dict(row) for row in rows]

    # 关闭连接，由 lifespan 在应用退出时调用，防止连接泄漏
    def close(self) -> None:
        with self._lock:
            self._conn.close()


# ------------------------------------------------------------
# 应用工厂：把"创建 app + 装配路由 + 绑定资源"打包成一个函数。
# Java 对照：不是 @SpringBootApplication 的自动扫描，而更像手写 @Configuration
# 里用 @Bean 显式装配。工厂形式的好处是测试时可以传入不同的 db_path。
# ------------------------------------------------------------
def create_app(db_path: str = ":memory:") -> FastAPI:
    # 先建仓储（构造函数里已确保建表），再把它闭包进路由函数使用
    repo = NoteRepository(db_path)

    # lifespan：应用启动/关闭的钩子，是一个异步上下文管理器
    # yield 之前是"启动逻辑"，yield 之后（finally）是"关闭逻辑"
    # Java 对照：@PostConstruct 初始化 / @PreDestroy 释放资源
    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        try:
            # 把控制权交还给 FastAPI：应用开始对外提供服务
            yield
        finally:
            # 无论正常退出还是异常，都关闭数据库连接 <-> try-finally 中调用 close()
            repo.close()

    # 实例化应用；title 会展示在 /docs 页面，lifespan 绑定上面的上下文管理器
    app = FastAPI(title="webdb-demo", lifespan=lifespan)

    # GET /health：健康检查接口 <-> @GetMapping("/health")
    # 返回 dict 时 FastAPI 会自动序列化成 JSON <-> @ResponseBody Map<String, Object>
    @app.get("/health")
    def health() -> dict:
        return {"status": "up"}

    # POST /notes：创建笔记
    # response_model=Note：用 Note 模型过滤/校验出参（多余字段不会返回）
    # status_code=201：创建成功的标准状态码 <-> @ResponseStatus(HttpStatus.CREATED)
    @app.post("/notes", response_model=Note, status_code=201)
    def create_note(req: NoteCreate) -> dict:
        # 普通 def：FastAPI 自动放到线程池，不会阻塞事件循环。
        # req: NoteCreate 表示请求体按 NoteCreate 解析并校验 <-> @RequestBody @Valid NoteCreate req
        # 校验失败会自动返回 422，函数体根本不会执行
        return repo.add(req.text)

    # GET /notes：列表接口；response_model=list[Note] 表示返回 JSON 数组
    # <-> @GetMapping 返回 List<NoteResponse>
    @app.get("/notes", response_model=list[Note])
    def list_notes() -> list[dict]:
        return repo.list()

    # 返回装配好的应用实例，交给 uvicorn 或 TestClient 使用
    return app


# 模块级单例：uvicorn 启动串 "09_webdb.app:app" 指向的就是这个变量
# <-> Spring Boot 里那个由容器托管的入口
app = create_app()
