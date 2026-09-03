# 字符串匹配：Trie 与 PII 脱敏

> 对应项目：`ai-cs-chat/.../util/PiiMasker.java`（手机号/身份证/银行卡/邮箱/地址脱敏，含 Luhn 校验）、
> `ai-cs-chat/.../security/`（Guardrails 安全网关的规则匹配）、
> `ai-cs-search`（Elasticsearch 倒排索引，字符串匹配的工程化形态）。

---

## 一、问题定义

在主串 `text`（长度 n）中查找模式串 `pattern`（长度 m）出现的位置。

```
text    = "ababcabcacbab"
pattern = "abcac"
                 ↓ 在下标 5 处匹配
```

---

## 二、解法一：BF 暴力匹配 O(mn)

**思路**：主串每个位置都尝试匹配一次，失配就右移一位重来。

```java
public int bruteForce(String text, String pattern) {
    int n = text.length(), m = pattern.length();
    for (int i = 0; i <= n - m; i++) {           // 主串起点
        int j = 0;
        while (j < m && text.charAt(i + j) == pattern.charAt(j)) j++;
        if (j == m) return i;                     // 完全匹配
    }
    return -1;
}
```

**复杂度**：最坏 **O(m × n)**。

**最坏场景**：
```
text    = "aaaaaaaaaaaaaaaaab"
pattern = "aaaab"
每次都在最后一个字符才失配，白比 5 次
```

**优点**：实现简单；实际运行中（非病态数据）性能往往不差，因为失配通常发生在前几个字符。
Java 的 `String.indexOf` 在短模式串时就用类似思路。

---

## 三、解法二：RK 滚动哈希（Rabin-Karp）

**思路**：用**哈希值**代替字符串比较。先算 pattern 的哈希，再滑动窗口算主串每个长度 m 的子串哈希，相等再逐字符确认。

**关键优化：滚动哈希** —— O(1) 从 `hash(s[i..i+m-1])` 算出 `hash(s[i+1..i+m])`。

```
以十进制类比（实际用大素数取模）：
  hash("abc") = a×R² + b×R¹ + c×R⁰       （R 是进制，如 26 或 256）

  滚动到 "bcd"：
  hash("bcd") = (hash("abc") - a×R²) × R + d
                              ↑ 去掉最高位  ↑ 加上新低位

这样每一步只需 O(1)，而非重新计算 O(m)
```

```java
public int rabinKarp(String text, String pattern) {
    int n = text.length(), m = pattern.length();
    long R = 256;                    // 进制（字符集大小）
    long Q = 999_999_937L;           // 大素数取模，防溢出
    long RM = 1;
    for (int i = 1; i < m; i++) RM = (RM * R) % Q;   // R^(m-1) % Q

    long patHash = hash(pattern, m, R, Q);
    long txtHash = hash(text, m, R, Q);

    if (patHash == txtHash) return 0;              // 首个窗口就匹配

    for (int i = m; i < n; i++) {
        // 滚动：去掉离开窗口的最高位字符，加上新进入的字符
        txtHash = (txtHash + Q - RM * text.charAt(i - m) % Q) % Q;
        txtHash = (txtHash * R + text.charAt(i)) % Q;

        if (patHash == txtHash) {
            // ⚠️ 哈希相等 ≠ 字符串相等（哈希碰撞），必须逐字符确认
            if (text.substring(i - m + 1, i + 1).equals(pattern)) {
                return i - m + 1;
            }
        }
    }
    return -1;
}

private long hash(String s, int m, long R, long Q) {
    long h = 0;
    for (int i = 0; i < m; i++) h = (h * R + s.charAt(i)) % Q;
    return h;
}
```

**复杂度**：
- 平均 **O(n + m)**（哈希计算 O(m) + 窗口滑动 O(n)）
- 最坏 O(mn)（所有窗口都哈希碰撞，每次都要 O(m) 确认——概率极低）

**要点**：
1. **必须二次确认**：哈希碰撞会导致假匹配，命中后要 `equals` 验证
2. **大素数取模**：防溢出 + 让分布均匀
3. **滚动是核心**：减最高位、乘 R、加新位，O(1) 更新

**与项目关联**：`VectorCacheStore` 用 SHA-256 做缓存键，本质是"用哈希代替长字符串比较"——思路同源，只是它要抗碰撞（用密码学哈希），而 RK 追求快。

---

## 四、解法三：KMP（面试必考，必背）

### 4.1 核心洞察：失配时不要回退主串指针

**BF 的问题**：主串指针 `i` 回退，重复比较已经比对过的字符。

```
text    = "abababca"
pattern = "abababca..."
              abababca
         i=0: a b a b a b c a
              a b a b a b c a
                          ↑ 失配

BF 的做法：i 回退到 1，j 回退到 0，重新比
KMP 的做法：i 不动，j 回退到"pattern 的最长相等前后缀"位置，继续比
```

**关键概念：最长相等前后缀（LPS / 部分匹配值）**

```
模式串 "abababca"

前缀集合（不含整体）："a", "ab", "aba", "abab", "ababa", "ababab", "abababc"
后缀集合（不含整体）："a", "ba", "bca", "abca", "babca", "ababca", "bababca"

最长相等前后缀 = "a"（长度 1）
```

### 4.2 next 数组（部分匹配表）

`next[j]` = `pattern[0..j-1]` 的最长相等前后缀长度。

```java
private int[] buildNext(String pattern) {
    int m = pattern.length();
    int[] next = new int[m + 1];     // next[j] 对应 pattern[0..j-1]
    next[0] = -1;                     // 哨兵：表示无法再回退
    int i = 0, j = -1;

    while (i < m) {
        if (j == -1 || pattern.charAt(i) == pattern.charAt(j)) {
            i++; j++;
            next[i] = j;
        } else {
            j = next[j];              // ⭐ 回退到更短的前后缀
        }
    }
    return next;
}
```

**举例**：`pattern = "abababca"`

| j | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|---|
| pattern | - | a | b | a | b | a | b | c | a |
| next[j] | **-1** | 0 | 0 | 1 | 2 | 3 | 4 | 0 | 1 |

### 4.3 KMP 主流程

```java
public int kmp(String text, String pattern) {
    int n = text.length(), m = pattern.length();
    int[] next = buildNext(pattern);

    int i = 0, j = 0;                // i 指向主串（永不回退），j 指向模式串
    while (i < n && j < m) {
        if (j == -1 || text.charAt(i) == pattern.charAt(j)) {
            i++; j++;                // 匹配，双双前进
        } else {
            j = next[j];             // ⭐ 失配：模式串右移，主串 i 不动
        }
    }
    return j == m ? i - m : -1;
}
```

**为什么 i 不回退？**
因为 `next[j]` 已经告诉我们：pattern 的前 `next[j]` 个字符与主串当前位置之前的部分**已经匹配过了**，无需重新比较。

### 4.4 复杂度

| 阶段 | 复杂度 | 说明 |
|---|---|---|
| 构建 next | **O(m)** | i 单调递增 |
| 匹配 | **O(n)** | i 单调递增，不回退 |
| **总计** | **O(n + m)** | 线性 |
| 空间 | O(m) | next 数组 |

**对比**：

| 算法 | 时间 | 空间 | 特点 |
|---|---|---|---|
| BF | O(mn) | O(1) | 简单，实际常够用 |
| RK | 平均 O(n+m) | O(1) | 需处理哈希碰撞 |
| **KMP** | **O(n+m)** | O(m) | ⭐ 稳定线性，无碰撞风险 |
| Sunday / BM | 平均 O(n/m) | O(m) | 实际最快（跳跃距离大），但最坏 O(mn) |

> **工程现实**：BM（Boyer-Moore）及其变体（Sunday）在实际文本搜索中通常比 KMP 更快，
> 因为它们能一次跳过多个字符（KMP 每次至少前进 1）。
> 但 **KMP 保证了最坏 O(n+m)**，且是面试标准考点，必须会手写。

---

## 五、Trie 树（字典树 / 前缀树）

### 5.1 结构

**用途**：高效存储和查找**字符串集合**，特别适合**前缀匹配**。

```
插入 "cat", "car", "card", "dog", "do"：

              root
             /    \
            c      d
            |      |
            a      o —— (end)   ← "do" 结束
           / \     |
          t   r    g —— (end)   ← "dog" 结束
        (end) |   
              d —— (end)        ← "card" 结束
```

**节点结构**：

```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd = false;      // 标记：从 root 到这里的路径构成一个完整单词
}
```

### 5.2 实现

```java
public class Trie {
    private final TrieNode root = new TrieNode();

    // 插入：O(m)，m = 单词长度
    public void insert(String word) {
        TrieNode cur = root;
        for (char c : word.toCharArray()) {
            cur.children.putIfAbsent(c, new TrieNode());
            cur = cur.children.get(c);
        }
        cur.isEnd = true;
    }

    // 查找完整单词：O(m)
    public boolean search(String word) {
        TrieNode node = find(word);
        return node != null && node.isEnd;
    }

    // 前缀匹配：O(m)
    public boolean startsWith(String prefix) {
        return find(prefix) != null;
    }

    private TrieNode find(String s) {
        TrieNode cur = root;
        for (char c : s.toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) return null;
        }
        return cur;
    }
}
```

### 5.3 复杂度与优劣

| 维度 | 说明 |
|---|---|
| 插入 / 查找 | **O(m)**，m = 字符串长度，**与集合中单词数量无关** ⭐ |
| 空间 | O(总字符数)，但每个节点有 Map 开销，可能较大 |
| 优势 | **前缀匹配** O(m)，哈希表做不到 |
| 劣势 | 空间开销大（指针/Map 比字符本身还大） |

**优化技巧**：
- 字符集确定时（如只含小写字母）用**数组**代替 HashMap：`TrieNode[] children = new TrieNode[26]`
- 大量稀疏节点时用**压缩 Trie**（Radix Tree / PATRICIA Tree）合并单子节点
- 用 `Character` 的 ASCII 直接索引

### 5.4 应用场景

| 场景 | 说明 |
|---|---|
| **敏感词过滤** | 见 §7 |
| **搜索自动补全** | 输入前缀 → 找所有该前缀的候选词 |
| 拼写检查 | 前缀树 + 编辑距离 |
| IP 路由表 | 最长前缀匹配（路由器用压缩 Trie） |
| 词频统计 | 节点上存计数 |

---

## 六、敏感词过滤（Trie 的经典应用）

### 6.1 方案对比

| 方案 | 复杂度 | 问题 |
|---|---|---|
| 逐个 `contains` | O(n × k)，k = 敏感词数量 | 词库 1 万时极慢 |
| **Trie** ⭐ | **O(n)** | 主串每个字符最多匹配最长敏感词的长度 |
| AC 自动机 | O(n) | Trie + KMP 的 fail 指针，理论最优，实现复杂 |
| DFA | O(n) | 与 Trie 类似，用状态转移表 |

### 6.2 Trie 实现敏感词过滤

```java
public String filter(String text, Trie trie) {
    StringBuilder sb = new StringBuilder();
    int n = text.length();

    for (int i = 0; i < n; ) {
        TrieNode cur = trie.root;
        int matchLen = 0;              // 从 i 开始匹配到的最长敏感词长度

        for (int j = i; j < n; j++) {
            TrieNode next = cur.children.get(text.charAt(j));
            if (next == null) break;   // 无法继续匹配
            cur = next;
            if (cur.isEnd) {
                matchLen = j - i + 1;  // 记录一次完整命中
                // ⚠️ 不 break！继续找"最长的"（如 "傻" 和 "傻逼" 都要能匹配到后者）
            }
        }

        if (matchLen > 0) {
            sb.append("*".repeat(matchLen));   // 替换为等长星号
            i += matchLen;                     // 跳过整个敏感词
        } else {
            sb.append(text.charAt(i));
            i++;
        }
    }
    return sb.toString();
}
```

**复杂度**：O(n × L)，L = 最长敏感词长度（通常很小，常数级）→ **实际 O(n)**。

### 6.3 AC 自动机（了解）

**问题**：Trie 在失配时要从 root 重新开始，若敏感词有大量公共前缀（如 "abc"、"bc"、"c"），会重复扫描。

**AC 自动机 = Trie + KMP 的 fail 指针**：
- 每个节点有一个 `fail` 指针，指向"当前串的最长可匹配后缀"对应的节点
- 失配时沿 fail 指针跳转，不回退主串指针
- **复杂度严格 O(n)**，且构建是 O(总字符数)

**何时用**：词库超过 1 万、对性能极致要求（如网关级内容审核）。一般业务用 Trie 足够。

---

## 七、项目现场：PiiMasker 脱敏

```java
// ai-cs-chat/.../util/PiiMasker.java
private static final Pattern PHONE     = Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");
private static final Pattern ID_CARD   = Pattern.compile("(?<!\\d)(\\d{6})\\d{8}(\\d{3}[\\dXx])(?!\\d)");
private static final Pattern BANK_CARD = Pattern.compile("(?<!\\d)(\\d{6})\\d{3,9}(\\d{4})(?!\\d)");
private static final Pattern EMAIL     = Pattern.compile("[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
private static final Pattern ADDRESS_NO= Pattern.compile("(\\d{1,6})(号(?:楼|院|栋)?|号楼|栋)");

public String mask(String text) {
    if (text == null || text.isEmpty()) return text;
    // ⭐ 脱敏顺序：先长后短、先具体后宽泛
    String masked = ID_CARD.matcher(text).replaceAll("$1********$2");   // 身份证 18 位
    masked = maskBankCards(masked);                                      // 银行卡 13-19 位 + Luhn
    masked = PHONE.matcher(masked).replaceAll("$1****$2");               // 手机号 11 位
    masked = EMAIL.matcher(masked).replaceAll("***@$1");                 // 邮箱
    masked = ADDRESS_NO.matcher(masked).replaceAll("***$2");             // 地址门牌
    return masked;
}
```

### 7.1 正则的三个精妙设计

#### ① 前后向断言 `(?<!\d)` `(?!\d)` —— 防误伤

```java
// 没有边界断言时：
"20260814000000123456"  ← 20 位订单号
       └── 18 位 ──┘   ← 会被身份证正则匹配！
              └─11位─┘  ← 还会被手机号正则匹配！

// 加了 (?<!\d)(?!\d) 后：
// 要求匹配的 18 位前后都不能是数字
// 订单号内部每一位前后都有数字 → 不匹配 ✓
```

**这是一个真实踩过的坑**（源码注释里明确记录）：
> 实战踩坑：20 位订单号 "20260814000000123456" 曾先被身份证正则（18位子串）、
> 再被手机号正则（11位子串）在长数字串内部滑动命中——加边界后不再误伤。

**零宽断言一览**：

| 语法 | 名称 | 含义 |
|---|---|---|
| `(?=X)` | 正向先行断言 | 后面必须跟着 X |
| `(?!X)` | 负向先行断言 | 后面**不能**跟着 X |
| `(?<=X)` | 正向后行断言 | 前面必须是 X |
| `(?<!X)` | 负向后行断言 | 前面**不能**是 X |

**"零宽"**：只做条件判断，不消耗字符、不进捕获组。

#### ② 捕获组保留部分内容

```java
PHONE = "(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)"
//        ↑断言    ↑组1（前3位） ↑丢弃  ↑组2（后4位） ↑断言

replaceAll("$1****$2")
//          组1  星号  组2

"13812345678" → "138****5678"
```

**设计考量**：保留前 3 位（号段）+ 后 4 位（用户可辨识自己的号码），遮蔽中间 4 位。
这是**可用性与安全性的平衡**——全遮蔽则用户无法确认是哪个号码。

#### ③ 脱敏顺序：先长后短、先具体后宽泛

```
身份证（18位） → 银行卡（13-19位+Luhn） → 手机号（11位） → 邮箱 → 地址
   ↑ 最长最具体                                        ↑ 最短最宽泛
```

**为什么必须有序**：
- 若先跑手机号（11 位），身份证号的 11 位子串会被误当手机号脱敏，导致后面的身份证正则匹配不上
- 若先跑宽泛的地址正则，可能破坏其他模式的结构

**通用原则**：**更具体、更长、更严格的规则先执行**。

### 7.2 Luhn 校验（银行卡合法性）

```java
private static boolean luhnValid(String digits) {
    if (digits.length() < 13 || digits.length() > 19) return false;
    int sum = 0;
    boolean alternate = false;
    for (int i = digits.length() - 1; i >= 0; i--) {   // 从右往左
        int d = digits.charAt(i) - '0';
        if (d < 0 || d > 9) return false;
        if (alternate) {                                // 隔位
            d *= 2;
            if (d > 9) d -= 9;                          // 两位数则减 9（等价于数位求和）
        }
        sum += d;
        alternate = !alternate;
    }
    return sum % 10 == 0;
}
```

**Luhn 算法步骤**：
1. 从右往左，从**倒数第二位**开始，隔位乘 2
2. 乘积若 > 9，则减 9（等价于 `d*2` 的十位 + 个位）
3. 全部相加，和能被 10 整除则合法

**示例**（ Visa 测试卡号 `4111111111111111`）：
```
4 1 1 1  1 1 1 1  1 1 1 1  1 1 1 1
 ↓   ↓   ↓   ↓    ↓   ↓   ↓   ↓
×2  ×2  ×2  ×2   ×2  ×2  ×2  ×2     （从右数第2、4、6...位）

8 1 2 1  2 1 2 1  2 1 2 1  2 1 2 1
和 = 8+1+2+1+2+1+2+1+2+1+2+1+2+1+2+1 = 30
30 % 10 == 0 ✓ 合法
```

**作用**：
1. **防误伤**（本项目主要目的）：订单号、时间戳等数字串大概率通不过 Luhn，不会被误判为银行卡
2. **校验输入**：用户输入卡号时立即发现手误（ISO/IEC 7812 标准）

**复杂度**：O(d)，d = 位数（≤19），可视为 O(1)。

> ⚠️ 注意：Luhn 只是**校验位算法**，不是加密，也不代表卡真实存在。

### 7.3 正则的性能注意

```java
// ⚠️ 危险：灾难性回溯（Catastrophic Backtracking）
Pattern evil = Pattern.compile("(a+)+b");
// 匹配 "aaaaaaaaaaaaaaaaaaaaaaaaaaaaa"（无 b）：
// 回溯次数是 2^n 级别，n=30 时约 10 亿次 → 卡死（ReDoS 攻击）

// ✅ 安全改进：
// 1. 避免嵌套量词 (a+)+、(a*)*
// 2. 用独占量词 a++ （不回溯）
// 3. 用原子组 (?>...)
```

**ReDoS（正则表达式拒绝服务攻击）**：恶意输入触发灾难性回溯，耗尽 CPU。
**本项目风险点**：PiiMasker 处理用户输入的长文本，正则若设计不当可能被攻击。

**本项目的安全性**：`PiiMasker` 的正则都是**简单线性模式**（无嵌套量词），不存在回溯爆炸。
且 `SecurityAuditRecorder` 在落库前会对输入做 `truncate(rawInput, 512)` **截断**，双重保险。

---

## 八、正则引擎原理（简要）

| 类型 | 实现 | 特点 | 代表 |
|---|---|---|---|
| **DFA**（确定有限自动机） | 状态转移，无回溯 | 快 O(n)，但功能弱（不支持反向引用、捕获组） | awk、grep |
| **NFA**（非确定有限自动机） | 回溯试探 | 功能强，但可能指数级回溯 | **Java、Python、PCRE、JavaScript** |

Java 的 `Pattern` 是 **NFA 回溯型**引擎，所以支持捕获组、反向引用、环视断言，但要警惕回溯爆炸。

---

## 九、面试高频问答

**Q1：KMP 相比暴力匹配优化在哪？next 数组的含义？**
A：暴力匹配失配时主串指针回退，重复比较已匹配的字符。KMP 利用"已匹配部分的结构信息"，失配时主串指针不回退，只移动模式串到"最长相等前后缀"位置。`next[j]` = `pattern[0..j-1]` 的最长相等前后缀长度。复杂度 O(n+m)。

**Q2：KMP 的 next 数组怎么构建？**
A：类似自己做匹配——用 pattern 的前缀去匹配 pattern 本身。双指针 i（主串位置）和 j（最长前后缀长度），匹配则双双前进，失配则 `j = next[j]` 回退。

**Q3：RK 算法的"滚动哈希"怎么做到 O(1) 更新？**
A：`hash(新) = (hash(旧) - 离开字符 × R^(m-1)) × R + 新进入字符`。去掉最高位贡献，整体左移一位，加上新低位。注意取模防溢出，且哈希命中后必须逐字符确认防碰撞。

**Q4：Trie 的查找复杂度为什么与单词数量无关？**
A：查找时沿着字符串逐字符走，每步从当前节点的 children 里取下一个字符对应的节点（O(1)）。总步数 = 字符串长度 m，与树里存了多少单词无关。这是它相比"遍历词库逐个比较"的核心优势。

**Q5：Trie 和哈希表怎么选？**
A：只需精确匹配 → 哈希表（O(1) 且省空间）。需要**前缀匹配**、自动补全、按字典序遍历 → Trie。Trie 的空间开销大（每个节点一个 Map/数组）。

**Q6：敏感词过滤用什么算法？**
A：词库小（几百）用 Trie 即可，O(n)。词库大（万级）且性能要求高用 **AC 自动机**（Trie + fail 指针，严格 O(n) 且无重复扫描）。也可用 DFA 状态转移表。

**Q7：正则的 `(?<!\d)` 和 `(?!\d)` 是什么？为什么本项目必须加？**
A：负向后行断言和负向先行断言，属于**零宽断言**（只判断不消耗字符）。本项目必须加是因为：20 位订单号内部包含 18 位和 11 位数字子串，不加边界会被身份证、手机号正则误匹配（这是源码注释里记录的真实踩坑）。

**Q8：什么是灾难性回溯 / ReDoS？**
A：NFA 正则引擎在嵌套量词（如 `(a+)+b`）遇到不匹配的长输入时，回溯次数呈指数级增长（2^n），导致 CPU 耗尽。防范：避免嵌套量词、用独占量词/原子组、限制输入长度。本项目通过"简单正则 + 输入截断 512 字符"双重防护。

**Q9：Luhn 算法的作用？**
A：银行卡号的校验位算法。从右往左隔位乘 2（>9 则减 9），求和能被 10 整除则格式合法。本项目用它**过滤误判**——订单号/时间戳通不过 Luhn，不会被误脱敏。

---

## 十、动手练习

1. 手写 KMP 的 `buildNext`，对 `"abababca"` 输出完整 next 数组（答案见 §4.2 表格）。
2. 用 Trie 实现"商品名称自动补全"：输入 `"iph"`，返回所有以它开头的商品名。
3. 给 `PiiMasker` 增加一条规则：脱敏 IPv4 地址（保留前两段，如 `192.168.***.***`）。注意边界断言，避免匹配到版本号 `1.2.3.4`。
4. 分析：为什么 `PiiMasker` 的正则顺序是"身份证 → 银行卡 → 手机号"，如果改成"手机号 → 身份证 → 银行卡"会出什么问题？

---

> 上一篇：[06-排序与二分查找](./06-排序与二分查找.md) ｜ 下一篇：[08-动态规划与贪心](./08-动态规划与贪心.md)
