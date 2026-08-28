"""对话接口测试:LLM 用 FakeLLM 替身,不发真实请求、不花钱。

覆盖:健康检查、普通对话(含多轮历史)、参数校验、SSE 流式分帧格式、CORS 预检。
运行:python -m pytest tests/ -v
"""
import json
from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.services.chat_service import ChatService


class FakeLLM:
    """LLM 替身:回显用户输入,并记录每次调用的入参供断言。"""

    def __init__(self):
        self.settings = SimpleNamespace(llm_model="fake-model")
        self.calls = []

    async def chat(self, history, message):
        self.calls.append({"history": history, "message": message})
        return f"echo:{message}"

    async def chat_stream(self, history, message):
        self.calls.append({"history": history, "message": message})
        for char in f"echo:{message}":
            yield char


@pytest.fixture()
def client(monkeypatch):
    """TestClient + 注入 FakeLLM 的 ChatService。"""
    from app.services import chat_service

    fake = FakeLLM()
    monkeypatch.setattr(chat_service, "get_chat_service", lambda: ChatService(llm=fake))

    test_client = TestClient(app)
    test_client.fake_llm = fake  # 挂到 client 上,便于断言 LLM 收到的入参
    return test_client


def test_health(client):
    resp = client.get("/api/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "up"}


def test_chat_returns_reply_and_model(client):
    resp = client.post("/api/chat", json={"message": "你好"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["reply"] == "echo:你好"
    assert body["model"] == "fake-model"


def test_chat_passes_history_and_session_to_llm(client):
    """多轮历史与 sessionId 应透传:历史进入 LLM 调用,sessionId 原样返回。"""
    history = [
        {"role": "user", "content": "我叫小明"},
        {"role": "assistant", "content": "你好,小明"},
    ]
    resp = client.post(
        "/api/chat",
        json={"message": "我叫什么名字?", "history": history, "sessionId": "s-001"},
    )
    assert resp.status_code == 200
    assert resp.json()["sessionId"] == "s-001"

    call = client.fake_llm.calls[0]
    assert [m.content for m in call["history"]] == ["我叫小明", "你好,小明"]
    assert call["message"] == "我叫什么名字?"


def test_chat_rejects_empty_message(client):
    resp = client.post("/api/chat", json={"message": ""})
    assert resp.status_code == 422


def test_chat_stream_sse_format(client):
    """SSE 分帧:除结束帧外每帧为 `data: {json}`,delta 拼接等于完整回复,末帧 [DONE]。"""
    resp = client.post("/api/chat/stream", json={"message": "你好"})
    assert resp.status_code == 200
    assert resp.headers["content-type"].startswith("text/event-stream")

    frames = [f for f in resp.text.split("\n\n") if f]
    assert frames[-1] == "data: [DONE]"
    deltas = [json.loads(f.removeprefix("data: ")) for f in frames[:-1]]
    assert all(set(d) == {"delta"} for d in deltas)
    assert "".join(d["delta"] for d in deltas) == "echo:你好"


def test_cors_allows_browser_preflight(client):
    """前端(http://localhost:5173)跨域直连时的预检请求必须放行。"""
    resp = client.options(
        "/api/chat",
        headers={
            "Origin": "http://localhost:5173",
            "Access-Control-Request-Method": "POST",
        },
    )
    assert resp.status_code == 200
    assert resp.headers["access-control-allow-origin"] == "*"
