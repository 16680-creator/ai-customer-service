"""02 · dataclass：替代 Lombok

运行: python 04_oop/02_dataclass.py
"""

from dataclasses import dataclass, field, asdict, astuple, replace   # dataclass 装饰器 + field 可变默认值 + 三个工具函数

# ============================================================
# 本节演示：手写四个魔术方法 vs @dataclass 四个字段声明
# 为什么重要：Java POJO/DTO 的样板代码（Lombok 帮你生成的那些）在 Python 里由装饰器生成。
# Java 对照：@dataclass ≈ @Getter @Setter @ToString @EqualsAndHashCode @AllArgsConstructor。
# ============================================================

print("=" * 60)
print("1. 手写类 vs dataclass")
print("=" * 60)

# 手写：需要 __init__ / __repr__ / __eq__ / __hash__
class OrderManual:
    def __init__(self, no, amount):
        self.no = no                 # 字段 1：订单号
        self.amount = amount         # 字段 2：金额

    def __repr__(self):
        return f"OrderManual(no={self.no!r}, amount={self.amount!r})"   # 等价 Java toString，形式便于重建对象

    def __eq__(self, other):
        if not isinstance(other, OrderManual):
            return NotImplemented     # 类型不对时返回 NotImplemented，而不是 False（Python 的反射比较协议）
        return (self.no, self.amount) == (other.no, other.amount)       # 逐字段比较，等价 Objects.equals

    def __hash__(self):
        return hash((self.no, self.amount))   # 用同一组字段算哈希，必须与 __eq__ 保持一致


# dataclass：4 行搞定
@dataclass(frozen=True)              # frozen=True：不可变 + 自动生成 __hash__（所以能放进 set）
class Order:
    no: str                          # 类体只写注解，__init__/__repr__/__eq__ 由装饰器生成
    amount: float

print(f"  Order('A', 1)     = {Order('A', 1)}")                        # 自动 __repr__
print(f"  == 值比较         = {Order('A', 1) == Order('A', 1)}")       # 自动 __eq__ → True（Java 不重写 equals 会是 false）
print(f"  可哈希（放 set）  = {len({Order('A', 1), Order('A', 1)})} 个")  # 两个相等对象在 set 中只留一个 → 1 个
print(f"  asdict            = {asdict(Order('A', 1))}")                # 转成 dict：{'no': 'A', 'amount': 1}
print(f"  astuple           = {astuple(Order('A', 1))}")               # 转成 tuple：('A', 1)
print(f"  replace           = {replace(Order('A', 1), amount=99)}")    # 生成改了一个字段的新对象（不可变对象的标准做法）


print("\n" + "=" * 60)
print("2. 默认值：可变对象必须用 default_factory")
print("=" * 60)

# ============================================================
# 本节演示：可变默认值必须用 field(default_factory=list)
# 为什么重要：直接写 items: list[str] = [] 会抛 ValueError——因为默认值在类定义时只创建一次，会被所有实例共享。
# Java 对照：相当于在字段上写 static List items = new ArrayList<>()，每个对象拿到的是同一个列表。
# ============================================================

@dataclass
class Cart:
    user_id: str
    items: list[str] = field(default_factory=list)   # 不能写 = []   ← factory 是"每次实例化都调用一次 list()"
    remark: str = ""                 # 不可变默认值可以直接写

c1, c2 = Cart("u1"), Cart("u2")      # 两个实例，各自拿到独立的空 list
c1.items.append("apple")             # 只影响 c1
print(f"  c1 = {c1}")                # items=['apple']
print(f"  c2 = {c2}   <- 各自独立")   # items=[]，证明没有共享

# 错误写法会直接报错
try:
    @dataclass
    class BadCart:
        items: list[str] = []        # dataclass 在类创建时就检测到可变默认值，直接抛 ValueError
except ValueError as e:
    print(f"\n  写 = [] -> ValueError: {e}")


print("=" * 60)
print("3. frozen / slots / order / kw_only")
print("=" * 60)

# ============================================================
# 本节演示：frozen（不可变+可哈希）、order（自动比较）、slots（省内存）
# 为什么重要：frozen 提供 Java final 做不到的"深度不可变 + 可哈希"；order 免费实现 Comparable。
# Java 对照：frozen≈所有字段 final 且无 setter；order≈实现 Comparable；slots≈字段在编译期固定（Java 本来如此）。
# ============================================================

@dataclass(frozen=True, order=True)  # frozen：赋值即抛异常；order：按字段顺序生成 < <= > >=
class Money:
    amount: float
    currency: str = "CNY"            # 带默认值的字段必须排在必填字段之后

m1, m2 = Money(10), Money(20)        # 只传 amount，currency 用默认值
print(f"  m1 = {m1}")                # 自动 __repr__：Money(amount=10, currency='CNY')
print(f"  m1 < m2          = {m1 < m2}     <- order=True 生成比较符")   # 先比较 amount → True
try:
    m1.amount = 99                   # 试图修改冻结字段
except Exception as e:
    print(f"  改 frozen 字段   -> {type(e).__name__}: {e}")   # FrozenInstanceError

@dataclass(slots=True)               # 3.10+：生成 __slots__，去掉实例 __dict__，省内存
class Point:
    x: int
    y: int

p = Point(1, 2)
print(f"\n  slots=True 的 Point = {p}，没有 __dict__")   # 依然有自动 __repr__，但实例没有属性字典
try:
    p.z = 3                          # 槽位只有 x、y，禁止动态加属性
except AttributeError as e:
    print(f"  动态加属性 -> AttributeError: {e}   <- slots 禁止了")


print("=" * 60)
print("4. 与 pydantic 的分工")
print("=" * 60)

# ============================================================
# 本节演示：dataclass 与 pydantic 的分工（不运行第三方库，只打印说明）
# 为什么重要：选错工具会导致"该校验的地方没校验"或"内部对象白白付出校验开销"。
# Java 对照：dataclass≈普通 POJO；pydantic≈带 Bean Validation 的 DTO（@Valid + @NotNull 等）。
# ============================================================

print("  @dataclass      -> 内部数据结构，零校验，快")   # 内部流转用它
print("  pydantic        -> 跨系统边界（HTTP/配置/消息），自动校验与类型转换")   # 边界用它

# 演示 pydantic 风格（本项目 app/schemas/chat.py 的做法）
# 下面是三引号字符串，只是打印出来的示例文本，不是可执行代码
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

# ============================================================
# 本节演示：实战组合——冻结的行项目 + 可变的订单聚合
# 为什么重要：这是业务代码里最典型的搭配：明细不可变（可安全共享/做缓存键），聚合对象可变（逐条 add）。
# Java 对照：OrderItem≈record（不可变）；OrderSummary≈普通 POJO 带 add 方法；@property≈getSubtotal()。
# ============================================================

from decimal import Decimal          # 金额必须用 Decimal，避免 float 误差

@dataclass(frozen=True)              # 明细一旦创建就不该改
class OrderItem:
    sku: str
    price: Decimal
    qty: int

    @property
    def subtotal(self) -> Decimal:   # 计算属性：每次访问都重算，不需要存储字段
        return self.price * self.qty # Decimal 相乘，精度安全


@dataclass
class OrderSummary:
    order_no: str
    items: list[OrderItem] = field(default_factory=list)   # 可变默认值必须用 factory

    @property
    def total(self) -> Decimal:      # 汇总金额
        return sum((i.subtotal for i in self.items), Decimal("0"))   # 生成器 + 起始值 Decimal("0")，避免 float 参与求和

    def add(self, item: OrderItem) -> "OrderSummary":   # 返回字符串注解，避免前向引用问题（类还没定义完）
        self.items.append(item)      # 追加明细
        return self                  # 返回 self 支持链式调用（类似 Java Builder 风格）


s = OrderSummary("NO001")            # 空订单
s.add(OrderItem("SKU1", Decimal("19.99"), 3))   # 追加第一条
s.add(OrderItem("SKU2", Decimal("5.50"), 2))    # 追加第二条（链式写法也可以：s.add(...).add(...)）
print(f"  订单号   {s.order_no}")
for it in s.items:                   # 遍历明细
    print(f"    {it.sku}  {it.price} x {it.qty} = {it.subtotal}")   # 小计由 @property 现算
print(f"  合计     {s.total}")        # 59.97 + 11.00 = 70.97


print("=" * 60)
print("6. dataclass 的局限")
print("=" * 60)

# ============================================================
# 本节演示：dataclass 的局限——不做运行时类型校验
# 为什么重要：类型注解只是给 mypy/IDE 看的，运行时照样能塞进错误类型，所以外部输入必须用 pydantic 挡一层。
# Java 对照：Java 泛型/字段类型在编译期就约束住（有类型擦除但字段仍强类型），Python 完全没有运行时约束。
# ============================================================

print("  ✗ 不做运行时类型校验（传错类型不会报错）")
print("  ✓ 需要校验 → 用 pydantic")
print("  ✗ 不支持 __post_init__ 之外的自定义构造流程（可以用 object.__setattr__ 绕）")
print("  ✓ frozen + slots 组合后性能最好")


@dataclass
class NoCheck:
    n: int                          # 注解写的是 int，但运行时不会检查

print(f"\n  NoCheck('abc') = {NoCheck('abc')}   <- 不校验，能跑")   # 字符串照样塞进去，直到参与算术运算才炸
