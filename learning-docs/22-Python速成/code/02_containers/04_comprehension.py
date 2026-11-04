"""04 · 推导式：替代 Stream 链式调用

运行: python 02_containers/04_comprehension.py
"""

# ============================================================
# 本文件演示推导式：用一行表达式替代 Java 的 Stream 链。
# 统一模板：[表达式 for 变量 in 可迭代对象 if 条件]
# 返回类型由最外层括号决定：[] 列表、{} 字典/集合、() 生成器。
# 阅读重点：末尾的 if 是"过滤"，开头的 if-else 是"表达式"，两者位置不同、含义不同。
# ============================================================

import sys      # getsizeof：后面用来对比列表与生成器的内存

print("=" * 60)
print("1. 列表推导式")
print("=" * 60)

nums = list(range(10))      # [0..9]，作为后续各种推导式的输入
print(f"nums                = {nums}")
print(f"映射 x*2            = {[x * 2 for x in nums]}")          # 每个元素映射成 x*2，等价 Stream.map
print(f"过滤偶数            = {[x for x in nums if x % 2 == 0]}")   # 尾部 if 决定元素是否保留
print(f"映射+过滤           = {[x * x for x in nums if x % 2 == 0]}")   # 先筛偶数再平方
print(f"条件表达式          = {[x if x < 5 else 0 for x in nums]}")     # 开头 if-else：每个元素都保留，只是值二选一
print(f"嵌套 flatMap        = {[x for row in [[1, 2], [3, 4], [5]] for x in row]}")   # 两层 for，等价 flatMap
print(f"字符串处理          = {[w.strip().upper() for w in ['  a ', 'b  ', ' c']]}")   # 链式调用：先去空白再转大写

# Java 对照：
#   IntStream.range(0,10).filter(x->x%2==0).map(x->x*x).boxed().toList()


# ------------------------------------------------------------
# 本段讲：字典推导式的四种常见形态。
# 形态固定为 {键表达式: 值表达式 for ...}；反转键值、变换键值、过滤、从两列表构造都靠它。
# 注意：字典键必须可哈希，所以翻转 {k: v} 时要求 v 可哈希。
# ------------------------------------------------------------
print("\n" + "=" * 60)
print("2. 字典推导式")
print("=" * 60)

print(f"反转键值            = {{v: k for k, v in {{'a': 1, 'b': 2}}.items()}}")   # k,v 互换位置；外层花括号要转义
print(f"键值变换            = {{k: len(k) for k in ['a', 'bb', 'ccc']}}")          # 键不变，值用表达式算出来
print(f"过滤                = {{k: v for k, v in {{'a': 1, 'b': 2, 'c': 3}}.items() if v > 1}}")   # 只保留 value > 1
print(f"从列表构造          = {{n: n * n for n in range(4)}}")                     # 数字 → 平方
print(f"两个列表            = {{k: v for k, v in zip(['x', 'y'], [1, 2])}}")       # zip 配对后直接建字典


# ------------------------------------------------------------
# 本段讲：集合推导式 —— 把 [] 换成 {}，元素自动去重，结果无序。
# 适合"算出一组唯一值"的场景，比如统计出现过的余数、单词长度种类。
# Java 对照：collect(Collectors.toSet())。
# ------------------------------------------------------------
print("=" * 60)
print("3. 集合推导式")
print("=" * 60)

print(f"余数集合            = {{x % 3 for x in range(10)}}")               # 0~9 对 3 取余，只有 0/1/2 三种结果
print(f"去重长度            = {{len(w) for w in ['a', 'bb', 'cc', 'ddd']}}")   # 长度 1/2/3，重复的 2 被合并


# ------------------------------------------------------------
# 本段讲：生成器表达式与列表推导式的内存差异。
# 语法只差一对括号：() 是惰性生成器，[] 是立即求值的列表。
# 生成器大小与 n 无关，适合大数据量 / 流式处理。
# ------------------------------------------------------------
print("=" * 60)
print("4. 生成器表达式：惰性，省内存")
print("=" * 60)

n = 1_000_000               # 一百万；下划线只是可读性分隔符

list_comp = [x * x for x in range(n)]   # 立即算完并全部驻留内存
gen_expr = (x * x for x in range(n))    # 只创建生成器对象，一个平方都没算

print(f"列表推导式内存      {sys.getsizeof(list_comp):>10,} 字节")   # >10, 右对齐并加千分位
print(f"生成器表达式内存    {sys.getsizeof(gen_expr):>10,} 字节   <- 与 n 无关")

del list_comp   # 释放：删掉名字后，引用计数归零，大列表被回收（Java 要等 GC）

print(f"\nsum(x*x for x in range(1_000_000)) = {sum(x * x for x in range(1_000_000)):,}")   # 生成器直接喂给 sum，不建中间列表

# 生成器只能遍历一次
g = (x for x in range(3))   # 新建生成器
print(f"\nsum(g) 第一次 = {sum(g)}")     # 3：0+1+2，遍历到耗尽
print(f"sum(g) 第二次 = {sum(g)}   <- 已耗尽")   # 0：空序列求和为 0，不会重置


# ------------------------------------------------------------
# 本段讲：可读性边界——单行推导式 vs 显式循环，两种写法产出同一个结果。
# 当条件超过两个时，推导式的"条件归属"会变模糊，拆成循环更易维护。
# 注意 ugly 版本用 and 连接两个条件，result 版本用两个 continue，语义完全等价。
# ------------------------------------------------------------
print("=" * 60)
print("5. 什么时候不该用推导式（可读性优先）")
print("=" * 60)

data = [                    # 三条业务记录：部门、金额、是否有效
    {"dept": "x", "amount": 100, "valid": True},
    {"dept": "y", "amount": 200, "valid": False},
    {"dept": "x", "amount": 300, "valid": True},
]

# 过度嵌套：一行写完但难读
ugly = [r["amount"] * 2 for r in data if r["valid"] and r["dept"] == "x"]   # 两个条件用 and 串起来
print(f"推导式（还能接受）  {ugly}")

# 更复杂的逻辑就该拆开
result = []                 # 显式累加器
for r in data:              # 逐条处理
    if not r["valid"]:      # 过滤条件一：无效记录直接跳过
        continue            # continue 等价 Java 的 continue，进入下一条
    if r["dept"] != "x":    # 过滤条件二：非 x 部门跳过
        continue
    result.append(r["amount"] * 2)   # 两条都通过才处理
print(f"拆成循环            {result}")


# ------------------------------------------------------------
# 本段讲：实测推导式比 for + append 快。
# 原因：推导式在 CPython 里有专门的字节码优化（LIST_APPEND），省掉属性查找和函数调用。
# 手法：同一份数据跑两种写法，用 perf_counter 计时对比。
# ------------------------------------------------------------
print("=" * 60)
print("6. 实测：推导式比 for-append 快")
print("=" * 60)

import time

N = 500_000                 # 数据规模

t0 = time.perf_counter()    # 开始计时第一种写法
out = []                    # 手动累加列表
for i in range(N):
    out.append(i * 2)       # 每次循环都要查 out.append 属性并调用函数
t_loop = time.perf_counter() - t0

t0 = time.perf_counter()    # 开始计时第二种写法
out2 = [i * 2 for i in range(N)]   # 一次表达式完成，解释器有专门优化
t_comp = time.perf_counter() - t0

print(f"for + append   {t_loop*1000:7.2f}ms")    # 转毫秒，宽度 7、两位小数
print(f"推导式         {t_comp*1000:7.2f}ms")
print(f"快 {t_loop/max(t_comp,1e-9):.2f} 倍")     # max 防止除零


# ------------------------------------------------------------
# 本段讲：三个高频实战推导式——展平、转置、建索引映射。
# 展平：两层 for；转置：zip(*matrix) 更地道；索引映射：enumerate 同时给出下标和值。
# Java 对照：flatMap / 手写双重循环 / IntStream.range + boxed。
# ------------------------------------------------------------
print("=" * 60)
print("7. 实战：三个高频推导式")
print("=" * 60)

# ① 展平二维
matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]     # 3x3 矩阵
print(f"展平        {[x for row in matrix for x in row]}")   # 先遍历行，再遍历行内元素

# ② 转置（zip 更地道）
print(f"转置(zip)   {[list(t) for t in zip(*matrix)]}")     # *matrix 解包成三行，zip 按列配对；list(t) 把元组转成列表

# ③ 建索引映射
items = ["apple", "banana", "cherry"]   # 有序名称列表
print(f"索引映射    {{v: i for i, v in enumerate(items)}} = { {v: i for i, v in enumerate(items)} }")   # enumerate 产出 (下标, 值)，反过来当 值→下标
