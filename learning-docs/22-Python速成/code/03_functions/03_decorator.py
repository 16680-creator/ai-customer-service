"""03 · 闭包与装饰器

运行: python 03_functions/03_decorator.py
"""

import functools
import time

# ---- 本文件讲：闭包如何演变成装饰器，以及装饰器的四种常见形态 ----
# 主线：函数是对象 → 能被返回 → 闭包记住外层变量 → 把函数当参数传给另一个函数 → @ 语法糖。
# Java 对照：装饰器 ≈ AOP 的 @Around 切面，但 Python 没有代理框架，全靠「返回一个新函数」实现。
# 重点记住 @functools.wraps 必须加，否则函数名/文档/签名会被 wrapper 覆盖，导致 pytest、FastAPI 全出问题。
print("=" * 60)
print("1. 闭包：函数记住它被创建时的环境")
print("=" * 60)

def make_multiplier(n):
    def inner(x):
        return x * n          # n 来自外层作用域：inner 被返回后，n 仍被保存在闭包单元格里
    return inner              # 返回的是函数对象，不是调用结果

double = make_multiplier(2)   # 生成「乘以 2」的函数；n=2 被固化在这个函数的环境里
triple = make_multiplier(3)   # 生成「乘以 3」的函数；与 double 互相独立
print(f"  double(5) = {double(5)}")    # 5*2 = 10
print(f"  triple(5) = {triple(5)}")    # 5*3 = 15
print(f"  double.__closure__ = {double.__closure__[0].cell_contents}")   # 直接查看闭包单元格里存的 n → 2


# ---- 2. 装饰器本质：timer_raw(fn) 收到函数，返回一个「加了计时的同名替身」 ----
# 这里故意不用 @ 语法糖，让你看清「装饰器就是把函数传进去、拿新函数出来」这一步。
# Java 对照：类似把一个方法包进 lambda，或动态代理生成增强对象；只是 Python 不需要接口。
print("\n" + "=" * 60)
print("2. 装饰器本质：函数当参数 + 返回新函数")
print("=" * 60)

# 不带语法糖的版本
def timer_raw(fn):
    def wrapper(*args, **kwargs):          # 全收参数，以便包装任意签名的函数
        start = time.perf_counter()        # 高精度计时起点（比 time.time() 更适合测耗时）
        result = fn(*args, **kwargs)       # 调用原函数，并把参数原样铺开转发
        cost = time.perf_counter() - start # 计算耗时
        print(f"    {fn.__name__} 耗时 {cost*1000:.3f}ms")   # 打印函数名与毫秒数（:.3f 保留三位小数）
        return result                      # 把原函数返回值透传出去，调用方无感
    return wrapper                         # 返回替身函数

def slow_add(a, b):
    time.sleep(0.05)          # 模拟耗时操作
    return a + b              # 真正的业务逻辑

print("  手动包装 slow_add = timer_raw(slow_add):")
slow_add = timer_raw(slow_add)   # 关键一行：名字没变，但此刻 slow_add 已经指向 wrapper
print(f"  -> {slow_add(1, 2)}")   # 调用时会先打印耗时，再返回 3


# ---- 3. @timer 只是语法糖；functools.wraps 负责把元信息从原函数复制到 wrapper ----
# `@timer def f(): ...` 完全等价于 `def f(): ...` 之后再写 `f = timer(f)`。
# 这一版把 return 放进 try，用 finally 保证「即使原函数抛异常也照样打印耗时」——比上一版更健壮。
print("\n" + "=" * 60)
print("3. @ 语法糖 + functools.wraps（必须）")
print("=" * 60)

def timer(fn):
    @functools.wraps(fn)          # 不加这个，名字/文档全丢：它把 fn 的 __name__/__doc__/__wrapped__ 等复制到 wrapper
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        try:
            return fn(*args, **kwargs)   # 先执行原函数，返回值直接 return 出去
        finally:
            # finally 无论成功还是抛异常都会执行 → 保证耗时一定被记录（Java 里常见的 try/finally 用法）
            print(f"    [{fn.__name__}] {(time.perf_counter()-start)*1000:.3f}ms")
    return wrapper

@timer                        # 等价于：定义完 fetch_data 后执行 fetch_data = timer(fetch_data)
def fetch_data(url: str) -> str:
    """拉取远程数据（这是文档字符串）"""
    time.sleep(0.05)          # 模拟网络延迟
    return f"data from {url}"

print(f"  fetch_data('http://x') = {fetch_data('http://x')}")   # 打印耗时后再输出 data from http://x
print(f"\n  fetch_data.__name__ = {fetch_data.__name__!r}   <- wraps 保住了")   # 仍是 'fetch_data' 而不是 'wrapper'
print(f"  fetch_data.__doc__  = {fetch_data.__doc__!r}")                       # 文档字符串也被保留


# ---- 4. 反例：不加 wraps 时，函数的「身份证」会被 wrapper 顶替 ----
# 后果很具体：pytest 收集用例时按函数名去重、FastAPI 按签名生成接口文档、日志按 __name__ 打点，全都会错。
# Java 对照：相当于动态代理对象丢掉了原方法的注解和签名信息，反射拿不到真实方法。
print("=" * 60)
print("4. 不加 wraps 的后果")
print("=" * 60)

def timer_bad(fn):
    def wrapper(*args, **kwargs):
        return fn(*args, **kwargs)   # 只做转发，但没有任何元信息复制
    return wrapper

@timer_bad
def my_func():
    """我的文档"""

print(f"  my_func.__name__ = {my_func.__name__!r}   <- 变成 wrapper")   # 输出 'wrapper'
print(f"  my_func.__doc__  = {my_func.__doc__}")                        # 输出 None（原文档丢了）
print("  后果：pytest 收集不到、FastAPI 拿不到签名、日志名字全错")


# ---- 5. 带参数的装饰器：三层嵌套，最外层只负责「收配置」 ----
# @retry(max_attempts=3, ...) 的执行顺序：先调用 retry(...) 返回 decorator，再用 decorator 装饰函数。
# 所以必须多包一层：retry(配置) → decorator(函数) → wrapper(实参)。
# 思路与 Resilience4j 的 @Retry 注解完全一致，只是 Python 用函数嵌套代替注解处理器。
print("\n" + "=" * 60)
print("5. 带参数的装饰器（多一层嵌套）")
print("=" * 60)

def retry(max_attempts=3, delay=0.0, exceptions=(Exception,)):
    """失败重试 —— 思路与 Resilience4j @Retry 一致"""
    def decorator(fn):                       # 第二层：真正接收被装饰函数
        @functools.wraps(fn)
        def wrapper(*args, **kwargs):        # 第三层：真正被调用的替身
            last = None                      # 保存最后一次异常，重试耗尽后抛出
            for attempt in range(1, max_attempts + 1):   # 尝试 1..max_attempts（range 左闭右开，所以要 +1）
                try:
                    return fn(*args, **kwargs)           # 成功就直接返回，不再重试
                except exceptions as e:                  # 只捕获指定类型（默认所有 Exception）
                    last = e
                    print(f"    第 {attempt} 次失败：{e}")
                    if attempt < max_attempts and delay: # 最后一次失败后不必再等
                        time.sleep(delay)                # 简单固定间隔，生产环境通常用指数退避
            print(f"    重试 {max_attempts} 次仍失败")
            raise last                       # 重试用尽：把最后一次异常抛给调用方（Java 的 throw）
        return wrapper
    return decorator                         # 返回第二层，交给 @ 去调用

state = {"n": 0}    # 用可变容器存取调用次数；不能用整数，因为闭包里直接赋值会变成局部变量（需 nonlocal）

@retry(max_attempts=3, delay=0.01, exceptions=(ValueError,))
def flaky():
    state["n"] += 1                     # 通过下标修改字典，不触发局部变量问题
    if state["n"] < 3:
        raise ValueError(f"第 {state['n']} 次失败")   # 前两次故意失败
    return "成功"                        # 第三次成功

print("  flaky():")
print(f"  -> {flaky()}（共调用 {state['n']} 次）")   # 前两次失败、第三次成功，所以共 3 次


# ---- 6. 多个装饰器：从下往上包装，从上往下执行 ----
# @deco_a 写在上面 → 最后才包装，所以它是最外层；调用时先进入 A，再进入 B，最后才是函数本体。
# 记忆方法：@ 语法就是函数调用，离函数最近的先执行（等价 deco_a(deco_b(target))）。
# Java 对照：类似 AOP 多个 @Around 切面的嵌套顺序，由 @Order 决定，这里由书写位置决定。
print("=" * 60)
print("6. 多个装饰器的执行顺序")
print("=" * 60)

def deco_a(fn):
    @functools.wraps(fn)
    def w(*a, **kw):                 # 参数名简写：a/kw 就是 args/kwargs
        print("    A 进入")
        r = fn(*a, **kw)             # 调用内层（可能是另一个 wrapper，也可能是原函数）
        print("    A 退出")
        return r                     # 原样返回结果
    return w

def deco_b(fn):
    @functools.wraps(fn)
    def w(*a, **kw):
        print("    B 进入")
        r = fn(*a, **kw)
        print("    B 退出")
        return r
    return w

@deco_a
@deco_b
def target():
    print("    函数本体")

print("  等价于 target = deco_a(deco_b(target))")
print("  调用 target():")
target()      # 输出顺序：A 进入 → B 进入 → 函数本体 → B 退出 → A 退出（像洋葱一样）


# ---- 7. 标准库/框架里的装饰器清单 + lru_cache 实测 ----
# 前 7 行是纯字符串输出（故意用 print 列清单，不涉及执行），最后用 lru_cache 演示「自动缓存」。
# lru_cache 以参数元组为键缓存返回值，参数必须可哈希；第二次相同参数调用直接命中，不重新计算。
print("=" * 60)
print("7. 标准库里的装饰器")
print("=" * 60)

print("  @functools.lru_cache   缓存")
print("  @functools.cache       无限制缓存（3.9+）")
print("  @functools.wraps       保留元信息")
print("  @functools.total_ordering  自动补全比较符")
print("  @property / @classmethod / @staticmethod")
print("  @pytest.fixture        测试夹具")
print("  @app.get / @router.post  FastAPI 路由")

calls = {"n": 0}    # 用字典记录「真实计算了几次」，用来证明第二次走的是缓存

@functools.lru_cache(maxsize=128)   # 最多缓存 128 组参数结果，超出按最近最少使用淘汰
def expensive(n):
    calls["n"] += 1                     # 只有真正进入函数体才 +1
    return sum(i * i for i in range(n)) # 生成器求和：计算 0²+1²+...+(n-1)²

print(f"\n  expensive(1000) 第一次 = {expensive(1000)}")   # 第一次：真实计算，calls 变 1
print(f"  expensive(1000) 第二次 = {expensive(1000)}")     # 第二次：同参数直接命中缓存，不执行函数体
print(f"  实际计算次数 = {calls['n']}   <- 第二次命中缓存")   # 输出 1
print(f"  cache_info() = {expensive.cache_info()}")        # hits=1, misses=1, currsize=1


# ---- 8. 实战：带参数的限流装饰器，状态存放在闭包里 ----
# 三层结构同 retry：rate_limit(配置) → decorator(函数) → wrapper(实参)。
# count 定义在 decorator 内、wrapper 外，所以每个被装饰的函数各有一份独立计数器。
# 注意：这只是单进程内存计数，多线程/多实例场景要用 Redis 或令牌桶（生产环境的真实做法）。
print("=" * 60)
print("8. 实战：给 FastAPI 风格的处理函数加日志与限流")
print("=" * 60)

def rate_limit(max_calls):
    def decorator(fn):
        count = {"n": 0}     # 闭包状态：用 dict 是因为要原地自增（整数自增需要 nonlocal）

        @functools.wraps(fn)
        def wrapper(*a, **kw):
            count["n"] += 1                  # 每调用一次先计数
            if count["n"] > max_calls:       # 超过阈值 → 直接拒绝，不执行原函数
                return {"error": "RATE_LIMITED", "detail": f"超过 {max_calls} 次"}
            return fn(*a, **kw)              # 未超限 → 正常转发

        return wrapper

    return decorator

@rate_limit(max_calls=2)
def handle(msg):
    return {"ok": True, "msg": msg}          # 正常返回业务结果

for i in range(4):
    print(f"  第 {i+1} 次调用 -> {handle('hi')}")   # 前 2 次成功，第 3、4 次返回 RATE_LIMITED
