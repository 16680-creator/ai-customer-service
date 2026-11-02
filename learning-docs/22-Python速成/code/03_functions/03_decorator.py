"""03 · 闭包与装饰器

运行: python 03_functions/03_decorator.py
"""

import functools
import time

print("=" * 60)
print("1. 闭包：函数记住它被创建时的环境")
print("=" * 60)

def make_multiplier(n):
    def inner(x):
        return x * n          # n 来自外层作用域
    return inner

double = make_multiplier(2)
triple = make_multiplier(3)
print(f"  double(5) = {double(5)}")
print(f"  triple(5) = {triple(5)}")
print(f"  double.__closure__ = {double.__closure__[0].cell_contents}")


print("\n" + "=" * 60)
print("2. 装饰器本质：函数当参数 + 返回新函数")
print("=" * 60)

# 不带语法糖的版本
def timer_raw(fn):
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = fn(*args, **kwargs)
        cost = time.perf_counter() - start
        print(f"    {fn.__name__} 耗时 {cost*1000:.3f}ms")
        return result
    return wrapper

def slow_add(a, b):
    time.sleep(0.05)
    return a + b

print("  手动包装 slow_add = timer_raw(slow_add):")
slow_add = timer_raw(slow_add)
print(f"  -> {slow_add(1, 2)}")


print("\n" + "=" * 60)
print("3. @ 语法糖 + functools.wraps（必须）")
print("=" * 60)

def timer(fn):
    @functools.wraps(fn)          # 不加这个，名字/文档全丢
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        try:
            return fn(*args, **kwargs)
        finally:
            print(f"    [{fn.__name__}] {(time.perf_counter()-start)*1000:.3f}ms")
    return wrapper

@timer
def fetch_data(url: str) -> str:
    """拉取远程数据（这是文档字符串）"""
    time.sleep(0.05)
    return f"data from {url}"

print(f"  fetch_data('http://x') = {fetch_data('http://x')}")
print(f"\n  fetch_data.__name__ = {fetch_data.__name__!r}   <- wraps 保住了")
print(f"  fetch_data.__doc__  = {fetch_data.__doc__!r}")


print("=" * 60)
print("4. 不加 wraps 的后果")
print("=" * 60)

def timer_bad(fn):
    def wrapper(*args, **kwargs):
        return fn(*args, **kwargs)
    return wrapper

@timer_bad
def my_func():
    """我的文档"""

print(f"  my_func.__name__ = {my_func.__name__!r}   <- 变成 wrapper")
print(f"  my_func.__doc__  = {my_func.__doc__}")
print("  后果：pytest 收集不到、FastAPI 拿不到签名、日志名字全错")


print("\n" + "=" * 60)
print("5. 带参数的装饰器（多一层嵌套）")
print("=" * 60)

def retry(max_attempts=3, delay=0.0, exceptions=(Exception,)):
    """失败重试 —— 思路与 Resilience4j @Retry 一致"""
    def decorator(fn):
        @functools.wraps(fn)
        def wrapper(*args, **kwargs):
            last = None
            for attempt in range(1, max_attempts + 1):
                try:
                    return fn(*args, **kwargs)
                except exceptions as e:
                    last = e
                    print(f"    第 {attempt} 次失败：{e}")
                    if attempt < max_attempts and delay:
                        time.sleep(delay)
            print(f"    重试 {max_attempts} 次仍失败")
            raise last
        return wrapper
    return decorator

state = {"n": 0}

@retry(max_attempts=3, delay=0.01, exceptions=(ValueError,))
def flaky():
    state["n"] += 1
    if state["n"] < 3:
        raise ValueError(f"第 {state['n']} 次失败")
    return "成功"

print("  flaky():")
print(f"  -> {flaky()}（共调用 {state['n']} 次）")


print("=" * 60)
print("6. 多个装饰器的执行顺序")
print("=" * 60)

def deco_a(fn):
    @functools.wraps(fn)
    def w(*a, **kw):
        print("    A 进入")
        r = fn(*a, **kw)
        print("    A 退出")
        return r
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
target()


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

calls = {"n": 0}

@functools.lru_cache(maxsize=128)
def expensive(n):
    calls["n"] += 1
    return sum(i * i for i in range(n))

print(f"\n  expensive(1000) 第一次 = {expensive(1000)}")
print(f"  expensive(1000) 第二次 = {expensive(1000)}")
print(f"  实际计算次数 = {calls['n']}   <- 第二次命中缓存")
print(f"  cache_info() = {expensive.cache_info()}")


print("=" * 60)
print("8. 实战：给 FastAPI 风格的处理函数加日志与限流")
print("=" * 60)

def rate_limit(max_calls):
    def decorator(fn):
        count = {"n": 0}

        @functools.wraps(fn)
        def wrapper(*a, **kw):
            count["n"] += 1
            if count["n"] > max_calls:
                return {"error": "RATE_LIMITED", "detail": f"超过 {max_calls} 次"}
            return fn(*a, **kw)

        return wrapper

    return decorator

@rate_limit(max_calls=2)
def handle(msg):
    return {"ok": True, "msg": msg}

for i in range(4):
    print(f"  第 {i+1} 次调用 -> {handle('hi')}")
