"""04 · 推导式：替代 Stream 链式调用

运行: python 02_containers/04_comprehension.py
"""

import sys

print("=" * 60)
print("1. 列表推导式")
print("=" * 60)

nums = list(range(10))
print(f"nums                = {nums}")
print(f"映射 x*2            = {[x * 2 for x in nums]}")
print(f"过滤偶数            = {[x for x in nums if x % 2 == 0]}")
print(f"映射+过滤           = {[x * x for x in nums if x % 2 == 0]}")
print(f"条件表达式          = {[x if x < 5 else 0 for x in nums]}")
print(f"嵌套 flatMap        = {[x for row in [[1, 2], [3, 4], [5]] for x in row]}")
print(f"字符串处理          = {[w.strip().upper() for w in ['  a ', 'b  ', ' c']]}")

# Java 对照：
#   IntStream.range(0,10).filter(x->x%2==0).map(x->x*x).boxed().toList()


print("\n" + "=" * 60)
print("2. 字典推导式")
print("=" * 60)

print(f"反转键值            = {{v: k for k, v in {{'a': 1, 'b': 2}}.items()}}")
print(f"键值变换            = {{k: len(k) for k in ['a', 'bb', 'ccc']}}")
print(f"过滤                = {{k: v for k, v in {{'a': 1, 'b': 2, 'c': 3}}.items() if v > 1}}")
print(f"从列表构造          = {{n: n * n for n in range(4)}}")
print(f"两个列表            = {{k: v for k, v in zip(['x', 'y'], [1, 2])}}")


print("=" * 60)
print("3. 集合推导式")
print("=" * 60)

print(f"余数集合            = {{x % 3 for x in range(10)}}")
print(f"去重长度            = {{len(w) for w in ['a', 'bb', 'cc', 'ddd']}}")


print("=" * 60)
print("4. 生成器表达式：惰性，省内存")
print("=" * 60)

n = 1_000_000

list_comp = [x * x for x in range(n)]
gen_expr = (x * x for x in range(n))

print(f"列表推导式内存      {sys.getsizeof(list_comp):>10,} 字节")
print(f"生成器表达式内存    {sys.getsizeof(gen_expr):>10,} 字节   <- 与 n 无关")

del list_comp   # 释放

print(f"\nsum(x*x for x in range(1_000_000)) = {sum(x * x for x in range(1_000_000)):,}")

# 生成器只能遍历一次
g = (x for x in range(3))
print(f"\nsum(g) 第一次 = {sum(g)}")
print(f"sum(g) 第二次 = {sum(g)}   <- 已耗尽")


print("=" * 60)
print("5. 什么时候不该用推导式（可读性优先）")
print("=" * 60)

data = [
    {"dept": "x", "amount": 100, "valid": True},
    {"dept": "y", "amount": 200, "valid": False},
    {"dept": "x", "amount": 300, "valid": True},
]

# 过度嵌套：一行写完但难读
ugly = [r["amount"] * 2 for r in data if r["valid"] and r["dept"] == "x"]
print(f"推导式（还能接受）  {ugly}")

# 更复杂的逻辑就该拆开
result = []
for r in data:
    if not r["valid"]:
        continue
    if r["dept"] != "x":
        continue
    result.append(r["amount"] * 2)
print(f"拆成循环            {result}")


print("=" * 60)
print("6. 实测：推导式比 for-append 快")
print("=" * 60)

import time

N = 500_000

t0 = time.perf_counter()
out = []
for i in range(N):
    out.append(i * 2)
t_loop = time.perf_counter() - t0

t0 = time.perf_counter()
out2 = [i * 2 for i in range(N)]
t_comp = time.perf_counter() - t0

print(f"for + append   {t_loop*1000:7.2f}ms")
print(f"推导式         {t_comp*1000:7.2f}ms")
print(f"快 {t_loop/max(t_comp,1e-9):.2f} 倍")


print("=" * 60)
print("7. 实战：三个高频推导式")
print("=" * 60)

# ① 展平二维
matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
print(f"展平        {[x for row in matrix for x in row]}")

# ② 转置（zip 更地道）
print(f"转置(zip)   {[list(t) for t in zip(*matrix)]}")

# ③ 建索引映射
items = ["apple", "banana", "cherry"]
print(f"索引映射    {{v: i for i, v in enumerate(items)}} = { {v: i for i, v in enumerate(items)} }")
