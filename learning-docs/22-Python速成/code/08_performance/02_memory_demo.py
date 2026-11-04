"""内存分析：全量列表与流式生成器的差异

运行: python 08_performance/02_memory_demo.py
"""

# ---- 本段讲：用 tracemalloc 测内存，对比「全量装列表」与「流式生成器」两种数据管道 ----
# tracemalloc 相当于 Java 侧的内存分析工具（如 VisualVM 的 Memory 采样、JProfiler 的分配记录），
# 区别是它只统计 Python 对象层的分配，不是整个进程的物理内存（RSS）。

import tracemalloc  # 标准库内存跟踪器：记录 Python 层的每次分配，能报告「当前占用」和「峰值」


def make_rows_list(n: int) -> list[dict]:
    # 急切版本：一次性把 n 行 dict 全部构造出来放进列表，n 行数据同时驻留内存。
    # 列表推导式逐个生成 {"id":..., "text":..., "payload":...}；"x" * 80 是字符串乘法，得到 80 个 'x'。
    # Java 里相当于先把全部记录塞进 ArrayList 再处理——数据量大时会撑爆堆（OOM 的常见来源）。
    return [{"id": i, "text": f"order-{i}", "payload": "x" * 80} for i in range(n)]


def make_rows_iter(n: int):
    # 惰性版本：函数体里出现 yield，它就不再是普通函数而是「生成器函数」（等价 Java 的 Stream/迭代器）。
    # 调用时函数体不执行，每次 for 循环来取值才执行到下一个 yield，同时内存里只有当前这一行。
    # Java 对应：返回 Stream<Row>，或实现 Iterable 逐条产出，而不是先收集完再返回 List。
    for i in range(n):
        yield {"id": i, "text": f"order-{i}", "payload": "x" * 80}  # 产出第 i 行，然后挂起等下次取


def process_list(n: int) -> int:
    # 消费「全量列表」：先把 n 行全部拿到内存，再逐行累加 id。
    # row["id"] 是 dict 取值，等价 Java 的 map.get("id")；sum(生成器) 边遍历边累加。
    rows = make_rows_list(n)
    return sum(row["id"] for row in rows)


def process_iter(n: int) -> int:
    # 消费「流式生成器」：边产出边累加，任何时刻内存里只有一行。
    # 返回结果与上一版完全相同——性能优化的前提是不改变功能语义。
    return sum(row["id"] for row in make_rows_iter(n))


def measure(label: str, fn, n: int) -> tuple[int, int]:
    # 通用测量函数：对传入的处理函数 fn 开启 tracemalloc → 执行 → 读取峰值 → 关闭。
    # 把「测量」从「业务」中拆出来，和 JMH 的 benchmark 方法包裹业务代码是同一个思路。
    tracemalloc.start()                       # 开始跟踪 Python 层内存分配（有一定性能开销，只在测量时开）
    result = fn(n)                            # 执行业务：传函数对象像 Java 传 Runnable/Supplier
    current, peak = tracemalloc.get_traced_memory()  # 返回 (当前仍在占用的字节数, 期间峰值字节数)
    tracemalloc.stop()                        # 停止跟踪并丢弃统计，避免影响后续测量
    # :18/:8.1 是格式说明：label 左对齐占 18 列，KiB 数右对齐占 8 列保留 1 位小数；/1024 把字节转成 KiB
    print(f"  {label:18} 当前 {current / 1024:8.1f} KiB  峰值 {peak / 1024:8.1f} KiB  结果 {result}")
    return current, peak                      # 峰值给调用方做倍数对比用


def main() -> None:
    # ---- 段落：入口——两种实现各测一次，对比峰值内存 ----
    n = 50_000                                # 数据规模；5 万行足以让列表版峰值明显领先（下划线只是千位分隔符）
    print("=" * 64)
    print(f"处理 {n:,} 行，tracemalloc 只统计 Python 跟踪到的分配")   # :, 给数字加千位逗号，等价 Java 的 %,d 手写
    print("=" * 64)

    # 只取返回元组的第二个元素（峰值），第一个用 _ 丢弃——这是解包时忽略某项的惯用写法。
    _, list_peak = measure("全量 list", process_list, n)      # 列表版：n 行同时驻留
    _, iter_peak = measure("流式 generator", process_iter, n)  # 生成器版：一次一行

    print()
    # max(iter_peak, 1)：防除以 0；输出列表版峰值是生成器版的多少倍
    print(f"列表版本峰值约为生成器的 {list_peak / max(iter_peak, 1):.0f} 倍")
    print("两种实现的返回结果相同，差别在是否同时保留全部行。")
    print("注意：tracemalloc 不等于进程 RSS；C 扩展和解释器开销另算。")
    # 最后一行提醒：tracemalloc 只看 Python 对象，等价 Java 里区别「堆内对象」和「直接内存/元空间」。


# ---- 段落：入口守卫 ----
# 与 Java 的 main 方法同义：直接运行本文件才执行；被 import 时不产生副作用。
if __name__ == "__main__":
    main()
