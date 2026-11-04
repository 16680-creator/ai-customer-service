"""07 · 容器自测（对应文档 03 篇第十一节）

运行: python 02_containers/07_quiz.py
先自己预测输出，再运行对比。
"""


# ---- 本文件讲：用 5 个小实验 + 4 条断言，验证容器最常踩的坑 ----
# 每个小节都是「先给一段容易写错的代码，再给正确写法」的对照，
# 最后用 check() 汇总结果。建议先遮住输出、自己在纸上预测，再运行对照。
# 这些坑的共同根源只有一个：Python 变量是「贴在对象上的标签」，不是装值的盒子。


def check(label: str, got, want) -> bool:
    # 自测断言辅助函数：比较「实际得到的值」和「期望值」，打印 PASS/FAIL 并返回布尔值。
    # 之所以单独写一个函数，是因为下面要重复校验 4 次；这是最朴素的测试思想，等价 JUnit 的 assertEquals。
    ok = got == want   # == 对 list/dict 是逐元素比较（值相等），不是 Java 的引用相等；引用相等要用 is
    # f-string 里的 {'PASS' if ok else 'FAIL'} 是三元表达式，等价 Java 的 ok ? "PASS" : "FAIL"
    # {got!r} 里的 !r 表示用 repr() 格式化，字符串会带上引号，方便区分 '1' 和数字 1
    print(f"  [{'PASS' if ok else 'FAIL'}] {label}: got={got!r} want={want!r}")
    return ok        # 返回 True/False，最后用 sum(results) 把 True 当 1 累加，统计通过数


# ---- ① 讲：b = a 只是多一个引用；list 的 += 是「原地修改」 ----
# Java 里 List b = a 同样是引用赋值，但 Java 没有 += 这个运算符可用在 List 上；
# Python 的 list += 走 __iadd__，等价于 a.extend([4])，所以别名 a 会跟着变。
print("=" * 60)
print("① a = [1,2,3]; b = a; b += [4]")
print("=" * 60)

a = [1, 2, 3]      # 创建列表；等价 Java 的 new ArrayList<>(List.of(1, 2, 3))
b = a              # 注意：这不是拷贝！a、b 两个标签贴在同一个 list 对象上
b += [4]           # += 对 list 调用 __iadd__ → 原地 extend，不产生新对象
print(f"  a = {a}")   # 所以 a 也跟着变成了 [1, 2, 3, 4]
print(f"  b = {b}")   # b 自然是 [1, 2, 3, 4]
print("  说明：list 的 += 调用 __iadd__，原地 extend，a 和 b 是同一个对象")

# ---- ② 讲：b = b + [4] 走的是 __add__，创建一个全新的列表再重新绑定 b ----
# 与 ① 只差一个 `+`，结果却完全不同 —— 这是「原地修改 vs 新建对象」的分水岭。
print("\n" + "=" * 60)
print("② a = [1,2,3]; b = a; b = b + [4]")
print("=" * 60)

a = [1, 2, 3]      # 同样创建列表
b = a              # b 与 a 仍指向同一个对象
b = b + [4]        # + 调用 __add__：新建列表 [1,2,3,4]，b 改指向这个新对象；a 不受影响
print(f"  a = {a}")   # a 仍是 [1, 2, 3]
print(f"  b = {b}")   # b 变成了 [1, 2, 3, 4]
print("  说明：+ 调用 __add__，创建新列表，b 指向了新对象，a 不变")


# ---- ③ 讲：[[0]*5]*5 是「5 个引用指向同一个内层列表」的经典陷阱 ----
# * 复制的是引用而不是值：外层虽然新建了 5 个槽位，但每个槽位都指向同一个内层 list。
# 正确写法必须让每一行「各自独立创建」，用列表推导式每轮生成一个新的 [0]*5。
# Java 对照：new int[5][5] 是真正的二维数组，天然没有这个问题，反而容易掉以轻心。
print("\n" + "=" * 60)
print("③ 正确生成 5x5 零矩阵")
print("=" * 60)

wrong = [[0] * 5] * 5        # 先造一个内层 [0,0,0,0,0]，再把它复制 5 份引用 → 5 行其实是同一个对象
wrong[0][0] = 9              # 只改第 0 行第 0 列，但 5 行共享同一内层列表，实际全被改
print(f"  [[0]*5]*5  -> 第 0 列 = {[row[0] for row in wrong]}   错误：全被改")   # 推导式取出每行首元素，会看到 5 个 9

right = [[0] * 5 for _ in range(5)]   # 推导式每轮都执行一次 [0]*5，得到 5 个互相独立的内层列表
right[0][0] = 9                       # 现在只影响第 0 行
print(f"  推导式      -> 第 0 列 = {[row[0] for row in right]}   正确")       # 只有第一个是 9，其余是 0


# ---- ④ 讲：按字段分组求和，两种写法对比 ----
# 版本一用 dict.get(k, 0) 做「取不到就给默认值」，这是 Java computeIfAbsent/merge 的 Python 等价思路。
# 版本二用 collections.defaultdict(int)，让缺失的键自动初始化为 0，代码更短。
# Java 对照：Map<String,Integer> acc = new HashMap<>(); acc.merge(k, v, Integer::sum);
print("\n" + "=" * 60)
print("④ 一趟循环分组求和")
print("=" * 60)


def group_sum(records: list[dict]) -> dict[str, int]:
    # 入参是「字典组成的列表」，出参是「键 → 累加和」的字典。
    # 注解里的 list[dict] / dict[str, int] 只是提示信息，运行时不检查（见 04 篇「函数注解」）。
    out: dict[str, int] = {}          # 结果累加器；空字典，等价 Java 的 new LinkedHashMap<>()（Python 3.7+ 字典保插入序）
    for r in records:                 # 逐条记录遍历；等价 Java 的 for (Map<String,Object> r : records)
        # out.get(key, 0)：键存在就取当前值，不存在就返回 0，避免 KeyError（Java 的 getOrDefault）
        # 然后把本条记录的 v 加上去，写回 out[key]
        out[r["n"]] = out.get(r["n"], 0) + r["v"]
    return out                        # 返回统计结果（dict 直接返回，无需包装）


data = [{"n": "a", "v": 3}, {"n": "b", "v": 1}, {"n": "a", "v": 2}]   # 两条 a、一条 b，用来验证分组累加
print(f"  {group_sum(data)}")     # 期望 {'a': 5, 'b': 1}
print(f"  defaultdict 版：")


def group_sum2(records):
    from collections import defaultdict   # 局部导入：只在这个函数里用到，缩小作用域（也可放文件顶部）
    acc = defaultdict(int)                # 默认工厂是 int → 访问不存在的键时自动创建并初始化为 0
    for r in records:
        acc[r["n"]] += r["v"]             # 因为 defaultdict 自动补 0，这里可以直接 +=，不用 get
    return dict(acc)                      # 转回普通 dict，避免调用方拿到一个「有默认行为」的特殊字典


print(f"  {group_sum2(data)}")    # 结果与版本一完全一致


# ---- ⑤ 讲：zip 把两个列表「拉链式」配对，字典推导式顺手做过滤 ----
# zip(keys, vals) 产出 (k, v) 二元组，长度以较短的为准，多余元素直接丢弃。
# {k: v for k, v in zip(...) if v > 10} = 构建字典 + 过滤一次完成，Java 需要 Collectors.toMap + filter。
print("\n" + "=" * 60)
print("⑤ 两列表构造 dict 并过滤 value > 10")
print("=" * 60)

keys = ["a", "b", "c"]     # 键列表
vals = [5, 20, 30]         # 值列表，与 keys 按下标一一对应
result = {k: v for k, v in zip(keys, vals) if v > 10}   # 配对 → 只保留 value>10 的项 → 组成新字典
print(f"  {result}")       # 期望 {'b': 20, 'c': 30}，'a' 因为 5>10 不成立被过滤掉


# ---- 附加题讲：5 个「看起来反直觉，其实是同一套引用/哈希规则」的输出预测 ----
# 1) dict 保插入序  2) list * n 共享同一个元素  3) 元组不可变但里面的 list 可变
# 4) set 元素必须可哈希 → list 不能放进去  5) dict.fromkeys 的默认值被所有键共享
print("\n" + "=" * 60)
print("附加题：下面的输出是什么？")
print("=" * 60)

# 1) 字典遍历顺序
d = {"b": 1, "a": 2, "c": 3}   # Python 3.7+ 字典按插入顺序保存，不再是无序（Java HashMap 无序，LinkedHashMap 才保序）
print(f"  1) dict 保持插入序 -> {list(d)}")   # list(d) 取的是键，顺序就是插入顺序 ['b','a','c']

# 2) list * 3 的可变性
inner = [0]                    # 内层列表，只有一个元素
outer = inner * 3              # * 复制的是「引用」→ outer 是 [inner, inner, inner]，三个槽位指向同一个列表
outer[0] = 9                   # 改第 0 个槽位 → 改的就是 inner 本身 → 三个位置看到的值一起变
print(f"  2) [0]*3 后改首位 -> {outer}   （同层是同一个对象）")   # 输出 [9, 9, 9]

# 3) 元组包含 list
t = (1, [2, 3])                # 元组本身不可变（不能 t[0]=...），但它保存的是引用
t[1].append(4)                 # 合法！没有修改元组结构，只是调用了其中一个 list 对象的方法
print(f"  3) 元组里的 list 可改 -> {t}")   # 输出 (1, [2, 3, 4])

# 4) set 里放不可哈希对象
try:
    {[1, 2]}                   # 集合字面量；set 需要把元素算哈希值，而 list 是「可变类型」→ 不可哈希
except TypeError as e:         # 捕获后打印，避免脚本中断（等价 Java 里捕获异常后继续跑）
    print(f"  4) set 放 list -> TypeError: {e}")   # unhashable type: 'list'

# 5) dict.fromkeys 用可变对象做值
bad = dict.fromkeys(["a", "b"], [])   # 所有键共用同一个 value 对象 []（不是每个键各造一个空列表）
bad["a"].append(1)                    # 通过 'a' 改的其实是那个共享列表 → 'b' 也受影响
print(f"  5) fromkeys 共享值 -> {bad}   <- 所有键指向同一个 list")   # {'a': [1], 'b': [1]}

good = {k: [] for k in ["a", "b"]}    # 推导式每轮执行 [] → 每个键拿到各自独立的新列表
good["a"].append(1)                   # 这次只影响 'a'
print(f"     推导式版本      -> {good}")   # {'a': [1], 'b': []}


# ---- 验证讲：把前面几段的正确结果固化成 4 条断言，改代码后跑一遍就知道有没有破坏 ----
# results 里存的是 4 个 True/False；sum() 对布尔值求和等价于数 True 的个数（True=1, False=0）。
print("\n" + "=" * 60)
print("验证")
print("=" * 60)
results = [
    check("④ 分组求和", group_sum(data), {"a": 5, "b": 1}),          # 期望 a 累加 3+2=5，b 为 1
    check("⑤ 过滤构造", result, {"b": 20, "c": 30}),                 # 期望只剩 value>10 的两项
    check("1) 插入序", list(d), ["b", "a", "c"]),                    # 期望保留写入顺序
    check("5) fromkeys 共享", bad["a"] == bad["b"], True),            # 期望两个键是同一个列表对象
]
print(f"{sum(results)}/{len(results)} 通过")   # 例如 "4/4 通过"；len 取总条数，sum 取通过条数
