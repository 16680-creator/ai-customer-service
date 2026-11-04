"""踩坑现场演示（对应文档 12 篇）

运行: python 06_exercises/traps_demo.py

每个坑演示：错误写法 -> 现象 -> 正确写法
"""

# ---- 本段讲：本文件的导入与终端分隔工具 ----
# 这个脚本把「12 篇踩坑清单」里最常踩的坑挨个现场跑一遍：
# 每个坑先演示错误写法的现象，再给正确写法对比，输出可以直接当对照表看。
# 导入统一放在文件顶部（PEP8 约定），Java 里 import 同样写在源文件最上方。
import copy              # 深拷贝：copy.deepcopy；Java 里通常要自己实现 clone 或序列化
import sys               # 读 sys.stdout.encoding，用来演示编码坑
import threading         # 演示 GIL：多线程跑 CPU 密集任务并不会变快
import time              # perf_counter 高精度计时（Java 对应 System.nanoTime）


def sep(title: str) -> None:
    # 打印一条横幅把每个坑隔开，方便在终端里对照阅读。
    # Python 的 print() 不带参数就只输出一个换行；Java 是 System.out.println()。
    print()
    print("=" * 66)     # 字符串 * 整数 = 重复拼接；Java 要写 "=".repeat(66)
    print(title)        # 当前坑的标题
    print("=" * 66)     # 与上方对称的收尾横线


def main() -> None:
    # ---- 坑 1：/ 永远得到 float，想要整除必须用 // ----
    # Java 里 int/int 会截断成 int；Python 3 的 / 一律返回 float，这是最常见的“数值悄悄变形”来源。
    sep("坑 1  整数除法变成浮点")
    print(f"  100 / 3   = {100 / 3}      <- 不是 33")          # 单斜杠：真除法，结果是 33.333...
    print(f"  100 // 3  = {100 // 3}")                          # 双斜杠：向下取整的地板除，才是 33
    print(f"  -7 // 2   = {-7 // 2}      <- Java 是 -3（这里向下取整）")   # 负数地板除得 -4，不是 Java 的截断 -3

    # ---- 坑 2：Python 没有 i++ / ++i ----
    # 这不是“不推荐”，而是语法层面就不存在；写出来会在解析阶段直接 SyntaxError。
    sep("坑 2  i++ 不存在")
    try:
        exec("i = 0\ni++")          # exec 把字符串当代码执行，只为复现这个语法错误
    except SyntaxError as e:        # 语法错误在编译阶段抛出，不是运行时异常
        print(f"  i++ -> SyntaxError: {e.msg}")   # e.msg 是错误描述（Java 对应 e.getMessage()）
    print("  正确写法：i += 1")      # Python 只有增量赋值；i += 1 等价 Java 的 i++

    # ---- 坑 3：判空用 is None，比值用 ==，永远别用 is 比数字 ----
    # is 比较的是对象身份（是不是同一块内存），== 比较的是值（会调用 __eq__）。
    sep("坑 3  == None 与小整数驻留")
    v = None                        # None 是单例对象，全程序只有一个
    print(f"  v == None  -> {v == None}    能跑但不对")   # 能得出 True，但语义是“值相等”，不是“就是 None”
    print(f"  v is None  -> {v is None}    正确")         # 身份比较，语义明确且更快；PEP8 推荐这种
    a, b = 257, 257                 # 元组解包同时赋值；两个 257 是不同位置创建的对象
    print(f"  257 is 257 -> {a is b}   不一定，别用 is 比值")   # 结果依赖解释器缓存策略，不能当稳定语义

    # ---- 坑 4：list 的 += 是原地扩展（IN_PLACE），+ 才是新建列表 ----
    # 变量只是对象的“标签”，b = a 让两个标签指向同一个 list，改 b 就是改 a。
    sep("坑 4  += 是原地修改")
    a = [1, 2]          # 新建列表，a 指向它
    b = a               # 别名：b 和 a 指向同一个对象（Java 里也是引用复制）
    b += [3]            # list 的 __iadd__ 原地 append，a 跟着变
    print(f"  b += [3]     -> a={a} b={b}   <- 同一个对象")
    a = [1, 2]          # 重新赋值一个全新列表，切断与之前的别名关系
    b = a               # 再次让 b 别名 a
    b = b + [3]         # + 会创建新列表再赋给 b；a 仍指向旧列表，不受影响
    print(f"  b = b + [3]  -> a={a} b={b}   <- 新对象")

    # ---- 坑 5：[[0]*3]*3 复制的是“内层列表的引用” ----
    # 外层重复了 3 次同一个内层列表，所以改任意一行都会让三行一起变。
    sep("坑 5  二维列表乘法复制")
    bad = [[0] * 3] * 3    # 只创建了 1 个内层列表，外层三个位置都指向它
    bad[0][0] = 9          # 改第 0 行第 0 列，另外两行同时变成 9
    print(f"  [[0]*3]*3      -> {bad}")
    good = [[0] * 3 for _ in range(3)]   # 列表推导式每轮都新建一个内层列表，互不影响
    good[0][0] = 9                       # 只改第一行
    print(f"  推导式          -> {good}")

    # ---- 坑 6：copy() 只浅拷贝一层，嵌套的可变对象仍共享 ----
    sep("坑 6  浅拷贝 vs 深拷贝")
    nested = [[1, 2], [3, 4]]      # 两层结构：外层列表 + 两个内层列表
    shallow = nested.copy()        # 浅拷贝：新建外层列表，但内层元素仍是同一批对象
    shallow[0][0] = 99             # 改的是内层列表 -> 原 nested 也被改
    print(f"  copy() 后改内层 -> nested={nested}   <- 被改了")
    nested = [[1, 2], [3, 4]]      # 恢复原始数据，重新演示
    deep = copy.deepcopy(nested)   # 深拷贝：递归复制每一层，完全独立
    deep[0][0] = 99                # 只影响副本
    print(f"  deepcopy 后     -> nested={nested}   <- 安全")

    # ---- 坑 7：{} 是空字典，空集合只能写 set() ----
    # 因为字典也用大括号，Python 把 {} 优先解释成 dict；这和 Java 的 new HashMap<>() 不同。
    sep("坑 7  空大括号是 dict 不是 set")
    print(f"  type({{}})   -> {type({}).__name__}")     # 注意 f-string 里的字面大括号要写成 {{ }}
    print(f"  type(set()) -> {type(set()).__name__}")   # 空集合必须显式调用 set() 构造

    # ---- 坑 8：d[key] 取不到会抛 KeyError，不是返回 null ----
    # Java 的 Map.get 返回 null；Python 的 [] 直接抛异常，所以“可能不存在”时要用 get。
    sep("坑 8  dict[key] 不存在抛 KeyError")
    d = {}                      # 空字典
    try:
        d["a"]                  # 键不存在 -> 抛 KeyError，程序不会继续往下走
    except KeyError as e:
        print(f"  d['a']     -> KeyError: {e}")
    print(f"  d.get('a') -> {d.get('a')}")   # get 取不到返回 None（可传第二个参数做默认值）

    # ---- 坑 9：用 or 做默认值会把 0 / "" / False 当成“没值” ----
    # 因为 0、空串、空容器在 Python 里都是假值（falsy），or 会继续取右边。
    sep("坑 9  or 做默认值吞掉 0/空串")
    d = {"count": 0}            # 键存在，值是合法的 0
    print(f"  d.get('count') or 100 -> {d.get('count') or 100}    <- 0 被当假值")   # 0 是 falsy，结果变成 100
    print(f"  d.get('count', 100)   -> {d.get('count', 100)}      <- 正确")          # 只有键缺失时才用默认值

    # ---- 坑 10：遍历字典/列表时不能增删元素 ----
    # 迭代器依赖内部结构，边遍历边改会触发 RuntimeError（Java 是 ConcurrentModificationException）。
    sep("坑 10  遍历时修改字典")
    d = {"a": 1, "b": 2}
    try:
        for k in d:
            del d[k]            # 遍历过程中改变字典大小 -> 运行时报错
    except RuntimeError as e:
        print(f"  直接删 -> RuntimeError: {e}")
    d = {"a": 1, "b": 2}
    for k in list(d.keys()):    # 先复制一份键列表，遍历副本、删原字典就安全了
        del d[k]
    print(f"  先复制 keys -> {d}")

    # ---- 坑 11：list.pop(0) 要整体前移元素，复杂度 O(n) ----
    # 当队列用时请用 collections.deque：两端出入都是 O(1)（Java 对应 ArrayDeque / LinkedList）。
    sep("坑 11  list.pop(0) 是 O(n)")
    from collections import deque   # 函数内 import 合法，但工程里通常放文件顶部
    q = deque([1, 2, 3])            # 双端队列，从左端弹出不需要搬移其余元素
    q.popleft()                     # 弹出左端元素 1，O(1)
    print(f"  deque.popleft() -> {list(q)}    O(1)")
    print("  list.pop(0) 是 O(n)，队列一定用 deque")

    # ---- 坑 12：x in list 是线性扫描 O(n)，x in set 是哈希查找 O(1) ----
    # 数据量一大，判断“存不存在”用 list 会慢几个数量级，这是最常见的性能事故之一。
    sep("坑 12  x in list 是 O(n)")
    big = list(range(100_000))      # 10 万个整数的列表；下划线只是数字可读性分隔符
    t0 = time.perf_counter()        # 记录起点时间（高精度秒）
    for _ in range(100):            # _ 表示“这个循环变量用不到”，只是跑 100 次
        99_999 in big               # 每次都要从头扫到尾，最坏比较 10 万次
    t_list = time.perf_counter() - t0
    big_set = set(big)              # 转成集合，建立哈希表（一次性 O(n) 成本）
    t0 = time.perf_counter()
    for _ in range(100):
        99_999 in big_set           # 哈希定位，平均 O(1)
    t_set = time.perf_counter() - t0
    print(f"  100 次 in list: {t_list*1000:7.2f}ms")    # 秒转毫秒；格式里 7.2f 指定宽度和小数位
    print(f"  100 次 in set : {t_set*1000:7.4f}ms")     # 集合通常快得多，多留几位小数才看得清
    print(f"  快约 {t_list/max(t_set,1e-9):.0f} 倍")    # max 防止除零（计时可能小到 0）

    # ---- 坑 13：默认参数只在定义函数时求值一次，可变对象会被所有调用共享 ----
    # 正确姿势：默认值写 None，函数体内判断后再新建（不要用 [] / {} 当默认值）。
    sep("坑 13  可变默认参数")
    # 错误写法：lst=[] 在 def 执行时就创建了一个列表，之后所有不传 lst 的调用共用它
    def bad_default(item, lst=[]):
        lst.append(item)        # 往“那个共享列表”里追加
        return lst
    print(f"  bad(1) = {bad_default(1)}")
    print(f"  bad(2) = {bad_default(2)}   <- 累积了")   # 第二次还能看到第一次的元素

    # 正确写法：默认 None 作哨兵，进函数后再创建新列表
    def good_default(item, lst=None):
        if lst is None:         # 只有调用方没传 lst 时才新建
            lst = []
        lst.append(item)
        return lst
    print(f"  good(1) = {good_default(1)}")
    print(f"  good(2) = {good_default(2)}")            # 两次互不影响

    # ---- 坑 14：闭包捕获的是变量本身，不是当时的值（延迟绑定）----
    # 循环结束后 i 已经是最后一个值，所有 lambda 读到的都是它。
    sep("坑 14  闭包延迟绑定")
    funcs = [lambda: i for i in range(3)]   # 三个 lambda 都引用同一个外层变量 i
    print(f"  [lambda: i ...] -> {[f() for f in funcs]}   <- 全是 2")
    funcs = [lambda i=i: i for i in range(3)]   # 用默认参数在创建时“拍照”固定当前 i
    print(f"  [lambda i=i: i] -> {[f() for f in funcs]}")

    # ---- 坑 15：装饰器不包 functools.wraps，函数名/文档会被 wrapper 覆盖 ----
    # 这会影响日志、调试、文档生成，甚至影响某些依赖 __name__ 的路由注册。
    sep("坑 15  装饰器忘加 functools.wraps")
    import functools

    # 错误写法：直接返回内部 wrapper，元信息全丢
    def timer_bad(fn):
        def wrapper(*a, **kw):      # *a 收位置参数、**kw 收关键字参数，原样透传
            return fn(*a, **kw)
        return wrapper

    # 正确写法：用 wraps 把原函数的 __name__ / __doc__ 等复制到 wrapper 上
    def timer_good(fn):
        @functools.wraps(fn)        # 装饰器套在 wrapper 上，负责复制元信息
        def wrapper(*a, **kw):
            return fn(*a, **kw)
        return wrapper

    @timer_bad          # 等价于 f1 = timer_bad(f1)
    def f1():
        """我的文档"""

    @timer_good
    def f2():
        """我的文档"""

    # !r 表示用 repr() 格式化字符串，能看出到底是普通字符串还是 None
    print(f"  不加 wraps: __name__={f1.__name__!r} __doc__={f1.__doc__!r}")
    print(f"  加了 wraps: __name__={f2.__name__!r} __doc__={f2.__doc__!r}")

    # ---- 坑 16：子类必须手动调 super().__init__()，Python 不会自动调用 ----
    # 而且不调也不会报错，只是父类字段没初始化，等到访问时才炸（AttributeError）。
    sep("坑 16  子类忘调 super().__init__()")
    # 父类：保存 name
    class Parent:
        def __init__(self, name):   # 构造方法；self 相当于 Java 的 this，必须显式声明
            self.name = name        # 实例属性在赋值时才被创建，无需事先声明字段

    # 子类：只初始化了 age，忘了调用父类构造
    class Child(Parent):
        def __init__(self, name, age):
            self.age = age          # 忘了 super()

    c = Child("x", 1)               # 创建对象；name 参数被吞掉，没有存到 self.name
    try:
        c.name                      # 访问不存在的属性 -> AttributeError（Java 是编译期就报错）
    except AttributeError as e:
        print(f"  c.name -> AttributeError: {e}")
    print("  Java 会自动调 super()，Python 不会，而且不报错")

    # ---- 坑 17：重写 __eq__ 后必须自己实现 __hash__，否则对象不可哈希 ----
    # Python 规定：定义了 __eq__ 的类，__hash__ 会被自动置为 None，不能放进 set/dict 键。
    sep("坑 17  __eq__ 有了但没 __hash__")
    class NoHash:
        def __init__(self, no):
            self.no = no        # 业务主键

        def __eq__(self, other):    # 自定义“相等”语义
            return isinstance(other, NoHash) and self.no == other.no   # 同类且主键相同才算相等

    try:
        {NoHash("A")}               # 集合字面量：插入时需要哈希值 -> 拿不到就 TypeError
    except TypeError as e:
        print(f"  放 set -> TypeError: {e}")

    # ---- 坑 18：生成器是“一次性”的，遍历完就耗尽 ----
    # 生成器表达式不会把数据存起来，而是边算边吐；第二次 sum 时已经没有元素了。
    sep("坑 18  生成器只能遍历一次")
    g = (x for x in range(3))       # 括号是生成器表达式（列表推导式则用方括号）
    print(f"  sum(g) 第一次 = {sum(g)}")    # 第一次遍历：0+1+2=3
    print(f"  sum(g) 第二次 = {sum(g)}")    # 已经耗尽，返回 0（不会报错，容易被忽略）

    # ---- 坑 19：GIL 让多线程无法并行跑 Python 字节码 ----
    # 线程适合 I/O 等待型任务；CPU 密集要绕开 GIL，用 multiprocessing / ProcessPoolExecutor。
    sep("坑 19  多线程跑 CPU 密集任务没有加速")
    # CPU 密集的小函数：把 0..n-1 的平方累加
    def cpu(n):
        return sum(i * i for i in range(n))   # 生成器表达式直接喂给 sum，不占额外内存
    N = 1_500_000                   # 循环规模：足够大到让耗时差异明显
    t0 = time.perf_counter()
    for _ in range(4):              # 串行跑 4 次，作为基准
        cpu(N)
    serial = time.perf_counter() - t0
    t0 = time.perf_counter()
    # 4 个线程同时跑；args=(N,) 是单元素元组，逗号不能省，否则只是数字
    ts = [threading.Thread(target=cpu, args=(N,)) for _ in range(4)]
    for t in ts:
        t.start()                   # 启动线程（Java 里是 thread.start()）
    for t in ts:
        t.join()                    # 等所有线程结束（Java 里是 thread.join()）
    threaded = time.perf_counter() - t0
    print(f"  串行 {serial:.2f}s   4 线程 {threaded:.2f}s   加速比 {serial/threaded:.2f}x")
    print("  加速比接近 1 -> GIL。CPU 密集必须用进程池")

    # ---- 坑 20：json.dumps 默认 ensure_ascii=True，中文会变成 \uXXXX ----
    # 这是接口返回“满屏转义符”的常见原因；中文场景几乎都要显式关掉。
    sep("坑 20  json.dumps 默认转义中文")
    import json
    data = {"名": "中文"}            # 键和值都是中文，转义后会变成 \u540d 之类
    print(f"  默认                -> {json.dumps(data)}")
    print(f"  ensure_ascii=False  -> {json.dumps(data, ensure_ascii=False)}")   # 保留原字符

    # ---- 坑 21：二进制浮点不能精确表示 0.1，算钱必须用 Decimal ----
    # 这不是 Python 的问题，Java 的 double 也一样；金额请用 Decimal / BigDecimal。
    sep("坑 21  用 float 算钱")
    print(f"  0.1 + 0.2        = {0.1 + 0.2}")            # 得到 0.30000000000000004
    print(f"  == 0.3           = {0.1 + 0.2 == 0.3}")     # 因此比较结果为 False
    from decimal import Decimal, ROUND_HALF_UP          # Java 对应 java.math.BigDecimal
    print(f"  Decimal('0.1')+  = {Decimal('0.1') + Decimal('0.2')}")   # 十进制精确运算，得 0.3
    print(f"  舍入             = "
          # quantize 把结果量化到 0.01（两位小数），ROUND_HALF_UP 是四舍五入
          f"{(Decimal('100') / Decimal('3')).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)}")

    # ---- 坑 22：assert 会在 python -O 下被整体删除，不能当业务校验 ----
    # assert 只适合调试期的不变量检查；对外部输入的校验必须用 if + raise。
    sep("坑 22  assert 用于业务校验")
    print("  python -O script.py 会移除所有 assert")
    print("  校验入参请用 if + raise ValueError(...)")

    # ---- 坑 23：open() 不写 encoding 会用系统默认编码，跨平台必翻车 ----
    # Windows 中文环境默认 cp936(GBK)，Linux 默认 UTF-8，同一段代码行为不一致。
    sep("坑 23  不指定 encoding（Windows 必炸）")
    import locale
    print(f"  当前系统默认编码: {locale.getpreferredencoding()}")   # 本机默认编码
    print("  Windows 上是 cp936(GBK)，Linux 是 UTF-8")
    print("  规则：所有 open 都写 encoding='utf-8'")               # 显式指定，避免依赖平台
    print(f"  sys.stdout.encoding = {sys.stdout.encoding}")         # 标准输出实际使用的编码


# 直接运行本文件时才执行 main；被其他模块 import 时不会自动跑这 23 个坑。
if __name__ == "__main__":
    main()
