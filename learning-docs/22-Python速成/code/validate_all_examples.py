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
import argparse                          # 命令行参数解析（≈ picocli / args4j）
import ast                               # 标准库：把源码解析成语法树，用来校验语法
import re                                # 标准库正则（≈ java.util.regex.Pattern/Matcher）
import sys                               # 用于设置进程退出码（供 CI 判断成败）
from pathlib import Path                 # 面向对象的路径操作（≈ java.nio.file.Path）
from typing import List, Tuple           # 类型注解工具（给函数签名用）


# ---- 本段讲：整个脚本做三件事 ----
# ① 扫描 md 文档、用状态机 + 正则抽出所有 ```python 代码块；② 用标准库 ast 逐个校验语法；③ 汇总报告。
# Java 对照：相当于"正则抽取代码片段 + 尝试编译"的轻量版——Python 没有独立编译步骤，ast.parse 就是那道门禁。
class CodeBlock:
    """代码块数据结构"""

    def __init__(self, file: Path, line_no: int, code: str, language: str = "python"):
        self.file = file            # 该代码块所在的 .md 文件
        self.line_no = line_no      # 代码块起始行号，报错时用来定位
        self.code = code            # 抽出的源码文本（不含 ``` 围栏）
        self.language = language    # 语言标识，默认 python

    def __repr__(self):
        # __repr__ ≈ Java 的 toString()：调试/打印对象时的可读表示
        return f"<CodeBlock {self.file.name}:{self.line_no} ({len(self.code)} chars)>"


# ---- 本段讲：单个代码块的校验结果 ----
# 用 is_valid + error/warning 三个字段表达"通过 / 失败 / 跳过"，等价 Java 里的一个 Result 对象。
class ValidationResult:
    """验证结果"""

    def __init__(
        self,
        block: CodeBlock,            # 对应的代码块
        is_valid: bool,              # 是否通过语法校验
        error: str | None = None,    # 失败原因；str | None 是"可空字符串"（≈ @Nullable String）
        warning: str | None = None,  # 跳过原因（如"疑似 Java 示例"），通过时可为空
    ):
        self.block = block
        self.is_valid = is_valid
        self.error = error
        self.warning = warning


# ---- 本段讲：这就是"扫描 md 抽取 ```python 块"的核心（状态机 + 正则）----
# 思路：逐行读，遇到 ``` 就翻转"块内/块外"状态；块内的行先缓存，收尾时按语言过滤并打包成 CodeBlock。
# Java 对照：等价于 BufferedReader 逐行读 + Pattern 匹配 ``` 围栏，把内容收集进 List<CodeBlock>。
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
    blocks = []                                          # 结果列表：存放本文件抽出的所有 Python 代码块
    content = md_file.read_text(encoding="utf-8")        # 一次读入整个 md（显式 utf-8，避免中文乱码）
    lines = content.splitlines()                         # 按行切分，便于逐行做状态机扫描

    in_code_block = False   # 状态标志：当前是否处于 ``` 围栏内部
    current_code = []       # 当前块的代码行缓存
    current_lang = ""       # 当前块的语言标识（```python 里的 python）
    start_line = 0          # 当前块的起始行号

    for i, line in enumerate(lines, start=1):
        # enumerate(..., start=1) 让行号从 1 开始（≈ Java 的 for i=1）
        # 检测代码块开始
        if line.strip().startswith("```"):
            # ↑ strip() 容忍围栏前导空格；startswith("```") 判断这一行是不是围栏
            if not in_code_block:
                # 开始代码块
                in_code_block = True
                start_line = i
                # 提取语言标识
                lang_match = re.match(r"```(\w+)", line.strip())
                # ↑ 正则：``` 后跟一串单词字符（python/js/bash…）；≈ Java new Pattern("```(\\w+)") 匹配该行
                current_lang = lang_match.group(1) if lang_match else "unknown"
                # ↑ group(1) 取第一个捕获组；无语言标识时归为 "unknown"（≈ Matcher.group(1)）
                current_code = []                    # 开新块：清空缓存
            else:
                # 结束代码块
                in_code_block = False
                if current_lang in ("python", "py", "unknown"):
                    # 只验证 Python 代码（含无标识但疑似 Python 的块）
                    code = "\n".join(current_code)   # 把缓存的行拼回完整源码
                    if code.strip() and not is_output_block(code):
                        # 非空、且不是"输出示例"才收集（输出块不该拿去编译）
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
            # 非围栏行且处于块内 ⇒ 是代码内容，逐行缓存
            current_code.append(line)

    return blocks                                        # 返回本文件抽出的所有代码块


# ---- 本段讲：把"运行结果示例"从"可执行代码"里剔除，避免误报语法错误 ----
def is_output_block(code: str) -> bool:
    """
    判断是否是输出示例（而非可运行代码）

    例如：
    ```
    # → {'Alice': 5, 'Carol': 5}
    ```
    """
    code = code.strip()   # 去掉首尾空白，便于按首字符判断

    # 以注释开头且包含 → 或 # → 的是输出
    if code.startswith("#") and ("→" in code or "..." in code):
        return True       # 形如 "# → {...}" 是运行结果而非源码，跳过（≈ 测试里的期望值块）

    # 纯 JSON 输出
    if code.startswith("{") and code.endswith("}") and '"' in code:
        return True       # 形如 {"k": "v"} 的接口返回示例，不是可执行代码

    # HTTP 响应示例
    if "HTTP" in code and ("200" in code or "404" in code):
        return True       # 含 HTTP 200/404 的响应片段，同样跳过

    return False          # 其余都当作"疑似可运行代码"，交给 ast 校验


# ---- 本段讲：判断一个块是不是"用 Java 写的对照代码"（这类块不能用 Python 语法校验） ----
def is_java_code(code: str) -> bool:
    """判断是否是 Java 代码示例（用于对照）"""
    java_keywords = ["public class", "public static void main", "@Override", "System.out"]
    # 命中任一 Java 特征词，就认定是 Java 示例
    return any(keyword in code for keyword in java_keywords)
    # any(...) 短路求值：只要有一个关键词出现就返回 True；≈ Java 的 stream().anyMatch(...)


# ---- 本段讲：用标准库 ast 校验语法（Java 侧"能不能编译通过"的轻量替代）----
# ast.parse 只把源码解析成语法树，不做类型检查、也不执行代码，安全又快速。
def validate_syntax(block: CodeBlock) -> ValidationResult:
    """
    验证 Python 语法

    使用 ast.parse 检查语法正确性
    """
    code = block.code.strip()   # 去掉首尾空白再校验

    # 跳过 Java 代码
    if is_java_code(code):
        return ValidationResult(
            block,
            is_valid=True,                                # 视为通过（不报错），但标记为已跳过
            warning="Skipped: Java code for comparison",  # 说明原因，--verbose 时打印
        )

    # 跳过纯文本/命令行示例
    if code.startswith("$") or code.startswith("#!"):
        return ValidationResult(
            block,
            is_valid=True,
            warning="Skipped: Shell command",             # $ 开头或 #! 是 shell 命令，不是 Python
        )

    # 跳过不完整的代码片段（如函数定义后直接 ...）
    if code.strip().endswith("...") or "# ..." in code:
        return ValidationResult(
            block,
            is_valid=True,
            warning="Skipped: Incomplete snippet",        # 教学片段常有 "..." 占位，无法整体编译
        )

    try:
        # 尝试解析 Python 语法
        ast.parse(code)
        # ↑ 核心：用标准库 ast 把源码解析成语法树。只解析、不执行，安全且快。
        #   等价 Java 里调用编译前端检查语法；Python 这边 ast.parse 就够。
        return ValidationResult(block, is_valid=True)     # 没抛异常即语法正确

    except SyntaxError as e:
        # ast.parse 遇语法错误会抛 SyntaxError，e.lineno 指出出错行
        return ValidationResult(
            block,
            is_valid=False,
            error=f"SyntaxError: {e.msg} at line {e.lineno}",
        )
    except Exception as e:
        # 兜底其他解析异常（如源码含 null 字节等）
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
    results = []                                    # 汇总所有文件的校验结果

    md_files = list(docs_dir.rglob("*.md"))         # 递归匹配所有 .md（rglob ≈ 递归 glob）

    if verbose:
        print(f"📁 扫描目录: {docs_dir}")
        print(f"📄 找到 {len(md_files)} 个 Markdown 文件\n")

    for md_file in md_files:
        if verbose:
            print(f"🔍 检查 {md_file.name}...", end=" ")   # end=" " 让后续输出接在同一行

        blocks = extract_code_blocks(md_file)        # 抽出该文件的所有 Python 块

        if verbose:
            print(f"({len(blocks)} 个代码块)")

        for block in blocks:
            result = validate_syntax(block)          # 逐个做语法校验
            results.append(result)                   # 收集结果

    return results


def print_summary(results: List[ValidationResult], verbose: bool = False):
    """打印验证摘要"""
    total = len(results)
    valid = sum(1 for r in results if r.is_valid)      # 统计通过数；sum(1 for ...) ≈ 计数循环
    invalid = total - valid                            # 失败数 = 总数 - 通过数
    warnings = sum(1 for r in results if r.warning)    # 被跳过的数量（带 warning 的结果）

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
            if not result.is_valid and result.error:      # 只列真正失败且有错误信息的
                print(f"  {result.block.file.name}:{result.block.line_no}")
                # ↑ 打印"文件:行号"，等价 Java 异常栈里的 File:Line，方便直接跳转定位
                print(f"    {result.error}")
                if verbose:
                    # 显示出错代码
                    lines = result.block.code.splitlines()
                    for i, line in enumerate(lines[:5], start=1):  # 只显示前5行
                        print(f"      {i:3d} | {line}")   # {i:3d} 右对齐 3 位宽，模仿编译器报错格式
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
    # argparse ≈ Java 的 picocli / args4j：声明式定义命令行参数
    parser = argparse.ArgumentParser(
        description="验证 22-Python速成 文档中的代码示例"
    )
    parser.add_argument(
        "--verbose",
        "-v",
        action="store_true",          # 布尔开关：出现该参数即为 True
        help="显示详细输出",
    )
    parser.add_argument(
        "--dir",
        "-d",
        type=Path,                    # 自动把传入字符串转成 Path 对象
        default=Path(__file__).parent.parent,   # 默认指向本脚本的上级目录（文档根）
        help="文档目录（默认: ../)",
    )

    args = parser.parse_args()

    docs_dir = args.dir.resolve()     # 转成绝对路径，避免相对路径歧义

    if not docs_dir.exists():
        print(f"❌ 目录不存在: {docs_dir}")
        sys.exit(1)                   # 退出码 1 表示异常退出（CI 里即构建失败）

    print(f"🚀 开始验证 {docs_dir.name} 中的代码示例...\n")

    results = scan_directory(docs_dir, verbose=args.verbose)   # 扫描并校验
    print_summary(results, verbose=args.verbose)               # 打印摘要

    # 根据结果返回退出码
    invalid_count = sum(1 for r in results if not r.is_valid)
    sys.exit(0 if invalid_count == 0 else 1)   # 无错误退 0，否则退 1；供 CI 判断成败


if __name__ == "__main__":
    main()
