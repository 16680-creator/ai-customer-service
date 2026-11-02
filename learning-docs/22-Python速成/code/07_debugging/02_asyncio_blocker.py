"""asyncio 阻塞定位：一个同步调用如何拖慢全部请求

运行: python 07_debugging/02_asyncio_blocker.py
"""

import asyncio
import time
from collections.abc import Awaitable, Callable


async def quick_request(i: int) -> str:
    await asyncio.sleep(0.01)
    return f"quick-{i}"


async def blocking_request() -> str:
    # 错误示例：同步 sleep 占用事件循环线程。
    time.sleep(0.25)
    return "blocking"


async def fixed_request() -> str:
    # 修复：无法改成异步的阻塞调用放到线程池。
    await asyncio.to_thread(time.sleep, 0.25)
    return "fixed"


async def measure(label: str, slow: Callable[[], Awaitable[str]]) -> float:
    start = time.perf_counter()
    results = await asyncio.gather(slow(), *(quick_request(i) for i in range(10)))
    cost = time.perf_counter() - start
    print(f"  {label:34} {cost:.3f}s  完成 {len(results)} 个请求")
    return cost


async def main() -> None:
    print("=" * 64)
    print("同一个慢请求 + 10 个快请求")
    print("=" * 64)

    bad = await measure("同步阻塞 time.sleep", blocking_request)
    good = await measure("修复 asyncio.to_thread", fixed_request)

    print()
    print(f"修复后约快 {bad / good:.1f} 倍（快请求不再等待整个事件循环）")
    print("排查规则：所有非 await 的网络、DB、文件、sleep 都是嫌疑点。")
    print("生产定位：asyncio debug + faulthandler + 分段耗时日志。")


if __name__ == "__main__":
    asyncio.run(main())
