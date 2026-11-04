"""不启动真实端口的 FastAPI + SQLite 演示

运行: python 09_webdb/demo.py
需要: fastapi + httpx（TestClient 依赖）
"""

# ============================================================
# 本文件演示：不启动 uvicorn、不占端口，直接用 TestClient 跑通接口。
# TestClient 会在进程内启动 ASGI 应用并模拟 HTTP 请求，
# 相当于 Spring 的 MockMvc / WebTestClient —— 不需要真实服务器。
# 流程：健康检查 -> 连续 POST 三条 -> 校验 422 -> GET 列表 -> 断言。
# ============================================================

# 把"导入失败"当成可预期情况处理：没装 fastapi 时不让脚本直接崩溃
try:
    # TestClient 是同步封装（依赖 httpx），可在普通脚本里直接调用接口 <-> MockMvc
    from fastapi.testclient import TestClient
except ModuleNotFoundError:
    # 打印跳过原因与安装命令后，以退出码 0 正常结束（视为跳过而不是失败）
    print("SKIP: 需要 fastapi/httpx。安装：python -m pip install fastapi httpx")
    raise SystemExit(0)

# 从同目录的 app.py 导入应用工厂
from app import create_app


def main() -> None:
    # with 语句在进入时触发 lifespan 启动、退出时触发关闭（关闭数据库连接）
    # 这行等价于"启动应用 + 拿到测试客户端" <-> @SpringBootTest 启动上下文 + 注入 MockMvc
    with TestClient(create_app()) as client:
        # 调用 GET /health 并解析响应 JSON <-> mockMvc.perform(get("/health"))
        print("health ->", client.get("/health").json())

        # 依次 POST 三条笔记；第三条故意是 SQL 注入字符串，用于验证参数化查询
        for text in ["第一条笔记", "第二条笔记", "'); DROP TABLE notes; --"]:
            # json= 会把 dict 序列化成请求体，并以 application/json 发送 <-> @RequestBody
            response = client.post("/notes", json={"text": text})
            # text!r 用 repr 展示原文，便于看清末尾的引号与分号
            print(f"POST {text!r} -> {response.status_code} {response.json()}")

        # 只含空白的文本：Pydantic 校验器抛 ValueError，FastAPI 最终返回 422
        blank = client.post("/notes", json={"text": "   "})
        print(f"POST 空白内容 -> {blank.status_code}（参数校验 422）")

        # GET 列表：应包含前面三条成功的写入
        notes = client.get("/notes").json()
        print(f"GET /notes -> 共 {len(notes)} 条")
        # 逐条打印，方便肉眼核对 id / text / created_at
        for note in notes:
            print(" ", note)

        # 断言条目数为 3 <-> JUnit 的 assertEquals
        assert len(notes) == 3
        # 断言注入字符串被"原样"保存下来，说明它没有被当成 SQL 执行
        assert notes[-1]["text"] == "'); DROP TABLE notes; --"
        print("SQL 注入字符串被当作普通文本保存：参数化查询生效。")


# 脚本入口：直接 python demo.py 时执行 main，被 import 时不执行
if __name__ == "__main__":
    main()
