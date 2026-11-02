"""02 · 参数体系：*args / **kwargs / 仅关键字

运行: python 03_functions/02_args.py
"""

print("=" * 60)
print("1. 参数顺序：位置 → 默认 → *args → 仅关键字 → **kwargs")
print("=" * 60)

def full(a, b=2, *args, c, d=4, **kwargs):
    print(f"  a={a} b={b} args={args} c={c} d={d} kwargs={kwargs}")

print("full(1, 3, 5, 6, c=7, e=8):")
full(1, 3, 5, 6, c=7, e=8)


print("\n" + "=" * 60)
print("2. 解包调用")
print("=" * 60)

def add3(a, b, c):
    return a + b + c

nums = [1, 2, 3]
print(f"add3(*[1,2,3])   = {add3(*nums)}")

kwargs = {"a": 1, "b": 2, "c": 3}
print(f"add3(**dict)     = {add3(**kwargs)}")

print("\n  Java 没有这个能力：必须手动拆数组或用 Builder")


print("=" * 60)
print("3. 仅关键字参数（* 后面的必须用关键字传）")
print("=" * 60)

def transfer(from_acct, to_acct, *, amount, currency="CNY"):
    return f"{from_acct} -> {to_acct} : {amount} {currency}"

print(f"  {transfer('A', 'B', amount=100)}")
print(f"  {transfer('A', 'B', amount=100, currency='USD')}")

try:
    transfer("A", "B", 100)
except TypeError as e:
    print(f"  位置传 -> TypeError: {e}")

print("\n  价值：防止参数顺序错乱造成生产事故")


print("=" * 60)
print("4. 仅位置参数（3.8+，/ 之前的只能按位置传）")
print("=" * 60)

def div(a, b, /):
    return a / b

print(f"  div(6, 2) = {div(6, 2)}")
try:
    div(a=6, b=2)
except TypeError as e:
    print(f"  div(a=6, b=2) -> TypeError: {e}")


print("=" * 60)
print("5. 默认参数陷阱")
print("=" * 60)

def bad(item, lst=[]):
    lst.append(item)
    return lst

print(f"  bad(1) = {bad(1)}")
print(f"  bad(2) = {bad(2)}    <- 不是 [2]")
print(f"  bad.__defaults__ = {bad.__defaults__}   <- 同一个对象被复用")

def good(item, lst=None):
    if lst is None:
        lst = []
    lst.append(item)
    return lst

print(f"\n  good(1) = {good(1)}")
print(f"  good(2) = {good(2)}")

print("\n  不可变默认值（0, '', None, ()）没有这个问题")


print("=" * 60)
print("6. 实战：用 **kwargs 做配置合并")
print("=" * 60)

DEFAULTS = {"timeout": 30, "retries": 3, "verbose": False}

def connect(host, **options):
    cfg = {**DEFAULTS, **options}     # 后者覆盖前者
    return {"host": host, **cfg}

print(f"  connect('db1')                      -> {connect('db1')}")
print(f"  connect('db1', timeout=5)           -> {connect('db1', timeout=5)}")
print(f"  connect('db1', retries=0, extra=1)  -> {connect('db1', retries=0, extra=1)}")


print("=" * 60)
print("7. 实战：转发所有参数给被包装函数")
print("=" * 60)

def log_call(fn):
    def wrapper(*args, **kwargs):
        print(f"    调用 {fn.__name__}(args={args}, kwargs={kwargs})")
        return fn(*args, **kwargs)
    return wrapper

@log_call
def create_order(order_no, amount, *, remark=""):
    return f"OK {order_no} {amount} {remark}"

print("  create_order('NO1', 100, remark='加急'):")
print(f"  -> {create_order('NO1', 100, remark='加急')}")

print("\n  装饰器必须用 *args/**kwargs，否则无法包装任意签名的函数")
