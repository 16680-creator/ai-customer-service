"""09 · 语法自测（对应文档 02 篇第十二节）

运行: python 01_basics/09_self_test.py
先自己想，再看下面的实现。
"""

# ---- 本段讲：标准库计数器 Counter 的导入 ----
# Counter 是 dict 的子类，专门用来统计频次，比手写 dict.get(k,0)+1 更简洁。
# 它来自 collections 模块，等价 Java 里自己写 Map<String,Integer> 再 merge。
from collections import Counter


# ---------------------------------------------------------------------------
# ① 给定列表，返回所有偶数的平方
# ---------------------------------------------------------------------------
# ---- 本段讲：列表推导式实现"过滤 + 映射" ----
# 只需要一行：遍历 → 用 x % 2 == 0 过滤 → 对留下的元素平方。
# 类型注解 list[int] 是 3.9+ 的写法，等价 Java 的 List<Integer>，但运行时不检查。
def q1_even_squares(nums: list[int]) -> list[int]:
    return [x * x for x in nums if x % 2 == 0]   # if 在 for 后是过滤，x*x 是映射结果


# ---------------------------------------------------------------------------
# ② 统计字符串中每个字符出现的次数
# ---------------------------------------------------------------------------
# ---- 本段讲：两种统计字符频次的写法 ----
# 方式一直接用 Counter，一行搞定；方式二注释里保留手写版本，便于理解内部逻辑。
def q2_char_count(text: str) -> dict[str, int]:
    # 方式一：Counter（最省事）
    return dict(Counter(text))     # Counter("hello") 得到 Counter({'l':2,...})，dict() 转成普通字典
    # 方式二：手写
    # 下面是等价实现：先建空字典，再逐字符累加，最后返回
    # out: dict[str, int] = {}
    # for ch in text:
    #     out[ch] = out.get(ch, 0) + 1
    # return out


# ---------------------------------------------------------------------------
# ③ 把两个列表合成字典
# ---------------------------------------------------------------------------
# ---- 本段讲：zip + dict 构造器把"键列表 + 值列表"合成字典 ----
# zip 按位置配对成 (key, value) 元组序列，dict() 直接吃这种二元组序列。
# Java 需要显式 for 循环 + put，或 Collectors.toMap。
def q3_zip_to_dict(keys: list, vals: list) -> dict:
    return dict(zip(keys, vals))   # 长度不一致时以短的为准，多余的键值被丢弃


# ---------------------------------------------------------------------------
# ④ 接收任意数量参数，返回最大值，空则返回 None
# ---------------------------------------------------------------------------
# ---- 本段讲：*nums 收集任意位置参数，空参数时返回 None ----
# nums 在函数内是 tuple，可以直接交给内置 max；但 max(()) 会抛 ValueError，
# 所以先用三元表达式判断"是否为空"，等价 Java 里先判 length 再取最大值。
def q4_max_of(*nums):
    return max(nums) if nums else None   # 非空取最大值；空元组是假值，返回 None


# ---------------------------------------------------------------------------
# ⑤ 安全取字典深层值
# ---------------------------------------------------------------------------
# ---- 本段讲：用"点号路径"安全访问嵌套字典 ----
# 逐段下钻，任何一层不是 dict 或缺少该键，就立刻返回 default，避免 KeyError/TypeError。
# 这等价于 Java 里层层判空 Optional 的写法，但可读性高得多。
def q5_get_nested(data, path: str, default=None):
    cur = data                             # cur 指向当前层，从根开始
    for key in path.split("."):            # 按 "." 切分成键序列，如 ["a","b","c"]
        if not isinstance(cur, dict) or key not in cur:   # 不是字典 或 键不存在 → 提前返回默认值
            return default
        cur = cur[key]                     # 下钻一层，等价 map.get(key) 且确定存在
    return cur                             # 全部命中，返回最内层的值


# ---------------------------------------------------------------------------
# 验证
# ---------------------------------------------------------------------------
# ---- 本段讲：一个极简断言器，打印 PASS/FAIL 并返回布尔值 ----
# 用 print + 返回值实现"测试结果收集"，比直接 assert 更友好：不会中断后续用例。
def check(label: str, got, want) -> bool:
    ok = got == want                       # 值比较用 ==；返回 bool
    mark = "PASS" if ok else "FAIL"        # 三元表达式生成标记文本
    print(f"  [{mark}] {label}: got={got!r} want={want!r}")   # !r 让实际值和期望值都带引号，便于比对
    return ok                              # 把结果交回调用方，供最后汇总


# ---- 本段讲：主流程——构造用例列表、统计通过数、用退出码表达成败 ----
# 这里把每个 check 的返回值收集进 results，最后 sum 统计 True 的个数。
# 返回 0/1 给 SystemExit，等价 Java main 里 System.exit(0/1)。
def main() -> int:
    print("语法自测 5 题")
    print("-" * 60)
    results = [                            # 列表里依次执行 7 个检查，元素都是 bool
        check("① 偶数平方", q1_even_squares([1, 2, 3, 4, 5, 6]), [4, 16, 36]),
        check("② 字符计数", q2_char_count("hello"),
              {"h": 1, "e": 1, "l": 2, "o": 1}),      # 跨行传参，括号内可以自由换行
        check("③ 列表转字典", q3_zip_to_dict(["a", "b"], [1, 2]),
              {"a": 1, "b": 2}),
        check("④ 任意参数最大值", q4_max_of(3, 1, 4, 1, 5), 5),
        check("④ 空参数返回 None", q4_max_of(), None),    # 专门测边界：无参数
        check("⑤ 深层取值", q5_get_nested({"a": {"b": {"c": 42}}}, "a.b.c"), 42),
        check("⑤ 路径缺失用默认值", q5_get_nested({"a": {}}, "a.x.c", 0), 0),
    ]
    print("-" * 60)
    print(f"{sum(results)}/{len(results)} 通过")   # True 参与求和等于 1，False 等于 0
    return 0 if all(results) else 1             # 全通过返回 0，否则返回 1


# 仅当作为脚本直接运行时才执行 main；被 import 时不跑测试。
if __name__ == "__main__":
    raise SystemExit(main())            # 用返回码结束进程，等价 System.exit(main())
