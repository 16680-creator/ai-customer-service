"""01 · list：你的主力容器

运行: python 02_containers/01_list.py
"""

print("=" * 60)
print("1. 创建")
print("=" * 60)

print(f"[]                    -> {[]}")
print(f"[1, 2, 3]             -> {[1, 2, 3]}")
print(f"[0] * 5               -> {[0] * 5}")
print(f"list(range(5))        -> {list(range(5))}")
print(f"list('abc')           -> {list('abc')}")
print(f"[x*2 for x in range(3)] -> {[x * 2 for x in range(3)]}")

# 二维列表陷阱
bad = [[0] * 3] * 3
bad[0][0] = 9
print(f"\n[[0]*3]*3 改 [0][0] -> {bad}   <- 三行是同一个对象")

good = [[0] * 3 for _ in range(3)]
good[0][0] = 9
print(f"推导式版本           -> {good}   <- 正确")


print("\n" + "=" * 60)
print("2. 常用方法（对照 Java ArrayList）")
print("=" * 60)

fruits = ["apple", "banana"]
print(f"初始            {fruits}")

fruits.append("cherry")
print(f"append          {fruits}")

fruits.extend(["date", "elder"])
print(f"extend          {fruits}")

fruits.insert(0, "avocado")
print(f"insert(0, ..)   {fruits}")

last = fruits.pop()
print(f"pop()           {fruits}   返回 {last}")

first = fruits.pop(0)
print(f"pop(0)          {fruits}   返回 {first}")

fruits.remove("cherry")
print(f"remove('cherry'){fruits}")

print(f"index('banana') {fruits.index('banana')}")
print(f"'date' in       {'date' in fruits}")

nums = [3, 1, 2]
nums.sort()
print(f"\nsort() 原地     {nums}   返回 None")

nums = [3, 1, 2]
print(f"sorted() 新列表 {sorted(nums)}   原列表仍是 {nums}")

nums.reverse()
print(f"reverse()       {nums}")


print("=" * 60)
print("3. 切片（Java 没有的能力）")
print("=" * 60)

a = [0, 1, 2, 3, 4, 5]
for expr in ["a[1:4]", "a[:3]", "a[3:]", "a[-2:]", "a[:-2]",
             "a[::2]", "a[::-1]", "a[4:1:-1]"]:
    print(f"{expr:10} = {eval(expr)}")

# 切片赋值：长度可变
b = [0, 1, 2, 3, 4, 5]
b[1:3] = [10, 20, 30]
print(f"\nb[1:3] = [10,20,30] -> {b}")

b[1:4] = []
print(f"b[1:4] = []         -> {b}   <- 删除区间")


print("=" * 60)
print("4. 排序：key 函数（比 Comparator 简洁）")
print("=" * 60)

orders = [
    {"no": "A", "amount": 300, "dept": "x"},
    {"no": "B", "amount": 100, "dept": "y"},
    {"no": "C", "amount": 200, "dept": "x"},
]

print("按金额升序：")
print(" ", [o["no"] for o in sorted(orders, key=lambda o: o["amount"])])

print("按金额降序：")
print(" ", [o["no"] for o in sorted(orders, key=lambda o: -o["amount"])])

print("按 dept 升序 + amount 降序：")
print(" ", [o["no"] for o in sorted(orders, key=lambda o: (o["dept"], -o["amount"]))])

# itemgetter 比 lambda 稍快，且可读
from operator import itemgetter
print("itemgetter('amount')：")
print(" ", [o["no"] for o in sorted(orders, key=itemgetter("amount"))])


print("=" * 60)
print("5. 复杂度：容易踩的性能坑")
print("=" * 60)

print("list.append      O(1) 均摊")
print("list.pop()       O(1)")
print("list.pop(0)      O(n)  <- 队列请用 deque")
print("x in list        O(n)  <- 频繁判断存在请用 set")
print("list[i]          O(1)")
print("list.remove(v)   O(n)")

from collections import deque

q = deque([1, 2, 3])
q.append(4)
q.appendleft(0)
print(f"\ndeque: {q}")
print(f"popleft() -> {q.popleft()}   O(1)")
print(f"pop()     -> {q.pop()}       O(1)")


print("=" * 60)
print("6. 去重保序（高频需求）")
print("=" * 60)

data = [3, 1, 3, 2, 1, 4]
print(f"原始              {data}")
print(f"dict.fromkeys     {list(dict.fromkeys(data))}    <- 推荐")
print(f"list(set(..))     {list(set(data))}   <- 顺序被打乱")

seen, out = set(), []
for x in data:
    if x not in seen:
        seen.add(x)
        out.append(x)
print(f"手写保序          {out}")
