"""02 · 类型、数值与真值判断

运行: python 01_basics/02_types.py
"""

from decimal import Decimal

print("=" * 60)
print("1. 基本类型：没有 int/long/float/double 之分")
print("=" * 60)

x = 42          # int，任意精度
y = 3.14        # float，就是 Java 的 double
s = "hello"     # str，不可变
b = True        # bool，注意大写
n = None        # 空值

for v in (x, y, s, b, n):
    print(f"{v!r:12} -> {type(v).__name__}")

# Java 的 int 是 32 位，Python 可以无限大
big = 2 ** 200
print(f"\n2**200 = {big}")
print(f"位数：{len(str(big))}")


print("\n" + "=" * 60)
print("2. 除法：Java 程序员第一个坑")
print("=" * 60)

print(f"7 / 2   = {7 / 2}      <- 真除法，不是 3")
print(f"7 // 2  = {7 // 2}       <- 整除")
print(f"7 % 2   = {7 % 2}")
print(f"2 ** 10 = {2 ** 10}     <- 幂运算")
print(f"divmod(7, 2) = {divmod(7, 2)}")

# 负数整除是向下取整，Java 是向零取整
print(f"\n-7 // 2 = {-7 // 2}    <- Java 是 -3")
print(f"-7 % 2  = {-7 % 2}     <- Java 是 -1")


print("\n" + "=" * 60)
print("3. 浮点精度：算钱必须用 Decimal")
print("=" * 60)

print(f"0.1 + 0.2            = {0.1 + 0.2}")
print(f"0.1 + 0.2 == 0.3     = {0.1 + 0.2 == 0.3}")

import math
print(f"math.isclose(...)    = {math.isclose(0.1 + 0.2, 0.3)}")

print(f"Decimal('0.1')+0.2   = {Decimal('0.1') + Decimal('0.2')}")
print(f"Decimal(0.1)         = {Decimal(0.1)}   <- 用 float 构造会带误差")
print(f"Decimal('0.1')       = {Decimal('0.1')} <- 永远用字符串构造")


print("\n" + "=" * 60)
print("4. 真值判断：空容器/0/空串/None 都是假")
print("=" * 60)

falsy = [None, False, 0, 0.0, "", [], {}, set(), ()]
print("假值：", [repr(v) for v in falsy])

items = []
print(f"\nif not items:  -> {not items}     <- Pythonic")
print(f"len(items)==0: -> {len(items) == 0}")

# 判空一律用 is
v = None
print(f"\nv == None -> {v == None}    能跑但不对")
print(f"v is None -> {v is None}    正确")


print("\n" + "=" * 60)
print("5. 类型转换与检查")
print("=" * 60)

print(f"int('42')         = {int('42')}")
print(f"int('1010', 2)    = {int('1010', 2)}    <- 二进制解析")
print(f"float('3.14')     = {float('3.14')}")
print(f"str(42)           = {str(42)!r}")
print(f"bool('')          = {bool('')}")

# is 与 == 的区别（小整数驻留）
a, c = 256, 256
print(f"\n256 is 256        = {a is c}     <- 驻留")
a, c = 257, 257
print(f"257 is 257        = {a is c}    <- 不一定，取决于实现")
print(f"257 == 257        = {a == c}     <- 值比较永远用 ==")

print(f"\nisinstance(42, int)         = {isinstance(42, int)}")
print(f"isinstance(True, int)       = {isinstance(True, int)}   <- bool 是 int 子类")
print(f"isinstance('x', (str,int))  = {isinstance('x', (str, int))}")


print("\n" + "=" * 60)
print("6. 多重赋值与交换")
print("=" * 60)

p, q = 1, 2
print(f"初始: p={p}, q={q}")
p, q = q, p
print(f"交换后: p={p}, q={q}   <- 不需要临时变量")

a, b, *rest = [1, 2, 3, 4, 5]
print(f"\na, b, *rest = [1..5]  -> a={a}, b={b}, rest={rest}")

first, *_, last = [1, 2, 3, 4]
print(f"first, *_, last       -> {first}, {last}")

# 下划线数字分隔符（同 Java 7+）
print(f"\n1_000_000 = {1_000_000}")
