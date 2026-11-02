"""03 · 字符串与格式化

运行: python 01_basics/03_strings.py
"""

import textwrap

s = "Hello, Python"

print("=" * 60)
print("1. 基本操作（多数是返回新串，不改原串）")
print("=" * 60)

rows = [
    ("len(s)", len(s)),
    ("s.upper()", s.upper()),
    ("s.lower()", s.lower()),
    ("s.strip()", "  x  ".strip()),
    ("s.replace('l','L')", s.replace("l", "L")),
    ("s.split(', ')", s.split(", ")),
    ("'-'.join(['a','b'])", "-".join(["a", "b"])),
    ("'Python' in s", "Python" in s),
    ("s.startswith('Hello')", s.startswith("Hello")),
    ("s.find('Python')", s.find("Python")),
    ("s.find('zzz')", s.find("zzz")),
    ("s * 2", s * 2),
]
for k, v in rows:
    print(f"{k:28} -> {v!r}")

print(f"\n原串未被修改：{s!r}")


print("\n" + "=" * 60)
print("2. 索引与切片（负索引 Java 没有）")
print("=" * 60)

t = "abcdef"
print(f"t        = {t!r}")
for expr in ["t[0]", "t[-1]", "t[1:4]", "t[:3]", "t[3:]",
             "t[-2:]", "t[::2]", "t[::-1]", "t[4:1:-1]"]:
    print(f"{expr:12} = {eval(expr)!r}")


print("\n" + "=" * 60)
print("3. f-string：替代 String.format")
print("=" * 60)

name, age, ratio = "张三", 30, 0.856

print(f'f"{{name}} 今年 {{age}} 岁"       -> {f"{name} 今年 {age} 岁"}')
print(f'{{age:05d}}                       -> {f"{age:05d}"}')
print(f'{{3.14159:.2f}}                    -> {f"{3.14159:.2f}"}')
print(f'{{ratio:.1%}}                      -> {f"{ratio:.1%}"}')
print(f'{{1234567:,}}                      -> {f"{1234567:,}"}')
print(f'{{name!r}}                         -> {f"{name!r}"}')
print(f'{{age=}}                           -> {f"{age=}"}   <- 调试神器')
print(f'左对齐 {{"abc":<8}}|                -> {"abc":<8}|')
print(f'右对齐 {{"abc":>8}}|                -> {"abc":>8}|')
print(f'居中   {{"abc":^8}}|                -> {"abc":^8}|')
print(f'填充   {{"abc":*^9}}|               -> {"abc":*^9}|')

# 金额格式化（业务高频）
for v in (1234.5, 1234567.891, 0.5):
    print(f"金额 {v} -> {v:,.2f}")


print("\n" + "=" * 60)
print("4. 多行字符串（等价 Java 15 text block）")
print("=" * 60)

sql = """
SELECT *
FROM orders
WHERE id = :id
"""
print(sql)
print(f"repr 形式：{sql!r}")


print("=" * 60)
print("5. 常用清洗套路")
print("=" * 60)

raw = "  Hello ,  World  \n"
print(f"raw          = {raw!r}")
print(f"strip        = {raw.strip()!r}")
print(f"strip + split= {[w.strip() for w in raw.split(',')]}")
print(f"压空白       = {' '.join(raw.split())!r}")

dirty = "NO-001,,NO-002 , ,NO-003"
cleaned = [x.strip() for x in dirty.split(",") if x.strip()]
print(f"\n过滤空项     = {cleaned}")

# textwrap 处理长文本
long = "这是一段很长的中文文本，需要在固定宽度处换行以便在终端里阅读，"
print("\n" + textwrap.fill(long * 3, width=30))


print("=" * 60)
print("6. 编码：字符串是 Unicode，字节是 bytes")
print("=" * 60)

zh = "中文"
print(f"len('中文')        = {len(zh)}          <- 字符数")
print(f"len(zh.encode())   = {len(zh.encode())}     <- 字节数（UTF-8 每字 3 字节）")
print(f"zh.encode()        = {zh.encode()}")
print(f"b'\\xe4\\xb8\\xad'.decode('utf-8') = {b'\xe4\xb8\xad'.decode('utf-8')!r}")

# GBK 与 UTF-8 不通用，这是 Windows 乱码的根源
print(f"zh.encode('gbk')   = {zh.encode('gbk')}     <- GBK 每字 2 字节")
try:
    zh.encode("gbk").decode("utf-8")
except UnicodeDecodeError as e:
    print(f"用错编码解码    -> UnicodeDecodeError: {e.reason}")
