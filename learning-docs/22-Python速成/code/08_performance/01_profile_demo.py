"""性能分析：基准测试 + cProfile 热点

运行: python 08_performance/01_profile_demo.py
"""

import cProfile
import pstats
import time
from timeit import timeit


def membership_with_list(ids: list[int], allowed: list[int]) -> int:
    return sum(1 for value in ids if value in allowed)


def membership_with_set(ids: list[int], allowed: set[int]) -> int:
    return sum(1 for value in ids if value in allowed)


def concatenate_naive(parts: list[str]) -> str:
    out = ""
    for part in parts:
        out += part
    return out


def concatenate_join(parts: list[str]) -> str:
    return "".join(parts)


def benchmark() -> None:
    print("=" * 64)
    print("1. 微基准：保持相同输入，跑多轮比较")
    print("=" * 64)

    setup = "data = list(range(10_000))"
    loop = timeit("out = []\nfor x in data: out.append(x * 2)", setup=setup, number=30)
    comp = timeit("[x * 2 for x in data]", setup=setup, number=30)
    print(f"  for + append: {loop:.4f}s / 30 轮")
    print(f"  推导式:       {comp:.4f}s / 30 轮")
    print(f"  相对速度:     {loop / comp:.2f}x")

    ids = list(range(2000))
    allowed_list = list(range(1000, 3000))
    allowed_set = set(allowed_list)

    t0 = time.perf_counter()
    count1 = membership_with_list(ids, allowed_list)
    list_cost = time.perf_counter() - t0

    t0 = time.perf_counter()
    count2 = membership_with_set(ids, allowed_set)
    set_cost = time.perf_counter() - t0

    print(f"\n  list 成员判断: {list_cost:.6f}s，命中 {count1}")
    print(f"  set 成员判断:  {set_cost:.6f}s，命中 {count2}")
    print(f"  结果一致: {count1 == count2}，加速约 {list_cost / max(set_cost, 1e-9):.0f}x")


def profile_target() -> None:
    """模拟一个业务流程：成员判断、拼接、简单计算。"""
    ids = list(range(2000))
    allowed = list(range(1000, 3000))
    parts = [f"row-{i}," for i in range(3000)]

    for _ in range(3):
        membership_with_list(ids, allowed)
        concatenate_join(parts)
        sum(i * i for i in range(50_000))


def profile() -> None:
    print()
    print("=" * 64)
    print("2. cProfile：先找热点，不要先猜")
    print("=" * 64)

    profiler = cProfile.Profile()
    profiler.enable()
    profile_target()
    profiler.disable()

    stats = pstats.Stats(profiler).strip_dirs().sort_stats("tottime")
    stats.print_stats(10)

    print("读法：tottime 高看函数自身；cumtime 高看子调用；ncalls 异常高看重复工作。")


if __name__ == "__main__":
    benchmark()
    profile()
