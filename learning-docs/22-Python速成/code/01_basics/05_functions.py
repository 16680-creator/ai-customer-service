"""05 · 函数基础：默认参数、关键字参数、*args/**kwargs

运行: python 01_basics/05_functions.py
"""

print("=" * 60)
print("1. 基本定义与调用")
print("=" * 60)

def greet(name: str, greeting: str = "你好") -> str:
    return f"{greeting}，{name}"

print(greet("张三"))
print(greet("张三", "早上好"))
print(greet(name="张三", greeting="嗨"))          # 关键字参数（Java 7 没有）
print(greet(greeting="嗨", name="张三"))          # 顺序无关


print("\n" + "=" * 60)
print("2. *args / **kwargs")
print("=" * 60)

def f(a, b=2, *args, c, d=4, **kwargs):
    print(f"  a={a} b={b} args={args} c={c} d={d} kwargs={kwargs}")

print("f(1, 3, 5, 6, c=7, e=8):")
f(1, 3, 5, 6, c=7, e=8)

def sum_all(*nums) -> float:
    return sum(nums)

print(f"\nsum_all(1,2,3)      = {sum_all(1, 2, 3)}")
print(f"sum_all(*[1,2,3])   = {sum_all(*[1, 2, 3])}   <- 解包")

def build_config(**opts) -> dict:
    return opts

print(f"\nbuild_config(host='h', port=1) = {build_config(host='h', port=1)}")
print(f"build_config(**{{'a':1}})       = {build_config(**{'a': 1})}")

def keyword_only(a, *, b, c=3):
    return (a, b, c)

print(f"\nkeyword_only(1, b=2) = {keyword_only(1, b=2)}")
try:
    keyword_only(1, 2)
except TypeError as e:
    print(f"keyword_only(1, 2) -> TypeError: {e}")


print("\n" + "=" * 60)
print("3. 默认参数陷阱（必考）")
print("=" * 60)

def bad(item, lst=[]):
    lst.append(item)
    return lst

print(f"bad(1) = {bad(1)}")
print(f"bad(2) = {bad(2)}    <- 不是 [2]，累积了！")

def good(item, lst=None):
    if lst is None:
        lst = []
    lst.append(item)
    return lst

print(f"\ngood(1) = {good(1)}")
print(f"good(2) = {good(2)}    <- 正确")

# 原因：默认值在 def 执行时求值一次，绑定到函数对象上
print(f"\nbad.__defaults__ = {bad.__defaults__}   <- 同一个 list 被复用")


print("=" * 60)
print("4. 函数是一等公民")
print("=" * 60)

def double(x):
    return x * 2

f = double                       # 赋值给变量
print(f"f(3)             = {f(3)}")

def apply(fn, v):                # 作为参数
    return fn(v)

print(f"apply(double, 5) = {apply(double, 5)}")

def make_multiplier(n):          # 作为返回值（闭包）
    def inner(x):
        return x * n
    return inner

triple = make_multiplier(3)
print(f"triple(4)        = {triple(4)}")

ops = {"double": double, "square": lambda x: x * x}   # 放进容器
print(f"ops['square'](4) = {ops['square'](4)}")


print("=" * 60)
print("5. 常用内置函数")
print("=" * 60)

data = [3, 1, 4, 1, 5, 9, 2]
print(f"sorted(data)                = {sorted(data)}")
print(f"sorted(data, reverse=True)  = {sorted(data, reverse=True)}")
print(f"list(map(str, data[:3]))    = {list(map(str, data[:3]))}")
print(f"list(filter(lambda x: x>3, data)) = {list(filter(lambda x: x > 3, data))}")
print(f"any(x > 8 for x in data)    = {any(x > 8 for x in data)}")
print(f"all(x > 0 for x in data)    = {all(x > 0 for x in data)}")
print(f"max(data), min(data), sum(data) = {max(data)}, {min(data)}, {sum(data)}")

orders = [
    {"no": "A", "amount": 300},
    {"no": "B", "amount": 100},
    {"no": "C", "amount": 200},
]
print(f"\nsorted by amount            = {[o['no'] for o in sorted(orders, key=lambda o: o['amount'])]}")
print(f"max by amount               = {max(orders, key=lambda o: o['amount'])}")
print(f"sum of amounts              = {sum(o['amount'] for o in orders)}")

# sorted 的稳定性：先排次要，再排主要
rows = [("a", 2), ("b", 1), ("a", 1)]
by_second = sorted(rows, key=lambda r: r[1])
by_first_then_second = sorted(by_second, key=lambda r: r[0])
print(f"\n稳定排序 -> {by_first_then_second}")


print("=" * 60)
print("6. lambda 的限制")
print("=" * 60)

square = lambda x: x * x
print(f"square(4) = {square(4)}")

# 只能单表达式，不能有语句块（Java 的 lambda 可以写 {} 块）
# 需要多语句就写 def
label = lambda s: "及格" if s >= 60 else "不及格"
print(f"label(75) = {label(75)}")
