"""02 · dict：用得比 list 还多

运行: python 02_containers/02_dict.py
"""

from collections import defaultdict, Counter

print("=" * 60)
print("1. 创建")
print("=" * 60)

print(f"{{}}                            -> {{}}")
print(f'{{"a": 1}}                       -> {{"a": 1}}')
print(f"dict(a=1, b=2)                -> {dict(a=1, b=2)}")
print(f"dict([('a',1),('b',2)])       -> {dict([('a', 1), ('b', 2)])}")
print(f"dict(zip(['a','b'],[1,2]))    -> {dict(zip(['a', 'b'], [1, 2]))}")
print(f"dict.fromkeys(['a','b'], 0)   -> {dict.fromkeys(['a', 'b'], 0)}")
print(f"{{k: len(k) for k in ['a','bb']}} -> {{k: len(k) for k in ['a', 'bb']}}")


print("\n" + "=" * 60)
print("2. 取值：永远优先 .get()")
print("=" * 60)

d = {"a": 1}

print(f'd["a"]           = {d["a"]}')
try:
    d["missing"]
except KeyError as e:
    print(f'd["missing"]     -> KeyError: {e}   <- Java 的 Map.get 返回 null，这里直接炸')

print(f'd.get("missing")        = {d.get("missing")}')
print(f'd.get("missing", 0)     = {d.get("missing", 0)}')

# or 做默认值的陷阱
d2 = {"count": 0}
print(f"\nd2.get('count') or 100  = {d2.get('count') or 100}   <- 0 被当假值")
print(f"d2.get('count', 100)    = {d2.get('count', 100)}   <- 正确")


print("=" * 60)
print("3. 增删改")
print("=" * 60)

d = {}
d["a"] = 1
d["a"] = 2
print(f'覆盖 -> {d}')

d.setdefault("b", [])
d.setdefault("b", []).append(1)
print(f'setdefault 分组 -> {d}')

d.update({"c": 3, "d": 4})
print(f'update -> {d}')

print(f'pop("a") -> {d.pop("a")}   剩余 {d}')
print(f'pop("zzz", None) -> {d.pop("zzz", None)}   <- 安全删除')

del d["b"]
print(f'del d["b"] -> {d}')

d.clear()
print(f"clear -> {d}")


print("=" * 60)
print("4. 合并（3.9+）")
print("=" * 60)

a = {"x": 1, "y": 2}
b = {"y": 20, "z": 30}
print(f"a | b       = {a | b}      <- 后者覆盖前者，返回新字典")
print(f"a 未被修改  = {a}")
print(f"{{**a, **b}}  = {{**a, **b}}")

a.update(b)
print(f"a.update(b) = {a}      <- 原地修改")


print("=" * 60)
print("5. 遍历")
print("=" * 60)

d = {"a": 1, "b": 2, "c": 3}
print(f"keys()   -> {list(d.keys())}")
print(f"values() -> {list(d.values())}")
print(f"items()  -> {list(d.items())}")

print("\nfor k, v in d.items():")
for k, v in d.items():
    print(f"  {k} = {v}")

# 遍历时改大小会报错
d2 = {"a": 1, "b": 2}
try:
    for k in d2:
        del d2[k]
except RuntimeError as e:
    print(f"\n遍历时删除 -> RuntimeError: {e}")

d2 = {"a": 1, "b": 2}
for k in list(d2.keys()):
    del d2[k]
print(f"先复制 keys -> {d2}")


print("=" * 60)
print("6. 分组统计（业务最高频）")
print("=" * 60)

orders = [
    {"type": "A", "amount": 100},
    {"type": "B", "amount": 200},
    {"type": "A", "amount": 300},
    {"type": "B", "amount": 50},
]

# ① defaultdict 累加
totals = defaultdict(float)
for o in orders:
    totals[o["type"]] += o["amount"]
print(f"分组求和     {dict(totals)}")

# ② defaultdict 收集（等价 Collectors.groupingBy）
grouped = defaultdict(list)
for o in orders:
    grouped[o["type"]].append(o["amount"])
print(f"分组收集     {dict(grouped)}")

# ③ 复合结构：分组 + count + total
acc = defaultdict(lambda: {"count": 0, "total": 0.0})
for o in orders:
    slot = acc[o["type"]]
    slot["count"] += 1
    slot["total"] += o["amount"]
print(f"分组统计     {dict(acc)}")


print("=" * 60)
print("7. Counter：计数专用")
print("=" * 60)

c = Counter("hello world")
print(f"Counter('hello world')     {dict(c)}")
print(f"most_common(3)             {c.most_common(3)}")
print(f"elements()                 {list(c.elements())[:6]}")

c2 = Counter(["a", "b", "a"])
c3 = Counter(["a"])
print(f"\nCounter 加法   {c2 + c3}")
print(f"Counter 减法   {c2 - c3}")


print("=" * 60)
print("8. 反转字典（值访问键）")
print("=" * 60)

d = {"a": 1, "b": 2, "c": 1}
reversed_map = defaultdict(list)
for k, v in d.items():
    reversed_map[v].append(k)
print(f"原字典 {d}")
print(f"反转   {dict(reversed_map)}   <- 一对多要收成 list")
