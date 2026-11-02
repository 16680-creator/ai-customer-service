"""05 · 可变性与拷贝语义（Java 程序员最容易翻车的地方）

运行: python 02_containers/05_copy.py
"""

import copy

print("=" * 60)
print("1. 变量名是标签，不是盒子")
print("=" * 60)

a = [1, 2, 3]
b = a
print(f"a = {a}, b = a")
b.append(4)
print(f"b.append(4) 后：a={a}  b={b}   <- 同一个对象")

# 用 id 看身份
print(f"\nid(a) == id(b)  -> {id(a) == id(b)}")


print("\n" + "=" * 60)
print("2. 浅拷贝的三种写法")
print("=" * 60)

a = [1, 2, 3]
c1 = a.copy()
c2 = a[:]
c3 = list(a)

for name, c in [("a.copy()", c1), ("a[:]", c2), ("list(a)", c3)]:
    c.append(99)
    print(f"{name:10} 追加后 {c}   原 a={a}")

print(f"\nid 不同 -> {id(a) != id(c1)}")


print("=" * 60)
print("3. 浅拷贝的边界：内层还是共享的")
print("=" * 60)

nested = [[1, 2], [3, 4]]
shallow = nested.copy()

print(f"nested  = {nested}")
print(f"shallow = {shallow}")

shallow[0][0] = 99
print(f"\nshallow[0][0] = 99 后：")
print(f"nested  = {nested}     <- 被改了！")
print(f"shallow = {shallow}")

print(f"\n外层 id 不同: {id(nested) != id(shallow)}")
print(f"内层 id 相同: {id(nested[0]) == id(shallow[0])}   <- 原因在这里")

# deepcopy 才是完全独立
nested = [[1, 2], [3, 4]]
deep = copy.deepcopy(nested)
deep[0][0] = 99
print(f"\ndeepcopy 后改内层：nested = {nested}   不受影响")


print("=" * 60)
print("4. += 是原地修改，+ 是新建（list 特有）")
print("=" * 60)

a = [1, 2]
b = a
b += [3]
print(f"b += [3] 后：a={a} b={b}")

a = [1, 2]
b = a
b = b + [3]
print(f"b = b + [3] 后：a={a} b={b}")

print("\n字符串/数字没有这个差异（不可变）：")
s = "a"
t = s
t += "b"
print(f"t += 'b' 后：s={s!r} t={t!r}")

# 元组包含可变对象时，+= 会报错或行为反直觉
try:
    tpl = ([1], 2)
    tpl[0] += [9]
except TypeError as e:
    print(f"\n元组里的 list 做 += -> TypeError: {e}   <- 改成功了但整体赋值失败")


print("=" * 60)
print("5. 函数参数：传的是对象引用")
print("=" * 60)

def mutate(lst):
    lst.append(99)

data = [1]
mutate(data)
print(f"mutate(data) 后 data = {data}   <- 被改了")

def reassign(lst):
    lst = [0, 0]      # 只改了局部名字，外部无感

data2 = [1]
reassign(data2)
print(f"reassign(data2) 后 data2 = {data2}   <- 没变")

def safe_mutate(lst):
    lst = list(lst)   # 先复制
    lst.append(99)
    return lst

data3 = [1]
print(f"safe_mutate 返回 {safe_mutate(data3)}，原 data3 = {data3}")


print("=" * 60)
print("6. 可变默认参数（最经典的坑）")
print("=" * 60)

def bad(item, lst=[]):
    lst.append(item)
    return lst

print(f"bad(1) = {bad(1)}")
print(f"bad(2) = {bad(2)}   <- 累积了！")
print(f"bad.__defaults__ = {bad.__defaults__}")

def good(item, lst=None):
    if lst is None:
        lst = []
    lst.append(item)
    return lst

print(f"\ngood(1) = {good(1)}")
print(f"good(2) = {good(2)}")


print("=" * 60)
print("7. 不可变 vs 可变：哪些能当 dict 键")
print("=" * 60)

candidates = [
    ("1", 1),
    ("'a'", "a"),
    ("(1, 2)", (1, 2)),
    ("[1, 2]", [1, 2]),
    ("{1, 2}", {1, 2}),
    ("{'a': 1}", {"a": 1}),
]
for label, v in candidates:
    try:
        hash(v)
        print(f"{label:12} -> 可哈希")
    except TypeError:
        print(f"{label:12} -> 不可哈希（不能当 dict 键）")


print("=" * 60)
print("8. 实战：配置合并时避免串改")
print("=" * 60)

default_cfg = {"db": {"host": "localhost", "port": 5432}, "debug": False}
user_cfg = {"db": {"host": "10.0.0.1"}}

# 错误：浅合并，db 仍共享
wrong = {**default_cfg, **user_cfg}
wrong["db"]["port"] = 9999
print(f"浅合并后改 db.port：default_cfg = {default_cfg}   <- 被污染")

# 正确：深拷贝后再合并
default_cfg = {"db": {"host": "localhost", "port": 5432}, "debug": False}
merged = copy.deepcopy(default_cfg)
merged["db"].update(user_cfg["db"])
print(f"深拷贝合并后：default_cfg = {default_cfg}   <- 干净")
print(f"                merged     = {merged}")
