"""01 · 类的基础：self、类属性、继承

运行: python 04_oop/01_basic.py
"""

print("=" * 60)
print("1. 最小类")
print("=" * 60)

class Order:
    counter = 0                     # 类属性（所有实例共享，类似 static）

    def __init__(self, order_no, amount):
        self.order_no = order_no    # 实例属性
        self.amount = amount
        Order.counter += 1

    def is_large(self):
        return self.amount > 10_000

    def __str__(self):              # = toString()
        return f"Order({self.order_no}, {self.amount})"

    def __repr__(self):             # 调试显示
        return f"Order(order_no={self.order_no!r}, amount={self.amount!r})"


o = Order("NO001", 20000)
print(f"  print(o)          -> {o}")
print(f"  repr(o)           -> {o!r}")
print(f"  o.order_no        -> {o.order_no}      <- 直接访问，没有 getter")
print(f"  o.is_large()      -> {o.is_large()}")
print(f"  Order.counter     -> {Order.counter}")


print("\n" + "=" * 60)
print("2. self 到底是什么")
print("=" * 60)

class A:
    def method(self):
        return id(self)

a = A()
print(f"  a.method()   -> {a.method()}")
print(f"  A.method(a)  -> {A.method(a)}     <- 完全等价")
print(f"  id(a)        -> {id(a)}")
print("  a.method() 本质是 A.method(a)：实例被自动作为第一个参数")


print("=" * 60)
print("3. 访问控制：靠约定，不靠关键字")
print("=" * 60)

class Account:
    def __init__(self):
        self.name = "公开"
        self._balance = 0            # 约定：内部用
        self.__secret = "x"          # 名称改写成 _Account__secret

    def show(self):
        return f"{self.name} / {self._balance} / {self.__secret}"

acc = Account()
print(f"  acc.name          = {acc.name}")
print(f"  acc._balance      = {acc._balance}      <- 能访问，只是不礼貌")
acc._balance = 999
print(f"  改了之后          = {acc._balance}")

try:
    acc.__secret
except AttributeError:
    print(f"  acc.__secret      -> AttributeError（被改写成 _Account__secret）")
print(f"  acc._Account__secret = {acc._Account__secret!r}   <- 想访问还是能访问")
print("  结论：这不是安全机制，只是防子类覆盖")


print("=" * 60)
print("4. 类属性写可变对象 = 所有实例共享（高频 bug）")
print("=" * 60)

class Bad:
    items = []                       # 类属性

a1, a2 = Bad(), Bad()
a1.items.append(1)
print(f"  Bad.items 类属性：a1.items={a1.items} a2.items={a2.items}   <- 共享")

class Good:
    def __init__(self):
        self.items = []              # 实例属性

b1, b2 = Good(), Good()
b1.items.append(1)
print(f"  实例属性：        b1.items={b1.items} b2.items={b2.items}   <- 独立")


print("=" * 60)
print("5. 继承")
print("=" * 60)

class Animal:
    def __init__(self, name):
        self.name = name

    def speak(self):
        return "..."

    def __str__(self):
        return f"{type(self).__name__}({self.name})"

class Dog(Animal):
    def __init__(self, name, breed):
        super().__init__(name)       # 必须显式调用！Java 会自动调
        self.breed = breed

    def speak(self):
        return "汪汪"

class Cat(Animal):
    def speak(self):
        return "喵"

for a in [Dog("旺财", "柴犬"), Cat("咪咪")]:
    print(f"  {a} -> {a.speak()}")

print(f"\n  isinstance(Dog(...), Animal) = {isinstance(Dog('x', 'y'), Animal)}")
print(f"  issubclass(Dog, Animal)      = {issubclass(Dog, Animal)}")


print("=" * 60)
print("6. 忘调 super().__init__ 的后果")
print("=" * 60)

class BrokenDog(Animal):
    def __init__(self, name, breed):
        self.breed = breed           # 忘了 super().__init__(name)

d = BrokenDog("旺财", "柴犬")
print(f"  d.breed = {d.breed}")
try:
    print(d.name)
except AttributeError as e:
    print(f"  d.name  -> AttributeError: {e}   <- 不报错，直到你访问时才炸")


print("=" * 60)
print("7. 方法类型：实例 / 类 / 静态")
print("=" * 60)

class Factory:
    rate = 0.9

    def __init__(self, no):
        self.no = no

    def instance_method(self):
        return f"self.no={self.no}"

    @classmethod
    def from_dict(cls, d):
        return cls(d["no"])          # cls 让子类也能正确创建

    @staticmethod
    def valid_no(no):
        return len(no) == 5

print(f"  Factory.from_dict({{'no':'A'}}).no = {Factory.from_dict({'no': 'A'}).no}")
print(f"  Factory.valid_no('NO001')        = {Factory.valid_no('NO001')}")
print(f"  Factory('X').instance_method()   = {Factory('X').instance_method()}")

# classmethod 的继承优势
class SubFactory(Factory):
    pass

print(f"\n  type(Factory.from_dict(...)) = {type(Factory.from_dict({'no': 'a'})).__name__}")
print(f"  type(SubFactory.from_dict(...)) = {type(SubFactory.from_dict({'no': 'a'})).__name__}   <- 自动适配")


print("=" * 60)
print("8. 多重继承与 MRO")
print("=" * 60)

class A1:
    def who(self): return "A1"

class B1(A1):
    def who(self): return "B1"

class C1(A1):
    def who(self): return "C1"

class D1(B1, C1):
    pass

print(f"  D1().who() = {D1().who()}")
print(f"  MRO       = {[c.__name__ for c in D1.__mro__]}")

class D2(B1, C1):
    def who(self):
        return f"D2->{super().who()}"

print(f"\n  D2().who() = {D2().who()}   <- super() 是按 MRO 找下一个，不是找父类")


print("=" * 60)
print("9. Mixin：给类加能力（唯一该用多继承的场景）")
print("=" * 60)

import json

class JsonMixin:
    def to_json(self):
        return json.dumps(self.__dict__, ensure_ascii=False)

class User(JsonMixin):
    def __init__(self, name):
        self.name = name

print(f"  User('张三').to_json() = {User('张三').to_json()}")
