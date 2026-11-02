"""故障证据演示：完整异常链、上下文日志、faulthandler

运行: python 07_debugging/01_failure_evidence.py
"""

import faulthandler
import json
import logging
import sys
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("debug-demo")


def parse_order(raw: str) -> dict:
    """底层解析：可能抛 JSONDecodeError。"""
    data = json.loads(raw)
    # 模拟没有运行时校验的错误：amount 可能是字符串
    data["amount"] = data["amount"] + 1
    return data


def create_order(raw: str) -> dict:
    """业务边界：把底层错误转换成业务错误，并保留异常链。"""
    try:
        return parse_order(raw)
    except (json.JSONDecodeError, TypeError) as exc:
        raise ValueError("订单请求格式不合法") from exc


def demo_exception_chain() -> None:
    print("=" * 64)
    print("1. 完整异常链：根因在最底层，业务语义在最外层")
    print("=" * 64)

    raw = '{"order_no": "NO001", "amount": "100"}'
    try:
        create_order(raw)
    except ValueError:
        logger.exception("创建订单失败 raw=%s", raw)

    print("\n错误做法：raise ValueError(str(exc)) 会丢失原始堆栈。")
    print("正确做法：raise ValueError(...) from exc")


def demo_request_timing() -> None:
    print()
    print("=" * 64)
    print("2. 日志要带上下文，但不要记录密钥和完整敏感请求")
    print("=" * 64)

    request = {
        "order_no": "NO001",
        "amount": 100,
        "api_key": "sk-should-not-be-logged",
    }
    safe_context = {k: v for k, v in request.items() if k != "api_key"}
    logger.info("处理订单 context=%s", safe_context)


def demo_faulthandler() -> None:
    print()
    print("=" * 64)
    print("3. faulthandler：卡住时打印 Python 线程栈")
    print("=" * 64)

    # 仅在演示脚本中启用；不要在生产里无条件重复开启。
    faulthandler.dump_traceback(file=sys.stdout)
    print("上面是当前线程栈。生产可用 dump_traceback_later(30, repeat=True) 定时输出。")


def demo_path_diagnosis() -> None:
    print()
    print("=" * 64)
    print("4. import 问题先打印解释器和路径")
    print("=" * 64)

    print(f"sys.executable = {sys.executable}")
    print(f"cwd            = {Path.cwd()}")
    print(f"script         = {Path(__file__).resolve()}")
    print("sys.path[0]    =", sys.path[0])
    print("排查顺序：解释器 -> 工作目录 -> sys.path -> 包安装位置 -> 同名文件遮蔽")


def main() -> None:
    demo_exception_chain()
    demo_request_timing()
    demo_faulthandler()
    demo_path_diagnosis()


if __name__ == "__main__":
    main()
