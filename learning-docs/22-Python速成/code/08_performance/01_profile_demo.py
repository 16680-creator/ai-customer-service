"""性能分析：基准测试 + cProfile 热点

运行: python 08_performance/01_profile_demo.py
"""

# ---- 本段讲：cProfile 找热点 + 微基准对比 ----
# 性能优化的铁律：先测量，再优化。cProfile 相当于 Java 世界的 JProfiler/async-profiler，
# 用来定位「时间花在哪个函数」；timeit 相当于 JMH，用来做公平的微基准对比。

import cProfile    # 标准库性能分析器，等价 Java 用 -agentpath 挂上 profiler（但这是纯 Python 层面的采样）
import pstats      # 配套的统计报告模块：把 Profile 的原始数据排序、格式化输出
import time        # 提供 perf_counter 高精度计时器（等价 Java 的 System.nanoTime）
from timeit import timeit  # timeit：专门做微基准的函数，会自动多轮执行并取总耗时


def membership_with_list(ids: list[int], allowed: list[int]) -> int:
    # `value in allowed`：列表的 in 是线性扫描 O(n)，等价 Java 的 allowed.contains(v)
    # （ArrayList.contains 也是 O(n)）。生成器表达式 sum(...) 逐个累加满足条件的个数。
    return sum(1 for value in ids if value in allowed)


def membership_with_set(ids: list[int], allowed: set[int]) -> int:
    # set 的 in 是哈希查找 O(1)，等价 Java 的 HashSet.contains(v)。
    # 逻辑完全一样，唯一差别是容器的类型——这就是接下来要测量的差异来源。
    return sum(1 for value in ids if value in allowed)


def concatenate_naive(parts: list[str]) -> str:
    # 反例：循环里用 += 拼字符串。Python 字符串不可变，每次 += 都要新建整个字符串，
    # 整体是 O(n²)——和 Java 里在循环中用 String.concat 一个道理，
    # Java 的正确做法是 StringBuilder，Python 的正确做法是下面的 "".join。
    out = ""
    for part in parts:  # 遍历列表每个元素，等价 Java 的 for (String part : parts)
        out += part     # 不可变对象 += 会产生新对象，旧的全部丢弃，越拼越慢
    return out


def concatenate_join(parts: list[str]) -> str:
    # 正解：join 一次性分配结果字符串，O(n)。等价 Java 的 String.join("", parts)
    # 或 StringBuilder.toString() 的最终一次拷贝。
    return "".join(parts)


def benchmark() -> None:
    # ---- 段落：微基准部分 ----
    # 演示两种测量手段：timeit（自动多轮、隔离变量）和 perf_counter（手工打点）。
    # 对应 Java：timeit ≈ JMH 的 @Benchmark，perf_counter ≈ System.nanoTime 手工计时。
    print("=" * 64)                       # 字符串乘法：64 个 '=' 组成的分隔线（Java 要写循环或 repeat）
    print("1. 微基准：保持相同输入，跑多轮比较")
    print("=" * 64)

    # setup 参数：每轮计时前执行一次的准备代码，相当于 JMH 的 @Setup，
    # 作用是让所有对比项从完全相同的输入 data 出发，保证公平。
    setup = "data = list(range(10_000))"
    # 第一项：命令式写法——空列表 + for 循环 + append（等价 Java 的 new + add）。
    # "number=30" 表示整段代码跑 30 遍，返回总耗时（秒，float）。
    # 代码以字符串形式传入，因此内部换行要用 \n 写在同一个字符串里。
    loop = timeit("out = []\nfor x in data: out.append(x * 2)", setup=setup, number=30)
    # 第二项：列表推导式，功能等价但由解释器用专用字节码实现，通常更快。
    comp = timeit("[x * 2 for x in data]", setup=setup, number=30)
    # f-string 格式化输出，:.4f 表示保留 4 位小数（等价 Java 的 String.format("%.4f", ...)）
    print(f"  for + append: {loop:.4f}s / 30 轮")
    print(f"  推导式:       {comp:.4f}s / 30 轮")
    print(f"  相对速度:     {loop / comp:.2f}x")   # 两个耗时相除得到倍数

    # ---- 段落：list vs set 的成员判断基准 ----
    # 用 perf_counter 手工计时对比两种容器。注意两组的 ids 相同、允许集合内容相同，
    # 只有容器类型不同——控制变量的做法和 JMH 的多参数 @Param 思路一致。
    ids = list(range(2000))               # 待查的 2000 个 id
    allowed_list = list(range(1000, 3000))  # 允许集合存成 list：in 为 O(n)
    allowed_set = set(allowed_list)       # 同样的元素存成 set：in 为 O(1)（等价 new HashSet<>(list)）

    t0 = time.perf_counter()              # 高精度单调时钟起点，比 time.time 更适合测量
    count1 = membership_with_list(ids, allowed_list)
    list_cost = time.perf_counter() - t0  # 结束时间减去起点 = 本次耗时（秒）

    t0 = time.perf_counter()              # 重置起点，测量第二组
    count2 = membership_with_set(ids, allowed_set)
    set_cost = time.perf_counter() - t0

    # 输出两个耗时和命中数；\n 在 f-string 里表示先空一行
    print(f"\n  list 成员判断: {list_cost:.6f}s，命中 {count1}")
    print(f"  set 成员判断:  {set_cost:.6f}s，命中 {count2}")
    # max(set_cost, 1e-9)：防止除以 0（耗时可能极小）；1e-9 即 1 纳秒的下限保护
    print(f"  结果一致: {count1 == count2}，加速约 {list_cost / max(set_cost, 1e-9):.0f}x")


def profile_target() -> None:
    """模拟一个业务流程：成员判断、拼接、简单计算。"""
    # 这是被分析的目标函数：故意混合了三类操作（线性查找、字符串拼接、数值累加），
    # 让 cProfile 的报告里有明确的「热点」可以找。等价 Java 中被 JProfiler 观察的线程入口。
    ids = list(range(2000))                    # 成员判断的输入
    allowed = list(range(1000, 3000))          # 故意用 list（O(n)），制造热点
    parts = [f"row-{i}," for i in range(3000)]  # 3000 个待拼接的字符串片段

    for _ in range(3):                    # 循环 3 次放大耗时，让统计更稳定；_ 表示不用循环变量
        membership_with_list(ids, allowed)     # 热点①：list 线性查找
        concatenate_join(parts)                # 热点②：字符串拼接（已用 join 的正确写法）
        sum(i * i for i in range(50_000))      # 热点③：纯计算，做对照组


def profile() -> None:
    # ---- 段落：cProfile 的标准使用套路 ----
    # 三步走：enable 开启记录 → 跑目标代码 → disable 停止。
    # 对应 Java：启动 profiler → 执行业务 → dump 火焰图/调用树。
    print()
    print("=" * 64)
    print("2. cProfile：先找热点，不要先猜")
    print("=" * 64)

    profiler = cProfile.Profile()    # 创建分析器实例（此刻还未开始记录）
    profiler.enable()                # 开始记录：之后所有函数调用都会被计时统计
    profile_target()                 # 被分析的目标代码
    profiler.disable()               # 停止记录：之后调用不再统计

    # pstats.Stats 包装统计结果；strip_dirs() 把路径截短方便阅读；
    # sort_stats("tottime") 按「函数自身耗时（不含子调用）」降序——
    # 这是最常用的排序，因为热点往往在自己身上而不在调用库上。
    stats = pstats.Stats(profiler).strip_dirs().sort_stats("tottime")
    stats.print_stats(10)            # 打印前 10 行报告

    print("读法：tottime 高看函数自身；cumtime 高看子调用；ncalls 异常高看重复工作。")


# ---- 段落：入口守卫 ----
# `__name__ == "__main__"` 等价 Java 的 public static void main：
# 只有直接运行本文件时才为真；被 import 时不执行，避免副作用。
if __name__ == "__main__":
    benchmark()    # 先跑微基准：对比语言级写法差异
    profile()      # 再跑 cProfile：演示如何定位真实热点
