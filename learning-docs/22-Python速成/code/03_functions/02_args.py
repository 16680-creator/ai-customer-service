"""02 · 参数体系：*args / **kwargs / 仅关键字

运行: python 03_functions/02_args.py
"""

# ---- 本文件讲：Python 的五种参数形态，以及调用时的两种解包写法 ----
# Python 参数比 Java 灵活得多：Java 只有「位置参数 + 可变参数（T...）」，没有关键字参数、默认参数写在签名里也没法跳着传。
# 这里演示的 def 签名顺序、`*`/`/` 分隔符、解包调用，都是读框架源码（FastAPI/Django/装饰器）必须认识的语法。
# 最后两段是「可变默认参数」这个真实生产事故级陷阱，以及装饰器必须用 *args/**kwargs 的原因。
print("=" * 60)
print("1. 参数顺序：位置 → 默认 → *args → 仅关键字 → **kwargs")
print("=" * 60)

def full(a, b=2, *args, c, d=4, **kwargs):
    # a：必填位置参数；b：带默认值的位置参数（可省略）
    # *args：把多余的位置实参打包成元组（Java 的 int... args，但这里是 tuple）
    # c：* 之后的「仅关键字参数」，必须用 c=... 传，不能靠位置
    # d=4：仅关键字且带默认值；**kwargs：把多余的关键字实参打包成字典
    print(f"  a={a} b={b} args={args} c={c} d={d} kwargs={kwargs}")

print("full(1, 3, 5, 6, c=7, e=8):")
full(1, 3, 5, 6, c=7, e=8)   # a=1, b=3，剩余位置 5、6 进 args，c 必须关键字给，e 落进 kwargs


# ---- 2. 调用侧解包：*list 按位置铺开，**dict 按关键字铺开 ----
# 这就是「定义时 *args/**kwargs 收、调用时 */** 放」的对称设计。
# Java 只能用反射 Method.invoke 或手写 Builder 才能做到「动态参数个数 + 动态参数名」。
print("\n" + "=" * 60)
print("2. 解包调用")
print("=" * 60)

def add3(a, b, c):
    return a + b + c        # 三个位置参数求和

nums = [1, 2, 3]            # 一个列表，长度必须正好等于形参个数
print(f"add3(*[1,2,3])   = {add3(*nums)}")   # *nums 等价于写 add3(1, 2, 3)

kwargs = {"a": 1, "b": 2, "c": 3}           # 键名必须与形参名完全一致
print(f"add3(**dict)     = {add3(**kwargs)}")   # **kwargs 等价于写 add3(a=1, b=2, c=3)

print("\n  Java 没有这个能力：必须手动拆数组或用 Builder")


# ---- 3. 仅关键字参数：`*` 之后的名字只能用 name=value 传 ----
# 作用是把「容易搞混的布尔/数值开关」强制变成具名参数，编译器帮你挡住顺序错误。
# Java 对照：Java 没有这个机制，只能用 Builder 或具名枚举来避免 transfer(a, b, true) 这种可读性灾难。
print("=" * 60)
print("3. 仅关键字参数（* 后面的必须用关键字传）")
print("=" * 60)

def transfer(from_acct, to_acct, *, amount, currency="CNY"):
    # from_acct/to_acct：允许位置传；`*` 之后的 amount/currency 必须写名字
    return f"{from_acct} -> {to_acct} : {amount} {currency}"

print(f"  {transfer('A', 'B', amount=100)}")                       # 正确：amount 用关键字，currency 走默认
print(f"  {transfer('A', 'B', amount=100, currency='USD')}")        # 正确：显式指定币种

try:
    transfer("A", "B", 100)      # 错误：第三个位置实参无法落到 amount 上
except TypeError as e:
    print(f"  位置传 -> TypeError: {e}")   # 报 "takes 2 positional arguments but 3 were given"

print("\n  价值：防止参数顺序错乱造成生产事故")


# ---- 4. 仅位置参数：`/` 之前的名字只能按位置传（3.8+） ----
# 与 `*` 正好相反，`/` 用于「参数名只是内部实现细节」的场景（内置函数、C 扩展常用）。
# Java 对照：Java 方法参数只能位置传，天然等价于「全部仅位置」，所以这条对 Java 人反而最熟悉。
print("=" * 60)
print("4. 仅位置参数（3.8+，/ 之前的只能按位置传）")
print("=" * 60)

def div(a, b, /):
    # a、b 都在 `/` 之前 → 只能 div(6, 2)，不能 div(a=6, b=2)
    return a / b

print(f"  div(6, 2) = {div(6, 2)}")   # 正常按位置调用，得到 3.0（注意 / 结果是 float）
try:
    div(a=6, b=2)              # 错误：a、b 被 / 声明为仅位置，用关键字传会报错
except TypeError as e:
    print(f"  div(a=6, b=2) -> TypeError: {e}")   # 报 "got some positional-only arguments passed as keyword arguments"


# ---- 5. 默认参数只在「定义时」求值一次，可变对象会被所有调用共享 ----
# def bad(item, lst=[])：那个 [] 在函数定义那一刻创建，之后每次调用复用同一个列表 → 数据不断累积。
# 这是 Python 最经典的坑之一，Java 不会出现，因为 Java 的默认值必须是编译期常量（根本无法写 new ArrayList<>()）。
# 正确做法：默认值用 None 当哨兵，函数体内再创建新对象（判断用 is None，不要用 ==）。
print("=" * 60)
print("5. 默认参数陷阱")
print("=" * 60)

def bad(item, lst=[]):
    lst.append(item)       # 追加到「默认参数那个列表」上，而它是在 def 时创建、全局唯一的
    return lst             # 返回同一个列表对象

print(f"  bad(1) = {bad(1)}")    # 第一次：[1]
print(f"  bad(2) = {bad(2)}    <- 不是 [2]")   # 第二次：[1, 2]，上次的结果还在
print(f"  bad.__defaults__ = {bad.__defaults__}   <- 同一个对象被复用")   # 查看默认值元组，确实是 ([1,2],)

def good(item, lst=None):
    if lst is None:        # 用 is None 判断「调用方没传」，而不是判断列表是否为空
        lst = []           # 每次调用都新建一个空列表 → 各次调用互不影响
    lst.append(item)
    return lst

print(f"\n  good(1) = {good(1)}")    # [1]
print(f"  good(2) = {good(2)}")      # [2]，不再累积

print("\n  不可变默认值（0, '', None, ()）没有这个问题")


# ---- 6. 实战：用 **kwargs + 字典解包做「默认配置 + 用户覆盖」 ----
# {**DEFAULTS, **options} 是字典的合并语法：后面的同名键覆盖前面的。
# Java 对照：等价于 new HashMap<>(DEFAULTS) 然后 putAll(options)，但 Python 一行就能表达，而且不修改原字典。
print("=" * 60)
print("6. 实战：用 **kwargs 做配置合并")
print("=" * 60)

DEFAULTS = {"timeout": 30, "retries": 3, "verbose": False}   # 模块级默认配置；约定用全大写表示常量

def connect(host, **options):
    # host：必填位置参数；**options：剩余所有关键字实参打包成 dict
    cfg = {**DEFAULTS, **options}     # 后者覆盖前者：先铺默认值，再用用户传入的同名键覆盖
    return {"host": host, **cfg}      # 再把 host 合并进去返回一个新字典（未改动 DEFAULTS）

print(f"  connect('db1')                      -> {connect('db1')}")                      # 全用默认值
print(f"  connect('db1', timeout=5)           -> {connect('db1', timeout=5)}")           # 只覆盖 timeout
print(f"  connect('db1', retries=0, extra=1)  -> {connect('db1', retries=0, extra=1)}")   # 覆盖 retries 并新增 extra 键


# ---- 7. 实战：装饰器必须用 (*args, **kwargs) 才能包装任意签名的函数 ----
# 包装器不知道、也不该关心原函数有几个参数，所以「全收 + 原样转发」是唯一通用的写法。
# 这是下面 03_decorator.py 的基础；Java 对应的是动态代理 / AOP 里 Method.invoke(target, args)。
print("=" * 60)
print("7. 实战：转发所有参数给被包装函数")
print("=" * 60)

def log_call(fn):
    def wrapper(*args, **kwargs):
        print(f"    调用 {fn.__name__}(args={args}, kwargs={kwargs})")   # 先打日志：打印收到的位置参数元组与关键字字典
        return fn(*args, **kwargs)   # 再原样转发：* 铺开位置、** 铺开关键字，并返回原函数的返回值
    return wrapper                    # 返回包装后的新函数

@log_call
def create_order(order_no, amount, *, remark=""):
    # 被包装的函数：两个位置参数 + 一个仅关键字参数
    return f"OK {order_no} {amount} {remark}"

print("  create_order('NO1', 100, remark='加急'):")
print(f"  -> {create_order('NO1', 100, remark='加急')}")   # 调用的是 wrapper，日志能看到 args/kwargs 的实际内容

print("\n  装饰器必须用 *args/**kwargs，否则无法包装任意签名的函数")
