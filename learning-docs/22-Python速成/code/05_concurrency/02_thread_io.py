"""02 · 线程池：IO 密集任务的实用方案

运行: python 05_concurrency/02_thread_io.py
"""

# ---- 本段讲：线程池的三个核心用法，以及一个必须避免的误用 ----
# ThreadPoolExecutor 是 Python 对「线程池」的标准封装，思路和 Java 的 ExecutorService 一致：
#   提交任务 → 拿 Future → 取结果；线程复用，避免反复创建销毁。
# 唯一要记住的前提：它只对 IO 密集有效（等待时释放 GIL），CPU 密集必须换 ProcessPoolExecutor。

import threading                                       # 用于第 4 小节演示"共享状态必须加锁"
import time                                            # sleep 模拟 IO；perf_counter 计时
from concurrent.futures import ThreadPoolExecutor, as_completed
# ThreadPoolExecutor：线程池本体，对应 Java 的 Executors.newFixedThreadPool(n)
# as_completed：谁先完成先返回，对应 Java 的 ExecutorCompletionService


def fake_http_get(url: str, delay: float = 0.2) -> dict:
    """模拟 HTTP 请求：time.sleep 会释放 GIL"""
    # 真实场景里这里是 requests.get / pymysql 查询；共同点是「大部分时间在等网络」。
    # 等待期间线程会释放 GIL，所以多个线程的等待可以重叠 —— 这是线程池有效的根本原因。
    time.sleep(delay)             # 默认参数 delay=0.2，调用时可不传；Java 要用重载方法实现
    return {"url": url, "status": 200, "len": len(url)}   # 返回 dict，等价 Java 的 Map<String,Object>


def cpu_task(n: int) -> int:
    # CPU 密集样板：生成器表达式求和，纯 Python 计算，全程持有 GIL。
    # sum(...) 把生成器里的值累加；对应 Java 的 IntStream.range(0,n).map(i -> i*i).sum()
    return sum(i * i for i in range(n))


def main() -> None:
    print("=" * 60)                # 分隔线，仅为输出美观
    print("1. map：按提交顺序返回结果")
    print("=" * 60)

    urls = [f"https://api/x/{i}" for i in range(6)]   # 造 6 个假 URL；f-string 插值，等价 Java 字符串拼接/String.format
    # 列表推导式生成 [".../0", ..., ".../5"]

    # ① 串行：6 次 0.2s 的等待首尾相接，总耗时 ≈ 1.2s
    t0 = time.perf_counter()      # 计时起点（单调时钟，不受系统改时间影响）
    serial = [fake_http_get(u) for u in urls]   # 逐个调用并把结果收进列表
    print(f"  串行 6 个请求          {time.perf_counter()-t0:.2f}s")

    # ② 线程池 6 并发：6 段等待重叠，总耗时 ≈ 0.2s
    t0 = time.perf_counter()      # 重新计时
    with ThreadPoolExecutor(max_workers=6) as pool:
        # max_workers 就是池内线程数；with 退出时自动 shutdown(wait=True)，等同 try-with-resources
        results = list(pool.map(fake_http_get, urls))
        # map(函数, 可迭代) 把每个元素丢进池里跑，并按「输入顺序」返回结果（不是完成顺序）
        # Java 对应：pool.invokeAll(tasks) 后按 List 顺序取 Future.get()
    print(f"  线程池 6 并发          {time.perf_counter()-t0:.2f}s")
    print(f"  结果顺序与输入一致:    {[r['url'][-1] for r in results]}")
    # r['url'][-1] 取 URL 最后一个字符（即序号），用来证明结果顺序 = 提交顺序


    print()
    print("=" * 60)
    print("2. submit + as_completed：谁先完成先处理")
    print("=" * 60)

    tasks = [
        ("fast", 0.1), ("medium", 0.3), ("slow", 0.5),
        ("a", 0.2), ("b", 0.4),
    ]
    # 列表里每个元素是 (任务名, 延迟秒数) 元组；延迟不同，完成顺序自然不同

    with ThreadPoolExecutor(max_workers=5) as pool:
        futures = {
            pool.submit(fake_http_get, name, d): name
            for name, d in tasks
        }
        # 字典推导式：键是 submit 返回的 Future，值是任务名（反向索引，方便回头查名字）
        # submit(fn, *args) 立即返回 Future，对应 Java 的 pool.submit(Callable)
        order = []                # 记录实际完成顺序
        for fut in as_completed(futures):
            # as_completed 是生成器：每有一个任务完成就 yield 它的 Future，与提交顺序无关
            name = futures[fut]   # 用 Future 反查任务名
            order.append(name)    # 追加到完成顺序列表
            print(f"  完成 {name:8} (期望延迟 {dict(tasks)[name]}s)")
            # {name:8} 表示左对齐、宽度 8 的填充；dict(tasks) 把元组列表转成名字→延迟的字典
        print(f"\n  完成顺序: {order}   <- 按耗时，不是提交顺序")


    print()
    print("=" * 60)
    print("3. 异常处理：一个失败不影响其他")
    print("=" * 60)

    def may_fail(i):
        # 嵌套函数：默认在某个任务 i==2 时抛异常，用来验证"单点失败不影响整体"
        if i == 2:
            raise ValueError(f"第 {i} 个失败了")   # 主动抛出；对应 Java 的 throw new IllegalArgumentException(...)
        return i                  # 其余任务正常返回

    with ThreadPoolExecutor(max_workers=4) as pool:
        futures = {pool.submit(may_fail, i): i for i in range(5)}   # 提交 5 个任务，建立 Future→入参 的映射
        for fut in as_completed(futures):   # 完成一个处理一个
            i = futures[fut]      # 取出对应的入参
            try:
                print(f"  {i} -> {fut.result()}")
                # result() 会阻塞到该任务结束；若任务内抛异常，这里会重新抛出该异常
            except ValueError as e:
                # 只捕获 ValueError；其他异常仍会向上冒泡。对应 Java 的 catch (IllegalArgumentException e)
                print(f"  {i} -> 捕获：{e}")


    print()
    print("=" * 60)
    print("4. 共享状态必须加锁（GIL 不等于线程安全）")
    print("=" * 60)

    counter = {"n": 0}
    # 用 dict 包一层是为了让内层函数能修改它：Python 对「重新赋值外层变量」需要 nonlocal/global，
    # 但修改 dict 的键值不需要声明。Java 里通常用 AtomicInteger 或 synchronized。

    def inc_unsafe(times):
        for _ in range(times):
            counter["n"] += 1        # 读-改-写三步，可能被打断

    # ① 不加锁：+= 看似一行，实际是「取旧值 → 加 1 → 写回」三条字节码，
    #    GIL 只保证单条字节码原子，线程可能在中间被切走，导致更新丢失。
    counter["n"] = 0              # 重置计数器
    ts = [threading.Thread(target=inc_unsafe, args=(100_000,)) for _ in range(4)]
    # 4 个线程各加 10 万次，理论结果应为 400000
    for t in ts:
        t.start()                 # 启动
    for t in ts:
        t.join()                  # 等全部结束再读结果，否则读到中间值
    print(f"  不加锁 4x100000 -> {counter['n']}  (期望 400000)")

    lock = threading.Lock()       # 互斥锁；对应 Java 的 synchronized 或 ReentrantLock

    def inc_safe(times):
        for _ in range(times):
            with lock:
                # with 自动 acquire / release（异常也会释放），对应 Java 的 try { lock.lock(); ... } finally { lock.unlock(); }
                counter["n"] += 1

    # ② 加锁：同一时刻只有一个线程能进入临界区，结果稳定为 400000
    counter["n"] = 0              # 再次重置
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

    N = 3_000_000                 # 单次计算量

    # ① 串行基准
    t0 = time.perf_counter()
    for _ in range(4):
        cpu_task(N)               # 重复 4 次，丢弃结果
    serial = time.perf_counter() - t0
    print(f"  串行 4 次      {serial:.2f}s")

    # ② 线程池跑同样 4 份：因为 GIL，加速比会接近 1（甚至更慢）
    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(cpu_task, [N] * 4))   # [N]*4 生成 4 份相同参数；list() 强制取完结果
    threaded = time.perf_counter() - t0
    print(f"  4 线程         {threaded:.2f}s   加速比 {serial/threaded:.2f}x")
    print("  加速比接近 1 → 说明该用进程池（见 03_asyncio_basics 或 01_gil_proof）")


    print()
    print("=" * 60)
    print("6. 实战：并发请求 + 超时控制")
    print("=" * 60)

    def slow_call(i):
        time.sleep(0.5)           # 模拟每个请求固定 0.5s
        return i                  # 回传入参，便于核对结果

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=3) as pool:
        futures = [pool.submit(slow_call, i) for i in range(6)]
        # 3 个 worker 跑 6 个任务 → 分两批，理论耗时 ≈ 1.0s
        done, not_done = [], []   # 预留两个列表，本示例未实际填充
        for f in futures:
            if f.done() or f.result(timeout=None) is not None:
                # f.done() 非阻塞地判断是否已完成；f.result(timeout=None) 表示"无限等"
                # 注意：这里没有真正实现超时，只是演示 API；真要超时需 result(timeout=N) 并捕获 TimeoutError
                pass              # 占位，不做处理
        results = [f.result() for f in futures]   # 按提交顺序收集结果（此时都已完成）
    print(f"  6 个 0.5s 任务，3 并发 -> {time.perf_counter()-t0:.2f}s  结果 {results}")
    print("  注意：ThreadPoolExecutor 没有内置超时取消，需要自己管理 future")


if __name__ == "__main__":
    # 直接运行本文件才执行 main()；被 import 时不执行（Java 没有这层保护，靠 main 方法约定）
    main()
