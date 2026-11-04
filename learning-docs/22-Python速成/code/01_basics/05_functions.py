"""05 · 函数基础：默认参数、关键字参数、*args/**kwargs

运行: python 01_basics/05_functions.py
"""

print("=" * 60)
print("1. 基本定义与调用")
print("=" * 60)

# ---- 本段讲：函数定义、类型注解、默认参数与关键字调用 ----
# 注解（name: str、-> str）只起提示作用，运行时不校验，等价 Java 的签名类型但更"软"。
# 关键字调用是 Python 特有：调用时直接写 形参名=值，参数顺序可以乱。
def greet(name: str, greeting: str = "你好") -> str:   # greeting 带默认值，等价 Java 的重载简化版
    return f"{greeting}，{name}"                      # 返回 f-string 拼出的问候语

print(greet("张三"))                                  # 只传必填参数，greeting 用默认值
print(greet("张三", "早上好"))                         # 按位置传参（位置参数）
print(greet(name="张三", greeting="嗨"))          # 关键字参数（Java 7 没有）
print(greet(greeting="嗨", name="张三"))          # 顺序无关


print("\n" + "=" * 60)
print("2. *args / **kwargs")
print("=" * 60)

# ---- 本段讲：五种参数形态的组合顺序与解包调用 ----
# 参数顺序规则：普通参数 → 默认参数 → *args → 仅关键字参数 → **kwargs。
# *args 把多余的位置参数收成 tuple，**kwargs 把多余的关键字参数收成 dict。
def f(a, b=2, *args, c, d=4, **kwargs):   # c 没有默认值且在 * 之后，因此必须用关键字传
    print(f"  a={a} b={b} args={args} c={c} d={d} kwargs={kwargs}")   # 打印每类参数收到的值

print("f(1, 3, 5, 6, c=7, e=8):")
f(1, 3, 5, 6, c=7, e=8)                  # a=1 b=3 args=(5,6) c=7 d=4 kwargs={'e': 8}

def sum_all(*nums) -> float:             # 任意个位置参数 → 函数内是一个 tuple
    return sum(nums)                     # sum 是内置函数，对可迭代对象求和

print(f"\nsum_all(1,2,3)      = {sum_all(1, 2, 3)}")
print(f"sum_all(*[1,2,3])   = {sum_all(*[1, 2, 3])}   <- 解包")   # 星号把列表拆成三个位置参数

def build_config(**opts) -> dict:        # 任意个关键字参数 → 函数内是一个 dict
    return opts                          # 直接原样返回，等价收集成 Map

print(f"\nbuild_config(host='h', port=1) = {build_config(host='h', port=1)}")
print(f"build_config(**{{'a':1}})       = {build_config(**{'a': 1})}")   # 双星号把字典拆成关键字参数

def keyword_only(a, *, b, c=3):          # 单独的 * 表示"后面只能是关键字参数"
    return (a, b, c)                     # 返回元组，方便一次打印三个值

print(f"\nkeyword_only(1, b=2) = {keyword_only(1, b=2)}")
try:
    keyword_only(1, 2)                   # 这里想按位置传 b，会直接报错
except TypeError as e:                   # 参数不匹配抛 TypeError（运行期异常）
    print(f"keyword_only(1, 2) -> TypeError: {e}")


print("\n" + "=" * 60)
print("3. 默认参数陷阱（必考）")
print("=" * 60)

# ---- 本段讲：默认参数在 def 时只求值一次，可变默认值会被所有调用共享 ----
# 这是 Python 最经典的陷阱：[] 只在函数定义时创建一次，之后每调用一次都往同一个 list 里加。
# 正确做法：默认值写 None，进函数后再判断并新建列表。
def bad(item, lst=[]):           # 默认值 [] 在 def 执行时创建，绑定在函数对象上
    lst.append(item)             # 每次都往同一个列表追加，等价 list.add
    return lst

print(f"bad(1) = {bad(1)}")
print(f"bad(2) = {bad(2)}    <- 不是 [2]，累积了！")   # 第二次调用拿到的还是第一次那个 list

def good(item, lst=None):        # 用 None 作哨兵值，避免可变对象被复用
    if lst is None:              # 只有调用方没传时才新建
        lst = []                 # 每次调用都得到全新的空列表
    lst.append(item)
    return lst

print(f"\ngood(1) = {good(1)}")
print(f"good(2) = {good(2)}    <- 正确")

# 原因：默认值在 def 执行时求值一次，绑定到函数对象上
print(f"\nbad.__defaults__ = {bad.__defaults__}   <- 同一个 list 被复用")   # 可以亲眼看到里面已经累积了元素


print("=" * 60)
print("4. 函数是一等公民")
print("=" * 60)

# ---- 本段讲：函数是一等公民——能赋值、能传参、能返回、能放进容器 ----
# Java 需要函数式接口（Function/Supplier）才能做到同样的事，Python 函数本身就是对象。
def double(x):
    return x * 2

f = double                       # 赋值给变量
print(f"f(3)             = {f(3)}")

def apply(fn, v):                # 作为参数
    return fn(v)                 # fn 是任意可调用对象，这里才真正执行

print(f"apply(double, 5) = {apply(double, 5)}")

def make_multiplier(n):          # 作为返回值（闭包）
    def inner(x):
        return x * n             # 内层函数捕获了外层变量 n，这就是闭包
    return inner                 # 返回函数对象，不执行

triple = make_multiplier(3)      # 得到"乘以 3"的函数
print(f"triple(4)        = {triple(4)}")

ops = {"double": double, "square": lambda x: x * x}   # 放进容器
print(f"ops['square'](4) = {ops['square'](4)}")        # 取出来加括号调用，等价 Map<String, Function>


print("=" * 60)
print("5. 常用内置函数")
print("=" * 60)

# ---- 本段讲：sorted / map / filter / any / all / max 的常见用法 ----
# 这些内置函数接受"函数 + 可迭代对象"，可视为 Java Stream 的轻量版，但写法短很多。
data = [3, 1, 4, 1, 5, 9, 2]
print(f"sorted(data)                = {sorted(data)}")                  # 返回新列表，不改原列表
print(f"sorted(data, reverse=True)  = {sorted(data, reverse=True)}")      # reverse=True 表示降序
print(f"list(map(str, data[:3]))    = {list(map(str, data[:3]))}")        # map 是惰性迭代器，需 list() 物化
print(f"list(filter(lambda x: x>3, data)) = {list(filter(lambda x: x > 3, data))}")   # filter 保留条件为真的元素
print(f"any(x > 8 for x in data)    = {any(x > 8 for x in data)}")        # 任一为真即 True，等价 Stream.anyMatch
print(f"all(x > 0 for x in data)    = {all(x > 0 for x in data)}")        # 全部为真才 True，等价 allMatch
print(f"max(data), min(data), sum(data) = {max(data)}, {min(data)}, {sum(data)}")   # 内置聚合函数，取代 Collections.max 等

orders = [                                  # 列表里放字典，模拟业务订单
    {"no": "A", "amount": 300},
    {"no": "B", "amount": 100},
    {"no": "C", "amount": 200},
]
print(f"\nsorted by amount            = {[o['no'] for o in sorted(orders, key=lambda o: o['amount'])]}")   # key 指定排序依据，等价 Comparator.comparing
print(f"max by amount               = {max(orders, key=lambda o: o['amount'])}")   # max 也支持 key，取金额最大的字典
print(f"sum of amounts              = {sum(o['amount'] for o in orders)}")         # 生成器表达式直接求和，省一次列表创建

# sorted 的稳定性：先排次要，再排主要
rows = [("a", 2), ("b", 1), ("a", 1)]
by_second = sorted(rows, key=lambda r: r[1])         # 先按第二列排
by_first_then_second = sorted(by_second, key=lambda r: r[0])   # 再按第一列排；稳定排序保证同组内保持上一步次序
print(f"\n稳定排序 -> {by_first_then_second}")        # 等价 Java 里多次 Collections.sort 的稳定行为


print("=" * 60)
print("6. lambda 的限制")
print("=" * 60)

# ---- 本段讲：lambda 只能写单个表达式，不能有语句块 ----
# lambda 等价 Java 的箭头函数，但函数体必须是"一个表达式"，不能写 if 语句/多行逻辑。
# 需要多语句就老老实实写 def。
square = lambda x: x * x          # 匿名函数，等价 (int x) -> x * x；赋值给变量只为演示
print(f"square(4) = {square(4)}")

# 只能单表达式，不能有语句块（Java 的 lambda 可以写 {} 块）
# 需要多语句就写 def
label = lambda s: "及格" if s >= 60 else "不及格"    # 三元表达式算"一个表达式"，所以 lambda 里合法
print(f"label(75) = {label(75)}")
