"""04 · 生成器：惰性求值，省内存

运行: python 03_functions/04_generator.py
"""

import itertools
import sys
import time

print("=" * 60)
print("1. yield 是什么")
print("=" * 60)

def count_up(n):
    i = 0
    while i < n:
        yield i          # 产出值并挂起，下次从这里继续
        i += 1

print(f"  list(count_up(5)) = {list(count_up(5))}")

g = count_up(3)
print(f"\n  next(g) = {next(g)}")
print(f"  next(g) = {next(g)}")
print(f"  next(g) = {next(g)}")
try:
    next(g)
except StopIteration:
    print("  next(g) -> StopIteration   <- 生成器耗尽")


print("\n" + "=" * 60)
print("2. 内存对比：这是生成器的核心价值")
print("=" * 60)

N = 1_000_000
lst = [i * i for i in range(N)]
gen = (i * i for i in range(N))

print(f"  列表推导式 {N:,} 个元素  {sys.getsizeof(lst):>12,} 字节")
print(f"  生成器表达式             {sys.getsizeof(gen):>12,} 字节")
print("  生成器大小与 N 无关")

del lst

# 生成器能处理无限序列
def naturals():
    n = 0
    while True:
        yield n
        n += 1

print(f"\n  无限序列取前 10 个：{list(itertools.islice(naturals(), 10))}")


print("=" * 60)
print("3. 逐行读大文件（实战最重要）")
print("=" * 60)

def read_lines(path, encoding="utf-8"):
    with open(path, encoding=encoding) as f:
        for line in f:
            line = line.strip()
            if line:
                yield line

# 演示：用当前文件自己
demo = __file__
count = 0
for line in read_lines(demo):
    count += 1
print(f"  逐行读完本文件，共 {count} 行，内存恒定")


print("\n" + "=" * 60)
print("4. 生成器只能遍历一次")
print("=" * 60)

g = (x for x in range(3))
print(f"  sum(g) 第一次 = {sum(g)}")
print(f"  sum(g) 第二次 = {sum(g)}   <- 已耗尽，不会重置")
print("  需要多次遍历 → 用列表或重新创建")


print("=" * 60)
print("5. 生成器管道：像 Unix 管道一样组合")
print("=" * 60)

def numbers(limit):
    for i in range(1, limit + 1):
        yield i

def square(src):
    for x in src:
        yield x * x

def only_even(src):
    for x in src:
        if x % 2 == 0:
            yield x

pipeline = only_even(square(numbers(10)))
print(f"  numbers(10) -> square -> only_even = {list(pipeline)}")
print("  每一步都是惰性的，不产生中间列表")


print("=" * 60)
print("6. 性能：惰性带来的收益（提前退出）")
print("=" * 60)

def find_first_generator(data, pred):
    for x in data:
        if pred(x):
            return x
    return None

big = list(range(10_000_000))
t0 = time.perf_counter()
r = find_first_generator(big, lambda x: x > 5)
t_gen = time.perf_counter() - t0
print(f"  生成器式查找（找到就返回）: {t_gen*1000:.4f}ms -> {r}")

t0 = time.perf_counter()
r2 = [x for x in big if x > 5][0]
t_list = time.perf_counter() - t0
print(f"  先建列表再取第一个:        {t_list*1000:.4f}ms -> {r2}")
print(f"  慢了约 {t_list/max(t_gen, 1e-9):.0f} 倍")


print("=" * 60)
print("7. yield from：委托给子生成器")
print("=" * 60)

def inner():
    yield 1
    yield 2

def outer():
    yield 0
    yield from inner()      # 等价于 for x in inner(): yield x
    yield 3

print(f"  list(outer()) = {list(outer())}")


print("=" * 60)
print("8. 实战：分批处理（避免一次性加载）")
print("=" * 60)

def batch(items, size):
    buf = []
    for it in items:
        buf.append(it)
        if len(buf) == size:
            yield buf
            buf = []
    if buf:
        yield buf

print(f"  list(batch(range(1,8), 3)) = {list(batch(range(1, 8), 3))}")

print("\n  典型用法：")
print("    for chunk in batch(read_lines('huge.csv'), 1000):")
print("        bulk_insert(chunk)")


print("=" * 60)
print("9. 对照项目的 SSE 流式")
print("=" * 60)

# 同步生成器
def stream_text():
    for ch in ["你", "好", "，", "世", "界"]:
        yield ch

# SSE 分帧格式：data: xxx\n\n
frames = [f"data: {ch}\n\n" for ch in stream_text()]
print("  同步生成器 -> SSE 帧：")
for f in frames[:3]:
    print(f"    {f!r}")
print("    ...")

# 异步版本（ai-cs-py-chat 的真实写法）
import asyncio

async def astream_text():
    for ch in ["你", "好"]:
        await asyncio.sleep(0)
        yield ch

async def collect():
    out = []
    async for ch in astream_text():
        out.append(f"data: {ch}\n\n")
    return out

print(f"\n  异步版本：{asyncio.run(collect())}")
print("  注意：AsyncIterator 同样只能遍历一次（所以流式不能重试）")
