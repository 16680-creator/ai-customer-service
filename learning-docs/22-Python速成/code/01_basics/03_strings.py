"""03 · 字符串与格式化

运行: python 01_basics/03_strings.py
"""

# ---- 本段讲：字符串常用方法总览（全部返回新串，原串不可变）----
# 与 Java 的 String 一样是不可变对象，所以 s.upper() 不会改 s，必须接收返回值。
# 这里把"表达式 + 结果"装进 rows 列表，再统一格式化打印，省掉十几次手写 print。
import textwrap                                        # 标准库，用于按宽度折行（后面第 5 节用到）

s = "Hello, Python"                                    # str 字面量；单双引号等价，无 char 类型

print("=" * 60)
print("1. 基本操作（多数是返回新串，不改原串）")
print("=" * 60)

rows = [                                               # 列表元素是二元组：(表达式文本, 计算结果)
    ("len(s)", len(s)),                                # len 是内置函数，等价 s.length()
    ("s.upper()", s.upper()),                          # 全部转大写，返回新串
    ("s.lower()", s.lower()),                          # 全部转小写
    ("s.strip()", "  x  ".strip()),                    # 去首尾空白，等价 trim()
    ("s.replace('l','L')", s.replace("l", "L")),       # 全局替换，等价 replace；Java 的 replace 也是全替换
    ("s.split(', ')", s.split(", ")),                  # 按分隔符切分，返回 list，等价 split 返回数组
    ("'-'.join(['a','b'])", "-".join(["a", "b"])),     # 用分隔符拼接，等价 String.join
    ("'Python' in s", "Python" in s),                  # 子串判断，等价 contains
    ("s.startswith('Hello')", s.startswith("Hello")),  # 前缀判断，等价 startsWith
    ("s.find('Python')", s.find("Python")),            # 找子串，返回下标；找不到返回 -1（不是抛异常）
    ("s.find('zzz')", s.find("zzz")),                  # 找不到的情况，验证返回 -1
    ("s * 2", s * 2),                                  # 字符串重复，Python 特有语法糖
]
for k, v in rows:                                      # 解包二元组，k 是表达式文本、v 是结果
    print(f"{k:28} -> {v!r}")                          # :28 左对齐占 28 列；!r 让字符串带引号

print(f"\n原串未被修改：{s!r}")                        # 验证不可变性：所有操作都没动 s 本身


print("\n" + "=" * 60)
print("2. 索引与切片（负索引 Java 没有）")
print("=" * 60)

# ---- 本段讲：索引与切片，重点是负索引和 [start:stop:step] ----
# 切片语法是 s[start:stop:step]，左闭右开；step 为负表示从右往左走。
# 切片永远返回新对象，不会修改原串，也不会因为越界抛异常（会被自动截断）。
t = "abcdef"
print(f"t        = {t!r}")
for expr in ["t[0]", "t[-1]", "t[1:4]", "t[:3]", "t[3:]",
             "t[-2:]", "t[::2]", "t[::-1]", "t[4:1:-1]"]:   # 把待演示的表达式写成字符串，循环里统一 eval
    print(f"{expr:12} = {eval(expr)!r}")                    # eval 执行字符串里的表达式；仅演示用，生产代码别这么写


print("\n" + "=" * 60)
print("3. f-string：替代 String.format")
print("=" * 60)

# ---- 本段讲：f-string 的各种格式说明符 ----
# 语法是 f"{值:格式}"，冒号后面依次是"填充字符 + 对齐(<^>) + 宽度 + 类型(d/f/%/,) "。
# 这里外层用单引号、内层用双引号，是为了把 f-string 本身当文本打出来，方便对照语法。
name, age, ratio = "张三", 30, 0.856                   # 元组解包，一次给三个变量赋值

print(f'f"{{name}} 今年 {{age}} 岁"       -> {f"{name} 今年 {age} 岁"}')   # 双大括号 {{ }} 是转义，表示字面量花括号
print(f'{{age:05d}}                       -> {f"{age:05d}"}')              # :05d 补零到 5 位，等价 %05d
print(f'{{3.14159:.2f}}                    -> {f"{3.14159:.2f}"}')          # .2f 保留两位小数，等价 %.2f
print(f'{{ratio:.1%}}                      -> {f"{ratio:.1%}"}')            # :.1% 转百分比并保留一位小数，Java 没有直接对应
print(f'{{1234567:,}}                      -> {f"{1234567:,}"}')            # :, 加千分位逗号
print(f'{{name!r}}                         -> {f"{name!r}"}')               # !r 用 repr 显示，字符串会带引号
print(f'{{age=}}                           -> {f"{age=}"}   <- 调试神器')    # {x=} 输出 "x=值"，3.8+ 语法
print(f'左对齐 {{"abc":<8}}|                -> {"abc":<8}|')                 # < 左对齐，宽度 8，右侧补空格
print(f'右对齐 {{"abc":>8}}|                -> {"abc":>8}|')                 # > 右对齐
print(f'居中   {{"abc":^8}}|                -> {"abc":^8}|')                 # ^ 居中
print(f'填充   {{"abc":*^9}}|               -> {"abc":*^9}|')                # * 是填充字符，相当于居中对齐补 *

# 金额格式化（业务高频）
for v in (1234.5, 1234567.891, 0.5):                    # 遍历元组，逐个演示
    print(f"金额 {v} -> {v:,.2f}")                      # 千分位 + 两位小数，金额展示的标准写法


print("\n" + "=" * 60)
print("4. 多行字符串（等价 Java 15 text block）")
print("=" * 60)

# ---- 本段讲：三引号多行字符串 ----
# 三个双引号（或单引号）包住的内容会原样保留换行和缩进，等价 Java 15 的 text block。
# 常用于写 SQL、JSON 模板、帮助文本；注意首尾的换行也会成为字符串内容。
sql = """
SELECT *
FROM orders
WHERE id = :id
"""                                                # 字符串从第二行的换行开始，到结尾换行结束
print(sql)                                         # 直接打印：换行按原样输出
print(f"repr 形式：{sql!r}")                        # !r 能看到 \n 和首尾空白，便于检查多余空行


print("=" * 60)
print("5. 常用清洗套路")
print("=" * 60)

# ---- 本段讲：真实业务里最常见的几种字符串清洗套路 ----
# 组合使用 strip / split / join / 列表推导式，把脏输入变成干净列表或规整文本。
raw = "  Hello ,  World  \n"                          # 模拟用户输入：前后空格 + 换行
print(f"raw          = {raw!r}")                       # repr 让空格和换行显形，便于验证下面每一步
print(f"strip        = {raw.strip()!r}")               # 先后去首尾空白（含 \n），等价 trim
print(f"strip + split= {[w.strip() for w in raw.split(',')]}")   # 先按逗号切，再对每段去空格
print(f"压空白       = {' '.join(raw.split())!r}")     # 无参 split() 按任意空白切分并丢弃空串，再用单个空格拼回

dirty = "NO-001,,NO-002 , ,NO-003"                     # 模拟带空项的编号串
cleaned = [x.strip() for x in dirty.split(",") if x.strip()]   # 推导式：切分 → 去空格 → 过滤空串
print(f"\n过滤空项     = {cleaned}")                    # 得到只含有效编号的列表

# textwrap 处理长文本
long = "这是一段很长的中文文本，需要在固定宽度处换行以便在终端里阅读，"
print("\n" + textwrap.fill(long * 3, width=30))        # fill 按 30 字符宽度自动折行，Java 需手写循环


print("=" * 60)
print("6. 编码：字符串是 Unicode，字节是 bytes")
print("=" * 60)

# ---- 本段讲：str 与 bytes 的区别，以及编码不匹配为什么会产生乱码 ----
# str 是"字符"，长度按字符数算；bytes 是"字节"，长度按字节数算。
# encode() 把 str 编成 bytes，decode() 把 bytes 还原成 str，两边编码方式必须一致。
zh = "中文"
print(f"len('中文')        = {len(zh)}          <- 字符数")           # 2 个字符，与编码无关
print(f"len(zh.encode())   = {len(zh.encode())}     <- 字节数（UTF-8 每字 3 字节）")   # 默认 UTF-8，所以 6 字节
print(f"zh.encode()        = {zh.encode()}")                          # 显示为 b'\xe4\xb8\xad\xe6\x96\x87' 这种字节字面量
print(f"b'\\xe4\\xb8\\xad'.decode('utf-8') = {b'\xe4\xb8\xad'.decode('utf-8')!r}")   # 把 3 个字节按 UTF-8 还原成一个汉字

# GBK 与 UTF-8 不通用，这是 Windows 乱码的根源
print(f"zh.encode('gbk')   = {zh.encode('gbk')}     <- GBK 每字 2 字节")   # GBK 双字节编码，所以只有 4 字节
try:                                                                    # 故意用错编码来复现乱码
    zh.encode("gbk").decode("utf-8")                                    # GBK 字节拿 UTF-8 解，必然失败
except UnicodeDecodeError as e:                                         # 捕获解码异常（等价 Java 的 UnsupportedEncoding 类问题）
    print(f"用错编码解码    -> UnicodeDecodeError: {e.reason}")          # e.reason 是简短的错误描述
