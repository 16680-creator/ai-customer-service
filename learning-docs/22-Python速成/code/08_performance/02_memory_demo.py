"""内存分析：全量列表与流式生成器的差异

运行: python 08_performance/02_memory_demo.py
"""

import tracemalloc


def make_rows_list(n: int) -> list[dict]:
    return [{"id": i, "text": f"order-{i}", "payload": "x" * 80} for i in range(n)]


def make_rows_iter(n: int):
    for i in range(n):
        yield {"id": i, "text": f"order-{i}", "payload": "x" * 80}


def process_list(n: int) -> int:
    rows = make_rows_list(n)
    return sum(row["id"] for row in rows)


def process_iter(n: int) -> int:
    return sum(row["id"] for row in make_rows_iter(n))


def measure(label: str, fn, n: int) -> tuple[int, int]:
    tracemalloc.start()
    result = fn(n)
    current, peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()
    print(f"  {label:18} 当前 {current / 1024:8.1f} KiB  峰值 {peak / 1024:8.1f} KiB  结果 {result}")
    return current, peak


def main() -> None:
    n = 50_000
    print("=" * 64)
    print(f"处理 {n:,} 行，tracemalloc 只统计 Python 跟踪到的分配")
    print("=" * 64)

    _, list_peak = measure("全量 list", process_list, n)
    _, iter_peak = measure("流式 generator", process_iter, n)

    print()
    print(f"列表版本峰值约为生成器的 {list_peak / max(iter_peak, 1):.0f} 倍")
    print("两种实现的返回结果相同，差别在是否同时保留全部行。")
    print("注意：tracemalloc 不等于进程 RSS；C 扩展和解释器开销另算。")


if __name__ == "__main__":
    main()
