"""03 · asyncio 基础：单线程协程并发

运行: python 05_concurrency/03_asyncio_basics.py
"""

import asyncio
import time


async def fetch(name: str, seconds: float) -> str:
    print(f"    {name} 开始")
    await asyncio.sleep(seconds)      # 非阻塞，让出控制权
    print(f"    {name} 结束")
    return f"{name}结果"


async def demo_gather() -> None:
    print("=" * 60)
    print("1. gather：并发执行，等全部完成")
    print("=" * 60)

    t0 = time.perf_counter()
    results = await asyncio.gather(
        fetch("A", 0.3),
        fetch("B", 0.2),
        fetch("C", 0.1),
    )
    print(f"  结果 {results}")
    print(f"  总耗时 {time.perf_counter()-t0:.2f}s   <- 约等于最长的 0.3s，不是 0.6s")


async def demo_sequential() -> None:
    print()
    print("=" * 60)
    print("2. 串行 await：不并发")
    print("=" * 60)

    t0 = time.perf_counter()
    a = await fetch("A", 0.3)
    b = await fetch("B", 0.2)
    c = await fetch("C", 0.1)
    print(f"  结果 {[a, b, c]}")
    print(f"  总耗时 {time.perf_counter()-t0:.2f}s   <- 累加 0.6s")


async def demo_taskgroup() -> None:
    print()
    print("=" * 60)
    print("3. TaskGroup（3.11+）：结构化并发")
    print("=" * 60)

    try:
        async with asyncio.TaskGroup() as tg:
            t1 = tg.create_task(fetch("X", 0.1))
            t2 = tg.create_task(fetch("Y", 0.15))
        print(f"  出块后全部完成：{t1.result()} {t2.result()}")
    except AttributeError:
        print("  当前 Python < 3.11，跳过 TaskGroup 演示")


async def demo_timeout() -> None:
    print()
    print("=" * 60)
    print("4. 超时")
    print("=" * 60)

    try:
        await asyncio.wait_for(fetch("slow", 1.0), timeout=0.2)
    except asyncio.TimeoutError:
        print("  wait_for 超时 -> asyncio.TimeoutError")

    # 3.11+ 的 timeout 上下文管理器
    try:
        async with asyncio.timeout(0.1):
            await fetch("slow2", 0.5)
    except (asyncio.TimeoutError, AttributeError):
        print("  asyncio.timeout 上下文管理器超时（或版本不支持）")


async def demo_asal_completed() -> None:
    print()
    print("=" * 60)
    print("5. as_completed：谁先完成先处理")
    print("=" * 60)

    coros = [fetch(n, d) for n, d in [("slow", 0.3), ("mid", 0.2), ("fast", 0.1)]]
    for fut in asyncio.as_completed(coros):
        print(f"  先完成：{await fut}")


async def demo_exception() -> None:
    print()
    print("=" * 60)
    print("6. 异常处理")
    print("=" * 60)

    async def boom():
        await asyncio.sleep(0.05)
        raise ValueError("炸了")

    # 默认：任一失败，gather 立刻抛
    try:
        await asyncio.gather(fetch("ok", 0.1), boom())
    except ValueError as e:
        print(f"  gather 默认 -> ValueError: {e}")

    # return_exceptions=True：异常作为结果返回
    results = await asyncio.gather(fetch("ok2", 0.05), boom(), return_exceptions=True)
    for r in results:
        kind = type(r).__name__ if isinstance(r, Exception) else "值"
        print(f"    return_exceptions -> {kind}: {r}")


async def main() -> None:
    await demo_gather()
    await demo_sequential()
    await demo_taskgroup()
    await demo_timeout()
    await demo_asal_completed()
    await demo_exception()

    print()
    print("=" * 60)
    print("总结")
    print("=" * 60)
    print("  await 是唯一的让出点 → 任何不是 await 的阻塞调用都会卡死 loop")
    print("  gather / TaskGroup / as_completed / wait_for 是四个核心组合子")
    print("  对照 Java：gather≈allOf, as_completed≈CompletionService,")
    print("            wait_for≈future.get(timeout), TaskGroup≈StructuredTaskScope")


if __name__ == "__main__":
    asyncio.run(main())
