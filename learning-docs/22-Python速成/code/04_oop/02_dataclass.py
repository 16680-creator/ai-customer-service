"""02 · dataclass：替代 Lombok

运行: python 04_oop/02_dataclass.py
"""

from dataclasses import dataclass, field, asdict, astuple, replace

print("=" * 60)
print("1. 手写类 vs dataclass")
print("=" * 60)

# 手写：需要 __init__ / __repr__ / __eq__ / __hash__
class OrderManual:
    def __init__(self, no, amount):
        self.no = no
        self.amount = amount

    def __repr__(self):
        return f"OrderManual(no={self.no!r}, amount={self.amount!r})"

    def __eq__(self, other):
        if not isinstance(other, OrderManual):
            return NotImplemented
        return (self.no, self.amount) == (other.no, other.amount)

    def __hash__(self):
        return hash((self.no, self.amount))


# dataclass：4 行搞定
@dataclass(frozen=True)
class Order:
    no: str
    amount: float

print(f"  Order('A', 1)     = {Order('A', 1)}")
print(f"  == 值比较         = {Order('A', 1) == Order('A', 1)}")
print(f"  可哈希（放 set）  = {len({Order('A', 1), Order('A', 1)})} 个")
print(f"  asdict            = {asdict(Order('A', 1))}")
print(f"  astuple           = {astuple(Order('A', 1))}")
print(f"  replace           = {replace(Order('A', 1), amount=99)}")


print("\n" + "=" * 60)
print("2. 默认值：可变对象必须用 default_factory")
print("=" * 60)

@dataclass
class Cart:
    user_id: str
    items: list[str] = field(default_factory=list)   # 不能写 = []
    remark: str = ""

c1, c2 = Cart("u1"), Cart("u2")
c1.items.append("apple")
print(f"  c1 = {c1}")
print(f"  c2 = {c2}   <- 各自独立")

# 错误写法会直接报错
try:
    @dataclass
    class BadCart:
        items: list[str] = []
except ValueError as e:
    print(f"\n  写 = [] -> ValueError: {e}")


print("=" * 60)
print("3. frozen / slots / order / kw_only")
print("=" * 60)

@dataclass(frozen=True, order=True)
class Money:
    amount: float
    currency: str = "CNY"

m1, m2 = Money(10), Money(20)
print(f"  m1 = {m1}")
print(f"  m1 < m2          = {m1 < m2}     <- order=True 生成比较符")
try:
    m1.amount = 99
except Exception as e:
    print(f"  改 frozen 字段   -> {type(e).__name__}: {e}")

@dataclass(slots=True)
class Point:
    x: int
    y: int

p = Point(1, 2)
print(f"\n  slots=True 的 Point = {p}，没有 __dict__")
try:
    p.z = 3
except AttributeError as e:
    print(f"  动态加属性 -> AttributeError: {e}   <- slots 禁止了")


print("=" * 60)
print("4. 与 pydantic 的分工")
print("=" * 60)

print("  @dataclass      -> 内部数据结构，零校验，快")
print("  pydantic        -> 跨系统边界（HTTP/配置/消息），自动校验与类型转换")

# 演示 pydantic 风格（本项目 app/schemas/chat.py 的做法）
print("""
  项目里的典型用法（app/schemas/chat.py）：
    class ChatRequest(BaseModel):
        message: str = Field(..., min_length=1)
        session_id: str | None = Field(None, alias="sessionId")

  好处：
    - 空 message 自动 422，不用写 if 判断
    - 前端发 camelCase，Python 内部用 snake_case
    - /docs 自动生成 schema
""")


print("=" * 60)
print("5. 实战：订单聚合")
print("=" * 60)

from decimal import Decimal

@dataclass(frozen=True)
class OrderItem:
    sku: str
    price: Decimal
    qty: int

    @property
    def subtotal(self) -> Decimal:
        return self.price * self.qty


@dataclass
class OrderSummary:
    order_no: str
    items: list[OrderItem] = field(default_factory=list)

    @property
    def total(self) -> Decimal:
        return sum((i.subtotal for i in self.items), Decimal("0"))

    def add(self, item: OrderItem) -> "OrderSummary":
        self.items.append(item)
        return self


s = OrderSummary("NO001")
s.add(OrderItem("SKU1", Decimal("19.99"), 3))
s.add(OrderItem("SKU2", Decimal("5.50"), 2))
print(f"  订单号   {s.order_no}")
for it in s.items:
    print(f"    {it.sku}  {it.price} x {it.qty} = {it.subtotal}")
print(f"  合计     {s.total}")


print("=" * 60)
print("6. dataclass 的局限")
print("=" * 60)

print("  ✗ 不做运行时类型校验（传错类型不会报错）")
print("  ✓ 需要校验 → 用 pydantic")
print("  ✗ 不支持 __post_init__ 之外的自定义构造流程（可以用 object.__setattr__ 绕）")
print("  ✓ frozen + slots 组合后性能最好")


@dataclass
class NoCheck:
    n: int

print(f"\n  NoCheck('abc') = {NoCheck('abc')}   <- 不校验，能跑")
