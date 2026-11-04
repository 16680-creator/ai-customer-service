"""题库参考答案（对应文档 14 篇，30 题）

运行: python 06_exercises/answers.py
      python 06_exercises/run_all.py     # 统一自测入口

直接运行本文件会跑全部有断言的题并输出 PASS/FAIL。
"""

# ---- 模块导入区：标准库 + 类型提示 ----
# 与 Java 的 import 一样是「显式导入」，但 Python 没有包路径层级，直接 import 顶层模块名。
import asyncio                    # 异步事件循环、Semaphore、gather（对应 Java 的 CompletableFuture 部分能力）
import csv                        # CSV 读写
import io                         # 内存流：io.StringIO 把字符串当成文件对象用
import json                       # JSON 序列化/反序列化（对应 Jackson）
import re                         # 正则（对应 java.util.regex）
import tempfile                   # 临时文件/临时目录（对应 Files.createTempDirectory）
import time                       # 时间：perf_counter / sleep / monotonic
from collections import Counter, defaultdict   # 计数器和带默认值的字典
from dataclasses import dataclass, field       # 自动生成 __init__/__eq__ 等（类似 Lombok @Data）
from decimal import Decimal                    # 精确十进制（对应 BigDecimal）
from functools import total_ordering           # 只实现 __eq__/__lt__，自动补齐其余比较运算符
from pathlib import Path                       # 面向对象的路径操作（对应 java.nio.file.Path）
from typing import Protocol                    # 结构化子类型/接口（对应 Java interface 的鸭子版）

# ===========================================================================
# 入门档 1-12
# ===========================================================================


# ---- 第 1 题：列表推导式过滤 + 变换 ----
# 目标：只保留偶数，并返回它们的平方。列表推导式是 Python 最常用的「一行 for」。
def q1_even_squares(nums: list[int]) -> list[int]:
    """1. 偶数的平方"""
    # 读法：对 nums 里每个 x，若 x % 2 == 0 就产出 x * x；等价 Java 的 stream().filter().map().toList()。
    return [x * x for x in nums if x % 2 == 0]


# ---- 第 2 题：Counter 统计字符出现次数 ----
# Counter 是 dict 子类，传入可迭代对象就自动计数；dict(...) 把它转回普通字典便于断言。
def q2_char_count(text: str) -> dict[str, int]:
    """2. 字符计数"""
    return dict(Counter(text))


# ---- 第 3 题：zip 把两个列表按位置配对，再转字典 ----
# zip 会截断到较短的那个列表；dict() 接受「(键, 值) 二元组」的可迭代对象。
def q3_zip_to_dict(keys: list, vals: list) -> dict:
    """3. 两列表合成字典"""
    return dict(zip(keys, vals))


# ---- 第 4 题：可变参数 *nums ----
# *nums 把调用时传的所有位置参数收集成一个元组（Java 的 int... nums）。
# max(空元组) 会抛 ValueError，所以先用 if nums else None 兜底。
def q4_max_of(*nums):
    """4. 任意参数的 max"""
    return max(nums) if nums else None


# ---- 第 5 题：按 "a.b.c" 路径安全取值 ----
# 任何一层缺失或不是 dict 都返回 default，绝不抛 KeyError/TypeError。
def q5_get_nested(data, path: str, default=None):
    """5. 安全取深层值"""
    cur = data                      # 游标，从根对象开始逐层下钻
    for key in path.split("."):     # 按点号拆成 ["a", "b", "c"]；split 返回列表
        if not isinstance(cur, dict) or key not in cur:   # 当前层不是字典，或键不存在
            return default          # 提前返回默认值，后续层级不再检查
        cur = cur[key]              # 下钻一层；cur 重新绑定到子对象
    return cur                      # 全部命中时返回最内层的值


# ---- 第 6 题：FizzBuzz ----
# 返回列表而不是直接打印，方便测试断言；默认参数 n=30 在调用时可不传。
def q6_fizzbuzz(n: int = 30) -> list[str]:
    """6. FizzBuzz，返回结果列表（便于断言）"""
    out = []                        # 结果累加器；Python 列表无需预设容量
    for i in range(1, n + 1):       # range 左闭右开，所以要 +1 才能取到 n
        if i % 15 == 0:             # 同时是 3 和 5 的倍数，必须先判断，否则会被下面的分支截走
            out.append("FizzBuzz")  # append 追加到末尾，等价 list.add
        elif i % 3 == 0:            # Python 用 elif，不是 else if
            out.append("Fizz")
        elif i % 5 == 0:
            out.append("Buzz")
        else:
            out.append(str(i))      # 数字要显式转字符串，列表里才能统一类型
    return out


# ---- 第 7 题：反转单词顺序 ----
# split() 不带参数会按任意空白切分并丢弃空串，所以多个空格/首尾空格都能正确处理。
# reversed 返回迭代器，join 一次性拼起来（Java 要 Collections.reverse + String.join）。
def q7_reverse_words(s: str) -> str:
    """7. 反转单词顺序"""
    return " ".join(reversed(s.split()))


# ---- 第 8 题：回文判断 ----
# 思路：先统一小写，再用正则去掉所有非字母数字，最后和「反转后的自己」比较。
def q8_is_palindrome(s: str) -> bool:
    """8. 判断回文（忽略大小写与非字母数字）"""
    cleaned = re.sub(r"[^a-z0-9]", "", s.lower())   # [^...] 表示取反：保留 a-z、0-9，其余删成空串
    return cleaned == cleaned[::-1]                  # [::-1] 是切片语法：步长 -1，得到反转后的新字符串


# ---- 第 9 题：合并两个字典 ----
# Python 3.9+ 的 | 运算符做字典并集；右侧同名键覆盖左侧，等价 dict1.update(dict2) 但返回新对象。
def q9_merge_dicts(a: dict, b: dict) -> dict:
    """9. 合并字典，后者覆盖前者"""
    return a | b


# ---- 第 10 题：去重但保留第一次出现的顺序 ----
# dict.fromkeys 用元素当键、值全为 None；字典保持插入序，所以重复元素自然被丢掉。
# set 也能去重，但顺序不保证，所以这里用 dict 而不是 set。
def q10_dedup_keep_order(items: list) -> list:
    """10. 去重保序"""
    return list(dict.fromkeys(items))


# ---- 第 11 题：金额格式化 ----
# str(v) 先转字符串，避免把已经是 float 的误差带进 Decimal（如 Decimal(0.1) 会带一长串小数）。
# f-string 的 :,.2f = 千分位逗号 + 保留两位小数 + 四舍五入。
def q11_format_money(v) -> str:
    """11. 格式化金额"""
    return f"{Decimal(str(v)):,.2f}"


# ---- 第 12 题：统计文件行数 ----
# 逐行迭代文件对象，不会把整个文件读进内存，适合大文件；sum 对每行计 1。
def q12_count_lines(path: str) -> int:
    """12. 计算文件行数（大文件友好）"""
    with open(path, encoding="utf-8") as f:   # with 保证异常时也会关闭文件
        return sum(1 for _ in f)              # 生成器表达式：每读一行产出 1，sum 求和


# ===========================================================================
# 进阶档 13-24
# ===========================================================================


# ---- 第 13 题：多字段排序（升/降序混用）----
# sorted 的 key 返回一个元组，Python 按元组逐项比较：先 amount，相同再比 order_no。
# amount 前加负号实现「降序」，order_no 不变即升序；等价 Java 的 Comparator
# .comparing(..., reverseOrder()).thenComparing(...)。
def q13_sort_orders(orders: list[dict]) -> list[dict]:
    """13. amount 降序、order_no 升序"""
    return sorted(orders, key=lambda o: (-o["amount"], o["order_no"]))


# ---- 第 14 题：一次遍历完成分组计数与求和 ----
# defaultdict 的默认值工厂是 lambda，每次遇到新分类就自动造一个全新的 dict，
# 避免「先判断 key 在不在，再初始化」的样板代码。
def q14_group_stats(records: list[dict]) -> dict[str, dict]:
    """14. 分组统计 count + total"""
    acc = defaultdict(lambda: {"count": 0, "total": 0})   # 工厂函数：每个新 key 得到独立字典
    for r in records:                    # 逐条记录累加
        slot = acc[r["cat"]]             # 按分类取桶；key 不存在时自动创建默认字典
        slot["count"] += 1               # 计数 +1（字典是可变对象，原地修改会反映到 acc 里）
        slot["total"] += r["amt"]        # 金额累加
    return dict(acc)                     # 转成普通 dict；避免调用方看到 defaultdict 的自动建键行为


# ---- 第 15 题：带参数的三层嵌套装饰器 ----
# 结构：q15_retry(...) 返回 decorator；decorator(fn) 返回 wrapper；wrapper 才是真正被调用的函数。
# 多这一层是为了让装饰器本身能接收 times/delay 等配置（Java 注解参数类似）。
def q15_retry(times=3, delay=0.0, exceptions=(Exception,)):
    """15. 带参数的重试装饰器"""
    import functools                     # 局部导入：只有用到重试装饰器时才需要

    def decorator(fn):                   # 真正接收被装饰函数的装饰器
        @functools.wraps(fn)             # 保留原函数名/文档，否则 pytest、日志都会看到 "wrapper"
        def wrapper(*args, **kwargs):    # 原样转发任意参数
            last = None                  # 记住最后一次异常，全部失败后抛出
            for attempt in range(1, times + 1):   # 第 1 次到第 times 次
                try:
                    return fn(*args, **kwargs)    # 成功就直接返回，不再重试
                except exceptions as e:  # 只捕获指定类型的异常（默认所有 Exception）
                    last = e             # 暂存异常对象
                    if attempt < times and delay:   # 最后一次失败后不再等待；delay=0 也不 sleep
                        time.sleep(delay)           # 固定间隔重试（生产常用指数退避）
            raise last                   # 循环结束仍未成功，抛出最后一次异常

        return wrapper                   # 用 wrapper 替换原函数

    return decorator                     # 把配置闭包进 decorator


# ---- 第 16 题：分批生成器 ----
# yield 让函数变成生成器：每攒满 size 个就产出一批，调用方用 for 逐个消费，内存只保留一批。
def q16_batch(items, size: int):
    """16. 分批生成器"""
    buf = []                             # 当前批次的缓冲区
    for it in items:                     # items 可以是任意可迭代对象（列表、range、生成器）
        buf.append(it)
        if len(buf) == size:             # 攒满一批
            yield buf                    # 产出当前批次；函数在此暂停，下次继续
            buf = []                     # 关键：重新绑定新列表，不能写 buf.clear()，否则已产出的批次会被清空
    if buf:                              # 处理最后不足一批的剩余元素
        yield buf


# ---- 第 17 题：值对象 Money ----
# @dataclass 自动生成 __init__/__repr__/__eq__；frozen=True 让实例不可变，同时自动生成 __hash__。
# @total_ordering 只需 __eq__ 和 __lt__，就能推导出 <=、>、>=。两者叠加等价 Java 的 record + Comparable。
@total_ordering
@dataclass(frozen=True)
class Money:
    """17. Money：支持 +、==、排序"""
    amount: float                        # 字段声明带类型注解；dataclass 据此生成构造器参数
    currency: str = "CNY"                # 带默认值的字段必须放在无默认值字段之后

    def _check(self, other):             # 内部辅助方法，下划线开头表示「非公开 API」
        if not isinstance(other, Money): # 类型检查：Python 不会自动保证运算对象类型
            raise TypeError("只能和 Money 运算")
        if self.currency != other.currency:   # 业务规则：不同币种不能直接相加/比较
            raise ValueError(f"币种不一致：{self.currency} vs {other.currency}")

    def __add__(self, other):            # 重载 + 运算符（Java 没有运算符重载，只能写 add 方法）
        self._check(other)               # 先校验类型与币种
        return Money(self.amount + other.amount, self.currency)   # 返回新对象（frozen 不可变）

    def __lt__(self, other):             # 重载 <；total_ordering 会据此补全其他比较
        self._check(other)
        return self.amount < other.amount

    def __str__(self):                   # 重载 str()/print() 的显示形式（Java 的 toString）
        return f"{self.amount:.2f} {self.currency}"   # .2f 表示保留两位小数


# ---- 第 18 题：用 contextmanager 写上下文管理器 ----
# 装饰器把「生成器函数」变成 with 可用的对象：yield 之前是进入逻辑，之后是退出逻辑。
# Java 对应 AutoCloseable + try-with-resources。
def q18_timer_trace() -> list[str]:
    """18. 上下文管理器计时器（返回记录以便断言）"""
    from contextlib import contextmanager   # 标准库工具：用生成器快速实现 with 协议

    log: list[str] = []                  # 记录每次 with 块的耗时，便于测试断言

    @contextmanager
    def timer(label=""):
        start = time.perf_counter()      # 高精度计时起点；比 time.time() 更适合测耗时
        try:
            yield                        # 暂停在这里，把控制权交给 with 块内的代码
        finally:
            # 无论 with 块正常结束还是抛异常，finally 都会执行，保证一定记录耗时。
            log.append(f"{label}:{(time.perf_counter()-start)*1000:.2f}ms")

    with timer("计算"):                  # 进入时执行到 yield，退出时继续执行 finally
        sum(range(1000))                 # 被计时的代码：求和，结果丢弃

    try:
        with timer("出错"):              # 演示异常场景下也能记录
            raise RuntimeError("boom")   # 主动抛异常，验证 finally 分支
    except RuntimeError:
        pass                             # 异常在这里被吞掉，不影响后续断言

    return log                           # 应包含「计算」和「出错」两条记录


# ---- 第 19 题：安全地把字符串转成 int ----
# 支持 "1,234" 和 " 42 " 这类带千分位/空白的输入；任何失败都返回默认值，不抛异常。
def q19_safe_int(s, default=0) -> int:
    """19. 安全整数转换（支持千分位）"""
    if s is None:                        # None 直接返回默认值，避免 str(None) 得到 "None" 再报错
        return default
    try:
        return int(re.sub(r"[,\s]", "", str(s)))   # 先删掉逗号和空白，再转 int；str(s) 兼容数字入参
    except (ValueError, TypeError):      # ValueError=内容不是数字，TypeError=类型无法转换
        return default


# ---- 第 20 题：按长度切分文本，但不切断单词 ----
# 贪心策略：能塞进当前行就塞，塞不下就换行；单词本身超过 size 时单独成行。
def q20_chunk(text: str, size: int) -> list[str]:
    """20. 按字符数切分，不切断单词"""
    words = text.split()                 # 按空白拆成单词列表，自动丢弃多余空格
    out, cur = [], ""                    # out=结果行，cur=当前正在拼的这一行
    for w in words:
        candidate = f"{cur} {w}".strip() # 试着把新单词接到当前行；strip 去掉行首多余空格
        if len(candidate) <= size:       # 没超长就接受这次拼接
            cur = candidate
        else:
            if cur:                      # 当前行非空才需要先把上一行收走
                out.append(cur)
            cur = w                      # 新单词开启下一行
    if cur:                              # 循环结束后别漏掉最后一行
        out.append(cur)
    return out


# ---- 第 21 题：用 Protocol 定义「结构化接口」 ----
# Protocol 是静态检查层面的鸭子类型：只要某个类有 save/find_by_id 两个方法，就算实现了它，
# 不需要像 Java 那样显式写 implements。函数体里的 ... 是占位符，表示这里只有签名。
class Repository(Protocol):
    """21. 仓储协议"""
    def save(self, entity: dict) -> None: ...          # 保存实体；返回 None
    def find_by_id(self, id: int) -> dict | None: ...  # 找不到返回 None（联合类型写法）


# ---- 内存版仓储实现：满足上面的 Protocol，但没有显式继承 ----
# 内部用 dict 当「表」，键是实体 id；这也是最简单的 Repository 实现。
class MemoryRepo:
    def __init__(self) -> None:
        self._data: dict[int, dict] = {}   # 实例属性在构造器里初始化；下划线表示内部使用

    def save(self, entity: dict) -> None:
        self._data[entity["id"]] = entity   # 用 id 做键，重复 id 会被覆盖（类似 upsert）

    def find_by_id(self, id: int) -> dict | None:
        return self._data.get(id)           # get 找不到返回 None，不抛 KeyError


# ---- 第 22 题：递归深度合并多个字典 ----
# 规则：遇到「两边都是 dict」的同一个键就递归合并；否则右侧覆盖左侧。
# *dicts 收集任意数量的位置参数；deep=True 是仅关键字参数（调用时必须写 deep=...）。
def q22_merge(*dicts, deep=True) -> dict:
    """22. 深度合并字典"""
    out: dict = {}                        # 累加器，初始为空字典
    for d in dicts:                       # 依次合并每个输入字典（后面的覆盖前面的）
        for k, v in d.items():            # items() 同时拿到键和值，等价 entrySet
            if deep and isinstance(v, dict) and isinstance(out.get(k), dict):
                # 双方都是字典才递归；否则直接赋值会丢掉嵌套的旧键。
                out[k] = q22_merge(out[k], v, deep=True)   # 递归合并子字典，结果替换原值
            else:
                out[k] = v                # 普通值/浅合并模式：直接覆盖
    return out


# ---- 第 23 题：用字典插入序实现 LRU ----
# Python 3.7+ dict 保持插入顺序：把「最近访问」的键删掉再插回，就跑到末尾；
# 淘汰时删掉第一个键，就是「最久未使用」。等价 Java 的 LinkedHashMap(accessOrder=true)。
class LRUCache:
    """23. 手写 LRU（利用 dict 插入序）"""

    def __init__(self, capacity: int):
        self.capacity = capacity          # 容量上限
        self.data: dict = {}              # 键 → 值；键的先后顺序就是访问顺序

    def get(self, key):
        if key not in self.data:          # 未命中直接返回 None
            return None
        self.data[key] = self.data.pop(key)   # pop 后重新插入 = 移到末尾，标记为「最近使用」
        return self.data[key]             # 返回取到的值

    def put(self, key, value) -> None:
        if key in self.data:              # 已存在则先删，保证重新插入后位于末尾
            self.data.pop(key)
        self.data[key] = value            # 新值放末尾
        if len(self.data) > self.capacity:   # 超出容量就淘汰
            del self.data[next(iter(self.data))]   # iter(dict) 产出键，next 取第一个 = 最久未使用


# ---- 第 24 题：asyncio.Semaphore 限制并发数 ----
# Semaphore 是计数信号量：最多允许 limit 个协程同时进入临界区，其余排队等待。
# 返回 (结果列表, 观测到的并发峰值)，便于测试断言「峰值不超过 limit」。
async def q24_run_limited(n: int = 100, limit: int = 10) -> tuple[list[int], int]:
    """24. asyncio 并发限流，返回 (结果, 并发峰值)"""
    sem = asyncio.Semaphore(limit)        # 初始许可数 = 最大并发数
    peak = {"now": 0, "max": 0}           # 用 dict 装可变状态：嵌套函数里改它不需要 nonlocal

    async def guarded(i: int) -> int:     # 每个任务都被信号量包一层
        async with sem:                   # 获取许可；拿不到就挂起等待，不会阻塞事件循环
            peak["now"] += 1              # 进入临界区，当前并发 +1
            peak["max"] = max(peak["max"], peak["now"])   # 更新历史峰值
            try:
                await asyncio.sleep(0.001)   # 模拟 I/O 等待；await 期间让出事件循环
                return i * i              # 返回计算结果
            finally:
                peak["now"] -= 1          # 无论成功/异常都释放计数；信号量本身由 async with 释放

    # gather 并发等待所有协程；星号把生成器展开成一个个位置参数。
    results = await asyncio.gather(*(guarded(i) for i in range(n)))
    return list(results), peak["max"]     # gather 返回结果列表，顺序与传入顺序一致


# ===========================================================================
# 实战档 25-30
# ===========================================================================

# ---- 第 25 题：预编译日志正则，用命名分组提取字段 ----
# 模式串分成多段相邻字符串，Python 会自动拼接（编译期完成），只是为了排版可读。
# ^...$ 锚定整行；\s+ 吃空白；[^\]]+ 表示「不含右括号的一串字符」。
LOG_PATTERN = re.compile(
    r"^(?P<ts>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s+"   # ts：2024-05-20 14:30:00
    r"(?P<level>[A-Z]+)\s+"                                # level：全大写级别
    r"\[(?P<module>[^\]]+)\]\s+"                           # module：方括号里的模块名
    r"(?P<msg>.+)$"                                        # msg：剩余全部内容
)


# ---- 解析日志行并统计各级别出现次数 ----
# 列表推导式里用海象运算符 := 先把匹配结果存进 m，再判断真值，避免重复 match。
def q25_parse_logs(lines: list[str]) -> tuple[list[dict], dict[str, int]]:
    """25. 日志解析 + 级别统计"""
    # 只保留匹配成功的行；groupdict() 把命名分组变成 {"ts":..., "level":...} 字典。
    rows = [m.groupdict() for line in lines if (m := LOG_PATTERN.match(line.strip()))]
    return rows, dict(Counter(r["level"] for r in rows))   # Counter 统计级别，再转普通 dict


# ---- 第 26 题：CSV 聚合成部门总额 ----
# 参数是「CSV 文本」而不是文件路径，测试时不用落盘；io.StringIO 让字符串伪装成文件对象。
def q26_aggregate_csv(csv_text: str) -> dict[str, float]:
    """26. CSV 聚合（从字符串读，便于测试）"""
    totals: dict[str, float] = defaultdict(float)   # 缺省值 0.0，可直接 +=
    for row in csv.DictReader(io.StringIO(csv_text)):   # DictReader 首行当表头
        totals[row["dept"]] += float(row["amount"] or 0)   # 空金额按 0 处理；值都是字符串要转换
    return dict(totals)                     # 转普通 dict，避免自动建键的副作用


# ---- 第 27 题：带重试与降级的异步调用 ----
# fetcher 是「可注入的异步函数」，测试时可传假实现，不必真的发网络请求（依赖注入的思想）。
# 星号 * 之后的 retries/fallback 是仅关键字参数，调用时必须写名字，避免位置参数歧义。
async def q27_fetch_json(fetcher, url: str, *, retries: int = 3, fallback=None):
    """27. 带重试与降级的调用（fetcher 可注入，便于测试）"""
    last = None                             # 记录最后一次超时异常
    for attempt in range(1, retries + 1):   # 尝试 1..retries 次
        try:
            return await fetcher(url)       # await 真正的异步调用；成功立即返回
        except asyncio.TimeoutError as e:   # 只对超时重试；其他异常直接向上抛
            last = e
            if attempt < retries:
                # 指数退避：0.01、0.02、0.04…… 避免失败时瞬间打爆下游。
                await asyncio.sleep(0.01 * 2 ** (attempt - 1))
    print(f"    (全部失败：{type(last).__name__})")   # 降级前的可观测性输出；type(...).__name__ 取类名
    return fallback                         # 全部失败返回兜底值，不抛异常


# ---- 第 28 题：统计字符数与单词数（FastAPI 端点核心逻辑）----
# 与 Web 框架解耦，纯函数便于单测；strip 去掉首尾空白，避免把用户误输入的空格算进字符数。
def q28_count_text(message: str) -> dict[str, int]:
    """28. FastAPI 端点的核心逻辑"""
    text = message.strip()                  # 去首尾空白（含换行、制表符）
    return {"chars": len(text), "words": len(text.split())}   # split() 按任意空白切词


# ---- 第 29 题：识别 asyncio 代码里的阻塞调用 ----
# 这是一张「调用 → 结论」的查表，用来快速判断某个写法是否阻塞事件循环。
BLOCKING_ANALYSIS = {
    "time.sleep(0.5)": "阻塞 —— 改 asyncio.sleep",            # 同步 sleep 会占住线程
    "requests.get(...)": "阻塞 —— 改 httpx.AsyncClient",      # requests 是同步 HTTP 客户端
    "sqlite3.execute(...)": "阻塞 —— 改 asyncio.to_thread",   # 同步 DB 驱动，丢线程池执行
    "open().write(...)": "阻塞 —— 改 asyncio.to_thread",      # 文件 I/O 同样是阻塞调用
    "await asyncio.sleep(0.1)": "正确",                       # 异步 sleep 会让出事件循环
}


# ---- 第 30 题：三态熔断器 ----
# CLOSED（正常放行）→ 连续失败达阈值 → OPEN（直接降级，不再调用）
# → 冷却时间到 → HALF_OPEN（放一个请求试探）→ 成功回 CLOSED / 失败回 OPEN。
# 与 Resilience4j 的 CircuitBreaker 状态机一致。
class CircuitBreaker:
    """30. 熔断器（对照 Resilience4j CircuitBreaker）"""
    CLOSED, OPEN, HALF_OPEN = "closed", "open", "half_open"   # 元组解包：一次给三个类属性赋值

    def __init__(self, threshold: int = 5, cooldown: float = 30.0):
        self.threshold = threshold        # 失败多少次后熔断
        self.cooldown = cooldown          # OPEN 状态持续多少秒后允许试探
        self.state = self.CLOSED          # 初始为正常状态
        self.failures = 0                 # 连续失败计数
        self.opened_at = 0.0              # 进入 OPEN 的时刻（monotonic 秒）

    def allow(self) -> bool:              # 判断本次调用是否放行
        if self.state == self.CLOSED:
            return True                   # 正常状态：全部放行
        if self.state == self.OPEN:
            if time.monotonic() - self.opened_at >= self.cooldown:   # 冷却结束
                self.state = self.HALF_OPEN   # 转半开，放一个请求去试探
                return True
            return False                  # 冷却中：直接拒绝（调用方走降级）
        return True                       # HALF_OPEN：允许试探请求通过

    def on_success(self) -> None:         # 调用成功
        self.state = self.CLOSED          # 无论之前什么状态，成功即恢复正常
        self.failures = 0                 # 失败计数清零

    def on_failure(self) -> None:         # 调用失败
        self.failures += 1
        if self.state == self.HALF_OPEN or self.failures >= self.threshold:
            # 半开状态试探失败，或累计失败达到阈值，都立即熔断。
            self.state = self.OPEN
            self.opened_at = time.monotonic()   # 记录熔断起点；monotonic 不受系统改时间影响

    async def call(self, fn, *args, fallback="降级文案", **kwargs):
        if not self.allow():              # 先过熔断闸门
            return fallback               # 熔断期间直接返回兜底值，绝不发起真实调用
        try:
            result = await fn(*args, **kwargs)   # 真正调用被保护的异步函数
            self.on_success()             # 成功上报
            return result
        except Exception:                 # 任何异常都算失败（生产可按类型细分）
            self.on_failure()             # 失败上报，可能触发熔断
            return fallback               # 降级返回，保证调用方拿到可用结果


# ===========================================================================
# 自测
# ===========================================================================

# ---- 断言辅助函数：比较实际值与期望值，打印 PASS/FAIL 并返回布尔 ----
# 不抛异常而是返回 bool，这样 main 可以跑完所有用例再统一汇总，而不是第一个失败就中断。
def _check(label: str, got, want) -> bool:
    ok = got == want                     # 值比较用 ==（对应 Java 的 equals，不是 is）
    print(f"  [{'PASS' if ok else 'FAIL'}] {label}")   # 三元表达式嵌在 f-string 里
    if not ok:
        print(f"         got  = {got!r}")   # !r 调用 repr()：能看清类型和引号
        print(f"         want = {want!r}")
    return ok


# ---- 自测入口：逐题调用并收集结果 ----
# results 里每个元素是一个 bool；最后 all(results) 决定进程退出码。
def main() -> int:
    results = []                         # 每个 _check 的返回值都 append 进来

    print("入门档 1-12")
    print("-" * 66)                      # 字符串乘法：重复 66 个减号，画分隔线
    results.append(_check("1. 偶数平方", q1_even_squares([1, 2, 3, 4, 5, 6]), [4, 16, 36]))
    results.append(_check("2. 字符计数", q2_char_count("hello"),
                          {"h": 1, "e": 1, "l": 2, "o": 1}))   # 长调用可换行，括号内自动续行
    results.append(_check("3. zip 转字典", q3_zip_to_dict(["a", "b"], [1, 2]), {"a": 1, "b": 2}))
    results.append(_check("4. 任意参数 max", q4_max_of(3, 1, 4, 1, 5), 5))
    results.append(_check("4b. 空参数", q4_max_of(), None))       # 边界用例：不传参数
    results.append(_check("5. 深层取值", q5_get_nested({"a": {"b": {"c": 42}}}, "a.b.c"), 42))
    results.append(_check("5b. 路径缺失", q5_get_nested({}, "a.b.c", 0), 0))
    fb = q6_fizzbuzz(15)                 # 先拿结果，后面按下标取几个关键位置断言
    results.append(_check("6. FizzBuzz", (fb[2], fb[4], fb[14]), ("Fizz", "Buzz", "FizzBuzz")))
    results.append(_check("7. 反转单词", q7_reverse_words("the sky is blue"), "blue is sky the"))
    results.append(_check("8. 回文", q8_is_palindrome("A man, a plan, a canal: Panama"), True))
    results.append(_check("8b. 非回文", q8_is_palindrome("hello"), False))
    results.append(_check("9. 合并字典", q9_merge_dicts({"x": 1, "y": 2}, {"y": 20}),
                          {"x": 1, "y": 20}))                    # 同键 y 被右侧覆盖
    results.append(_check("10. 去重保序", q10_dedup_keep_order([3, 1, 3, 2, 1, 4]), [3, 1, 2, 4]))
    results.append(_check("11. 格式化金额", q11_format_money(1234567.891), "1,234,567.89"))

    # 临时目录上下文：退出 with 时自动递归删除目录，不污染项目文件。
    with tempfile.TemporaryDirectory() as td:
        p = Path(td) / "a.txt"           # 在临时目录里拼一个文件路径
        p.write_text("l1\nl2\nl3\n", encoding="utf-8")   # 写 3 行（末尾换行不额外算一行）
        results.append(_check("12. 文件行数", q12_count_lines(str(p)), 3))   # 函数要 str，Path 先转换

    print()
    print("进阶档 13-24")
    print("-" * 66)
    # 构造测试数据：两条 100 元（B、A），一条 300 元（C），用于验证「金额降序、单号升序」。
    orders = [
        {"order_no": "B", "amount": 100},
        {"order_no": "A", "amount": 100},
        {"order_no": "C", "amount": 300},
    ]
    # 列表推导式只抽出排序后的单号，断言更直观：C(300) 最先，A/B 同额时按单号升序。
    results.append(_check("13. 多字段排序",
                          [o["order_no"] for o in q13_sort_orders(orders)],
                          ["C", "A", "B"]))
    results.append(_check(
        "14. 分组统计",
        # 三条记录两个分类：A 两条共 15，B 一条 20。
        q14_group_stats([{"cat": "A", "amt": 10}, {"cat": "B", "amt": 20}, {"cat": "A", "amt": 5}]),
        {"A": {"count": 2, "total": 15}, "B": {"count": 1, "total": 20}},
    ))

    state = {"n": 0}                     # 用 dict 保存计数器，闭包里可直接修改内部值

    @q15_retry(times=3, exceptions=(ValueError,))   # 把参数传给装饰器工厂
    def flaky():
        state["n"] += 1                  # 记录被调用次数
        if state["n"] < 3:
            raise ValueError("fail")     # 前两次失败，第三次成功，验证重试 3 次
        return "ok"

    # 元组断言：既检查返回值，也检查「恰好调用了 3 次」。
    results.append(_check("15. 重试装饰器", (flaky(), state["n"]), ("ok", 3)))
    results.append(_check("16. 分批", list(q16_batch(range(1, 6), 2)), [[1, 2], [3, 4], [5]]))
    results.append(_check("17. Money 相加", str(Money(10) + Money(5.5)), "15.50 CNY"))
    # 排序后只取 amount 列表，验证 __lt__ 生效（升序）。
    results.append(_check("17b. Money 排序",
                          [m.amount for m in sorted([Money(3), Money(1)])], [1, 3]))
    results.append(_check("18. 计时器日志条数", len(q18_timer_trace()), 2))   # 正常+异常各一条
    results.append(_check("19. safe_int", (q19_safe_int("1,234"), q19_safe_int("abc")), (1234, 0)))
    results.append(_check("20. 切分文本",
                          q20_chunk("hello world foo bar", 12), ["hello world", "foo bar"]))
    repo = MemoryRepo()                  # 直接用内存实现，不需要数据库
    repo.save({"id": 1, "name": "x"})
    results.append(_check("21. MemoryRepo", repo.find_by_id(1)["name"], "x"))
    results.append(_check(
        "22. 深度合并",
        # db 下 a 被覆盖为 9，b 保留为 2，验证递归合并而非整体替换。
        q22_merge({"db": {"a": 1, "b": 2}}, {"db": {"a": 9}}),
        {"db": {"a": 9, "b": 2}},
    ))
    c = LRUCache(2)                      # 容量 2
    c.put("a", 1); c.put("b", 2); c.get("a"); c.put("c", 3)   # 分号连写；访问 a 后 b 变成最久未用
    results.append(_check("23. LRU 淘汰", list(c.data), ["a", "c"]))   # 淘汰 b，dict 顺序为 [a, c]

    # asyncio.run 是「同步入口 → 异步世界」的桥；返回后事件循环自动关闭。
    res, peak = asyncio.run(q24_run_limited(50, 5))
    results.append(_check("24. 并发限流", (len(res), peak <= 5), (50, True)))

    print()
    print("实战档 25-30")
    print("-" * 66)
    # 4 行输入里有 1 行不是日志，解析结果应为 3 行、其中 ERROR 1 条。
    logs = [
        "2024-05-20 14:30:00 ERROR [order] 超时 30s",
        "2024-05-20 14:30:01 INFO [gateway] 请求进入",
        "不是日志行",
        "2024-05-20 14:30:02 WARN [order] 库存偏低",
    ]
    rows, levels = q25_parse_logs(logs)   # 元组解包：同时拿到明细和级别统计
    results.append(_check("25. 日志解析", (len(rows), levels["ERROR"]), (3, 1)))
    results.append(_check(
        "26. CSV 聚合",
        # 内联 CSV 文本：a 部门两条 10+5，b 部门一条 20。
        q26_aggregate_csv("dept,name,amount\na,x,10\na,y,5\nb,z,20\n"),
        {"a": 15.0, "b": 20.0},
    ))

    # 两个假 fetcher：一个永远超时，一个永远成功，用来在不发网络请求的情况下测重试逻辑。
    async def flaky_fetcher(url):
        raise asyncio.TimeoutError       # 模拟超时

    async def ok_fetcher(url):
        return {"ok": url}               # 模拟成功响应

    async def run_27():                  # 把两个异步调用包进一个协程，交给 asyncio.run 统一执行
        a = await q27_fetch_json(flaky_fetcher, "u", retries=2, fallback="兜底")
        b = await q27_fetch_json(ok_fetcher, "u", retries=2, fallback="兜底")
        return a, b

    results.append(_check("27. 重试+降级", asyncio.run(run_27()), ("兜底", {"ok": "u"})))
    results.append(_check("28. count 端点逻辑", q28_count_text("你好 世界"),
                          {"chars": 5, "words": 2}))   # 中文按字符算，空格分隔出 2 个词
    results.append(_check("29. 阻塞调用识别",
                          BLOCKING_ANALYSIS["time.sleep(0.5)"], "阻塞 —— 改 asyncio.sleep"))

    async def always_fail():
        raise RuntimeError("nope")       # 每次调用都失败，用于触发熔断

    async def run_30():
        br = CircuitBreaker(threshold=3, cooldown=60)   # 失败 3 次熔断，冷却 60 秒
        # 连续调用 5 次，前 3 次真实失败，后 2 次直接被熔断
        out = []
        for _ in range(5):
            out.append(await br.call(always_fail))   # 熔断后 call 直接返回 fallback，不会执行 always_fail
            if br.state == "open":       # 已熔断就提前结束，不必再跑满 5 次
                break
        return br.state, len(out)        # 期望 ("open", 3)：第 3 次失败后状态变 open

    results.append(_check("30. 熔断器", asyncio.run(run_30()), ("open", 3)))

    print()
    print("=" * 66)
    print(f"{sum(results)}/{len(results)} 通过")   # bool 相加等于 True 的个数
    print("=" * 66)
    return 0 if all(results) else 1       # 全部通过返回 0；有失败返回 1（Unix 退出码约定）


# 直接运行本文件时执行 main；被测试文件 import 时不执行，避免副作用。
if __name__ == "__main__":
    raise SystemExit(main())             # 把返回值作为进程退出码，等价 sys.exit(main())
