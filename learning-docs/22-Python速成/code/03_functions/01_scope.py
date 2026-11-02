"""01 · 作用域 LEGB

运行: python 03_functions/01_scope.py
"""

x = "global-x"

print("=" * 60)
print("1. LEGB 查找顺序")
print("=" * 60)

def outer():
    x = "enclosing-x"

    def inner():
        # x = "local-x"
        print(f"  inner 看到 x = {x}")

    inner()
    print(f"  outer 看到 x = {x}")

outer()
print(f"  全局 x = {x}")


print("\n" + "=" * 60)
print("2. 函数内赋值 → 整个函数内都是局部变量")
print("=" * 60)

count = 10

def broken():
    try:
        print(f"  print(count) -> {count}")
    except UnboundLocalError as e:
        print(f"  print(count) -> UnboundLocalError: {e}")
        print("  原因：下面有 count = ...，编译期就判定 count 是局部变量")
    count = 20      # 这行导致上面的 print 失败

broken()

print("\n  Java 对照：这会是编译错误 'variable might not have been initialized'")
print("  Python：运行时才炸，更难发现")


print("=" * 60)
print("3. global")
print("=" * 60)

counter = 0

def inc_global():
    global counter
    counter += 1

inc_global()
inc_global()
print(f"  两次 inc_global 后 counter = {counter}")

def inc_broken():
    counter += 1     # 不加 global 会怎样

try:
    inc_broken()
except UnboundLocalError as e:
    print(f"  不加 global -> UnboundLocalError: {e}")


print("=" * 60)
print("4. nonlocal：修改外层函数变量")
print("=" * 60)

def make_counter():
    cnt = 0

    def inc():
        nonlocal cnt
        cnt += 1
        return cnt

    return inc

c1 = make_counter()
c2 = make_counter()
print(f"  c1() c1() c2() = {c1()} {c1()} {c2()}   <- 各自独立状态")


print("=" * 60)
print("5. 闭包捕获的是变量，不是值")
print("=" * 60)

# 错误：所有 lambda 看到同一个 i
funcs_bad = []
for i in range(3):
    funcs_bad.append(lambda: i)
print(f"  [lambda: i for i in range(3)] -> {[f() for f in funcs_bad]}")

# 正确方式一：默认参数固定
funcs_ok1 = []
for i in range(3):
    funcs_ok1.append(lambda i=i: i)
print(f"  lambda i=i: i                 -> {[f() for f in funcs_ok1]}")

# 正确方式二：闭包工厂
def make(i):
    return lambda: i

funcs_ok2 = [make(i) for i in range(3)]
print(f"  闭包工厂                      -> {[f() for f in funcs_ok2]}")


print("=" * 60)
print("6. 内建名字被覆盖（很隐蔽的 bug）")
print("=" * 60)

print(f"  正常 len([1,2,3]) = {len([1, 2, 3])}")

len = lambda x: 999      # 覆盖了内建函数（!!!别这么干)

print(f"  覆盖后 len([1,2,3]) = {len([1, 2, 3])}")
del len                  # 恢复

print(f"  恢复后 = {__builtins__.len([1, 2, 3])}")

print("\n  常见的被覆盖名字：list / dict / id / sum / max / str / input")
