"""不启动真实端口的 FastAPI + SQLite 演示

运行: python 09_webdb/demo.py
需要: fastapi + httpx（TestClient 依赖）
"""

try:
    from fastapi.testclient import TestClient
except ModuleNotFoundError:
    print("SKIP: 需要 fastapi/httpx。安装：python -m pip install fastapi httpx")
    raise SystemExit(0)

from app import create_app


def main() -> None:
    with TestClient(create_app()) as client:
        print("health ->", client.get("/health").json())

        for text in ["第一条笔记", "第二条笔记", "'); DROP TABLE notes; --"]:
            response = client.post("/notes", json={"text": text})
            print(f"POST {text!r} -> {response.status_code} {response.json()}")

        blank = client.post("/notes", json={"text": "   "})
        print(f"POST 空白内容 -> {blank.status_code}（参数校验 422）")

        notes = client.get("/notes").json()
        print(f"GET /notes -> 共 {len(notes)} 条")
        for note in notes:
            print(" ", note)

        assert len(notes) == 3
        assert notes[-1]["text"] == "'); DROP TABLE notes; --"
        print("SQL 注入字符串被当作普通文本保存：参数化查询生效。")


if __name__ == "__main__":
    main()
