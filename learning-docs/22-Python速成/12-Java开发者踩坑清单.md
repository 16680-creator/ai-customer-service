# 12 · Java 开发者踩坑清单（50 条）

> 每条给：**现象 / 原因 / 正确做法**。
> 标注 `🔴` 表示一定会踩（写出 bug）、`🟡` 表示大概率踩（写法别扭）、`🟢` 表示知道就好。
> 建议用法：写代码时遇到奇怪行为，回来扫一遍这个清单。

---

## A. 语法与类型（1-10）

### 1. 🔴 整数除法变成浮点

```python
print(100 / 3)      # 33.333333333333336   ← 不是 33
print(100 // 3)     # 33
```

**原因**：`/` 是真除法。Java 里 `int / int` 是整除。
**正确做法**：要整除用 `//`；金额计算用 `Decimal`。

```python
# 顺带：负数整除是向下取整，不是向零取整
print(-7 // 2)      # -4     Java 是 -3
print(-7 % 2)       # 1      Java 是 -1
```

### 2. 🔴 `i++` 不存在

```python
i = 0
i++          # SyntaxError: invalid syntax
i += 1       # ✅
```

### 3. 🔴 缩进错误 = 逻辑错误（而且不报错）

```python
def f(items):
    total = 0
    for x in items:
        total += x
    return total        # ✅ 正确缩进

# 下面这段不报错，但逻辑错了
def g(items):
    total = 0
    for x in items:
        total += x
        return total    # ❌ return 在循环里，只执行一次
```

**建议**：配置编辑器显示缩进参考线 + format on save（ruff/black）。**绝不混用 Tab 和空格**。

### 4. 🔴 可变默认参数

```python
def add(item, lst=[]):       # ❌ 默认值只创建一次
    lst.append(item)
    return lst

print(add(1), add(2))        # [1, 2] [1, 2]

def add(item, lst=None):     # ✅
    if lst is None:
        lst = []
    lst.append(item)
    return lst
```

### 5. 🔴 `== None` 而不是 `is None`（以及小整数驻留）

```python
x = None
if x == None:      # ⚠️ 能跑但不对（如果对象重载了 __eq__ 会出错）
if x is None:      # ✅

# 更隐蔽的坑：str/int 的驻留与 == 无关，但 is 行为诡异
a = 256
b = 256
print(a is b)      # True（小整数驻留）
a = 257
b = 257
print(a is b)      # False（可能，取决于实现）

# 结论：is 只用于 None / True / False / 对象身份比较
# 值比较一律 ==
```

### 6. 🟡 `+=` 是原地修改，`+` 创建新对象

```python
a = [1, 2]
b = a
b += [3]
print(a)        # [1, 2, 3]   ← a 也变了

a = [1, 2]
b = a
b = b + [3]
print(a)        # [1, 2]      ← a 不变
```

**差异原因**：list 实现了 `__iadd__`（原地 extend），而 `+` 调用 `__add__`（新建）。**在函数参数上这个差异会引发 bug**。

### 7. 🔴 二维列表的乘法复制

```python
grid = [[0] * 3] * 3        # ❌ 三行是同一个 list 对象
grid[0][0] = 9
print(grid)                 # [[9,0,0],[9,0,0],[9,0,0]]

grid = [[0] * 3 for _ in range(3)]     # ✅
```

### 8. 🔴 浅拷贝 vs 深拷贝

```python
import copy

nested = [[1, 2], [3, 4]]
shallow = nested.copy()        # 只复制外层
shallow[0][0] = 99
print(nested)                  # [[99,2],[3,4]]  ← 被改了！

deep = copy.deepcopy(nested)   # ✅
```

### 9. 🔴 作用域：函数里赋值导致 UnboundLocalError

```python
count = 0

def inc():
    print(count)      # ❌ UnboundLocalError
    count += 1        # 这行让 count 成为局部变量

def inc_fixed():
    global count      # ✅（但更好的是不用 global）
    count += 1
```

**Java 对照**：Java 里这会是**编译错误**，Python 在运行时才炸，更难发现。

### 10. 🟡 字符串不可变（方法返回新串）

```python
s = "hello"
s.upper()             # 返回值被丢弃
print(s)              # 'hello'   ← 没变

s = s.upper()         # ✅
```

**Java 里 String 也是不可变**，但 Python 的 `list.sort()` 是原地、`sorted()` 是新对象，两者混着更容易晕：

```python
nums.sort()           # 原地，返回 None
new = sorted(nums)    # 新列表
# new = nums.sort()   # ❌ new 是 None
```

---

## B. 容器（11-20）

### 11. 🔴 `{}` 是空字典不是空集合

```python
type({})          # <class 'dict'>
type(set())       # <class 'set'>
```

### 12. 🔴 `dict[key]` 不存在会抛 KeyError

```python
d = {}
d["a"]                    # ❌ KeyError
d.get("a")                # None
d.get("a", 0)             # 0
```

Java 的 `Map.get` 返回 null，Python 的 `dict[]` 直接炸。**养成一律用 `.get()` 的习惯**。

### 13. 🔴 `or` 做默认值会吞掉 0/""/False

```python
d = {"count": 0}
d.get("count") or 100        # 100    ❌ 期望 0
d.get("count", 100)          # 0      ✅

name = "" or "匿名"           # "匿名"  （这里是对的，但要知道为什么）
```

### 14. 🔴 遍历 dict/list 时修改

```python
d = {"a": 1, "b": 2}
for k in d:
    del d[k]          # ❌ RuntimeError: dictionary changed size during iteration

for k in list(d.keys()):    # ✅ 先复制键
    del d[k]
```

### 15. 🟡 `list.remove` 按值删，值不存在抛异常

```python
nums = [1, 2, 3]
nums.remove(9)         # ❌ ValueError

# 安全写法
if 9 in nums:
    nums.remove(9)
# 或
try:
    nums.remove(9)
except ValueError:
    pass
```

### 16. 🔴 `list.pop(0)` 是 O(n)

```python
from collections import deque

# ❌ 循环里用 list.pop(0) 做队列，复杂度 O(n²)
# ✅
q = deque()
q.append(1)
q.popleft()      # O(1)
```

### 17. 🔴 `x in list` 是 O(n)

```python
# 数据量大时
big = list(range(1_000_000))
999_999 in big          # 慢

big_set = set(big)
999_999 in big_set      # 快，O(1)
```

### 18. 🟡 `itertools.groupby` 必须先排序

```python
import itertools

data = [("a", 1), ("b", 2), ("a", 3)]
for k, g in itertools.groupby(data, key=lambda x: x[0]):
    print(k, list(g))
# a [(a,1)]
# b [(b,2)]
# a [(a,3)]     ← 同一个 key 被分成两组！

# ✅ 先排序
for k, g in itertools.groupby(sorted(data, key=lambda x: x[0]), key=lambda x: x[0]):
    ...
```

Java 的 `Collectors.groupingBy` 没有这个限制。

### 19. 🟡 排序时混用 int 和 str

```python
sorted([1, "2", 3])       # ❌ TypeError: '<' not supported between 'str' and 'int'
```

Python 3 不支持跨类型比较（Java 里也不会，但 Java 编译期就拦住了）。

### 20. 🟡 `tuple` 单元素必须带逗号

```python
t = (1)      # ❌ 这是 int
t = (1,)     # ✅
```

---

## C. 函数与类（21-30）

### 21. 🔴 `self` 忘写 / 忘传

```python
class A:
    def method():          # ❌ 少 self
        pass

A().method()               # TypeError: method() takes 0 positional arguments but 1 was given
```

### 22. 🔴 子类忘调 `super().__init__()`

```python
class Parent:
    def __init__(self, name):
        self.name = name

class Child(Parent):
    def __init__(self, name, age):
        self.age = age        # ❌ 忘调 super，self.name 不存在
        # ✅ super().__init__(name)

c = Child("x", 1)
print(c.name)                # AttributeError
```

**Java 会自动调 super()，Python 不会，而且不报错（直到你访问那个字段）。**

### 23. 🔴 类属性写可变对象

```python
class Bad:
    items = []            # ❌ 所有实例共享

class Good:
    def __init__(self):
        self.items = []
```

### 24. 🔴 `__eq__` 定义了却没定义 `__hash__`

```python
class Order:
    def __init__(self, no): self.no = no
    def __eq__(self, o): return self.no == o.no

{Order("A")}      # ❌ TypeError: unhashable type: 'Order'
```

**原因**：定义了 `__eq__` 后 Python 自动把 `__hash__` 设为 None。
**正确做法**：同时定义 `__hash__`，或用 `@dataclass(frozen=True)`。

### 25. 🔴 闭包延迟绑定（循环里的 lambda）

```python
funcs = [lambda: i for i in range(3)]
print([f() for f in funcs])       # [2, 2, 2]   ❌ 期望 [0,1,2]

funcs = [lambda i=i: i for i in range(3)]
print([f() for f in funcs])       # [0, 1, 2]   ✅
```

### 26. 🔴 装饰器忘加 `functools.wraps`

```python
import functools

def timer(fn):
    def wrapper(*args, **kwargs):
        return fn(*args, **kwargs)
    return wrapper

@timer
def fetch():
    """拉取数据"""

print(fetch.__name__)     # wrapper   ❌ 名字被覆盖
print(fetch.__doc__)      # None      ❌ 文档丢了

# ✅ 加 @functools.wraps(fn)
```

**后果**：pytest 收集不到测试、FastAPI 拿不到参数签名、日志名字全错。

### 27. 🟡 lambda 只能写单个表达式

```python
f = lambda x: (a := x * 2, a + 1)[1]     # 能用但别这么写
# 需要多语句就写 def
```

### 28. 🔴 生成器只能遍历一次

```python
g = (x for x in range(3))
print(sum(g))     # 3
print(sum(g))     # 0    ← 已耗尽
```

### 29. 🟡 递归深度只有 ~1000

```python
def fact(n):
    return 1 if n <= 1 else n * fact(n - 1)

fact(2000)        # RecursionError
```

没有尾递归优化，深度递归必须改迭代。

### 30. 🟡 `@property` 与同名字段冲突

```python
class A:
    @property
    def x(self): return self._x

    def __init__(self):
        self.x = 1        # ❌ AttributeError: can't set attribute（没有 setter）
        self._x = 1       # ✅
```

---

## D. 并发（31-36）（**最容易出生产事故**）

### 31. 🔴 多线程跑 CPU 密集任务没有加速

```python
# GIL 导致同一时刻只有一个线程执行 Python 字节码
# CPU 密集 → 用 multiprocessing.ProcessPoolExecutor
```

实测：`python code/05_concurrency/01_gil_proof.py`

### 32. 🔴 `async def` 里调同步阻塞函数

```python
import time, requests

async def bad():
    requests.get(url)      # ❌ 卡死整个事件循环，所有并发请求排队
    time.sleep(1)          # ❌ 同上

async def good():
    await asyncio.to_thread(requests.get, url)     # ✅
    await asyncio.sleep(1)                          # ✅
```

**这是 FastAPI 第一大坑**，且症状隐蔽：单个请求测试没问题，一并发就全卡。

### 33. 🔴 忘了 `if __name__ == "__main__":`（Windows 多进程）

```python
from concurrent.futures import ProcessPoolExecutor

def work(x): return x * 2

# ❌ 顶层直接执行，Windows 上子进程重新 import 模块 → 无限递归创建进程
with ProcessPoolExecutor() as pool:
    pool.map(work, range(10))

# ✅
if __name__ == "__main__":
    with ProcessPoolExecutor() as pool:
        pool.map(work, range(10))
```

### 34. 🟡 多进程传不可 pickle 的对象

```python
pool.map(lambda x: x * 2, data)     # ❌ Can't pickle local object
# ✅ 用模块级函数
```

连接对象、锁、生成器、lambda、局部函数都不能跨进程传。

### 35. 🟡 GIL 不等于线程安全

```python
counter = 0
def inc():
    global counter
    for _ in range(100_000):
        counter += 1      # ❌ 读-改-写三步，非原子

# ✅ 必须加锁
lock = threading.Lock()
def inc_safe():
    global counter
    for _ in range(100_000):
        with lock:
            counter += 1
```

### 36. 🔴 `asyncio.run()` 不能在已有事件循环里调

```python
async def outer():
    asyncio.run(inner())      # ❌ RuntimeError: asyncio.run() cannot be called from a running event loop
    await inner()             # ✅ 直接 await
```

---

## E. 文件与编码（37-42）

### 37. 🔴🔴 不指定 encoding（Windows 必炸）

```python
open("a.txt").read()                            # ❌ Windows 默认 GBK
open("a.txt", encoding="utf-8").read()          # ✅
Path("a.txt").read_text(encoding="utf-8")       # ✅

# 给 Excel 看的中文 CSV 要用 utf-8-sig
open("a.csv", "w", encoding="utf-8-sig", newline="")
```

**这条是本清单里最容易浪费半天的坑**。跟 Java 的 `FileReader` 平台编码问题一模一样。

### 38. 🔴 CSV 写入不加 `newline=""`

```python
# ❌ Windows 上每行之间多一个空行
with open("a.csv", "w", encoding="utf-8") as f:
    csv.writer(f).writerow([1, 2])

# ✅
with open("a.csv", "w", encoding="utf-8", newline="") as f:
```

### 39. 🔴 `json.dumps` 默认转义中文

```python
json.dumps({"名": "中文"})                       # '{"\\u540d": "\\u4e2d\\u6587"}'
json.dumps({"名": "中文"}, ensure_ascii=False)   # '{"名": "中文"}'
```

### 40. 🟡 `"w"` 模式静默清空文件

```python
open("important.txt", "w").close()      # 内容全没了，不报错

# 想"存在就报错"用 "x"，想追加用 "a"
open("important.txt", "x")
open("log.txt", "a")
```

### 41. 🟡 相对路径的基准不是你以为的

```python
# 基准是"当前工作目录"，不是"脚本所在目录"
# 在 A 目录执行 python /path/to/script.py，相对路径基于 A

# ✅ 用脚本位置构造绝对路径
from pathlib import Path
BASE = Path(__file__).resolve().parent
config = BASE / "config.json"
```

**Java 里 `getResourceAsStream` 是基于 classpath，Python 没有这个东西**，路径全靠 `__file__` 自己算。

### 42. 🔴 用 float 算钱

```python
print(0.1 + 0.2)              # 0.30000000000000004
print(0.1 + 0.2 == 0.3)       # False

from decimal import Decimal
Decimal("0.1") + Decimal("0.2")     # Decimal('0.3')
Decimal(0.1)                        # ❌ Decimal('0.1000000000000000055...')
Decimal("0.1")                      # ✅ 永远用字符串构造
```

**项目里算订单金额、手续费，必须用 `Decimal`**——和 Java 用 `BigDecimal` 同理。

---

## F. 模块与工程（43-47）

### 43. 🔴 `python script.py` 在包内会 import 失败

```bash
python app/main.py         # ❌ ModuleNotFoundError: No module named 'app'
python -m app.main         # ✅ 从项目根执行
```

原因：`sys.path[0]` 是脚本目录，不是项目根。

### 44. 🔴 装到全局环境

```bash
pip install fastapi              # ❌ 可能装到系统 Python
python -m pip install fastapi    # ✅ 一定装到当前解释器

# 更根本的解决：先建 venv 并激活
```

### 45. 🟡 循环导入

```python
# a.py: from b import B
# b.py: from a import A     → ImportError

# 解法：函数内延迟导入 / import 模块而非名字 / 抽公共模块
```

### 46. 🔴 提交了 `.env`

`.env` 含密钥。提交 `.env.example`（占位值），`.env` 进 `.gitignore`。
**一旦提交过，密钥就等于泄露了**（git 历史里永远存在），必须立刻轮换。

### 47. 🟡 用 `import *`

```python
from module import *      # ❌ 污染命名空间，看不出名字来源，覆盖内置函数
```

---

## G. 其他高频（48-50）

### 48. 🔴 `assert` 用于业务校验

```bash
python -O script.py     # assert 全部失效！
```

```python
def withdraw(amount):
    assert amount > 0, "金额必须为正"    # ❌ -O 模式下不生效
    if amount <= 0:
        raise ValueError("金额必须为正")  # ✅
```

### 49. 🔴 SQL 拼接字符串

```python
cur.execute(f"SELECT * FROM t WHERE name = '{user_input}'")    # ❌ SQL 注入
cur.execute("SELECT * FROM t WHERE name = ?", (user_input,))   # ✅
```

Java 里用 PreparedStatement 是常识，Python 里手写字符串拼接的代码仍然很多。

### 50. 🟡 用 `print` 打日志

```python
print(f"order={order}")        # ❌ 无法分级、无法关闭、无时间戳、同步阻塞

import logging
logger = logging.getLogger(__name__)
logger.info("order=%s", order)  # ✅ 惰性格式化，级别可控
```

---

## 附：按"踩过几次"排序的前 8 条

如果只有时间记 8 条，记这些（实战中最常见）：

| 排名 | 条目 | 典型症状 |
|---|---|---|
| 1 | 不指定 `encoding="utf-8"` | Windows 上 `UnicodeDecodeError` |
| 2 | `async def` 里调同步阻塞 | 并发一上来整个服务卡死 |
| 3 | 多线程跑 CPU 密集 | "为什么没变快" |
| 4 | 可变默认参数 `def f(x=[])` | 两次调用结果累积 |
| 5 | `dict[key]` vs `.get(key)` | 随机 `KeyError` |
| 6 | 浅拷贝当深拷贝用 | 改了 A 影响 B |
| 7 | `json.dumps` 没加 `ensure_ascii=False` | 中文变 `\uXXXX` |
| 8 | `python x.py` vs `python -m x` | `ModuleNotFoundError: No module named 'app'` |

---

## 自测方式

```bash
cd learning-docs/22-Python速成/code
python 06_exercises/traps_demo.py
```

这个脚本会把上面 20 个坑逐个演示"错误写法 → 正确写法"，跑一遍比读十遍有效。

---

**上一篇**：[11-FastAPI实战-对着SpringBoot学](11-FastAPI实战-对着SpringBoot学.md)　**下一篇**：[13-速查表](13-速查表.md)
