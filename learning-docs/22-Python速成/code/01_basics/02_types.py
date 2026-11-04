"""02 · 类型、数值与真值判断

运行: python 01_basics/02_types.py
"""

# ---- 本段讲：Python 的常用内置类型只有 int / float / str / bool / None ----
# Java 按位宽分 byte/short/int/long，再分 float/double；Python 只有 int（任意精度）
# 和 float（就是 Java 的 double），所以"选类型"这件事基本不用操心。
# 但"任意精度"和"没有 float32"这两点，在金额、科学计算里会带来不同行为，见下面几节。
from decimal import Decimal    # 标准库模块；Decimal 做精确十进制运算，对应 Java 的 BigDecimal

print("=" * 60)
print("1. 基本类型：没有 int/long/float/double 之分")
print("=" * 60)

# 下面五行是"赋值即声明"：没有类型关键字，变量的类型由当前绑定的值决定。
x = 42          # int，任意精度
y = 3.14        # float，就是 Java 的 double
s = "hello"     # str，不可变
b = True        # bool，注意大写
n = None        # 空值

# 用元组把五个值打包，循环打印"值的 repr 表示 + 类型名"。
# !r 表示用 repr() 而不是 str() 显示（字符串会带引号），:12 表示占 12 列左对齐。
for v in (x, y, s, b, n):                 # 元组字面量，等价 List.of(...)，但不可变
    print(f"{v!r:12} -> {type(v).__name__}")   # type(v) 返回类型对象，.__name__ 取类名字符串

# Java 的 int 是 32 位，Python 可以无限大
big = 2 ** 200                 # ** 是幂运算符；Java 里要写 BigInteger.TWO.pow(200)
print(f"\n2**200 = {big}")
print(f"位数：{len(str(big))}")   # 转成十进制字符串后取长度，验证"真的算出来了"


print("\n" + "=" * 60)
print("2. 除法：Java 程序员第一个坑")
print("=" * 60)

# ---- 本段讲：/ 与 // 的区别（Java 程序员第一坑）----
# Java 里 7 / 2 得到 3（整数除法）；Python 的 / 永远返回 float。
# 要整除必须写 //，而且负数方向也不同：Python 向下取整，Java 向零取整。
print(f"7 / 2   = {7 / 2}      <- 真除法，不是 3")      # 7/2 -> 3.5；Java 的 7/2 是 3
print(f"7 // 2  = {7 // 2}       <- 整除")              # 向下取整的整数除法，正数时等价 Java 的 7/2
print(f"7 % 2   = {7 % 2}")                              # 取余，同 Java 的 %
print(f"2 ** 10 = {2 ** 10}     <- 幂运算")              # 2 的 10 次方；Java 用 Math.pow 且返回 double
print(f"divmod(7, 2) = {divmod(7, 2)}")                  # 一次拿到 (商, 余数)，Java 需分别算两次

# 负数整除是向下取整，Java 是向零取整
print(f"\n-7 // 2 = {-7 // 2}    <- Java 是 -3")       # Python 得 -4，因为往下取到更小的整数
print(f"-7 % 2  = {-7 % 2}     <- Java 是 -1")          # Python 余数符号跟随除数，得 1；结果保证 a == (a//b)*b + a%b


print("\n" + "=" * 60)
print("3. 浮点精度：算钱必须用 Decimal")
print("=" * 60)

# ---- 本段讲：float 的二进制精度问题，以及金额为什么要用 Decimal ----
# float 用 IEEE 754 双精度存储，0.1 无法用二进制精确表示，累加会出现尾差。
# 这跟 Java 的 double 完全一样，所以"金额不要用 double"的老规矩在 Python 同样成立。
print(f"0.1 + 0.2            = {0.1 + 0.2}")           # 实际是 0.30000000000000004，不是 0.3
print(f"0.1 + 0.2 == 0.3     = {0.1 + 0.2 == 0.3}")      # 因此直接 == 比较得到 False

import math                                            # 标准库数学模块，对应 Java 的 java.lang.Math
print(f"math.isclose(...)    = {math.isclose(0.1 + 0.2, 0.3)}")   # 浮点比较应带容差，等价 Java 的 Math.abs(a-b) < eps

print(f"Decimal('0.1')+0.2   = {Decimal('0.1') + Decimal('0.2')}")          # 十进制精确运算，得 0.3
print(f"Decimal(0.1)         = {Decimal(0.1)}   <- 用 float 构造会带误差")    # 传 float 会把二进制误差一起带进来
print(f"Decimal('0.1')       = {Decimal('0.1')} <- 永远用字符串构造")         # 传字符串才能得到精确的 0.1


print("\n" + "=" * 60)
print("4. 真值判断：空容器/0/空串/None 都是假")
print("=" * 60)

# ---- 本段讲：什么是"假值"，以及判空为什么不用 length()/isEmpty() ----
# Python 任何对象都能直接放进 if：空容器、0、空字符串、None、False 全部算假。
# 所以 Java 里的 list.size() == 0、str.isEmpty()、obj == null，在 Python 统一写 not x。
falsy = [None, False, 0, 0.0, "", [], {}, set(), ()]   # 把这些"看起来空"的值放进列表
print("假值：", [repr(v) for v in falsy])              # 列表推导式逐个取 repr，看清每个值的字面形态

items = []                                             # 空列表，等价 new ArrayList<>()
print(f"\nif not items:  -> {not items}     <- Pythonic")     # not 对容器取"是否为空"
print(f"len(items)==0: -> {len(items) == 0}")                 # 老写法，结果一样但不够地道

# 判空一律用 is
v = None
print(f"\nv == None -> {v == None}    能跑但不对")    # == 可能被自定义 __eq__ 改写，语义是"值相等"
print(f"v is None -> {v is None}    正确")            # is 比较对象身份，判断"是不是同一个 None 对象"


print("\n" + "=" * 60)
print("5. 类型转换与检查")
print("=" * 60)

# ---- 本段讲：类型转换、is 与 == 的区别、isinstance 用法 ----
# int()/float()/str()/bool() 是"构造函数式"转换，失败会抛 ValueError（不是返回默认值）。
print(f"int('42')         = {int('42')}")             # 字符串转整数，等价 Integer.parseInt("42")
print(f"int('1010', 2)    = {int('1010', 2)}    <- 二进制解析")   # 第二个参数是进制，Java 用 Integer.parseInt(s, 2)
print(f"float('3.14')     = {float('3.14')}")          # 字符串转浮点，等价 Double.parseDouble
print(f"str(42)           = {str(42)!r}")              # 任意对象转字符串，等价 String.valueOf / Integer.toString
print(f"bool('')          = {bool('')}")               # 空串转布尔为 False；非空串都是 True

# is 与 == 的区别（小整数驻留）
a, c = 256, 256                                       # 一行赋两个变量：右边元组 (256,256) 解包
print(f"\n256 is 256        = {a is c}     <- 驻留")  # CPython 缓存 -5~256，所以同一个对象，is 为 True
a, c = 257, 257                                       # 超出缓存范围，可能各自新建对象
print(f"257 is 257        = {a is c}    <- 不一定，取决于实现")   # 不要依赖这个结果
print(f"257 == 257        = {a == c}     <- 值比较永远用 ==")      # 判值一律 ==，判 None 才用 is

print(f"\nisinstance(42, int)         = {isinstance(42, int)}")            # 等价 Java 的 obj instanceof Integer
print(f"isinstance(True, int)       = {isinstance(True, int)}   <- bool 是 int 子类")   # True==1 的根源
print(f"isinstance('x', (str,int))  = {isinstance('x', (str, int))}")      # 第二个参数可传元组表示"任一即可"


print("\n" + "=" * 60)
print("6. 多重赋值与交换")
print("=" * 60)

# ---- 本段讲：多重赋值、交换、星号解包、数字分隔符 ----
# 核心机制：等号右边先打包成元组，左边再按位置解包，所以能一行完成交换。
p, q = 1, 2                                    # 右边是元组 (1,2)，左边两个变量分别接收
print(f"初始: p={p}, q={q}")
p, q = q, p                                    # 先算右边 (q,p) 得到 (2,1)，再赋回 p,q；Java 需要 temp
print(f"交换后: p={p}, q={q}   <- 不需要临时变量")

a, b, *rest = [1, 2, 3, 4, 5]                  # 带 * 的变量吸收"剩下的所有元素"，结果永远是 list
print(f"\na, b, *rest = [1..5]  -> a={a}, b={b}, rest={rest}")

first, *_, last = [1, 2, 3, 4]                 # _ 只是约定俗成的"丢弃变量"，Python 不会特殊处理它
print(f"first, *_, last       -> {first}, {last}")

# 下划线数字分隔符（同 Java 7+）
print(f"\n1_000_000 = {1_000_000}")            # 下划线仅为可读性，编译/解释时直接忽略
