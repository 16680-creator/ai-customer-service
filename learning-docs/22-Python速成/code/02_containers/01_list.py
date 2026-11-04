"""01 · list：你的主力容器

运行: python 02_containers/01_list.py
"""

# ============================================================
# 本文件演示 list（对应 Java 的 ArrayList）的创建、增删改查、切片与排序。
# 阅读方式：每个小节先看"这段在演示什么"，再看每行行尾注释里的 Java 对照。
# 重点不是记住方法名，而是记住两个语义差异：
#   1) 切片/拷贝返回新列表，赋值 `b = a` 只是多一个引用；
#   2) 头部插入/删除是 O(n)，需要队列时要用 deque。
# ============================================================

print("=" * 60)          # 打印 60 个等号做分隔线：字符串 * int 表示重复，Java 要写 "=".repeat(60)
print("1. 创建")          # 小节标题；print 是内置函数，等价 System.out.println
print("=" * 60)

print(f"[]                    -> {[]}")          # f-string 里 {} 是插值占位；这里直接嵌一个字面量空列表
print(f"[1, 2, 3]             -> {[1, 2, 3]}")  # 字面量创建：元素类型可混，Java 要 new ArrayList<>(List.of(...))
print(f"[0] * 5               -> {[0] * 5}")    # 乘法复制：生成 5 个 0，等价 Java 的 Arrays.fill + new int[5]（但这里仍是 list）
print(f"list(range(5))        -> {list(range(5))}")   # range 是惰性序列，list() 才物化成 [0,1,2,3,4]
print(f"list('abc')           -> {list('abc')}")      # 字符串可迭代，按字符拆开；Java 要 s.toCharArray() 再装箱
print(f"[x*2 for x in range(3)] -> {[x * 2 for x in range(3)]}")   # 列表推导式：等价 map + collect

# 二维列表陷阱
bad = [[0] * 3] * 3      # 外层乘法只复制"引用"：三行指向同一个内层列表（Java 里 new int[3][3] 不会这样）
bad[0][0] = 9            # 改第一行第一列 → 实际改的是那个被共享的内层列表
print(f"\n[[0]*3]*3 改 [0][0] -> {bad}   <- 三行是同一个对象")   # \n 是换行符；输出可见三行一起变

good = [[0] * 3 for _ in range(3)]   # 推导式：每轮重新执行 [0]*3，产生三个独立的内层列表
good[0][0] = 9                       # 只影响第一行
print(f"推导式版本           -> {good}   <- 正确")   # 其余两行保持 0


print("\n" + "=" * 60)
print("2. 常用方法（对照 Java ArrayList）")
print("=" * 60)

# ------------------------------------------------------------
# 本段讲：增删改查 API，逐个对应 Java ArrayList 的方法。
# 注意 extend 接收"可迭代对象"，会把元素逐个加入；而 append 是把参数当成一个元素。
# 还要注意 pop(0)/insert(0, ..) 都是 O(n)，方法能跑通不代表适合高频调用。
# ------------------------------------------------------------
fruits = ["apple", "banana"]        # 初始化：Java 写 new ArrayList<>(List.of("apple", "banana"))
print(f"初始            {fruits}")

fruits.append("cherry")             # 尾部追加，等价 add(e)；均摊 O(1)，内部预留容量后很少搬移
print(f"append          {fruits}")

fruits.extend(["date", "elder"])    # 批量追加：遍历参数逐个 append，等价 addAll(c)；传字符串会被拆成字符！
print(f"extend          {fruits}")

fruits.insert(0, "avocado")         # 在下标 0 插入，原有元素整体后移，O(n)，等价 add(0, e)
print(f"insert(0, ..)   {fruits}")

last = fruits.pop()                 # 删除并返回末尾元素，O(1)，等价 remove(size()-1) 但要自己先取值
print(f"pop()           {fruits}   返回 {last}")

first = fruits.pop(0)               # 删除并返回下标 0 的元素，O(n)；高频头部弹出请改用 deque
print(f"pop(0)          {fruits}   返回 {first}")

fruits.remove("cherry")             # 按值删除第一个相等元素，O(n)；找不到抛 ValueError（Java 返回 boolean）
print(f"remove('cherry'){fruits}")

print(f"index('banana') {fruits.index('banana')}")   # 返回值对应的下标，等价 indexOf，找不到抛异常
print(f"'date' in       {'date' in fruits}")        # 成员判断，O(n) 线性扫描；等价 contains

nums = [3, 1, 2]                    # 准备一个乱序列表
nums.sort()                         # 原地排序，直接改 nums 自身
print(f"\nsort() 原地     {nums}   返回 None")   # 注意 sort() 返回 None，不能写 nums = nums.sort()

nums = [3, 1, 2]                    # 重新初始化，验证非原地版本
print(f"sorted() 新列表 {sorted(nums)}   原列表仍是 {nums}")   # sorted 返回新 list，原列表保持原顺序

nums.reverse()                      # 原地反转（此时 nums 还是 [3,1,2]），等价 Collections.reverse
print(f"reverse()       {nums}")


print("=" * 60)
print("3. 切片（Java 没有的能力）")
print("=" * 60)

# ------------------------------------------------------------
# 本段讲：切片 [start:stop:step]，左闭右开、负下标、可反向。
# 用 eval(expr) 是为了在循环里把表达式字符串直接求值，少写 8 行重复 print。
# 注意 eval 只适合这种教学脚本，生产代码不要对用户输入用 eval。
# ------------------------------------------------------------
a = [0, 1, 2, 3, 4, 5]      # 待切片的源列表，下标 0~5
for expr in ["a[1:4]", "a[:3]", "a[3:]", "a[-2:]", "a[:-2]",
             "a[::2]", "a[::-1]", "a[4:1:-1]"]:     # 把切片表达式写成字符串，逐个求值
    print(f"{expr:10} = {eval(expr)}")               # {expr:10} 左对齐占 10 格；eval 在当前作用域里执行表达式

# 切片赋值：长度可变
b = [0, 1, 2, 3, 4, 5]      # 另一份数据，避免影响 a
b[1:3] = [10, 20, 30]       # 把下标 1、2 两个位置替换成三个值 → 列表变长（Java 无此语法）
print(f"\nb[1:3] = [10,20,30] -> {b}")

b[1:4] = []                 # 用一个空列表替换下标 1~3 → 等价删除这一段
print(f"b[1:4] = []         -> {b}   <- 删除区间")


# ------------------------------------------------------------
# 本段讲：用 key 函数排序，等价 Java 的 Comparator，但更短。
# key 接收一个元素，返回"用于比较的键"；返回元组即可表达多字段排序。
# itemgetter 是 C 实现的取键函数，比 lambda 稍快，也更适合复用。
# ------------------------------------------------------------
print("=" * 60)
print("4. 排序：key 函数（比 Comparator 简洁）")
print("=" * 60)

orders = [                           # 用 dict 模拟订单对象（Java 里通常是一个 record/POJO）
    {"no": "A", "amount": 300, "dept": "x"},
    {"no": "B", "amount": 100, "dept": "y"},
    {"no": "C", "amount": 200, "dept": "x"},
]

print("按金额升序：")
print(" ", [o["no"] for o in sorted(orders, key=lambda o: o["amount"])])   # 推导式只取单号，便于观察顺序

print("按金额降序：")
print(" ", [o["no"] for o in sorted(orders, key=lambda o: -o["amount"])])  # 取负号实现降序，等价 Comparator.reversed()

print("按 dept 升序 + amount 降序：")
print(" ", [o["no"] for o in sorted(orders, key=lambda o: (o["dept"], -o["amount"]))])   # 元组按字典序逐项比较

# itemgetter 比 lambda 稍快，且可读
from operator import itemgetter      # 标准库：把一个或多个取下标操作变成可调用对象
print("itemgetter('amount')：")
print(" ", [o["no"] for o in sorted(orders, key=itemgetter("amount"))])   # 等价 key=lambda o: o["amount"]


# ------------------------------------------------------------
# 本段讲：list 各操作的复杂度，以及用 deque 替代"队列式 list"。
# 这些行只是打印结论，为了让你建立"哪个操作贵"的直觉。
# Java 对照：ArrayDeque 的 addFirst/pollFirst 就是这里的 appendleft/popleft，都是 O(1)。
# ------------------------------------------------------------
print("=" * 60)
print("5. 复杂度：容易踩的性能坑")
print("=" * 60)

print("list.append      O(1) 均摊")      # 尾部追加：容量不够时才整体扩容，摊还下来是常数
print("list.pop()       O(1)")           # 尾部弹出：不需要搬移其他元素
print("list.pop(0)      O(n)  <- 队列请用 deque")   # 头部弹出：后面所有元素前移一格
print("x in list        O(n)  <- 频繁判断存在请用 set")   # 线性扫描；set 是哈希定位 O(1)
print("list[i]          O(1)")           # 下标访问：底层是连续数组，直接按偏移取
print("list.remove(v)   O(n)")           # 按值删除：先线性查找，再搬移后面的元素

from collections import deque           # 双端队列，两端插入/删除都是 O(1)

q = deque([1, 2, 3])    # 用可迭代对象初始化；Java 对应 new ArrayDeque<>(List.of(1,2,3))
q.append(4)             # 右入队：等价 addLast
q.appendleft(0)         # 左入队：等价 addFirst（list 没有 O(1) 的对应操作）
print(f"\ndeque: {q}")
print(f"popleft() -> {q.popleft()}   O(1)")   # 左出队：等价 pollFirst
print(f"pop()     -> {q.pop()}       O(1)")   # 右出队：等价 pollLast


# ------------------------------------------------------------
# 本段讲：去重且保留首次出现顺序（业务高频需求）。
# dict.fromkeys 借"键唯一 + 保插入序"一行完成；手写 set 版本更直观也更容易加自定义逻辑。
# 直接用 list(set(...)) 会打乱顺序，只在不在意顺序时使用。
# ------------------------------------------------------------
print("=" * 60)
print("6. 去重保序（高频需求）")
print("=" * 60)

data = [3, 1, 3, 2, 1, 4]       # 含重复元素的原始数据
print(f"原始              {data}")
print(f"dict.fromkeys     {list(dict.fromkeys(data))}    <- 推荐")   # 建字典时重复键只保留第一次，再取键列表
print(f"list(set(..))     {list(set(data))}   <- 顺序被打乱")        # set 无序，顺序取决于哈希，不可依赖

seen, out = set(), []           # 同行赋两个值：seen 判重、out 保序输出（Java 要写两行）
for x in data:                  # 单趟遍历
    if x not in seen:           # set 的 in 是 O(1)，整体接近线性
        seen.add(x)             # 打标记
        out.append(x)           # 按首次出现顺序收集
print(f"手写保序          {out}")
