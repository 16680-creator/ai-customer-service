# 22-Python 速成（Java 开发者视角）

> **写给谁**：写过 Spring Boot、习惯强类型与 IoC 容器、第一次认真学 Python 的 Java 开发者。
> **目标**：不是"从零学一门语言"，而是**用你已有的 Java 心智模型做映射，把迁移成本压到最短**。
> **产出**：7 天能读能改 Python 服务端代码；14 天能独立写一个小型 Python 服务；
> 再加 10 天工程化加练，能调试、测试、优化、部署并做安全检查。

---

## 一、先看结论：Java 开发者学 Python 的三阶段

```text
阶段一（1~2 天）语法映射   把 Java 语法逐条翻译成 Python，用对照表打通
       ↓
阶段二（3~5 天）心智模型   理解「动态类型 / 缩进即语法 / 协程并发 / 无容器」四件事
       ↓
阶段三（6~7 天）工程落地   venv + 依赖管理 + pytest + 类型注解 + 能跑起来
```

**最大的认知陷阱**：用 Java 的方式写 Python（到处建 class、写 getter、抽 interface）。
Python 的"简洁"不是省字符，而是**减少抽象层级**。本目录多处强调这一点。

---

## 二、🎬 场景直达（不按顺序学，按需求跳转）

### 我要快速实现一个功能

| 需求 | 直达章节 | 耗时 |
|------|----------|------|
| 加个 RESTful 端点 | [11篇 §4 路由层](11-FastAPI实战-对着SpringBoot学.md#四路由层apichatpy) | 15min |
| 实现流式 SSE 响应 | [11篇 §8 流式](11-FastAPI实战-对着SpringBoot学.md#八sse-流式对照-fluxserversentevent) | 20min |
| 调用第三方 API | [19篇 §2 HTTP](19-HTTP与数据库实战.md) | 20min |
| 写异步任务/定时任务 | [08篇 §3-5 并发](08-并发模型.md) | 30min |
| 操作 MySQL/SQLite | [19篇 §4 数据库](19-HTTP与数据库实战.md) | 25min |
| 实现 RAG 知识库检索 | [21篇 AI实战](21-AI应用实战-从对话到RAG.md) | 1h |
| 给端点加参数校验 | [11篇 §5 Schema](11-FastAPI实战-对着SpringBoot学.md#五schema-层pydantic-模型这里是最大的效率来源) | 10min |
| 部署到 Docker | [20篇 部署](20-部署与生产运行.md) | 30min |

### 我遇到了问题

| 症状 | 诊断章节 | 关键词 |
|------|----------|--------|
| async 函数卡住/不执行 | [16篇 §3](16-调试与故障排查手册.md) | `time.sleep` 阻塞、`to_thread` |
| Python 太慢怎么办 | [17篇 §1-2](17-性能优化与基准测试.md) | cProfile、算法优化、numpy |
| 测试怎么 mock/隔离 | [18篇 §4-5](18-测试进阶-pytest与可测性设计.md) | monkeypatch、FakeLLM |
| 部署后内存泄漏/OOM | [20篇 §5](20-部署与生产运行.md) + [17篇 tracemalloc](17-性能优化与基准测试.md) | worker 数、连接池 |
| 日志没 trace_id | [25篇 §1 结构化日志](25-可观测性实战-日志、监控、链路追踪.md) | contextvars、JSONFormatter |
| 字符串乱码（Windows） | [06篇 §3 编码](06-异常、文件与上下文管理器.md) | `encoding="utf-8"` |
| 为什么我的 list 被意外改了 | [03篇 §6 拷贝语义](03-数据类型与容器.md) | `copy.deepcopy` |
| import 找不到模块 | [07篇 §2 包与import](07-模块、包与依赖管理.md) | `sys.path`、`__init__.py` |

### 我要对照 Java 学某个主题

| Java 主题 | Python 对应章节 |
|-----------|----------------|
| Spring Boot 分层结构 | [11篇 FastAPI实战](11-FastAPI实战-对着SpringBoot学.md) |
| 线程池/ExecutorService | [08篇 §4 进程池](08-并发模型.md) |
| Flux/Reactor 响应式 | [08篇 §5 asyncio](08-并发模型.md) |
| SLF4J + MDC | [25篇 §1 结构化日志](25-可观测性实战-日志、监控、链路追踪.md) |
| Micrometer 指标 | [25篇 §2 Prometheus](25-可观测性实战-日志、监控、链路追踪.md) |
| Zipkin/Sleuth 链路追踪 | [25篇 §3 OpenTelemetry](25-可观测性实战-日志、监控、链路追踪.md) |
| Bean Validation | [11篇 §5 Pydantic](11-FastAPI实战-对着SpringBoot学.md#五schema-层pydantic-模型这里是最大的效率来源) |
| @Transactional | [19篇 §4 事务](19-HTTP与数据库实战.md) |
| Optional\<T\> | [02篇 §2 None](02-语法速通-Java对照.md) + [09篇 类型注解](09-类型注解与工程化.md) |

### 按角色选学习路径

#### 🎯 后端开发（Spring Boot → FastAPI）
**7天速成**: 02→03→11→12→18→19→20  
**必做练习**: 在 `ai-cs-py-chat` 上加一个带参数校验的端点并写测试

#### ⚙️ 微服务开发（Spring Cloud → Python 微服务）
**核心**: 07(依赖管理)→09(类型注解)→25(可观测性)→24(迁移策略)  
**项目实战**: 在 ai-customer-service 中用 Python 实现一个轻量级推荐服务

#### 🤖 AI 应用开发
**快速上手**: 02→08(并发)→11(FastAPI)→21(RAG)  
**进阶**: 23(安全)→25(可观测性)  
**必做**: 给 ai-cs-chat 加 Function Calling

#### 🔧 运维/SRE
**监控诊断**: 16(调试)→17(性能)→25(可观测性)→20(部署)  
**关注点**: 内存/CPU 分析、日志聚合、告警规则

---

## 三、目录导航（完整版）

### 第一篇 · 入门

| 文档 | 内容 | 建议耗时 |
|---|---|---|
| [00-学习计划与验收标准](00-学习计划与验收标准.md) | 7 天 / 14 天两套计划，每天的可验证验收点 | 30 min 通读 |
| [01-环境搭建与工具链](01-环境搭建与工具链.md) | Python 安装、uv/venv、pyproject、IDE 配置、调试 | 1 h 动手 |
| [02-语法速通-Java对照](02-语法速通-Java对照.md) | 一张总表 + 逐条代码对照，覆盖 90% 日常语法 | 3 h |

### 第二篇 · 核心

| 文档 | 内容 |
|---|---|
| [03-数据类型与容器](03-数据类型与容器.md) | list/dict/set/tuple、切片、推导式、可变性与拷贝语义 |
| [04-函数与作用域](04-函数与作用域.md) | 默认参数陷阱、`*args/**kwargs`、闭包、装饰器、生成器 |
| [05-面向对象与Java对比](05-面向对象与Java对比.md) | `self`、dataclass、`@property`、魔术方法、MRO、Protocol |
| [06-异常、文件与上下文管理器](06-异常、文件与上下文管理器.md) | EAFP 哲学、`with`、pathlib、JSON、**Windows 编码坑** |
| [07-模块、包与依赖管理](07-模块、包与依赖管理.md) | 包结构、import 机制、venv、uv、pyproject vs pom.xml |

### 第三篇 · 进阶

| 文档 | 内容 |
|---|---|
| [08-并发模型](08-并发模型.md) | **GIL**、threading、asyncio、进程池，与 Java 线程模型正面碰撞 |
| [09-类型注解与工程化](09-类型注解与工程化.md) | typing、mypy/pyright、ruff、pytest、pre-commit |
| [10-标准库必会清单](10-标准库必会清单.md) | pathlib / json / re / datetime / collections / logging 等 |

### 第四篇 · 实战

| 文档 | 内容 |
|---|---|
| [11-FastAPI实战-对着SpringBoot学](11-FastAPI实战-对着SpringBoot学.md) | 用本项目 `ai-cs-py-chat` 真实代码讲服务端开发 |
| [12-Java开发者踩坑清单](12-Java开发者踩坑清单.md) | 50 条高频坑：现象 / 原因 / 正确做法 |
| [13-速查表](13-速查表.md) | 一页纸语法 + 标准库 + 常用命令 |
| [14-练习题库与参考答案](14-练习题库与参考答案.md) | 30 题分三档，每题可运行验证 |
| [15-项目实战路线](15-项目实战路线.md) | 三个递进项目 + 用 Python 重写 Java 小工具 |

### 第五篇 · 工程化与 AI 实战

| 文档 | 内容 |
|---|---|
| [16-调试与故障排查手册](16-调试与故障排查手册.md) | 异常链、pdb、faulthandler、asyncio 卡死、内存/编码/import 排查 |
| [17-性能优化与基准测试](17-性能优化与基准测试.md) | timeit、cProfile、tracemalloc、算法/缓存/IO 优化与基准纪律 |
| [18-测试进阶-pytest与可测性设计](18-测试进阶-pytest与可测性设计.md) | 测试分层、fixture、monkeypatch/Mock、异步/FastAPI/覆盖率 |
| [19-HTTP与数据库实战](19-HTTP与数据库实战.md) | httpx 异步/超时/重试、SQLite 事务、FastAPI 线程池与 SQLAlchemy 边界 |
| [20-部署与生产运行](20-部署与生产运行.md) | Docker、worker 模型、配置、日志、健康检查、优雅停机、SSE 代理 |
| [21-AI应用实战-从对话到RAG](21-AI应用实战-从对话到RAG.md) | OpenAI 兼容、上下文预算、RAG、Rerank、引用、结构化输出与评估 |
| [22-Pythonic重构清单](22-Pythonic重构清单.md) | 24 组 Java 风格 → Pythonic 重构对照与提交前检查 |
| [23-安全编码与常见漏洞](23-安全编码与常见漏洞.md) | 注入、路径穿越、SSRF、反序列化、密钥、越权、CORS、AI 特有风险 |
| [24-Java项目迁移Python的策略与边界](24-Java项目迁移Python的策略与边界.md) | 迁移决策表、旁路/网关/逐步替换、契约与数据一致性、验收标准 |
| [25-可观测性实战-日志、监控、链路追踪](25-可观测性实战-日志、监控、链路追踪.md) | 结构化日志、Prometheus 指标、OpenTelemetry 链路、与 ai-customer-service 整合 |
| [26-微服务治理-Nacos与服务发现](26-微服务治理-Nacos与服务发现.md) | Nacos 注册发现、服务调用、负载均衡、配置中心、健康检查 |

### 代码目录

```text
code/
├── 01_basics/          基础语法对照示例（可直接 python 运行）
├── 02_containers/      容器与推导式
├── 03_functions/       函数、闭包、装饰器、生成器
├── 04_oop/             dataclass、魔术方法、Protocol
├── 05_concurrency/     GIL 实证、asyncio 对比
├── 06_exercises/       练习题库的参考答案 + 自测脚本
├── 07_debugging/       异常证据、faulthandler、asyncio 阻塞定位
├── 08_performance/     基准测试、cProfile、内存对比
├── 09_webdb/           FastAPI + SQLite 服务与接口测试
└── 10_ai/              零依赖迷你 RAG 与引用映射测试
```

运行方式：

```bash
cd learning-docs/22-Python速成/code
python 01_basics/01_hello.py
python 06_exercises/run_all.py     # 跑完全部示例；缺可选依赖的示例会标记 SKIP

# 接口/RAG 测试需要可选依赖
python -m pip install -r requirements-dev.txt
python -m pytest                  # 52 个测试用例
```

---

## 三、和本项目的关系

本目录**大量取材于你自己仓库里的真实代码**，不是通用教程：

| 学习点 | 对应项目位置 |
|---|---|
| FastAPI 分层写法 | [`ai-cs-py-chat/app/`](../../ai-cs-py-chat/app/) |
| Pydantic 校验 | [`app/schemas/chat.py`](../../ai-cs-py-chat/app/schemas/chat.py) |
| async 流式（SSE） | [`app/api/chat.py`](../../ai-cs-py-chat/app/api/chat.py) |
| pytest 全 mock 测试 | [`tests/test_chat_api.py`](../../ai-cs-py-chat/tests/test_chat_api.py) |
| 依赖清单 | [`requirements.txt`](../../ai-cs-py-chat/requirements.txt) |
| 已有的 FastAPI 讲解 | [10-Python服务/01-FastAPI对话服务实战](../10-Python服务/01-FastAPI对话服务实战.md) |

> 说明：`10-Python服务` 是**面向项目架构**的服务解析，本目录是**面向语言学习**的教程，两者互为补充：先在这里学会语言，再回去读那份文档理解架构取舍。

---

## 四、知识依赖图（学习顺序可视化）

```text
01-环境 ─────────────┬──→ 02-语法 ─→ 03-容器 ─→ 04-函数 ─┐
                     │                                  │
                     └──→ 05-OOP ──→ 06-异常/文件 ──────┤
                                          ↓              ↓
                                    07-模块/依赖 ──→ 11-FastAPI实战 ←── 10-标准库
                                          ↓              ↓
            ┌─────────────────────────────┼──────────────┴─────────┐
            ↓                             ↓                        ↓
    08-并发(GIL/asyncio)          09-类型注解/工程化        12-踩坑清单
            ↓                             ↓                        ↓
    ┌───────┴────────┐           ┌────────┴────────┐      13-速查表
    ↓                ↓           ↓                 ↓      14-练习题库
18-测试         21-AI应用    25-可观测性      20-部署      15-项目实战
    ↓                ↓           ↓                 ↓
    └────────────────┴───────────┴─────────────────┴──→ 生产就绪
                                                              ↓
                   16-调试 + 17-性能 + 23-安全 + 24-迁移策略
```

**推荐路线**:
- **最短路径**（3h紧急上手）: 02→03→13速查表 + 边改代码边查
- **7天实战**（能改能写）: 绿色路径 01→02→03→04→05→11→12
- **14天扎实**（能独立开发）: 绿色路径 + 08→09→18→19→20→21
- **工程化全栈**（能上线）: 完整路径 + 16→17→23→24→25

---

## 五、三条学习原则

1. **不要看《Python 从入门到实践》前 200 页**。你不需要重新学"什么是变量"。直接读 02 篇的对照表，遇到不懂的回查。
2. **每学一个概念，立刻用 Python 重写一段你写过的 Java 逻辑**。你有 10 年的业务经验，缺的只是语法外壳。
3. **必须动手跑**。Python 最大的优势是 REPL 即时反馈，`python` 敲进去就能试。放着不动只看，效率会掉一半。

---

## 五、学完自检（不通过就别往下走）

```python
# ---- 本段讲：学完 22 篇后的自检样例——能默写说明语法已过关 ----
# 能在 60 秒内不查资料写出来，说明基础过关
from dataclasses import dataclass   # dataclass 装饰器：自动生成 __init__/__repr__/__eq__，省掉 Java 式样板
from typing import Protocol         # Protocol：结构化接口（鸭子类型的静态版），此处仅作导入示例

@dataclass                          # 标注 Order 为数据类，免手写构造函数
class Order:                        # 一个订单模型，对应 Java 的 record/DTO
    order_no: str                   # 字段：订单号（类型注解供 IDE/mypy 检查，运行时不强制）
    amount: float                   # 字段：金额
    items: list[str]                # 字段：商品列表，list[str] 等价 Java 的 List<String>

    @property                       # @property 把方法伪装成只读属性，调用方写 o.is_large 而非 o.is_large()
    def is_large(self) -> bool:     # 是否为"大额单"
        return self.amount > 10_000   # 10_000 的下划线是数字分隔符，等价 Java 的 10000

def settle(orders: list[Order]) -> dict[str, float]:   # 批量结算：传入订单列表，返回"订单号→金额"映射
    """按订单号聚合金额，只保留大额单"""
    return {o.order_no: o.amount for o in orders if o.is_large}   # 字典推导式：一行完成 filter+map+toMap，等价 Java Stream
```

写不出来 → 回到 03/04/05 篇。能写出来但觉得别扭 → 正常，写 300 行就顺了。

---

**下一步**：[00-学习计划与验收标准](00-学习计划与验收标准.md)
