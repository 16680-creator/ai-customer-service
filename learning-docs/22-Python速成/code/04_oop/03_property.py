"""03 · @property 与魔术方法

运行: python 04_oop/03_property.py
"""

from functools import cached_property, total_ordering

print("=" * 60)
print("1. @property：比 getter/setter 优雅")
print("=" * 60)

class Account:
    def __init__(self, balance=0):
        self._balance = balance

    @property
    def balance(self):
        """只读属性，访问方式像字段"""
        return self._balance

    @balance.setter
    def balance(self, value):
        if value < 0:
            raise ValueError(f"余额不能为负：{value}")
        self._balance = value


a = Account(100)
print(f"  a.balance      = {a.balance}      <- 不用写括号")
a.balance = 200
print(f"  赋值后         = {a.balance}")

try:
    a.balance = -1
except ValueError as e:
    print(f"  设负数         -> ValueError: {e}")


print("\n" + "=" * 60)
print("2. 计算属性")
print("=" * 60)

class Order:
    def __init__(self, price, qty):
        self.price = price
        self.qty = qty

    @property
    def total(self):
        return self.price * self.qty

o = Order(10, 3)
print(f"  o.total = {o.total}   <- 每次访问都计算")


print("=" * 60)
print("3. cached_property：重计算场景")
print("=" * 60)

class Report:
    def __init__(self, rows):
        self.rows = rows
        self.calc_count = 0

    @cached_property
    def summary(self):
        self.calc_count += 1
        return sum(self.rows)

r = Report([1, 2, 3])
print(f"  第一次 summary = {r.summary}，计算次数 {r.calc_count}")
print(f"  第二次 summary = {r.summary}，计算次数 {r.calc_count}   <- 命中缓存")
print("  注意：cached_property 要求类没有 __slots__，且实例可写")


print("=" * 60)
print("4. 魔术方法：运算符重载")
print("=" * 60)

@total_ordering
class Money:
    def __init__(self, amount, currency="CNY"):
        self.amount = amount
        self.currency = currency

    def __repr__(self):
        return f"Money({self.amount!r}, {self.currency!r})"

    def __str__(self):
        return f"{self.amount:.2f} {self.currency}"

    def __eq__(self, other):
        if not isinstance(other, Money):
            return NotImplemented
        return (self.amount, self.currency) == (other.amount, other.currency)

    def __hash__(self):
        return hash((self.amount, self.currency))

    def _check(self, other):
        if not isinstance(other, Money):
            raise TypeError("只能和 Money 运算")
        if self.currency != other.currency:
            raise ValueError(f"币种不一致：{self.currency} vs {other.currency}")

    def __add__(self, other):
        self._check(other)
        return Money(self.amount + other.amount, self.currency)

    def __sub__(self, other):
        self._check(other)
        return Money(self.amount - other.amount, self.currency)

    def __mul__(self, n):
        return Money(self.amount * n, self.currency)

    def __lt__(self, other):
        self._check(other)
        return self.amount < other.amount

    def __bool__(self):
        return self.amount != 0

    def __len__(self):
        return int(self.amount)


m1, m2 = Money(10), Money(5.5)
print(f"  str(m1)       = {m1}")
print(f"  repr(m1)      = {m1!r}")
print(f"  m1 + m2       = {m1 + m2}")
print(f"  m1 - m2       = {m1 - m2}")
print(f"  m1 * 3        = {m1 * 3}")
print(f"  m1 > m2       = {m1 > m2}     <- total_ordering 从 __lt__ 推导")
print(f"  sorted        = {sorted([Money(3), Money(1)])}")
print(f"  bool(Money(0)) = {bool(Money(0))}")
print(f"  len(Money(7.9)) = {len(Money(7.9))}")

try:
    m1 + Money(1, "USD")
except ValueError as e:
    print(f"  跨币种相加     -> ValueError: {e}")


print("\n" + "=" * 60)
print("5. 容器协议：让对象可迭代/可索引")
print("=" * 60)

class Page:
    def __init__(self, items):
        self.items = list(items)

    def __len__(self):
        return len(self.items)

    def __getitem__(self, i):
        return self.items[i]

    def __iter__(self):
        return iter(self.items)

    def __contains__(self, v):
        return v in self.items

p = Page([1, 2, 3])
print(f"  len(p)      = {len(p)}")
print(f"  p[0]        = {p[0]}")
print(f"  p[-1]       = {p[-1]}")
print(f"  2 in p      = {2 in p}")
print(f"  list(p)     = {list(p)}")
print(f"  for 循环    = {[x * 10 for x in p]}")
print(f"  p[1:]       = {p[1:]}   <- 切片也自动支持")


print("=" * 60)
print("6. __call__：让实例可调用")
print("=" * 60)

class Adder:
    def __init__(self, n):
        self.n = n

    def __call__(self, x):
        return x + self.n

add10 = Adder(10)
print(f"  add10(5) = {add10(5)}   <- 实例像函数一样调用")
print(f"  callable(add10) = {callable(add10)}")


print("=" * 60)
print("7. 常见坑：__eq__ 定义了却没 __hash__")
print("=" * 60)

class NoHash:
    def __init__(self, no):
        self.no = no

    def __eq__(self, other):
        return isinstance(other, NoHash) and self.no == other.no
    # 没定义 __hash__

try:
    {NoHash("A")}
except TypeError as e:
    print(f"  放 set 里 -> TypeError: {e}")
    print("  原因：定义 __eq__ 后 Python 自动把 __hash__ 设为 None")


print("=" * 60)
print("8. __repr__ vs __str__")
print("=" * 60)

class Demo:
    def __str__(self):
        return "给用户看的"

    def __repr__(self):
        return "给开发者看的"

d = Demo()
print(f"  str(d)   = {d}")
print(f"  repr(d)  = {d!r}")
print(f"  f'{{d}}'   = {f'{d}'}")
print(f"  f'{{d!r}}' = {f'{d!r}'}")
print("  只写一个时，写 __repr__（__str__ 缺失会 fallback 到它）")
