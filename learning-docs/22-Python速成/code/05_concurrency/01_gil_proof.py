"""01 · GIL 实证：多线程跑 CPU 密集任务为什么没加速

运行: python 05_concurrency/01_gil_proof.py
预计耗时: 5-15 秒（取决于机器）

注意：需要 if __name__ == "__main__" 保护（Windows 多进程要求）
"""

import sys
import threading
import time
from concurrent.futures import ProcessPoolExecutor


def cpu_task(n: int) -> int:
    """纯 CPU 计算：无 IO、无等待"""
    total = 0
    for i in range(n):
        total += i * i
    return total


def io_task(seconds: float) -> str:
    """模拟 IO 等待：sleep 时会释放 GIL"""
    time.sleep(seconds)
    return "done"


def main() -> None:
    print(f"Python {sys.version.split()[0]} on {sys.platform}")
    print(f"CPU 核数: {__import__('os').cpu_count()}")
    print(f"切换间隔 sys.getswitchinterval() = {sys.getswitchinterval()}s")
    print()

    N = 4_000_000

    # ---------------- CPU 密集 ----------------
    print("=" * 60)
    print("CPU 密集任务：4 次 cpu_task(4_000_000)")
    print("=" * 60)

    t0 = time.perf_counter()
    for _ in range(4):
        cpu_task(N)
    serial = time.perf_counter() - t0
    print(f"  串行        {serial:.2f}s")

    t0 = time.perf_counter()
    threads = [threading.Thread(target=cpu_task, args=(N,)) for _ in range(4)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    threaded = time.perf_counter() - t0
    print(f"  4 线程      {threaded:.2f}s   加速比 {serial/threaded:.2f}x")

    t0 = time.perf_counter()
    with ProcessPoolExecutor(max_workers=4) as pool:
        list(pool.map(cpu_task, [N] * 4))
    multiproc = time.perf_counter() - t0
    print(f"  4 进程      {multiproc:.2f}s   加速比 {serial/multiproc:.2f}x")

    print()
    print(f"  结论：多线程加速比 ≈ 1（GIL 导致无法利用多核）")
    print(f"        多进程加速比 ≈ 核数（真并行）")

    # ---------------- IO 密集 ----------------
    print()
    print("=" * 60)
    print("IO 密集任务：8 次 io_task(0.3s)")
    print("=" * 60)

    WAIT = 0.3
    COUNT = 8

    t0 = time.perf_counter()
    for _ in range(COUNT):
        io_task(WAIT)
    io_serial = time.perf_counter() - t0
    print(f"  串行        {io_serial:.2f}s")

    t0 = time.perf_counter()
    threads = [threading.Thread(target=io_task, args=(WAIT,)) for _ in range(COUNT)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    io_threaded = time.perf_counter() - t0
    print(f"  8 线程      {io_threaded:.2f}s   加速比 {io_serial/io_threaded:.2f}x")

    print()
    print("  结论：sleep 会释放 GIL，所以 IO 等待能重叠，多线程有效")

    # ---------------- 线程数不是越多越好 ----------------
    print()
    print("=" * 60)
    print("补充：线程数增加并不缩短 CPU 任务（调度开销反而更慢）")
    print("=" * 60)
    for n_threads in (1, 2, 8):
        t0 = time.perf_counter()
        ts = [threading.Thread(target=cpu_task, args=(N // 4,)) for _ in range(n_threads)]
        for t in ts:
            t.start()
        for t in ts:
            t.join()
        print(f"  {n_threads} 线程跑 {n_threads} 份 1/4 任务: {time.perf_counter()-t0:.2f}s")

    print()
    print("=" * 60)
    print("总结")
    print("=" * 60)
    print("  CPU 密集 → ProcessPoolExecutor")
    print("  IO 密集  → asyncio（首选）或 ThreadPoolExecutor")
    print("  不确定   → 先写串行，测量，再决定")


if __name__ == "__main__":
    main()
