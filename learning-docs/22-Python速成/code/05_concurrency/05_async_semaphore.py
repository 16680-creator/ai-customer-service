"""05 · asyncio 并发限流（对照 Java Semaphore）

运行: python 05_concurrency/05_async_semaphore.py
"""

import asyncio
import random
import time


async def api_call(i: int, delay: float | None = None) -> int:
    await asyncio.sleep(delay if delay is not None else random.uniform(0.05, 0.2))
    return i


# ---------------------------------------------------------------------------
# 1. Semaphore 限流
# ---------------------------------------------------------------------------
async def limited_gather(n: int, limit: int) -> tuple[list[int], int]:
    sem = asyncio.Semaphore(limit)
    peak = {"now": 0, "max": 0}

    async def guarded(i: int) -> int:
        async with sem:                     # 超过 limit 就等待
            peak["now"] += 1
            peak["max"] = max(peak["max"], peak["now"])
            try:
                return await api_call(i)
            finally:
                peak["now"] -= 1

    results = await asyncio.gather(*(guarded(i) for i in range(n)))
    return results, peak["max"]


# ---------------------------------------------------------------------------
# 2. 队列版限流（生产者-消费者）
# ---------------------------------------------------------------------------
async def producer_consumer(n: int, workers: int) -> list[int]:
    q: asyncio.Queue[int] = asyncio.Queue(maxsize=workers * 2)
    out: list[int] = []

    async def producer():
        for i in range(n):
            await q.put(i)              # 队列满时自动等待（背压）
        for _ in range(workers):
            await q.put(-1)             # 哨兵

    async def worker():
        while True:
            item = await q.get()
            try:
                if item == -1:
                    return
                out.append(await api_call(item))
            finally:
                q.task_done()           # 必须调用，否则 q.join() 永远阻塞

    await asyncio.gather(producer(), *(worker() for _ in range(workers)))
    return out


# ---------------------------------------------------------------------------
# 3. 批量分批处理（比一次性 gather 更适合海量任务）
# ---------------------------------------------------------------------------
async def batch_process(items: list[int], batch_size: int) -> list[int]:
    results = []
    for i in range(0, len(items), batch_size):
        chunk = items[i:i + batch_size]
        results.extend(await asyncio.gather(*(api_call(x) for x in chunk)))
    return results


# ---------------------------------------------------------------------------
# 4. 带重试 + 超时的调用（项目里该有的样子）
# ---------------------------------------------------------------------------
async def resilient_call(i: int, *, retries: int = 3, timeout: float = 0.3) -> dict:
    last = None
    for attempt in range(1, retries + 1):
        try:
            result = await asyncio.wait_for(api_call(i, delay=0.5 if i % 5 == 0 else 0.05),
                                            timeout=timeout)
            return {"i": i, "ok": True, "value": result, "attempt": attempt}
        except asyncio.TimeoutError as e:
            last = e
            if attempt < retries:
                await asyncio.sleep(0.05 * 2 ** (attempt - 1))   # 指数退避
    return {"i": i, "ok": False, "error": type(last).__name__, "attempt": retries}


async def main() -> None:
    print("=" * 60)
    print("1. Semaphore 限流")
    print("=" * 60)

    N, LIMIT = 50, 5
    t0 = time.perf_counter()
    results, peak = await limited_gather(N, LIMIT)
    cost = time.perf_counter() - t0
    print(f"  {N} 个任务，限流 {LIMIT}")
    print(f"  并发峰值 {peak}（应 <= {LIMIT}）")
    print(f"  结果数 {len(results)}，耗时 {cost:.2f}s")
    assert peak <= LIMIT, "限流失效"
    assert sorted(results) == list(range(N)), "结果缺失"


    print()
    print("=" * 60)
    print("2. 队列 + 固定 worker（带背压）")
    print("=" * 60)

    t0 = time.perf_counter()
    out = await producer_consumer(20, workers=4)
    print(f"  20 个任务 4 worker，耗时 {time.perf_counter()-t0:.2f}s")
    print(f"  结果数 {len(out)}")


    print()
    print("=" * 60)
    print("3. 分批处理（避免一次创建上万个 Task）")
    print("=" * 60)

    t0 = time.perf_counter()
    out = await batch_process(list(range(30)), batch_size=10)
    print(f"  30 个任务，每批 10 个，耗时 {time.perf_counter()-t0:.2f}s")
    print(f"  结果数 {len(out)}")


    print()
    print("=" * 60)
    print("4. 重试 + 超时（对照项目 ResilientAiService）")
    print("=" * 60)

    results = await asyncio.gather(*(resilient_call(i) for i in range(1, 7)))
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
    asyncio.run(main())
