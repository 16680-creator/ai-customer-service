"""04 · 控制流：没有 for(i)、没有 switch、有 for-else

运行: python 01_basics/04_control.py
"""

print("=" * 60)
print("1. if / elif / else + 三元表达式")
print("=" * 60)

score = 85
if score >= 90:
    grade = "A"
elif score >= 80:       # 注意不是 else if
    grade = "B"
elif score >= 60:
    grade = "C"
else:
    grade = "D"
print(f"score={score} -> grade={grade}")

# 三元：顺序与 Java 相反
label = "及格" if score >= 60 else "不及格"
print(f"三元表达式 -> {label}")

# 链式比较（Java 要写两次比较）
x = 50
print(f"0 <= {x} < 100  -> {0 <= x < 100}")


print("\n" + "=" * 60)
print("2. for 循环：只有 foreach 语义，没有 for(i=..)")
print("=" * 60)

print("for x in [10, 20, 30]:")
for v in [10, 20, 30]:
    print(f"  {v}")

print("\nfor i, v in enumerate([...]):")
for i, v in enumerate(["a", "b", "c"]):
    print(f"  {i} {v}")

print("\nfor i in range(5):")
print("  ", list(range(5)))

print("\nfor i in range(2, 10, 3):")
print("  ", list(range(2, 10, 3)))

print("\nfor i in range(5, 0, -1):")
print("  ", list(range(5, 0, -1)))

print("\nfor a, b in zip([1,2,3], 'xyz'):")
for a, b in zip([1, 2, 3], "xyz"):
    print(f"  {a} {b}")


print("=" * 60)
print("3. break / continue")
print("=" * 60)

i = 0
while i < 10:
    i += 1
    if i == 2:
        continue        # 跳过本次
    if i == 5:
        break           # 结束循环
    print(f"  i={i}")


print("=" * 60)
print("4. for-else：Java 完全没有")
print("=" * 60)

def find_first_even(nums):
    for n in nums:
        if n % 2 == 0:
            return f"找到 {n}"
    else:
        return "没有偶数"

print(f"[1,3,5]   -> {find_first_even([1, 3, 5])}")
print(f"[1,3,4,5] -> {find_first_even([1, 3, 4, 5])}")

# 注意 return 也会跳过 else，所以这个模式下 else 等价于"正常走完"
print("\n用 break-else 判断是否找到：")
for n in [1, 3, 5, 7]:
    if n % 2 == 0:
        print(f"  找到 {n}")
        break
else:
    print("  没有偶数")     # 循环没被 break 才执行


print("=" * 60)
print("5. match（3.10+，类似 Java 21 switch 模式匹配）")
print("=" * 60)

def handle(cmd):
    match cmd:
        case "start":
            return "启动"
        case "stop" | "halt":
            return "停止"
        case {"action": action}:
            return f"动作 {action}"
        case int() as n:
            return f"数字 {n}"
        case _:
            return "未知"

for c in ["start", "halt", {"action": "reload"}, 42, None]:
    print(f"  {c!r:22} -> {handle(c)}")

# 日常更常用字典分发（性能更好，也更灵活）
handlers = {
    "start": lambda: "启动",
    "stop": lambda: "停止",
}
print(f"\n字典分发 'start' -> {handlers.get('start', lambda: '未知')()}")
print(f"字典分发 'xxx'   -> {handlers.get('xxx', lambda: '未知')()}")


print("=" * 60)
print("6. 列表推导式：替代 Stream 链式调用")
print("=" * 60)

nums = list(range(1, 11))
print(f"nums                  = {nums}")
print(f"偶数                  = {[x for x in nums if x % 2 == 0]}")
print(f"偶数的平方            = {[x * x for x in nums if x % 2 == 0]}")
print(f"数字转字符串          = {[str(x) for x in nums[:5]]}")
print(f"负数变 0，正数不变    = {[x if x > 5 else 0 for x in nums]}")


print("=" * 60)
print("7. 常见控制流错误演示")
print("=" * 60)

# 错误 1：缩进错导致 return 在循环里
def wrong_sum(items):
    total = 0
    for x in items:
        total += x
        return total     # 只执行一次

def right_sum(items):
    total = 0
    for x in items:
        total += x
    return total

print(f"wrong_sum([1,2,3]) = {wrong_sum([1, 2, 3])}   <- 应该是 6")
print(f"right_sum([1,2,3]) = {right_sum([1, 2, 3])}")

# 错误 2：i++ 不存在
try:
    exec("i = 0\ni++")
except SyntaxError as e:
    print(f"\ni++ -> SyntaxError: {e.msg}   <- 只能 i += 1")
