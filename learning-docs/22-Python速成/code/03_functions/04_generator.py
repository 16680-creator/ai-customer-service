"""04 · 生成器：惰性求值，省内存

运行: python 03_functions/04_generator.py
"""

import itertools
import sys
import time

# ---- 本文件讲：生成器（yield）—— Python 版的「惰性序列 + 迭代器」 ----
# 一句话理解：普通函数 return 一次就结束；生成器函数每次 yield 暂停并交出值，下次 next() 从暂停处继续。
# Java 对照：没有原生 yield，只能用 Iterator + 手写状态机，或 Stream 的惰性中间操作模拟。
# 生产价值：读大文件/流式响应时内存恒定，这是 ai-cs-py-chat 里 SSE 流式输出的基础。
print("=" * 60)
print("1. yield 是什么")
print("=" * 60)

def count_up(n):
    # 只要函数体里有 yield，这个函数就变成「生成器函数」：调用它不会执行函数体，而是返回一个生成器对象。
    i = 0
    while i < n:
        yield i          # 产出值并挂起，下次从这里继续（i 的值、循环位置都被保存在生成器帧里）
        i += 1

print(f"  list(count_up(5)) = {list(count_up(5))}")   # list() 会不断 next() 直到耗尽 → [0,1,2,3,4]

g = count_up(3)      # 创建生成器对象（此时函数体一行都还没执行）
print(f"\n  next(g) = {next(g)}")   # 第一次 next → 执行到 yield i 产出 0
print(f"  next(g) = {next(g)}")     # 从上次暂停处继续 → 产出 1
print(f"  next(g) = {next(g)}")     # 再产出 2
try:
    next(g)                        # 第四次：循环已结束，函数返回 → 抛 StopIteration
except StopIteration:
    print("  next(g) -> StopIteration   <- 生成器耗尽")


# ---- 2. 内存对比：列表把所有元素算好放内存，生成器只存「当前进度」 ----
# 列表推导式 [i*i for i in range(N)] 立即生成 N 个元素；生成器表达式 (i*i for i in range(N)) 一个都不算。
# sys.getsizeof 只测对象本身的开销，但足以看出差别：列表随 N 线性增长，生成器大小恒定。
# Java 对照：List<Integer> 全部物化 vs Stream.iterate(...) 只保留流水线状态。
print("\n" + "=" * 60)
print("2. 内存对比：这是生成器的核心价值")
print("=" * 60)

N = 1_000_000                       # 下划线是数字分隔符，纯粹为了可读性，等价 1000000
lst = [i * i for i in range(N)]     # 列表推导式：立刻计算并存储 100 万个 int 对象 → 占用大量内存
gen = (i * i for i in range(N))     # 生成器表达式：用圆括号，只创建迭代器，一个元素都还没计算

print(f"  列表推导式 {N:,} 个元素  {sys.getsizeof(lst):>12,} 字节")   # {N:,} 千分位格式；:>12, 右对齐并加千分位
print(f"  生成器表达式             {sys.getsizeof(gen):>12,} 字节")   # 只有几百字节，与 N 无关
print("  生成器大小与 N 无关")

del lst                             # 主动释放那个大列表，避免后面继续占内存

# 生成器能处理无限序列
def naturals():
    n = 0
    while True:                     # 无限循环：因为没有 return，只在每次 next() 时产出一个值
        yield n
        n += 1

print(f"\n  无限序列取前 10 个：{list(itertools.islice(naturals(), 10))}")   # islice 限制取前 10 个，否则 list() 会永远转下去


# ---- 3. 实战：逐行读大文件，内存与文件大小无关 ----
# 文件对象本身就是迭代器，for line in f 每次只读一行进内存；配合 yield 再向外「转发」。
# Java 对照：Files.lines(path) 返回 Stream<String>，思路一致；不要用 read()/readAllLines() 把整个文件读进来。
# `with open(...)` 保证即使中途异常也会关闭文件（等价 Java 的 try-with-resources）。
print("=" * 60)
print("3. 逐行读大文件（实战最重要）")
print("=" * 60)

def read_lines(path, encoding="utf-8"):
    with open(path, encoding=encoding) as f:      # with 块结束时自动 close，不用手动 try/finally
        for line in f:                            # 文件对象逐行迭代：内存里同时只保留一行
            line = line.strip()                   # 去掉行尾换行符和首尾空白
            if line:                              # 跳过空行
                yield line                        # 把有效行交给调用方，然后暂停等待下一次迭代

# 演示：用当前文件自己
demo = __file__        # 内置变量：当前脚本的路径，不用写死文件名
count = 0              # 计数器
for line in read_lines(demo):    # 消费生成器：每次循环取一行，读完自动结束
    count += 1
print(f"  逐行读完本文件，共 {count} 行，内存恒定")


# ---- 4. 生成器是一次性的：耗尽后不会自动重置 ----
# 它只保存「当前读到哪」这一个游标，sum() 读完就空了，第二次 sum 得到 0。
# 这是流式场景的硬约束：SSE 流中断后不能「重放」，只能让客户端重新发起请求。
print("\n" + "=" * 60)
print("4. 生成器只能遍历一次")
print("=" * 60)

g = (x for x in range(3))          # 生成器表达式，含 0、1、2 三个元素
print(f"  sum(g) 第一次 = {sum(g)}")   # 第一次遍历：0+1+2 = 3，此时生成器已耗尽
print(f"  sum(g) 第二次 = {sum(g)}   <- 已耗尽，不会重置")   # 第二次遍历：没有元素可读 → 0
print("  需要多次遍历 → 用列表或重新创建")


# ---- 5. 生成器管道：每层只处理一个元素，不产生中间列表 ----
# only_even(square(numbers(10))) 只是把三个生成器「串」起来，真正逐个取值时才一层层往外拉。
# Java 对照：numbers.stream().map(x->x*x).filter(x->x%2==0).collect(...)，但 Python 写法更原始也更直观。
print("=" * 60)
print("5. 生成器管道：像 Unix 管道一样组合")
print("=" * 60)

def numbers(limit):
    for i in range(1, limit + 1):   # 1..limit（range 右开，所以要 +1）
        yield i                     # 逐个产出数字

def square(src):
    for x in src:                   # src 可以是任何可迭代对象，包括另一个生成器
        yield x * x                 # 变换：平方

def only_even(src):
    for x in src:
        if x % 2 == 0:              # 过滤：只留偶数
            yield x

pipeline = only_even(square(numbers(10)))   # 此时一行都没执行，只是搭好了管道
print(f"  numbers(10) -> square -> only_even = {list(pipeline)}")   # list 才真正驱动整条管道
print("  每一步都是惰性的，不产生中间列表")


# ---- 6. 性能对比：找到就返回，不要先建完整列表再取第一个 ----
# 两种写法都能得到 6，但列表推导式要把 1000 万个元素全部过滤一遍、还要建一个新列表；
# 生成器式查找在 index=6 就 return 了，O(1) 级别的工作量。差距通常是几百倍。
# 注意：这里的 big 本身仍是 list（已经占内存），真正省的是「中间结果」和「提前退出」。
print("=" * 60)
print("6. 性能：惰性带来的收益（提前退出）")
print("=" * 60)

def find_first_generator(data, pred):
    # 名字里带 generator 只是命名习惯；这个函数是普通函数，靠「提前 return」避免全量遍历
    for x in data:
        if pred(x):                 # pred 是谓词函数（Java 的 Predicate<Integer>）
            return x                # 找到立刻返回，后面的元素根本不会看
    return None                     # 全都不满足才返回 None

big = list(range(10_000_000))              # 1000 万个整数，约 400MB 级别的列表
t0 = time.perf_counter()                   # 计时起点
r = find_first_generator(big, lambda x: x > 5)   # 从头扫，第 7 个元素就满足，立刻返回
t_gen = time.perf_counter() - t0           # 耗时
print(f"  生成器式查找（找到就返回）: {t_gen*1000:.4f}ms -> {r}")   # 输出 6

t0 = time.perf_counter()
r2 = [x for x in big if x > 5][0]          # 先构造完整过滤列表（约 1000 万个元素），再取下标 0
t_list = time.perf_counter() - t0
print(f"  先建列表再取第一个:        {t_list*1000:.4f}ms -> {r2}")   # 同样输出 6，但慢得多
print(f"  慢了约 {t_list/max(t_gen, 1e-9):.0f} 倍")                  # max 防止除零，:.0f 取整显示倍数


# ---- 7. yield from：把另一个生成器的元素「原样转发」出来 ----
# yield from inner() 等价于 for x in inner(): yield x，但更短，而且会正确传递 send()/throw()。
# 用途：把多个子生成器拼成一个，常用于递归展开树、组合多个数据源。
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

print(f"  list(outer()) = {list(outer())}")   # 依次产出 0 → 1 → 2 → 3


# ---- 8. 实战：分批处理，把大流切成固定大小的 chunk ----
# buf 是「缓冲区」：攒够 size 个就 yield 一批并清空；循环结束后把尾巴（不足 size 的）也交出去。
# Java 对照：List.partition / Guava Lists.partition，但这里输入可以是无限流，内存只占一个批次。
# 典型的批量写库场景：读 100 万行 → 每 1000 行一次 bulk insert。
print("=" * 60)
print("8. 实战：分批处理（避免一次性加载）")
print("=" * 60)

def batch(items, size):
    buf = []                         # 缓冲区，只保存当前这一批
    for it in items:                 # items 可以是列表、文件、另一个生成器
        buf.append(it)               # 攒一个
        if len(buf) == size:         # 攒够一批
            yield buf                # 交出去（注意：交的是同一个列表对象）
            buf = []                 # 关键：新建一个列表，而不是 buf.clear()
    if buf:                          # 最后不足一批的剩余元素
        yield buf                    # 也要交出去，否则尾部数据会丢

print(f"  list(batch(range(1,8), 3)) = {list(batch(range(1, 8), 3))}")   # [1,2,3] [4,5,6] [7]

print("\n  典型用法：")
print("    for chunk in batch(read_lines('huge.csv'), 1000):")
print("        bulk_insert(chunk)")   # 每 1000 行批量入库一次，内存恒定


# ---- 9. 对照项目的 SSE：同步生成器 + 异步生成器（async for） ----
# 同步版：yield 一段文本 → 外面拼成 "data: xxx\n\n" 这一帧通信格式（SSE 协议要求两个换行分帧）。
# 异步版：async def + yield 就是异步生成器，必须用 async for 消费，中间可 await（等模型返回）。
# Java 对照：Spring 的 SseEmitter / WebFlux Flux，只是 Python 用协程语法直接写。
print("=" * 60)
print("9. 对照项目的 SSE 流式")
print("=" * 60)

# 同步生成器
def stream_text():
    for ch in ["你", "好", "，", "世", "界"]:   # 逐字产出，模拟大模型的流式 token
        yield ch                               # 每产出一段就暂停，交给网络层立即发出

# SSE 分帧格式：data: xxx\n\n
frames = [f"data: {ch}\n\n" for ch in stream_text()]   # 把每个 token 包成 SSE 帧；\n\n 是两个换行
print("  同步生成器 -> SSE 帧：")
for f in frames[:3]:        # 只打印前三帧，避免输出太长
    print(f"    {f!r}")     # !r 用 repr 显示，能清楚看到 \n\n
print("    ...")

# 异步版本（ai-cs-py-chat 的真实写法）
import asyncio              # 标准库的事件循环，用来运行协程（Java 的 CompletableFuture/Reactor 角色）

async def astream_text():
    for ch in ["你", "好"]:
        await asyncio.sleep(0)   # 模拟 IO 等待点：让出事件循环，不阻塞其他请求（sleep(0) 只是语法演示）
        yield ch                 # async 生成器同样用 yield 产出

async def collect():
    out = []                        # 收集所有帧
    async for ch in astream_text(): # 必须用 async for，不能用普通 for
        out.append(f"data: {ch}\n\n")   # 与同步版拼帧格式完全一致
    return out                      # 返回列表供打印

print(f"\n  异步版本：{asyncio.run(collect())}")   # asyncio.run 启动事件循环并执行协程，直到完成
print("  注意：AsyncIterator 同样只能遍历一次（所以流式不能重试）")
