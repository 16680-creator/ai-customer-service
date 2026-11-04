"""04 · 阻塞调用对 asyncio 的破坏（最重要的一课）

运行: python 05_concurrency/04_async_vs_thread.py

这个脚本会实际演示：
  - async def 里调用 time.sleep / 同步函数 → 整个事件循环被卡住
  - 正确写法（asyncio.sleep / asyncio.to_thread）→ 真正并发
"""

# ---- 本段讲：为什么"async def 里写同步阻塞"是最坏的组合，以及两种修复方式 ----
# 核心一句话：asyncio 只有一个线程，阻塞它 = 阻塞所有并发请求。
# 三种 handler 的差别只在一行：
#   bad_handler  → time.sleep        ：占住线程死等，loop 完全停摆（错误）
#   good_handler → await asyncio.sleep：注册定时器后让出控制权（正确，真异步）
#   thread_wrapped → asyncio.to_thread：把阻塞函数丢进线程池，loop 继续转（正确，兼容同步库）

import asyncio                                         # 事件循环、gather、to_thread
import time                                            # 仅用于演示阻塞调用和计时


async def bad_handler(name: str) -> str:
    """错误：在协程里用同步阻塞调用"""
    # 这是反面教材：函数是 async 的，但内部没有任何 await，函数体会一口气占住线程。
    time.sleep(0.3)                  # 卡死整个 event loop
    # time.sleep 不会让出控制权，loop 在这 0.3s 内无法调度其它协程；
    # Java 里类似在 Reactor 的 event loop 线程上调用 Thread.sleep —— 所有请求一起卡住。
    return f"{name}完成"


async def good_handler(name: str) -> str:
    """正确：用 asyncio.sleep"""
    # await 让出控制权：当前协程挂起，loop 立刻去跑别的就绪协程，定时器到点再唤醒它。
    await asyncio.sleep(0.3)
    return f"{name}完成"


async def thread_wrapped(name: str) -> str:
    """正确：阻塞调用丢到线程池"""
    # to_thread 把同步函数提交给默认线程池执行，再用 await 等结果 —— loop 不会被阻塞。
    # 对应 Java 的 Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())
    await asyncio.to_thread(time.sleep, 0.3)
    # 注意参数是"函数对象 + 位置参数"，不要写成 time.sleep(0.3)，那会先同步执行再传结果
    return f"{name}完成"


def cpu_block(seconds: float) -> str:
    """模拟一个无法改造成异步的同步库调用"""
    # 代表 requests / pymysql / boto3 / PIL 这类只有同步 API 的库
    time.sleep(seconds)
    return "ok"


async def main() -> None:
    print("=" * 60)
    print("1. 反例：async def 里 time.sleep")
    print("=" * 60)

    t0 = time.perf_counter()          # 计时起点
    await asyncio.gather(*(bad_handler(f"T{i}") for i in range(5)))
    # *(生成器) 是解包：把 5 个协程对象作为 5 个位置参数传给 gather
    # 注意协程对象在创建时并未执行，gather 才真正调度它们
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
    # 5 个协程都在 await 处让出，等待区间互相重叠 → 总耗时 ≈ 0.3s
    good_cost = time.perf_counter() - t0
    print(f"  5 个各 0.3s 的并发任务，耗时 {good_cost:.2f}s")
    print(f"  -> 提速 {bad_cost/max(good_cost,1e-9):.1f} 倍")
    # max(good_cost, 1e-9) 防止除零（good_cost 理论上不会为 0，但这是安全的写法）

    print()
    print("=" * 60)
    print("3. 正例：asyncio.to_thread 包装阻塞调用")
    print("=" * 60)

    t0 = time.perf_counter()
    await asyncio.gather(*(thread_wrapped(f"T{i}") for i in range(5)))
    # 5 个协程各自把 sleep 丢到线程池，5 个线程并行睡 → 总耗时同样 ≈ 0.3s
    thread_cost = time.perf_counter() - t0
    print(f"  5 个 to_thread 任务，耗时 {thread_cost:.2f}s   <- 也是并发的")

    print()
    print("=" * 60)
    print("4. 更严重的情况：一个阻塞调用拖垮所有请求")
    print("=" * 60)

    async def fast_request():
        # 快请求：只等 0.01s（模拟一次缓存命中）
        await asyncio.sleep(0.01)
        return "fast"

    async def slow_blocking():
        # 慢且阻塞：模拟某个接口里混进了一行同步 sleep
        time.sleep(0.5)              # 假设某个接口里有这一行
        return "slow"

    # 场景 A：全是快请求
    t0 = time.perf_counter()
    await asyncio.gather(*(fast_request() for _ in range(50)))
    # 50 个协程并发，0.01s 的等待全部重叠 → 总耗时 ≈ 0.01s
    print(f"  50 个快请求                 {time.perf_counter()-t0:.3f}s")

    # 场景 B：夹一个阻塞请求
    t0 = time.perf_counter()
    await asyncio.gather(slow_blocking(), *(fast_request() for _ in range(50)))
    # 第一个位置参数是阻塞协程；它先被调度并卡住 0.5s，50 个快请求只能排队等
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
    # 启动事件循环运行 main()；run 结束时会取消所有残留任务并关闭 loop
    asyncio.run(main())
