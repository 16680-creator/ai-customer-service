"""05 · asyncio 并发限流（对照 Java Semaphore）

运行: python 05_concurrency/05_async_semaphore.py
"""

# ---- 本段讲：三种 asyncio 限流方案 + 一个带重试/超时的实战调用封装 ----
# 为什么需要限流：asyncio 能瞬间创建上万个 Task，但下游（LLM、DB、第三方 API）扛不住。
#   Semaphore        ≈ java.util.concurrent.Semaphore（许可证计数）
#   Queue + worker   ≈ BlockingQueue + 固定线程数的消费者
#   分批 gather      ≈ CompletableFuture.allOf 分批 await
# 三者共同点：把"并发度"变成一个显式参数，而不是让它随任务数无限增长。

import asyncio                                         # Semaphore / Queue / gather / wait_for
import random                                          # random.uniform 生成随机延迟，模拟真实网络抖动
import time                                            # perf_counter 计时


async def api_call(i: int, delay: float | None = None) -> int:
    # delay 的类型是 float | None（3.10+ 联合类型写法），None 表示"未指定，用随机延迟"。
    # 三元表达式 a if cond else b 等价 Java 的 cond ? a : b。
    await asyncio.sleep(delay if delay is not None else random.uniform(0.05, 0.2))
    # random.uniform(0.05, 0.2) 返回区间内的随机浮点数，模拟每次请求耗时不同
    return i                                          # 原样返回入参，方便最后校验结果是否缺失


# ---------------------------------------------------------------------------
# 1. Semaphore 限流
# ---------------------------------------------------------------------------
async def limited_gather(n: int, limit: int) -> tuple[list[int], int]:
    # ---- 本段讲：用 Semaphore 把并发数压到 limit，并统计实际并发峰值 ----
    # 返回 (结果列表, 峰值并发数)；返回类型是 tuple[list[int], int]，等价 Java 的自定义 Pair。
    sem = asyncio.Semaphore(limit)
    # 信号量初值 = 允许同时进入的协程数；每 acquire 减 1，release 加 1，减到 0 就排队等待。
    # 对应 Java 的 new Semaphore(limit)，用法也几乎一样。
    peak = {"now": 0, "max": 0}
    # 用 dict 存"当前并发数 / 历史峰值"，因为内层函数要修改它（Python 对闭包变量重新赋值需 nonlocal）。

    async def guarded(i: int) -> int:
        # 包一层守卫函数：进入临界区前后做计数，用来验证限流是否真的生效
        async with sem:                     # 超过 limit 就等待
            # async with 会 await acquire()，退出时自动 release()（异常也会释放），
            # 等价 Java 的 try { sem.acquire(); ... } finally { sem.release(); }
            peak["now"] += 1                          # 当前并发 +1
            peak["max"] = max(peak["max"], peak["now"])   # 更新历史峰值
            try:
                return await api_call(i)              # 真正的业务调用（此处会 await 让出）
            finally:
                peak["now"] -= 1                      # 无论成功失败都要减回去，否则计数会漂移

    results = await asyncio.gather(*(guarded(i) for i in range(n)))
    # 一次性创建 n 个协程并交给 gather；虽然 Task 全建出来了，但 Semaphore 保证同时只有 limit 个在跑
    return results, peak["max"]


# ---------------------------------------------------------------------------
# 2. 队列版限流（生产者-消费者）
# ---------------------------------------------------------------------------
async def producer_consumer(n: int, workers: int) -> list[int]:
    # ---- 本段讲：固定数量 worker 消费队列，天然带"背压"（队列满则生产者等待）----
    # 与 Semaphore 版的最大区别：Task 数量恒定（workers 个），不会因为 n 很大而爆内存。
    q: asyncio.Queue[int] = asyncio.Queue(maxsize=workers * 2)
    # 有界队列；maxsize 是背压的关键。对应 Java 的 new ArrayBlockingQueue<>(capacity)。
    out: list[int] = []                               # 收集结果；list 追加是线程/协程安全的（单 loop 内无并发写）

    async def producer():
        # 生产者：快速把任务塞进队列
        for i in range(n):
            await q.put(i)              # 队列满时自动等待（背压）
            # put 是协程，队列满时会挂起生产者，直到消费者取走一个 → 内存不会无限增长
        for _ in range(workers):
            await q.put(-1)             # 哨兵
            # 每个 worker 一个 -1 作为"毒丸"，worker 读到它就退出；避免用取消或超时来收尾

    async def worker():
        # 消费者：循环取任务直到读到哨兵
        while True:
            item = await q.get()        # 队列空时挂起等待；对应 BlockingQueue.take()
            try:
                if item == -1:
                    return              # 收到哨兵，正常退出（注意 finally 仍会执行 task_done）
                out.append(await api_call(item))   # 处理任务并收集结果
            finally:
                q.task_done()           # 必须调用，否则 q.join() 永远阻塞
                # 对应 Java 的 CountDownLatch.countDown() 语义：告诉队列"这个任务处理完了"

    await asyncio.gather(producer(), *(worker() for _ in range(workers)))
    # 生产者 + workers 个消费者一起跑；gather 等所有 worker 读到哨兵退出后才返回
    return out


# ---------------------------------------------------------------------------
# 3. 批量分批处理（比一次性 gather 更适合海量任务）
# ---------------------------------------------------------------------------
async def batch_process(items: list[int], batch_size: int) -> list[int]:
    # ---- 本段讲：把大列表切成小批，一批一批 await，控制同时在飞的请求数 ----
    results = []                                      # 汇总所有批次的结果
    for i in range(0, len(items), batch_size):
        # range(start, stop, step)：每批的起始下标，步长 = 批大小
        chunk = items[i:i + batch_size]               # 切片取 [i, i+batch_size)，左闭右开，越界自动截断
        results.extend(await asyncio.gather(*(api_call(x) for x in chunk)))
        # gather 等这一批全部完成；extend 把整批结果摊平追加进 results
        # 代价：批与批之间是串行的，吞吐略低于持续满并发，但实现最简单、并发上限最直观
    return results


# ---------------------------------------------------------------------------
# 4. 带重试 + 超时的调用（项目里该有的样子）
# ---------------------------------------------------------------------------
async def resilient_call(i: int, *, retries: int = 3, timeout: float = 0.3) -> dict:
    # ---- 本段讲：超时 + 指数退避重试，是调用外部服务的最小可用封装 ----
    # 参数列表里的 * 表示其后全是"仅关键字参数"，调用时必须写 retries=... / timeout=...，
    # Java 没有这个语法，最接近的是 Builder 模式或强制命名的参数对象。
    last = None                                       # 记录最后一次异常，便于失败时报告类型
    for attempt in range(1, retries + 1):             # 尝试次数从 1 数到 retries（含）
        try:
            result = await asyncio.wait_for(api_call(i, delay=0.5 if i % 5 == 0 else 0.05),
                                            timeout=timeout)
            # i % 5 == 0 的少数任务故意给 0.5s 延迟 → 必然超时，用来演示重试与最终失败
            # wait_for 超时会取消内部协程并抛 asyncio.TimeoutError
            return {"i": i, "ok": True, "value": result, "attempt": attempt}
            # 成功即返回，带上"第几次成功"，方便观测重试效果
        except asyncio.TimeoutError as e:
            last = e                                  # 保存异常对象
            if attempt < retries:
                await asyncio.sleep(0.05 * 2 ** (attempt - 1))   # 指数退避
                # 退避序列：0.05s → 0.1s → 0.2s ...；** 是幂运算，等价 Java 的 Math.pow(2, n)
    return {"i": i, "ok": False, "error": type(last).__name__, "attempt": retries}
    # 全部尝试都失败：返回结构化结果而不是抛异常，方便调用方汇总（也可改为 raise）


async def main() -> None:
    print("=" * 60)
    print("1. Semaphore 限流")
    print("=" * 60)

    N, LIMIT = 50, 5                 # 50 个任务，最多 5 个并发（元组解包赋值，Python 特有写法）
    t0 = time.perf_counter()         # 计时起点
    results, peak = await limited_gather(N, LIMIT)   # 拿到结果与峰值并发
    cost = time.perf_counter() - t0
    print(f"  {N} 个任务，限流 {LIMIT}")
    print(f"  并发峰值 {peak}（应 <= {LIMIT}）")
    print(f"  结果数 {len(results)}，耗时 {cost:.2f}s")
    assert peak <= LIMIT, "限流失效"                    # 断言：峰值不得超过上限
    # assert 在生产环境可被 python -O 关掉，只适合做"自检/测试"，不要用它做业务校验
    assert sorted(results) == list(range(N)), "结果缺失"
    # 排序后与 0..N-1 比较，确认每个任务都恰好返回一次


    print()
    print("=" * 60)
    print("2. 队列 + 固定 worker（带背压）")
    print("=" * 60)

    t0 = time.perf_counter()
    out = await producer_consumer(20, workers=4)     # 20 个任务，4 个消费者
    print(f"  20 个任务 4 worker，耗时 {time.perf_counter()-t0:.2f}s")
    print(f"  结果数 {len(out)}")


    print()
    print("=" * 60)
    print("3. 分批处理（避免一次创建上万个 Task）")
    print("=" * 60)

    t0 = time.perf_counter()
    out = await batch_process(list(range(30)), batch_size=10)   # 30 个任务，每批 10 个
    print(f"  30 个任务，每批 10 个，耗时 {time.perf_counter()-t0:.2f}s")
    print(f"  结果数 {len(out)}")


    print()
    print("=" * 60)
    print("4. 重试 + 超时（对照项目 ResilientAiService）")
    print("=" * 60)

    results = await asyncio.gather(*(resilient_call(i) for i in range(1, 7)))
    # 1..6 六个任务并发跑；其中 5 的延迟被设成 0.5s，必然 3 次全超时 → 演示失败分支
    for r in results:
        if r["ok"]:
            print(f"  {r['i']}: 成功（第 {r['attempt']} 次）")
        else:
            print(f"  {r['i']}: 失败 {r['error']}（试了 {r['attempt']} 次）")


    print()
    print("=" * 60)
    print("5. 三种限流方案对比")
    print("=" * 60)
    print("""
  Semaphore
    + 最简洁，一行 async with
    + 天然的并发上限
    - 所有任务同时创建（Task 对象仍占内存）
    适用：任务量 <= 几千

  Queue + 固定 worker
    + 有背压（队列满时生产者等待）
    + 内存恒定
    - 代码略复杂，要处理哨兵
    适用：海量任务、生产消费模型

  分批 gather
    + 最简单，无额外并发原语
    - 批与批之间有等待（吞吐略低）
    适用：ETL、批处理脚本

  对照 Java：
    Semaphore          ≈ java.util.concurrent.Semaphore
    Queue              ≈ BlockingQueue
    分批 gather        ≈ CompletableFuture.allOf 分批
""")


if __name__ == "__main__":
    # 单线程事件循环跑 main()；所有并发都在这个 loop 里调度完成
    asyncio.run(main())
