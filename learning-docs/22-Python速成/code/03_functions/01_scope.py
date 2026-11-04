"""01 · 作用域 LEGB

运行: python 03_functions/01_scope.py
"""

# ---- 本文件讲：Python 的 LEGB 作用域规则，以及与 Java 的三处关键差异 ----
# 差异一：Python 没有「块作用域」，if/for 里赋值的变量会泄漏到函数/模块级（Java 的 {} 会隔离）。
# 差异二：函数内一旦对某名字赋值，该名字在整个函数内就被判定为局部变量（编译期决定），可能引发 UnboundLocalError。
# 差异三：修改外层变量要显式声明 global / nonlocal（Java 需要 effectively final，思路相反）。
# 这三点是 Java 开发者读 Python 时最容易被「静默行为」坑到的地方。

x = "global-x"     # 模块级（全局）变量；等价 Java 的类静态字段，只是 Python 不需要 class 包裹

# ---- 1. LEGB 演示：内层优先看到最近的绑定 ----
# L(local) → E(enclosing) → G(global) → B(builtins)，找不到才抛 NameError。
print("=" * 60)
print("1. LEGB 查找顺序")
print("=" * 60)

def outer():
    x = "enclosing-x"          # 外层函数的局部变量，对内层函数来说属于 E（enclosing）层

    def inner():
        # x = "local-x"        # 如果取消注释，inner 里就有自己的 L 层 x，会输出 local-x
        print(f"  inner 看到 x = {x}")   # inner 自己没有 x → 向外找到 outer 的 E 层 → 输出 enclosing-x

    inner()                    # 调用内层函数
    print(f"  outer 看到 x = {x}")      # outer 自己就是 L 层 → 输出 enclosing-x

outer()                        # 调用外层函数
print(f"  全局 x = {x}")        # 模块层没有任何 x 被修改 → 仍是 global-x


# ---- 2. 编译期判定局部变量：函数体里出现 count = ...，count 就全程是局部变量 ----
# 关键点：判定发生在「编译函数时」，而不是执行到赋值那一行时。
# 所以虽然 print 在赋值之前，Python 也不会回头去读全局 count，而是直接报未绑定。
# Java 对照：这会是编译错误 "variable count might not have been initialized"，编译期就拦住。
print("\n" + "=" * 60)
print("2. 函数内赋值 → 整个函数内都是局部变量")
print("=" * 60)

count = 10      # 模块级变量，注意它与函数内的 count 是两个不同的变量

def broken():
    # Python 编译器扫描函数体，发现下面有 `count = 20`，于是把 count 标记为局部变量。
    # 因此这里的 print 想读的是「尚未赋值的局部变量」→ 运行时抛 UnboundLocalError。
    try:
        print(f"  print(count) -> {count}")     # 实际执行到这里就会抛异常
    except UnboundLocalError as e:              # 只捕获「局部变量未绑定」，不吞掉其他异常
        print(f"  print(count) -> UnboundLocalError: {e}")
        print("  原因：下面有 count = ...，编译期就判定 count 是局部变量")
    count = 20      # 这行导致上面的 print 失败（它让 count 成为局部变量）

broken()

print("\n  Java 对照：这会是编译错误 'variable might not have been initialized'")
print("  Python：运行时才炸，更难发现")


# ---- 3. global：显式声明「这个函数要读写模块级变量」 ----
# 只有声明了 global，`+=` 才会去读全局值再写回全局；否则又会变成「局部变量未绑定」。
# Java 对照：Java 里直接操作静态字段即可，没有这层声明；这是 Python 为了「不用声明」的设计付出的代价。
print("=" * 60)
print("3. global")
print("=" * 60)

counter = 0                 # 模块级计数器

def inc_global():
    global counter          # 声明 counter 指向模块级作用域，本函数内不再新建局部变量
    counter += 1            # 现在等价于 counter = counter + 1，读写的都是全局那个

inc_global()                # 第一次自增 → 1
inc_global()                # 第二次自增 → 2
print(f"  两次 inc_global 后 counter = {counter}")   # 输出 2，证明改的是全局变量

def inc_broken():
    counter += 1     # 不加 global 会怎样：编译器判定 counter 是局部变量，读的时候还没赋值

try:
    inc_broken()     # 调用时立刻抛 UnboundLocalError
except UnboundLocalError as e:
    print(f"  不加 global -> UnboundLocalError: {e}")


# ---- 4. nonlocal：在闭包里修改「外层函数的局部变量」 ----
# 这是 Python 里实现「带状态的函数」的标准手法：状态挂在闭包单元格里，而不是对象的字段。
# Java 对照：lambda 只能捕获 effectively final 的变量；想改状态必须用数组/AtomicInteger 之类的容器绕过去。
# Python 用 nonlocal 直接声明「我要改外层那个」，比 Java 的绕法更直白，也更容易写出隐式共享状态。
print("=" * 60)
print("4. nonlocal：修改外层函数变量")
print("=" * 60)

def make_counter():
    cnt = 0                # 外层函数的局部变量，被内层函数通过闭包持有

    def inc():
        nonlocal cnt       # 声明 cnt 来自外层函数作用域（E 层），不是新建局部变量
        cnt += 1           # 因此这里读写的都是 outer 里那个 cnt
        return cnt         # 返回自增后的值

    return inc             # 返回函数对象本身（不是调用结果）；这是「闭包工厂」

c1 = make_counter()        # 第一次调用工厂 → 生成一个独立计数器（自带一个 cnt=0）
c2 = make_counter()        # 第二次调用 → 又生成一个全新、互不影响的计数器
print(f"  c1() c1() c2() = {c1()} {c1()} {c2()}   <- 各自独立状态")   # 1 2 1，注意最后一个回到 1


# ---- 5. 闭包捕获的是「变量」而不是「当时的值」 ----
# 循环里创建的 lambda 共享同一个 i（单元格），等循环结束时 i 已经是最后一个值，
# 所以三个函数调用出来全是 2。Java 的 lambda 不允许捕获可变局部变量，因此不会踩这个坑。
# 两种修法：默认参数在定义时就把值固化；闭包工厂让每轮创建独立作用域。
print("=" * 60)
print("5. 闭包捕获的是变量，不是值")
print("=" * 60)

# 错误：所有 lambda 看到同一个 i
funcs_bad = []                  # 用来收集三个 lambda
for i in range(3):              # i 依次是 0 1 2，但循环结束后 i 停在 2
    funcs_bad.append(lambda: i)  # lambda 只记录「去哪个作用域取 i」，不记录当时 i 的值
print(f"  [lambda: i for i in range(3)] -> {[f() for f in funcs_bad]}")   # 输出 [2, 2, 2]

# 正确方式一：默认参数固定
funcs_ok1 = []
for i in range(3):
    funcs_ok1.append(lambda i=i: i)   # 默认值在「定义函数时」求值 → 每轮把当时的 i 存进参数默认值
print(f"  lambda i=i: i                 -> {[f() for f in funcs_ok1]}")   # 输出 [0, 1, 2]

# 正确方式二：闭包工厂
def make(i):
    return lambda: i        # 每次调用 make 都产生一个新的作用域，i 是这次调用的私有参数

funcs_ok2 = [make(i) for i in range(3)]   # 三个独立作用域，各持有自己的 i
print(f"  闭包工厂                      -> {[f() for f in funcs_ok2]}")   # 输出 [0, 1, 2]


# ---- 6. 覆盖内建名字：模块级赋值会遮蔽 builtins 层（B 层） ----
# 演示 `len = lambda x: 999` 之后，原本的 len 就查不到了；del 删除本地绑定后恢复查找。
# Java 没有这个问题，因为 java.lang 的类名不会被局部变量遮蔽到「调用不到」的程度。
# 实际项目里绝不要用 list / dict / str / id / sum / max / input 当变量名。
print("=" * 60)
print("6. 内建名字被覆盖（很隐蔽的 bug）")
print("=" * 60)

print(f"  正常 len([1,2,3]) = {len([1, 2, 3])}")   # 此时 len 还是内建函数，输出 3

len = lambda x: 999      # 覆盖了内建函数（!!!别这么干)  模块级新建了一个名为 len 的变量，遮蔽 B 层

print(f"  覆盖后 len([1,2,3]) = {len([1, 2, 3])}")   # 现在调用的是我们自己的 lambda，输出 999
del len                  # 恢复：删除模块级这个绑定，查找链重新落到 builtins 层

print(f"  恢复后 = {__builtins__.len([1, 2, 3])}")  # __builtins__ 是内建模块，显式访问它里面的 len，输出 3

print("\n  常见的被覆盖名字：list / dict / id / sum / max / str / input")
