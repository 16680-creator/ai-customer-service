"""asyncio 阻塞定位：一个同步调用如何拖慢全部请求

运行: python 07_debugging/02_asyncio_blocker.py
"""

# ---- 本段讲：本文件的依赖与类型导入 ----
# 这个脚本用一个「慢请求 + 10 个快请求」的对比，展示同步阻塞调用如何拖慢整个事件循环，
# 以及用 asyncio.to_thread 把阻塞调用挪到线程池后的效果。Java 对照：CompletableFuture + 线程池。
import asyncio                                  # 事件循环、sleep、gather、to_thread 都在这里
import time                                     # 需要同步的 time.sleep 来制造「阻塞」
from collections.abc import Awaitable, Callable # 仅用于类型标注，运行时不产生开销


# ---- 本段讲：一个正常的异步快请求 ----
async def quick_request(i: int) -> str:
    # await 表示「把控制权交还事件循环」，等待期间可以跑别的协程；
    # 这与 time.sleep 的「占着线程干等」是完全不同的语义。
    await asyncio.sleep(0.01)
    return f"quick-{i}"                         # f-string 插值，等价 Java 的 "quick-" + i


# ---- 本段讲：错误示范 —— 在协程里调用同步阻塞函数 ----
async def blocking_request() -> str:
    # 错误示例：同步 sleep 占用事件循环线程。
    # time.sleep 不交出控制权，整个事件循环被冻结，其他协程全部排队等待。
    # Java 类比：在单线程的 EventLoop 里做 Thread.sleep，同样会卡住所有任务。
    time.sleep(0.25)
    return "blocking"


# ---- 本段讲：正确写法 —— 阻塞调用丢进线程池 ----
async def fixed_request() -> str:
    # 修复：无法改成异步的阻塞调用放到线程池。
    # asyncio.to_thread 会把 time.sleep 放到工作线程执行，主事件循环可以继续调度快请求。
    # Java 类比：把阻塞任务提交给 ExecutorService，再用 CompletableFuture 等待。
    await asyncio.to_thread(time.sleep, 0.25)
    return "fixed"


# ---- 本段讲：统一测量函数（对比两种写法的总耗时）----
async def measure(label: str, slow: Callable[[], Awaitable[str]]) -> float:
    # 参数 slow 是一个「调用后返回可等待对象」的函数；类型标注只是提示，Python 不强制检查
    start = time.perf_counter()                  # 高精度计时起点
    # gather 并发启动：1 个慢请求 + 10 个快请求同时跑；解包 * 把生成器展开成多个位置参数。
    # Java 类比：CompletableFuture.allOf(...) 后统一 join 取结果。
    results = await asyncio.gather(slow(), *(quick_request(i) for i in range(10)))
    cost = time.perf_counter() - start           # 总耗时
    print(f"  {label:34} {cost:.3f}s  完成 {len(results)} 个请求")   # :34 左对齐补齐到 34 列
    return cost


# ---- 本段讲：主流程 —— 跑两次对比并给出排查结论 ----
async def main() -> None:
    print("=" * 64)
    print("同一个慢请求 + 10 个快请求")
    print("=" * 64)

    # 先跑错误版：慢请求用 time.sleep 阻塞，10 个 10ms 的快请求要等它睡完才能跑
    bad = await measure("同步阻塞 time.sleep", blocking_request)
    # 再跑修复版：阻塞调用进线程池，快请求可以与慢请求真正并发
    good = await measure("修复 asyncio.to_thread", fixed_request)

    print()
    # 计算加速比；:.1f 保留一位小数
    print(f"修复后约快 {bad / good:.1f} 倍（快请求不再等待整个事件循环）")
    print("排查规则：所有非 await 的网络、DB、文件、sleep 都是嫌疑点。")
    print("生产定位：asyncio debug + faulthandler + 分段耗时日志。")


# 入口：asyncio.run 负责创建事件循环、执行 main、最后关闭循环。
# Java 类比：自己 new 一个 EventLoop 并 submit 任务，结束后优雅关闭。
if __name__ == "__main__":
    asyncio.run(main())
