"""01 · 类的基础：self、类属性、继承

运行: python 04_oop/01_basic.py
"""

# ============================================================
# 本节演示：Python 最基础的类 —— 字段、构造、方法、__str__/__repr__
# 为什么重要：Java 里字段要声明、构造器要写、toString 要重写；Python 全部靠 __init__ + 魔术方法。
# Java 对照：class Order 相当于 public class Order，__init__ 相当于构造器，__str__ 相当于 toString()。
# ============================================================

print("=" * 60)
print("1. 最小类")
print("=" * 60)

class Order:
    counter = 0                     # 类属性（所有实例共享，类似 static）

    def __init__(self, order_no, amount):
        # __init__ 不是"构造函数"而是"初始化方法"：对象已由 __new__ 创建好，这里只负责填字段
        self.order_no = order_no    # 实例属性
        self.amount = amount        # 实例属性；Python 在赋值这一刻才"创建"字段，无需提前声明
        Order.counter += 1          # 通过类名访问类属性并自增（不能用 self.counter += 1，那会创建同名实例属性）

    def is_large(self):
        return self.amount > 10_000  # 实例方法必须带 self；10_000 的下划线只是可读性分隔符

    def __str__(self):              # = toString()
        return f"Order({self.order_no}, {self.amount})"   # print(obj) 时优先调用它，面向"用户/日志"

    def __repr__(self):             # 调试显示
        return f"Order(order_no={self.order_no!r}, amount={self.amount!r})"   # !r 用 repr 格式化，字符串会带引号


o = Order("NO001", 20000)          # 实例化不需要 new，直接"类名(参数)"就会触发 __init__
print(f"  print(o)          -> {o}")           # 走 __str__
print(f"  repr(o)           -> {o!r}")        # !r 强制走 __repr__
print(f"  o.order_no        -> {o.order_no}      <- 直接访问，没有 getter")   # 属性公开，无访问控制
print(f"  o.is_large()      -> {o.is_large()}")   # 方法调用必须带括号
print(f"  Order.counter     -> {Order.counter}")  # 类属性通过类名读取，等价 Java 的 static 字段


print("\n" + "=" * 60)
print("2. self 到底是什么")
print("=" * 60)

# ============================================================
# 本节演示：self 的真实身份 —— 它就是方法的第一个普通位置参数
# 为什么重要：Java 的 this 是隐式关键字，Python 把它显式化，这是读 Python 代码最常见的困惑点。
# Java 对照：a.method() 在 Java 里等价 this 自动传入；Python 里等价 A.method(a)，需要手动给。
# ============================================================

class A:
    def method(self):
        return id(self)             # id() 返回对象身份（内存地址的整数表示），等价 Java 的 System.identityHashCode 思路


a = A()                             # 创建实例
print(f"  a.method()   -> {a.method()}")     # 绑定方法：解释器自动把 a 作为 self 传入
print(f"  A.method(a)  -> {A.method(a)}     <- 完全等价")   # 通过类调用：实例必须自己传
print(f"  id(a)        -> {id(a)}")          # 与上面两个值相同，证明 self 就是 a 本身
print("  a.method() 本质是 A.method(a)：实例被自动作为第一个参数")


print("=" * 60)
print("3. 访问控制：靠约定，不靠关键字")
print("=" * 60)

# ============================================================
# 本节演示：访问控制的三条命名约定（公开 / _ 内部 / __ 名称改写）
# 为什么重要：Python 没有 private，字段默认全公开；理解 __ 的改名规则才不会在调试时认不出属性名。
# Java 对照：name≈public，_balance≈protected 的"自觉版"，__secret≈private 的"改名版"（不是安全机制）。
# ============================================================

class Account:
    def __init__(self):
        self.name = "公开"           # 公开
        self._balance = 0            # 约定：内部用
        self.__secret = "x"          # 名称改写成 _Account__secret

    def show(self):
        return f"{self.name} / {self._balance} / {self.__secret}"   # 类内部访问 __secret 会被编译成 self._Account__secret，所以能读到

acc = Account()                             # 创建实例
print(f"  acc.name          = {acc.name}")   # 公开属性，随意读写
print(f"  acc._balance      = {acc._balance}      <- 能访问，只是不礼貌")   # 单下划线只是约定
acc._balance = 999                           # 直接改"内部"字段，Python 不拦
print(f"  改了之后          = {acc._balance}")   # 999，证明没有任何运行时保护

try:
    acc.__secret                             # 类外写 __secret 不会做名称改写，所以找不到该属性
except AttributeError:
    print(f"  acc.__secret      -> AttributeError（被改写成 _Account__secret）")
print(f"  acc._Account__secret = {acc._Account__secret!r}   <- 想访问还是能访问")   # 用改写后的名字绕过
print("  结论：这不是安全机制，只是防子类覆盖")


print("=" * 60)
print("4. 类属性写可变对象 = 所有实例共享（高频 bug）")
print("=" * 60)

# ============================================================
# 本节演示：可变类属性被所有实例共享（Python 里的"隐式 static"）
# 为什么重要：这行 items = [] 写在类体里，就等价 Java 的 static List items，所有实例共用一份，改一个全变。
# Java 对照：正确做法是写在构造器里 this.items = new ArrayList<>()，对应下面的 self.items = []。
# ============================================================

class Bad:
    items = []                       # 类属性   ← 整个类只有这一个 list

a1, a2 = Bad(), Bad()                # 两个实例
a1.items.append(1)                   # append 是"改内容"，找到的仍是类属性那份 list
print(f"  Bad.items 类属性：a1.items={a1.items} a2.items={a2.items}   <- 共享")   # 两个都显示 [1]

class Good:
    def __init__(self):
        self.items = []              # 实例属性   ← 每次构造都新建，互不影响

b1, b2 = Good(), Good()
b1.items.append(1)                   # 只改 b1 自己的 list
print(f"  实例属性：        b1.items={b1.items} b2.items={b2.items}   <- 独立")   # b2 仍是 []


print("=" * 60)
print("5. 继承")
print("=" * 60)

# ============================================================
# 本节演示：单继承、方法重写、多态、isinstance / issubclass
# 为什么重要：Python 继承语法是 class Dog(Animal)，但 __init__ 不会自动调父类（与 Java 最大差异）。
# Java 对照：Dog extends Animal；super().__init__(name) 相当于 Java 里隐式的 super(name)。
# ============================================================

class Animal:
    def __init__(self, name):
        self.name = name             # 父类字段；子类不调 super().__init__ 就没有它

    def speak(self):
        return "..."                 # 默认实现，子类可覆盖（等价 Java 的虚方法）

    def __str__(self):
        return f"{type(self).__name__}({self.name})"   # type(self).__name__ 拿到实际运行时类名（子类会显示 Dog/Cat）

class Dog(Animal):
    def __init__(self, name, breed):
        super().__init__(name)       # 必须显式调用！Java 会自动调   ← 少写这一行不会报错，只会在访问 self.name 时炸
        self.breed = breed           # 子类自己的字段

    def speak(self):
        return "汪汪"                 # 重写父类方法，无需 @Override

class Cat(Animal):
    def speak(self):
        return "喵"                   # 没有 __init__，直接继承父类构造

for a in [Dog("旺财", "柴犬"), Cat("咪咪")]:   # 列表中放两个不同子类实例
    print(f"  {a} -> {a.speak()}")              # 多态：同一个调用点，按实际类型执行不同实现

print(f"\n  isinstance(Dog(...), Animal) = {isinstance(Dog('x', 'y'), Animal)}")   # True，等价 Java instanceof
print(f"  issubclass(Dog, Animal)      = {issubclass(Dog, Animal)}")               # True，判断类与类的继承关系


print("=" * 60)
print("6. 忘调 super().__init__ 的后果")
print("=" * 60)

# ============================================================
# 本节演示：忘记 super().__init__ 的后果（延迟爆炸）
# 为什么重要：构造时完全不报错，只有真正访问父类字段时才抛 AttributeError，很容易被误判成别的问题。
# Java 对照：Java 编译器会强制插入 super()，不存在这个坑；Python 不做任何检查。
# ============================================================

class BrokenDog(Animal):
    def __init__(self, name, breed):
        self.breed = breed           # 忘了 super().__init__(name)

d = BrokenDog("旺财", "柴犬")         # 构造成功，没有异常——因为 Python 不检查父类是否初始化
print(f"  d.breed = {d.breed}")      # 自己的字段存在，能正常读
try:
    print(d.name)                    # 这里才炸：父类 __init__ 没跑，self.name 从未被赋值
except AttributeError as e:
    print(f"  d.name  -> AttributeError: {e}   <- 不报错，直到你访问时才炸")


print("=" * 60)
print("7. 方法类型：实例 / 类 / 静态")
print("=" * 60)

# ============================================================
# 本节演示：三种方法（实例 / 类 / 静态）以及 classmethod 的继承优势
# 为什么重要：Java 只有实例方法和 static 方法；Python 多出 classmethod，用来写"能适配子类"的工厂构造。
# Java 对照：@staticmethod ≈ static 方法；@classmethod ≈ 静态工厂 Order.of(...)，但 cls 会自动变成子类。
# ============================================================

class Factory:
    rate = 0.9                       # 类属性，等价 Java 的 static 字段

    def __init__(self, no):
        self.no = no                 # 实例字段

    def instance_method(self):
        return f"self.no={self.no}"  # 实例方法：第一个参数 self，自动传实例

    @classmethod
    def from_dict(cls, d):
        return cls(d["no"])          # cls 让子类也能正确创建   ← cls 是调用者本身，不是写死的 Factory

    @staticmethod
    def valid_no(no):
        return len(no) == 5          # 静态方法：不自动传 self/cls，等价 Java 的 static 工具方法

print(f"  Factory.from_dict({{'no':'A'}}).no = {Factory.from_dict({'no': 'A'}).no}")   # 通过类调用工厂
print(f"  Factory.valid_no('NO001')        = {Factory.valid_no('NO001')}")           # 长度 5 → True
print(f"  Factory('X').instance_method()   = {Factory('X').instance_method()}")      # 实例方法需要实例

# classmethod 的继承优势
class SubFactory(Factory):
    pass                             # 空类体：只继承，不写任何东西

print(f"\n  type(Factory.from_dict(...)) = {type(Factory.from_dict({'no': 'a'})).__name__}")       # Factory
print(f"  type(SubFactory.from_dict(...)) = {type(SubFactory.from_dict({'no': 'a'})).__name__}   <- 自动适配")   # SubFactory，证明 cls 生效


print("=" * 60)
print("8. 多重继承与 MRO")
print("=" * 60)

# ============================================================
# 本节演示：多重继承与 MRO（C3 线性化），以及 super() 的真实含义
# 为什么重要：super() 不是"调父类"，而是"按 MRO 顺序调下一个"，这是理解菱形继承的关键。
# Java 对照：Java 不允许多继承类，只能多实现接口；Python 直接支持，并给出确定的查找顺序。
# ============================================================

class A1:
    def who(self): return "A1"        # 共同祖先

class B1(A1):
    def who(self): return "B1"        # 左父类

class C1(A1):
    def who(self): return "C1"        # 右父类

class D1(B1, C1):
    pass                              # 自己不实现，按 MRO 从左到右找

print(f"  D1().who() = {D1().who()}")   # B1：B1 在 MRO 中排在 C1 前
print(f"  MRO       = {[c.__name__ for c in D1.__mro__]}")   # 列表推导式取出每层的类名，顺序 D1→B1→C1→A1→object

class D2(B1, C1):
    def who(self):
        return f"D2->{super().who()}"   # super() 在本类 MRO 的下一站是 B1

print(f"\n  D2().who() = {D2().who()}   <- super() 是按 MRO 找下一个，不是找父类")   # D2->B1


print("=" * 60)
print("9. Mixin：给类加能力（唯一该用多继承的场景）")
print("=" * 60)

# ============================================================
# 本节演示：Mixin（混入）——给已有类"贴"上一个能力
# 为什么重要：这是业务代码里唯一推荐使用多继承的场景，避免为了一个 to_json 就建继承树。
# Java 对照：相当于 interface 的 default 方法，或用静态工具类 + 组合来实现；Python 直接多继承最省。
# ============================================================

import json                          # 标准库 JSON 模块（放在这里是为了贴近使用点，Python 允许任意位置 import）

class JsonMixin:
    def to_json(self):
        return json.dumps(self.__dict__, ensure_ascii=False)   # __dict__ 是实例的属性字典，可直接序列化

class User(JsonMixin):
    def __init__(self, name):
        self.name = name             # 只有一个字段

print(f"  User('张三').to_json() = {User('张三').to_json()}")   # 类通过 Mixin 免费获得 to_json 能力
