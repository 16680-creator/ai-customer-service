"""pytest 形式的题库测试（对应文档 09 篇的测试写法）

需要 pytest:
    python -m pip install pytest pytest-asyncio
    python -m pytest 06_exercises/test_answers.py -v

没有装 pytest 时，用 `python 06_exercises/answers.py` 也能验证全部答案。
"""

# ---- 本段讲：pytest 测试文件的导入区 ----
# 这个文件是「题库答案」的自动化测试：把 answers.py 里的每个答案函数 import 进来，
# 用 pytest 的 assert 验证它在一个个具体用例下的返回值。
# Java 对照：相当于 src/test/java 下用 JUnit 5 写的 xxxTest 类；
# 区别是 Python 不需要类、也不需要继承 TestCase，函数名以 test_ 开头就会被自动收集。
import asyncio                    # 异步用例里要用 asyncio.sleep / asyncio.TimeoutError

import pytest                    # 第三方测试框架；Java 对应 JUnit 5 的 org.junit.jupiter

# 从同目录的 answers.py 中导入被测对象。
# 括号换行是 Python 的「隐式续行」：只要还在括号里，换行就不算语句结束；Java 的 import 只能一条一条写。
from answers import (
    CircuitBreaker,        # 熔断器类：连续失败达到阈值后 open，冷却后允许试探恢复
    LRUCache,              # 手写 LRU 缓存：dict 记住插入顺序 + 命中时 move_to_end
    MemoryRepo,            # 内存仓储，演示最简 DAO 的 save / find_by_id
    Money,                 # 金额值对象：重载 +、==、< 并实现 __hash__，演示值语义
    # 下面这些是 qN_xxx 形式的题目答案函数，命名规则 = 题号 + 做的事
    q1_even_squares,       # 第 1 题：取偶数并平方
    q2_char_count,         # 第 2 题：统计每个字符出现次数
    q3_zip_to_dict,        # 第 3 题：两个序列按位置合并成字典
    q4_max_of,             # 第 4 题：可变参数求最大值（空参返回 None）
    q5_get_nested,         # 第 5 题：按 "a.b.c" 路径取嵌套字典的值
    q6_fizzbuzz,           # 第 6 题：FizzBuzz 序列
    q7_reverse_words,      # 第 7 题：反转单词顺序并压缩多余空格
    q8_is_palindrome,      # 第 8 题：忽略非字母数字、忽略大小写的回文判断
    q9_merge_dicts,        # 第 9 题：合并两个字典（后者覆盖前者）
    q10_dedup_keep_order,  # 第 10 题：去重且保持首次出现顺序
    q11_format_money,      # 第 11 题：金额格式化，千分位 + 两位小数
    q13_sort_orders,       # 第 13 题：订单按金额降序、同额按单号升序
    q14_group_stats,       # 第 14 题：按分类分组统计 count 与 total
    q16_batch,             # 第 16 题：把可迭代对象切成固定大小的批（生成器）
    q19_safe_int,          # 第 19 题：宽松转 int（去逗号、失败返回默认值）
    q20_chunk,             # 第 20 题：按长度把字符串重新分行，不打乱单词
    q22_merge,             # 第 22 题：嵌套字典合并（deep 参数控制深浅）
    q24_run_limited,       # 第 24 题：用 Semaphore 限制并发数的批量任务
    q25_parse_logs,        # 第 25 题：解析日志行，返回结构化行 + 级别统计
    q26_aggregate_csv,     # 第 26 题：按部门汇总 CSV 里的金额
    q27_fetch_json,        # 第 27 题：带重试与兜底值的异步抓取
    q28_count_text,        # 第 28 题：统计字符数与单词数
)


# ---------------------------------------------------------------------------
# 入门档：参数化写法（对照 JUnit @ParameterizedTest）
# ---------------------------------------------------------------------------
# ---- 本段讲：参数化测试（一个函数跑多组用例）----
# JUnit5 用 @ParameterizedTest + @CsvSource 给同一段断言喂多组数据；
# pytest 用 @pytest.mark.parametrize("参数名列表", [一组组元组])，
# 每组数据都会独立跑一次 test_q1，失败报告里能看到具体是哪一组挂了。
@pytest.mark.parametrize("nums,expected", [   # 声明两个参数名，顺序必须和下面每个元组一致
    ([1, 2, 3, 4, 5, 6], [4, 16, 36]),        # 偶数 2/4/6 -> 平方 4/16/36
    ([], []),                                  # 空输入边界：输出也应为空列表
    ([1, 3, 5], []),                           # 全是奇数 -> 一个偶数都没有
    ([-2, -3], [4]),                           # 负数除 2 余 0 也算偶数：-2 的平方是 4
])
def test_q1(nums, expected):                   # 函数名以 test_ 开头，pytest 才会自动发现
    assert q1_even_squares(nums) == expected   # assert 为假即用例失败；Java 里是 assertEquals


def test_q2_char_count():
    # 逐字符计数，断言完整字典；字典比较与顺序无关，只看键值对集合。
    assert q2_char_count("hello") == {"h": 1, "e": 1, "l": 2, "o": 1}   # 'l' 出现两次
    assert q2_char_count("") == {}                                      # 空串应得到空字典，而不是 None


@pytest.mark.parametrize("keys,vals,expected", [
    (["a", "b"], [1, 2], {"a": 1, "b": 2}),      # 长度相同：一一配对
    (["a", "b"], [1], {"a": 1}),                 # zip 截断到最短
    ([], [], {}),                                 # 两边都空：结果为空字典
])
def test_q3(keys, vals, expected):
    assert q3_zip_to_dict(keys, vals) == expected


# ---- 本段讲：普通断言型用例（q4~q8）----
# 没有参数化时，直接在一个 test_ 函数里连续写多条 assert，
# 任意一条失败该用例即失败；Java 里是一个 @Test 方法里多次 assertEquals。
def test_q4_max_of():
    assert q4_max_of(3, 1, 4, 1, 5) == 5      # 可变参数：正常取到最大 5
    assert q4_max_of() is None                # 空参边界：返回 None；必须用 is 判断 None
    assert q4_max_of(-1, -5) == -1            # 全负数：最大值是 -1，不能拿 0 当初值


def test_q5_get_nested():
    d = {"a": {"b": {"c": 42}}}               # 三层嵌套字典，模拟 JSON 结构
    assert q5_get_nested(d, "a.b.c") == 42             # 路径存在 -> 取到最深层的 42
    assert q5_get_nested(d, "a.x.c", 0) == 0           # 中间键不存在 -> 返回默认值 0
    assert q5_get_nested(d, "a.b.c.d", None) is None   # 把 42 当字典再取 -> 兜底 None
    assert q5_get_nested({}, "x", "dft") == "dft"      # 空字典 -> 直接用默认值


def test_q6_fizzbuzz():
    out = q6_fizzbuzz(15)          # 1..15 的 FizzBuzz 结果列表
    assert len(out) == 15          # 数量必须等于 n，不能多也不能少
    assert out[2] == "Fizz"        # 下标 2 对应数字 3：3 的倍数
    assert out[4] == "Buzz"        # 下标 4 对应数字 5：5 的倍数
    assert out[14] == "FizzBuzz"   # 下标 14 对应数字 15：同时是 3 和 5 的倍数
    assert out[0] == "1"           # 下标 0 对应数字 1：既不是 3 也不是 5，转成字符串 "1"


def test_q7_reverse_words():
    assert q7_reverse_words("the sky is blue") == "blue is sky the"   # 单词顺序反转
    assert q7_reverse_words("  a   b  ") == "b a"                     # 前后与中间多余空格要被压缩
    assert q7_reverse_words("") == ""                                 # 空串返回空串，不能抛异常


# ---- 本段讲：回文判断的参数化用例 ----
# 每个元组 = (输入字符串, 期望的布尔值)，重点覆盖标点、大小写、空串等边界。
@pytest.mark.parametrize("s,expected", [
    ("A man, a plan, a canal: Panama", True),   # 忽略空格/逗号/冒号和大小写后是回文
    ("hello", False),                            # 普通非回文
    ("", True),                                  # 空串视为回文（边界约定）
    ("a", True),                                 # 单字符必然是回文
])
def test_q8_palindrome(s, expected):
    assert q8_is_palindrome(s) is expected   # 布尔值用 is 比较：True/False 是单例对象


# ---- 本段讲：字典合并、去重保序、金额格式化（q9~q11）----
def test_q9_merge():
    # 键 "y" 冲突：后者覆盖前者 -> y=20，其余键保留
    assert q9_merge_dicts({"x": 1, "y": 2}, {"y": 20, "z": 30}) == {"x": 1, "y": 20, "z": 30}
    # 空字典与有值字典合并：结果就是有值那个（验证不会原地改坏入参）
    assert q9_merge_dicts({}, {"a": 1}) == {"a": 1}


def test_q10_dedup():
    # 重复元素只保留第一次出现的位置顺序：3,1,3,2,1,4 -> 3,1,2,4
    assert q10_dedup_keep_order([3, 1, 3, 2, 1, 4]) == [3, 1, 2, 4]
    assert q10_dedup_keep_order([]) == []   # 空列表边界


def test_q11_format_money():
    assert q11_format_money(1234567.891) == "1,234,567.89"   # 千分位逗号 + 四舍五入到两位
    assert q11_format_money(0) == "0.00"                     # 整数 0 也要补两位小数
    assert q11_format_money("12.5") == "12.50"               # 传字符串也要能处理，并补齐小数位


# ---------------------------------------------------------------------------
# 进阶档
# ---------------------------------------------------------------------------
# ---- 本段讲：排序、分组统计（q13~q14）----
def test_q13_sort():
    orders = [
        {"order_no": "B", "amount": 100},   # 与 A 金额相同，用来验证第二排序键
        {"order_no": "A", "amount": 100},
        {"order_no": "C", "amount": 300},   # 金额最大，应排第一
    ]
    # 期望顺序：金额降序（C=300 最前），金额相同按单号升序（A 在 B 前）
    assert [o["order_no"] for o in q13_sort_orders(orders)] == ["C", "A", "B"]


def test_q14_group_stats():
    got = q14_group_stats([
        {"cat": "A", "amt": 10},   # A 类第 1 笔
        {"cat": "B", "amt": 20},   # B 类第 1 笔
        {"cat": "A", "amt": 5},    # A 类第 2 笔
    ])
    # A：2 笔、合计 15；B：1 笔、合计 20
    assert got == {"A": {"count": 2, "total": 15}, "B": {"count": 1, "total": 20}}


# ---- 本段讲：分批、值对象、容错转换、按长度换行、仓储（q16~q20）----
def test_q16_batch():
    # range(1,6) 产生 1..5，每 2 个一批 -> [1,2] [3,4] [5]（最后一批允许不足）
    assert list(q16_batch(range(1, 6), 2)) == [[1, 2], [3, 4], [5]]
    # 刚好整除：不会多出一个空批次
    assert list(q16_batch(range(1, 5), 2)) == [[1, 2], [3, 4]]
    assert list(q16_batch([], 2)) == []   # 空输入 -> 生成器不产出任何批次


def test_money():
    # __add__：同币种相加，结果按 2 位小数格式化输出
    assert str(Money(10) + Money(5.5)) == "15.50 CNY"
    assert Money(1) == Money(1)        # __eq__：值相等即相等，而不是比较对象地址
    assert Money(1) < Money(2)         # __lt__：支持小于比较（functools.total_ordering 补齐其他比较）
    assert len({Money(1), Money(1)}) == 1   # __hash__：值相等的对象进 set 只会留一个
    with pytest.raises(ValueError):
        Money(1) + Money(1, "USD")     # 币种不同必须抛异常；with pytest.raises 断言「确实抛了」


def test_q19_safe_int():
    assert q19_safe_int("1,234") == 1234     # 先去掉千分位逗号再转
    assert q19_safe_int("  42  ") == 42      # 前后空格要 strip 掉
    assert q19_safe_int("abc") == 0          # 无法解析 -> 用默认值 0，而不是抛 ValueError
    assert q19_safe_int(None, -1) == -1      # None 且显式传默认值 -> -1
    assert q19_safe_int(None) == 0           # None 且不传默认值 -> 内置默认 0


def test_q20_chunk():
    # 每行最多 12 字符，在空格处断行且不切断单词
    assert q20_chunk("hello world foo bar", 12) == ["hello world", "foo bar"]
    assert q20_chunk("a b c", 1) == ["a", "b", "c"]   # 每行只能放 1 个字符时逐个单词成行
    assert q20_chunk("", 5) == []                     # 空串 -> 空列表，不能返回 [""]


def test_repo():
    # 内存仓储：save 后能按 id 取回同一条记录
    r = MemoryRepo()
    r.save({"id": 1, "name": "x"})
    assert r.find_by_id(1) == {"id": 1, "name": "x"}
    assert r.find_by_id(99) is None   # 查不到返回 None（Python 惯例），不抛异常


# ---- 本段讲：嵌套合并、LRU 缓存、并发限流（q22~q24）----
def test_q22_merge():
    # deep=True（默认）：递归合并内层字典，外层 "db" 里 a 被覆盖、b 保留
    assert q22_merge({"db": {"a": 1, "b": 2}}, {"db": {"a": 9}}) == {"db": {"a": 9, "b": 2}}
    # deep=False：整块替换，第二个 "db" 直接覆盖掉第一个，b 消失
    assert q22_merge({"db": {"a": 1, "b": 2}}, {"db": {"a": 9}}, deep=False) == {"db": {"a": 9}}


def test_lru():
    # 容量 2 的 LRU：put a、put b 后缓存满
    c = LRUCache(2)
    c.put("a", 1)
    c.put("b", 2)
    assert c.get("a") == 1        # 命中 a，同时 a 被移到「最近使用」队尾
    c.put("c", 3)                 # 再放 c 时，最久未使用的 b 被淘汰
    assert list(c.data) == ["a", "c"]   # list(dict) 得到键顺序：仍是 a 在前、c 在后
    assert c.get("b") is None           # b 已被淘汰 -> 返回 None


# ---- 本段讲：异步限流用例（pytest-asyncio）----
# @pytest.mark.asyncio 让 pytest 知道要跑协程函数；
# 没有它，async def 只会被创建成协程对象而不会真正执行（还会报警告）。
# Java 对照：类似用 CompletableFuture / 虚拟线程跑并发测试。
@pytest.mark.asyncio
async def test_q24_limit():
    results, peak = await q24_run_limited(50, 5)   # 50 个任务、最多同时 5 个
    assert len(results) == 50                       # 所有任务都必须完成，一个不能漏
    assert peak <= 5                                # 实测峰值并发不能超过 Semaphore 许可数


# ---------------------------------------------------------------------------
# 实战档
# ---------------------------------------------------------------------------
# ---- 本段讲：日志解析与 CSV 汇总（q25~q26）----
def test_q25_parse_logs():
    lines = [
        "2024-05-20 14:30:00 ERROR [order] 超时 30s",   # 合法行：ERROR 级别
        "2024-05-20 14:30:01 INFO [gateway] 请求进入",  # 合法行：INFO 级别
        "不是日志行",                                     # 脏数据：应被跳过而非报错
    ]
    rows, levels = q25_parse_logs(lines)    # 返回结构化行列表 + 各级别计数
    assert len(rows) == 2                   # 脏数据被过滤，只留 2 条
    assert rows[0]["level"] == "ERROR"      # 第 1 条的结构化字段：级别
    assert rows[0]["module"] == "order"     # 方括号里的模块名
    assert rows[0]["msg"] == "超时 30s"     # 方括号之后的消息正文
    assert levels == {"ERROR": 1, "INFO": 1}   # Counter 计数结果


def test_q26_aggregate():
    # 传入 CSV 文本（首行表头）；a 部门两行 10+5=15，b 部门 20
    assert q26_aggregate_csv("dept,name,amount\na,x,10\na,y,5\nb,z,20\n") == {"a": 15.0, "b": 20.0}


# ---- 本段讲：异步重试与兜底（q27）----
# 用「假 fetcher」替代真实网络：既能精确控制失败次数，又不会让测试变慢/变不稳定。
# 这是单元测试的核心思想——把外部依赖替换成可控的桩（Java 里常用 Mockito）。
@pytest.mark.asyncio
async def test_q27_retry_and_fallback():
    calls = {"n": 0}   # 用可变字典记录调用次数；闭包内修改它不需要 nonlocal

    async def flaky(url):
        calls["n"] += 1                 # 每调用一次计数 +1
        if calls["n"] < 2:
            raise asyncio.TimeoutError  # 第 1 次失败，触发重试
        return {"ok": url}              # 第 2 次成功

    # 重试策略应吞掉一次超时并在第 2 次拿到结果
    assert await q27_fetch_json(flaky, "u", retries=3) == {"ok": "u"}
    assert calls["n"] == 2              # 验证「重试了一次」，而不是一次就成功或无限重试

    async def always_fail(url):
        raise asyncio.TimeoutError      # 永远失败，模拟服务彻底不可用

    # 重试耗尽后应返回 fallback，而不是把异常抛给调用方
    assert await q27_fetch_json(always_fail, "u", retries=2, fallback="兜底") == "兜底"


# ---- 本段讲：文本统计（q28）----
def test_q28_count():
    # 先 strip 再计数："你好 世界" 去掉两端空白后长度是 5（4 个汉字 + 1 个中间空格），按空格切出 2 个单词
    assert q28_count_text("你好 世界") == {"chars": 5, "words": 2}
    assert q28_count_text("  a  ") == {"chars": 1, "words": 1}   # 前后空格不计入


# ---- 本段讲：熔断器（CircuitBreaker）行为测试 ----
# 熔断器三态：closed（正常）-> open（快速失败）-> half-open（试探）-> closed。
# 这里验证的是「连续失败达到阈值后打开，并在冷却期内直接走 fallback」。
@pytest.mark.asyncio
async def test_circuit_breaker():
    br = CircuitBreaker(threshold=3, cooldown=60)   # 失败 3 次打开，冷却 60 秒

    async def fail():
        raise RuntimeError("nope")                  # 模拟下游一直报错

    for _ in range(3):
        # 每次失败都应被熔断器接住并返回 fallback，而不是把异常抛出去
        assert await br.call(fail, fallback="FB") == "FB"
    assert br.state == "open"                       # 达到阈值后状态必须变为 open

    # 熔断后不再真的调用
    async def should_not_run():
        raise AssertionError("熔断期间不应发起调用")   # 如果被调用，测试立刻失败

    # open 状态下即使传进来的是必然失败的函数，也应直接返回 fallback（证明没有执行它）
    assert await br.call(should_not_run, fallback="FB") == "FB"


@pytest.mark.asyncio
async def test_circuit_breaker_recover():
    # 冷却时间设得很短（0.05s），方便在测试里等待恢复
    br = CircuitBreaker(threshold=2, cooldown=0.05)

    async def fail():
        raise RuntimeError      # 只关心「失败」这个事实，不需要具体信息

    async def ok():
        return "ok"             # 恢复后下游返回正常值

    await br.call(fail)         # 第 1 次失败
    await br.call(fail)         # 第 2 次失败 -> 达到阈值
    assert br.state == "open"   # 此时应处于熔断状态

    await asyncio.sleep(0.1)    # 睡过冷却期，让熔断器允许一次试探
    assert await br.call(ok) == "ok"   # 试探成功 -> 放行真实调用
    assert br.state == "closed"        # 成功后应恢复为正常状态
