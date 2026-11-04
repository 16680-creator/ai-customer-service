"""02 · dict：用得比 list 还多

运行: python 02_containers/02_dict.py
"""

# ============================================================
# 本文件演示 dict（对应 Java 的 LinkedHashMap）的常见操作。
# 阅读重点：
#   1) 取值优先用 get(k, 默认值)，[] 取不到会抛 KeyError；
#   2) defaultdict 能省掉"判断键是否存在"的分支；
#   3) 遍历中修改大小会抛 RuntimeError，要先复制 key 列表。
# ============================================================

from collections import defaultdict, Counter   # defaultdict：缺键自动初始化；Counter：现成的计数器

print("=" * 60)
print("1. 创建")
print("=" * 60)

print(f"{{}}                            -> {{}}")          # 在 f-string 里写花括号要写两次 {{ }} 转义，否则会被当成插值
print(f'{{"a": 1}}                       -> {{"a": 1}}')  # 单引号包外层，内部就能直接写双引号
print(f"dict(a=1, b=2)                -> {dict(a=1, b=2)}")           # 关键字构造：键自动变成字符串 "a"/"b"
print(f"dict([('a',1),('b',2)])       -> {dict([('a', 1), ('b', 2)])}")   # 从 (键, 值) 二元组序列构造
print(f"dict(zip(['a','b'],[1,2]))    -> {dict(zip(['a', 'b'], [1, 2]))}")   # zip 按位置配对，最常用的建字典方式
print(f"dict.fromkeys(['a','b'], 0)   -> {dict.fromkeys(['a', 'b'], 0)}")   # 批量建键；值若可变会所有键共享
print(f"{{k: len(k) for k in ['a','bb']}} -> {{k: len(k) for k in ['a', 'bb']}}")   # 字典型推导式：键 = 原元素，值 = 长度


print("\n" + "=" * 60)
print("2. 取值：永远优先 .get()")
print("=" * 60)

# ------------------------------------------------------------
# 本段讲：[] 与 get() 的差别，以及"or 给默认值"的陷阱。
# Java 的 Map.get 取不到返回 null；Python 的 d[k] 取不到直接抛 KeyError。
# 所以只要键可能不存在，就用 d.get(k, 默认值)。
# ------------------------------------------------------------
d = {"a": 1}        # 单键字典

print(f'd["a"]           = {d["a"]}')     # 键存在，下标语法安全
try:                # Python 的异常捕获是 try/except，不是 try/catch
    d["missing"]    # 键不存在 → 抛 KeyError；这一行故意触发异常给下一行演示
except KeyError as e:                   # as e 绑定异常对象（Java: catch (KeyError e)）
    print(f'd["missing"]     -> KeyError: {e}   <- Java 的 Map.get 返回 null，这里直接炸')

print(f'd.get("missing")        = {d.get("missing")}')       # 缺键返回 None（Python 的空引用）
print(f'd.get("missing", 0)     = {d.get("missing", 0)}')    # 第二个参数是兜底值，等价 Optional.orElse

# or 做默认值的陷阱
d2 = {"count": 0}   # 键存在，值是 0
print(f"\nd2.get('count') or 100  = {d2.get('count') or 100}   <- 0 被当假值")   # 0 是假值 → 返回 100，逻辑错了
print(f"d2.get('count', 100)    = {d2.get('count', 100)}   <- 正确")            # 键存在就返回 0，与真假无关


print("=" * 60)
print("3. 增删改")
print("=" * 60)

# ------------------------------------------------------------
# 本段讲：增删改（赋值即插入/覆盖）、setdefault 分组、pop 安全删除。
# setdefault(k, 默认值) 是"键不存在则写入并返回"的原子操作，等价 Java 的 computeIfAbsent。
# pop(k, 默认值) 是安全删除：键不存在也不报错，适合清理可选字段。
# ------------------------------------------------------------
d = {}                  # 空字典
d["a"] = 1              # 键不存在 → 插入；不需要 Java 的 put
d["a"] = 2              # 键已存在 → 覆盖，不报错
print(f'覆盖 -> {d}')     # {'a': 2}

d.setdefault("b", [])     # 键 "b" 缺失 → 写入 []，并返回这个空列表（此处没接收返回值）
d.setdefault("b", []).append(1)     # 第二次调用键已存在 → 直接返回已有列表，因此 append 到同一个列表
print(f'setdefault 分组 -> {d}')      # {'a': 2, 'b': [1]}

d.update({"c": 3, "d": 4})   # 批量写入，等价 putAll；重复键会被新值覆盖
print(f'update -> {d}')

print(f'pop("a") -> {d.pop("a")}   剩余 {d}')            # pop 删除并返回值，等价 remove 但有返回值
print(f'pop("zzz", None) -> {d.pop("zzz", None)}   <- 安全删除')   # 键不存在时返回默认值 None，不抛异常

del d["b"]                # del 是语句：删除键，不存在会抛 KeyError（比 pop 更激进）
print(f'del d["b"] -> {d}')

d.clear()                 # 清空所有键值
print(f"clear -> {d}")


# ------------------------------------------------------------
# 本段讲：字典合并的三种写法（|、{**a, **b}、update）。
# `a | b` 返回新字典（3.9+）；`a.update(b)` 原地修改；`{**a, **b}` 是更早版本就有的写法。
# 三者都是"右边覆盖左边"。
# ------------------------------------------------------------
print("=" * 60)
print("4. 合并（3.9+）")
print("=" * 60)

a = {"x": 1, "y": 2}      # 左字典
b = {"y": 20, "z": 30}    # 右字典，与 a 有重复键 "y"
print(f"a | b       = {a | b}      <- 后者覆盖前者，返回新字典")   # 合并运算符：Python 独有的语法糖
print(f"a 未被修改  = {a}")                                      # 验证 | 不修改操作数
print(f"{{**a, **b}}  = {{**a, **b}}")                            # 解包合并：把两个字典的键值都展开到新字面量里

a.update(b)               # 原地合并：把 b 的键值写进 a，重复键覆盖
print(f"a.update(b) = {a}      <- 原地修改")


# ------------------------------------------------------------
# 本段讲：遍历 dict 的三种视图，以及"遍历中删除"的正确姿势。
# keys()/values()/items() 返回的是视图对象，不是列表，所以用 list() 包一层才方便打印。
# 直接边遍历边删会改大小 → RuntimeError；先 list(d.keys()) 做快照即可。
# ------------------------------------------------------------
print("=" * 60)
print("5. 遍历")
print("=" * 60)

d = {"a": 1, "b": 2, "c": 3}     # 三个键值对
print(f"keys()   -> {list(d.keys())}")     # 键视图 → 列表；等价 keySet()
print(f"values() -> {list(d.values())}")   # 值视图 → 列表；等价 values()
print(f"items()  -> {list(d.items())}")    # (键, 值) 二元组视图；等价 entrySet()

print("\nfor k, v in d.items():")
for k, v in d.items():           # 每轮产出 (k, v) 元组，循环变量自动解包
    print(f"  {k} = {v}")        # 两个空格缩进只为输出好看

# 遍历时改大小会报错
d2 = {"a": 1, "b": 2}
try:
    for k in d2:                 # 直接遍历字典 → 拿到的其实是键的迭代器
        del d2[k]                # 第一轮删掉 "a" 后字典大小变化，下一轮迭代就抛错
except RuntimeError as e:        # 等价 Java 的 ConcurrentModificationException
    print(f"\n遍历时删除 -> RuntimeError: {e}")

d2 = {"a": 1, "b": 2}            # 重新构造，保证有数据可删
for k in list(d2.keys()):        # list(...) 生成键的快照，遍历的是这个新列表
    del d2[k]                    # 删原字典不影响快照的迭代
print(f"先复制 keys -> {d2}")


# ------------------------------------------------------------
# 本段讲：三种分组统计写法，全部基于 defaultdict，一趟循环搞定。
# totals：键 → 数值累加；grouped：键 → 列表收集；acc：键 → 复合 dict（计数值 + 合计）。
# Java 对照：Collectors.groupingBy / groupingBy + reducing，通常要写好几层收集器。
# ------------------------------------------------------------
print("=" * 60)
print("6. 分组统计（业务最高频）")
print("=" * 60)

orders = [                       # 原始订单流水
    {"type": "A", "amount": 100},
    {"type": "B", "amount": 200},
    {"type": "A", "amount": 300},
    {"type": "B", "amount": 50},
]

# ① defaultdict 累加
totals = defaultdict(float)      # 缺键时自动创建 0.0（float() 的返回值）
for o in orders:
    totals[o["type"]] += o["amount"]    # 等价 totals[t] = totals.get(t, 0) + amount；省掉判空分支
print(f"分组求和     {dict(totals)}")   # dict(...) 转普通字典，输出更干净

# ② defaultdict 收集（等价 Collectors.groupingBy）
grouped = defaultdict(list)      # 缺键时自动创建空列表
for o in orders:
    grouped[o["type"]].append(o["amount"])   # 直接 append，不必先判断键是否存在
print(f"分组收集     {dict(grouped)}")

# ③ 复合结构：分组 + count + total
acc = defaultdict(lambda: {"count": 0, "total": 0.0})   # 工厂是 lambda：每次新建一份"统计槽"，避免所有键共享同一个 dict
for o in orders:
    slot = acc[o["type"]]            # 取出该分类的累加槽（顺便完成初始化）
    slot["count"] += 1               # 计数
    slot["total"] += o["amount"]     # 求和
print(f"分组统计     {dict(acc)}")


# ------------------------------------------------------------
# 本段讲：Counter —— 专门用来计数的 dict 子类。
# 直接传字符串/列表即可统计频次，most_common 取 TopN，elements 按计数展开。
# Counter 还支持加减运算，做"差异统计"很方便。
# ------------------------------------------------------------
print("=" * 60)
print("7. Counter：计数专用")
print("=" * 60)

c = Counter("hello world")           # 逐字符统计，空格也算一个字符
print(f"Counter('hello world')     {dict(c)}")          # 转 dict 打印：{'h':1,'e':1,'l':3,...}
print(f"most_common(3)             {c.most_common(3)}")   # 返回按次数降序的 [(元素, 次数)] 前 3 个
print(f"elements()                 {list(c.elements())[:6]}")   # 按计数展开成迭代器，切片只看前 6 个

c2 = Counter(["a", "b", "a"])        # {'a': 2, 'b': 1}
c3 = Counter(["a"])                  # {'a': 1}
print(f"\nCounter 加法   {c2 + c3}")   # 逐键相加，结果 {'a': 3, 'b': 1}
print(f"Counter 减法   {c2 - c3}")     # 逐键相减，<=0 的键会被丢弃，结果 {'a': 1, 'b': 1}


# ------------------------------------------------------------
# 本段讲：反转字典（值 → 键）。
# 因为多个键可能有同一个值，所以结果是"值 → 键列表"，用 defaultdict(list) 收集。
# Java 里同样要 Map<V, List<K>> + computeIfAbsent。
# ------------------------------------------------------------
print("=" * 60)
print("8. 反转字典（值访问键）")
print("=" * 60)

d = {"a": 1, "b": 2, "c": 1}     # 注意 "a" 和 "c" 的值都是 1
reversed_map = defaultdict(list) # 反转后的容器：一个值对应多个键
for k, v in d.items():           # 遍历原始键值对
    reversed_map[v].append(k)    # 以"值"为新键，把原键塞进列表
print(f"原字典 {d}")
print(f"反转   {dict(reversed_map)}   <- 一对多要收成 list")
