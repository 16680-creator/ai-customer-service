"""FastAPI + SQLite 接口测试

运行:
    pytest 09_webdb/test_app.py -q
"""

import pytest
from fastapi.testclient import TestClient

from app import create_app


@pytest.fixture
def client(tmp_path):
    app = create_app(str(tmp_path / "notes.db"))
    with TestClient(app) as test_client:
        yield test_client


def test_health(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "up"}


def test_create_and_list_notes(client):
    first = client.post("/notes", json={"text": "第一条"})
    second = client.post("/notes", json={"text": "第二条"})

    assert first.status_code == 201
    assert first.json()["id"] == 1
    assert first.json()["text"] == "第一条"
    assert second.json()["id"] == 2

    response = client.get("/notes")
    assert response.status_code == 200
    assert [n["text"] for n in response.json()] == ["第一条", "第二条"]


@pytest.mark.parametrize("text", ["", "   ", "a" * 201])
def test_invalid_text_returns_422(client, text):
    response = client.post("/notes", json={"text": text})
    assert response.status_code == 422


def test_text_is_stripped(client):
    response = client.post("/notes", json={"text": "  有内容  "})
    assert response.status_code == 201
    assert response.json()["text"] == "有内容"


def test_sql_is_parameterized(client):
    payload = "'); DROP TABLE notes; --"
    created = client.post("/notes", json={"text": payload})
    assert created.status_code == 201

    notes = client.get("/notes").json()
    assert notes[0]["text"] == payload


def test_data_persists_across_app_instances(tmp_path):
    db_path = str(tmp_path / "persistent.db")

    with TestClient(create_app(db_path)) as client:
        assert client.post("/notes", json={"text": "重启前"}).status_code == 201

    with TestClient(create_app(db_path)) as client:
        notes = client.get("/notes").json()
        assert [n["text"] for n in notes] == ["重启前"]
