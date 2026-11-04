"""05 · 可变性与拷贝语义（Java 程序员最容易翻车的地方）

运行: python 02_containers/05_copy.py
"""

# ============================================================
# 本文件专门讲"可变性 / 拷贝语义"—— Java 程序员最容易翻车的地方。
# 一句话结论：Python 的变量是"标签"，赋值不复制对象；要独立副本必须显式拷贝。
# 四个容易搞错的点：
#   1) b = a 共用对象；2) 浅拷贝只复制一层；3) += 原地改、+ 新建；4) 默认参数只创建一次。
# ============================================================

import copy     # deepcopy 在这个模块里；浅拷贝也可以用 list.copy() / 切片 / list()

print("=" * 60)
print("1. 变量名是标签，不是盒子")
print("=" * 60)

a = [1, 2, 3]       # 创建一个 list 对象，a 指向它
b = a               # 不复制：b 也指向同一个对象（Java 里 List b = a; 完全一样）
print(f"a = {a}, b = a")
b.append(4)         # 通过 b 修改对象本身，a 看到的是同一个对象
print(f"b.append(4) 后：a={a}  b={b}   <- 同一个对象")

# 用 id 看身份
print(f"\nid(a) == id(b)  -> {id(a) == id(b)}")   # id 是对象身份标识（类似 Java 的 System.identityHashCode）


# ------------------------------------------------------------
# 本段讲：三种等价的浅拷贝写法，以及"拷贝是独立对象"的验证。
# a.copy()、a[:]、list(a) 都新建外层列表，改副本不影响原列表。
# 注意循环里每次都往 c 追加，所以打印时原 a 始终保持 [1,2,3]。
# ------------------------------------------------------------
print("\n" + "=" * 60)
print("2. 浅拷贝的三种写法")
print("=" * 60)

a = [1, 2, 3]       # 原始列表
c1 = a.copy()       # 写法一：list.copy()
c2 = a[:]           # 写法二：全切片
c3 = list(a)        # 写法三：用内置构造函数复制

for name, c in [("a.copy()", c1), ("a[:]", c2), ("list(a)", c3)]:   # 元组列表，逐个验证
    c.append(99)    # 每个副本各追加一个 99
    print(f"{name:10} 追加后 {c}   原 a={a}")   # 三种写法下原 a 都不变

print(f"\nid 不同 -> {id(a) != id(c1)}")   # 身份不同，说明确实是新对象


# ------------------------------------------------------------
# 本段讲：浅拷贝的边界——外层独立、内层仍共享。
# nested.copy() 只复制外层列表，两个内层列表仍是同一批对象。
# 验证方式：比较外层 id（不同）和内层 id（相同），一眼看出共享发生在哪一层。
# ------------------------------------------------------------
print("=" * 60)
print("3. 浅拷贝的边界：内层还是共享的")
print("=" * 60)

nested = [[1, 2], [3, 4]]       # 二维列表
shallow = nested.copy()         # 只复制外层

print(f"nested  = {nested}")
print(f"shallow = {shallow}")

shallow[0][0] = 99              # shallow[0] 与 nested[0] 是同一个对象，所以改到了 nested
print(f"\nshallow[0][0] = 99 后：")
print(f"nested  = {nested}     <- 被改了！")
print(f"shallow = {shallow}")

print(f"\n外层 id 不同: {id(nested) != id(shallow)}")              # 外层确实是两个对象
print(f"内层 id 相同: {id(nested[0]) == id(shallow[0])}   <- 原因在这里")   # 内层是同一个对象

# deepcopy 才是完全独立
nested = [[1, 2], [3, 4]]       # 重置数据，避免受上面影响
deep = copy.deepcopy(nested)    # 递归复制每一层
deep[0][0] = 99                 # 只改副本的内层
print(f"\ndeepcopy 后改内层：nested = {nested}   不受影响")


# ------------------------------------------------------------
# 本段讲：list 的 += 与 + 语义不同——这是最容易漏的差异。
# `b += [3]` 调 __iadd__ → 原地 extend，a 和 b 一起变；
# `b = b + [3]` 调 __add__ → 新建列表再赋给 b，a 不变。
# 字符串/数字不可变，所以 += 一定产生新对象，看不出这个差异。
# ------------------------------------------------------------
print("=" * 60)
print("4. += 是原地修改，+ 是新建（list 特有）")
print("=" * 60)

a = [1, 2]          # 原始列表
b = a               # 同一个对象
b += [3]            # 原地 extend：底层直接在原列表上追加（等价 a.extend([3])）
print(f"b += [3] 后：a={a} b={b}")   # a、b 都变成 [1,2,3]

a = [1, 2]          # 重置
b = a               # 又指向同一个对象
b = b + [3]         # 先新建列表 [1,2,3]，再把名字 b 重新绑定到新对象
print(f"b = b + [3] 后：a={a} b={b}")   # a 仍是 [1,2]，只有 b 是新列表

print("\n字符串/数字没有这个差异（不可变）：")
s = "a"             # 字符串不可变
t = s               # 同一个字符串对象
t += "b"            # 不可变 → 必然创建新字符串 "ab"，t 指向新对象
print(f"t += 'b' 后：s={s!r} t={t!r}")   # !r 用 repr 输出，能看到引号，确认没被原地改

# 元组包含可变对象时，+= 会报错或行为反直觉
try:
    tpl = ([1], 2)      # 元组本身不可变，但第一个元素是可变 list
    tpl[0] += [9]       # 先原地改 list 成功，再尝试把结果写回 tpl[0] → 失败
except TypeError as e:
    print(f"\n元组里的 list 做 += -> TypeError: {e}   <- 改成功了但整体赋值失败")


# ------------------------------------------------------------
# 本段讲：函数参数是"传对象引用"（Java 常说的"值传递，值就是引用"）。
# mutate 通过引用改对象内容 → 调用方可见；
# reassign 只是让局部名字指向新对象 → 调用方无感。
# safe_mutate 先复制再改，属于"不产生副作用"的写法。
# ------------------------------------------------------------
print("=" * 60)
print("5. 函数参数：传的是对象引用")
print("=" * 60)

def mutate(lst):        # 参数是引用的副本，指向调用方的同一个 list
    lst.append(99)      # 改对象内容 → 外部可见（Java 也一样）

data = [1]              # 调用方的列表
mutate(data)            # 传入引用
print(f"mutate(data) 后 data = {data}   <- 被改了")

def reassign(lst):      # 同名参数，但这里做的是"重新绑定"
    lst = [0, 0]        # 只改了局部名字 lst，调用方的对象没动（Java 里重新赋值形参同样无效）

data2 = [1]
reassign(data2)
print(f"reassign(data2) 后 data2 = {data2}   <- 没变")

def safe_mutate(lst):   # 防御性拷贝版本
    lst = list(lst)     # 先浅拷贝一份，后续修改只影响副本
    lst.append(99)      # 改副本
    return lst          # 把新列表返回给调用方

data3 = [1]
print(f"safe_mutate 返回 {safe_mutate(data3)}，原 data3 = {data3}")


# ------------------------------------------------------------
# 本段讲：可变默认参数陷阱的完整演示，并打印 __defaults__ 佐证。
# __defaults__ 是函数对象上保存默认值的元组，可以看到那个 list 一直没变、只是在增长。
# 正确写法用 None 哨兵，每次调用内部新建列表。
# ------------------------------------------------------------
print("=" * 60)
print("6. 可变默认参数（最经典的坑）")
print("=" * 60)

def bad(item, lst=[]):      # 默认 list 在 def 时创建一次，挂在函数对象上
    lst.append(item)        # 每次调用都往同一个 list 追加
    return lst

print(f"bad(1) = {bad(1)}")
print(f"bad(2) = {bad(2)}   <- 累积了！")          # 第二次拿到的是 [1]，追加后变 [1,2]
print(f"bad.__defaults__ = {bad.__defaults__}")   # 直接看到默认值元组里的 list 已经是 [1,2]

def good(item, lst=None):   # None 不可变，做哨兵最安全
    if lst is None:         # 区分"没传"和"传了空列表"
        lst = []            # 每次调用新建，互不影响
    lst.append(item)
    return lst

print(f"\ngood(1) = {good(1)}")
print(f"good(2) = {good(2)}")


# ------------------------------------------------------------
# 本段讲：哪些类型能当 dict 键 / 放进 set —— 用 hash() 逐个试探。
# 数字、字符串、元组（元素都不可变）可哈希；list、set、dict 不可哈希。
# Java 对照：HashMap 的键要求 equals/hashCode 稳定，但可变对象照样能放进去（只是行为会诡异）。
# ------------------------------------------------------------
print("=" * 60)
print("7. 不可变 vs 可变：哪些能当 dict 键")
print("=" * 60)

candidates = [              # (标签, 待测值) 列表
    ("1", 1),
    ("'a'", "a"),
    ("(1, 2)", (1, 2)),
    ("[1, 2]", [1, 2]),
    ("{1, 2}", {1, 2}),
    ("{'a': 1}", {"a": 1}),
]
for label, v in candidates:     # 逐个测试
    try:
        hash(v)                 # 能算出哈希值就说明可作为键；不能会抛 TypeError
        print(f"{label:12} -> 可哈希")
    except TypeError:
        print(f"{label:12} -> 不可哈希（不能当 dict 键）")


# ------------------------------------------------------------
# 本段讲：实战——配置合并时避免污染默认配置。
# 浅合并 {**a, **b} 只会替换顶层键，嵌套的 db 字典仍是同一个对象；
# 正确做法是先 deepcopy 默认配置，再用 update 覆盖子项。
# 这是"多个请求共享同一份全局配置"时最常见的生产 bug。
# ------------------------------------------------------------
print("=" * 60)
print("8. 实战：配置合并时避免串改")
print("=" * 60)

default_cfg = {"db": {"host": "localhost", "port": 5432}, "debug": False}   # 全局默认配置（嵌套结构）
user_cfg = {"db": {"host": "10.0.0.1"}}                                     # 用户覆盖项

# 错误：浅合并，db 仍共享
wrong = {**default_cfg, **user_cfg}     # 顶层键被替换，但 wrong["db"] 与 user_cfg["db"] 是同一个对象
wrong["db"]["port"] = 9999              # 往这个共享 dict 里加 port
print(f"浅合并后改 db.port：default_cfg = {default_cfg}   <- 被污染")   # 默认配置里也出现了 9999

# 正确：深拷贝后再合并
default_cfg = {"db": {"host": "localhost", "port": 5432}, "debug": False}   # 重置被污染的默认配置
merged = copy.deepcopy(default_cfg)     # 深拷贝：连嵌套的 db 字典也复制一份
merged["db"].update(user_cfg["db"])     # 在副本上覆盖用户项，只改 host
print(f"深拷贝合并后：default_cfg = {default_cfg}   <- 干净")
print(f"                merged     = {merged}")
