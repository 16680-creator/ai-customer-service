# 缓存淘汰：LRU · LFU 与两级缓存

> 对应项目：`ai-cs-chat/.../cache/VectorCacheStore.java`（L1 进程内 LRU + L2 Redis 两级缓存）、
> `ai-cs-chat/.../cache/SemanticCacheService.java`（Redis ZSET 实现分布式 LRU + 余弦相似度）、
> `ai-cs-chat/.../cache/HotQaCacheService.java`（ZINCRBY 频次统计 + Top-N 热榜）。
> 相关：[04-树堆与TopK](./04-树堆与TopK.md)（TopK 与 ZSET）、[03-哈希](./03-哈希-冲突扰动与分片路由.md)（SHA-256 缓存键）。

---

## 一、为什么需要淘汰策略

缓存容量有限，装满了必须有取舍。**目标**：让缓存命中率最高。

**理论基础：局部性原理**

| 类型 | 含义 | 例子 |
|---|---|---|
| **时间局部性** | 刚被访问的数据，短期内容易再被访问 | 循环里的变量、热点商品 |
| **空间局部性** | 被访问的数据，其邻近数据也容易被访问 | 数组顺序遍历、数据库预读 |

**LRU 基于时间局部性**：最近用过的，很可能马上再用。
**LFU 基于访问频率**：用得最多的，大概率继续高频。

---

## 二、四种淘汰策略对比

| 策略 | 规则 | 优点 | 缺点 | 实现复杂度 |
|---|---|---|---|---|
| **FIFO** | 先进先出，淘汰最早进入的 | 极简单 | ❌ 可能淘汰热点数据 | O(1) |
| **LRU** | 淘汰最久未访问的 | ⭐ 符合时间局部性，通用性最好 | 一次全表扫描会"污染"缓存 | O(1) |
| **LFU** | 淘汰访问次数最少的 | 适合稳定热点 | ❌ 历史热点难淘汰；新数据饿死 | O(log n) |
| **Random** | 随机淘汰 | 无需维护状态 | 命中率不稳定 | O(1) |

### 2.1 LRU 的致命弱点：缓存污染

```
场景：缓存容量 3，内容 [A, B, C]（A 最久未用）

正常访问序列：A, B, C, A, B, C ... → 命中率高

一次全表扫描：D, E, F 依次进入
  → 淘汰 A、B、C
  → 扫描结束，缓存里是 [D, E, F]（都是一次性数据）
  → 后续正常的 A、B、C 访问全部 MISS ❌
```

**生产影响**：MySQL 的全表扫描、报表查询这类"一次性大批量访问"会把真正的热点数据挤出去。

**改进方案**：
- **LRU-K**：访问 K 次才进入缓存队列（K=2 最常用），过滤一次性访问
- **Two Queues（2Q）**：FIFO 队列 + LRU 队列，首次访问进 FIFO，再次访问才晋升到 LRU
- **Redis 的近似 LRU**：随机采样淘汰（见 §5）

### 2.2 LFU 的致命弱点：历史包袱

```
场景：热点迁移
  第 1 个月：商品 A 被访问 10 万次
  第 2 个月：商品 A 下架了，但累计频次 10 万，永远不会被淘汰 ❌
             新商品 B 频次 10，刚进就被淘汰 ❌
```

**改进**：
- **时间衰减 LFU**：频次随时间衰减（如每小时 ×0.9）
- **Window-LFU**：只统计最近时间窗口内的频次（本项目 `HotQaCacheService` 的做法，见 §6.3）

---

## 三、LRU 实现一：LinkedHashMap（项目在用，最简单）

```java
// ai-cs-chat/.../cache/VectorCacheStore.java
private final Map<String, float[]> l1Cache = Collections.synchronizedMap(
        new LinkedHashMap<>(128, 0.75f, true) {      // ⭐ 第三个参数 accessOrder=true
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                return size() > Math.max(1, properties.getVector().getL1MaxEntries());
            }
        });
```

### 3.1 三个关键点

| 参数/方法 | 作用 |
|---|---|
| `accessOrder = true` | `get()` 也算"访问"，会移动节点到链表尾部；`false` 时只有 `put` 影响顺序（即 FIFO） |
| `removeEldestEntry` | 插入后回调，返回 true 则删除最老节点（链表头部） |
| `Collections.synchronizedMap` | 保证线程安全（`LinkedHashMap` 本身不是） |

### 3.2 底层结构

`LinkedHashMap` = 哈希表 + **双向链表**：

```
哈希表（O(1) 定位）：
  bucket[0] → [key="A"]
  bucket[1] → [key="B"]
  ...

双向链表（维护访问顺序，头=最老，尾=最新）：
  head ⇄ [A] ⇄ [B] ⇄ [C] ⇄ tail
          ↑ 最久未访问      ↑ 最近访问

get("B") → B 移到尾部：head ⇄ [A] ⇄ [C] ⇄ [B] ⇄ tail
put("D") 且超容量 → 删除头部 A，D 插入尾部
```

### 3.3 复杂度

| 操作 | 复杂度 | 说明 |
|---|---|---|
| `get` | **O(1)** | 哈希表定位 + 链表移到尾部（已知节点，O(1)） |
| `put` | **O(1)** | 哈希表插入 + 链表尾插 |
| 淘汰 | **O(1)** | 删链表头节点 |
| 空间 | O(capacity) | |

**为什么链表操作是 O(1)**：哈希表直接给出节点引用，双向链表删除/移动已知节点只需改 4 个指针。

---

## 四、LRU 实现二：哈希表 + 双向链表（面试必背，LeetCode 146）

> 面试官通常要求**不用 `LinkedHashMap`**，手写一个。

```java
public class LRUCache {
    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();
    private final Node head = new Node(0, 0);   // 虚拟头哨兵
    private final Node tail = new Node(0, 0);   // 虚拟尾哨兵

    public LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        Node node = map.get(key);
        if (node == null) return -1;
        moveToTail(node);          // ⭐ 访问即"刷新"，移到最新
        return node.value;
    }

    public void put(int key, int value) {
        Node node = map.get(key);
        if (node != null) {
            node.value = value;    // 已存在，更新值并刷新
            moveToTail(node);
            return;
        }
        Node newNode = new Node(key, value);
        map.put(key, newNode);
        addToTail(newNode);
        if (map.size() > capacity) {
            Node toRemove = removeHead();   // 淘汰最久未访问
            map.remove(toRemove.key);       // ⭐ 别忘了从哈希表删除
        }
    }

    // ---- 双向链表操作 ----
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void addToTail(Node node) {
        node.prev = tail.prev;
        node.next = tail;
        tail.prev.next = node;
        tail.prev = node;
    }

    private void moveToTail(Node node) {
        removeNode(node);
        addToTail(node);
    }

    private Node removeHead() {
        Node first = head.next;
        removeNode(first);
        return first;
    }

    static class Node {
        int key, value;
        Node prev, next;
        Node(int k, int v) { key = k; value = v; }
    }
}
```

### 4.1 为什么必须用双向链表？

**删除节点需要知道它的前驱**。单链表删除要 O(n) 找前驱，双向链表 O(1)。

### 4.2 为什么用哨兵节点（dummy head/tail）？

```
无哨兵时需要处理大量边界：
  - 删除的是头节点？
  - 插入时链表为空？
  - 移动节点后它是尾节点？

有哨兵后，任何真实节点都有 prev 和 next，代码统一，无需判空
```

**这是链表题的通用技巧**：虚拟头尾节点消除边界判断。

### 4.3 易错点

| 坑 | 后果 | 正确做法 |
|---|---|---|
| 淘汰时只删链表，忘删 map | 内存泄漏 + 下次 get 到已删除节点 | `map.remove(node.key)` |
| 单链表实现 | 删除 O(n) | 用双向链表 |
| `put` 已存在的 key 时没更新 | 值不生效 | 先查 map，存在则更新并移到尾部 |
| 容量判断用 `>=` 还是 `>` | 差一错误 | 插入后 `size() > capacity` 才淘汰 |

---

## 五、Redis 的内存淘汰策略

Redis `maxmemory-policy` 共 8 种（Redis 6+）：

| 策略 | 范围 | 算法 |
|---|---|---|
| `noeviction` | — | 不淘汰，写操作返回错误（默认） |
| `allkeys-lru` | **所有 key** | **近似 LRU** ⭐ 最常用 |
| `volatile-lru` | 仅设了 TTL 的 key | 近似 LRU |
| `allkeys-lfu` | 所有 key | **近似 LFU**（Redis 4.0+） |
| `volatile-lfu` | 仅设了 TTL 的 key | 近似 LFU |
| `allkeys-random` | 所有 key | 随机 |
| `volatile-random` | 仅设了 TTL 的 key | 随机 |
| `volatile-ttl` | 仅设了 TTL 的 key | 淘汰剩余 TTL 最短的 |

### 5.1 为什么是"近似"LRU？

**严格 LRU 的代价**：需要维护一个全局链表，每次访问都要移动节点 → 内存开销大（每个 key 额外 24 字节指针）+ 频繁写操作。

**Redis 的做法：随机采样**

```c
// 每次淘汰时，随机取 maxmemory-samples（默认 5）个 key，
// 淘汰其中"最久未访问"的那个
// Redis 3.0+ 还有"淘汰池"（pool of 16）：跨多次调用保留最优候选，提高精度
```

**权衡**：用极小的精度损失，换取 O(1) 时间和零额外内存。
采样数 `maxmemory-samples` 设为 10 时，效果已接近严格 LRU（Redis 官方基准测试）。

### 5.2 Redis 的 LFU 实现（Redis 4.0+）

**核心**：用 24 位 lru 字段存 **16 位分钟级时间戳 + 8 位对数计数器**。

```
┌─────────────┬──────────────┐
│  16 bits    │    8 bits    │
│  LDT        │   counter    │
│ (分钟级时间) │  (对数频次)   │
└─────────────┴──────────────┘
```

| 字段 | 含义 |
|---|---|
| **LDT**（Last Decrement Time） | 上次衰减时间，分钟级精度 |
| **counter** | 访问频次，但用**对数**近似存储（0-255 表示 0 到几百万次） |

**两个精妙设计**：

**① 对数计数**：counter 最大 255，但要表示上百万次访问。
用概率递增——counter 越大，增长越慢：
```c
// counter 越大，随机递增的概率越低（近似对数）
double r = (double)rand() / RAND_MAX;
double p = 1.0 / (baseval * server.lfu_log_factor + 1);
if (r < p) counter++;
```

**② 时间衰减**：定期按"距离上次访问的分钟数"衰减 counter，解决历史包袱问题。
```c
// 每过 N 分钟，counter 减 N / lfu_decay_time
```

---

## 六、项目现场

### 6.1 VectorCacheStore：两级缓存 L1 + L2

```java
// ai-cs-chat/.../cache/VectorCacheStore.java
public float[] get(String modelKey, String text) {
    String key = cacheKey(modelKey, text);
    float[] cached = l1Cache.get(key);              // ① 先查 L1（进程内 LRU）
    if (cached != null) return cached;

    try {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + key);   // ② 再查 L2（Redis）
        if (json == null) return null;
        float[] vector = objectMapper.readValue(json, float[].class);
        l1Cache.put(key, vector);                   // ③ 命中 L2 → 回填 L1
        return vector;
    } catch (Exception e) {
        log.warn("向量缓存 L2 读取失败，降级 L1-only: err={}", e.getMessage());
        return null;                                // ④ Redis 挂了 → 降级，不阻断
    }
}

public void put(String modelKey, String text, float[] vector) {
    String key = cacheKey(modelKey, text);
    l1Cache.put(key, vector);                       // L1 常驻（靠 LRU 淘汰）
    try {
        redisTemplate.opsForValue().set(KEY_PREFIX + key, json, Duration.ofHours(ttlHours));
    } catch (Exception e) {
        log.warn("向量缓存 L2 写入失败（不影响业务）: err={}", e.getMessage());
    }                                               // L2 带 TTL
}
```

**架构图**：

```
                    查询请求
                       │
              ┌────────▼────────┐
              │  L1: 进程内 LRU  │ ← 纳秒级，无网络，但实例间不共享
              │  LinkedHashMap  │
              └────────┬────────┘
               命中 ✓   │ MISS
                       ▼
              ┌─────────────────┐
              │  L2: Redis      │ ← 毫秒级，多实例共享，可持久化
              └────────┬────────┘
               命中 ✓   │ MISS → 回填 L1
                       ▼
                  调用 Embedding API（秒级，昂贵）
                       │
                       └→ 双写 L1 + L2
```

**四个设计要点**：

| 要点 | 说明 |
|---|---|
| **读路径** | L1 → L2 → 回填 L1。回填让热点在每台机器上都有副本 |
| **写路径** | 双写（L1 常驻靠 LRU 淘汰，L2 带 TTL 自动过期） |
| **降级** | Redis 不可用时仅告警，退化为 L1-only，**不阻断检索链路** |
| **缓存键** | `modelKey + ":" + SHA-256(text)`——模型隔离 + 定长（见 [03-哈希](./03-哈希-冲突扰动与分片路由.md) §5） |

**为什么 L1 用 LRU 而不是 Redis 的 LFU？**
L1 容量很小（128 条），命中窗口短，时间局部性比频次更重要。且 LRU 的 `LinkedHashMap` 实现零依赖。

**为什么 Redis 挂了要降级而不是报错？**
向量缓存是**性能优化**而非**功能依赖**。缓存挂了只是变慢，不应该让整个 RAG 检索失败。
这体现了**降级设计原则**：非核心依赖必须可降级。

### 6.2 SemanticCacheService：ZSET 实现分布式 LRU

```java
// ai-cs-chat/.../cache/SemanticCacheService.java（简化）
// 记录访问：ZADD，score = 当前时间戳
redisTemplate.opsForZSet().add(INDEX_KEY, cacheKey, System.currentTimeMillis());

// 淘汰：ZSET 按 score（时间戳）升序，最小的就是最久未访问的
private void evictOverflow() {
    long size = redisTemplate.opsForZSet().zCard(INDEX_KEY);
    if (size <= maxEntries) return;
    long toEvict = size - maxEntries;
    // 取 score 最小的 toEvict 个（最久未访问），删除
    Set<String> oldest = redisTemplate.opsForZSet().range(INDEX_KEY, 0, toEvict - 1);
    ...
}
```

**算法解读**：

| 操作 | Redis 命令 | 复杂度 |
|---|---|---|
| 访问记录 | `ZADD key timestamp member` | O(log n) |
| 查最久未访问 | `ZRANGE key 0 k-1` | O(log n + k) |
| 删除 | `ZREM` | O(log n) |

**为什么用 ZSET 而不是给每个 key 设 TTL？**
TTL 实现的是"过期时间"而非"LRU"——TTL 到期就删，不管是否最近被访问。
ZSET 的 score 记录了**真实的最近访问时间**，能实现真正的 LRU 语义。

**对比 Redis 的 allkeys-lru**：
Redis 内置策略是全局的（对所有 key），这里是**业务级别的精确 LRU**（只对语义缓存这个集合）。业务自己控制范围更精准。

**完整查找流程**（含相似度匹配）：

```
1. 生成查询向量（走 VectorCacheStore 两级缓存）
2. 从 ZSET 拿到候选 key（上限 200 条）
3. 逐条算余弦相似度，O(n) 线性扫描
4. 超过阈值 → 命中，返回缓存答案
5. 命中后更新该条目的时间戳（ZADD 覆盖 score）→ 刷新 LRU 位置
```

### 6.3 HotQaCacheService：频次统计（近似 LFU）

```java
// ai-cs-chat/.../cache/HotQaCacheService.java（简化）
// 记录问题：ZINCRBY 累加频次
redisTemplate.opsForZSet().incrementScore(HOT_KEY, question, 1.0);

// 达阈值后提升进缓存（只有"够热"的问题才值得缓存答案）
if (score >= hotThreshold) { /* 写入 QA 缓存 */ }

// 取 Top-N 热榜
Set<ZSetOperations.TypedTuple<String>> top =
    redisTemplate.opsForZSet().reverseRangeWithScores(HOT_KEY, 0, topN - 1);
```

**这是"频次维度"的近似 LFU**：

| LFU 要素 | 本项目实现 |
|---|---|
| 记录访问频次 | `ZINCRBY`（累加 score） |
| 淘汰低频 | 达阈值才提升（低频问题根本不进缓存） |
| Top-N 热榜 | `ZREVRANGE`（跳表范围查询，O(log n + N)） |

**为什么是"近似"**：没有时间衰减（Redis LFU 有），理论上存在历史包袱。
但业务上问题热度本身变化不快，且 `HotQaCacheService` 主要用于**统计**而非淘汰，可接受。

---

## 七、缓存三大问题与解法

| 问题 | 定义 | 原因 | 解法 |
|---|---|---|---|
| **穿透** | 查**不存在**的数据，缓存永远不命中，全打到 DB | 恶意攻击 / 无效 ID | ① 缓存空值（短 TTL）② **布隆过滤器** ⭐ |
| **击穿** | **热点 key 过期**瞬间，大量请求同时打到 DB | 热点 key 集中失效 | ① 互斥锁重建（只放一个线程去查 DB）② 逻辑过期（永不过期，后台异步刷新） |
| **雪崩** | **大量 key 同时过期**，DB 瞬间压力暴增 | TTL 设置相同 | ① TTL 加随机抖动 ② 多级缓存 ③ 熔断降级 |

### 7.1 布隆过滤器防穿透

```java
// 查询前先过布隆过滤器
if (!bloomFilter.mightContain(userId)) {
    return null;        // 一定不存在，直接返回，不打 DB
}
// 可能存在 → 查缓存 → 查 DB
```

特性回顾（详见 [03-哈希](./03-哈希-冲突扰动与分片路由.md) §8）：
- **绝不假阴性**：说"不存在"就一定不存在 ← 防穿透的关键
- 可能假阳性（误报存在，概率约 1%，多查一次 DB，无害）
- 空间极省：100 万元素 + 1% 误判 ≈ 1.2 MB

### 7.2 击穿的互斥锁方案

```java
public Product getProduct(Long id) {
    Product p = cache.get(id);
    if (p != null) return p;

    String lockKey = "lock:product:" + id;
    if (redis.setNx(lockKey, "1", 10, TimeUnit.SECONDS)) {   // 抢锁
        try {
            p = db.query(id);           // 只放一个线程查 DB
            cache.put(id, p, ttl);
            return p;
        } finally {
            redis.delete(lockKey);
        }
    } else {
        Thread.sleep(50);               // 没抢到锁，短暂等待后重试读缓存
        return getProduct(id);
    }
}
```

**注意**：要设置锁的过期时间（防死锁），且用 `setNx + expire` 的**原子操作**（不能分两步）。

---

## 八、面试高频问答

**Q1：手写一个 LRU 缓存（LeetCode 146）。**
A：哈希表 + 双向链表。哈希表 O(1) 定位节点，双向链表 O(1) 删除/移动。用虚拟头尾哨兵消除边界判断。核心操作：`get` 后移到尾部，`put` 超容量时删头部并**同步删除 map 中的 key**。

**Q2：为什么用双向链表而不是单链表？**
A：删除节点需要知道前驱节点。单链表找前驱要 O(n) 遍历；双向链表直接 `node.prev` 即 O(1)。

**Q3：LinkedHashMap 怎么实现 LRU？**
A：构造时传 `accessOrder=true`，使 `get()` 也移动节点到链表尾部；重写 `removeEldestEntry()` 在超容量时返回 true 删除最老节点（链表头）。

**Q4：LRU 和 LFU 的优缺点？**
A：LRU 符合时间局部性，实现简单（O(1)），但一次全表扫描会污染缓存。LFU 适合稳定热点，但有历史包袱问题（旧热点难淘汰、新数据饿死），且实现复杂（需维护频次堆）。改进：LRU-K、2Q、带时间衰减的 LFU。

**Q5：Redis 的 LRU 为什么是"近似"的？**
A：严格 LRU 需要维护全局链表，每个 key 额外 24 字节指针 + 每次访问都要移动节点。Redis 改为**随机采样**（默认取 5 个样本，淘汰其中最久未访问的）+ 淘汰池优化，用极小精度损失换取 O(1) 时间和零额外内存。

**Q6：Redis LFU 的 8 位 counter 怎么表示上百万次访问？**
A：用**对数近似**——counter 越大，随机递增的概率越低（概率递增）。同时用 16 位记录上次衰减时间（分钟级），定期按时间衰减，解决历史包袱。

**Q7：缓存穿透、击穿、雪崩的区别和解法？**
A：穿透=查不存在的数据（布隆过滤器 / 缓存空值）；击穿=热点 key 过期瞬间大量请求（互斥锁重建 / 逻辑过期）；雪崩=大量 key 同时过期（TTL 加随机抖动 / 多级缓存 / 熔断降级）。

**Q8：本项目为什么用两级缓存而不是只用 Redis？**
A：L1 进程内 LRU 是纳秒级、无网络开销，能吃掉最热的那部分流量；Redis 是毫秒级、多实例共享，兜住跨实例的重复计算。两级配合既避免每次都走网络，又避免单机缓存的容量和共享限制。且 Redis 挂了可降级 L1-only。

**Q9：为什么 Redis 挂了要降级而不是抛异常？**
A：向量缓存是性能优化而非功能依赖。缓存失效只会导致变慢（多调一次 Embedding API），不应阻断 RAG 检索主链路。这是**非核心依赖必须可降级**的设计原则。

---

## 九、动手练习

1. 手写 LRU（LeetCode 146），要求 `get`/`put` 都是 O(1)。注意淘汰时同步删除 map 中的 key。
2. 在手写 LRU 基础上实现 **LRU-K**（K=2）：访问 2 次才进入缓存队列。
3. 分析本项目 `VectorCacheStore`：若 L1 容量设为 128，L2 TTL 设为 24 小时，单个实例 QPS 100，估算缓存命中率与内存占用。
4. 用 `HotQaCacheService` 的 ZSET 思路实现"近 7 天热榜"（提示：按天分 key，`ZUNIONSTORE` 合并）。
5. 思考：为什么 `SemanticCacheService` 用 ZSET 手动实现 LRU，而不是直接用 Redis 的 `allkeys-lru`？（提示：作用域不同——内置策略作用于所有 key，业务需要精确控制语义缓存这个集合）

---

> 上一篇：[08-动态规划与贪心](./08-动态规划与贪心.md) ｜ 下一篇：[10-限流算法：四大算法与网关实现](./10-限流算法-四大算法与网关实现.md)
