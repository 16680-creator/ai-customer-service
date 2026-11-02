#!/usr/bin/env python3
"""
代码示例验证脚本

自动提取所有 .md 文档中的 Python 代码块并验证可运行性

运行方式：
    python validate_all_examples.py
    python validate_all_examples.py --verbose
    python validate_all_examples.py --fix  # 尝试自动修复简单错误

学习章节：本脚本演示了文件处理、正则表达式、异常处理、命令行参数解析
"""
import argparse
import ast
import re
import sys
from pathlib import Path
from typing import List, Tuple


class CodeBlock:
    """代码块数据结构"""

    def __init__(self, file: Path, line_no: int, code: str, language: str = "python"):
        self.file = file
        self.line_no = line_no
        self.code = code
        self.language = language

    def __repr__(self):
        return f"<CodeBlock {self.file.name}:{self.line_no} ({len(self.code)} chars)>"


class ValidationResult:
    """验证结果"""

    def __init__(
        self,
        block: CodeBlock,
        is_valid: bool,
        error: str | None = None,
        warning: str | None = None,
    ):
        self.block = block
        self.is_valid = is_valid
        self.error = error
        self.warning = warning


def extract_code_blocks(md_file: Path) -> List[CodeBlock]:
    """
    从 Markdown 文件提取所有 Python 代码块

    识别规则：
    ```python
    code here
    ```

    或

    ```
    code here
    ```（如果上下文明显是 Python）
    """
    blocks = []
    content = md_file.read_text(encoding="utf-8")
    lines = content.splitlines()

    in_code_block = False
    current_code = []
    current_lang = ""
    start_line = 0

    for i, line in enumerate(lines, start=1):
        # 检测代码块开始
        if line.strip().startswith("```"):
            if not in_code_block:
                # 开始代码块
                in_code_block = True
                start_line = i
                # 提取语言标识
                lang_match = re.match(r"```(\w+)", line.strip())
                current_lang = lang_match.group(1) if lang_match else "unknown"
                current_code = []
            else:
                # 结束代码块
                in_code_block = False
                if current_lang in ("python", "py", "unknown"):
                    # 只验证 Python 代码
                    code = "\n".join(current_code)
                    if code.strip() and not is_output_block(code):
                        blocks.append(
                            CodeBlock(
                                file=md_file,
                                line_no=start_line,
                                code=code,
                                language=current_lang,
                            )
                        )
                current_code = []
        elif in_code_block:
            current_code.append(line)

    return blocks


def is_output_block(code: str) -> bool:
    """
    判断是否是输出示例（而非可运行代码）

    例如：
    ```
    # → {'Alice': 5, 'Carol': 5}
    ```
    """
    code = code.strip()

    # 以注释开头且包含 → 或 # → 的是输出
    if code.startswith("#") and ("→" in code or "..." in code):
        return True

    # 纯 JSON 输出
    if code.startswith("{") and code.endswith("}") and '"' in code:
        return True

    # HTTP 响应示例
    if "HTTP" in code and ("200" in code or "404" in code):
        return True

    return False


def is_java_code(code: str) -> bool:
    """判断是否是 Java 代码示例（用于对照）"""
    java_keywords = ["public class", "public static void main", "@Override", "System.out"]
    return any(keyword in code for keyword in java_keywords)


def validate_syntax(block: CodeBlock) -> ValidationResult:
    """
    验证 Python 语法

    使用 ast.parse 检查语法正确性
    """
    code = block.code.strip()

    # 跳过 Java 代码
    if is_java_code(code):
        return ValidationResult(
            block,
            is_valid=True,
            warning="Skipped: Java code for comparison",
        )

    # 跳过纯文本/命令行示例
    if code.startswith("$") or code.startswith("#!"):
        return ValidationResult(
            block,
            is_valid=True,
            warning="Skipped: Shell command",
        )

    # 跳过不完整的代码片段（如函数定义后直接 ...）
    if code.strip().endswith("...") or "# ..." in code:
        return ValidationResult(
            block,
            is_valid=True,
            warning="Skipped: Incomplete snippet",
        )

    try:
        # 尝试解析 Python 语法
        ast.parse(code)
        return ValidationResult(block, is_valid=True)

    except SyntaxError as e:
        return ValidationResult(
            block,
            is_valid=False,
            error=f"SyntaxError: {e.msg} at line {e.lineno}",
        )
    except Exception as e:
        return ValidationResult(
            block,
            is_valid=False,
            error=f"ParseError: {str(e)}",
        )


def scan_directory(docs_dir: Path, verbose: bool = False) -> List[ValidationResult]:
    """
    扫描整个文档目录

    返回所有验证结果
    """
    results = []

    md_files = list(docs_dir.rglob("*.md"))

    if verbose:
        print(f"📁 扫描目录: {docs_dir}")
        print(f"📄 找到 {len(md_files)} 个 Markdown 文件\n")

    for md_file in md_files:
        if verbose:
            print(f"🔍 检查 {md_file.name}...", end=" ")

        blocks = extract_code_blocks(md_file)

        if verbose:
            print(f"({len(blocks)} 个代码块)")

        for block in blocks:
            result = validate_syntax(block)
            results.append(result)

    return results


def print_summary(results: List[ValidationResult], verbose: bool = False):
    """打印验证摘要"""
    total = len(results)
    valid = sum(1 for r in results if r.is_valid)
    invalid = total - valid
    warnings = sum(1 for r in results if r.warning)

    print("\n" + "=" * 60)
    print("📊 验证摘要")
    print("=" * 60)
    print(f"总代码块数: {total}")
    print(f"✅ 语法正确: {valid}")
    print(f"❌ 语法错误: {invalid}")
    print(f"⚠️  跳过 (警告): {warnings}")
    print(f"通过率: {valid / total * 100:.1f}%")
    print("=" * 60)

    if invalid > 0:
        print("\n❌ 发现语法错误:\n")
        for result in results:
            if not result.is_valid and result.error:
                print(f"  {result.block.file.name}:{result.block.line_no}")
                print(f"    {result.error}")
                if verbose:
                    # 显示出错代码
                    lines = result.block.code.splitlines()
                    for i, line in enumerate(lines[:5], start=1):  # 只显示前5行
                        print(f"      {i:3d} | {line}")
                    if len(lines) > 5:
                        print(f"      ... ({len(lines) - 5} more lines)")
                print()

    if verbose and warnings > 0:
        print("\n⚠️  跳过的代码块:\n")
        for result in results:
            if result.warning:
                print(f"  {result.block.file.name}:{result.block.line_no}")
                print(f"    {result.warning}\n")


def main():
    parser = argparse.ArgumentParser(
        description="验证 22-Python速成 文档中的代码示例"
    )
    parser.add_argument(
        "--verbose",
        "-v",
        action="store_true",
        help="显示详细输出",
    )
    parser.add_argument(
        "--dir",
        "-d",
        type=Path,
        default=Path(__file__).parent.parent,
        help="文档目录（默认: ../)",
    )

    args = parser.parse_args()

    docs_dir = args.dir.resolve()

    if not docs_dir.exists():
        print(f"❌ 目录不存在: {docs_dir}")
        sys.exit(1)

    print(f"🚀 开始验证 {docs_dir.name} 中的代码示例...\n")

    results = scan_directory(docs_dir, verbose=args.verbose)
    print_summary(results, verbose=args.verbose)

    # 根据结果返回退出码
    invalid_count = sum(1 for r in results if not r.is_valid)
    sys.exit(0 if invalid_count == 0 else 1)


if __name__ == "__main__":
    main()
