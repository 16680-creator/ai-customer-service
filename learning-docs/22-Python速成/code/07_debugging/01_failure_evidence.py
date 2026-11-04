"""故障证据演示：完整异常链、上下文日志、faulthandler

运行: python 07_debugging/01_failure_evidence.py
"""

# ---- 本段讲：调试用的标准库与日志初始化 ----
# 这个脚本演示「线上出问题时怎么留下足够证据」：完整异常链、带上下文的日志、线程栈。
# Java 对照：异常链相当于 cause，日志相当于 slf4j + logback，faulthandler 相当于 jstack。
import faulthandler       # C 层实现的故障处理：可打印任意时刻的 Python 线程栈
import json               # 解析 JSON 文本；格式错误会抛 json.JSONDecodeError
import logging            # 标准库日志模块；工程里几乎都用它而不是 print
import sys                # 用于把栈信息输出到 stdout，以及读取 sys.executable / sys.path
from pathlib import Path  # 面向对象的路径操作

# 全局日志配置：级别 INFO，格式 = 级别 + logger 名 + 消息。
# 只需配置一次，之后所有 getLogger 得到的 logger 都继承它（Java 里是 logback.xml）。
logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("debug-demo")   # 按模块名取 logger；Java 里是 LoggerFactory.getLogger(Xxx.class)


# ---- 本段讲：底层解析函数（可能抛出原始异常）----
def parse_order(raw: str) -> dict:
    """底层解析：可能抛 JSONDecodeError。"""
    data = json.loads(raw)            # 把字符串解析成 dict；非法 JSON 会抛 JSONDecodeError
    # 模拟没有运行时校验的错误：amount 可能是字符串
    data["amount"] = data["amount"] + 1   # 若 amount 是字符串，这里会抛 TypeError（str + int）
    return data                        # 成功时返回补齐后的字典


# ---- 本段讲：业务边界统一转换异常，并保留根因 ----
# 关键点：raise ... from exc 会把原始异常挂到 __cause__ 上，
# 打印时能看到「The above exception was the direct cause of...」，根因不会丢。
# Java 里对应 throw new BizException("...", cause)。
def create_order(raw: str) -> dict:
    """业务边界：把底层错误转换成业务错误，并保留异常链。"""
    try:
        return parse_order(raw)       # 正常路径：直接返回
    except (json.JSONDecodeError, TypeError) as exc:   # 同时接两种底层异常（元组形式）
        raise ValueError("订单请求格式不合法") from exc   # 转成业务异常；from 保留原始堆栈


# ---- 本段讲：演示 1 —— 完整异常链 ----
def demo_exception_chain() -> None:
    print("=" * 64)
    print("1. 完整异常链：根因在最底层，业务语义在最外层")
    print("=" * 64)

    # amount 故意写成字符串 "100"：JSON 本身合法，但后续 + 1 会触发 TypeError
    raw = '{"order_no": "NO001", "amount": "100"}'
    try:
        create_order(raw)              # 会抛 ValueError（内部 from 了 TypeError）
    except ValueError:
        # logger.exception 会自动附带完整 traceback 和异常链；
        # 注意用 %s 占位而不是 f-string：日志被过滤时不会提前做字符串拼接。
        logger.exception("创建订单失败 raw=%s", raw)

    print("\n错误做法：raise ValueError(str(exc)) 会丢失原始堆栈。")
    print("正确做法：raise ValueError(...) from exc")


# ---- 本段讲：演示 2 —— 日志要带上下文，但不能泄露敏感字段 ----
def demo_request_timing() -> None:
    print()
    print("=" * 64)
    print("2. 日志要带上下文，但不要记录密钥和完整敏感请求")
    print("=" * 64)

    request = {
        "order_no": "NO001",                    # 业务单号：排查时必要的上下文
        "amount": 100,                          # 金额：有助于复现问题
        "api_key": "sk-should-not-be-logged",   # 密钥：绝对不能进日志
    }
    # 字典推导式：遍历 request.items()（键值对），过滤掉 api_key 这个键。
    # Java 里要手写 for 循环 + 新 HashMap，或引入脱敏工具。
    safe_context = {k: v for k, v in request.items() if k != "api_key"}
    logger.info("处理订单 context=%s", safe_context)   # 只记录脱敏后的上下文


# ---- 本段讲：演示 3 —— faulthandler 打印当前线程栈 ----
# 程序「卡死」时通常没有异常可抓，faulthandler 能从信号/超时直接吐出栈，
# 效果类似 Java 的 jstack；生产里常用 dump_traceback_later 定时落盘。
def demo_faulthandler() -> None:
    print()
    print("=" * 64)
    print("3. faulthandler：卡住时打印 Python 线程栈")
    print("=" * 64)

    # 仅在演示脚本中启用；不要在生产里无条件重复开启。
    faulthandler.dump_traceback(file=sys.stdout)   # 立即把当前所有线程的调用栈打到标准输出
    print("上面是当前线程栈。生产可用 dump_traceback_later(30, repeat=True) 定时输出。")


# ---- 本段讲：演示 4 —— import 失败时先确认解释器与路径 ----
# Python 的 import 依赖 sys.path 的搜索顺序，和 Java 的 classpath 类似；
# 出现“本地能跑、服务器找不到模块”时，按这四项逐层核对最快。
def demo_path_diagnosis() -> None:
    print()
    print("=" * 64)
    print("4. import 问题先打印解释器和路径")
    print("=" * 64)

    print(f"sys.executable = {sys.executable}")            # 当前解释器绝对路径（确认是否用错 venv）
    print(f"cwd            = {Path.cwd()}")                # 当前工作目录（相对路径都相对它解析）
    print(f"script         = {Path(__file__).resolve()}")  # 脚本自身绝对路径（确认跑的是哪份文件）
    print("sys.path[0]    =", sys.path[0])                 # 模块搜索路径第一项，通常是脚本所在目录
    print("排查顺序：解释器 -> 工作目录 -> sys.path -> 包安装位置 -> 同名文件遮蔽")


# ---- 本段讲：依次运行四个演示 ----
def main() -> None:
    demo_exception_chain()   # 1. 异常链
    demo_request_timing()    # 2. 上下文日志 + 脱敏
    demo_faulthandler()      # 3. 线程栈
    demo_path_diagnosis()    # 4. 解释器与搜索路径


# 直接运行才执行；被 import 时不产生输出副作用。
if __name__ == "__main__":
    main()
