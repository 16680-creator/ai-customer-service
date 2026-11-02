# 22 · Pythonic 重构清单

> 判断 Python 学没学会，不是看能不能跑，而是看有没有把 Java 的抽象习惯原样搬过来。
> 这篇给你 24 组“能跑但不够 Pythonic → 更自然的写法”，用于 code review 和自我重构。

---

## 一、少写不必要的类和样板代码

### 1. POJO + getter/setter

```python
# Java 风格
class Order:
    def __init__(self, order_no, amount):
        self._order_no = order_no
        self._amount = amount

    def get_order_no(self):
        return self._order_no

    def get_amount(self):
        return self._amount
```

```python
# Pythonic
from dataclasses import dataclass

@dataclass(frozen=True)
class Order:
    order_no: str
    amount: Decimal
```

需要校验再加 `@property`；跨系统边界用 Pydantic。

### 2. Builder 模式

```python
# 不必要
RequestBuilder().message("hi").temperature(0.7).build()
```

```python
ChatRequest(message="hi", temperature=0.7)
```

默认参数 + 关键字参数已经解决可读性和可选参数问题。

### 3. 只有一个公共方法的 Service 类

```python
# 没必要为了“分层”制造类
class Formatter:
    def format(self, value): ...
```

```python
def format_amount(value: Decimal) -> str:
    return f"{value:,.2f}"
```

有状态、有依赖、有生命周期时才更需要类。

### 4. 每个依赖都抽接口

```python
# 内部只有一个实现时，先直接传对象
service = ChatService(llm=client)
```

需要静态约束或多种实现时再加 `Protocol`，不要复制 Java 的 `IXxx + XxxImpl` 全套。

---

## 二、循环与容器

### 5. 手工维护下标

```python
for i in range(len(items)):
    print(i, items[i])
```

```python
for i, item in enumerate(items):
    print(i, item)
```

### 6. map/filter 式循环

```python
result = []
for order in orders:
    if order.amount > 100:
        result.append(order.order_no)
```

```python
result = [o.order_no for o in orders if o.amount > 100]
```

过滤条件复杂时，保留普通循环反而更清楚。

### 7. 手工拼接两个列表

```python
for i in range(len(keys)):
    result[keys[i]] = values[i]
```

```python
result = dict(zip(keys, values))
```

### 8. 手工判空计数/分组

```python
if key not in grouped:
    grouped[key] = []
grouped[key].append(item)
```

```python
grouped = defaultdict(list)
grouped[key].append(item)
```

### 9. 多次循环求聚合

```python
valid = [x for x in items if x.valid]
amounts = [x.amount for x in valid]
total = sum(amounts)
```

```python
total = sum(x.amount for x in items if x.valid)
```

生成器表达式不建中间列表。

### 10. 手写 any/all

```python
found = False
for item in items:
    if item.failed:
        found = True
        break
```

```python
found = any(item.failed for item in items)
all_valid = all(item.valid for item in items)
```

### 11. 用 list 做高频存在判断

```python
if order_no in valid_order_nos:     # list: O(n)
```

```python
valid_order_nos = set(valid_order_nos)
if order_no in valid_order_nos:     # set: O(1)
```

---

## 三、字符串、排序与格式化

### 12. 循环字符串相加

```python
text = ""
for part in parts:
    text += part
```

```python
text = "".join(parts)
```

### 13. 旧式格式化

```python
"%s: %.2f" % (name, amount)
```

```python
f"{name}: {amount:.2f}"
```

日志例外：`logger.info("%s", value)` 保留惰性格式化优势。

### 14. 写 Comparator 式排序

```python
orders.sort(key=lambda o: o.amount)
orders.sort(key=lambda o: (-o.amount, o.order_no))   # 金额降序，单号升序
```

多字段优先返回元组，不要手写 cmp 函数。

### 15. 手动找最大元素

```python
max_order = max(orders, key=lambda o: o.amount)
```

`max(..., key=)` 返回的是整个对象，不只是最大值。

---

## 四、空值、异常与资源

### 16. Java 式空判

```python
if value is not None and value != "":
```

如果业务上空字符串也无意义：

```python
if value:
```

但数值 `0` 是合法值时不能用 truthiness：

```python
if count is not None:
```

### 17. 手动关闭资源

```python
f = open(path, encoding="utf-8")
try:
    ...
finally:
    f.close()
```

```python
with open(path, encoding="utf-8") as f:
    ...
```

### 18. 过度防御式检查

```python
if "amount" in data:
    amount = data["amount"]
```

```python
amount = data.get("amount", 0)
```

需要区分“键不存在”和“值为 None”时仍应显式判断。

### 19. 捕获所有异常然后吞掉

```python
try:
    process()
except Exception:
    pass
```

```python
try:
    process()
except (TimeoutError, ConnectionError) as exc:
    logger.warning("下游失败，走降级：%s", exc)
    return fallback
```

兜底必须有可观察日志和明确降级语义。

---

## 五、函数与 API 设计

### 20. 返回多个含义不明的位置值

```python
return order_no, amount, status       # 调用方容易记错顺序
```

字段稳定且很少时可解包；否则：

```python
@dataclass(frozen=True)
class OrderResult:
    order_no: str
    amount: Decimal
    status: str
```

### 21. 布尔参数导致调用不可读

```python
create_order(req, True, False)
```

```python
create_order(req, notify=True, async_save=False)
```

定义时用 `*` 强制关键字参数。

### 22. 用 `dict` 传所有东西

边界入口可以收 JSON，但进入核心业务后应有明确模型：

```python
def create_order(command: CreateOrderCommand) -> OrderResult:
```

不要核心业务函数全写成 `def create_order(data: dict)`。

### 23. 在 async 函数里机械包 await

```python
async def get_name():
    return await get_name_async()     # 如果后者只是立即返回常量，没必要 async
```

异步是为了等待 IO，不是让所有函数都加 `async`。

### 24. 把生成器当列表多次遍历

```python
rows = (parse(line) for line in lines)
process(rows)
write_report(rows)      # 到这里已经耗尽
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
