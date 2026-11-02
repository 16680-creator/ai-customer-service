"""07 · 容器自测（对应文档 03 篇第十一节）

运行: python 02_containers/07_quiz.py
先自己预测输出，再运行对比。
"""


def check(label: str, got, want) -> bool:
    ok = got == want
    print(f"  [{'PASS' if ok else 'FAIL'}] {label}: got={got!r} want={want!r}")
    return ok


print("=" * 60)
print("① a = [1,2,3]; b = a; b += [4]")
print("=" * 60)

a = [1, 2, 3]
b = a
b += [4]
print(f"  a = {a}")
print(f"  b = {b}")
print("  说明：list 的 += 调用 __iadd__，原地 extend，a 和 b 是同一个对象")

print("\n" + "=" * 60)
print("② a = [1,2,3]; b = a; b = b + [4]")
print("=" * 60)

a = [1, 2, 3]
b = a
b = b + [4]
print(f"  a = {a}")
print(f"  b = {b}")
print("  说明：+ 调用 __add__，创建新列表，b 指向了新对象，a 不变")


print("\n" + "=" * 60)
print("③ 正确生成 5x5 零矩阵")
print("=" * 60)

wrong = [[0] * 5] * 5
wrong[0][0] = 9
print(f"  [[0]*5]*5  -> 第 0 列 = {[row[0] for row in wrong]}   错误：全被改")

right = [[0] * 5 for _ in range(5)]
right[0][0] = 9
print(f"  推导式      -> 第 0 列 = {[row[0] for row in right]}   正确")


print("\n" + "=" * 60)
print("④ 一趟循环分组求和")
print("=" * 60)


def group_sum(records: list[dict]) -> dict[str, int]:
    out: dict[str, int] = {}
    for r in records:
        out[r["n"]] = out.get(r["n"], 0) + r["v"]
    return out


data = [{"n": "a", "v": 3}, {"n": "b", "v": 1}, {"n": "a", "v": 2}]
print(f"  {group_sum(data)}")
print(f"  defaultdict 版：")


def group_sum2(records):
    from collections import defaultdict
    acc = defaultdict(int)
    for r in records:
        acc[r["n"]] += r["v"]
    return dict(acc)


print(f"  {group_sum2(data)}")


print("\n" + "=" * 60)
print("⑤ 两列表构造 dict 并过滤 value > 10")
print("=" * 60)

keys = ["a", "b", "c"]
vals = [5, 20, 30]
result = {k: v for k, v in zip(keys, vals) if v > 10}
print(f"  {result}")


print("\n" + "=" * 60)
print("附加题：下面的输出是什么？")
print("=" * 60)

# 1) 字典遍历顺序
d = {"b": 1, "a": 2, "c": 3}
print(f"  1) dict 保持插入序 -> {list(d)}")

# 2) list * 3 的可变性
inner = [0]
outer = inner * 3
outer[0] = 9
print(f"  2) [0]*3 后改首位 -> {outer}   （同层是同一个对象）")

# 3) 元组包含 list
t = (1, [2, 3])
t[1].append(4)
print(f"  3) 元组里的 list 可改 -> {t}")

# 4) set 里放不可哈希对象
try:
    {[1, 2]}
except TypeError as e:
    print(f"  4) set 放 list -> TypeError: {e}")

# 5) dict.fromkeys 用可变对象做值
bad = dict.fromkeys(["a", "b"], [])
bad["a"].append(1)
print(f"  5) fromkeys 共享值 -> {bad}   <- 所有键指向同一个 list")

good = {k: [] for k in ["a", "b"]}
good["a"].append(1)
print(f"     推导式版本      -> {good}")


print("\n" + "=" * 60)
print("验证")
print("=" * 60)
results = [
    check("④ 分组求和", group_sum(data), {"a": 5, "b": 1}),
    check("⑤ 过滤构造", result, {"b": 20, "c": 30}),
    check("1) 插入序", list(d), ["b", "a", "c"]),
    check("5) fromkeys 共享", bad["a"] == bad["b"], True),
]
print(f"{sum(results)}/{len(results)} 通过")
