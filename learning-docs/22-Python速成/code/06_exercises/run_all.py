"""统一自测入口：跑完全部示例脚本 + 题库答案

运行:
    python 06_exercises/run_all.py            # 全部
    python 06_exercises/run_all.py -q         # 只显示结果，不显示脚本输出
    python 06_exercises/run_all.py -k concurrency   # 只跑名字含 concurrency 的

退出码 0 = 没有失败（可能包含 SKIP，例如缺少可选依赖时的 Web 演示）
"""

import argparse
import subprocess
import sys
import time
from pathlib import Path

# ---- 本段讲：要跑哪些脚本（清单配置）----
# CODE_ROOT 是 code/ 目录：本文件在 code/06_exercises/ 下，parent 是 06_exercises，再 parent 才是 code。
# resolve() 把路径转成绝对路径（会解析符号链接），__file__ 是当前脚本自身的路径。
# Java 里没有等价写法，通常用 System.getProperty("user.dir") 或 ClassLoader 资源定位。
CODE_ROOT = Path(__file__).resolve().parent.parent

# 全部示例脚本的相对路径清单（相对 CODE_ROOT）。
SCRIPTS = [
    # 01 基础语法：变量、类型、字符串、控制流、函数
    "01_basics/01_hello.py",
    "01_basics/02_types.py",
    "01_basics/03_strings.py",
    "01_basics/04_control.py",
    "01_basics/05_functions.py",
    "01_basics/09_self_test.py",
    # 02 容器：list / dict / set / tuple / 推导式 / 拷贝
    "02_containers/01_list.py",
    "02_containers/02_dict.py",
    "02_containers/03_set_tuple.py",
    "02_containers/04_comprehension.py",
    "02_containers/05_copy.py",
    "02_containers/07_quiz.py",
    # 03 函数：作用域、参数、装饰器、生成器
    "03_functions/01_scope.py",
    "03_functions/02_args.py",
    "03_functions/03_decorator.py",
    "03_functions/04_generator.py",
    # 04 面向对象：类、dataclass、property、接口（抽象基类）
    "04_oop/01_basic.py",
    "04_oop/02_dataclass.py",
    "04_oop/03_property.py",
    "04_oop/04_interface.py",
    # 05 并发：GIL 证明、线程 IO、asyncio、异步与线程对比、信号量
    "05_concurrency/01_gil_proof.py",
    "05_concurrency/02_thread_io.py",
    "05_concurrency/03_asyncio_basics.py",
    "05_concurrency/04_async_vs_thread.py",
    "05_concurrency/05_async_semaphore.py",
    # 06 练习：题库答案 + 踩坑演示（本测试入口会连它们一起跑）
    "06_exercises/answers.py",
    "06_exercises/traps_demo.py",
    # 07 调试：异常链证据、asyncio 阻塞定位
    "07_debugging/01_failure_evidence.py",
    "07_debugging/02_asyncio_blocker.py",
    # 08 性能：cProfile 与内存占用
    "08_performance/01_profile_demo.py",
    "08_performance/02_memory_demo.py",
    # 09 Web/DB、10 AI：缺少可选依赖时会自行输出 SKIP
    "09_webdb/demo.py",
    "10_ai/rag_demo.py",
]


# ---- 本段讲：跑单个脚本并判定结果 ----
# 用子进程执行每个示例脚本，收集它的 stdout/stderr 和耗时。
# 这样某个脚本崩溃/卡住不会影响总入口本身，和 CI 里逐个跑测试用例是一个思路。
def run_one(rel_path: str, quiet: bool) -> tuple[str, str, float, str]:
    path = CODE_ROOT / rel_path          # 用 / 拼路径；pathlib 会自动处理分隔符，比字符串拼接安全
    if not path.exists():                # 文件缺失直接判 FAIL，避免抛异常中断整轮
        return rel_path, "FAIL", 0.0, "文件不存在"

    t0 = time.perf_counter()             # 计时起点
    # 启动子进程：用当前解释器 sys.executable 跑目标脚本，保证和本进程同一个 Python 环境。
    # capture_output 捕获输出，text=True 以文本返回（否则是 bytes），
    # encoding 显式指定 utf-8 防止 Windows 默认 GBK 解码失败，errors="replace" 遇到坏字节不炸。
    # cwd 设为脚本所在目录：示例里用的相对路径（如 data/xxx.csv）才能正确找到。
    proc = subprocess.run(
        [sys.executable, str(path)],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        cwd=str(path.parent),
    )
    cost = time.perf_counter() - t0      # 本次耗时（秒）

    # 合并标准输出与标准错误；`or ""` 是防 None（虽然 text=True 下一般不会是 None）
    out = (proc.stdout or "") + (proc.stderr or "")

    # 判定顺序：显式 SKIP -> FAIL -> PASS。
    # SKIP 表示缺少可选依赖或环境不支持，不算失败，但不能混进“通过数”。
    if proc.returncode == 0 and out.lstrip().startswith("SKIP:"):
        status = "SKIP"                  # 脚本自己声明跳过，且退出码为 0
    elif proc.returncode != 0 or "FAIL" in (proc.stdout or ""):
        status = "FAIL"                  # 非零退出码，或输出里出现 FAIL 标记
    else:
        status = "PASS"                  # 其余情况视为通过

    if not quiet:
        # 非安静模式：把每个脚本的输出完整打出来，方便定位
        print("=" * 70)
        print(f">>> {rel_path}")
        print("=" * 70)
        if out.strip():                  # 只在有实际内容时打印，避免空行噪音
            print(out.rstrip())          # rstrip 去掉末尾多余空白
        if status == "FAIL":
            print(f"[!] 退出码 {proc.returncode}")   # 额外提示退出码，便于排查

    return rel_path, status, cost, out   # 返回元组供汇总统计使用


# ---- 本段讲：命令行入口与汇总报告 ----
# main 负责解析 -q/-k 参数、逐个调用 run_one、最后打印统计并返回退出码。
def main() -> int:
    parser = argparse.ArgumentParser(description="跑完全部示例脚本")   # Java 里常用 args4j / picocli
    parser.add_argument("-q", "--quiet", action="store_true", help="只显示结果")
    # action="store_true"：出现该开关就存 True，不需要再传值（类似布尔 flag）
    parser.add_argument("-k", "--keyword", default="", help="按路径关键字过滤")
    args = parser.parse_args()       # 解析 sys.argv；解析失败会自动打印帮助并退出

    # 关键字过滤：lower() 统一小写，实现大小写不敏感匹配
    targets = [s for s in SCRIPTS if args.keyword.lower() in s.lower()]
    if not targets:
        print(f"没有匹配 '{args.keyword}' 的脚本")
        return 1                     # 非零退出码表示“这次运行有问题”

    # sys.version 形如 "3.12.1 (main, ...)"，split()[0] 只取版本号
    print(f"共 {len(targets)} 个脚本，解释器：{sys.version.split()[0]}")
    print()

    results = []                     # 收集 (脚本名, 状态, 耗时)
    total_start = time.perf_counter()
    for rel in targets:
        name, status, cost, out = run_one(rel, args.quiet)   # out 这里用不到，但仍需解包
        results.append((name, status, cost))
        if args.quiet:
            # 安静模式：每个脚本只打一行结果；:42 是左对齐补空格到 42 列
            print(f"  [{status}] {name:42} {cost:6.2f}s")

    total = time.perf_counter() - total_start   # 总耗时

    print()
    print("=" * 70)
    print("汇总")
    print("=" * 70)
    if not args.quiet:
        # 非安静模式此时才统一输出结果表（逐条输出时已经打过详细内容了）
        for name, status, cost in results:
            print(f"  [{status:4}] {name:42} {cost:6.2f}s")

    # 三种状态的统计：生成器 + sum 计数；列表推导收集名字，便于最后列出来
    passed = sum(1 for _, status, _ in results if status == "PASS")
    skipped = [n for n, status, _ in results if status == "SKIP"]
    failed = [n for n, status, _ in results if status == "FAIL"]

    print("-" * 70)
    summary = f"  {passed} 通过"
    if skipped:
        summary += f"，{len(skipped)} 跳过"   # 只在实际有跳过时附加
    if failed:
        summary += f"，{len(failed)} 失败"
    print(f"{summary}（共 {len(results)} 个），总耗时 {total:.1f}s")
    if skipped:
        print("  跳过（缺少可选依赖或环境不支持）：")
        for name in skipped:
            print(f"    - {name}")
    if failed:
        print("  失败：")
        for name in failed:
            print(f"    - {name}")
    print("=" * 70)

    # 有失败就返回 1，否则 0；CI 依靠退出码判断这一步是否成功
    return 0 if not failed else 1


# 直接运行本文件时把 main 的返回值交给 SystemExit，成为进程退出码；
# 被 import 时不会执行，避免产生副作用。
if __name__ == "__main__":
    raise SystemExit(main())
