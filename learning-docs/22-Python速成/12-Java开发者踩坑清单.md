# 12 · Java 开发者踩坑清单（50 条）

> 每条给：**现象 / 原因 / 正确做法**。
> 标注 `🔴` 表示一定会踩（写出 bug）、`🟡` 表示大概率踩（写法别扭）、`🟢` 表示知道就好。
> 建议用法：写代码时遇到奇怪行为，回来扫一遍这个清单。

---

## A. 语法与类型（1-10）

### 1. 🔴 整数除法变成浮点

```python
# ---- 本段讲：/ 是真除法（结果一定是 float），// 才是整除 ----
# Java 里 int / int 会自动截断；Python 3 的 / 永远返回 float，这是 Java 开发者最常写错的第一条。
print(100 / 3)      # 33.333333333333336   ← 不是 33
print(100 // 3)     # 33
```

**原因**：`/` 是真除法。Java 里 `int / int` 是整除。
**正确做法**：要整除用 `//`；金额计算用 `Decimal`。

```python
# ---- 本段讲：负数整除/取模的方向与 Java 不同 ----
# Python 的 // 是「向下取整（floor）」，Java 的 / 是「向零取整（truncate）」，负数时结果差 1。
# 顺带：负数整除是向下取整，不是向零取整
print(-7 // 2)      # -4     Java 是 -3
print(-7 % 2)       # 1      Java 是 -1；Python 保证 (a // b) * b + (a % b) == a
```

### 2. 🔴 `i++` 不存在

```python
# ---- 本段讲：Python 没有 ++ / -- 自增自减运算符 ----
# Java 的 i++ 是表达式；Python 里 ++ 会被当成两个一元加号，但赋值语句 i++ 本身语法非法。
i = 0
i++          # SyntaxError: invalid syntax   ← 整行无法解析，运行前就报错
i += 1       # ✅ 自增只能写复合赋值；同理自减用 i -= 1
```

### 3. 🔴 缩进错误 = 逻辑错误（而且不报错）

```python
# ---- 本段讲：缩进就是代码块边界，缩进错了不会报语法错但逻辑会错 ----
# Java 用 {} 划分块、缩进只是给人看；Python 用缩进划分块，同一块必须严格对齐。
def f(items):
    total = 0          # 累加器初值；赋值即定义变量（Java 要写 int total = 0;）
    for x in items:    # 冒号结尾，下一行缩进 4 空格表示进入循环体
        total += x     # 循环体：每次把 x 累加进 total
    return total        # ✅ 正确缩进；与 for 同级，循环结束后才执行一次

# 下面这段不报错，但逻辑错了
def g(items):
    total = 0
    for x in items:
        total += x
        return total    # ❌ return 在循环里，只执行一次；因为缩进比 for 深一层，它属于循环体
```

**建议**：配置编辑器显示缩进参考线 + format on save（ruff/black）。**绝不混用 Tab 和空格**。

### 4. 🔴 可变默认参数

```python
# ---- 本段讲：可变对象当默认参数会被所有调用共享 ----
# 默认值在「函数定义时」求值一次并挂在函数对象上，之后每次调用都复用同一个 list。
def add(item, lst=[]):       # ❌ 默认值只创建一次
    lst.append(item)         # 往那个共享的 list 追加，历史调用结果会累积
    return lst               # 返回的就是同一个 list 对象

print(add(1), add(2))        # [1, 2] [1, 2]  第一次打印时两个引用指向同一对象，所以都显示 [1,2]

def add(item, lst=None):     # ✅
    if lst is None:          # 用 None 当哨兵：调用方没传才新建
        lst = []
    lst.append(item)         # 每次调用都是全新的 list，不会互相污染
    return lst
```

### 5. 🔴 `== None` 而不是 `is None`（以及小整数驻留）

```python
# ---- 本段讲：判 None 用 is 不用 ==；is 比较身份、== 比较值 ----
# is 判断「是不是同一个对象」（Java 的 ==）；== 判断「值是否相等」（Java 的 equals）。
# 注意下面两个 if 都没有函数体，仅用于演示写法，真实代码必须补上缩进的语句。
x = None
if x == None:      # ⚠️ 能跑但不对（如果对象重载了 __eq__ 会出错）
if x is None:      # ✅ 语义明确，且不受 __eq__ 重载影响，速度也更快

# 更隐蔽的坑：str/int 的驻留与 == 无关，但 is 行为诡异
# 小整数（-5~256）和短字符串会被解释器缓存复用，所以 is 可能碰巧为 True，但不能依赖。
a = 256
b = 256
print(a is b)      # True（小整数驻留）
a = 257
b = 257
print(a is b)      # False（可能，取决于实现）  ← 交互式逐行执行和同文件编译结果都可能不同

# 结论：is 只用于 None / True / False / 对象身份比较
# 值比较一律 ==
```

### 6. 🟡 `+=` 是原地修改，`+` 创建新对象

```python
# ---- 本段讲：+= 对 list 是原地修改，+ 是新建列表 ----
# list 实现了 __iadd__（内部调 extend，直接改自己）；+ 调 __add__，返回一个全新 list。
a = [1, 2]
b = a           # b 和 a 指向同一个 list 对象（不是复制！）
b += [3]        # 原地扩展，a 看到的内容也跟着变
print(a)        # [1, 2, 3]   ← a 也变了

a = [1, 2]
b = a           # 同样先让 b 指向 a
b = b + [3]     # + 生成新 list 再赋给 b，b 改指向新对象，a 仍指原对象
print(a)        # [1, 2]      ← a 不变
```

**差异原因**：list 实现了 `__iadd__`（原地 extend），而 `+` 调用 `__add__`（新建）。**在函数参数上这个差异会引发 bug**。

### 7. 🔴 二维列表的乘法复制

```python
# ---- 本段讲：用 * 复制嵌套列表，复制出来的是「同一行的引用」 ----
# [x] * 3 只是把同一个引用重复 3 次；改任意一行，三行一起变。
grid = [[0] * 3] * 3        # ❌ 三行是同一个 list 对象
grid[0][0] = 9              # 改第一行第 0 列，实际上改的是三个行引用共享的那个 list
print(grid)                 # [[9,0,0],[9,0,0],[9,0,0]]

# 推导式每轮都执行一次 [0] * 3，因此得到 3 个互相独立的 list（Java 要循环 new int[3]）。
grid = [[0] * 3 for _ in range(3)]     # ✅
```

### 8. 🔴 浅拷贝 vs 深拷贝

```python
# ---- 本段讲：浅拷贝只复制第一层，嵌套对象仍是共享引用 ----
# Java 的 new ArrayList<>(old) 也是浅拷贝，行为一致；要彻底隔离必须深拷贝。
import copy

nested = [[1, 2], [3, 4]]      # 外层 list，元素是两个内层 list
shallow = nested.copy()        # 只复制外层；内层 list 对象仍是同一批引用
shallow[0][0] = 99             # 通过副本改内层，原对象也受影响
print(nested)                  # [[99,2],[3,4]]  ← 被改了！

deep = copy.deepcopy(nested)   # ✅ 递归复制每一层，得到完全独立的副本
```

### 9. 🔴 作用域：函数里赋值导致 UnboundLocalError

```python
# ---- 本段讲：函数里只要对变量「赋值」，整个函数内它就是局部变量 ----
# Python 在编译函数时扫描赋值语句，发现 count 被赋值就把它登记为局部变量，
# 于是前面的 print(count) 找不到局部变量、又不会去读全局，直接抛 UnboundLocalError。
count = 0             # 模块级（全局）变量

def inc():
    print(count)      # ❌ UnboundLocalError
    count += 1        # 这行让 count 成为局部变量

def inc_fixed():
    global count      # ✅（但更好的是不用 global）  声明「这里的 count 指全局那个」
    count += 1        # 现在才是对全局变量做自增
```

**Java 对照**：Java 里这会是**编译错误**，Python 在运行时才炸，更难发现。

### 10. 🟡 字符串不可变（方法返回新串）

```python
# ---- 本段讲：字符串不可变，所有「修改」方法都返回新串 ----
# 和 Java 的 String 一样：toUpperCase() 不会改原对象，必须用返回值覆盖。
s = "hello"
s.upper()             # 返回值被丢弃；这行执行完 s 仍是 "hello"
print(s)              # 'hello'   ← 没变

s = s.upper()         # ✅ 把新字符串重新赋回 s，才真正「看起来被修改」
```

**Java 里 String 也是不可变**，但 Python 的 `list.sort()` 是原地、`sorted()` 是新对象，两者混着更容易晕：

```python
# ---- 本段讲：list.sort() 原地排序返回 None，sorted() 返回新列表 ----
# Java 的 Collections.sort(list) 返回 void，思路接近 list.sort()；要新列表就用 sorted()。
nums.sort()           # 原地，返回 None；nums 需在前面定义，排序结果直接体现在 nums 上
new = sorted(nums)    # 新列表；原 nums 顺序不变，适合不想改动原数据时用
# new = nums.sort()   # ❌ new 是 None；把「原地方法」的返回值赋给变量是最常见的 NoneType 报错来源
```

---

## B. 容器（11-20）

### 11. 🔴 `{}` 是空字典不是空集合

```python
# ---- 本段讲：{} 是空字典，空集合必须写 set() ----
# Java 里 Map/Set 都有明确接口类型；Python 的字面量 {} 历史上先给了 dict，空集合只能用构造函数。
type({})          # <class 'dict'>   {} 表示空字典，不是空集合
type(set())       # <class 'set'>    set() 才是空集合；非空集合可写 {1, 2}
```

### 12. 🔴 `dict[key]` 不存在会抛 KeyError

```python
# ---- 本段讲：dict[key] 缺键直接抛 KeyError，get 才返回默认值 ----
# Java 的 Map.get 缺键返回 null；Python 的下标访问更严格，不做「找不到就 null」的兜底。
d = {}                    # 空字典（不是空集合）
d["a"]                    # ❌ KeyError  中括号只用于「确定存在」的场景
d.get("a")                # None        缺键返回 None，不抛异常
d.get("a", 0)             # 0           缺键返回指定的默认值；键存在但值是 0 时仍返回 0
```

Java 的 `Map.get` 返回 null，Python 的 `dict[]` 直接炸。**养成一律用 `.get()` 的习惯**。

### 13. 🔴 `or` 做默认值会吞掉 0/""/False

```python
# ---- 本段讲：or 的短路求值会把 0 / "" / False 当成「没有值」 ----
# Python 的 or 返回「第一个为真的操作数」；0、""、[]、{}、None 都是假值。
d = {"count": 0}
d.get("count") or 100        # 100    ❌ 期望 0：取到 0 后判定为假，于是返回了 100
d.get("count", 100)          # 0      ✅ 由 get 自己判断「键在不在」，键存在就原样返回 0

# 这里 or 恰好是想要的效果（空串视为没填），但要清楚原因：空串是假值所以取右边。
name = "" or "匿名"           # "匿名"  （这里是对的，但要知道为什么）
```

### 14. 🔴 遍历 dict/list 时修改

```python
# ---- 本段讲：遍历字典时不能改变大小，要删就先复制键列表 ----
# for k in d 迭代的是字典的实时视图，删除元素会让迭代器失效（同 Java 的 ConcurrentModificationException）。
d = {"a": 1, "b": 2}
for k in d:
    del d[k]          # ❌ RuntimeError: dictionary changed size during iteration

# list(d.keys()) 先做一份独立快照，遍历快照、删除原字典，两者互不干扰。
for k in list(d.keys()):    # ✅ 先复制键
    del d[k]                # 删除的是 d 里的键，快照本身不受影响
```

### 15. 🟡 `list.remove` 按值删，值不存在抛异常

```python
# ---- 本段讲：list.remove 按「值」删，值不存在会抛 ValueError ----
# Java 的 List.remove(Object) 返回 boolean 不抛异常；Python 的 remove 找不到就炸，语义不同。
nums = [1, 2, 3]
nums.remove(9)         # ❌ ValueError  只删第一个匹配项，这里根本没有 9

# 安全写法
# 先判断再删：多一次 O(n) 查找，但逻辑最直白。
if 9 in nums:
    nums.remove(9)
# 或
# EAFP 风格：直接尝试，捕获「值不存在」这一种异常，其他异常照常抛出。
try:
    nums.remove(9)
except ValueError:
    pass               # 值不存在时静默忽略；若业务需要提示，应在这里记日志
```

### 16. 🔴 `list.pop(0)` 是 O(n)

```python
# ---- 本段讲：队列用 deque，不要用 list.pop(0) ----
# list 是连续数组，pop(0) 要把后面所有元素前移一位，单次 O(n)，循环 n 次就是 O(n²)。
# deque 是双向链表式结构，两端进出都是 O(1)。
from collections import deque

# ❌ 循环里用 list.pop(0) 做队列，复杂度 O(n²)
# ✅
q = deque()      # 创建空双端队列；也常写 deque([初始元素...])
q.append(1)      # 入队：从右端追加，O(1)
q.popleft()      # O(1)  出队：从左端弹出，对应 Java 的 Queue.poll()
```

### 17. 🔴 `x in list` 是 O(n)

```python
# ---- 本段讲：成员判断 list 是 O(n)，set/dict 是 O(1) ----
# list 只能从头逐个比对；set 基于哈希表，平均一次探测就能定位。
# 数字里的下划线只是分隔符，1_000_000 等价 1000000。
# 数据量大时
big = list(range(1_000_000))   # 生成 0~999999 的列表；range 是惰性的，list() 才真正materialize
999_999 in big          # 慢：要扫到最后一个元素，最坏 100 万次比较

big_set = set(big)      # 建哈希集合；构建本身 O(n)，但只做一次
999_999 in big_set      # 快，O(1)；之后每次判断都是常数时间
```

### 18. 🟡 `itertools.groupby` 必须先排序

```python
# ---- 本段讲：groupby 只合并「相邻」的相同 key，必须先排序 ----
# 它相当于「扫描一遍、遇到 key 变化就切一刀」，并非全局分组；Java 的 groupingBy 则是全局的。
import itertools

data = [("a", 1), ("b", 2), ("a", 3)]   # 两个 a 被 b 隔开
for k, g in itertools.groupby(data, key=lambda x: x[0]):
    print(k, list(g))    # g 是迭代器，list() 取出该组内容
# a [(a,1)]
# b [(b,2)]
# a [(a,3)]     ← 同一个 key 被分成两组！

# ✅ 先排序
# sorted 让相同 key 连续排列；key 参数指定「按元素的第 0 项排序」。
for k, g in itertools.groupby(sorted(data, key=lambda x: x[0]), key=lambda x: x[0]):
    ...                  # 省略号占位；这里应写 print(k, list(g))，a 组会一次拿到两条
```

Java 的 `Collectors.groupingBy` 没有这个限制。

### 19. 🟡 排序时混用 int 和 str

```python
# ---- 本段讲：Python 3 不允许 int 与 str 比较大小，排序会直接抛 TypeError ----
# Java 是强类型，编译期就不让你把 Integer 和 String 放一起排；Python 要到运行时排序比较时才炸。
sorted([1, "2", 3])       # ❌ TypeError: '<' not supported between 'str' and 'int'
```

Python 3 不支持跨类型比较（Java 里也不会，但 Java 编译期就拦住了）。

### 20. 🟡 `tuple` 单元素必须带逗号

```python
# ---- 本段讲：单元素元组必须带尾逗号，括号本身不构成元组 ----
# 元组的本质是「逗号分隔」，括号只是分组；(1) 就是一个带括号的整数表达式。
t = (1)      # ❌ 这是 int；type(t) 是 <class 'int'>
t = (1,)     # ✅ 尾逗号才表示「只有一个元素的元组」；type(t) 是 <class 'tuple'>
```

---

## C. 函数与类（21-30）

### 21. 🔴 `self` 忘写 / 忘传

```python
# ---- 本段讲：实例方法的第一个参数必须是 self ----
# Python 调用 a.method() 时，会自动把 a 作为第一个位置参数传进去（Java 的 this 是隐式的，Python 是显式的）。
class A:
    def method():          # ❌ 少 self；形参列表是空的
        pass               # 函数体为空也要写 pass，不能留空白

A().method()               # TypeError: method() takes 0 positional arguments but 1 was given
                           # 报错含义：方法声明收 0 个参数，但调用时自动传了 1 个（实例自身）
```

### 22. 🔴 子类忘调 `super().__init__()`

```python
# ---- 本段讲：子类重写 __init__ 后必须显式调用 super().__init__ ----
# Java 会在子类构造器第一行自动插入 super()；Python 不会，父类的初始化逻辑完全不执行。
class Parent:
    def __init__(self, name):
        self.name = name      # 父类负责初始化 name 字段

class Child(Parent):
    def __init__(self, name, age):
        self.age = age        # ❌ 忘调 super，self.name 不存在
        # ✅ super().__init__(name)   补上这行才会创建 self.name

c = Child("x", 1)             # 构造本身不会报错，因为 Python 不检查字段是否初始化
print(c.name)                # AttributeError  直到访问不存在的属性才暴露
```

**Java 会自动调 super()，Python 不会，而且不报错（直到你访问那个字段）。**

### 23. 🔴 类属性写可变对象

```python
# ---- 本段讲：类属性是「类级别」的，可变类属性会被所有实例共享 ----
# item = [] 写在 class 体里属于类属性（类似 Java 的 static 字段），不是每个对象一份。
class Bad:
    items = []            # ❌ 所有实例共享；任何实例 append 都会影响全部实例

class Good:
    def __init__(self):
        self.items = []   # ✅ 在构造器里赋值，self.items 才是「每个实例自己的」属性
```

### 24. 🔴 `__eq__` 定义了却没定义 `__hash__`

```python
# ---- 本段讲：自定义 __eq__ 后必须同时定义 __hash__，否则对象不可哈希 ----
# 定义 __eq__ 会让 Python 自动把 __hash__ 置为 None（因为「相等对象应有相同哈希」）。
class Order:
    def __init__(self, no): self.no = no      # 单行写法：冒号后直接跟语句，等价于换行缩进
    def __eq__(self, o): return self.no == o.no   # 只按订单号判断相等

{Order("A")}      # ❌ TypeError: unhashable type: 'Order'
                  # 放进 set/dict 需要哈希值，但 __hash__ 已被隐式禁用
```

**原因**：定义了 `__eq__` 后 Python 自动把 `__hash__` 设为 None。
**正确做法**：同时定义 `__hash__`，或用 `@dataclass(frozen=True)`。

### 25. 🔴 闭包延迟绑定（循环里的 lambda）

```python
# ---- 本段讲：闭包按「变量」而不是「当时的取值」捕获，循环里要默认参数绑定 ----
# lambda 体在调用时才读 i，而那时循环早已结束，i 停在最后一个值 2。
funcs = [lambda: i for i in range(3)]   # 创建 3 个 lambda，都引用同一个外层变量 i
print([f() for f in funcs])       # [2, 2, 2]   ❌ 期望 [0,1,2]

# i=i 是默认参数：定义 lambda 的那一刻就把当前 i 的值存进默认值，相当于按值捕获。
funcs = [lambda i=i: i for i in range(3)]
print([f() for f in funcs])       # [0, 1, 2]   ✅
```

### 26. 🔴 装饰器忘加 `functools.wraps`

```python
# ---- 本段讲：装饰器返回的 wrapper 会顶替原函数的元信息 ----
# @timer 语法等价于 fetch = timer(fetch)，所以 fetch 这个名字最终指向 wrapper。
import functools

def timer(fn):                # 接收被装饰的函数
    def wrapper(*args, **kwargs):   # *args/**kwargs 原样转发任意位置参数和关键字参数
        return fn(*args, **kwargs)  # 这里省略了计时逻辑，只演示包装结构
    return wrapper            # 返回内部函数，原函数被替换掉

@timer
def fetch():
    """拉取数据"""             # 只有 docstring 的函数体，函数实际返回 None

print(fetch.__name__)     # wrapper   ❌ 名字被覆盖；本该是 "fetch"
print(fetch.__doc__)      # None      ❌ 文档丢了；本该是 "拉取数据"

# ✅ 加 @functools.wraps(fn)  在 wrapper 定义上加这行，元信息就会复制回来
```

**后果**：pytest 收集不到测试、FastAPI 拿不到参数签名、日志名字全错。

### 27. 🟡 lambda 只能写单个表达式

```python
# ---- 本段讲：lambda 体只能是一个表达式，别用海象运算符硬凑多步 ----
# lambda 没有语句块，不能写 return / if 语句 / 赋值语句，只能写「能求值出结果」的表达式。
f = lambda x: (a := x * 2, a + 1)[1]     # 能用但别这么写；海象运算符 := 在表达式里赋值，再用元组下标取第二项
# 需要多语句就写 def；可读性远比「一行流」重要
```

### 28. 🔴 生成器只能遍历一次

```python
# ---- 本段讲：生成器是「一次性」迭代器，遍历完就空了 ----
# 生成器表达式用圆括号，惰性求值；内部游标只能前进，不会重置。
g = (x for x in range(3))   # 此时还没真正计算，只是创建了生成器对象
print(sum(g))     # 3    第一次 sum 把 0、1、2 依次取完
print(sum(g))     # 0    ← 已耗尽；再迭代没有任何元素，sum 返回 0（不是报错）
```

### 29. 🟡 递归深度只有 ~1000

```python
# ---- 本段讲：Python 没有尾递归优化，默认递归深度约 1000 层 ----
# 每层调用都真实占用 C 栈；超过 sys.getrecursionlimit()（默认 1000）就抛 RecursionError。
def fact(n):
    return 1 if n <= 1 else n * fact(n - 1)   # 三元表达式写基线条件；递归向下展开

fact(2000)        # RecursionError  递归 2000 层，远超默认上限；应改写为循环
```

没有尾递归优化，深度递归必须改迭代。

### 30. 🟡 `@property` 与同名字段冲突

```python
# ---- 本段讲：@property 只读属性会拦截同名赋值 ----
# 属性名 x 被 property 接管后，self.x = ... 会调用 setter；没定义 setter 就抛 AttributeError。
class A:
    @property
    def x(self): return self._x     # getter：读取 a.x 时执行；真实数据存在 _x
                                    # 单行写法，等价于 return 语句独占一行

    def __init__(self):
        self.x = 1        # ❌ AttributeError: can't set attribute（没有 setter）
        self._x = 1       # ✅ 约定：下划线开头表示「内部字段」，直接写它绕开 property
```

---

## D. 并发（31-36）（**最容易出生产事故**）

### 31. 🔴 多线程跑 CPU 密集任务没有加速

```python
# ---- 本段讲：GIL 让多线程无法并行执行 CPU 密集任务 ----
# GIL（全局解释器锁）保证同一时刻只有一个线程执行 Python 字节码，
# 所以 threading 只对「等待 I/O」有效；纯计算要用多进程绕开 GIL。
# GIL 导致同一时刻只有一个线程执行 Python 字节码
# CPU 密集 → 用 multiprocessing.ProcessPoolExecutor
```

实测：`python code/05_concurrency/01_gil_proof.py`

### 32. 🔴 `async def` 里调同步阻塞函数

```python
# ---- 本段讲：async def 里调用同步阻塞函数会卡死整个事件循环 ----
# 事件循环是单线程的：一个协程不让出控制权，其他所有协程都无法推进。
# 判断标准很简单——调用处没有 await、又可能等待（网络/磁盘/sleep），就是嫌疑点。
import time, requests

async def bad():
    requests.get(url)      # ❌ 卡死整个事件循环，所有并发请求排队
    time.sleep(1)          # ❌ 同上；time.sleep 会真正阻塞 OS 线程，不会让出执行权

async def good():
    await asyncio.to_thread(requests.get, url)     # ✅ 丢到线程池执行，await 期间事件循环可调度其他协程
    await asyncio.sleep(1)                          # ✅ 异步 sleep：把控制权交还事件循环，1 秒后再恢复
```

**这是 FastAPI 第一大坑**，且症状隐蔽：单个请求测试没问题，一并发就全卡。

### 33. 🔴 忘了 `if __name__ == "__main__":`（Windows 多进程）

```python
# ---- 本段讲：Windows 多进程必须把「启动代码」放进 __main__ 保护 ----
# Windows 用 spawn 方式启动子进程：子进程会重新 import 主模块，
# 若进程池创建写在顶层，import 时又执行一次，于是不断创建新进程（递归爆炸）。
from concurrent.futures import ProcessPoolExecutor

def work(x): return x * 2       # 模块级函数，可被 pickle 后传给子进程

# ❌ 顶层直接执行，Windows 上子进程重新 import 模块 → 无限递归创建进程
with ProcessPoolExecutor() as pool:
    pool.map(work, range(10))

# ✅
if __name__ == "__main__":      # 只有「直接运行」时该条件为真；子进程 import 时为假
    with ProcessPoolExecutor() as pool:   # with 结束会自动 shutdown 并等待任务完成
        pool.map(work, range(10))         # map 返回迭代器，需迭代或等待才会真正取结果
```

### 34. 🟡 多进程传不可 pickle 的对象

```python
# ---- 本段讲：跨进程传参必须可 pickle，lambda/局部函数都不行 ----
# 多进程不是共享内存，任务函数和参数要序列化后发给子进程；pickle 只认「有模块路径的具名对象」。
pool.map(lambda x: x * 2, data)     # ❌ Can't pickle local object；lambda 没有可导入的模块名
# ✅ 用模块级函数  在文件顶层 def double(x): return x * 2，再 pool.map(double, data)
```

连接对象、锁、生成器、lambda、局部函数都不能跨进程传。

### 35. 🟡 GIL 不等于线程安全

```python
# ---- 本段讲：GIL 只保证单字节码原子，counter += 1 仍是「读-改-写」三步 ----
# 线程可能在任意字节码边界被切换，两个线程读到同一个旧值就会丢更新。
counter = 0
def inc():
    global counter               # 声明要修改的是模块级 counter，否则会变成局部变量
    for _ in range(100_000):     # 下划线表示「循环变量用不到」
        counter += 1      # ❌ 读-改-写三步，非原子

# ✅ 必须加锁
lock = threading.Lock()          # 可重入？不，这是普通互斥锁（RLock 才可重入）
def inc_safe():
    global counter
    for _ in range(100_000):
        with lock:               # with 自动 acquire/release，异常时也能释放
            counter += 1         # 临界区：同一时刻只有一个线程能进来
```

### 36. 🔴 `asyncio.run()` 不能在已有事件循环里调

```python
# ---- 本段讲：事件循环里不能嵌套 asyncio.run()，协程之间用 await 组合 ----
# asyncio.run 的职责是「创建并关闭事件循环」，循环里已经有循环了，再建就冲突。
async def outer():
    asyncio.run(inner())      # ❌ RuntimeError: asyncio.run() cannot be called from a running event loop
    await inner()             # ✅ 直接 await；inner 必须也是 async 函数，返回可等待对象
```

---

## E. 文件与编码（37-42）

### 37. 🔴🔴 不指定 encoding（Windows 必炸）

```python
# ---- 本段讲：读写文本必须显式指定 encoding="utf-8" ----
# 不指定时用的是「系统默认编码」：Windows 中文环境是 GBK(cp936)，Linux/macOS 是 UTF-8；
# 同一份 UTF-8 文件在 Windows 上读就会 UnicodeDecodeError，和 Java FileReader 的平台编码坑一样。
open("a.txt").read()                            # ❌ Windows 默认 GBK
open("a.txt", encoding="utf-8").read()          # ✅ 显式指定编码，跨平台一致
Path("a.txt").read_text(encoding="utf-8")       # ✅ pathlib 版本；参数名同样是 encoding

# 给 Excel 看的中文 CSV 要用 utf-8-sig
# utf-8-sig 会写入 BOM，Excel 靠 BOM 识别编码，双击打开才不会乱码。
open("a.csv", "w", encoding="utf-8-sig", newline="")   # newline="" 交给 csv 模块控制换行
```

**这条是本清单里最容易浪费半天的坑**。跟 Java 的 `FileReader` 平台编码问题一模一样。

### 38. 🔴 CSV 写入不加 `newline=""`

```python
# ---- 本段讲：csv 写入不加 newline="" 会在 Windows 上多出空行 ----
# csv 模块自己会写 \r\n；文本模式又做一次换行转换，\r\n 变成 \r\r\n，看起来就是空行。
# ❌ Windows 上每行之间多一个空行
with open("a.csv", "w", encoding="utf-8") as f:
    csv.writer(f).writerow([1, 2])      # 每写一行就多插一个空行

# ✅
# newline="" 关闭 Python 的换行转换，让 csv 模块完全掌控行尾。
with open("a.csv", "w", encoding="utf-8", newline="") as f:
```

### 39. 🔴 `json.dumps` 默认转义中文

```python
# ---- 本段讲：json.dumps 默认转义非 ASCII 字符 ----
# 默认 ensure_ascii=True，所有中文（包括键名）都会变成 \uXXXX；日志和接口返回里可读性归零。
# 注意：转义后仍是合法 JSON，程序能正常解析，所以这个坑往往只在人工排查时才发现。
json.dumps({"名": "中文"})                       # '{"\\u540d": "\\u4e2d\\u6587"}'
json.dumps({"名": "中文"}, ensure_ascii=False)   # '{"名": "中文"}'  中文原样输出，推荐默认加上
```

### 40. 🟡 `"w"` 模式静默清空文件

```python
# ---- 本段讲：文件模式决定「覆盖 / 独占创建 / 追加」，选错会静默丢数据 ----
# "w" 一打开就截断为 0 字节，哪怕你一个字都没写；这是不可逆的数据丢失。
open("important.txt", "w").close()      # 内容全没了，不报错；close() 立刻关文件

# 想"存在就报错"用 "x"，想追加用 "a"
open("important.txt", "x")              # 独占创建：文件已存在抛 FileExistsError，绝不会覆盖
open("log.txt", "a")                    # 追加：写到文件末尾，原内容保留
```

### 41. 🟡 相对路径的基准不是你以为的

```python
# ---- 本段讲：相对路径的基准是「进程工作目录」，不是脚本所在目录 ----
# 在哪个目录敲 python 命令，相对路径就从哪里算；换一处启动就找不到文件。
# 基准是"当前工作目录"，不是"脚本所在目录"
# 在 A 目录执行 python /path/to/script.py，相对路径基于 A

# ✅ 用脚本位置构造绝对路径
# __file__ 是当前脚本的路径（导入时是相对/绝对路径，取决于启动方式），resolve 补全成绝对路径。
from pathlib import Path
BASE = Path(__file__).resolve().parent   # 脚本所在目录；对应 Java 里基于 classpath 定位资源
config = BASE / "config.json"            # 与启动目录无关，任何地方运行都指向同一文件
```

**Java 里 `getResourceAsStream` 是基于 classpath，Python 没有这个东西**，路径全靠 `__file__` 自己算。

### 42. 🔴 用 float 算钱

```python
# ---- 本段讲：float 是二进制近似值，算钱必须用 Decimal 且用字符串构造 ----
# 0.1 在二进制里是无限循环小数，存成 float 时已被截断，相加误差会暴露出来。
print(0.1 + 0.2)              # 0.30000000000000004
print(0.1 + 0.2 == 0.3)       # False  所以浮点数永远不要用 == 比较

from decimal import Decimal
Decimal("0.1") + Decimal("0.2")     # Decimal('0.3')  精确十进制运算
Decimal(0.1)                        # ❌ Decimal('0.1000000000000000055...')  把已失真的 float 带进 Decimal
Decimal("0.1")                      # ✅ 永远用字符串构造；对应 Java new BigDecimal("0.1")
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
# ---- 本段讲：模块级互相 import 会形成循环，导致 ImportError ----
# a 导入 b 时 b 还没定义完就去导入 a，a 里被引用的名字此时尚不存在。
# a.py: from b import B
# b.py: from a import A     → ImportError

# 解法：函数内延迟导入 / import 模块而非名字 / 抽公共模块
# 函数内导入：把 import 挪进函数体，调用时才执行，避开模块加载期的循环。
# import 模块而非名字：写 import b 再用 b.B，属性在运行时才解析。
# 抽公共模块：把双方都依赖的部分抽到第三个模块，消除环。
```

### 46. 🔴 提交了 `.env`

`.env` 含密钥。提交 `.env.example`（占位值），`.env` 进 `.gitignore`。
**一旦提交过，密钥就等于泄露了**（git 历史里永远存在），必须立刻轮换。

### 47. 🟡 用 `import *`

```python
# ---- 本段讲：import * 会把目标模块所有公开名字塞进当前命名空间 ----
# 后果：名字来源不可追溯、可能覆盖内置函数（如 list/str），静态检查也失效。
from module import *      # ❌ 污染命名空间，看不出名字来源，覆盖内置函数
```

---

## G. 其他高频（48-50）

### 48. 🔴 `assert` 用于业务校验

```bash
python -O script.py     # assert 全部失效！
```

```python
# ---- 本段讲：assert 是「调试断言」，不是业务校验 ----
# python -O 启动会移除所有 assert，校验逻辑会凭空消失，而且不会有任何提示。
def withdraw(amount):
    assert amount > 0, "金额必须为正"    # ❌ -O 模式下不生效
    if amount <= 0:                      # 业务校验必须用显式条件判断
        raise ValueError("金额必须为正")  # ✅ 无论是否 -O 都会执行，异常类型也明确
```

### 49. 🔴 SQL 拼接字符串

```python
# ---- 本段讲：SQL 一律用占位符传参，不要用 f-string 拼进语句 ----
# 拼字符串时用户输入会被当成 SQL 语法解析（如 ' OR 1=1 --），这就是 SQL 注入。
cur.execute(f"SELECT * FROM t WHERE name = '{user_input}'")    # ❌ SQL 注入
cur.execute("SELECT * FROM t WHERE name = ?", (user_input,))   # ✅ 占位符由驱动转义；参数必须是元组
```

Java 里用 PreparedStatement 是常识，Python 里手写字符串拼接的代码仍然很多。

### 50. 🟡 用 `print` 打日志

```python
# ---- 本段讲：print 不能替代日志，生产环境必须用 logging ----
# print 没有级别、无法按环境关闭、没有时间戳和模块名，也不能重定向到文件/采集系统。
print(f"order={order}")        # ❌ 无法分级、无法关闭、无时间戳、同步阻塞

import logging
logger = logging.getLogger(__name__)   # 模块级 logger，名字就是模块名，便于按模块调级别
logger.info("order=%s", order)  # ✅ 惰性格式化，级别可控；只有真要输出时才拼字符串
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
