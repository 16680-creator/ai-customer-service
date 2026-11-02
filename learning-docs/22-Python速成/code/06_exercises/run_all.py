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

CODE_ROOT = Path(__file__).resolve().parent.parent

SCRIPTS = [
    "01_basics/01_hello.py",
    "01_basics/02_types.py",
    "01_basics/03_strings.py",
    "01_basics/04_control.py",
    "01_basics/05_functions.py",
    "01_basics/09_self_test.py",
    "02_containers/01_list.py",
    "02_containers/02_dict.py",
    "02_containers/03_set_tuple.py",
    "02_containers/04_comprehension.py",
    "02_containers/05_copy.py",
    "02_containers/07_quiz.py",
    "03_functions/01_scope.py",
    "03_functions/02_args.py",
    "03_functions/03_decorator.py",
    "03_functions/04_generator.py",
    "04_oop/01_basic.py",
    "04_oop/02_dataclass.py",
    "04_oop/03_property.py",
    "04_oop/04_interface.py",
    "05_concurrency/01_gil_proof.py",
    "05_concurrency/02_thread_io.py",
    "05_concurrency/03_asyncio_basics.py",
    "05_concurrency/04_async_vs_thread.py",
    "05_concurrency/05_async_semaphore.py",
    "06_exercises/answers.py",
    "06_exercises/traps_demo.py",
    "07_debugging/01_failure_evidence.py",
    "07_debugging/02_asyncio_blocker.py",
    "08_performance/01_profile_demo.py",
    "08_performance/02_memory_demo.py",
    "09_webdb/demo.py",
    "10_ai/rag_demo.py",
]


def run_one(rel_path: str, quiet: bool) -> tuple[str, str, float, str]:
    path = CODE_ROOT / rel_path
    if not path.exists():
        return rel_path, "FAIL", 0.0, "文件不存在"

    t0 = time.perf_counter()
    proc = subprocess.run(
        [sys.executable, str(path)],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        cwd=str(path.parent),
    )
    cost = time.perf_counter() - t0

    out = (proc.stdout or "") + (proc.stderr or "")

    # 判定顺序：显式 SKIP -> FAIL -> PASS。
    # SKIP 表示缺少可选依赖或环境不支持，不算失败，但不能混进“通过数”。
    if proc.returncode == 0 and out.lstrip().startswith("SKIP:"):
        status = "SKIP"
    elif proc.returncode != 0 or "FAIL" in (proc.stdout or ""):
        status = "FAIL"
    else:
        status = "PASS"

    if not quiet:
        print("=" * 70)
        print(f">>> {rel_path}")
        print("=" * 70)
        if out.strip():
            print(out.rstrip())
        if status == "FAIL":
            print(f"[!] 退出码 {proc.returncode}")

    return rel_path, status, cost, out


def main() -> int:
    parser = argparse.ArgumentParser(description="跑完全部示例脚本")
    parser.add_argument("-q", "--quiet", action="store_true", help="只显示结果")
    parser.add_argument("-k", "--keyword", default="", help="按路径关键字过滤")
    args = parser.parse_args()

    targets = [s for s in SCRIPTS if args.keyword.lower() in s.lower()]
    if not targets:
        print(f"没有匹配 '{args.keyword}' 的脚本")
        return 1

    print(f"共 {len(targets)} 个脚本，解释器：{sys.version.split()[0]}")
    print()

    results = []
    total_start = time.perf_counter()
    for rel in targets:
        name, status, cost, out = run_one(rel, args.quiet)
        results.append((name, status, cost))
        if args.quiet:
            print(f"  [{status}] {name:42} {cost:6.2f}s")

    total = time.perf_counter() - total_start

    print()
    print("=" * 70)
    print("汇总")
    print("=" * 70)
    if not args.quiet:
        for name, status, cost in results:
            print(f"  [{status:4}] {name:42} {cost:6.2f}s")

    passed = sum(1 for _, status, _ in results if status == "PASS")
    skipped = [n for n, status, _ in results if status == "SKIP"]
    failed = [n for n, status, _ in results if status == "FAIL"]

    print("-" * 70)
    summary = f"  {passed} 通过"
    if skipped:
        summary += f"，{len(skipped)} 跳过"
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

    return 0 if not failed else 1


if __name__ == "__main__":
    raise SystemExit(main())
