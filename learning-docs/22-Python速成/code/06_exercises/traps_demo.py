"""踩坑现场演示（对应文档 12 篇）

运行: python 06_exercises/traps_demo.py

每个坑演示：错误写法 -> 现象 -> 正确写法
"""

import copy
import sys
import threading
import time


def sep(title: str) -> None:
    print()
    print("=" * 66)
    print(title)
    print("=" * 66)


def main() -> None:
    sep("坑 1  整数除法变成浮点")
    print(f"  100 / 3   = {100 / 3}      <- 不是 33")
    print(f"  100 // 3  = {100 // 3}")
    print(f"  -7 // 2   = {-7 // 2}      <- Java 是 -3（这里向下取整）")

    sep("坑 2  i++ 不存在")
    try:
        exec("i = 0\ni++")
    except SyntaxError as e:
        print(f"  i++ -> SyntaxError: {e.msg}")
    print("  正确写法：i += 1")

    sep("坑 3  == None 与小整数驻留")
    v = None
    print(f"  v == None  -> {v == None}    能跑但不对")
    print(f"  v is None  -> {v is None}    正确")
    a, b = 257, 257
    print(f"  257 is 257 -> {a is b}   不一定，别用 is 比值")

    sep("坑 4  += 是原地修改")
    a = [1, 2]
    b = a
    b += [3]
    print(f"  b += [3]     -> a={a} b={b}   <- 同一个对象")
    a = [1, 2]
    b = a
    b = b + [3]
    print(f"  b = b + [3]  -> a={a} b={b}   <- 新对象")

    sep("坑 5  二维列表乘法复制")
    bad = [[0] * 3] * 3
    bad[0][0] = 9
    print(f"  [[0]*3]*3      -> {bad}")
    good = [[0] * 3 for _ in range(3)]
    good[0][0] = 9
    print(f"  推导式          -> {good}")

    sep("坑 6  浅拷贝 vs 深拷贝")
    nested = [[1, 2], [3, 4]]
    shallow = nested.copy()
    shallow[0][0] = 99
    print(f"  copy() 后改内层 -> nested={nested}   <- 被改了")
    nested = [[1, 2], [3, 4]]
    deep = copy.deepcopy(nested)
    deep[0][0] = 99
    print(f"  deepcopy 后     -> nested={nested}   <- 安全")

    sep("坑 7  空大括号是 dict 不是 set")
    print(f"  type({{}})   -> {type({}).__name__}")
    print(f"  type(set()) -> {type(set()).__name__}")

    sep("坑 8  dict[key] 不存在抛 KeyError")
    d = {}
    try:
        d["a"]
    except KeyError as e:
        print(f"  d['a']     -> KeyError: {e}")
    print(f"  d.get('a') -> {d.get('a')}")

    sep("坑 9  or 做默认值吞掉 0/空串")
    d = {"count": 0}
    print(f"  d.get('count') or 100 -> {d.get('count') or 100}    <- 0 被当假值")
    print(f"  d.get('count', 100)   -> {d.get('count', 100)}      <- 正确")

    sep("坑 10  遍历时修改字典")
    d = {"a": 1, "b": 2}
    try:
        for k in d:
            del d[k]
    except RuntimeError as e:
        print(f"  直接删 -> RuntimeError: {e}")
    d = {"a": 1, "b": 2}
    for k in list(d.keys()):
        del d[k]
    print(f"  先复制 keys -> {d}")

    sep("坑 11  list.pop(0) 是 O(n)")
    from collections import deque
    q = deque([1, 2, 3])
    q.popleft()
    print(f"  deque.popleft() -> {list(q)}    O(1)")
    print("  list.pop(0) 是 O(n)，队列一定用 deque")

    sep("坑 12  x in list 是 O(n)")
    big = list(range(100_000))
    t0 = time.perf_counter()
    for _ in range(100):
        99_999 in big
    t_list = time.perf_counter() - t0
    big_set = set(big)
    t0 = time.perf_counter()
    for _ in range(100):
        99_999 in big_set
    t_set = time.perf_counter() - t0
    print(f"  100 次 in list: {t_list*1000:7.2f}ms")
    print(f"  100 次 in set : {t_set*1000:7.4f}ms")
    print(f"  快约 {t_list/max(t_set,1e-9):.0f} 倍")

    sep("坑 13  可变默认参数")
    def bad_default(item, lst=[]):
        lst.append(item)
        return lst
    print(f"  bad(1) = {bad_default(1)}")
    print(f"  bad(2) = {bad_default(2)}   <- 累积了")

    def good_default(item, lst=None):
        if lst is None:
            lst = []
        lst.append(item)
        return lst
    print(f"  good(1) = {good_default(1)}")
    print(f"  good(2) = {good_default(2)}")

    sep("坑 14  闭包延迟绑定")
    funcs = [lambda: i for i in range(3)]
    print(f"  [lambda: i ...] -> {[f() for f in funcs]}   <- 全是 2")
    funcs = [lambda i=i: i for i in range(3)]
    print(f"  [lambda i=i: i] -> {[f() for f in funcs]}")

    sep("坑 15  装饰器忘加 functools.wraps")
    import functools

    def timer_bad(fn):
        def wrapper(*a, **kw):
            return fn(*a, **kw)
        return wrapper

    def timer_good(fn):
        @functools.wraps(fn)
        def wrapper(*a, **kw):
            return fn(*a, **kw)
        return wrapper

    @timer_bad
    def f1():
        """我的文档"""

    @timer_good
    def f2():
        """我的文档"""

    print(f"  不加 wraps: __name__={f1.__name__!r} __doc__={f1.__doc__!r}")
    print(f"  加了 wraps: __name__={f2.__name__!r} __doc__={f2.__doc__!r}")

    sep("坑 16  子类忘调 super().__init__()")
    class Parent:
        def __init__(self, name):
            self.name = name

    class Child(Parent):
        def __init__(self, name, age):
            self.age = age          # 忘了 super()

    c = Child("x", 1)
    try:
        c.name
    except AttributeError as e:
        print(f"  c.name -> AttributeError: {e}")
    print("  Java 会自动调 super()，Python 不会，而且不报错")

    sep("坑 17  __eq__ 有了但没 __hash__")
    class NoHash:
        def __init__(self, no):
            self.no = no

        def __eq__(self, other):
            return isinstance(other, NoHash) and self.no == other.no

    try:
        {NoHash("A")}
    except TypeError as e:
        print(f"  放 set -> TypeError: {e}")

    sep("坑 18  生成器只能遍历一次")
    g = (x for x in range(3))
    print(f"  sum(g) 第一次 = {sum(g)}")
    print(f"  sum(g) 第二次 = {sum(g)}")

    sep("坑 19  多线程跑 CPU 密集任务没有加速")
    def cpu(n):
        return sum(i * i for i in range(n))
    N = 1_500_000
    t0 = time.perf_counter()
    for _ in range(4):
        cpu(N)
    serial = time.perf_counter() - t0
    t0 = time.perf_counter()
    ts = [threading.Thread(target=cpu, args=(N,)) for _ in range(4)]
    for t in ts:
        t.start()
    for t in ts:
        t.join()
    threaded = time.perf_counter() - t0
    print(f"  串行 {serial:.2f}s   4 线程 {threaded:.2f}s   加速比 {serial/threaded:.2f}x")
    print("  加速比接近 1 -> GIL。CPU 密集必须用进程池")

    sep("坑 20  json.dumps 默认转义中文")
    import json
    data = {"名": "中文"}
    print(f"  默认                -> {json.dumps(data)}")
    print(f"  ensure_ascii=False  -> {json.dumps(data, ensure_ascii=False)}")

    sep("坑 21  用 float 算钱")
    print(f"  0.1 + 0.2        = {0.1 + 0.2}")
    print(f"  == 0.3           = {0.1 + 0.2 == 0.3}")
    from decimal import Decimal, ROUND_HALF_UP
    print(f"  Decimal('0.1')+  = {Decimal('0.1') + Decimal('0.2')}")
    print(f"  舍入             = "
          f"{(Decimal('100') / Decimal('3')).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)}")

    sep("坑 22  assert 用于业务校验")
    print("  python -O script.py 会移除所有 assert")
    print("  校验入参请用 if + raise ValueError(...)")

    sep("坑 23  不指定 encoding（Windows 必炸）")
    import locale
    print(f"  当前系统默认编码: {locale.getpreferredencoding()}")
    print("  Windows 上是 cp936(GBK)，Linux 是 UTF-8")
    print("  规则：所有 open 都写 encoding='utf-8'")
    print(f"  sys.stdout.encoding = {sys.stdout.encoding}")


if __name__ == "__main__":
    main()
