"""pytest 形式的题库测试（对应文档 09 篇的测试写法）

需要 pytest:
    python -m pip install pytest pytest-asyncio
    python -m pytest 06_exercises/test_answers.py -v

没有装 pytest 时，用 `python 06_exercises/answers.py` 也能验证全部答案。
"""

import asyncio

import pytest

from answers import (
    CircuitBreaker,
    LRUCache,
    MemoryRepo,
    Money,
    q1_even_squares,
    q2_char_count,
    q3_zip_to_dict,
    q4_max_of,
    q5_get_nested,
    q6_fizzbuzz,
    q7_reverse_words,
    q8_is_palindrome,
    q9_merge_dicts,
    q10_dedup_keep_order,
    q11_format_money,
    q13_sort_orders,
    q14_group_stats,
    q16_batch,
    q19_safe_int,
    q20_chunk,
    q22_merge,
    q24_run_limited,
    q25_parse_logs,
    q26_aggregate_csv,
    q27_fetch_json,
    q28_count_text,
)


# ---------------------------------------------------------------------------
# 入门档：参数化写法（对照 JUnit @ParameterizedTest）
# ---------------------------------------------------------------------------
@pytest.mark.parametrize("nums,expected", [
    ([1, 2, 3, 4, 5, 6], [4, 16, 36]),
    ([], []),
    ([1, 3, 5], []),
    ([-2, -3], [4]),
])
def test_q1(nums, expected):
    assert q1_even_squares(nums) == expected


def test_q2_char_count():
    assert q2_char_count("hello") == {"h": 1, "e": 1, "l": 2, "o": 1}
    assert q2_char_count("") == {}


@pytest.mark.parametrize("keys,vals,expected", [
    (["a", "b"], [1, 2], {"a": 1, "b": 2}),
    (["a", "b"], [1], {"a": 1}),                 # zip 截断到最短
    ([], [], {}),
])
def test_q3(keys, vals, expected):
    assert q3_zip_to_dict(keys, vals) == expected


def test_q4_max_of():
    assert q4_max_of(3, 1, 4, 1, 5) == 5
    assert q4_max_of() is None
    assert q4_max_of(-1, -5) == -1


def test_q5_get_nested():
    d = {"a": {"b": {"c": 42}}}
    assert q5_get_nested(d, "a.b.c") == 42
    assert q5_get_nested(d, "a.x.c", 0) == 0
    assert q5_get_nested(d, "a.b.c.d", None) is None
    assert q5_get_nested({}, "x", "dft") == "dft"


def test_q6_fizzbuzz():
    out = q6_fizzbuzz(15)
    assert len(out) == 15
    assert out[2] == "Fizz"
    assert out[4] == "Buzz"
    assert out[14] == "FizzBuzz"
    assert out[0] == "1"


def test_q7_reverse_words():
    assert q7_reverse_words("the sky is blue") == "blue is sky the"
    assert q7_reverse_words("  a   b  ") == "b a"
    assert q7_reverse_words("") == ""


@pytest.mark.parametrize("s,expected", [
    ("A man, a plan, a canal: Panama", True),
    ("hello", False),
    ("", True),
    ("a", True),
])
def test_q8_palindrome(s, expected):
    assert q8_is_palindrome(s) is expected


def test_q9_merge():
    assert q9_merge_dicts({"x": 1, "y": 2}, {"y": 20, "z": 30}) == {"x": 1, "y": 20, "z": 30}
    assert q9_merge_dicts({}, {"a": 1}) == {"a": 1}


def test_q10_dedup():
    assert q10_dedup_keep_order([3, 1, 3, 2, 1, 4]) == [3, 1, 2, 4]
    assert q10_dedup_keep_order([]) == []


def test_q11_format_money():
    assert q11_format_money(1234567.891) == "1,234,567.89"
    assert q11_format_money(0) == "0.00"
    assert q11_format_money("12.5") == "12.50"


# ---------------------------------------------------------------------------
# 进阶档
# ---------------------------------------------------------------------------
def test_q13_sort():
    orders = [
        {"order_no": "B", "amount": 100},
        {"order_no": "A", "amount": 100},
        {"order_no": "C", "amount": 300},
    ]
    assert [o["order_no"] for o in q13_sort_orders(orders)] == ["C", "A", "B"]


def test_q14_group_stats():
    got = q14_group_stats([
        {"cat": "A", "amt": 10},
        {"cat": "B", "amt": 20},
        {"cat": "A", "amt": 5},
    ])
    assert got == {"A": {"count": 2, "total": 15}, "B": {"count": 1, "total": 20}}


def test_q16_batch():
    assert list(q16_batch(range(1, 6), 2)) == [[1, 2], [3, 4], [5]]
    assert list(q16_batch(range(1, 5), 2)) == [[1, 2], [3, 4]]
    assert list(q16_batch([], 2)) == []


def test_money():
    assert str(Money(10) + Money(5.5)) == "15.50 CNY"
    assert Money(1) == Money(1)
    assert Money(1) < Money(2)
    assert len({Money(1), Money(1)}) == 1
    with pytest.raises(ValueError):
        Money(1) + Money(1, "USD")


def test_q19_safe_int():
    assert q19_safe_int("1,234") == 1234
    assert q19_safe_int("  42  ") == 42
    assert q19_safe_int("abc") == 0
    assert q19_safe_int(None, -1) == -1
    assert q19_safe_int(None) == 0


def test_q20_chunk():
    assert q20_chunk("hello world foo bar", 12) == ["hello world", "foo bar"]
    assert q20_chunk("a b c", 1) == ["a", "b", "c"]
    assert q20_chunk("", 5) == []


def test_repo():
    r = MemoryRepo()
    r.save({"id": 1, "name": "x"})
    assert r.find_by_id(1) == {"id": 1, "name": "x"}
    assert r.find_by_id(99) is None


def test_q22_merge():
    assert q22_merge({"db": {"a": 1, "b": 2}}, {"db": {"a": 9}}) == {"db": {"a": 9, "b": 2}}
    assert q22_merge({"db": {"a": 1, "b": 2}}, {"db": {"a": 9}}, deep=False) == {"db": {"a": 9}}


def test_lru():
    c = LRUCache(2)
    c.put("a", 1)
    c.put("b", 2)
    assert c.get("a") == 1
    c.put("c", 3)
    assert list(c.data) == ["a", "c"]
    assert c.get("b") is None


@pytest.mark.asyncio
async def test_q24_limit():
    results, peak = await q24_run_limited(50, 5)
    assert len(results) == 50
    assert peak <= 5


# ---------------------------------------------------------------------------
# 实战档
# ---------------------------------------------------------------------------
def test_q25_parse_logs():
    lines = [
        "2024-05-20 14:30:00 ERROR [order] 超时 30s",
        "2024-05-20 14:30:01 INFO [gateway] 请求进入",
        "不是日志行",
    ]
    rows, levels = q25_parse_logs(lines)
    assert len(rows) == 2
    assert rows[0]["level"] == "ERROR"
    assert rows[0]["module"] == "order"
    assert rows[0]["msg"] == "超时 30s"
    assert levels == {"ERROR": 1, "INFO": 1}


def test_q26_aggregate():
    assert q26_aggregate_csv("dept,name,amount\na,x,10\na,y,5\nb,z,20\n") == {"a": 15.0, "b": 20.0}


@pytest.mark.asyncio
async def test_q27_retry_and_fallback():
    calls = {"n": 0}

    async def flaky(url):
        calls["n"] += 1
        if calls["n"] < 2:
            raise asyncio.TimeoutError
        return {"ok": url}

    assert await q27_fetch_json(flaky, "u", retries=3) == {"ok": "u"}
    assert calls["n"] == 2

    async def always_fail(url):
        raise asyncio.TimeoutError

    assert await q27_fetch_json(always_fail, "u", retries=2, fallback="兜底") == "兜底"


def test_q28_count():
    assert q28_count_text("你好 世界") == {"chars": 5, "words": 2}
    assert q28_count_text("  a  ") == {"chars": 1, "words": 1}


@pytest.mark.asyncio
async def test_circuit_breaker():
    br = CircuitBreaker(threshold=3, cooldown=60)

    async def fail():
        raise RuntimeError("nope")

    for _ in range(3):
        assert await br.call(fail, fallback="FB") == "FB"
    assert br.state == "open"

    # 熔断后不再真的调用
    async def should_not_run():
        raise AssertionError("熔断期间不应发起调用")

    assert await br.call(should_not_run, fallback="FB") == "FB"


@pytest.mark.asyncio
async def test_circuit_breaker_recover():
    br = CircuitBreaker(threshold=2, cooldown=0.05)

    async def fail():
        raise RuntimeError

    async def ok():
        return "ok"

    await br.call(fail)
    await br.call(fail)
    assert br.state == "open"

    await asyncio.sleep(0.1)
    assert await br.call(ok) == "ok"
    assert br.state == "closed"
