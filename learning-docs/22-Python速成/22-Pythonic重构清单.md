# 22 · Pythonic 重构清单

> 判断 Python 学没学会，不是看能不能跑，而是看有没有把 Java 的抽象习惯原样搬过来。
> 这篇给你 24 组“能跑但不够 Pythonic → 更自然的写法”，用于 code review 和自我重构。

---

## 一、少写不必要的类和样板代码

### 1. POJO + getter/setter

```python
# Java 风格
# ---- 本段讲：Java 的 POJO + getter/setter 习惯搬进 Python 后长什么样 ----
# 问题：为每个字段手写一对 getter/setter，是 Java 因缺少"属性访问器"语法造成的样板代码；
# Python 的属性访问本来就是公开的，这套封装只会让调用方写 o.get_order_no() 而不是 o.order_no。
class Order:
    def __init__(self, order_no, amount):   # 构造函数：Java 里对应构造器/字段初始化
        self._order_no = order_no           # 前缀下划线是"约定私有"，只是提示，不强制（Java 的 private 是语言级强制）
        self._amount = amount

    def get_order_no(self):                 # Java 习惯的 getter：Python 里没人这么写
        return self._order_no

    def get_amount(self):
        return self._amount
```

```python
# Pythonic
# ---- 本段讲：用 dataclass 替代 POJO + getter/setter ----
# dataclass 相当于 Java 的 Lombok @Value/@Data + 记录类：自动生成 __init__、__repr__、__eq__。
from dataclasses import dataclass   # 标准库，无需安装；Java 里没有等价内置物，要靠 Lombok 或 record

@dataclass(frozen=True)             # frozen=True 生成后实例不可变（字段赋值会抛异常），等价 Java record 的不可变语义
class Order:
    order_no: str                   # 直接声明公开字段 + 类型注解；调用方直接写 order.order_no，无需 getter
    amount: Decimal                 # 类型注解给 mypy/IDE 用，运行时不强制（Java 的泛型擦除后其实也差不多）
```

需要校验再加 `@property`；跨系统边界用 Pydantic。

### 2. Builder 模式

```python
# 不必要
# ---- 本段讲：Java 的 Builder 模式在 Python 里通常是多余的 ----
# Java 用 Builder 解决"构造参数多、想要可选参数、不能命名的多个构造器"的问题；
# Python 用默认参数 + 关键字参数天生就解决了，Builder 只是转译 Java 痛点的产物。
RequestBuilder().message("hi").temperature(0.7).build()   # 链式调用每个方法都返回 self，纯样板；每次调用都有对象开销
```

```python
# ---- 本段讲：Builder 的 Pythonic 替代——关键字参数构造 ----
# 一次调用直接构造，参数名自解释，顺序随意；等价于 Java 的 new ChatRequest("hi", 0.7)
# 但 Java 构造器靠位置区分参数，这里靠名字区分，可读性反而更好。
ChatRequest(message="hi", temperature=0.7)   # temperature 若有默认值可不传；每个关键字参数即"可选项"
```

默认参数 + 关键字参数已经解决可读性和可选参数问题。

### 3. 只有一个公共方法的 Service 类

```python
# 没必要为了“分层”制造类
# ---- 本段讲：只有一个公共方法的类，应降级为函数 ----
# Java 里"万物皆对象"，一个转换逻辑也要包成 XxxService/XxxFormatter；
# Python 里函数是一等公民，无状态转换直接写函数，少一层间接、少一个 self。
class Formatter:
    def format(self, value): ...   # 类里只有一个方法且不依赖任何实例状态——self 完全是摆设
```

```python
# ---- 本段讲：单方法类的函数式替代 ----
# def + 类型注解即声明；调用方直接 format_amount(value)，比 Formatter().format(value) 短且清晰。
def format_amount(value: Decimal) -> str:   # 参数带类型、返回带类型，等价 Java 的方法签名
    return f"{value:,.2f}"                  # f-string 格式化：千分位 + 两位小数，相当于 String.format("%,.2f", value)
```

有状态、有依赖、有生命周期时才更需要类。

### 4. 每个依赖都抽接口

```python
# 内部只有一个实现时，先直接传对象
# ---- 本段讲：不要为单一实现抽接口（Java 的 IXxx + XxxImpl 习惯） ----
# Java 抽接口常是为了 Mockito mock 和 DI；Python 用 monkeypatch/duck typing 就能替换依赖，
# 不需要先定义 Protocol 才能传"像那个东西"的对象。
service = ChatService(llm=client)   # 依赖通过构造参数传入（仍是构造注入思想），但 client 无需实现任何接口
```

需要静态约束或多种实现时再加 `Protocol`，不要复制 Java 的 `IXxx + XxxImpl` 全套。

---

## 二、循环与容器

### 5. 手工维护下标

```python
# ---- 本段讲：手工维护下标遍历列表 ----
# 坏味道：range(len(items)) 生成 0..n-1，再用 items[i] 取元素——这是 C/Java 的写法，
# 多了一次下标访问，读代码的人要自己把 i 和 items[i] 脑内对齐。
for i in range(len(items)):   # len(items) 先求长度，range 生成下标序列；本身没错，只是绕
    print(i, items[i])        # 每次迭代做两次操作：取 i、按下标取元素
```

```python
# ---- 本段讲：enumerate——同时拿下标和元素的正统写法 ----
# enumerate 返回 (下标, 元素) 的迭代器，一次解包两个变量；等价 Java 的带计数 for 循环。
for i, item in enumerate(items):   # 默认从 0 开始计数；需要从 1 开始可写 enumerate(items, start=1)
    print(i, item)                 # 元素直接拿到，无需再按 i 查一次列表
```

### 6. map/filter 式循环

```python
# ---- 本段讲：用"循环 + if + append"实现 map/filter（Java Stream 的老路） ----
# 坏味道：三行做的事其实就是 Java 的 orders.stream().filter(o -> o.amount > 100)
# .map(o -> o.order_no).toList()；初始化空列表、循环、条件、追加各占一处，意图被拆散。
result = []                      # 先声明结果容器——Java 思维里"先 new 一个 ArrayList"
for order in orders:             # 遍历源集合
    if order.amount > 100:       # 过滤条件写在循环体内
        result.append(order.order_no)   # 命中才追加；四处代码合起来才表达一个意图
```

```python
# ---- 本段讲：列表推导式——一行同时表达"映射 + 过滤" ----
# 结构固定为 [表达式 for 变量 in 序列 if 条件]，从左到右读就是"对每个 o，取 o.order_no，只要金额>100"；
# 等价 Java Stream 的 map+filter+collect，且无中间 builder 对象。
result = [o.order_no for o in orders if o.amount > 100]   # 注意：条件复杂（多分支/嵌套）时，普通循环反而更可读，不要硬压成一行
```

过滤条件复杂时，保留普通循环反而更清楚。

### 7. 手工拼接两个列表

```python
# ---- 本段讲：用下标同时遍历两个列表来拼字典 ----
# 坏味道：假设 keys 和 values 等长，靠 range(len(...)) 对齐两个列表——错位风险高、不直观；
# Java 里这对应 for (int i...) map.put(keys.get(i), values.get(i))。
for i in range(len(keys)):
    result[keys[i]] = values[i]   # 逐个按下标取两边再写入；若两列表长度不同会静默出错或越界
```

```python
# ---- 本段讲：zip 一次成对地遍历多个序列 ----
# zip 把 keys/values 按位置配对成 (k, v) 迭代器，dict() 直接收 (k, v) 对构造字典；
# 等价 Java 8 后的 IntStream.range(0, keys.size()).mapToObj(...) 之类，但简短得多。
result = dict(zip(keys, values))   # zip 以较短的序列为准截断；两序列必须事先保证等长对应
```

### 8. 手工判空计数/分组

```python
# ---- 本段讲：分组前先手工判空再建容器 ----
# 坏味道：每次 append 前都要"检查键在不在，不在就先放个空列表"——两行样板散落在调用处；
# Java 里对应 map.computeIfAbsent(key, k -> new ArrayList<>()).add(item) 的手写版。
if key not in grouped:        # 第一次遇到该键？
    grouped[key] = []         # 先初始化空列表——纯防御性样板
grouped[key].append(item)     # 再追加；逻辑正确，但每个使用点都要重复这两步
```

```python
# ---- 本段讲：defaultdict——访问不存在的键时自动创建默认值 ----
# defaultdict(list) 表示"取/写一个不存在的键时，先自动调 list() 造一个空列表挂上去"；
# 于是调用处永远不需要判空初始化，分组逻辑只剩一行。
grouped = defaultdict(list)     # 从 collections 导入；等价 Java 的 HashMap 包 computeIfAbsent，但更省
grouped[key].append(item)       # 键不存在也会自动建列表再 append，一行搞定
```

### 9. 多次循环求聚合

```python
# ---- 本段讲：聚合用三次循环 + 三个中间变量 ----
# 坏味道：过滤、映射、求和拆成三步，每步都建一个完整列表占内存；
# 这正是 Java Stream filter/map/reduce 链想解决的问题——链式一次完成，不落中间集合。
valid = [x for x in items if x.valid]    # 第一遍循环：筛出有效项，建一个新列表
amounts = [x.amount for x in valid]      # 第二遍循环：从 valid 再抽金额，又建一个新列表
total = sum(amounts)                     # 第三遍：求和；前面两个中间列表用完即弃
```

```python
# ---- 本段讲：生成器表达式 + sum 一次完成聚合 ----
# 圆括号里是"生成器表达式"：惰性逐个产出，不建中间列表，内存 O(1)、只扫一遍；
# sum 直接消费它；等价 Java 的 items.stream().filter(x -> x.valid).mapToDouble(x -> x.amount).sum()。
total = sum(x.amount for x in items if x.valid)   # 注意：作为唯一实参时圆括号可省略成这样；单独写时需 ((...))
```

生成器表达式不建中间列表。

### 10. 手写 any/all

```python
# ---- 本段讲：手写"存在性判断"的循环 ----
# 坏味道：用标志变量 + break 模拟"只要有一个满足即为真"——五行代码表达一个谓词；
# Java 里对应 boolean found = items.stream().anyMatch(i -> i.failed)，本质一样的啰嗦。
found = False                   # 标志位：Java 思维里先声明结果
for item in items:
    if item.failed:
        found = True            # 命中就置位
        break                   # 再跳出；找到后剩余元素本不该继续看
```

```python
# ---- 本段讲：any/all 内建函数替代手写循环 ----
# any/all 接收生成器表达式，短路求值（any 找到真即返回，all 找到假即返回），语义一目了然；
# any 相当 Java anyMatch，all 相当 allMatch，且是语言内置、无需 Stream 脚手架。
found = any(item.failed for item in items)          # 只要有一个 failed 就为 True，短路停止
all_valid = all(item.valid for item in items)       # 全部 valid 才为 True；空序列时 all 返回 True（vacuous truth）
```

### 11. 用 list 做高频存在判断

```python
# ---- 本段讲：用 list 做"是否存在"判断的性能坑 ----
# 原写法本身没语法错误，问题在数据结构：list 的 in 是线性扫描 O(n)，数据量大、判断频繁时是隐藏热点；
# Java 的 ArrayList.contains 同理，Java 老手会换 HashSet，这里一样。
if order_no in valid_order_nos:     # list: O(n)  # 每次判断都要从头扫到尾（或命中处）
```

```python
# ---- 本段讲：先把 list 转成 set，再用 in 判断 ----
# set 基于哈希表，in 是 O(1)；等价 Java 的 new HashSet<>(list).contains(x)。
# 一次性转换的 O(n) 成本，会被多次 O(1) 查询摊薄——前提是判断次数 > 1。
valid_order_nos = set(valid_order_nos)      # list → set：去重 + 建哈希索引
if order_no in valid_order_nos:     # set: O(1)  # 哈希直接定位，与集合大小无关
```

---

## 三、字符串、排序与格式化

### 12. 循环字符串相加

```python
# ---- 本段讲：循环里用 += 拼接字符串 ----
# 反例：每次 += 都要新建一个字符串对象再拷贝旧内容，n 个片段是 O(n²) 的复制开销；
# Java 里 StringBuilder 就是为此而生，Python 的对应解法是 join。
text = ""              # 反例：先建空字符串充当累加器
for part in parts:     # 逐个片段循环
    text += part       # 每轮都产生新字符串对象（字符串不可变），片段越多越慢
```

```python
# ---- 本段讲：str.join 一次拼接所有片段 ----
# 正例：join 先算出总长度、一次性分配结果字符串，O(n)；相当于 Java 的 String.join("", parts)。
text = "".join(parts)   # 以空串为分隔符把 parts 连成一体；无累加器、无循环、无中间对象

```

### 13. 旧式格式化

```python
# ---- 本段讲：%-格式化（旧式 C 风格） ----
# 反例：占位符 %s/%.2f 和数据分离，参数一多就难对齐；等价 Java 的 String.format，但 Python 有更好的选择。
"%s: %.2f" % (name, amount)   # % 运算符把元组依次填入占位符；读代码要在两处之间来回对照

```

```python
# ---- 本段讲：f-string——变量名即占位符的正统写法 ----
# 正例：值直接内嵌在字符串里，格式说明 .2f 跟在变量后，无需在两处对齐；等价 Java 的 text block + formatted。
f"{name}: {amount:.2f}"   # f 前缀启用插值；: .2f 表示保留两位小数，规则与 % 格式化兼容

```

日志例外：`logger.info("%s", value)` 保留惰性格式化优势。

### 14. 写 Comparator 式排序

```python
# ---- 本段讲：key 函数排序——不写 Comparator ----
# 正例：sort 的 key 参数只需提供"排序依据"，Python 自动比较；等价 Java 的 Comparator.comparing(o -> o.amount)。
orders.sort(key=lambda o: o.amount)                       # 单字段：按金额升序，lambda 返回比较键
orders.sort(key=lambda o: (-o.amount, o.order_no))   # 金额降序，单号升序   # 多字段：元组按位比较，负号实现"该字段降序"，替代 Java 的 thenComparing + reversed

```

多字段优先返回元组，不要手写 cmp 函数。

### 15. 手动找最大元素

```python
# ---- 本段讲：max + key 直接拿"整个对象" ----
# 正例：手写循环找最大值要自己维护比较和暂存变量；max(key=) 一行完成，且返回的是元素本身而非键值。
max_order = max(orders, key=lambda o: o.amount)   # 按金额比较，但返回金额最大的那个订单对象；空列表会抛 ValueError

```

`max(..., key=)` 返回的是整个对象，不只是最大值。

---

## 四、空值、异常与资源

### 16. Java 式空判

```python
# ---- 本段讲：Java 式"非 null 且非空串"判断 ----
# 反例：两个条件分开写是 Java 的防御习惯；Python 里非空字符串本身就是真值，多数场景可合并。
# 注意：仅当业务上 None 和 "" 都算"无值"时才能简化，见下面第 3 块的反例。
if value is not None and value != "":   # 先排除 None（避免与 "" 混淆），再排除空串——两步合一

```

如果业务上空字符串也无意义：

```python
# ---- 本段讲：truthiness 简化空值判断 ----
# 正例：空串、None、空列表、0 等都为假，if value 即"有实际内容吗"；等价 Java 的 StringUtils.isNotBlank 思路。
if value:   # 简洁，但语义变宽了——0、空列表也会被当作"无值"，见下一块

```

但数值 `0` 是合法值时不能用 truthiness：

```python
# ---- 本段讲：0 是合法值时必须用 is not None ----
# 反例场景：count=0 是有效数据，但 truthiness 会把 0 当假——这是从 Java 的 != null 迁移时最经典的坑。
# 判 None 一律用 is/is not（None 是单例，身份比较精确且快），不用 ==。
if count is not None:   # 精确只排除"未赋值"，0 和空串都能通过——业务语义不被 truthiness 吞掉

```

### 17. 手动关闭资源

```python
# ---- 本段讲：手动打开 + try/finally 关闭资源（反例） ----
# 反例：靠 finally 兜底关闭，比完全不管强，但仍要自己记得写 finally、记得调 close；
# 一旦 try 里中途 return 或抛异常，稍不留神漏掉收尾就泄漏句柄。等价 Java 的 try-finally。
f = open(path, encoding="utf-8")   # 手动打开文件——必须人工保证后面一定会 close
try:
    ...                            # 真正读取/处理的逻辑
finally:
    f.close()                      # 反例代价：每个 open 都要手写这段收尾，易漏且不够直观
```

```python
# ---- 本段讲：with 上下文管理器（正例） ----
# 正例：with 进入时 open、离开块时无论正常还是异常都自动 close，等价 Java 的 try-with-resources；
# 不需要 finally、不需要手动 close，资源管理交给语言，代码只剩业务本身。
with open(path, encoding="utf-8") as f:   # ✅ 正例：块结束自动 close，异常也不泄漏
    ...                                    # 缩进块内使用文件对象 f
```

### 18. 过度防御式检查

```python
# ---- 本段讲：先判断键存在再取值（反例） ----
# 反例：为了"不 KeyError"先 in 判断、再取一次值——同一个键查了两遍，两行才表达一次语义；
# 等价 Java 的 map.containsKey 后再 get，啰嗦且做了两次哈希查找，两处逻辑还容易改漏。
if "amount" in data:         # 第一遍：先问"键在不在"
    amount = data["amount"]  # 第二遍：确认在，再取一次
```

```python
# ---- 本段讲：dict.get 带默认值（正例） ----
# 正例：get 一次完成"取值 + 缺省兜底"，键不存在时返回默认 0，不需要额外判断，也不会抛 KeyError；
# 等价 Java 的 map.getOrDefault("amount", 0)，但更短。
amount = data.get("amount", 0)   # ✅ 正例：键缺失时用 0 兜底，只查一次哈希
```

需要区分“键不存在”和“值为 None”时仍应显式判断。

### 19. 捕获所有异常然后吞掉

```python
# ---- 本段讲：捕获所有异常然后静默吞掉（反例） ----
# 反例：except Exception 把预期外的错误也一并接住，再 pass 掉——出错时既无日志也无降级，
# 问题被彻底隐藏，调用方以为一切正常，线上排查如同大海捞针。
try:
    process()                 # 可能抛各种异常的调用
except Exception:             # 太宽：把业务 bug、下游故障、拼写错误一起兜住
    pass                      # 最危险的一行：异常被吃掉，外界无从感知
```

```python
# ---- 本段讲：只捕获预期异常 + 记日志 + 明确降级（正例） ----
# 正例：异常类型收窄到真正能处理的两种（超时、连接失败），其余异常继续向上抛；
# 捕获后记录可观察日志并返回降级结果，语义明确，出问题能查到、能解释。
try:
    process()                                                   # 受保护的下游调用
except (TimeoutError, ConnectionError) as exc:                  # ✅ 只接住预期内的网络类异常
    logger.warning("下游失败，走降级：%s", exc)                 # 留下可追踪的日志证据
    return fallback                                             # 返回明确降级值，而不是静默 pass
```

兜底必须有可观察日志和明确降级语义。

---

## 五、函数与 API 设计

### 20. 返回多个含义不明的位置值

```python
# ---- 本段讲：返回多个位置值（裸元组）的反例 ----
# 反例：三个字段平铺成元组返回，调用方靠位置对齐 a, b, c = f()，字段一多或顺序变动就静默错位；
# Java 里会定义一个 DTO 返回，这里图省事用裸元组，牺牲了可读性和可维护性。
return order_no, amount, status       # 调用方容易记错顺序
```

字段稳定且很少时可解包；否则：

```python
# ---- 本段讲：用 dataclass 给返回值命名（正例） ----
# 正例：字段有名字，调用方写 result.amount 而非靠位置猜；增删字段不影响已有访问点，
# 等价 Java 定义一个 record/DTO，但不必手写构造函数等样板。
@dataclass(frozen=True)          # 复用标准库 dataclass：自动生成 __init__/__repr__/__eq__，且实例不可变
class OrderResult:               # 返回值有了明确类型名，契约显式
    order_no: str                # 字段名即语义，调用方按名访问
    amount: Decimal              # 金额字段，类型注解供静态检查
    status: str                  # 状态字段
```

### 21. 布尔参数导致调用不可读

```python
# ---- 本段讲：布尔位置参数导致调用不可读（反例） ----
# 反例：True/False 站在调用处完全看不出含义，"魔法布尔"必须翻函数定义才知道谁是谁；
# 这是 Java 也有的老毛病，Python 有关键字参数可以从根上避免。
create_order(req, True, False)   # ❌ 反例：这两个布尔分别代表什么？读代码时无从判断
```

```python
# ---- 本段讲：关键字参数让布尔含义自解释（正例） ----
# 正例：每个布尔带名字，调用处一眼看懂"通知=是、异步保存=否"，顺序也不再重要；
# 定义侧可用 * 强制这些参数只能按关键字传，从源头杜绝位置传参。
create_order(req, notify=True, async_save=False)   # ✅ 正例：参数名即文档，无需回查签名
```

定义时用 `*` 强制关键字参数。

### 22. 用 `dict` 传所有东西

边界入口可以收 JSON，但进入核心业务后应有明确模型：

```python
# ---- 本段讲：用明确的命令对象代替裸 dict（正例） ----
# 正例：入口参数是带类型、带字段名的模型，IDE 能补全、静态检查能校验；
# 相比 Java 传 Map<String,Object>，这里用 dataclass/Pydantic 把契约显式化。
def create_order(command: CreateOrderCommand) -> OrderResult:   # 参数与返回值都有明确模型，而非 data: dict
```

不要核心业务函数全写成 `def create_order(data: dict)`。

### 23. 在 async 函数里机械包 await

```python
# ---- 本段讲：机械地给函数套 async/await（反例） ----
# 反例：本函数没有真实 IO，只是 await 一个立即返回的调用，凭空增加协程调度开销、拖慢调用链；
# 异步是为了等待 IO 而生的，不该给纯计算/纯转发函数也加上 async。
async def get_name():
    return await get_name_async()     # 如果后者只是立即返回常量，没必要 async
```

异步是为了等待 IO，不是让所有函数都加 `async`。

### 24. 把生成器当列表多次遍历

```python
# ---- 本段讲：生成器被多次遍历（反例） ----
# 反例：生成器是"一次性"的，第一次消费后就被耗尽，第二次遍历得到空结果却不报错——
# Java 的 Stream 同样只能消费一次，需要复用就得重新创建流或物化成集合。
rows = (parse(line) for line in lines)   # 生成器表达式：惰性产出，只能从前往后走一遍
process(rows)                            # 第一次消费：rows 被逐步掏空
write_report(rows)      # 到这里已经耗尽   # 第二次消费：拿到的是空的，且不抛异常——静默出错
```

要么只消费一次，要么显式 `list(...)`（评估内存），要么重新创建生成器。

---

## 六、重构顺序

不要一次把项目“全面 Pythonic 化”。按收益排序：

```text
① 正确性：编码、空值、事务、并发、异常吞掉
② 可测性：外部依赖可注入、时间/网络可控
③ 可读性：超长函数、过深嵌套、无意义抽象
④ Pythonic：推导式、dataclass、pathlib、defaultdict
⑤ 性能：只在测量后进行
```

**每次重构都配测试**。风格变化不应该改变外部行为。

---

## 七、提交前 60 秒自查

- [ ] 有没有可以删掉的 getter/setter、Builder、单方法类？
- [ ] 有没有手写循环可用 `enumerate` / `zip` / 推导式？
- [ ] 有没有 `if key not in d` 可用 `defaultdict` / `setdefault`？
- [ ] 有没有 `open` 没写 `encoding="utf-8"`？
- [ ] 有没有 `dict[key]` 实际应该用 `.get()`？
- [ ] 有没有可变默认参数？
- [ ] 有没有 `except Exception: pass`？
- [ ] async 函数里有没有同步 IO？
- [ ] 新类用 dataclass 还是 Pydantic，选择理由是否明确？
- [ ] 这是“更简洁”，还是只是“更短但更难读”？

---

**上一篇**：[21-AI应用实战-从对话到RAG](21-AI应用实战-从对话到RAG.md)　**下一篇**：[23-安全编码与常见漏洞](23-安全编码与常见漏洞.md)
