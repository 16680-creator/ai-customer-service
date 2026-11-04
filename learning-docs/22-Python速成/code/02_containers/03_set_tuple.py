"""03 · set / tuple / frozenset

运行: python 02_containers/03_set_tuple.py
"""

# ============================================================
# 本文件演示 set / tuple / frozenset 三兄弟：
#   set       —— 去重 + O(1) 成员判断（对应 Java HashSet）
#   tuple     —— 不可变序列，可哈希、可解包（Java 无等价物）
#   frozenset —— 不可变集合，因此能当 dict 键（Java 的 unmodifiableSet 不行）
# 阅读重点：哪些对象能进 set / 当键，取决于它是否可哈希。
# ============================================================

import sys                  # getsizeof 用来对比各容器的内存占用
from typing import NamedTuple   # 具名元组：带字段名的 tuple 子类

print("=" * 60)
print("1. set：去重 + O(1) 成员判断")
print("=" * 60)

s = {1, 2, 3}               # 集合字面量：花括号里只写值（没有冒号）
print(f"{{1,2,3}}      -> {s}")                       # 打印顺序不保证，set 是无序的
print(f"set([1,2,2,3]) -> {set([1, 2, 2, 3])}")        # 用可迭代对象构造，自动去重
print(f"set()          -> {set()}     <- 空集合不能用 {{}}")   # 空集合只能 set()；{} 是空字典
print(f"type({{}})       -> {type({}).__name__}    <- 空大括号是 dict！")   # __name__ 取类型名字符串

s.add(4)                    # 添加元素；已存在则无操作，等价 HashSet.add
print(f"\nadd(4)         -> {s}")
s.discard(4)                # 安全删除：不存在也不报错
print(f"discard(4)     -> {s}    <- 不存在也不报错")
try:                        # 演示 remove 的严格行为
    s.remove(99)            # 99 不在集合里 → 抛 KeyError
except KeyError as e:       # 捕获并打印，避免脚本中断
    print(f"remove(99)     -> KeyError: {e}   <- 不存在会抛")

a, b = {1, 2, 3}, {2, 3, 4}     # 同一行解包赋值两个集合
print(f"\na={a}, b={b}")
print(f"a | b  并集    {a | b}")     # union：两边合并去重
print(f"a & b  交集    {a & b}")     # intersection：两边都有的
print(f"a - b  差集    {a - b}")     # difference：在 a 不在 b 的
print(f"a ^ b  对称差  {a ^ b}")     # symmetric_difference：只出现在一边的
is_subset = {1, 2} <= a         # 子集判断：左边元素是否全在右边
print(f"子集判断 {{1,2}} <= a  -> {is_subset}")


# ------------------------------------------------------------
# 本段讲：实测 list 与 set 的成员判断性能差距。
# 手法：用 time.perf_counter 分别计时 200 次 in 操作，再算倍数。
# 结论：list 是 O(n) 线性扫描，set 是 O(1) 哈希查找，数据量大时差距是数量级的。
# ------------------------------------------------------------
print("\n" + "=" * 60)
print("2. 为什么用 set 而不是 list 判断存在")
print("=" * 60)

import time                 # 计时模块

big_list = list(range(200_000))     # 20 万个元素；下划线是数字分隔符，等价 200000
big_set = set(big_list)             # 转成 set，建表本身要花时间，但查询变快

t0 = time.perf_counter()            # 开始计时（高精度单调时钟）
for _ in range(200):                # 重复 200 次取平均效果；_ 表示"这个循环变量用不到"
    199_999 in big_list             # 每次都线性扫到末尾才找到，最坏情况
t_list = time.perf_counter() - t0   # 结束计时，差值即总耗时

t0 = time.perf_counter()
for _ in range(200):
    199_999 in big_set              # 哈希定位，一次命中
t_set = time.perf_counter() - t0

print(f"200 次 'in list' 耗时 {t_list*1000:.2f}ms   O(n)")     # 乘 1000 换成毫秒
print(f"200 次 'in set'  耗时 {t_set*1000:.4f}ms   O(1)")     # set 太快，保留 4 位小数
print(f"快 {t_list/max(t_set, 1e-9):.0f} 倍")                  # max 防止除以 0（计时可能极端接近 0）


# ------------------------------------------------------------
# 本段讲：tuple 的不可变性、单元素逗号、函数返回多值、可哈希当键。
# 关键记忆点：(1) 是 int，(1,) 才是元组——括号不是元组的标志，逗号才是。
# Java 对照：返回多值要自定义 DTO 或 Object[]；元组当键就是 Map 的复合键。
# ------------------------------------------------------------
print("=" * 60)
print("3. tuple：不只是不可变 list")
print("=" * 60)

t = (1, 2, 3)               # 三元组
print(f"t            = {t}")
print(f"t[0]         = {t[0]}")     # 按下标读，与 list 一致
try:
    t[0] = 9                # 元组没有 __setitem__，赋值会失败
except TypeError as e:      # 捕获类型错误并打印原因
    print(f"t[0] = 9     -> TypeError: {e}")

print(f"\n(1) 类型     -> {type((1)).__name__}     <- 不是 tuple！")   # (1) 只是加了括号的 int
print(f"(1,) 类型    -> {type((1,)).__name__}")                        # 逗号才让它成为元组

# 函数返回多值
def divmod2(a, b):              # 同时返回商和余数
    return a // b, a % b        # 逗号分隔 → 实际返回 tuple；(商, 余数)

q, r = divmod2(7, 2)            # 解包接收两个返回值，q=3、r=1
print(f"\ndivmod2(7,2) -> q={q}, r={r}")

# 可哈希，能当 dict 键
cache = {(1, 2): "value"}       # 字面量字典，键是元组（复合键）
print(f"cache[(1,2)] -> {cache[(1, 2)]}")   # 用同样的元组取值：相等且哈希相同，能命中


# ------------------------------------------------------------
# 本段讲：NamedTuple —— 给 tuple 的每个位置起名字。
# 它仍然是 tuple，所以支持下标访问、解包、可哈希；额外提供 .x/.y 命名访问和 _replace。
# Java 对照：record Point(int x, int y)，但 record 不是"元组"，不能按下标访问。
# ------------------------------------------------------------
print("=" * 60)
print("4. NamedTuple：Java record 的表亲")
print("=" * 60)

class Point(NamedTuple):    # 继承 NamedTuple，字段顺序即元组下标顺序
    x: int                  # 下标 0，名字 x
    y: int                  # 下标 1，名字 y

p = Point(1, 2)             # 构造：位置参数按字段顺序填
print(f"p              = {p}")                 # repr 形如 Point(x=1, y=2)，可读性比普通元组好
print(f"p.x, p.y       = {p.x}, {p.y}     <- 命名访问")   # 属性访问，IDE 可补全
print(f"p[0]           = {p[0]}       <- 位置访问")       # 底层仍是 tuple，也支持下标
x, y = p                    # 解包成两个变量
print(f"解包           = {x}, {y}")
print(f"p._replace(x=9)= {p._replace(x=9)}")    # 不可变对象"改字段"：返回新实例，原 p 不变
print(f"p._asdict()    = {p._asdict()}")        # 转成有序字典（3.8+ 返回普通 dict）


# ------------------------------------------------------------
# 本段讲：frozenset —— 不可变集合，所以可哈希、能当字典键。
# 可变对象不能当键的原因是：内容变了哈希就变了，哈希表会找不到它。
# Java 的 Collections.unmodifiableSet 只是"视图不可改"，底层仍可变，所以依然不能安全当键。
# ------------------------------------------------------------
print("=" * 60)
print("5. frozenset：可哈希的集合")
print("=" * 60)

fs = frozenset({1, 2, 3})   # 从普通 set 构造不可变版本
print(f"fs             = {fs}")
print(f"能作 dict 键   -> {{frozenset({{1,2}}): 'v'}} = {{frozenset({1, 2}): 'v'}}")   # 直接当键演示
try:
    fs.add(4)               # frozenset 没有 add 方法
except AttributeError as e:  # 属性不存在 → AttributeError，而不是 TypeError
    print(f"fs.add(4)      -> AttributeError: {e}")


# ------------------------------------------------------------
# 本段讲：四种容器的内存占用与类型对比。
# samples 是一个"标签 → 容器"的字典，遍历它统一打印，避免重复代码。
# 注意：getsizeof 只算容器自身，不含元素对象，所以 tuple 反而比 list 小。
# ------------------------------------------------------------
print("=" * 60)
print("6. 四种容器的内存与特性对比")
print("=" * 60)

samples = {                     # 键是展示用的标签，值是要测量的容器对象
    "list    [1,2,3]": [1, 2, 3],
    "tuple   (1,2,3)": (1, 2, 3),
    "set     {1,2,3}": {1, 2, 3},
    "dict    {'a':1}": {"a": 1},
}
for label, obj in samples.items():      # 同时拿到标签和对象
    print(f"{label:20} 内存 {sys.getsizeof(obj):4d} 字节  类型 {type(obj).__name__}")   # :20 左对齐，:4d 右对齐整数


# ------------------------------------------------------------
# 本段讲：实战——按"多字段组合"去重。
# 把多个字段拼成元组当唯一键，再用 set 判重；这是最常用的业务去重写法。
# Java 对照：用 record/自定义 Key 对象配合 HashSet，或者拼字符串当 key（容易有分隔符冲突）。
# ------------------------------------------------------------
print("=" * 60)
print("7. 实战：按多重键去重")
print("=" * 60)

records = [                     # 三条记录，前两条完全重复
    {"order_no": "A", "sku": "S1"},
    {"order_no": "A", "sku": "S1"},
    {"order_no": "A", "sku": "S2"},
]
seen = set()                    # 已出现过的 (order_no, sku) 组合
unique = []                     # 保序输出去重结果
for r in records:
    key = (r["order_no"], r["sku"])     # 元组当复合键：可哈希，且比较是逐字段的
    if key not in seen:                 # O(1) 判断是否重复
        seen.add(key)                   # 记录该组合
        unique.append(r)                # 保留第一次出现的整条记录
print(f"原始 {len(records)} 条 -> 去重 {len(unique)} 条")   # len 等价 size()
print(f"{unique}")
