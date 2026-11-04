"""FastAPI + SQLite 接口测试

运行:
    pytest 09_webdb/test_app.py -q
"""

# ============================================================
# 本文件演示：用 pytest + TestClient 对接口做黑盒测试。
# 覆盖点：健康检查、创建/列表、参数校验 422、首尾空白清洗、
#         SQL 注入防护、数据跨应用实例持久化。
# Java 对照：@SpringBootTest + MockMvc；fixture 对应 @BeforeEach，
#            @pytest.mark.parametrize 对应 @ParameterizedTest，tmp_path 对应 @TempDir。
# ============================================================

import pytest
# TestClient：在进程内调用 ASGI 应用，无需启动真实端口 <-> MockMvc
from fastapi.testclient import TestClient

# 从同目录 app.py 导入工厂函数（pytest 会把测试文件所在目录加入 sys.path）
from app import create_app


# @pytest.fixture：声明"可复用的测试前置"，需要它的用例把名字当参数写上即可
# <-> JUnit 的 @BeforeEach + 成员字段；pytest 用"参数注入"代替字段
# tmp_path：pytest 内置 fixture，每个用例拿到一个独立临时目录 <-> @TempDir
@pytest.fixture
def client(tmp_path):
    # 每个用例用独立 db 文件，用例之间互不污染（避免共享内存库导致串数据）
    app = create_app(str(tmp_path / "notes.db"))
    # with 会执行 lifespan 的启动/关闭逻辑；yield 把客户端交出去给用例使用
    with TestClient(app) as test_client:
        # yield 是"前置 + 后置"的分界点：此处之前是 setup，with 退出时是 teardown
        yield test_client


# 健康检查应返回 200 和固定 JSON
def test_health(client):
    response = client.get("/health")
    # 断言 HTTP 状态码 <-> assertEquals(200, status)
    assert response.status_code == 200
    # 断言完整 JSON 结构，而不只是某个字段
    assert response.json() == {"status": "up"}


# 连续创建两条，验证自增 id 与列表顺序
def test_create_and_list_notes(client):
    first = client.post("/notes", json={"text": "第一条"})
    second = client.post("/notes", json={"text": "第二条"})

    # 创建成功应返回 201 而不是 200
    assert first.status_code == 201
    # 自增主键从 1 开始
    assert first.json()["id"] == 1
    # 响应正文与请求一致
    assert first.json()["text"] == "第一条"
    # 第二条拿到 id = 2
    assert second.json()["id"] == 2

    response = client.get("/notes")
    assert response.status_code == 200
    # 列表按 id 升序；这里只提取 text 字段做比较
    assert [n["text"] for n in response.json()] == ["第一条", "第二条"]


# @pytest.mark.parametrize：同一个测试函数跑多组入参
# <-> JUnit5 的 @ParameterizedTest + @ValueSource
# 三种非法输入：空串、纯空白、超过 200 字符（"a" * 201 即重复 201 次）
@pytest.mark.parametrize("text", ["", "   ", "a" * 201])
def test_invalid_text_returns_422(client, text):
    response = client.post("/notes", json={"text": text})
    # 422 Unprocessable Entity：请求格式对但内容不合法 <-> @Valid 校验失败
    assert response.status_code == 422


# 验证自定义校验器真的去掉了首尾空白
def test_text_is_stripped(client):
    response = client.post("/notes", json={"text": "  有内容  "})
    assert response.status_code == 201
    # 入库/返回的是清洗后的值
    assert response.json()["text"] == "有内容"


# 验证 SQL 注入字符串只会被当成普通文本保存
def test_sql_is_parameterized(client):
    payload = "'); DROP TABLE notes; --"
    created = client.post("/notes", json={"text": payload})
    # 请求应成功返回，而不是报 SQL 语法错误
    assert created.status_code == 201

    notes = client.get("/notes").json()
    # 回读内容与原文完全一致，说明参数化占位符生效
    assert notes[0]["text"] == payload


# 验证数据落盘：应用实例重建后数据仍在（同一个 db 文件）
def test_data_persists_across_app_instances(tmp_path):
    db_path = str(tmp_path / "persistent.db")

    # 第一个"应用生命周期"：写入一条数据，退出 with 时应用关闭
    with TestClient(create_app(db_path)) as client:
        assert client.post("/notes", json={"text": "重启前"}).status_code == 201

    # 第二个"应用生命周期"：重新创建应用连接同一个文件，应能读到那条数据
    with TestClient(create_app(db_path)) as client:
        notes = client.get("/notes").json()
        assert [n["text"] for n in notes] == ["重启前"]
