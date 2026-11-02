"""09 · 语法自测（对应文档 02 篇第十二节）

运行: python 01_basics/09_self_test.py
先自己想，再看下面的实现。
"""

from collections import Counter


# ---------------------------------------------------------------------------
# ① 给定列表，返回所有偶数的平方
# ---------------------------------------------------------------------------
def q1_even_squares(nums: list[int]) -> list[int]:
    return [x * x for x in nums if x % 2 == 0]


# ---------------------------------------------------------------------------
# ② 统计字符串中每个字符出现的次数
# ---------------------------------------------------------------------------
def q2_char_count(text: str) -> dict[str, int]:
    # 方式一：Counter（最省事）
    return dict(Counter(text))
    # 方式二：手写
    # out: dict[str, int] = {}
    # for ch in text:
    #     out[ch] = out.get(ch, 0) + 1
    # return out


# ---------------------------------------------------------------------------
# ③ 把两个列表合成字典
# ---------------------------------------------------------------------------
def q3_zip_to_dict(keys: list, vals: list) -> dict:
    return dict(zip(keys, vals))


# ---------------------------------------------------------------------------
# ④ 接收任意数量参数，返回最大值，空则返回 None
# ---------------------------------------------------------------------------
def q4_max_of(*nums):
    return max(nums) if nums else None


# ---------------------------------------------------------------------------
# ⑤ 安全取字典深层值
# ---------------------------------------------------------------------------
def q5_get_nested(data, path: str, default=None):
    cur = data
    for key in path.split("."):
        if not isinstance(cur, dict) or key not in cur:
            return default
        cur = cur[key]
    return cur


# ---------------------------------------------------------------------------
# 验证
# ---------------------------------------------------------------------------
def check(label: str, got, want) -> bool:
    ok = got == want
    mark = "PASS" if ok else "FAIL"
    print(f"  [{mark}] {label}: got={got!r} want={want!r}")
    return ok


def main() -> int:
    print("语法自测 5 题")
    print("-" * 60)
    results = [
        check("① 偶数平方", q1_even_squares([1, 2, 3, 4, 5, 6]), [4, 16, 36]),
        check("② 字符计数", q2_char_count("hello"),
              {"h": 1, "e": 1, "l": 2, "o": 1}),
        check("③ 列表转字典", q3_zip_to_dict(["a", "b"], [1, 2]),
              {"a": 1, "b": 2}),
        check("④ 任意参数最大值", q4_max_of(3, 1, 4, 1, 5), 5),
        check("④ 空参数返回 None", q4_max_of(), None),
        check("⑤ 深层取值", q5_get_nested({"a": {"b": {"c": 42}}}, "a.b.c"), 42),
        check("⑤ 路径缺失用默认值", q5_get_nested({"a": {}}, "a.x.c", 0), 0),
    ]
    print("-" * 60)
    print(f"{sum(results)}/{len(results)} 通过")
    return 0 if all(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
