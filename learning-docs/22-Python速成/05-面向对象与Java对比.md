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
class Order:
    def __init__(self, order_no: str, amount: float):
        self.order_no = order_no
        self.amount = amount

    def is_large(self) -> bool:
        return self.amount > 10_000

    def __str__(self) -> str:
        return f"Order(order_no={self.order_no!r}, amount={self.amount})"


o = Order("NO001", 20000)
print(o)                        # Order(order_no='NO001', amount=20000)
print(o.order_no)               # 直接访问，没有 getOrderNo()
print(o.is_large())             # True
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
class A:
    def method(self):
        print(self)

a = A()
a.method()          # <__main__.A object at 0x...>
A.method(a)         # 完全等价！  a 就是传进去的 self
```

`a.method()` 本质是 `A.method(a)`——**实例被自动作为第一个参数传入**。这就是为什么 `self` 必须写在参数列表里：Python 没有"隐式 this"。

### 访问控制的真实做法

```python
class Account:
    def __init__(self):
        self.name = "公开"           # 公开
        self._balance = 0            # 约定：内部用，别外部改（但拦不住）
        self.__secret = "x"          # 名称改写为 _Account__secret（防止子类覆盖，不是安全机制）

a = Account()
a._balance = 999          # ⚠️ 能改，只是"不礼貌"
print(a.__secret)         # ❌ AttributeError
print(a._Account__secret) # ✅ 'x'，改写后的名字
```

> **心态转变**：Python 社区的说法是 "we are all consenting adults"——信任调用方。想在边界做校验，用 `@property`；想强约束，用 pydantic 模型（见 11 篇）。

---

## 二、`@dataclass`：替代 Lombok

**99% 的 Java POJO/DTO，在 Python 里都应该写成 dataclass。**

```python
from dataclasses import dataclass, field

@dataclass
class Order:
    order_no: str
    amount: float
    items: list[str] = field(default_factory=list)   # 可变默认值必须用 factory
    remark: str = ""

o = Order("NO001", 100.0)
print(o)        # Order(order_no='NO001', amount=100.0, items=[], remark='')
print(o == Order("NO001", 100.0))   # True    ← 自动生成 __eq__
```

`@dataclass` 自动生成：`__init__`、`__repr__`、`__eq__`。相当于：

```java
@Getter @Setter @ToString @EqualsAndHashCode @AllArgsConstructor
public class Order { ... }
```

### 常用参数

```python
@dataclass(
    frozen=True,        # 不可变 + 可哈希（可放 set / 作 dict key）
    slots=True,         # 3.10+：省内存 + 属性访问更快（禁止动态加属性）
    kw_only=True,       # 3.10+：所有字段必须关键字传
    order=True,         # 生成 <, > 等比较方法
)
class Point:
    x: int
    y: int

p = Point(1, 2)
# p.x = 3             # ❌ FrozenInstanceError
print({Point(1,2), Point(1,2)})   # 只有一个元素
```

### 对比手写类的差距

```python
# 手写：15 行
class Order:
    def __init__(self, no, amount):
        self.no = no
        self.amount = amount
    def __repr__(self):
        return f"Order(no={self.no!r}, amount={self.amount!r})"
    def __eq__(self, other):
        if not isinstance(other, Order):
            return NotImplemented
        return (self.no, self.amount) == (other.no, other.amount)
    def __hash__(self):
        return hash((self.no, self.amount))

# dataclass：4 行
@dataclass(frozen=True)
class Order:
    no: str
    amount: float
```

**什么时候不用 dataclass**：

- 需要复杂校验逻辑 → 用 pydantic `BaseModel`（FastAPI 场景）
- 需要大量自定义方法/状态机 → 普通类
- 字段结构动态变化 → 直接用 `dict`

---

## 三、`@property`：比 getter/setter 优雅

```python
class Account:
    def __init__(self, balance=0):
        self._balance = balance

    @property
    def balance(self):
        """只读属性，访问方式像字段：account.balance"""
        return self._balance

    @balance.setter
    def balance(self, value):
        if value < 0:
            raise ValueError("余额不能为负")
        self._balance = value

    @balance.deleter
    def balance(self):
        raise AttributeError("余额不可删除")


a = Account(100)
print(a.balance)        # 100         ← 不用写括号
a.balance = 200         # ✅ 走 setter
# a.balance = -1        # ❌ ValueError
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
class Order:
    def __init__(self, price, qty):
        self.price = price
        self.qty = qty

    @property
    def total(self):
        return self.price * self.qty

o = Order(10, 3)
print(o.total)      # 30   ← 像字段一样用，实际每次计算
```

### 缓存属性（重计算场景）

```python
from functools import cached_property   # 3.8+

class Report:
    def __init__(self, rows):
        self.rows = rows

    @cached_property
    def summary(self):
        print("计算一次")
        return sum(r["amount"] for r in self.rows)

r = Report([{"amount": 1}, {"amount": 2}])
print(r.summary)    # 3   打印"计算一次"
print(r.summary)    # 3   不再打印
```

---

## 四、类方法、静态方法、实例方法

```python
class Order:
    rate = 0.9          # 类属性（相当于 Java static 字段）

    def __init__(self, no):
        self.no = no

    def instance_method(self):          # 实例方法：能访问 self
        return self.no

    @classmethod
    def from_dict(cls, d):              # 类方法：第一个参数是类
        return cls(d["no"])             # 常用于工厂方法 / 替代重载构造

    @staticmethod
    def valid_no(no):                   # 静态方法：什么都不自动传
        return len(no) == 5


o = Order.from_dict({"no": "NO001"})    # 工厂方法
print(Order.valid_no("NO001"))          # True
print(o.instance_method())              # NO001
print(Order.rate)                       # 0.9
```

| Java | Python |
|---|---|
| `static` 方法 | `@staticmethod` |
| 静态工厂 `Order.of(...)` | `@classmethod`（`cls` 让子类也能用） |
| 实例方法 | 默认，带 `self` |
| 构造重载 | 用 `@classmethod` 工厂 + 默认参数 |

### 为什么用 `@classmethod` 而不是 `@staticmethod` 做工厂

```python
class Base:
    @classmethod
    def create(cls):
        return cls()            # cls 是调用者，子类调用时创建子类实例

class Child(Base):
    pass

print(type(Base.create()))      # <class 'Base'>
print(type(Child.create()))     # <class 'Child'>   ← 自动适配

# 用 staticmethod 就锁死了
class Base2:
    @staticmethod
    def create():
        return Base2()

print(type(Child.create()))     # <class 'Base2'>   ❌
```

---

## 五、继承

```python
class Animal:
    def __init__(self, name):
        self.name = name

    def speak(self) -> str:
        return "..."

class Dog(Animal):
    def __init__(self, name, breed):
        super().__init__(name)          # 必须显式调用父类 __init__
        self.breed = breed

    def speak(self) -> str:
        return "汪汪"

d = Dog("旺财", "柴犬")
print(d.speak(), d.name, d.breed)       # 汪汪 旺财 柴犬
print(isinstance(d, Animal))            # True   ← instanceof
print(issubclass(Dog, Animal))          # True
```

> **注意**：子类不会自动调用父类 `__init__`（Java 里会自动调 super()）。忘写就丢字段，而且不报错。这是高频 bug。

### 多重继承与 MRO

Python 支持多继承（Java 不支持），用 **C3 线性化** 决定方法查找顺序：

```python
class A:
    def who(self): return "A"
class B(A):
    def who(self): return "B"
class C(A):
    def who(self): return "C"
class D(B, C):
    pass

print(D().who())        # B
print([c.__name__ for c in D.__mro__])
# ['D', 'B', 'C', 'A', 'object']

# super() 是"按 MRO 找下一个"，不是"找父类"
class D2(B, C):
    def who(self):
        return f"D2->{super().who()}"
print(D2().who())       # D2->B
```

**实践建议**：业务代码里几乎不该用多重继承。它主要用于**混入（Mixin）**——给类加能力：

```python
class JsonMixin:
    def to_json(self):
        import json
        from dataclasses import asdict
        return json.dumps(asdict(self), ensure_ascii=False)

class Order(JsonMixin):        # 单个 Mixin 是可接受的
    ...
```

### `super()` 的坑

```python
# ❌ 写死父类名：菱形继承时会重复调用
class D(B, C):
    def __init__(self):
        B.__init__(self)
        C.__init__(self)

# ✅ 用 super()，让 MRO 决定
class D(B, C):
    def __init__(self):
        super().__init__()
```

---

## 六、接口的三种替代方案

Java 的 `interface` 在 Python 里有三个层次的替代，**按需要选，别一上来就抽接口**。

### 方案一：什么都不写（鸭子类型）

```python
def notify(sender):
    sender.send("hello")        # 只要有 send 方法就行，不检查类型

class EmailSender:
    def send(self, msg): ...

class SmsSender:
    def send(self, msg): ...

notify(EmailSender())
notify(SmsSender())
```

90% 的场景这是最好的选择。Java 里你必须抽 `NotificationSender` 接口 + 两个实现 + 注入，Python 里零成本。

### 方案二：`Protocol`（结构化类型，等价 interface 但不需要显式实现）

```python
from typing import Protocol

class NotificationSender(Protocol):
    def send(self, msg: str) -> None: ...

def notify(sender: NotificationSender, msg: str) -> None:   # mypy 会检查
    sender.send(msg)

class EmailSender:                  # 注意：没有 implements！
    def send(self, msg: str) -> None:
        print(f"邮件：{msg}")

notify(EmailSender(), "hi")         # ✅ 静态检查通过
```

这是"**隐式实现**"——只要结构匹配就满足协议。好处：不改已有类就能适配接口。

### 方案三：`ABC`（抽象基类，等价 abstract class）

```python
from abc import ABC, abstractmethod

class Repository(ABC):
    @abstractmethod
    def find_by_id(self, id: int): ...

    @abstractmethod
    def save(self, entity): ...

    def health(self):               # 可以有默认实现（Java interface default method）
        return "ok"

class OrderRepo(Repository):        # 必须显式继承
    def find_by_id(self, id): return {"id": id}
    def save(self, entity): pass

# Repository()      # ❌ TypeError: Can't instantiate abstract class
OrderRepo()         # ✅
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
class Money:
    def __init__(self, amount: float, currency: str = "CNY"):
        self.amount = amount
        self.currency = currency

    def __repr__(self):                     # 调试显示（给开发者看）
        return f"Money({self.amount!r}, {self.currency!r})"

    def __str__(self):                      # 用户显示（给用户看）
        return f"{self.amount:.2f} {self.currency}"

    def __eq__(self, other):                # ==
        if not isinstance(other, Money):
            return NotImplemented
        return (self.amount, self.currency) == (other.amount, other.currency)

    def __hash__(self):                     # 可放 set / dict key
        return hash((self.amount, self.currency))

    def __add__(self, other):               # +
        if self.currency != other.currency:
            raise ValueError("币种不一致")
        return Money(self.amount + other.amount, self.currency)

    def __lt__(self, other):                # <  （sorted 需要）
        return self.amount < other.amount

    def __len__(self):                      # len()
        return int(self.amount)


print(repr(Money(10)))
print(str(Money(10)))
print(Money(10) + Money(5))                 # 15.00 CNY
print(sorted([Money(3), Money(1)]))         # [Money(1.0,'CNY'), Money(3.0,'CNY')]
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
class Page:
    def __init__(self, items, size=10):
        self.items = items
        self.size = size

    def __len__(self):
        return len(self.items)

    def __getitem__(self, i):
        return self.items[i]

    def __iter__(self):
        return iter(self.items)

    def __contains__(self, v):
        return v in self.items


p = Page([1, 2, 3])
print(len(p))            # 3
print(p[0])              # 1
print(2 in p)            # True
print(list(p))           # [1, 2, 3]
for x in p:
    print(x)
```

### 实用示例：`__call__` 让实例变成可调用（做"带状态的函数"）

```python
class RateLimiter:
    def __init__(self, max_calls):
        self.max_calls = max_calls
        self.calls = 0

    def __call__(self, fn):
        def wrapper(*args, **kwargs):
            self.calls += 1
            if self.calls > self.max_calls:
                raise RuntimeError("超限")
            return fn(*args, **kwargs)
        return wrapper

limiter = RateLimiter(2)
```

---

## 八、`__slots__`：省内存的优化开关（了解即可）

```python
class WithDict:
    def __init__(self, x):
        self.x = x

class WithSlots:
    __slots__ = ("x",)
    def __init__(self, x):
        self.x = x

import sys
a, b = WithDict(1), WithSlots(1)
print(sys.getsizeof(a.__dict__))     # 有 __dict__（约 104 字节起）
# print(b.__dict__)                  # ❌ AttributeError，没有 __dict__
```

收益：每实例省几十字节，属性访问略快。代价：**不能再动态加属性**。

> 建议：除非在写需要创建百万实例的数据结构，否则**不要用**。Python 3.10+ 用 `@dataclass(slots=True)` 更安全。

---

## 九、类属性的坑（可变类属性是共享的）

```python
class Bad:
    items = []          # ❌ 类属性，所有实例共享

a, b = Bad(), Bad()
a.items.append(1)
print(b.items)          # [1]   ← 不是 [ ]

class Good:
    def __init__(self):
        self.items = []  # ✅ 实例属性

# 这是 Java 的反直觉点：
# Java 里 static List items = new ArrayList<>() 也会共享，
# 但 Java 程序员清楚地知道 static 意味着共享，Python 里这个 "static" 是隐式的
```

**判断依据**：写在 `class` 体内、方法外 → 类属性（共享）。写在 `__init__` 里 `self.xxx` → 实例属性。

---

## 十、Pydantic：FastAPI 场景下的"强约束类"（项目实战用）

`ai-cs-py-chat` 用的是 Pydantic 而不是 dataclass，因为要做**入参校验**：

```python
# app/schemas/chat.py 的真实写法（简化）
from pydantic import BaseModel, Field

class Message(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    messages: list[Message]
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    max_tokens: int = Field(default=2048, gt=0)

# 校验自动发生
req = ChatRequest(messages=[{"role": "user", "content": "hi"}])
print(req.messages[0].role)      # user

ChatRequest(messages=[], temperature=9.9)    # ❌ ValidationError
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
# app/config.py —— 用 pydantic-settings 读 .env，替代 @ConfigurationProperties
class Settings(BaseSettings):
    llm_base_url: str = "https://api.deepseek.com"
    llm_api_key: str = ""
    model_config = SettingsConfigDict(env_file=".env")
```

```java
// Java 等价
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(String baseUrl, String apiKey) {}
```

```python
# app/services/chat_service.py —— 模块级单例 + lru_cache，替代 Bean
from functools import lru_cache

@lru_cache(maxsize=1)
def get_chat_service() -> ChatService:
    return ChatService()
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
# ① 用 dataclass 定义一个 Money 类，支持相加、比较、打印

# ② 下面输出什么？
class A:
    items = []
class B(A):
    pass
A.items.append(1)
print(B.items)          # ?

# ③ 用 @property 实现"只读 + 校验"的 age 属性

# ④ 用 Protocol 定义一个 Repo 协议，写两个实现类，并用 mypy 检查

# ⑤ 解释为什么子类忘写 super().__init__() 不会报错但会出 bug
```

答案：[`code/04_oop/`](code/04_oop/)

---

**上一篇**：[04-函数与作用域](04-函数与作用域.md)　**下一篇**：[06-异常、文件与上下文管理器](06-异常、文件与上下文管理器.md)
