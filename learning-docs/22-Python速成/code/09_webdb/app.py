"""FastAPI + SQLite 最小服务：参数化 SQL、事务、连接生命周期

运行真实服务（需要 fastapi/uvicorn）:
    uvicorn 09_webdb.app:app --reload --port 8000

无端口演示:
    python 09_webdb/demo.py
测试:
    pytest 09_webdb/test_app.py -q
"""

import sqlite3
import threading
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI
from pydantic import BaseModel, Field, field_validator


class NoteCreate(BaseModel):
    text: str = Field(min_length=1, max_length=200)

    @field_validator("text")
    @classmethod
    def strip_and_reject_blank(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("内容不能只包含空白")
        return value


class Note(BaseModel):
    id: int
    text: str
    created_at: str


class NoteRepository:
    """SQLite 仓储。

    教学/低并发实现：单连接 + 显式锁，避免 FastAPI 线程池中并发使用连接。
    生产高并发场景应换成成熟连接池与 session 管理。
    """

    def __init__(self, db_path: str = ":memory:"):
        self._conn = sqlite3.connect(db_path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        self._lock = threading.Lock()
        with self._conn:
            self._conn.execute(
                """
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
            )

    def add(self, text: str) -> dict:
        # with conn: 正常结束 commit，异常 rollback。
        with self._lock, self._conn:
            cursor = self._conn.execute("INSERT INTO notes(text) VALUES (?)", (text,))
            row = self._conn.execute(
                "SELECT id, text, created_at FROM notes WHERE id = ?",
                (cursor.lastrowid,),
            ).fetchone()
        return dict(row)

    def list(self) -> list[dict]:
        with self._lock:
            rows = self._conn.execute(
                "SELECT id, text, created_at FROM notes ORDER BY id"
            ).fetchall()
        return [dict(row) for row in rows]

    def close(self) -> None:
        with self._lock:
            self._conn.close()


def create_app(db_path: str = ":memory:") -> FastAPI:
    repo = NoteRepository(db_path)

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        try:
            yield
        finally:
            repo.close()

    app = FastAPI(title="webdb-demo", lifespan=lifespan)

    @app.get("/health")
    def health() -> dict:
        return {"status": "up"}

    @app.post("/notes", response_model=Note, status_code=201)
    def create_note(req: NoteCreate) -> dict:
        # 普通 def：FastAPI 自动放到线程池，不会阻塞事件循环。
        return repo.add(req.text)

    @app.get("/notes", response_model=list[Note])
    def list_notes() -> list[dict]:
        return repo.list()

    return app


app = create_app()
