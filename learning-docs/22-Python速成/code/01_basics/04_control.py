"""04 · 控制流：没有 for(i)、没有 switch、有 for-else

运行: python 01_basics/04_control.py
"""

print("=" * 60)
print("1. if / elif / else + 三元表达式")
print("=" * 60)

# ---- 本段讲：if / elif / else 与三元表达式 ----
# 冒号 + 缩进就是代码块，没有大括号；elif 是 else if 的合并关键字。
# 三元表达式写作 "值A if 条件 else 值B"，顺序和 Java 的 "cond ? A : B" 相反。
score = 85                       # 赋值即声明，无需 int
if score >= 90:                  # 条件后必须跟冒号
    grade = "A"                  # 4 空格缩进表示属于 if 分支
elif score >= 80:       # 注意不是 else if
    grade = "B"
elif score >= 60:
    grade = "C"
else:                            # 兜底分支，同样用冒号
    grade = "D"
print(f"score={score} -> grade={grade}")

# 三元：顺序与 Java 相反
label = "及格" if score >= 60 else "不及格"    # 先写"条件成立时的值"，再写条件，再写 else 值
print(f"三元表达式 -> {label}")

# 链式比较（Java 要写两次比较）
x = 50
print(f"0 <= {x} < 100  -> {0 <= x < 100}")   # Python 支持 a < b < c，等价 0 <= x and x < 100


print("\n" + "=" * 60)
print("2. for 循环：只有 foreach 语义，没有 for(i=..)")
print("=" * 60)

# ---- 本段讲：Python 只有 for-each，下标循环靠 enumerate / range / zip ----
# Java 的 for (int i=0; i<n; i++) 在 Python 里没有对应语法，
# 需要下标就 enumerate，需要数字序列就 range，需要并行遍历就 zip。
print("for x in [10, 20, 30]:")
for v in [10, 20, 30]:           # 直接遍历元素，等价 Java 的 for (int v : list)
    print(f"  {v}")              # f-string 里两个空格是字面缩进，用于输出对齐

print("\nfor i, v in enumerate([...]):")
for i, v in enumerate(["a", "b", "c"]):   # enumerate 同时给"下标 + 元素"，等价 for (int i=0; ...) list.get(i)
    print(f"  {i} {v}")

print("\nfor i in range(5):")
print("  ", list(range(5)))      # range(5) 是惰性序列 0..4；list() 转成列表才能直接打印

print("\nfor i in range(2, 10, 3):")
print("  ", list(range(2, 10, 3)))   # 起点 2、终点 10（不含）、步长 3 → 2,5,8

print("\nfor i in range(5, 0, -1):")
print("  ", list(range(5, 0, -1)))   # 步长为负表示倒序，终点 0 不包含

print("\nfor a, b in zip([1,2,3], 'xyz'):")
for a, b in zip([1, 2, 3], "xyz"):    # zip 把两个序列"拉链式"配对，长度以短的为准
    print(f"  {a} {b}")


print("=" * 60)
print("3. break / continue")
print("=" * 60)

# ---- 本段讲：while + continue + break 的执行顺序 ----
# 关键点：没有 i++，自增必须写 i += 1；且 continue 前一定要先自增，否则会死循环。
i = 0
while i < 10:                    # 条件为真就继续，等价 Java 的 while
    i += 1                       # 先自增再判断，等价 Java 的 ++i；Python 没有 ++ 运算符
    if i == 2:
        continue        # 跳过本次
    if i == 5:
        break           # 结束循环
    print(f"  i={i}")


print("=" * 60)
print("4. for-else：Java 完全没有")
print("=" * 60)

# ---- 本段讲：for-else —— 循环"正常走完"才执行 else ----
# 等价于 Java 里用一个 boolean found 标志 + 循环后判断，Python 把它内建了。
# 注意：循环里用 return 退出，同样不会执行 else。
def find_first_even(nums):       # 形参不写类型也行，nums 是任意可迭代对象
    for n in nums:
        if n % 2 == 0:           # % 取余，偶数余数为 0
            return f"找到 {n}"    # 找到就立即返回，后面的 else 分支被跳过
    else:
        return "没有偶数"          # 只有循环"自然结束"（没有 break/return）才会走到这里

print(f"[1,3,5]   -> {find_first_even([1, 3, 5])}")     # 无偶数 → 走 else
print(f"[1,3,4,5] -> {find_first_even([1, 3, 4, 5])}")   # 有偶数 → 直接 return

# 注意 return 也会跳过 else，所以这个模式下 else 等价于"正常走完"
print("\n用 break-else 判断是否找到：")
for n in [1, 3, 5, 7]:
    if n % 2 == 0:
        print(f"  找到 {n}")
        break                    # 一旦 break，else 整块都不执行
else:
    print("  没有偶数")     # 循环没被 break 才执行


print("=" * 60)
print("5. match（3.10+，类似 Java 21 switch 模式匹配）")
print("=" * 60)

# ---- 本段讲：match / case 结构化模式匹配（3.10+）----
# 对标 Java 21 的 switch 模式匹配；case 后面可以直接写"结构"（如字典、类型）。
# 通配符 _ 是兜底分支，等价 Java switch 的 default。
def handle(cmd):                 # 定义函数；cmd 是待匹配的任意对象
    match cmd:                   # match 后面跟要匹配的值
        case "start":            # 字面量匹配
            return "启动"
        case "stop" | "halt":    # | 表示"或"，一个分支匹配多个值
            return "停止"
        case {"action": action}: # 字典结构匹配：只要含 action 键就命中，并把它绑定到 action
            return f"动作 {action}"
        case int() as n:         # 类型匹配：是 int 就命中，并绑定为 n
            return f"数字 {n}"
        case _:                  # 兜底，等价 default
            return "未知"

for c in ["start", "halt", {"action": "reload"}, 42, None]:   # 逐个类型都试一遍
    print(f"  {c!r:22} -> {handle(c)}")                       # !r 加 :22 左对齐，让输出整齐

# 日常更常用字典分发（性能更好，也更灵活）
handlers = {
    "start": lambda: "启动",     # 值是 lambda（无参匿名函数），等价 Java 的 Supplier
    "stop": lambda: "停止",
}
print(f"\n字典分发 'start' -> {handlers.get('start', lambda: '未知')()}")   # get 取函数，末尾 () 立即调用
print(f"字典分发 'xxx'   -> {handlers.get('xxx', lambda: '未知')()}")     # 键不存在时返回默认 lambda，不会 KeyError


print("=" * 60)
print("6. 列表推导式：替代 Stream 链式调用")
print("=" * 60)

# ---- 本段讲：列表推导式，替代 Java Stream 的 map/filter ----
# 语法：[表达式 for 元素 in 可迭代对象 if 条件]，可读性远高于链式调用。
# 注意 if 在 for 之后是"过滤"，如果写在 for 之前则是"三元取值"。
nums = list(range(1, 11))        # range 是惰性对象，list() 物化成 [1..10]
print(f"nums                  = {nums}")
print(f"偶数                  = {[x for x in nums if x % 2 == 0]}")               # 过滤：等价 filter(x -> x%2==0)
print(f"偶数的平方            = {[x * x for x in nums if x % 2 == 0]}")           # 过滤 + 映射：filter 后再 map
print(f"数字转字符串          = {[str(x) for x in nums[:5]]}")                    # 切片取前 5 个，再逐个 str
print(f"负数变 0，正数不变    = {[x if x > 5 else 0 for x in nums]}")              # if 在 for 前 → 三元表达式，不过滤


print("=" * 60)
print("7. 常见控制流错误演示")
print("=" * 60)

# ---- 本段讲：两个新手最常犯的语法/缩进错误 ----
# 错误 1 是缩进导致 return 落进了循环体，函数只累加第一个元素就返回；
# 错误 2 演示 Java 的 i++ 在 Python 里直接是语法错误。
# 错误 1：缩进错导致 return 在循环里
def wrong_sum(items):
    total = 0                    # 累加器初值；Java 要写 int total = 0;
    for x in items:
        total += x               # += 累加，等价 total = total + x
        return total     # 只执行一次

def right_sum(items):
    total = 0
    for x in items:
        total += x               # 循环内只累加，不返回
    return total                 # 缩进回到函数体，等循环结束再返回

print(f"wrong_sum([1,2,3]) = {wrong_sum([1, 2, 3])}   <- 应该是 6")   # 实际输出 1
print(f"right_sum([1,2,3]) = {right_sum([1, 2, 3])}")

# 错误 2：i++ 不存在
try:                             # 用 try 捕获，避免演示代码直接崩掉
    exec("i = 0\ni++")           # exec 把字符串当代码执行；i++ 在这里会触发编译期错误
except SyntaxError as e:         # 语法错误是 SyntaxError，对应 Java 的编译错误
    print(f"\ni++ -> SyntaxError: {e.msg}   <- 只能 i += 1")   # e.msg 是简要错误说明
