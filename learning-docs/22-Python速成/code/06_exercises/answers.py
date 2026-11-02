"""题库参考答案（对应文档 14 篇，30 题）

运行: python 06_exercises/answers.py
      python 06_exercises/run_all.py     # 统一自测入口

直接运行本文件会跑全部有断言的题并输出 PASS/FAIL。
"""

import asyncio
import csv
import io
import json
import re
import tempfile
import time
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from decimal import Decimal
from functools import total_ordering
from pathlib import Path
from typing import Protocol

# ===========================================================================
# 入门档 1-12
# ===========================================================================


def q1_even_squares(nums: list[int]) -> list[int]:
    """1. 偶数的平方"""
    return [x * x for x in nums if x % 2 == 0]


def q2_char_count(text: str) -> dict[str, int]:
    """2. 字符计数"""
    return dict(Counter(text))


def q3_zip_to_dict(keys: list, vals: list) -> dict:
    """3. 两列表合成字典"""
    return dict(zip(keys, vals))


def q4_max_of(*nums):
    """4. 任意参数的 max"""
    return max(nums) if nums else None


def q5_get_nested(data, path: str, default=None):
    """5. 安全取深层值"""
    cur = data
    for key in path.split("."):
        if not isinstance(cur, dict) or key not in cur:
            return default
        cur = cur[key]
    return cur


def q6_fizzbuzz(n: int = 30) -> list[str]:
    """6. FizzBuzz，返回结果列表（便于断言）"""
    out = []
    for i in range(1, n + 1):
        if i % 15 == 0:
            out.append("FizzBuzz")
        elif i % 3 == 0:
            out.append("Fizz")
        elif i % 5 == 0:
            out.append("Buzz")
        else:
            out.append(str(i))
    return out


def q7_reverse_words(s: str) -> str:
    """7. 反转单词顺序"""
    return " ".join(reversed(s.split()))


def q8_is_palindrome(s: str) -> bool:
    """8. 判断回文（忽略大小写与非字母数字）"""
    cleaned = re.sub(r"[^a-z0-9]", "", s.lower())
    return cleaned == cleaned[::-1]


def q9_merge_dicts(a: dict, b: dict) -> dict:
    """9. 合并字典，后者覆盖前者"""
    return a | b


def q10_dedup_keep_order(items: list) -> list:
    """10. 去重保序"""
    return list(dict.fromkeys(items))


def q11_format_money(v) -> str:
    """11. 格式化金额"""
    return f"{Decimal(str(v)):,.2f}"


def q12_count_lines(path: str) -> int:
    """12. 计算文件行数（大文件友好）"""
    with open(path, encoding="utf-8") as f:
        return sum(1 for _ in f)


# ===========================================================================
# 进阶档 13-24
# ===========================================================================


def q13_sort_orders(orders: list[dict]) -> list[dict]:
    """13. amount 降序、order_no 升序"""
    return sorted(orders, key=lambda o: (-o["amount"], o["order_no"]))


def q14_group_stats(records: list[dict]) -> dict[str, dict]:
    """14. 分组统计 count + total"""
    acc = defaultdict(lambda: {"count": 0, "total": 0})
    for r in records:
        slot = acc[r["cat"]]
        slot["count"] += 1
        slot["total"] += r["amt"]
    return dict(acc)


def q15_retry(times=3, delay=0.0, exceptions=(Exception,)):
    """15. 带参数的重试装饰器"""
    import functools

    def decorator(fn):
        @functools.wraps(fn)
        def wrapper(*args, **kwargs):
            last = None
            for attempt in range(1, times + 1):
                try:
                    return fn(*args, **kwargs)
                except exceptions as e:
                    last = e
                    if attempt < times and delay:
                        time.sleep(delay)
            raise last

        return wrapper

    return decorator


def q16_batch(items, size: int):
    """16. 分批生成器"""
    buf = []
    for it in items:
        buf.append(it)
        if len(buf) == size:
            yield buf
            buf = []
    if buf:
        yield buf


@total_ordering
@dataclass(frozen=True)
class Money:
    """17. Money：支持 +、==、排序"""
    amount: float
    currency: str = "CNY"

    def _check(self, other):
        if not isinstance(other, Money):
            raise TypeError("只能和 Money 运算")
        if self.currency != other.currency:
            raise ValueError(f"币种不一致：{self.currency} vs {other.currency}")

    def __add__(self, other):
        self._check(other)
        return Money(self.amount + other.amount, self.currency)

    def __lt__(self, other):
        self._check(other)
        return self.amount < other.amount

    def __str__(self):
        return f"{self.amount:.2f} {self.currency}"


def q18_timer_trace() -> list[str]:
    """18. 上下文管理器计时器（返回记录以便断言）"""
    from contextlib import contextmanager

    log: list[str] = []

    @contextmanager
    def timer(label=""):
        start = time.perf_counter()
        try:
            yield
        finally:
            log.append(f"{label}:{(time.perf_counter()-start)*1000:.2f}ms")

    with timer("计算"):
        sum(range(1000))

    try:
        with timer("出错"):
            raise RuntimeError("boom")
    except RuntimeError:
        pass

    return log


def q19_safe_int(s, default=0) -> int:
    """19. 安全整数转换（支持千分位）"""
    if s is None:
        return default
    try:
        return int(re.sub(r"[,\s]", "", str(s)))
    except (ValueError, TypeError):
        return default


def q20_chunk(text: str, size: int) -> list[str]:
    """20. 按字符数切分，不切断单词"""
    words = text.split()
    out, cur = [], ""
    for w in words:
        candidate = f"{cur} {w}".strip()
        if len(candidate) <= size:
            cur = candidate
        else:
            if cur:
                out.append(cur)
            cur = w
    if cur:
        out.append(cur)
    return out


class Repository(Protocol):
    """21. 仓储协议"""
    def save(self, entity: dict) -> None: ...
    def find_by_id(self, id: int) -> dict | None: ...


class MemoryRepo:
    def __init__(self) -> None:
        self._data: dict[int, dict] = {}

    def save(self, entity: dict) -> None:
        self._data[entity["id"]] = entity

    def find_by_id(self, id: int) -> dict | None:
        return self._data.get(id)


def q22_merge(*dicts, deep=True) -> dict:
    """22. 深度合并字典"""
    out: dict = {}
    for d in dicts:
        for k, v in d.items():
            if deep and isinstance(v, dict) and isinstance(out.get(k), dict):
                out[k] = q22_merge(out[k], v, deep=True)
            else:
                out[k] = v
    return out


class LRUCache:
    """23. 手写 LRU（利用 dict 插入序）"""

    def __init__(self, capacity: int):
        self.capacity = capacity
        self.data: dict = {}

    def get(self, key):
        if key not in self.data:
            return None
        self.data[key] = self.data.pop(key)
        return self.data[key]

    def put(self, key, value) -> None:
        if key in self.data:
            self.data.pop(key)
        self.data[key] = value
        if len(self.data) > self.capacity:
            del self.data[next(iter(self.data))]


async def q24_run_limited(n: int = 100, limit: int = 10) -> tuple[list[int], int]:
    """24. asyncio 并发限流，返回 (结果, 并发峰值)"""
    sem = asyncio.Semaphore(limit)
    peak = {"now": 0, "max": 0}

    async def guarded(i: int) -> int:
        async with sem:
            peak["now"] += 1
            peak["max"] = max(peak["max"], peak["now"])
            try:
                await asyncio.sleep(0.001)
                return i * i
            finally:
                peak["now"] -= 1

    results = await asyncio.gather(*(guarded(i) for i in range(n)))
    return list(results), peak["max"]


# ===========================================================================
# 实战档 25-30
# ===========================================================================

LOG_PATTERN = re.compile(
    r"^(?P<ts>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s+"
    r"(?P<level>[A-Z]+)\s+"
    r"\[(?P<module>[^\]]+)\]\s+"
    r"(?P<msg>.+)$"
)


def q25_parse_logs(lines: list[str]) -> tuple[list[dict], dict[str, int]]:
    """25. 日志解析 + 级别统计"""
    rows = [m.groupdict() for line in lines if (m := LOG_PATTERN.match(line.strip()))]
    return rows, dict(Counter(r["level"] for r in rows))


def q26_aggregate_csv(csv_text: str) -> dict[str, float]:
    """26. CSV 聚合（从字符串读，便于测试）"""
    totals: dict[str, float] = defaultdict(float)
    for row in csv.DictReader(io.StringIO(csv_text)):
        totals[row["dept"]] += float(row["amount"] or 0)
    return dict(totals)


async def q27_fetch_json(fetcher, url: str, *, retries: int = 3, fallback=None):
    """27. 带重试与降级的调用（fetcher 可注入，便于测试）"""
    last = None
    for attempt in range(1, retries + 1):
        try:
            return await fetcher(url)
        except asyncio.TimeoutError as e:
            last = e
            if attempt < retries:
                await asyncio.sleep(0.01 * 2 ** (attempt - 1))
    print(f"    (全部失败：{type(last).__name__})")
    return fallback


def q28_count_text(message: str) -> dict[str, int]:
    """28. FastAPI 端点的核心逻辑"""
    text = message.strip()
    return {"chars": len(text), "words": len(text.split())}


BLOCKING_ANALYSIS = {
    "time.sleep(0.5)": "阻塞 —— 改 asyncio.sleep",
    "requests.get(...)": "阻塞 —— 改 httpx.AsyncClient",
    "sqlite3.execute(...)": "阻塞 —— 改 asyncio.to_thread",
    "open().write(...)": "阻塞 —— 改 asyncio.to_thread",
    "await asyncio.sleep(0.1)": "正确",
}


class CircuitBreaker:
    """30. 熔断器（对照 Resilience4j CircuitBreaker）"""
    CLOSED, OPEN, HALF_OPEN = "closed", "open", "half_open"

    def __init__(self, threshold: int = 5, cooldown: float = 30.0):
        self.threshold = threshold
        self.cooldown = cooldown
        self.state = self.CLOSED
        self.failures = 0
        self.opened_at = 0.0

    def allow(self) -> bool:
        if self.state == self.CLOSED:
            return True
        if self.state == self.OPEN:
            if time.monotonic() - self.opened_at >= self.cooldown:
                self.state = self.HALF_OPEN
                return True
            return False
        return True

    def on_success(self) -> None:
        self.state = self.CLOSED
        self.failures = 0

    def on_failure(self) -> None:
        self.failures += 1
        if self.state == self.HALF_OPEN or self.failures >= self.threshold:
            self.state = self.OPEN
            self.opened_at = time.monotonic()

    async def call(self, fn, *args, fallback="降级文案", **kwargs):
        if not self.allow():
            return fallback
        try:
            result = await fn(*args, **kwargs)
            self.on_success()
            return result
        except Exception:
            self.on_failure()
            return fallback


# ===========================================================================
# 自测
# ===========================================================================

def _check(label: str, got, want) -> bool:
    ok = got == want
    print(f"  [{'PASS' if ok else 'FAIL'}] {label}")
    if not ok:
        print(f"         got  = {got!r}")
        print(f"         want = {want!r}")
    return ok


def main() -> int:
    results = []

    print("入门档 1-12")
    print("-" * 66)
    results.append(_check("1. 偶数平方", q1_even_squares([1, 2, 3, 4, 5, 6]), [4, 16, 36]))
    results.append(_check("2. 字符计数", q2_char_count("hello"),
                          {"h": 1, "e": 1, "l": 2, "o": 1}))
    results.append(_check("3. zip 转字典", q3_zip_to_dict(["a", "b"], [1, 2]), {"a": 1, "b": 2}))
    results.append(_check("4. 任意参数 max", q4_max_of(3, 1, 4, 1, 5), 5))
    results.append(_check("4b. 空参数", q4_max_of(), None))
    results.append(_check("5. 深层取值", q5_get_nested({"a": {"b": {"c": 42}}}, "a.b.c"), 42))
    results.append(_check("5b. 路径缺失", q5_get_nested({}, "a.b.c", 0), 0))
    fb = q6_fizzbuzz(15)
    results.append(_check("6. FizzBuzz", (fb[2], fb[4], fb[14]), ("Fizz", "Buzz", "FizzBuzz")))
    results.append(_check("7. 反转单词", q7_reverse_words("the sky is blue"), "blue is sky the"))
    results.append(_check("8. 回文", q8_is_palindrome("A man, a plan, a canal: Panama"), True))
    results.append(_check("8b. 非回文", q8_is_palindrome("hello"), False))
    results.append(_check("9. 合并字典", q9_merge_dicts({"x": 1, "y": 2}, {"y": 20}),
                          {"x": 1, "y": 20}))
    results.append(_check("10. 去重保序", q10_dedup_keep_order([3, 1, 3, 2, 1, 4]), [3, 1, 2, 4]))
    results.append(_check("11. 格式化金额", q11_format_money(1234567.891), "1,234,567.89"))

    with tempfile.TemporaryDirectory() as td:
        p = Path(td) / "a.txt"
        p.write_text("l1\nl2\nl3\n", encoding="utf-8")
        results.append(_check("12. 文件行数", q12_count_lines(str(p)), 3))

    print()
    print("进阶档 13-24")
    print("-" * 66)
    orders = [
        {"order_no": "B", "amount": 100},
        {"order_no": "A", "amount": 100},
        {"order_no": "C", "amount": 300},
    ]
    results.append(_check("13. 多字段排序",
                          [o["order_no"] for o in q13_sort_orders(orders)],
                          ["C", "A", "B"]))
    results.append(_check(
        "14. 分组统计",
        q14_group_stats([{"cat": "A", "amt": 10}, {"cat": "B", "amt": 20}, {"cat": "A", "amt": 5}]),
        {"A": {"count": 2, "total": 15}, "B": {"count": 1, "total": 20}},
    ))

    state = {"n": 0}

    @q15_retry(times=3, exceptions=(ValueError,))
    def flaky():
        state["n"] += 1
        if state["n"] < 3:
            raise ValueError("fail")
        return "ok"

    results.append(_check("15. 重试装饰器", (flaky(), state["n"]), ("ok", 3)))
    results.append(_check("16. 分批", list(q16_batch(range(1, 6), 2)), [[1, 2], [3, 4], [5]]))
    results.append(_check("17. Money 相加", str(Money(10) + Money(5.5)), "15.50 CNY"))
    results.append(_check("17b. Money 排序",
                          [m.amount for m in sorted([Money(3), Money(1)])], [1, 3]))
    results.append(_check("18. 计时器日志条数", len(q18_timer_trace()), 2))
    results.append(_check("19. safe_int", (q19_safe_int("1,234"), q19_safe_int("abc")), (1234, 0)))
    results.append(_check("20. 切分文本",
                          q20_chunk("hello world foo bar", 12), ["hello world", "foo bar"]))
    repo = MemoryRepo()
    repo.save({"id": 1, "name": "x"})
    results.append(_check("21. MemoryRepo", repo.find_by_id(1)["name"], "x"))
    results.append(_check(
        "22. 深度合并",
        q22_merge({"db": {"a": 1, "b": 2}}, {"db": {"a": 9}}),
        {"db": {"a": 9, "b": 2}},
    ))
    c = LRUCache(2)
    c.put("a", 1); c.put("b", 2); c.get("a"); c.put("c", 3)
    results.append(_check("23. LRU 淘汰", list(c.data), ["a", "c"]))

    res, peak = asyncio.run(q24_run_limited(50, 5))
    results.append(_check("24. 并发限流", (len(res), peak <= 5), (50, True)))

    print()
    print("实战档 25-30")
    print("-" * 66)
    logs = [
        "2024-05-20 14:30:00 ERROR [order] 超时 30s",
        "2024-05-20 14:30:01 INFO [gateway] 请求进入",
        "不是日志行",
        "2024-05-20 14:30:02 WARN [order] 库存偏低",
    ]
    rows, levels = q25_parse_logs(logs)
    results.append(_check("25. 日志解析", (len(rows), levels["ERROR"]), (3, 1)))
    results.append(_check(
        "26. CSV 聚合",
        q26_aggregate_csv("dept,name,amount\na,x,10\na,y,5\nb,z,20\n"),
        {"a": 15.0, "b": 20.0},
    ))

    async def flaky_fetcher(url):
        raise asyncio.TimeoutError

    async def ok_fetcher(url):
        return {"ok": url}

    async def run_27():
        a = await q27_fetch_json(flaky_fetcher, "u", retries=2, fallback="兜底")
        b = await q27_fetch_json(ok_fetcher, "u", retries=2, fallback="兜底")
        return a, b

    results.append(_check("27. 重试+降级", asyncio.run(run_27()), ("兜底", {"ok": "u"})))
    results.append(_check("28. count 端点逻辑", q28_count_text("你好 世界"),
                          {"chars": 5, "words": 2}))
    results.append(_check("29. 阻塞调用识别",
                          BLOCKING_ANALYSIS["time.sleep(0.5)"], "阻塞 —— 改 asyncio.sleep"))

    async def always_fail():
        raise RuntimeError("nope")

    async def run_30():
        br = CircuitBreaker(threshold=3, cooldown=60)
        # 连续调用 5 次，前 3 次真实失败，后 2 次直接被熔断
        out = []
        for _ in range(5):
            out.append(await br.call(always_fail))
            if br.state == "open":
                break
        return br.state, len(out)

    results.append(_check("30. 熔断器", asyncio.run(run_30()), ("open", 3)))

    print()
    print("=" * 66)
    print(f"{sum(results)}/{len(results)} 通过")
    print("=" * 66)
    return 0 if all(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
