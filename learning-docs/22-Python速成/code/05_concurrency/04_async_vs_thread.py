"""04 · 阻塞调用对 asyncio 的破坏（最重要的一课）

运行: python 05_concurrency/04_async_vs_thread.py

这个脚本会实际演示：
  - async def 里调用 time.sleep / 同步函数 → 整个事件循环被卡住
  - 正确写法（asyncio.sleep / asyncio.to_thread）→ 真正并发
"""

import asyncio
import time


async def bad_handler(name: str) -> str:
    """错误：在协程里用同步阻塞调用"""
    time.sleep(0.3)                  # 卡死整个 event loop
    return f"{name}完成"


async def good_handler(name: str) -> str:
    """正确：用 asyncio.sleep"""
    await asyncio.sleep(0.3)
    return f"{name}完成"


async def thread_wrapped(name: str) -> str:
    """正确：阻塞调用丢到线程池"""
    await asyncio.to_thread(time.sleep, 0.3)
    return f"{name}完成"


def cpu_block(seconds: float) -> str:
    """模拟一个无法改造成异步的同步库调用"""
    time.sleep(seconds)
    return "ok"


async def main() -> None:
    print("=" * 60)
    print("1. 反例：async def 里 time.sleep")
    print("=" * 60)

    t0 = time.perf_counter()
    await asyncio.gather(*(bad_handler(f"T{i}") for i in range(5)))
    bad_cost = time.perf_counter() - t0
    print(f"  5 个各 0.3s 的『并发』任务，耗时 {bad_cost:.2f}s")
    print("  -> 应该 0.3s 左右，实际 1.5s：完全没有并发")
    print("  原因：time.sleep 不释放控制权，loop 无法切换任务")

    print()
    print("=" * 60)
    print("2. 正例：await asyncio.sleep")
    print("=" * 60)

    t0 = time.perf_counter()
    await asyncio.gather(*(good_handler(f"T{i}") for i in range(5)))
    good_cost = time.perf_counter() - t0
    print(f"  5 个各 0.3s 的并发任务，耗时 {good_cost:.2f}s")
    print(f"  -> 提速 {bad_cost/max(good_cost,1e-9):.1f} 倍")

    print()
    print("=" * 60)
    print("3. 正例：asyncio.to_thread 包装阻塞调用")
    print("=" * 60)

    t0 = time.perf_counter()
    await asyncio.gather(*(thread_wrapped(f"T{i}") for i in range(5)))
    thread_cost = time.perf_counter() - t0
    print(f"  5 个 to_thread 任务，耗时 {thread_cost:.2f}s   <- 也是并发的")

    print()
    print("=" * 60)
    print("4. 更严重的情况：一个阻塞调用拖垮所有请求")
    print("=" * 60)

    async def fast_request():
        await asyncio.sleep(0.01)
        return "fast"

    async def slow_blocking():
        time.sleep(0.5)              # 假设某个接口里有这一行
        return "slow"

    # 场景 A：全是快请求
    t0 = time.perf_counter()
    await asyncio.gather(*(fast_request() for _ in range(50)))
    print(f"  50 个快请求                 {time.perf_counter()-t0:.3f}s")

    # 场景 B：夹一个阻塞请求
    t0 = time.perf_counter()
    await asyncio.gather(slow_blocking(), *(fast_request() for _ in range(50)))
    print(f"  50 个快请求 + 1 个阻塞请求  {time.perf_counter()-t0:.3f}s")
    print("  -> 所有快请求都被那一个阻塞调用拖慢了")

    print()
    print("=" * 60)
    print("5. 判断标准")
    print("=" * 60)
    print("""
  审查 async 代码时只问一句：
    「这个调用是 await 的吗？」

  不是 await 的 IO 调用 → 一律是 bug：
    requests.get()          -> httpx.AsyncClient
    time.sleep()            -> asyncio.sleep
    open().read()           -> asyncio.to_thread(...)
    pymysql / psycopg2      -> asyncpg / aiomysql，或 to_thread
    boto3                   -> aioboto3，或 to_thread
    PIL / numpy 大计算      -> to_thread（甚至会释放 GIL）
""")

    print("=" * 60)
    print("6. 对照你项目里的 Java 经验")
    print("=" * 60)
    print("""
  Java（Reactor）：
    Mono.fromCallable(blocking) 必须 .subscribeOn(Schedulers.boundedElastic())
    → 否则阻塞调用占用调用方线程，.timeout() 计时器形同虚设

  Python（asyncio）：
    await asyncio.to_thread(blocking)
    → 否则整个 event loop 卡死，所有并发请求排队

  同一个道理：阻塞调用必须挪到专门的线程池。
  Python 里这个问题更严重——Java 至少有线程池兜底（Tomcat 每请求一线程），
  Python 的 asyncio 是单线程的，一个卡住就是全站卡住。
""")


if __name__ == "__main__":
    asyncio.run(main())
