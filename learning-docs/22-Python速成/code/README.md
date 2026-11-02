# 运行说明

基础示例只依赖标准库；`09_webdb` 的 FastAPI 演示和全部 pytest 需要可选依赖。没有安装 FastAPI 时，Web 演示会显示 SKIP，不影响其他脚本。

```bash
cd learning-docs/22-Python速成/code

python 01_basics/01_hello.py
python 02_containers/01_list.py
python 03_functions/04_decorator.py
python 04_oop/02_dataclass.py
python 05_concurrency/01_gil_proof.py

# 跑完全部 33 个示例（输出 PASS / SKIP / FAIL，SKIP 一般是缺可选依赖）
python 06_exercises/run_all.py

# 可选：接口测试、异步测试、迷你 RAG 测试
python -m pip install -r requirements-dev.txt
python -m pytest
```

## 目录

| 目录 | 内容 | 对应文档 |
|---|---|---|
| `01_basics/` | 语法对照：类型、字符串、控制流、函数 | [02](../02-语法速通-Java对照.md) |
| `02_containers/` | list/dict/set/tuple、推导式、拷贝语义 | [03](../03-数据类型与容器.md) |
| `03_functions/` | 参数、作用域、闭包、装饰器、生成器 | [04](../04-函数与作用域.md) |
| `04_oop/` | 类、dataclass、property、魔术方法、Protocol | [05](../05-面向对象与Java对比.md) |
| `05_concurrency/` | GIL 实证、线程池、asyncio、限流 | [08](../08-并发模型.md) |
| `06_exercises/` | 题库答案、踩坑演示、统一自测入口 | [14](../14-练习题库与参考答案.md) |
| `07_debugging/` | 异常链、faulthandler、asyncio 阻塞定位 | [16](../16-调试与故障排查手册.md) |
| `08_performance/` | timeit、cProfile、tracemalloc | [17](../17-性能优化与基准测试.md) |
| `09_webdb/` | FastAPI + SQLite、参数化 SQL、接口测试 | [19](../19-HTTP与数据库实战.md) |
| `10_ai/` | 零依赖迷你 RAG、检索与引用映射测试 | [21](../21-AI应用实战-从对话到RAG.md) |

## 建议用法

1. **先猜再跑**：看源码里的 `print` 预测输出，然后运行对比。猜错的地方就是你要重点理解的地方。
2. **改一改再跑**：把数字改大、把条件反过来，观察行为变化。
3. **用 REPL 验证**：`python` 进去，把片段粘一遍，比读文档快。

## 注意

- `05_concurrency/01_gil_proof.py` 会跑几秒（要做性能对比），属正常。
- `02_thread_io.py` / `04_async_vs_thread.py` 用 `time.sleep` 模拟 IO，不联网。
- 全部脚本只使用 Python 3.10+ 语法。
- `09_webdb` 需要 `fastapi/httpx`，启动真实服务还需要 `uvicorn`。
- 测试配置在 `pytest.ini`；从 `code/` 根目录直接运行 `python -m pytest` 即可。
