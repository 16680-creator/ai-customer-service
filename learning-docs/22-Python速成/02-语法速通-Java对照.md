# 02 · 语法速通：Java 对照

> 这一篇是**速查手册式的通读材料**，覆盖日常编码 90% 的语法。
> 读法建议：不要背。通读一遍建立索引，然后打开 `code/01_basics/` 边跑边改。

---

## 一、第一印象：同一个程序，两种写法

**Java**

```java
import java.util.*;

public class Hello {
    public static void main(String[] args) {
        List<String> names = new ArrayList<>(Arrays.asList("Alice", "Bob", "Carol"));
        Map<String, Integer> lens = new LinkedHashMap<>();
        for (String n : names) {
            if (n.length() > 3) {
                lens.put(n, n.length());
            }
        }
        System.out.println(lens);
    }
}
// → {Alice=5, Carol=5}
```

**Python**

```python
names = ["Alice", "Bob", "Carol"]
lens = {n: len(n) for n in names if len(n) > 3}
print(lens)
# → {'Alice': 5, 'Carol': 5}
```

差异清单（就是你要适应的全部）：

| 维度 | Java | Python |
|---|---|---|
| 编译 | 编译型，`javac` → 字节码 | 解释型，源码直接执行（有 `.pyc` 缓存但不强制） |
| 类型 | 静态、强类型 | 动态、强类型（运行时才检查，但有类型注解可选） |
| 代码块 | `{}` | **缩进**（4 空格，Tab/空格混用会报错） |
| 语句结尾 | `;` | 换行 |
| 入口 | `public static void main` | 文件顶层代码，或 `if __name__ == "__main__":` |
| 变量声明 | `int x = 1;` | `x = 1`（无需声明） |
| 命名 | `camelCase` / `PascalCase` | `snake_case` / `PascalCase`（类名仍大写开头） |
| 常量 | `final` / `static final` | 全大写约定，**无强制** |
| 空值 | `null` | `None` |
| 打印 | `System.out.println` | `print()` |

---

## 二、变量、类型与真值判断

```python
x = 42              # int（任意精度，没有 int/long 之分）
y = 3.14            # float（就是 double，Python 没有 float32）
s = "hello"         # str（不可变）
b = True            # bool（注意大写 T/F）
n = None            # 空值

# 多重赋值
a, b_ = 1, 2
a, b_ = b_, a       # 交换，不需要临时变量

# 类型查询（运行时）
print(type(x))          # <class 'int'>
print(isinstance(x, int))   # True —— 等价于 Java 的 instanceof

# 类型转换
int("42")      # 42       对应 Integer.parseInt
str(42)        # "42"
float("3.14")  # 3.14
```

### 整数与除法（**Java 程序员第一坑**）

```python
7 / 2      # 3.5   ← 真除法，不是整除！
7 // 2     # 3     ← 整除（向下取整，注意负数：-7 // 2 == -4）
7 % 2      # 1
2 ** 10    # 1024  ← 幂运算，Java 没有运算符
divmod(7, 2)   # (3, 1)  ← 商和余数一次拿到
```

```java
// Java
7 / 2        // 3        ← 整数除法
7 % 2        // 1
Math.pow(2, 10)
```

> **大坑**：`int / int` 在 Java 里是整除，在 Python 里是浮点。金融计算时尤其注意——`100 / 3` 在 Python 里是 `33.333...`。

### 真值判断（Python 独有，非常好用也非常坑）

```python
# 以下都是"假"
if not []:      pass    # 空列表
if not {}:      pass    # 空字典
if not "":      pass    # 空字符串
if not 0:       pass    # 零
if not None:    pass    # None
if not False:   pass

# 所以写法是
items = []
if not items:               # ✅ Pythonic
    print("空")

if len(items) == 0:         # ⚠️ 能用但不地道
    print("空")

# 坑：这个判断是错的
if x == None:               # ❌
if x is None:               # ✅ 判空一律用 is
```

---

## 三、字符串

```python
s = "Hello"

len(s)              # 5
s.upper()           # "HELLO"        （注意：返回新字符串，原串不变）
s.lower()           # "hello"
s[0]                # 'H'            索引
s[-1]               # 'o'            负索引 = 从后往前，Java 没有
s[1:4]              # 'ell'          切片，左闭右开
s[::-1]             # 'olleH'        反转
s.strip()           # 去首尾空白（= trim）
s.replace("l", "L") # "HeLLo"
s.split("l")        # ['He', '', 'o']
"-".join(["a","b"]) # "a-b"          Array.join 的反向操作
"ell" in s          # True           = contains
s * 3               # "HelloHelloHello"
s.startswith("He")  # True
s.find("l")         # 2              找不到返回 -1（不是抛异常）
```

### f-string（**取代 String.format / 字符串拼接**）

```python
name, age = "张三", 30

print(f"{name} 今年 {age} 岁")
print(f"{age:05d}")              # 00030      数字补零
print(f"{3.14159:.2f}")          # 3.14       保留两位
print(f"{0.856:.1%}")            # 85.6%      百分比
print(f"{1234567:,}")            # 1,234,567  千分位
print(f"{name!r}")               # '张三'     显示 repr（带引号）
print(f"{age=}")                 # age=30     调试神器，3.8+
print(f"{'左':<10}|")            # 左对齐
print(f"{'右':>10}|")            # 右对齐
print(f"{'中':^10}|")            # 居中
```

```java
// Java 等价
String.format("%s 今年 %d 岁", name, age)
String.format("%05d", age)
String.format("%.2f", 3.14159)
```

### 多行字符串

```python
sql = """
SELECT *
FROM orders
WHERE id = :id
"""
# 三引号 = 多行 + 保留格式，等价于 Java 15 的 text block
```

---

## 四、控制流

### if / elif / else

```python
score = 85

if score >= 90:
    grade = "A"
elif score >= 80:          # 注意是 elif，不是 else if
    grade = "B"
elif score >= 60:
    grade = "C"
else:
    grade = "D"

# 三元表达式（顺序与 Java 不同！）
label = "及格" if score >= 60 else "不及格"
# Java: score >= 60 ? "及格" : "不及格"
```

### for 循环（**没有经典 for(i=0;i<n;i++)**）

```python
# 遍历序列
for name in ["a", "b"]:
    print(name)

# 需要下标：用 enumerate（替代 Java 的 for (int i = 0; ...)）
for i, name in enumerate(["a", "b"]):
    print(i, name)

for i, name in enumerate(["a", "b"], start=1):   # 从 1 开始
    print(i, name)

# 范围循环：range
for i in range(5):          # 0,1,2,3,4
    print(i)
for i in range(2, 10, 3):   # 2,5,8
    print(i)
for i in range(10, 0, -1):  # 倒序
    print(i)

# 同时遍历两个序列：zip
for a, b in zip([1, 2], ["x", "y"]):
    print(a, b)             # 1 x / 2 y

# 遍历字典
d = {"a": 1, "b": 2}
for k in d:                 # 遍历键
    print(k)
for k, v in d.items():      # 键值对（最常用）
    print(k, v)
for v in d.values():
    print(v)
```

### while / break / continue

```python
i = 0
while i < 5:
    if i == 2:
        i += 1
        continue
    if i == 4:
        break
    print(i)
    i += 1
```

> **没有 `i++` / `++i`**！Python 里 `i++` 是语法错误。只能 `i += 1`。

### for-else（Java 完全没有，很好用）

```python
# 找第一个偶数，找不到则打印提示
for n in [1, 3, 5, 7]:
    if n % 2 == 0:
        print("找到了", n)
        break
else:
    print("没有偶数")       # 循环正常结束（没 break）才执行
```

### match（3.10+，类似 Java 21 的 switch 模式匹配）

```python
def handle(cmd):
    match cmd:
        case "start":
            return "启动"
        case "stop" | "halt":        # 多值
            return "停止"
        case {"action": action}:     # 匹配字典结构
            return f"动作 {action}"
        case _:
            return "未知"
```

日常更常用字典分发（性能更好，也更灵活）：

```python
handlers = {
    "start": lambda: "启动",
    "stop": lambda: "停止",
}
handlers.get(cmd, lambda: "未知")()
```

---

## 五、集合类型总览（详见 03 篇）

```python
lst = [1, 2, 3]                  # list       → ArrayList
tup = (1, 2, 3)                  # tuple      → 不可变 list（无直接对应）
st  = {1, 2, 3}                  # set        → HashSet
dct = {"a": 1}                   # dict       → LinkedHashMap（3.7+ 保证插入序）
```

```python
# 常用操作
lst.append(4)          # add
lst.extend([5, 6])     # addAll
lst.insert(0, 0)       # add(index, e)
lst.pop()              # 移除末尾并返回
lst.pop(0)             # 移除指定下标
lst.remove(2)          # 按值删除（值不存在会抛 ValueError）
lst.index(3)           # 找下标
lst.sort()             # 原地排序
sorted(lst)            # 返回新列表
lst.reverse()          # 原地反转
3 in lst               # contains
len(lst)               # size()

dct["b"] = 2           # put
dct.get("c")           # 不存在返回 None（不抛异常！）
dct.get("c", 0)        # 带默认值
dct.setdefault("c", 9) # 不存在才设
dct.pop("a")           # 删除并返回
"a" in dct             # containsKey
dct.keys(); dct.values(); dct.items()
dct | {"x": 1}         # 合并（3.9+，返回新字典）
```

> **最大差异**：Java 的 `Map.get` 返回 null 但 `List.get` 越界抛异常；Python 的 `dict["不存在"]` 会抛 `KeyError`，`list[越界]` 抛 `IndexError`。所以字典取值优先用 `.get()`。

---

## 六、函数

```python
def greet(name: str, greeting: str = "你好") -> str:
    return f"{greeting}，{name}"

greet("张三")                    # 位置参数
greet("张三", "早上好")           # 位置参数
greet(name="张三", greeting="嗨")  # 关键字参数（Java 没有）
greet(greeting="嗨", name="张三")  # 顺序无关
```

Python 函数的特点（Java 都没有）：

```python
# 1. 默认参数
def f(a, b=2): ...

# 2. 可变位置参数 → 元组
def f2(*args):
    print(args)         # (1, 2, 3)
f2(1, 2, 3)

# 3. 可变关键字参数 → 字典
def f3(**kwargs):
    print(kwargs)       # {'a': 1, 'b': 2}
f3(a=1, b=2)

# 4. 解包调用
nums = [1, 2, 3]
f2(*nums)               # 等价于 f2(1, 2, 3)

d = {"a": 1, "b": 2}
f3(**d)                 # 等价于 f3(a=1, b=2)

# 5. 强制关键字参数（* 之后的必须用关键字传）
def f4(a, *, b, c): ...
f4(1, b=2, c=3)         # ✅
# f4(1, 2, 3)           # ❌ TypeError

# 6. 函数是一等公民
def apply(fn, x):
    return fn(x)
apply(lambda v: v * 2, 5)    # 10
```

> 详细展开见 [04 篇](04-函数与作用域.md)。

---

## 七、类（详见 05 篇）

```python
class Order:
    # 类属性（所有实例共享，类似 Java static）
    counter = 0

    def __init__(self, order_no: str, amount: float):
        # 实例属性（类似 Java 字段，但必须在 __init__ 里赋初值）
        self.order_no = order_no
        self.amount = amount
        Order.counter += 1

    def is_large(self) -> bool:
        return self.amount > 10_000

    def __str__(self) -> str:          # 等价于 toString()
        return f"Order({self.order_no}, {self.amount})"

    def __repr__(self) -> str:         # 调试/REPL 显示
        return f"Order(order_no={self.order_no!r}, amount={self.amount!r})"


o = Order("NO001", 20000)
print(o)                    # Order(NO001, 20000)
print(o.is_large())         # True
```

对照点：

| Java | Python | 说明 |
|---|---|---|
| `this` | `self` | **必须显式写在参数列表第一位**，不是关键字 |
| `private String x;` | `self.x = x` | 没有访问修饰符，靠 `_x`（约定）表示"内部用" |
| `Order()` 无参构造 | `__init__` | 没有构造函数重载概念 |
| `toString()` | `__str__` | 魔术方法 |
| `equals/hashCode` | `__eq__` / `__hash__` | |
| 静态方法 | `@staticmethod` | |
| 类方法 | `@classmethod` | 第一个参数是 `cls`，常用于工厂方法 |
| 属性读写 | `@property` | 比 getter/setter 优雅得多 |

---

## 八、异常

```python
try:
    result = 10 / 0
except ZeroDivisionError as e:
    print(f"除零：{e}")
except (TypeError, ValueError) as e:          # 捕获多种
    print(f"类型或值错误：{e}")
except Exception as e:                         # 兜底（相当于 catch (Exception)）
    print(f"未预期：{e}")
    raise                                      # 重新抛出（= throw e）
else:
    print("没异常才会执行")                     # Java 没有
finally:
    print("总会执行")
```

```java
try {
    int r = 10 / 0;
} catch (ArithmeticException e) {
    System.out.println("除零：" + e.getMessage());
} finally {
    System.out.println("总会执行");
}
```

自定义异常：

```python
class BusinessException(Exception):          # 继承 Exception
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


class OrderNotFoundException(BusinessException):
    def __init__(self, order_no: str):
        super().__init__("ORDER_NOT_FOUND", f"订单不存在：{order_no}")
        self.order_no = order_no
```

> **没有 checked exception**。Python 里所有异常都是 unchecked，不需要 `throws` 声明。这是与 Java 的一大分歧，见 [06 篇](06-异常、文件与上下文管理器.md)。

---

## 九、常用惯用法对照表（**背下来收益最高**）

| 需求 | Java | Python |
|---|---|---|
| 空判 | `if (x == null)` | `if x is None:` |
| 空集合判 | `if (list.isEmpty())` | `if not lst:` |
| 遍历带下标 | `for (int i = 0; i < a.length; i++)` | `for i, v in enumerate(a):` |
| 建 list | `new ArrayList<>()` | `[]` |
| 建 map | `new HashMap<>()` | `{}` |
| 映射 | `stream().map(f).collect(toList())` | `[f(x) for x in xs]` |
| 过滤 | `stream().filter(p)` | `[x for x in xs if p(x)]` |
| 求和 | `stream().mapToInt(x->x).sum()` | `sum(xs)` |
| 最值 | `Collections.max(xs)` | `max(xs)` |
| 排序 | `Collections.sort(xs, cmp)` | `sorted(xs, key=f)` |
| 分组 | `groupingBy` | `defaultdict(list)` / `itertools.groupby` |
| 字符串拼接 | `String.join` | `"".join(xs)` |
| 打日志 | `log.info("a={}", a)` | `logger.info("a=%s", a)` |
| 读文件 | `Files.readString(p)` | `Path(p).read_text(encoding="utf-8")` |
| 判空字符串 | `s.isEmpty()` | `if not s:` |
| 默认值 | `Optional.ofNullable(x).orElse(d)` | `x or d`（注意 0/"" 也会被替换） |
| 数值比较浮点 | `Math.abs(a-b) < eps` | `math.isclose(a, b)` |

---

## 十、Python 独有的"语法糖"清单（用好了会很快）

```python
# 1. 链式比较
if 0 <= x < 100:              # Java 里必须写 0 <= x && x < 100
    pass

# 2. 海象运算符（3.8+）：赋值 + 判断
if (n := len(data)) > 10:
    print(f"太长：{n}")

# 3. 解包
a, b, *rest = [1, 2, 3, 4]     # a=1, b=2, rest=[3,4]
first, *_, last = [1,2,3,4]    # first=1, last=4

# 4. 条件赋值 + or 短路默认值
name = user_input or "匿名"     # 空字符串/None 都变 "匿名"

# 5. 交换
a, b = b, a

# 6. 生成器表达式（惰性，省内存）
total = sum(x * x for x in range(1_000_000))

# 7. 任何可迭代对象都能 for
for line in open("a.txt", encoding="utf-8"):
    print(line)                 # 逐行读，不占内存

# 8. 下划线数字分隔符
amount = 1_000_000              # 与 Java 7 相同

# 9. 一行读写文件
content = open("a.txt", encoding="utf-8").read()   # ⚠️ 不推荐，见 06 篇

# 10. 用 in 做成员判断（list 是 O(n)，set/dict 是 O(1)）
if key in my_set:
    pass
```

---

## 十一、你不需要（或极少需要）的 Java 习惯

| Java 习惯 | Python 里怎么办 |
|---|---|
| 为每个字段写 getter/setter | 直接访问属性；需要校验时才用 `@property` |
| 抽 `IUserService` 接口 | 直接用具体类；需要多实现时才用 `Protocol` / `ABC` |
| `Builder` 模式 | 用默认参数 + 关键字参数 |
| `Optional<T>` | 返回 `None`，注解写 `str \| None` |
| `Objects.equals(a, b)` | `a == b`（None 参与也不崩） |
| `StringBuilder` 循环拼接 | `"".join(list)` |
| `Arrays.asList` 包装 | 直接用列表字面量 |
| 大量 DTO 转换类 | 用 `dict` 或 `@dataclass` |
| 为异常建 10 个子类 | 通常 1~2 个业务异常 + 错误码字段 |
| `synchronized` | `threading.Lock`（但先想清楚是否真的需要多线程） |

---

## 十二、10 分钟自测

不用查资料，写出下面代码。写不出来的部分，就是你要回头补的地方。

```python
# ① 给定列表，返回所有偶数的平方
nums = [1, 2, 3, 4, 5, 6]
# 期望 [4, 16, 36]


# ② 统计字符串中每个字符出现的次数
text = "hello"
# 期望 {'h': 1, 'e': 1, 'l': 2, 'o': 1}


# ③ 把两个列表合成字典
keys = ["a", "b"]
vals = [1, 2]
# 期望 {'a': 1, 'b': 2}


# ④ 一个函数，接收任意数量参数，返回最大值，空则返回 None


# ⑤ 安全地取字典深层值：get_nested(d, "a.b.c", default=0)
d = {"a": {"b": {"c": 42}}}
# 期望 42
```

参考答案在 [`code/01_basics/09_self_test.py`](code/01_basics/09_self_test.py)。

---

**上一篇**：[01-环境搭建与工具链](01-环境搭建与工具链.md)　**下一篇**：[03-数据类型与容器](03-数据类型与容器.md)
