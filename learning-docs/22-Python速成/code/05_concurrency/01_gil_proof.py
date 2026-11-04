"""01 · GIL 实证：多线程跑 CPU 密集任务为什么没加速

运行: python 05_concurrency/01_gil_proof.py
预计耗时: 5-15 秒（取决于机器）

注意：需要 if __name__ == "__main__" 保护（Windows 多进程要求）
"""

# ---- 本段讲：准备 GIL 实证所需的工具，以及两个"被测函数" ----
# 这个脚本要证明「多线程跑 CPU 密集任务不会加速」，所以需要四样东西：
#   1) sys：查看解释器版本、线程切换间隔（GIL 多久换一次线程）；
#   2) threading：手动建线程，对应 Java 的 new Thread(...)；
#   3) time：perf_counter() 是单调高精度计时器，对应 Java 的 System.nanoTime()；
#   4) ProcessPoolExecutor：多进程池，是绕开 GIL 的标准做法（Java 不需要，JVM 线程真并行）。
import sys                                             # sys.version / sys.platform / sys.getswitchinterval()
import threading                                       # 底层线程 API；对应 Java 的 Thread
import time                                            # sleep + 计时；对应 Thread.sleep / System.nanoTime
from concurrent.futures import ProcessPoolExecutor     # 进程池；每个进程有独立解释器和独立 GIL


def cpu_task(n: int) -> int:
    """纯 CPU 计算：无 IO、无等待"""
    # 本函数是"CPU 密集"的样板：循环体内只有算术，没有任何会让出执行权的地方。
    # 因为全是纯 Python 字节码，执行期间必须一直持有 GIL，所以多线程无法真正并行。
    total = 0                     # 累加器初值；局部变量放在栈帧里，天生线程私有（Java 的局部变量同理）
    for i in range(n):            # range(n) 是惰性整数序列；等价 Java 的 for (int i = 0; i < n; i++)
        total += i * i            # 读-改-写三步；纯 Python 运算，全程占着 GIL，别的线程插不进来
    return total                  # 返回 int；Python 的 int 是任意精度，不像 Java 的 int 会溢出


def io_task(seconds: float) -> str:
    """模拟 IO 等待：sleep 时会释放 GIL"""
    # 对照组：time.sleep 会把当前线程挂起并释放 GIL，这正是多线程在 IO 场景能提速的原因。
    time.sleep(seconds)           # 阻塞线程 seconds 秒；Java 对应 Thread.sleep((long)(seconds*1000))
    return "done"                 # 返回值只用于占位，内容不重要


def main() -> None:
    # 所有实验都收在 main() 里，再由文件末尾的 if __name__ == "__main__" 调用。
    # 这样做是为了 Windows 多进程：子进程会重新 import 本模块，若实验写在模块顶层就会被重复执行。
    print(f"Python {sys.version.split()[0]} on {sys.platform}")     # 打印解释器版本与平台，便于对照结果
    print(f"CPU 核数: {__import__('os').cpu_count()}")              # 核数决定多进程的理论加速上限
    print(f"切换间隔 sys.getswitchinterval() = {sys.getswitchinterval()}s")   # GIL 每 5ms 左右强制换线程一次
    print()                                                          # 空行，只为输出排版

    N = 4_000_000                 # 单次计算量；下划线是数字分隔符，等价 Java 的 4_000_000 写法

    # ---------------- CPU 密集 ----------------
    # ---- 本段讲：同一份 CPU 任务，分别用「串行 / 4 线程 / 4 进程」跑，对比耗时 ----
    # 预期：串行 ≈ 多线程（GIL 限制），多进程 ≈ 串行/核数（真并行）。
    # Java 里这段三个数字应当接近 1 : 1/4 : 1/4，因为 JVM 线程没有 GIL。
    print("=" * 60)                                 # 分隔线，纯输出美化
    print("CPU 密集任务：4 次 cpu_task(4_000_000)")   # 说明本组实验内容
    print("=" * 60)

    # ① 串行基准：4 次调用依次执行，作为后面"加速比"的分母
    t0 = time.perf_counter()      # 单调高精度计时起点；对应 Java 的 System.nanoTime()
    for _ in range(4):            # 下划线 _ 表示"循环变量用不到"，是 Python 的惯用写法
        cpu_task(N)               # 丢弃返回值，只关心耗时
    serial = time.perf_counter() - t0    # 时间差 = 串行总耗时（秒，float）
    print(f"  串行        {serial:.2f}s")  # :.2f 表示保留两位小数，等价 Java 的 %.2f

    # ② 4 线程：期望加速 4 倍，实测几乎为 1 倍（GIL 只允许一个线程执行字节码）
    t0 = time.perf_counter()      # 重新计时
    threads = [threading.Thread(target=cpu_task, args=(N,)) for _ in range(4)]
    # 列表推导式一次创建 4 个线程对象；target= 是线程入口函数，args= 必须是元组（单个参数也要加逗号）
    # Java 对应：IntStream.range(0,4).mapToObj(i -> new Thread(() -> cpuTask(N))).toList()
    for t in threads:             # 逐个启动；此时才真正创建 OS 线程
        t.start()                 # 对应 Java 的 thread.start()
    for t in threads:             # 再逐个等待结束
        t.join()                  # 对应 Java 的 thread.join()，阻塞到该线程终止
    threaded = time.perf_counter() - t0  # 4 线程总耗时
    print(f"  4 线程      {threaded:.2f}s   加速比 {serial/threaded:.2f}x")   # 加速比≈1 就是 GIL 的铁证

    # ③ 4 进程：每个子进程有独立解释器和独立 GIL，因此能吃到多核
    t0 = time.perf_counter()      # 重新计时
    with ProcessPoolExecutor(max_workers=4) as pool:
        # with 语句保证退出时自动 shutdown()，等价 Java try-with-resources 里的 ExecutorService
        list(pool.map(cpu_task, [N] * 4))
        # map 把 [N]*4 逐个喂给 cpu_task，返回迭代器；list() 强制取完所有结果才会继续
        # Java 对应：pool.invokeAll(4 个 Callable)，但 Java 用线程池即可，不需要换"进程池"
    multiproc = time.perf_counter() - t0    # 4 进程总耗时
    print(f"  4 进程      {multiproc:.2f}s   加速比 {serial/multiproc:.2f}x")

    print()
    print(f"  结论：多线程加速比 ≈ 1（GIL 导致无法利用多核）")
    print(f"        多进程加速比 ≈ 核数（真并行）")

    # ---------------- IO 密集 ----------------
    # ---- 本段讲：同样是多线程，换成"等待型"任务就能提速 ----
    # 因为 time.sleep / socket 读写会把线程挂起并释放 GIL，等待时间可以互相重叠。
    # Java 里线程 sleep 本来就不占 CPU，所以这个结论在 Java 更直观。
    print()
    print("=" * 60)
    print("IO 密集任务：8 次 io_task(0.3s)")
    print("=" * 60)

    WAIT = 0.3                    # 每次"网络往返"的模拟耗时（秒）
    COUNT = 8                     # 任务个数；8 × 0.3 = 2.4s 是串行理论值

    # ① 串行基准：8 次等待一个接一个，总耗时 ≈ 2.4s
    t0 = time.perf_counter()      # 计时起点
    for _ in range(COUNT):        # 依次执行，后一个必须等前一个 sleep 结束
        io_task(WAIT)             # 期间线程睡在系统调用上，但没人跟它重叠
    io_serial = time.perf_counter() - t0   # 串行总耗时
    print(f"  串行        {io_serial:.2f}s")

    # ② 8 个线程：8 段等待完全重叠，总耗时 ≈ 0.3s（加速 8 倍左右）
    t0 = time.perf_counter()      # 计时起点
    threads = [threading.Thread(target=io_task, args=(WAIT,)) for _ in range(COUNT)]
    # 每个线程跑一次 io_task；注意参数元组写成 (WAIT,)，逗号不能省，否则就是普通括号
    for t in threads:             # 启动全部线程
        t.start()
    for t in threads:             # 等待全部结束
        t.join()                  # join 返回时 8 次 sleep 都已结束
    io_threaded = time.perf_counter() - t0  # 多线程总耗时
    print(f"  8 线程      {io_threaded:.2f}s   加速比 {io_serial/io_threaded:.2f}x")

    print()
    print("  结论：sleep 会释放 GIL，所以 IO 等待能重叠，多线程有效")

    # ---------------- 线程数不是越多越好 ----------------
    # ---- 本段讲：线程数从 1 加到 8，CPU 任务的总耗时并不会下降 ----
    # 因为总计算量固定（每次跑 N//4，线程数就是为了让"份数"与之匹配），
    # 而 GIL 让同一时刻只有一个线程在算，多出来的线程只增加切换与锁竞争开销。
    print()
    print("=" * 60)
    print("补充：线程数增加并不缩短 CPU 任务（调度开销反而更慢）")
    print("=" * 60)
    for n_threads in (1, 2, 8):   # 依次测 1、2、8 个线程；元组遍历，等价 Java 的 int[] {1,2,8}
        t0 = time.perf_counter()  # 每组重新计时
        ts = [threading.Thread(target=cpu_task, args=(N // 4,)) for _ in range(n_threads)]
        # 每个线程都跑 N//4 的计算量；N//4 是整除（向下取整），等价 Java 的 N / 4（都是正数时）
        for t in ts:              # 启动这一组线程
            t.start()
        for t in ts:              # 等这一组全部结束
            t.join()
        print(f"  {n_threads} 线程跑 {n_threads} 份 1/4 任务: {time.perf_counter()-t0:.2f}s")
        # 观察点：8 线程的数字 ≈ 1 线程的数字，甚至更大 → 加线程对 CPU 任务无效

    print()
    print("=" * 60)
    print("总结")
    print("=" * 60)
    print("  CPU 密集 → ProcessPoolExecutor")                              # 唯一能吃到多核的写法
    print("  IO 密集  → asyncio（首选）或 ThreadPoolExecutor")             # 等待可以重叠，单线程即可
    print("  不确定   → 先写串行，测量，再决定")                            # 别凭直觉上并发


if __name__ == "__main__":
    # 只有「python 01_gil_proof.py」直接运行时 __name__ 才是 "__main__"；
    # 被 import（例如 Windows 子进程重新加载本模块）时不执行，避免递归创建进程。
    # Java 没有等价机制：Java 只能靠 main 方法入口，子类/工具类被加载时不会自动跑逻辑。
    main()
