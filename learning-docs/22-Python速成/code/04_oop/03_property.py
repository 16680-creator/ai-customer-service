"""03 · @property 与魔术方法

运行: python 04_oop/03_property.py
"""

from functools import cached_property, total_ordering   # 缓存属性 + 用 __lt__ 自动补全其余比较符

# ============================================================
# 本节演示：@property 实现 getter / setter 的字段式访问
# 为什么重要：调用方写 a.balance（无括号），未来把普通字段升级成带校验的属性时不用改任何调用方。
# Java 对照：等价 getBalance()/setBalance()，但语法上像字段；校验逻辑写在 setter 里。
# ============================================================

print("=" * 60)
print("1. @property：比 getter/setter 优雅")
print("=" * 60)

class Account:
    def __init__(self, balance=0):
        self._balance = balance      # 真实数据放在 _balance，避免与属性名 balance 冲突（否则无限递归）

    @property
    def balance(self):
        """只读属性，访问方式像字段"""
        return self._balance         # 读：a.balance 时执行这里

    @balance.setter
    def balance(self, value):
        if value < 0:                # 写校验：等价 Java setter 里的 if 判断
            raise ValueError(f"余额不能为负：{value}")
        self._balance = value        # 校验通过才落库到内部字段


a = Account(100)                     # 构造时走 __init__，不经过 setter
print(f"  a.balance      = {a.balance}      <- 不用写括号")   # 看起来像字段，实际调用 getter
a.balance = 200                      # 赋值语法，实际调用 setter
print(f"  赋值后         = {a.balance}")   # 200

try:
    a.balance = -1                   # 被 setter 拦截
except ValueError as e:
    print(f"  设负数         -> ValueError: {e}")


print("\n" + "=" * 60)
print("2. 计算属性")
print("=" * 60)

# ============================================================
# 本节演示：计算属性（没有存储字段，每次访问现算）
# 为什么重要：把"派生值"暴露成字段形式，调用方不需要知道它是算出来的，也不用担心数据不同步。
# Java 对照：只能写成 getTotal()，调用时必须带括号。
# ============================================================

class Order:
    def __init__(self, price, qty):
        self.price = price           # 单价（示例用 float，真实金额应 Decimal）
        self.qty = qty               # 数量

    @property
    def total(self):                 # 没有对应存储字段，纯计算
        return self.price * self.qty # 每次访问都重新计算，price/qty 变了结果自动变

o = Order(10, 3)
print(f"  o.total = {o.total}   <- 每次访问都计算")   # 30，无需手动同步


print("=" * 60)
print("3. cached_property：重计算场景")
print("=" * 60)

# ============================================================
# 本节演示：@cached_property —— 第一次算完写进实例 __dict__，之后直接读缓存
# 为什么重要：重计算场景（聚合、远程调用）性能差别巨大；但要牢记"输入变了必须手动失效缓存"。
# Java 对照：相当于 Spring 的 @Cacheable，或自己写一个 volatile 字段 + 双重检查。
# ============================================================

class Report:
    def __init__(self, rows):
        self.rows = rows             # 数据源
        self.calc_count = 0          # 计数器：用来证明只算了一次

    @cached_property
    def summary(self):
        self.calc_count += 1         # 只有真正执行函数时才会加 1
        return sum(self.rows)        # 求和结果会被缓存到 self.__dict__["summary"]

r = Report([1, 2, 3])
print(f"  第一次 summary = {r.summary}，计算次数 {r.calc_count}")   # 6，次数 1（执行函数）
print(f"  第二次 summary = {r.summary}，计算次数 {r.calc_count}   <- 命中缓存")   # 6，次数仍是 1
print("  注意：cached_property 要求类没有 __slots__，且实例可写")


print("=" * 60)
print("4. 魔术方法：运算符重载")
print("=" * 60)

# ============================================================
# 本节演示：魔术方法全家桶（显示 / 相等 / 哈希 / 加减乘 / 比较 / 布尔 / 长度）
# 为什么重要：Java 只能用 equals/hashCode/toString，Python 可以让运算符作用于自定义类型，代码更自然。
# Java 对照：__repr__/__str__≈toString，__eq__/__hash__≈equals/hashCode，__lt__≈compareTo，__add__ 无对应。
# ============================================================

@total_ordering                      # 只需实现 __eq__ + __lt__，其余 <= > >= 自动推导
class Money:
    def __init__(self, amount, currency="CNY"):
        self.amount = amount         # 金额
        self.currency = currency     # 币种

    def __repr__(self):
        return f"Money({self.amount!r}, {self.currency!r})"   # 调试/容器显示用，形式可重建对象

    def __str__(self):
        return f"{self.amount:.2f} {self.currency}"   # 面向用户显示，:.2f 保留两位小数

    def __eq__(self, other):
        if not isinstance(other, Money):
            return NotImplemented    # 类型不同交给解释器处理，不要直接 False
        return (self.amount, self.currency) == (other.amount, other.currency)   # 金额和币种都相同才算相等

    def __hash__(self):
        return hash((self.amount, self.currency))   # 与 __eq__ 用同一组字段，保证契约一致

    def _check(self, other):
        if not isinstance(other, Money):
            raise TypeError("只能和 Money 运算")     # 类型不合法
        if self.currency != other.currency:
            raise ValueError(f"币种不一致：{self.currency} vs {other.currency}")   # 不同币种不能直接算

    def __add__(self, other):
        self._check(other)           # 先校验
        return Money(self.amount + other.amount, self.currency)   # 返回新对象，不修改自身（不可变风格）

    def __sub__(self, other):
        self._check(other)
        return Money(self.amount - other.amount, self.currency)   # 减法同理

    def __mul__(self, n):
        return Money(self.amount * n, self.currency)   # 乘以标量，币种不变

    def __lt__(self, other):
        self._check(other)
        return self.amount < other.amount   # 只按金额比较，total_ordering 用它推导其余比较符

    def __bool__(self):
        return self.amount != 0      # 金额为 0 时对象在 if 中视为假（Java 不允许自定义真假）

    def __len__(self):
        return int(self.amount)      # 让 len(money) 返回整数金额（示例用法）


m1, m2 = Money(10), Money(5.5)
print(f"  str(m1)       = {m1}")            # 走 __str__ → 10.00 CNY
print(f"  repr(m1)      = {m1!r}")         # !r 强制走 __repr__
print(f"  m1 + m2       = {m1 + m2}")      # 走 __add__ 再走 __str__
print(f"  m1 - m2       = {m1 - m2}")      # 走 __sub__
print(f"  m1 * 3        = {m1 * 3}")       # 走 __mul__
print(f"  m1 > m2       = {m1 > m2}     <- total_ordering 从 __lt__ 推导")   # 走 __lt__ 的反向推导
print(f"  sorted        = {sorted([Money(3), Money(1)])}")   # 排序依赖 __lt__，显示依赖 __repr__
print(f"  bool(Money(0)) = {bool(Money(0))}")   # 走 __bool__ → False
print(f"  len(Money(7.9)) = {len(Money(7.9))}")  # 走 __len__ → 7（int 截断）

try:
    m1 + Money(1, "USD")             # 币种不同
except ValueError as e:
    print(f"  跨币种相加     -> ValueError: {e}")


print("\n" + "=" * 60)
print("5. 容器协议：让对象可迭代/可索引")
print("=" * 60)

# ============================================================
# 本节演示：容器协议（len / 下标 / 迭代 / in），让自定义对象表现得像 list
# 为什么重要：实现这几个魔术方法后，for、切片、in、list() 全部自动可用，无需继承任何容器类。
# Java 对照：需要 implements Iterable 并实现 iterator()，而且切片、in 之类的语法糖仍要自己写方法。
# ============================================================

class Page:
    def __init__(self, items):
        self.items = list(items)     # 复制一份，避免外部列表被改动影响本对象

    def __len__(self):
        return len(self.items)       # 支持 len(p)

    def __getitem__(self, i):
        return self.items[i]         # 支持 p[0]、p[-1]；i 是 slice 时支持 p[1:]

    def __iter__(self):
        return iter(self.items)      # 支持 for x in p、list(p)

    def __contains__(self, v):
        return v in self.items       # 支持 2 in p

p = Page([1, 2, 3])
print(f"  len(p)      = {len(p)}")           # 3 → __len__
print(f"  p[0]        = {p[0]}")             # 1 → __getitem__
print(f"  p[-1]       = {p[-1]}")            # 3，负索引从尾部数
print(f"  2 in p      = {2 in p}")           # True → __contains__
print(f"  list(p)     = {list(p)}")          # [1, 2, 3] → __iter__
print(f"  for 循环    = {[x * 10 for x in p]}")   # 列表推导式内部走 __iter__
print(f"  p[1:]       = {p[1:]}   <- 切片也自动支持")   # slice 对象被传给 __getitem__，list 原生支持


print("=" * 60)
print("6. __call__：让实例可调用")
print("=" * 60)

# ============================================================
# 本节演示：__call__ 让实例像函数一样调用
# 为什么重要：可以让"配置"存在实例字段里，把实例当一个带状态的函数传递（装饰器、策略对象常用）。
# Java 对照：实现 Function<Integer,Integer> 并重写 apply，但调用时仍写 f.apply(5)，不能写 f(5)。
# ============================================================

class Adder:
    def __init__(self, n):
        self.n = n                   # 把"加数"保存在实例状态里

    def __call__(self, x):
        return x + self.n            # add10(5) 会执行这里，self.n 是构造时固定的 10

add10 = Adder(10)                    # 创建实例
print(f"  add10(5) = {add10(5)}   <- 实例像函数一样调用")   # 15
print(f"  callable(add10) = {callable(add10)}")            # True，因为实现了 __call__


print("=" * 60)
print("7. 常见坑：__eq__ 定义了却没 __hash__")
print("=" * 60)

# ============================================================
# 本节演示：只定义 __eq__ 不定义 __hash__ 的后果（对象变得不可哈希）
# 为什么重要：Python 认为"你既然重定义了相等语义，默认的哈希就不再安全"，于是把 __hash__ 置为 None。
# Java 对照：Java 不强制关联，只定义 equals 不定义 hashCode 编译能过，但放进 HashMap 会出现"找不到"的诡异 bug。
# ============================================================

class NoHash:
    def __init__(self, no):
        self.no = no                 # 唯一标识

    def __eq__(self, other):
        return isinstance(other, NoHash) and self.no == other.no   # 只比较 no
    # 没定义 __hash__

try:
    {NoHash("A")}                    # 放进 set 需要哈希，而 __hash__ 已被自动设为 None
except TypeError as e:
    print(f"  放 set 里 -> TypeError: {e}")
    print("  原因：定义 __eq__ 后 Python 自动把 __hash__ 设为 None")


print("=" * 60)
print("8. __repr__ vs __str__")
print("=" * 60)

# ============================================================
# 本节演示：__repr__ 与 __str__ 的调用场景差异
# 为什么重要：f"{obj}" 走 __str__（面向用户），f"{obj!r}" 走 __repr__（面向开发者）；
#            print(obj) 优先 __str__，而列表/字典内部显示元素时用的是 __repr__。
# Java 对照：只有 toString()，Python 拆成两个，调试和展示可以分别定制；只写一个时写 __repr__。
# ============================================================

class Demo:
    def __str__(self):
        return "给用户看的"          # f"{d}" / str(d) / print(d) 走这里

    def __repr__(self):
        return "给开发者看的"        # f"{d!r}" / repr(d) / 容器内显示 走这里

d = Demo()
print(f"  str(d)   = {d}")           # 走 __str__ → 给用户看的
print(f"  repr(d)  = {d!r}")         # !r 强制 __repr__
print(f"  f'{{d}}'   = {f'{d}'}")    # 格式化占位符 {d} 等价 str(d)
print(f"  f'{{d!r}}' = {f'{d!r}'}")  # 占位符加 !r 等价 repr(d)
print("  只写一个时，写 __repr__（__str__ 缺失会 fallback 到它）")
