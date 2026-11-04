# 05 · 面向对象与 Java 对比

> 核心结论先说：**Python 的类比 Java 简单得多**。
> 没有访问修饰符、没有接口（有替代品）、没有方法重载、没有抽象类强制约束。代价是"自由"，收益是"写起来快"。
> 但**继承 + 魔术方法 + `__eq__/__hash__` 契约**这几块必须精确掌握，否则会写出难查的 bug。

---

## 一、最小可用类

**Java**

```java
public class Order {
    private final String orderNo;
    private final double amount;

    public Order(String orderNo, double amount) {
        this.orderNo = orderNo;
        this.amount = amount;
    }

    public String getOrderNo() { return orderNo; }
    public double getAmount() { return amount; }

    public boolean isLarge() { return amount > 10_000; }

    @Override
    public String toString() {
        return "Order{orderNo='" + orderNo + "', amount=" + amount + "}";
    }
}
```

**Python**

```python
# ---- 本段讲：最小可用类，对照上面的 Java Order ----
# 一句话：class + __init__ 就完成了 Java 里「字段声明 + 构造器 + getter」三件事。
# 三点与 Java 的差异必须记住：
#   ① 属性不用提前声明，在 __init__ 里 self.xxx = 值 就等于"声明并初始化"；
#   ② self 必须显式写在方法参数第一位（Java 的 this 是编译器隐式传的）；
#   ③ __str__ 就是 toString()，print(对象) 时自动调用。
class Order:                                    # 类声明；Java 写 public class Order {
    def __init__(self, order_no: str, amount: float):   # __init__ ≈ 构造器；对象已创建好后执行，只负责初始化
        self.order_no = order_no                # 把参数存成实例属性；等价 Java this.orderNo = orderNo（右边不带 self 的是参数）
        self.amount = amount                    # : float 只是类型提示，运行时不检查、不做转换

    def is_large(self) -> bool:                 # 实例方法；self 是自动传入的当前实例，-> bool 仅是返回值提示
        return self.amount > 10_000             # 10_000 的下划线只是可读性分隔，值就是 10000

    def __str__(self) -> str:                   # 魔术方法：等价 Java 的 toString()
        return f"Order(order_no={self.order_no!r}, amount={self.amount})"   # f-string 插值；!r 表示用 repr() 格式化，字符串会自动带引号


o = Order("NO001", 20000)                       # 实例化不需要 new，直接「类名(参数)」触发 __init__
print(o)                        # Order(order_no='NO001', amount=20000)   ← 走 __str__
print(o.order_no)               # 直接访问，没有 getOrderNo()
print(o.is_large())             # True   ← 方法必须带括号调用，不带括号拿到的是方法对象
```

行数差 3 倍。差异点逐条：

| Java | Python | 说明 |
|---|---|---|
| `this` | `self` | **显式写在参数第一位**，不是关键字，名字其实可以随便叫（但必须叫 self） |
| `private` / `public` | 无 | 靠命名约定：`_x` 内部用，`__x` 名称改写 |
| getter/setter | 直接访问属性 | 需要时用 `@property` |
| 构造函数 | `__init__` | 不是构造函数（对象已创建），是"初始化方法" |
| `final` 字段 | 无强制 | 靠约定；`@dataclass(frozen=True)` 可部分实现 |
| 必须有一个类 | 可以只有函数 | 文件就是个模块，不需要类包裹 |

### `self` 到底是什么

```python
# ---- 本段讲：self 到底是什么 ----
# self 不是关键字，也不是语法糖，它就是「方法的第一个普通位置参数」，由解释器在 obj.method() 时自动填。
# 看懂这一点，所有「为什么方法定义里要写 self」「为什么调用时不用传」的疑惑都会消失。
class A:                                # 定义一个最简类；没写 __init__ 也能实例化，用 object 的默认实现
    def method(self):                   # self 只是约定名（理论上能叫 me），但全社区都叫 self，别改
        print(self)                     # 打印实例对象本身，输出形如 <__main__.A object at 0x0000...>

a = A()                                 # 创建实例；Java 对应 A a = new A();
a.method()          # <__main__.A object at 0x...>   ← 绑定方法：a 被自动作为 self 传入
A.method(a)         # 完全等价！  a 就是传进去的 self   ← 通过类名调用时，实例必须自己传
```

`a.method()` 本质是 `A.method(a)`——**实例被自动作为第一个参数传入**。这就是为什么 `self` 必须写在参数列表里：Python 没有"隐式 this"。

### 访问控制的真实做法

```python
# ---- 本段讲：访问控制的真实做法（三条命名约定） ----
# Java 用 private/public 让编译器把关；Python 没有这些关键字，只靠命名约定：
#   name   → 公开，谁都能读写；
#   _name  → "内部使用"，只是礼貌约定，运行时拦不住；
#   __name → 名称改写（name mangling）：在类体内被改名为 _类名__name，目的是防子类误覆盖，不是安全机制。
class Account:
    def __init__(self):
        # 下面三行对应 Java 的三个字段声明；Python 在 __init__ 里赋值就等于创建了实例属性
        self.name = "公开"           # 公开
        self._balance = 0            # 约定：内部用，别外部改（但拦不住）
        self.__secret = "x"          # 名称改写为 _Account__secret（防止子类覆盖，不是安全机制）

# 下面几行验证上面的约定：单下划线可以直接改，双下划线在类外访问会失败，但用改写名仍能访问
a = Account()                    # 实例化
a._balance = 999          # ⚠️ 能改，只是"不礼貌"   ← Java 里 private 字段在编译期就会被拒绝
print(a.__secret)         # ❌ AttributeError   ← 类外写的 __secret 不会做改名，所以找不到这个属性
print(a._Account__secret) # ✅ 'x'，改写后的名字   ← 知道规则就能绕过，所以它不是安全边界
```

> **心态转变**：Python 社区的说法是 "we are all consenting adults"——信任调用方。想在边界做校验，用 `@property`；想强约束，用 pydantic 模型（见 11 篇）。

---

## 二、`@dataclass`：替代 Lombok

**99% 的 Java POJO/DTO，在 Python 里都应该写成 dataclass。**

```python
# ---- 本段讲：@dataclass 替代 Lombok ----
# 类体里只写「字段名: 类型」，装饰器自动生成 __init__ / __repr__ / __eq__。
# Java 对应 @Getter @Setter @ToString @EqualsAndHashCode @AllArgsConstructor（或一个 @Data）。
# 关键陷阱：可变默认值（list/dict/set）必须用 field(default_factory=list)，
# 直接写 = [] 会抛 ValueError，因为那样所有实例会共享同一个列表（同 Java 里 static 集合被共享的坑）。
from dataclasses import dataclass, field          # dataclass 是装饰器；field 用来声明可变默认值的工厂

@dataclass                                        # 装饰器在类创建后扫描注解，动态补上样板方法
class Order:
    order_no: str                                 # 字段声明就是类型注解；不用写 self.order_no = order_no
    amount: float                                 # 注解顺序 = 自动生成的 __init__ 的参数顺序
    items: list[str] = field(default_factory=list)   # 可变默认值必须用 factory
    remark: str = ""                              # 不可变默认值可以直接写；有默认值的字段必须排在无默认值字段之后

o = Order("NO001", 100.0)                         # order_no/amount 必填，后两个走默认值
print(o)        # Order(order_no='NO001', amount=100.0, items=[], remark='')   ← 自动生成的 __repr__
print(o == Order("NO001", 100.0))   # True    ← 自动生成 __eq__
```

`@dataclass` 自动生成：`__init__`、`__repr__`、`__eq__`。相当于：

```java
@Getter @Setter @ToString @EqualsAndHashCode @AllArgsConstructor
public class Order { ... }
```

### 常用参数

```python
# ---- 本段讲：@dataclass 的四个常用开关 ----
# frozen  → 不可变（赋值抛 FrozenInstanceError），同时自动生成 __hash__，可当 dict key；
# slots   → 3.10+，用 __slots__ 去掉实例 __dict__，省内存、属性访问更快，但不能再动态加属性；
# kw_only → 3.10+，字段只能用关键字传参，避免"两个 int 传反了"；
# order   → 自动生成 < <= > >=，可直接用 sorted()（对应 Java 实现 Comparable）。
# 下面这个 Point 同时开启四个开关，注意 kw_only 打开后正确写法是 Point(x=1, y=2)。
@dataclass(
    frozen=True,        # 不可变 + 可哈希（可放 set / 作 dict key）
    slots=True,         # 3.10+：省内存 + 属性访问更快（禁止动态加属性）
    kw_only=True,       # 3.10+：所有字段必须关键字传
    order=True,         # 生成 <, > 等比较方法
)
class Point:                              # 类体只有字段注解，没有任何方法
    x: int                                # 字段 1；顺序决定 order 比较时的优先级
    y: int                                # 字段 2

p = Point(1, 2)                           # 演示用：实际 kw_only=True 时应写 Point(x=1, y=2)
# p.x = 3             # ❌ FrozenInstanceError   ← frozen 冻结了赋值，等价 Java 的 final 且更强
print({Point(1,2), Point(1,2)})   # 只有一个元素   ← frozen 自动生成 __hash__，值相同的两个实例在 set 里算同一个
```

### 对比手写类的差距

```python
# ---- 本段讲：手写类 vs dataclass，看清自动生成的东西是什么 ----
# 左边 15 行手写了 __init__ / __repr__ / __eq__ / __hash__，右边 4 行声明字段就等价。
# 重点看 __eq__ 里的两个细节，Java 的 equals 完全对应：
#   ① 类型不匹配返回 NotImplemented 而不是 False（让 Python 有机会去试对方的 __eq__，交互性更好）；
#   ② __hash__ 必须和 __eq__ 用同一组字段，否则"相等但哈希不同"，放进 set/dict 会出鬼。
# 手写：15 行
class Order:                                  # 手写版本（演示用，实际直接写 dataclass）
    def __init__(self, no, amount):           # 对应 Java 的构造器
        self.no = no                          # 字段 1
        self.amount = amount                  # 字段 2
    def __repr__(self):                       # 调试显示，对应 Java 的 toString()
        return f"Order(no={self.no!r}, amount={self.amount!r})"   # !r 让字符串带引号，便于区分 "1" 和 1
    def __eq__(self, other):                  # 对应 Java 的 equals
        if not isinstance(other, Order):      # 类型不同
            return NotImplemented             # 返回 NotImplemented（不是 False），交给解释器处理反射比较
        return (self.no, self.amount) == (other.no, other.amount)   # 元组逐项比较，等价 Java 的 Objects.equals
    def __hash__(self):                       # 对应 Java 的 hashCode
        return hash((self.no, self.amount))   # 用同样两个字段算哈希，保证与 __eq__ 契约一致

# dataclass：4 行
@dataclass(frozen=True)                       # frozen 顺带生成 __hash__，无需手写
class Order:                                  # 同名类覆盖上面的手写版（演示对比）
    no: str                                   # 字段声明
    amount: float                             # 字段声明
```

**什么时候不用 dataclass**：

- 需要复杂校验逻辑 → 用 pydantic `BaseModel`（FastAPI 场景）
- 需要大量自定义方法/状态机 → 普通类
- 字段结构动态变化 → 直接用 `dict`

---

## 三、`@property`：比 getter/setter 优雅

```python
# ---- 本段讲：@property 把"方法"伪装成"字段" ----
# 三段式：@property 定义读（getter）、@xxx.setter 定义写（setter）、@xxx.deleter 定义 del 行为。
# 与 Java 的区别：调用方写 a.balance 而不是 a.getBalance()，所以将来把字段升级成属性不需要改调用方。
# 这也是 Python 唯一推荐的"在边界做校验"的方式。
class Account:
    def __init__(self, balance=0):            # 默认参数：不传就是 0（Java 要用重载构造器实现）
        self._balance = balance               # 真实数据放在 _balance（单下划线=内部用），避免和属性名冲突

    @property
    def balance(self):                        # getter：方法名就是属性名，参数只有 self
        """只读属性，访问方式像字段：account.balance"""
        return self._balance                  # 读操作直接返回内部字段

    @balance.setter                           # 用「属性名.setter」注册写方法
    def balance(self, value):                 # 参数是 self + 新值
        if value < 0:                         # 校验逻辑写在这里，Java 写在 setBalance 里
            raise ValueError("余额不能为负")   # 抛异常而不是返回 false，调用方必须处理
        self._balance = value                 # 校验通过才真正赋值

    @balance.deleter                          # 注册 del 行为（很少用，Java 没有对应物）
    def balance(self):                        # del a.balance 时会调用它
        raise AttributeError("余额不可删除")   # 直接禁止删除


a = Account(100)                              # 构造时给初始余额
print(a.balance)        # 100         ← 不用写括号   ← 看起来像字段访问，实际执行 getter
a.balance = 200         # ✅ 走 setter   ← 赋值看起来像字段赋值，实际执行 setter 里的校验
# a.balance = -1        # ❌ ValueError   ← 被 setter 拦住
```

```java
// Java 需要显式写 getter/setter 并在 setter 里校验
public void setBalance(double value) {
    if (value < 0) throw new IllegalArgumentException("余额不能为负");
    this.balance = value;
}
```

### 计算属性（Java 里只能写成方法）

```python
# ---- 本段讲：用 @property 做"计算属性" ----
# total 没有对应的存储字段，每次访问都现算；Java 里只能写成 getTotal() 方法。
# 好处：调用方始终写 o.total，未来若改成"存字段"也不用改调用方代码。
class Order:
    def __init__(self, price, qty):           # 保存两个输入字段
        self.price = price                    # 单价
        self.qty = qty                        # 数量

    @property
    def total(self):                          # 只写 @property 不写 setter → 只读属性
        return self.price * self.qty          # 每次访问都重新计算，不缓存

o = Order(10, 3)                              # 单价 10、数量 3
print(o.total)      # 30   ← 像字段一样用，实际每次计算   ← 注意没有括号
```

### 缓存属性（重计算场景）

```python
# ---- 本段讲：@cached_property（算一次就缓存） ----
# 与 @property 的唯一区别：第一次访问把结果写进实例 __dict__，之后直接读缓存、不再执行函数。
# 适用：结果只依赖不变输入、计算昂贵（如聚合统计、远程调用）。
# 注意：要求实例可写（不能配 __slots__），且输入变了要手动 del r.summary 才能重算。
from functools import cached_property   # 3.8+ 标准库；不需要自己写缓存字典

class Report:
    def __init__(self, rows):                 # 构造时保存数据源
        self.rows = rows                      # 一个列表，元素是 dict

    @cached_property                          # 首次访问执行函数，并把返回值存到 self.__dict__["summary"]
    def summary(self):                        # 依然用"属性"方式访问，不写括号
        print("计算一次")                     # 用打印证明只算了一次
        return sum(r["amount"] for r in self.rows)   # 生成器表达式求和，等价 Java 的 stream().mapToInt().sum()

r = Report([{"amount": 1}, {"amount": 2}])    # 两行数据，合计 3
print(r.summary)    # 3   打印"计算一次"   ← 第一次：执行函数
print(r.summary)    # 3   不再打印   ← 第二次：直接取缓存值
```

---

## 四、类方法、静态方法、实例方法

```python
# ---- 本段讲：三种方法（实例 / 类 / 静态） ----
# 区别只在「第一个参数自动传什么」：
#   实例方法 → 自动传 self（实例）；@classmethod → 自动传 cls（类）；@staticmethod → 什么都不传。
# Java 只有两种（实例方法 / static 方法），classmethod 是 Python 独有，用来写"工厂构造"。
class Order:
    rate = 0.9          # 类属性（相当于 Java static 字段）   ← 定义在 class 体内、方法外

    def __init__(self, no):                   # 初始化方法
        self.no = no                          # 实例属性；注意它不是类属性

    def instance_method(self):          # 实例方法：能访问 self   ← 普通方法，必须通过实例调用
        return self.no                    # 读取实例自己的字段

    @classmethod
    def from_dict(cls, d):              # 类方法：第一个参数是类   ← cls 是"调用者"，可能是子类
        return cls(d["no"])             # 常用于工厂方法 / 替代重载构造   ← cls(...) 会创建 cls 的实例

    @staticmethod
    def valid_no(no):                   # 静态方法：什么都不自动传   ← 就是个挂在类命名空间下的普通函数
        return len(no) == 5               # 不依赖 self/cls，纯工具逻辑


o = Order.from_dict({"no": "NO001"})    # 工厂方法   ← 类似 Java 的静态工厂 Order.of(...)
print(Order.valid_no("NO001"))          # True   ← 长度 5
print(o.instance_method())              # NO001
print(Order.rate)                       # 0.9   ← 类属性直接通过类名访问，Java 的 static 字段同理
```

| Java | Python |
|---|---|
| `static` 方法 | `@staticmethod` |
| 静态工厂 `Order.of(...)` | `@classmethod`（`cls` 让子类也能用） |
| 实例方法 | 默认，带 `self` |
| 构造重载 | 用 `@classmethod` 工厂 + 默认参数 |

### 为什么用 `@classmethod` 而不是 `@staticmethod` 做工厂

```python
# ---- 本段讲：为什么工厂方法要用 @classmethod 而不是 @staticmethod ----
# 核心一句：@classmethod 的 cls 是"实际调用者"，子类调用时自动变成子类；
# @staticmethod 把类名写死了，子类调用也只能造出父类对象。
# 这在 Java 里对应"静态工厂里用反射/泛型拿到实际类型"，Python 直接用 cls 就免费得到。
class Base:
    @classmethod
    def create(cls):                      # cls 自动接收调用这个方法的类
        return cls()            # cls 是调用者，子类调用时创建子类实例   ← 关键：不写死 Base()

class Child(Base):
    pass                                  # 空类体，继承 Base 的 create

print(type(Base.create()))      # <class 'Base'>   ← 通过父类调用，cls = Base
print(type(Child.create()))     # <class 'Child'>   ← 自动适配   ← 通过子类调用，cls = Child

# 用 staticmethod 就锁死了
class Base2:
    @staticmethod
    def create():                         # 没有 cls，只剩一个普通函数
        return Base2()                    # 写死了 Base2，子类调用也只会得到父类实例

print(type(Child.create()))     # <class 'Base2'>   ❌   ← 这里演示的是"继承链断了"的后果
```

---

## 五、继承

```python
# ---- 本段讲：继承的写法与两个类型判断函数 ----
# 语法：class 子类(父类)，对应 Java 的 class Dog extends Animal。
# 最大的差异在 __init__：Java 会自动调 super()，Python 不会——必须手写 super().__init__(...)，
# 忘了不会报错，只会在访问父类字段时报 AttributeError（后面第五节会演示）。
# 另外：Python 的方法重写不需要 @Override（写了也不校验），只要同名就覆盖。
class Animal:
    def __init__(self, name):                 # 父类初始化方法
        self.name = name                      # 子类若不调 super().__init__，这个字段就不存在

    def speak(self) -> str:                   # 子类可覆盖的"虚方法"
        return "..."                          # 父类默认实现

class Dog(Animal):                            # 单继承：括号里写父类
    def __init__(self, name, breed):          # 子类构造器需要自己声明父类参数 + 自己的参数
        super().__init__(name)          # 必须显式调用父类 __init__   ← 忘写就不报错但丢字段（高频 bug）
        self.breed = breed                    # 自己的字段

    def speak(self) -> str:                   # 重写父类方法：同名即可，无需 @Override
        return "汪汪"                         # 多态：父类引用调用时执行子类实现

d = Dog("旺财", "柴犬")                        # 创建子类实例
print(d.speak(), d.name, d.breed)       # 汪汪 旺财 柴犬   ← 三个值分别来自子类方法、父类字段、子类字段
print(isinstance(d, Animal))            # True   ← instanceof   ← 等价 Java 的 d instanceof Animal
print(issubclass(Dog, Animal))          # True   ← 判断"类与类"的关系，Java 没有直接对应（只能反射）
```

> **注意**：子类不会自动调用父类 `__init__`（Java 里会自动调 super()）。忘写就丢字段，而且不报错。这是高频 bug。

### 多重继承与 MRO

Python 支持多继承（Java 不支持），用 **C3 线性化** 决定方法查找顺序：

```python
# ---- 本段讲：多继承与 MRO（方法解析顺序） ----
# Python 允许 class D(B, C) 继承多个父类（Java 不允许，只能多实现接口）。
# 方法查找按 C3 线性化后的 MRO 顺序进行，可以理解为"从左到右、子类优先"的一张查找表。
# 重要认知：super() 不是"调父类"，而是"按 MRO 调下一个"，所以在菱形继承里也不会重复执行。
class A:
    def who(self): return "A"                 # 祖类实现
class B(A):
    def who(self): return "B"                 # 左父类，覆盖 A
class C(A):
    def who(self): return "C"                 # 右父类，覆盖 A
class D(B, C):
    pass                                      # 自己不定义 who，按 MRO 去找

print(D().who())        # B   ← B 排在 C 前面，所以先找到 B
print([c.__name__ for c in D.__mro__])        # 列表推导式列出查找顺序
# ['D', 'B', 'C', 'A', 'object']   ← 自己 → 左父 → 右父 → 共同祖先 → object，每个类只出现一次

# super() 是"按 MRO 找下一个"，不是"找父类"
class D2(B, C):
    def who(self):
        return f"D2->{super().who()}"         # super() 在 D2 的 MRO 里取下一个是 B，不是 A
print(D2().who())       # D2->B
```

**实践建议**：业务代码里几乎不该用多重继承。它主要用于**混入（Mixin）**——给类加能力：

```python
# ---- 本段讲：Mixin（混入）——唯一推荐使用多继承的场景 ----
# Mixin 是一个"只提供方法、自己不独立使用"的小类，用来给别的类"贴"上一个能力。
# Java 里对应 interface 的 default 方法，或用组合 + 工具类实现；Python 直接多继承最省事。
# 约定：Mixin 命名以 Mixin 结尾，且不定义 __init__、不保存状态。
class JsonMixin:
    def to_json(self):                        # 给宿主类新增一个方法
        import json                           # 局部 import：用到才导入，避免模块顶部依赖
        from dataclasses import asdict        # asdict 把 dataclass 实例转成普通 dict
        return json.dumps(asdict(self), ensure_ascii=False)   # ensure_ascii=False 保留中文原样输出

class Order(JsonMixin):        # 单个 Mixin 是可接受的   ← 继承顺序：Mixin 写在左边
    ...                                       # 类体省略（本段只演示能力混入）
```

### `super()` 的坑

```python
# ---- 本段讲：super() 的坑（反例 vs 正例） ----
# ❌ 写法把父类名写死，菱形继承时 B、C 各自又去调 A.__init__，导致 A 被初始化两次。
# ✅ 写法交给 MRO 链式推进，A 只会被执行一次。
# 对照 Java：Java 里 super() 由编译器保证只调直接父类，不存在这种重复调用问题。
# ❌ 写死父类名：菱形继承时会重复调用
class D(B, C):
    def __init__(self):
        B.__init__(self)                      # 直接点名 B：跳过 MRO，只走 B 这一支
        C.__init__(self)                      # 再点名 C；若 B、C 都调了 A.__init__，A 就初始化两次

# ✅ 用 super()，让 MRO 决定
class D(B, C):
    def __init__(self):
        super().__init__()                    # 只调 MRO 里的下一个，沿链传递，每个类只初始化一次
```

---

## 六、接口的三种替代方案

Java 的 `interface` 在 Python 里有三个层次的替代，**按需要选，别一上来就抽接口**。

### 方案一：什么都不写（鸭子类型）

```python
# ---- 本段讲：方案一 · 鸭子类型（不写任何接口） ----
# "如果它走起来像鸭子、叫起来像鸭子，那它就是鸭子"——只关心对象有没有 send 方法，不关心它的类型。
# Java 里必须抽 NotificationSender 接口 + 两个 implements + 依赖注入，Python 里零成本。
# 代价：没有静态检查，方法名写错要到运行时才炸（可配合 Protocol 补上）。
def notify(sender):
    sender.send("hello")        # 只要有 send 方法就行，不检查类型   ← 运行到这里才发现缺方法，抛 AttributeError

class EmailSender:
    def send(self, msg): ...                  # ... 是 Ellipsis，占位表示"实现省略"（不是 pass，但效果一样）
                                              # 这里假想它发邮件
class SmsSender:
    def send(self, msg): ...                  # 结构相同即可，无需继承任何基类

notify(EmailSender())                         # 传入任意有 send 的实例都能跑
notify(SmsSender())                           # 换实现不用改 notify 一行代码
```

90% 的场景这是最好的选择。Java 里你必须抽 `NotificationSender` 接口 + 两个实现 + 注入，Python 里零成本。

### 方案二：`Protocol`（结构化类型，等价 interface 但不需要显式实现）

```python
# ---- 本段讲：方案二 · Protocol（结构化类型） ----
# Protocol 定义"需要哪些方法/属性"，但实现类不需要继承它——这叫"隐式实现"（structural typing）。
# 等价 Java 的 interface，区别是：Java 要写 implements，Python 只要方法签名长得一样就算满足。
# 作用主要给 mypy/pyright 这类静态检查器看；运行时不做任何校验（除非加 @runtime_checkable）。
from typing import Protocol                   # 标准库 typing 里的协议基类

class NotificationSender(Protocol):           # 定义协议：任何有 send(self, msg: str) -> None 的类都满足
    def send(self, msg: str) -> None: ...     # 只有签名，没有实现；... 表示方法体省略

def notify(sender: NotificationSender, msg: str) -> None:   # mypy 会检查   ← 注解写协议名，调用方传的实参必须结构匹配
    sender.send(msg)                          # 真正调用，运行时不关心类型

class EmailSender:                  # 注意：没有 implements！   ← 关键差异：不继承 NotificationSender
    def send(self, msg: str) -> None:         # 签名与协议一致即可
        print(f"邮件：{msg}")                 # 具体实现

notify(EmailSender(), "hi")         # ✅ 静态检查通过   ← 因为 EmailSender 结构上满足协议
```

这是"**隐式实现**"——只要结构匹配就满足协议。好处：不改已有类就能适配接口。

### 方案三：`ABC`（抽象基类，等价 abstract class）

```python
# ---- 本段讲：方案三 · ABC 抽象基类 ----
# ABC = Abstract Base Class，最接近 Java 的 abstract class + interface 混合体：
#   ① @abstractmethod 声明"子类必须实现"，不实现就实例化报 TypeError（运行时强制，真正的硬约束）；
#   ② 普通方法可以有默认实现（等价 Java interface 的 default 方法）；
#   ③ 必须显式继承——这是它和 Protocol 的核心区别，所以无法适配已有的第三方类。
from abc import ABC, abstractmethod           # ABC 是抽象基类；abstractmethod 标记必须实现的方法

class Repository(ABC):                        # 继承 ABC 才能用抽象方法机制（Java 写 abstract class Repository）
    @abstractmethod
    def find_by_id(self, id: int): ...        # 只有签名；子类不实现就无法实例化

    @abstractmethod
    def save(self, entity): ...               # 同上；一个类里可以有多个抽象方法

    def health(self):               # 可以有默认实现（Java interface default method）   ← 子类可继承也可覆盖
        return "ok"                           # 共享的通用逻辑

class OrderRepo(Repository):        # 必须显式继承   ← 关键：不写 (Repository) 就不会有任何检查
    def find_by_id(self, id): return {"id": id}   # 补上第一个抽象方法
    def save(self, entity): pass                  # 补上第二个抽象方法（pass = 空实现，占位）

# Repository()      # ❌ TypeError: Can't instantiate abstract class   ← 抽象类不能直接实例化，运行时强制
OrderRepo()         # ✅   ← 所有抽象方法都实现了，才能创建
```

### 怎么选

| 场景 | 选择 |
|---|---|
| 只需"用起来能跑" | 鸭子类型，什么都不写 |
| 要静态检查约束 + 适配已有类 | `Protocol` |
| 有共享的默认实现 + 强制子类实现 | `ABC` |
| 传统 Java 思维的项目 | `ABC`（最接近） |

---

## 七、魔术方法：Python 的运算符重载

Java 只能重写 `equals`/`hashCode`/`toString`。Python 可以重载**几乎所有语法**。

### 最常用的 8 个

```python
# ---- 本段讲：魔术方法 / 运算符重载（最常用的 8 个） ----
# 魔术方法（dunder = double underscore）是 Python 的"协议钩子"：你实现它，语法就自动支持。
#   __repr__/__str__ → print、调试显示（对应 Java toString）
#   __eq__/__hash__  → == 与 HashMap/Set（对应 equals/hashCode）
#   __add__/__lt__   → + 与 <（Java 只能靠 add()、compareTo() 方法名，不能用运算符）
#   __len__          → len(obj)（Java 只有集合的 size()）
# 记住两条契约：__eq__ 与 __hash__ 必须用同一组字段；只写一个显示方法时写 __repr__。
class Money:
    def __init__(self, amount: float, currency: str = "CNY"):   # 金额 + 币种，币种有默认值
        self.amount = amount                  # 数值
        self.currency = currency              # 币种字符串

    def __repr__(self):                     # 调试显示（给开发者看）
        return f"Money({self.amount!r}, {self.currency!r})"   # 形式接近构造函数，便于"重建对象"

    def __str__(self):                      # 用户显示（给用户看）
        return f"{self.amount:.2f} {self.currency}"   # :.2f 保留两位小数，类似 Java 的 %.2f

    def __eq__(self, other):                # ==   ← 对应 Java equals
        if not isinstance(other, Money):      # 类型不同
            return NotImplemented             # 交给解释器处理，不要直接返回 False
        return (self.amount, self.currency) == (other.amount, other.currency)   # 两个字段都相等才算相等

    def __hash__(self):                     # 可放 set / dict key   ← 对应 Java hashCode
        return hash((self.amount, self.currency))   # 用同一组字段，保证与 __eq__ 一致

    def __add__(self, other):               # +   ← Java 里只能写 m1.add(m2)
        if self.currency != other.currency:   # 币种不同不能相加
            raise ValueError("币种不一致")     # 抛异常，避免算出错误金额
        return Money(self.amount + other.amount, self.currency)   # 返回新对象（不可变风格，同 BigDecimal）

    def __lt__(self, other):                # <  （sorted 需要）   ← 对应 Java 的 compareTo
        return self.amount < other.amount     # 只按金额比较，忽略币种（示例简化）

    def __len__(self):                      # len()   ← 让 len(money) 有意义
        return int(self.amount)               # 截断成整数返回（示例用法，实际业务别这么写）


print(repr(Money(10)))                        # 主动调 repr()，输出 Money(10, 'CNY')
print(str(Money(10)))                         # 主动调 str()，输出 10.00 CNY
print(Money(10) + Money(5))                 # 15.00 CNY   ← 走 __add__ 再走 __str__
print(sorted([Money(3), Money(1)]))         # [Money(1.0,'CNY'), Money(3.0,'CNY')]   ← 走 __lt__ 排序，显示走 __repr__
```

> **`__repr__` vs `__str__` 的区别**：`repr` 面向调试（应该"能重建对象"），`str` 面向展示。`print(obj)` 优先用 `__str__`，REPL 里显示 `__repr__`。**只写一个时，写 `__repr__`**（因为 `__str__` 缺失会 fallback 到 `repr`）。

### 完整清单（知道有就行）

| 类别 | 方法 |
|---|---|
| 构造销毁 | `__new__` `__init__` `__del__` |
| 表示 | `__repr__` `__str__` `__format__` `__bytes__` |
| 比较 | `__eq__` `__ne__` `__lt__` `__le__` `__gt__` `__ge__` `__hash__` `__bool__` |
| 算术 | `__add__` `__sub__` `__mul__` `__truediv__` `__floordiv__` `__mod__` `__pow__` 及 `__i*__` 原地版、`__r*__` 反射版 |
| 容器 | `__len__` `__getitem__` `__setitem__` `__delitem__` `__contains__` `__iter__` `__next__` `__reversed__` |
| 上下文 | `__enter__` `__exit__` （见 06 篇） |
| 属性 | `__getattr__` `__setattr__` `__getattribute__` `__slots__` |
| 可调用 | `__call__` |

### 实用示例：让对象可迭代

```python
# ---- 本段讲：让自定义对象可迭代 / 可索引 / 可 len ----
# 四个魔术方法分别接管四个内置函数：
#   __len__       → len(p)
#   __getitem__   → p[i]，并且"只实现 __getitem__"就能支持 for 循环和切片
#   __iter__      → for x in p 时拿迭代器（比 __getitem__ 的旧机制更明确）
#   __contains__  → x in p
# Java 里要实现 Iterable + iterator() 才能 for-each，这里一个方法就行，而且切片是白送的。
class Page:
    def __init__(self, items, size=10):       # items 是数据，size 只做演示用
        self.items = items                    # 保存原始列表
        self.size = size                      # 每页大小

    def __len__(self):
        return len(self.items)                # 委托给内置 list 求长度

    def __getitem__(self, i):
        return self.items[i]                  # 下标访问；传切片对象时 i 就是 slice，所以 p[1:] 自动可用

    def __iter__(self):
        return iter(self.items)               # 返回 list 的迭代器（也可以直接 return self 配 __next__）

    def __contains__(self, v):
        return v in self.items                # in 运算符背后调这里


p = Page([1, 2, 3])                           # 三元素分页对象
print(len(p))            # 3   ← __len__
print(p[0])              # 1   ← __getitem__
print(2 in p)            # True   ← __contains__
print(list(p))           # [1, 2, 3]   ← __iter__
for x in p:                                   # for 循环优先用 __iter__
    print(x)                                  # 依次打印 1 2 3
```

### 实用示例：`__call__` 让实例变成可调用（做"带状态的函数"）

```python
# ---- 本段讲：__call__ 让实例像函数一样被调用 ----
# 实现 __call__ 后，obj(...) 不再报错，而是执行 __call__ 方法体。
# 用途：做"带状态的函数/装饰器对象"——状态存在 self 上，比闭包更好调试，比 Java 的 Function 实现类更轻。
# Java 里对应实现 Function<T,R> 接口并重写 apply，但调用时仍要写 apply(...)，没有运算符级语法。
class RateLimiter:
    def __init__(self, max_calls):            # 构造时固定上限
        self.max_calls = max_calls            # 允许的最大调用次数（状态存在实例上）
        self.calls = 0                        # 已调用次数

    def __call__(self, fn):                   # 使用 limiter(fn) 时触发；返回包装后的函数
        def wrapper(*args, **kwargs):         # 内层闭包：*args 收位置参数元组，**kwargs 收关键字参数字典
            self.calls += 1                   # 每次调用计数 +1（闭包捕获了外层的 self）
            if self.calls > self.max_calls:   # 超过上限
                raise RuntimeError("超限")     # 直接抛错，阻止继续执行
            return fn(*args, **kwargs)        # 把参数原样转发给被包装函数并返回结果
        return wrapper                        # 返回闭包，调用方拿到的就是"带限流的新函数"

limiter = RateLimiter(2)                      # 最多允许 2 次调用
```

---

## 八、`__slots__`：省内存的优化开关（了解即可）

```python
# ---- 本段讲：__slots__ 省内存的原理 ----
# 普通实例的属性都存在自己的 __dict__ 里（一个哈希表），所以能随时 obj.新属性 = 值。
# 声明 __slots__ = ("x",) 后，实例改为用固定槽位存属性，没有 __dict__，更省内存、访问稍快，但不能动态加属性。
# Java 的类字段本来就是编译期固定的，不存在 __dict__ 这层，所以这个优化是 Python 特有的。
class WithDict:
    def __init__(self, x):
        self.x = x                            # 存进实例 __dict__，之后还能加 y、z 等任意属性

class WithSlots:
    __slots__ = ("x",)                        # 白名单：只允许 x 这一个属性
    def __init__(self, x):
        self.x = x                            # 存进固定槽位；写 self.y 会直接 AttributeError

import sys                                    # 标准库，用来查看对象内存占用
a, b = WithDict(1), WithSlots(1)              # 元组解包：一行创建两个不同实现的实例
print(sys.getsizeof(a.__dict__))     # 有 __dict__（约 104 字节起）   ← 这是额外的内存开销
# print(b.__dict__)                  # ❌ AttributeError，没有 __dict__   ← 省下的就是这部分
```

收益：每实例省几十字节，属性访问略快。代价：**不能再动态加属性**。

> 建议：除非在写需要创建百万实例的数据结构，否则**不要用**。Python 3.10+ 用 `@dataclass(slots=True)` 更安全。

---

## 九、类属性的坑（可变类属性是共享的）

```python
# ---- 本段讲：可变类属性被所有实例共享（高频 bug） ----
# 判断依据：写在 class 体内、方法外的是「类属性」，所有实例共用一份；
# 写在 __init__ 里 self.xxx 的才是「实例属性」，每个对象各有一份。
# 下面对比两个类：Bad 把 list 放在类体里，a 一改 b 也变；Good 在 __init__ 里建 list，互不影响。
class Bad:
    items = []          # ❌ 类属性，所有实例共享   ← 整个类只有这一个 list 对象

a, b = Bad(), Bad()                           # 两个不同实例
a.items.append(1)                             # 通过 a 找到的其实是类属性，直接往共享列表里加元素
print(b.items)          # [1]   ← 不是 [ ]   ← b 读到的也是同一份

class Good:
    def __init__(self):
        self.items = []  # ✅ 实例属性   ← 每次构造都新建一个 list，各自独立

# 这是 Java 的反直觉点：
# Java 里 static List items = new ArrayList<>() 也会共享，
# 但 Java 程序员清楚地知道 static 意味着共享，Python 里这个 "static" 是隐式的
```

**判断依据**：写在 `class` 体内、方法外 → 类属性（共享）。写在 `__init__` 里 `self.xxx` → 实例属性。

---

## 十、Pydantic：FastAPI 场景下的"强约束类"（项目实战用）

`ai-cs-py-chat` 用的是 Pydantic 而不是 dataclass，因为要做**入参校验**：

```python
# ---- 本段讲：Pydantic BaseModel（跨系统边界的强约束类） ----
# 和 dataclass 的区别：BaseModel 在「构造时」就做类型校验 + 类型转换（"123" → 123），不合法直接抛 ValidationError。
# Java 里对应 @Valid + Bean Validation 注解（@NotNull/@Min/@Max），在 Controller 入参处生效。
# 注意：下面这段是项目真实写法的简化，需要安装 pydantic 才能运行。
# app/schemas/chat.py 的真实写法（简化）
from pydantic import BaseModel, Field         # BaseModel 是校验基类；Field 声明约束与默认值

class Message(BaseModel):
    role: str                                 # 必填；缺了或类型不对会抛 ValidationError
    content: str                              # 必填

class ChatRequest(BaseModel):
    messages: list[Message]                   # 嵌套模型：传 [{"role":..., "content":...}] 会自动转成 Message
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)   # ge=大于等于，le=小于等于；默认 0.7
    max_tokens: int = Field(default=2048, gt=0)               # gt=严格大于；默认 2048

# 校验自动发生
req = ChatRequest(messages=[{"role": "user", "content": "hi"}])   # 传 dict 也能被自动转成 Message 对象
print(req.messages[0].role)      # user   ← 说明完成了 dict → 模型 的转换

ChatRequest(messages=[], temperature=9.9)    # ❌ ValidationError   ← 9.9 超出 le=2.0，构造即失败
```

| | `@dataclass` | Pydantic `BaseModel` |
|---|---|---|
| 用途 | 内部数据结构 | 外部输入输出（HTTP/配置/消息） |
| 校验 | 无 | 自动 + 类型转换 |
| 性能 | 快 | 略慢（有校验开销） |
| 依赖 | 标准库 | 第三方 |
| 典型场景 | 领域对象、内部传递 | API 请求/响应、配置文件 |

> 选型原则：**跨越系统边界用 Pydantic，内部流转用 dataclass**。

---

## 十一、项目里 `ai-cs-py-chat` 的类设计点评

```python
# ---- 本段讲：用 pydantic-settings 读 .env 配置 ----
# Settings 继承 BaseSettings 后，会自动按"字段名 = 环境变量名(大小写不敏感)"去读环境变量或 .env 文件。
# Java 里对应 @ConfigurationProperties + application.yml；区别是这里没有 Spring 容器，直接实例化就完成绑定。
# model_config 是 Pydantic v2 的类级配置，SettingsConfigDict(env_file=...) 指定配置文件路径。
# app/config.py —— 用 pydantic-settings 读 .env，替代 @ConfigurationProperties
class Settings(BaseSettings):                 # BaseSettings 来自 pydantic_settings 包（本段未展示 import）
    llm_base_url: str = "https://api.deepseek.com"   # 字段名 llm_base_url 对应环境变量 LLM_BASE_URL；有默认值
    llm_api_key: str = ""                     # 敏感配置；默认为空，避免没配时报错
    model_config = SettingsConfigDict(env_file=".env")   # 告诉它从当前目录 .env 读取键值对
```

```java
// Java 等价
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(String baseUrl, String apiKey) {}
```

```python
# ---- 本段讲：模块级单例（用 lru_cache 替代 Spring Bean） ----
# @lru_cache(maxsize=1) 会缓存函数返回值：第一次调用执行 ChatService()，之后同参数调用直接返回缓存对象。
# 因为没有参数，所以全局只有一个实例 —— 这就是"单例 Bean"的最小实现，不需要容器。
# Java 里对应 @Service + 容器管理生命周期；Python 里一个装饰器就够了。
# app/services/chat_service.py —— 模块级单例 + lru_cache，替代 Bean
from functools import lru_cache              # 标准库缓存装饰器；maxsize=1 表示只保留 1 份结果

@lru_cache(maxsize=1)
def get_chat_service() -> ChatService:       # 无参数 → 缓存键恒定 → 全局唯一实例
    return ChatService()                     # 只在首次调用时真正构造，之后都命中缓存
```

```java
// Java：容器管理单例
@Service
public class ChatService { }
```

> **这就是"没有 IoC 容器"的真实样子**：一个加了缓存的工厂函数。99% 的 Python 服务不需要真正的 DI 框架。

---

## 十二、自测

```python
# ---- 本段讲：本章自测题（都是注释，没有可执行语句） ----
# 这段代码块是"题目清单"，不会真的运行；每题对应本章一个知识点，建议先自己写再看 code/04_oop/ 的答案。
# ① 用 dataclass 定义一个 Money 类，支持相加、比较、打印
#    → 提示：@dataclass(frozen=True, order=True) + __add__，注意币种校验

# ② 下面输出什么？
#    → 考点是"可变类属性共享"：items 属于类 A，B 只是继承、没有自己的 items，所以打印 [1]
class A:
    items = []                                # 类属性，A 和所有子类共享同一份
class B(A):
    pass                                      # 没定义自己的 items，查找时会找到 A.items
A.items.append(1)                             # 往共享列表追加
print(B.items)          # ?   ← 结果 [1]

# ③ 用 @property 实现"只读 + 校验"的 age 属性
#    → 提示：@property 写 getter，@age.setter 里 if value < 0: raise ValueError(...)

# ④ 用 Protocol 定义一个 Repo 协议，写两个实现类，并用 mypy 检查
#    → 提示：实现类不需要继承协议，只要方法签名一致；运行 mypy xxx.py 验证

# ⑤ 解释为什么子类忘写 super().__init__() 不会报错但会出 bug
#    → 提示：Python 不做"父类初始化"的强制检查，只有访问缺失的父类字段时才抛 AttributeError
```

答案：[`code/04_oop/`](code/04_oop/)

---

**上一篇**：[04-函数与作用域](04-函数与作用域.md)　**下一篇**：[06-异常、文件与上下文管理器](06-异常、文件与上下文管理器.md)
