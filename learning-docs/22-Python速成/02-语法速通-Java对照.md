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
# ---- 本段讲：同一件事的 Python 写法——"列表字面量 + 字典推导式" ----
# 上面 Java 用了 ArrayList + LinkedHashMap + for + if + put 共十几行；
# Python 两行就能完成，而且 dict 从 3.7 起保证插入顺序，等价 Java 的 LinkedHashMap。
names = ["Alice", "Bob", "Carol"]                  # list 字面量；Java 要 new ArrayList<>(List.of(...))
lens = {n: len(n) for n in names if len(n) > 3}    # 字典推导式：过滤长度>3，并建立 name -> 长度
print(lens)                                        # 等价 System.out.println(lens)

lens = {}                                          # 空字典字面量，等价 new LinkedHashMap<>()
for n in names:                                    # for-each 语义，直接遍历元素而非下标
    if len(n) > 3:                                 # 缩进就是代码块，没有大括号
        lens[n] = len(n)                           # 下标赋值即 put；键不存在会自动新增
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
# ---- 本段讲：变量、空值、类型查询与转换 ----
# Python 没有"声明变量"这一步，第一次赋值就创建；类型跟着值走，随时可以改绑。
x = 42              # int（任意精度，没有 int/long 之分）
y = 3.14            # float（就是 double，Python 没有 float32）
s = "hello"         # str（不可变）
b = True            # bool（注意大写 T/F）
n = None            # 空值

# 多重赋值
a, b_ = 1, 2        # 右边打包成元组 (1,2)，左边按位置解包；b_ 是为了不和上面的 b 冲突
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
# ---- 本段讲：/ 与 // 的区别，Java 程序员的第一坑 ----
# 每一行都是"表达式 # 结果"的形式，可直接粘到 REPL 里逐行验证。
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
# ---- 本段讲：什么是"假值"，以及判空/判 None 的推荐写法 ----
# Python 允许直接 if 任何对象：空容器、0、空串、None、False 都算假。
# 所以 Java 里的 isEmpty()/size()==0 在这里统一写成 not x。
# 以下都是"假"
if not []:      pass    # 空列表
if not {}:      pass    # 空字典
if not "":      pass    # 空字符串
if not 0:       pass    # 零
if not None:    pass    # None
if not False:   pass

# 所以写法是
items = []                  # 空列表，等价 new ArrayList<>()
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
# ---- 本段讲：str 常用方法速查（每行都是"表达式 → 返回值"）----
# 注意 str 不可变：每个方法都返回新串，原串 s 永远不变（和 Java String 一致）。
s = "Hello"        # 字面量；单双引号等价，不需要 new String

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
# ---- 本段讲：f-string 的格式说明符，逐行对应 String.format ----
# 语法 f"{值:格式}"：冒号后依次是"填充字符 + 对齐(<^>) + 宽度/精度 + 类型"。
name, age = "张三", 30            # 元组解包，一次赋两个变量

print(f"{name} 今年 {age} 岁")    # 最基础用法，等价 String.format("%s 今年 %s 岁", name, age)
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
# ---- 本段讲：三引号多行字符串 ----
# 三个双引号之间的内容原样保留换行和缩进，等价 Java 15 的 text block。
# 常用于写 SQL / JSON 模板；注意开头的换行也会包含进字符串。
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
# ---- 本段讲：if / elif / else 与三元表达式 ----
# 冒号 + 缩进就是代码块；elif 是 else if 的合并关键字（没有 else if 这种写法）。
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
# ---- 本段讲：for 循环的四种形态——遍历、带下标、数字范围、并行遍历 ----
# Python 的 for 只接受"可迭代对象"，没有 for(init;cond;step) 三表达式。
# 需要下标用 enumerate，需要计数用 range，需要并行用 zip。
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
# ---- 本段讲：while + continue/break 的执行顺序 ----
# 千万注意：continue 之前必须先 i += 1，否则条件永远为真，变成死循环。
i = 0
while i < 5:
    if i == 2:
        i += 1        # 先自增再 continue，否则 i 卡在 2 出不去
        continue      # 跳过本轮剩下的语句，直接回到条件判断
    if i == 4:
        break         # 整个 while 结束
    print(i)
    i += 1            # 普通路径也要自增；Python 没有 i++
```

> **没有 `i++` / `++i`**！Python 里 `i++` 是语法错误。只能 `i += 1`。

### for-else（Java 完全没有，很好用）

```python
# ---- 本段讲：for-else 语义——循环未被打断时执行 else ----
# 等价于 Java 里用 boolean found 标志 + 循环后 if (!found)，Python 直接内建。
# 找第一个偶数，找不到则打印提示
for n in [1, 3, 5, 7]:
    if n % 2 == 0:
        print("找到了", n)
        break                 # 一旦 break，else 整块被跳过
else:
    print("没有偶数")       # 循环正常结束（没 break）才执行
```

### match（3.10+，类似 Java 21 的 switch 模式匹配）

```python
# ---- 本段讲：match/case 结构化模式匹配（Python 3.10+）----
# 对标 Java 21 的 switch 模式匹配：case 后可以直接写"结构"，不只是常量。
def handle(cmd):
    match cmd:                   # 类似 switch(cmd)，但用冒号而不是大括号
        case "start":            # 字面量匹配，等价 case "start" ->
            return "启动"
        case "stop" | "halt":        # 多值
            return "停止"
        case {"action": action}:     # 匹配字典结构
            return f"动作 {action}"
        case _:                  # 通配符兜底，等价 default
            return "未知"
```

日常更常用字典分发（性能更好，也更灵活）：

```python
# ---- 本段讲：用字典 + lambda 做"命令分发"，比 match 更常用 ----
# dict 的值可以是函数；get(key, default) 找不到时返回默认函数，不会抛 KeyError。
handlers = {
    "start": lambda: "启动",      # 每个值是一个无参函数，等价 Supplier<String>
    "stop": lambda: "停止",
}
handlers.get(cmd, lambda: "未知")()   # 先取函数，再加 () 立即调用；cmd 来自上下文（片段省略了定义）
```

---

## 五、集合类型总览（详见 03 篇）

```python
# ---- 本段讲：四种内置容器的字面量写法与 Java 对应关系 ----
# 都是语言级字面量，不需要 new 具体实现类；类型由括号形状区分。
lst = [1, 2, 3]                  # list       → ArrayList
tup = (1, 2, 3)                  # tuple      → 不可变 list（无直接对应）
st  = {1, 2, 3}                  # set        → HashSet
dct = {"a": 1}                   # dict       → LinkedHashMap（3.7+ 保证插入序）
```

```python
# ---- 本段讲：list / dict 常用操作速查（每行都是"表达式 → 等价 Java API"）----
# 注意区分"原地修改"（append/sort/reverse）与"返回新对象"（sorted）。
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
# ---- 本段讲：函数定义 + 三种调用方式 ----
# 注解只做提示；默认参数让"少传参数"成为常态，不必像 Java 那样写重载。
def greet(name: str, greeting: str = "你好") -> str:   # greeting 有默认值，可省略
    return f"{greeting}，{name}"                       # 返回拼好的字符串

greet("张三")                    # 位置参数
greet("张三", "早上好")           # 位置参数
greet(name="张三", greeting="嗨")  # 关键字参数（Java 没有）
greet(greeting="嗨", name="张三")  # 顺序无关
```

Python 函数的特点（Java 都没有）：

```python
# ---- 本段讲：Python 函数参数的六种特有能力 ----
# 这一段多是签名演示，函数体用 ...（省略号）占位，不是完整可运行逻辑。
# 1. 默认参数
def f(a, b=2): ...          # b 可不传；默认值在 def 时求值一次（可变默认值是坑）

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
def f4(a, *, b, c): ...     # 单独的 * 是分隔符，不是参数名
f4(1, b=2, c=3)         # ✅
# f4(1, 2, 3)           # ❌ TypeError

# 6. 函数是一等公民
def apply(fn, x):       # 函数可以作为参数传入
    return fn(x)
apply(lambda v: v * 2, 5)    # 10
```

> 详细展开见 [04 篇](04-函数与作用域.md)。

---

## 七、类（详见 05 篇）

```python
# ---- 本段讲：类定义、实例属性与魔术方法 ----
# Python 没有 private/public，也没有构造函数重载；self 是显式写出的 this。
# __init__ 负责初始化实例，__str__/__repr__ 分别对应 toString() 和调试显示。
class Order:
    # 类属性（所有实例共享，类似 Java static）
    counter = 0

    def __init__(self, order_no: str, amount: float):   # 构造/初始化方法，第一个参数必须是 self
        # 实例属性（类似 Java 字段，但必须在 __init__ 里赋初值）
        self.order_no = order_no        # 绑定到当前实例；不写 self 就成了局部变量
        self.amount = amount
        Order.counter += 1              # 通过类名改类属性，等价 static 计数器自增

    def is_large(self) -> bool:         # 普通实例方法，第一个参数同样是 self
        return self.amount > 10_000     # 下划线只是数字分隔符，值为 10000

    def __str__(self) -> str:          # 等价于 toString()
        return f"Order({self.order_no}, {self.amount})"

    def __repr__(self) -> str:         # 调试/REPL 显示
        return f"Order(order_no={self.order_no!r}, amount={self.amount!r})"


o = Order("NO001", 20000)          # 创建实例时自动调用 __init__，不需要 new 关键字
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
# ---- 本段讲：try / except / else / finally 的完整结构 ----
# except 可以写多个分支，从上到下匹配；as e 绑定异常对象（对应 Java 的 catch (X e)）。
# else 是 Python 独有：只有 try 体没抛异常才执行，适合放"成功后的后续逻辑"。
try:
    result = 10 / 0                     # 整数除以 0，抛 ZeroDivisionError
except ZeroDivisionError as e:          # 精确捕获除零
    print(f"除零：{e}")
except (TypeError, ValueError) as e:          # 捕获多种
    print(f"类型或值错误：{e}")
except Exception as e:                         # 兜底（相当于 catch (Exception)）
    print(f"未预期：{e}")
    raise                                      # 重新抛出（= throw e）
else:
    print("没异常才会执行")                     # Java 没有
finally:
    print("总会执行")                           # 无论成功失败都会跑，用来释放资源
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
# ---- 本段讲：自定义异常——继承 Exception 并加业务字段 ----
# Python 没有 checked exception，不需要 throws 声明；继承 Exception 即可用 raise 抛出。
class BusinessException(Exception):          # 继承 Exception
    def __init__(self, code: str, message: str):
        super().__init__(message)            # 调父类构造，把 message 存进异常信息（= super(message)）
        self.code = code                     # 额外加一个错误码字段


class OrderNotFoundException(BusinessException):
    def __init__(self, order_no: str):
        super().__init__("ORDER_NOT_FOUND", f"订单不存在：{order_no}")   # 固定错误码，消息由参数拼接
        self.order_no = order_no             # 保留订单号，方便上层日志排查
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

### 1. 链式比较

```python
# ---- 本段讲：链式比较，一个表达式完成多段区间判断 ----
# Python 会把 a < b < c 解释成 (a < b) and (b < c)，中间的 b 只求值一次。
if 0 <= x < 100:              # Java 里必须写 0 <= x && x < 100
    pass                      # pass 是空语句，占位用（Java 用空的大括号）

# 更灵活的链式
if a < b < c:                 # a < b 且 b < c
    pass
if x == y == z:               # 三个值相等
    pass
if 0 <= x <= y <= 100:        # 多段链式
    pass
```

### 2. 海象运算符（3.8+）：赋值 + 判断

```python
# ---- 本段讲：海象运算符 := ——"表达式里做赋值" ----
# 好处是同一个值只计算一次，且赋值结果能立刻参与判断，不用写两遍表达式。
# 注意：在 if/while 的条件里必须用括号包住，否则是语法错误。
# 基本用法
if (n := len(data)) > 10:
    print(f"太长：{n}")
# ① 计算 len(data) 并赋值给变量 n
# ② 判断 n > 10 是否成立
# ③ 如果成立，执行 print，此时可以直接使用 n

# 典型场景：while 循环
while (line := input(">>> ")).strip() != "quit":   # 读一行 → 去空格 → 判断，一行完成
    print(f"你输入了: {line}")

# 典型场景：列表推导式中过滤
orders = [15, 8, 25, 3]
large_doubled = [doubled for o in orders if (doubled := o * 2) > 20]   # 先算并绑定 doubled，再判断是否 >20
# 结果: [30, 50]
```

⚠️ **注意**：必须加括号 `(n := len(data))`，不要滥用以免降低可读性。

### 3. 解包（Unpacking）

```python
# ---- 本段讲：解包——把序列/字典按结构拆给多个变量 ----
# 带 * 的变量吸收"剩余部分"并得到 list；** 用于字典合并（键重复时后者覆盖前者）。
# 基本解包
a, b, *rest = [1, 2, 3, 4]     # a=1, b=2, rest=[3,4]
first, *_, last = [1,2,3,4]    # first=1, last=4, _=[2,3]（丢弃）

# 函数参数解包
def process(a, b, c):
    pass                       # 空函数体，只演示调用形式
args = [1, 2, 3]
process(*args)                 # 等价于 process(1, 2, 3)

# 字典解包（Python 3.5+）
defaults = {"color": "blue", "size": "M"}
custom = {"size": "L", "brand": "Nike"}
merged = {**defaults, **custom}  # {'color': 'blue', 'size': 'L', 'brand': 'Nike'}
```

**Java 对比**：Java 没有解包语法，需要手动访问数组/集合元素。

### 4. 条件赋值 + `or` 短路默认值

```python
# ---- 本段讲：用 or 做"默认值"（短路求值）----
# a or b：a 为真就返回 a，否则返回 b；等价 Java 的三元 + Optional.orElse，但会误伤 0/""。
name = user_input or "匿名"
# 如果 user_input 是 ""、None、0、[]、{} 等"假值"，都会变成 "匿名"

# 链式 or（优先级依次降低）
port = env_port or config_port or 8080        # 从左到右，第一个"真值"胜出

# ⚠️ 注意：如果 0 是有效值，不要用 or
count = user_count or 10  # ❌ 如果 user_count=0，会变成 10
count = user_count if user_count is not None else 10  # ✅ 显式判 None，保留 0
```

### 5. 交换变量

```python
# ---- 本段讲：一行交换两个变量，以及它为什么能成立 ----
# 关键在于等号右边先"整体求值"成一个元组，再解包赋给左边，所以右边的旧值不会被覆盖。
a, b = b, a  # 无需临时变量

# 底层原理：
# ① 右边先打包成元组 (b, a)
# ② 左边解包赋值
# 等价于：a, b = (b, a)
```

**Java 对比**：
```java
int temp = a;  // 需要临时变量
a = b;
b = temp;
```

### 6. 生成器表达式（惰性求值，省内存）

```python
# ---- 本段讲：生成器表达式——惰性求值，逐个产出、不占内存 ----
# 圆括号 () 得到生成器，方括号 [] 得到列表；前者只算"怎么算"，后者立刻算完并全部存下。
total = sum(x * x for x in range(1_000_000))
# 不会一次性创建 100 万个元素的列表，省内存

# 对比列表推导式
squares_list = [x * x for x in range(1_000_000)]  # [] 立即生成所有元素
squares_gen = (x * x for x in range(1_000_000))   # () 按需生成

# 使用场景
for sq in squares_gen:  # 每次只计算一个
    if sq > 100:
        break           # 提前退出，后面 99 万多个元素根本不会被计算
```

**Java 对比**：类似 Java Stream API，但 Python 语法更轻量。

### 7. 任何可迭代对象都能 `for`

```python
# ---- 本段讲：任何可迭代对象都能 for，包括文件对象 ----
# 文件对象天然可迭代，每次产出一行，适合大文件（不会一次读进内存）。
# 逐行读文件（不会一次性加载整个文件）
for line in open("a.txt", encoding="utf-8"):   # 但这样拿不到关闭时机，不推荐
    print(line.strip())                        # 行尾自带 \n，strip 去掉

# ✅ 推荐写法（自动关闭文件）
with open("a.txt", encoding="utf-8") as f:     # with 结束自动 close，等价 try-with-resources
    for line in f:
        print(line.strip())

# 可迭代对象包括（示意，不能直接运行）：
#   列表 [1, 2, 3]
#   字符串 "hello"
#   字典的键 {"a": 1, "b": 2}
#   集合 {1, 2, 3}
#   range 对象 range(10)
# 实际使用时直接写 for x in 对象: 即可。
```

### 8. 下划线数字分隔符

```python
# ---- 本段讲：数字里的下划线分隔符（仅提升可读性）----
# 解释器会直接忽略下划线，所以 1_000_000 与 1000000 完全等价。
amount = 1_000_000              # 与 Java 7 相同
binary = 0b1010_0101            # 二进制
hex_val = 0xFF_EC_DE_5E         # 十六进制
float_num = 3.141_592_653       # 浮点数

# 仅提高可读性，不影响值
assert 1_000_000 == 1000000     # True
```

### 9. 一行读写文件

```python
# ---- 本段讲：一行读写文件的三种方式，重点是"谁负责关文件" ----
# 不关闭文件会占用句柄，Windows 上还可能导致文件被锁住删不掉。
# ❌ 不推荐：文件不会立即关闭（依赖 GC）
content = open("a.txt", encoding="utf-8").read()   # 读完没有 close()，句柄何时释放不确定

# ✅ 推荐方式 1：with 语句（自动关闭）
with open("a.txt", encoding="utf-8") as f:         # 退出 with 块时自动调用 f.close()
    content = f.read()                              # 一次性读全部内容

# ✅ 推荐方式 2：pathlib（Python 3.5+，更简洁）
from pathlib import Path                            # 面向对象的路径库，等价 java.nio.file.Path
content = Path("a.txt").read_text(encoding="utf-8")   # 一行读完且自动关闭
Path("output.txt").write_text("Hello", encoding="utf-8")   # 一行写文件，不存在则创建
```

详见 [06 篇](06-异常、文件与上下文管理器.md)。

### 10. 用 `in` 做成员判断

```python
# ---- 本段讲：用 in 做成员判断，以及 list 与 set 的复杂度差异 ----
# list 是线性扫描 O(n)，set/dict 是哈希查找 O(1)；数据量大时差别巨大。
# list - O(n) 时间复杂度
if key in [1, 2, 3, 4, 5]:
    pass

# set/dict - O(1) 时间复杂度（推荐）
if key in my_set:            # 原示例：my_set 是 set，查找是哈希定位
    pass
if key in {1, 2, 3, 4, 5}:   # 等价的字面量写法
    pass
if key in {"a": 1, "b": 2}:  # 判断键是否存在
    pass
```

**性能对比**：
```python
# ---- 本段讲：实测"list 查找 vs set 查找"的耗时差距 ----
# 这里只记录起点时间、不打印差值，是示意性代码；真实测量应写 end - start。
import time                       # 标准库计时模块，time.time() 返回当前时间戳（秒）

# 列表查找（慢）
big_list = list(range(10_000_000))   # 造 1000 万个元素的 list，需要较多内存
start = time.time()                  # 记录起点（后面没用到这个变量，仅示意）
# 下面是"线性扫描"：要逐个比对，最坏情况扫到最后一个才命中
9_999_999 in big_list  # ~0.1 秒

# 集合查找（快 100 万倍）
big_set = set(range(10_000_000))     # 转成哈希表，构建本身要花时间和内存
start = time.time()                  # 重新记录起点，覆盖上面的值
# 下面是"哈希定位"：直接算出桶位置，耗时基本与元素个数无关
9_999_999 in big_set  # ~0.0000001 秒
```

**最佳实践**：频繁查找用 `set`，保持顺序用 `list`。

**Java 对比**：
```java
// Java 用 contains()
if (list.contains(key)) { }
if (set.contains(key)) { }
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
# ---- 本段讲：5 道自测题的"题面"（只有输入和期望输出，答案留空）----
# 这不是完整程序，而是一组练习脚手架：每段给出已知数据，要求你补出实现。
# 参考答案在 code/01_basics/09_self_test.py，先自己写再看。
# ① 给定列表，返回所有偶数的平方
nums = [1, 2, 3, 4, 5, 6]     # 输入数据
# 期望 [4, 16, 36]
# 提示：列表推导式 + if 过滤（x*x for x in nums if x % 2 == 0）


# ② 统计字符串中每个字符出现的次数
text = "hello"                # 输入数据
# 期望 {'h': 1, 'e': 1, 'l': 2, 'o': 1}
# 提示：out[ch] = out.get(ch, 0) + 1，或直接用 collections.Counter


# ③ 把两个列表合成字典
keys = ["a", "b"]             # 键列表
vals = [1, 2]                 # 值列表
# 期望 {'a': 1, 'b': 2}
# 提示：zip 按位置配对后交给 dict()


# ④ 一个函数，接收任意数量参数，返回最大值，空则返回 None
# 提示：def q4(*nums)，再用三元表达式判空（空元组是假值）


# ⑤ 安全地取字典深层值：get_nested(d, "a.b.c", default=0)
d = {"a": {"b": {"c": 42}}}   # 嵌套字典输入
# 期望 42
# 提示：按 "." 切分路径，逐层判断"当前是不是 dict / 键在不在"
```

参考答案在 [`code/01_basics/09_self_test.py`](code/01_basics/09_self_test.py)。

---

**上一篇**：[01-环境搭建与工具链](01-环境搭建与工具链.md)　**下一篇**：[03-数据类型与容器](03-数据类型与容器.md)
