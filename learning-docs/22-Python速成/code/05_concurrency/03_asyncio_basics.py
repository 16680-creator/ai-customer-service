"""03 · asyncio 基础：单线程协程并发

运行: python 05_concurrency/03_asyncio_basics.py
"""

# ---- 本段讲：asyncio 的六种核心用法（gather / 串行 await / TaskGroup / 超时 / as_completed / 异常）----
# 关键心智模型：整个程序只有【一个线程】和一个【事件循环】。
# await 是唯一的切换点：遇到 await 就把控制权交回 loop，loop 去跑别的就绪协程。
# 对照 Java：await ≈ CompletableFuture 的回调续接；asyncio.run ≈ 启动一个 Reactor 事件循环。

import asyncio                                          # 事件循环、gather、TaskGroup、wait_for 都在这里
import time                                             # 仅用 perf_counter 计时；注意别在协程里用 time.sleep


async def fetch(name: str, seconds: float) -> str:
    # async def 定义的是「协程函数」：直接调用它不会执行函数体，而是返回一个协程对象。
    # Java 里没有完全对等物；最接近的是返回 CompletableFuture 的方法（调用即返回，稍后才跑）。
    print(f"    {name} 开始")                             # 4 个空格缩进是字符串内容的一部分，用于输出对齐
    await asyncio.sleep(seconds)      # 非阻塞，让出控制权
    # asyncio.sleep 会向 loop 注册一个定时器并立刻 yield；到点后 loop 再唤醒本协程。
    # 千万别写成 time.sleep —— 那会占着线程死等，loop 无法切换，全站卡死。
    print(f"    {name} 结束")
    return f"{name}结果"              # 协程的返回值通过 await / gather 的结果拿到


async def demo_gather() -> None:
    # ---- 本段讲：gather 并发跑多个协程，总耗时取决于最慢的那个 ----
    print("=" * 60)
    print("1. gather：并发执行，等全部完成")
    print("=" * 60)

    t0 = time.perf_counter()          # 计时起点
    results = await asyncio.gather(
        fetch("A", 0.3),              # 直接传协程对象（不是调用结果），gather 内部会把它们包装成 Task
        fetch("B", 0.2),
        fetch("C", 0.1),
    )
    # gather 会同时调度三个协程，等全部完成后按【传入顺序】返回结果列表。
    # Java 对应：CompletableFuture.allOf(f1, f2, f3).join() 后再各自 get()
    print(f"  结果 {results}")         # 按传入顺序：[A结果, B结果, C结果]，与谁先完成无关
    print(f"  总耗时 {time.perf_counter()-t0:.2f}s   <- 约等于最长的 0.3s，不是 0.6s")


async def demo_sequential() -> None:
    # ---- 本段讲：连续 await 是串行的，不会自动并发 ----
    # 这是新手最常见的误解：以为写了 async/await 就"自动并行"。
    print()
    print("=" * 60)
    print("2. 串行 await：不并发")
    print("=" * 60)

    t0 = time.perf_counter()          # 计时起点
    a = await fetch("A", 0.3)         # 必须等 A 完全结束，才走到下一行
    b = await fetch("B", 0.2)         # 再等 B
    c = await fetch("C", 0.1)         # 再等 C
    # Java 对应：futureA.get(); futureB.get(); futureC.get(); —— 顺序阻塞，总耗时相加
    print(f"  结果 {[a, b, c]}")       # 手工装进列表，效果与 gather 相同但耗时翻倍
    print(f"  总耗时 {time.perf_counter()-t0:.2f}s   <- 累加 0.6s")


async def demo_taskgroup() -> None:
    # ---- 本段讲：3.11+ 的 TaskGroup（结构化并发），异常语义比 gather 更安全 ----
    print()
    print("=" * 60)
    print("3. TaskGroup（3.11+）：结构化并发")
    print("=" * 60)

    try:
        async with asyncio.TaskGroup() as tg:
            # async with 进入时创建 TaskGroup；退出代码块时自动等待组内所有任务完成。
            # 与 gather 的差别：任一任务失败会【取消其余任务】并抛 ExceptionGroup，
            # 不会留下"孤儿任务"在后台继续跑。Java 21 的 StructuredTaskScope 就是同一设计。
            t1 = tg.create_task(fetch("X", 0.1))    # create_task 立即把协程排进 loop，返回 Task
            t2 = tg.create_task(fetch("Y", 0.15))   # 两个任务并发执行
        print(f"  出块后全部完成：{t1.result()} {t2.result()}")
        # 出了 async with 才能安全读 result()，此时两个任务保证已完成
    except AttributeError:
        # 3.11 以下没有 asyncio.TaskGroup，访问属性会抛 AttributeError，这里降级跳过
        print("  当前 Python < 3.11，跳过 TaskGroup 演示")


async def demo_timeout() -> None:
    # ---- 本段讲：两种超时写法，以及"超时会取消内部协程"这一语义 ----
    print()
    print("=" * 60)
    print("4. 超时")
    print("=" * 60)

    try:
        await asyncio.wait_for(fetch("slow", 1.0), timeout=0.2)
        # wait_for(协程, timeout=秒)：最多等 0.2s，超时就取消内部协程并抛 TimeoutError。
        # Java 对应：future.get(200, TimeUnit.MILLISECONDS) 抛 TimeoutException
    except asyncio.TimeoutError:
        # 3.11 起 TimeoutError 已内置为 asyncio.TimeoutError 的别名，这里捕获的是同一个类
        print("  wait_for 超时 -> asyncio.TimeoutError")

    # 3.11+ 的 timeout 上下文管理器
    try:
        async with asyncio.timeout(0.1):
            # 整块代码共享同一个截止时间，块内任何 await 超时都会抛 TimeoutError
            # 适合"多处 await 总耗时不超过 N 秒"的场景，比逐个 wait_for 更精确
            await fetch("slow2", 0.5)
    except (asyncio.TimeoutError, AttributeError):
        # 同时兼容"确实超时"和"版本太低没有 asyncio.timeout"两种情况
        print("  asyncio.timeout 上下文管理器超时（或版本不支持）")


async def demo_asal_completed() -> None:
    # ---- 本段讲：谁先完成先处理（流式处理结果，不必等最慢的）----
    print()
    print("=" * 60)
    print("5. as_completed：谁先完成先处理")
    print("=" * 60)

    coros = [fetch(n, d) for n, d in [("slow", 0.3), ("mid", 0.2), ("fast", 0.1)]]
    # 列表推导式创建 3 个协程对象（此时还没开始执行，只是"待跑"状态）
    for fut in asyncio.as_completed(coros):
        # as_completed 每完成一个就 yield 一个可 await 的对象，顺序 = 完成顺序
        # Java 对应：ExecutorCompletionService.take()
        print(f"  先完成：{await fut}")   # 对 yield 出来的对象 await 才能拿到返回值


async def demo_exception() -> None:
    # ---- 本段讲：gather 的默认异常语义 vs return_exceptions=True ----
    print()
    print("=" * 60)
    print("6. 异常处理")
    print("=" * 60)

    async def boom():
        # 嵌套协程：睡 0.05s 后必定抛 ValueError，用来制造"部分失败"
        await asyncio.sleep(0.05)
        raise ValueError("炸了")       # 协程里抛异常，异常会存进 Task，await 时重新抛出

    # 默认：任一失败，gather 立刻抛
    try:
        await asyncio.gather(fetch("ok", 0.1), boom())
        # 默认行为：boom 抛错后 gather 立即向上抛，fetch("ok") 的结果被丢弃（但不会被取消）
        # Java 对应：CompletableFuture.allOf(...).join() 抛 CompletionException
    except ValueError as e:
        print(f"  gather 默认 -> ValueError: {e}")

    # return_exceptions=True：异常作为结果返回
    results = await asyncio.gather(fetch("ok2", 0.05), boom(), return_exceptions=True)
    # 加了 return_exceptions=True 后，gather 不再抛，而是把异常对象当成"结果"放进列表对应位置
    # Java 里没有直接等价物，通常要自己给每个 future 包 try/catch 收集
    for r in results:
        kind = type(r).__name__ if isinstance(r, Exception) else "值"
        # isinstance 判断是否为异常实例；type(x).__name__ 取类名字符串（等价 Java 的 x.getClass().getSimpleName()）
        print(f"    return_exceptions -> {kind}: {r}")


async def main() -> None:
    # 依次 await 六个演示函数：这里刻意用串行，保证输出顺序清晰可读
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
    # asyncio.run 会：新建事件循环 → 把 main() 协程跑完 → 关闭循环并清理残留任务。
    # 一个线程只能有一个运行中的 loop，所以整个程序入口只调一次。
    # Java 侧没有"必须由 run 启动"的约束，但 Reactor 也要求 subscribe 才会真正执行。
    asyncio.run(main())
