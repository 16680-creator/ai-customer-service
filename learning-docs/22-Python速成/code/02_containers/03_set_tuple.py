"""03 · set / tuple / frozenset

运行: python 02_containers/03_set_tuple.py
"""

import sys
from typing import NamedTuple

print("=" * 60)
print("1. set：去重 + O(1) 成员判断")
print("=" * 60)

s = {1, 2, 3}
print(f"{{1,2,3}}      -> {s}")
print(f"set([1,2,2,3]) -> {set([1, 2, 2, 3])}")
print(f"set()          -> {set()}     <- 空集合不能用 {{}}")
print(f"type({{}})       -> {type({}).__name__}    <- 空大括号是 dict！")

s.add(4)
print(f"\nadd(4)         -> {s}")
s.discard(4)
print(f"discard(4)     -> {s}    <- 不存在也不报错")
try:
    s.remove(99)
except KeyError as e:
    print(f"remove(99)     -> KeyError: {e}   <- 不存在会抛")

a, b = {1, 2, 3}, {2, 3, 4}
print(f"\na={a}, b={b}")
print(f"a | b  并集    {a | b}")
print(f"a & b  交集    {a & b}")
print(f"a - b  差集    {a - b}")
print(f"a ^ b  对称差  {a ^ b}")
is_subset = {1, 2} <= a
print(f"子集判断 {{1,2}} <= a  -> {is_subset}")


print("\n" + "=" * 60)
print("2. 为什么用 set 而不是 list 判断存在")
print("=" * 60)

import time

big_list = list(range(200_000))
big_set = set(big_list)

t0 = time.perf_counter()
for _ in range(200):
    199_999 in big_list
t_list = time.perf_counter() - t0

t0 = time.perf_counter()
for _ in range(200):
    199_999 in big_set
t_set = time.perf_counter() - t0

print(f"200 次 'in list' 耗时 {t_list*1000:.2f}ms   O(n)")
print(f"200 次 'in set'  耗时 {t_set*1000:.4f}ms   O(1)")
print(f"快 {t_list/max(t_set, 1e-9):.0f} 倍")


print("=" * 60)
print("3. tuple：不只是不可变 list")
print("=" * 60)

t = (1, 2, 3)
print(f"t            = {t}")
print(f"t[0]         = {t[0]}")
try:
    t[0] = 9
except TypeError as e:
    print(f"t[0] = 9     -> TypeError: {e}")

print(f"\n(1) 类型     -> {type((1)).__name__}     <- 不是 tuple！")
print(f"(1,) 类型    -> {type((1,)).__name__}")

# 函数返回多值
def divmod2(a, b):
    return a // b, a % b

q, r = divmod2(7, 2)
print(f"\ndivmod2(7,2) -> q={q}, r={r}")

# 可哈希，能当 dict 键
cache = {(1, 2): "value"}
print(f"cache[(1,2)] -> {cache[(1, 2)]}")


print("=" * 60)
print("4. NamedTuple：Java record 的表亲")
print("=" * 60)

class Point(NamedTuple):
    x: int
    y: int

p = Point(1, 2)
print(f"p              = {p}")
print(f"p.x, p.y       = {p.x}, {p.y}     <- 命名访问")
print(f"p[0]           = {p[0]}       <- 位置访问")
x, y = p
print(f"解包           = {x}, {y}")
print(f"p._replace(x=9)= {p._replace(x=9)}")
print(f"p._asdict()    = {p._asdict()}")


print("=" * 60)
print("5. frozenset：可哈希的集合")
print("=" * 60)

fs = frozenset({1, 2, 3})
print(f"fs             = {fs}")
print(f"能作 dict 键   -> {{frozenset({{1,2}}): 'v'}} = {{frozenset({1, 2}): 'v'}}")
try:
    fs.add(4)
except AttributeError as e:
    print(f"fs.add(4)      -> AttributeError: {e}")


print("=" * 60)
print("6. 四种容器的内存与特性对比")
print("=" * 60)

samples = {
    "list    [1,2,3]": [1, 2, 3],
    "tuple   (1,2,3)": (1, 2, 3),
    "set     {1,2,3}": {1, 2, 3},
    "dict    {'a':1}": {"a": 1},
}
for label, obj in samples.items():
    print(f"{label:20} 内存 {sys.getsizeof(obj):4d} 字节  类型 {type(obj).__name__}")


print("=" * 60)
print("7. 实战：按多重键去重")
print("=" * 60)

records = [
    {"order_no": "A", "sku": "S1"},
    {"order_no": "A", "sku": "S1"},
    {"order_no": "A", "sku": "S2"},
]
seen = set()
unique = []
for r in records:
    key = (r["order_no"], r["sku"])     # 元组当复合键
    if key not in seen:
        seen.add(key)
        unique.append(r)
print(f"原始 {len(records)} 条 -> 去重 {len(unique)} 条")
print(f"{unique}")
