"""02 · 线程池：IO 密集任务的实用方案

运行: python 05_concurrency/02_thread_io.py
"""

import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed


def fake_http_get(url: str, delay: float = 0.2) -> dict:
    """模拟 HTTP 请求：time.sleep 会释放 GIL"""
    time.sleep(delay)
    return {"url": url, "status": 200, "len": len(url)}


def cpu_task(n: int) -> int:
    return sum(i * i for i in range(n))


def main() -> None:
    print("=" * 60)
    print("1. map：按提交顺序返回结果")
    print("=" * 60)

    urls = [f"https://api/x/{i}" for i in range(6)]

    t0 = time.perf_counter()
    serial = [fake_http_get(u) for u in urls]
    print(f"  串行 6 个请求          {time.perf_counter()-t0:.2f}s")

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=6) as pool:
        results = list(pool.map(fake_http_get, urls))
    print(f"  线程池 6 并发          {time.perf_counter()-t0:.2f}s")
    print(f"  结果顺序与输入一致:    {[r['url'][-1] for r in results]}")


    print()
    print("=" * 60)
    print("2. submit + as_completed：谁先完成先处理")
    print("=" * 60)

    tasks = [
        ("fast", 0.1), ("medium", 0.3), ("slow", 0.5),
        ("a", 0.2), ("b", 0.4),
    ]

    with ThreadPoolExecutor(max_workers=5) as pool:
        futures = {
            pool.submit(fake_http_get, name, d): name
            for name, d in tasks
        }
        order = []
        for fut in as_completed(futures):
            name = futures[fut]
            order.append(name)
            print(f"  完成 {name:8} (期望延迟 {dict(tasks)[name]}s)")
        print(f"\n  完成顺序: {order}   <- 按耗时，不是提交顺序")


    print()
    print("=" * 60)
    print("3. 异常处理：一个失败不影响其他")
    print("=" * 60)

    def may_fail(i):
        if i == 2:
            raise ValueError(f"第 {i} 个失败了")
        return i

    with ThreadPoolExecutor(max_workers=4) as pool:
        futures = {pool.submit(may_fail, i): i for i in range(5)}
        for fut in as_completed(futures):
            i = futures[fut]
            try:
                print(f"  {i} -> {fut.result()}")
            except ValueError as e:
                print(f"  {i} -> 捕获：{e}")


    print()
    print("=" * 60)
    print("4. 共享状态必须加锁（GIL 不等于线程安全）")
    print("=" * 60)

    counter = {"n": 0}

    def inc_unsafe(times):
        for _ in range(times):
            counter["n"] += 1        # 读-改-写三步，可能被打断

    counter["n"] = 0
    ts = [threading.Thread(target=inc_unsafe, args=(100_000,)) for _ in range(4)]
    for t in ts:
        t.start()
    for t in ts:
        t.join()
    print(f"  不加锁 4x100000 -> {counter['n']}  (期望 400000)")

    lock = threading.Lock()

    def inc_safe(times):
        for _ in range(times):
            with lock:
                counter["n"] += 1

    counter["n"] = 0
    ts = [threading.Thread(target=inc_safe, args=(100_000,)) for _ in range(4)]
    for t in ts:
        t.start()
    for t in ts:
        t.join()
    print(f"  加锁   4x100000 -> {counter['n']}  (正确)")


    print()
    print("=" * 60)
    print("5. 避免：用线程池跑 CPU 密集任务")
    print("=" * 60)

    N = 3_000_000

    t0 = time.perf_counter()
    for _ in range(4):
        cpu_task(N)
    serial = time.perf_counter() - t0
    print(f"  串行 4 次      {serial:.2f}s")

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(cpu_task, [N] * 4))
    threaded = time.perf_counter() - t0
    print(f"  4 线程         {threaded:.2f}s   加速比 {serial/threaded:.2f}x")
    print("  加速比接近 1 → 说明该用进程池（见 03_asyncio_basics 或 01_gil_proof）")


    print()
    print("=" * 60)
    print("6. 实战：并发请求 + 超时控制")
    print("=" * 60)

    def slow_call(i):
        time.sleep(0.5)
        return i

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=3) as pool:
        futures = [pool.submit(slow_call, i) for i in range(6)]
        done, not_done = [], []
        for f in futures:
            if f.done() or f.result(timeout=None) is not None:
                pass
        results = [f.result() for f in futures]
    print(f"  6 个 0.5s 任务，3 并发 -> {time.perf_counter()-t0:.2f}s  结果 {results}")
    print("  注意：ThreadPoolExecutor 没有内置超时取消，需要自己管理 future")


if __name__ == "__main__":
    main()
