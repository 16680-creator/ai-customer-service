# 树 · 堆与 TopK

> 对应项目：`ai-cs-chat/.../rag/retrieve/MultiQueryMerger.java`（RRF 融合取 Top-N）、
> `ai-cs-chat/.../cache/HotQaCacheService.java`（ZSET 热度 Top-N）、
> `ai-cs-chat/.../cache/SemanticCacheService.java`（ZSET 时间戳做分布式 LRU）。

---

## 一、树的基础概念

```
        1          ← 根节点（root），深度 0
       / \
      2   3        ← 深度 1
     / \   \
    4   5   6      ← 深度 2
   /
  7                ← 深度 3，叶子节点（leaf）

节点 1 的高度 = 3（到最深叶子的边数）
节点 1 的深度 = 0（到根的边数）
节点 2 的子树 = {2,4,5,7}
```

**术语对照**（面试常考但容易记混）：

| 术语 | 定义 | 计算方向 |
|---|---|---|
| 深度（depth） | 根到该节点的边数 | **从上往下**，根=0 |
| 高度（height） | 该节点到最深叶子的边数 | **从下往上**，叶子=0 |
| 度数（degree） | 子节点个数 | — |
| 层（level） | 深度 + 1（有些教材根为第 1 层） | — |

### 二叉树分类

| 类型 | 定义 | 性质 |
|---|---|---|
| 满二叉树 | 每个节点有 0 或 2 个子节点 | — |
| **完全二叉树** | 除最后一层外全满，最后一层靠左排列 | **可用数组存储，堆的基础** |
| 二叉搜索树 BST | 左 < 根 < 右 | 中序遍历有序，但可能退化为链表 |
| **平衡二叉树 AVL** | 任意节点左右子树高度差 ≤ 1 | 严格平衡，插入删除旋转频繁 |
| **红黑树** | 五大性质，最长路径 ≤ 2× 最短路径 | 近似平衡，插入删除最多 3 次旋转，工程首选 |

---

## 二、完全二叉树的数组表示（堆的基石）

完全二叉树可以**不用指针**，直接用数组存，这是堆高效的关键。

```
下标（从 0 开始）：
              0
            /   \
           1     2
          / \   /
         3   4 5

数组: [0, 1, 2, 3, 4, 5]
```

**下标关系**（0-based）：

| 关系 | 公式 | 位运算 |
|---|---|---|
| 父节点 | `(i - 1) / 2` | `(i - 1) >> 1` |
| 左孩子 | `2i + 1` | `(i << 1) + 1` |
| 右孩子 | `2i + 2` | `(i << 1) + 2` |
| 最后一个非叶子节点 | `n/2 - 1` | `(n >> 1) - 1` |

**为什么完全二叉树才能用数组**：节点编号连续无空洞。普通二叉树用数组会浪费大量空间（缺失节点占空位）。

---

## 三、堆（Heap / PriorityQueue）

### 3.1 堆的两个性质

1. **结构性**：是一棵**完全二叉树**（用数组存）
2. **堆序性**：
   - **小顶堆**：每个节点 ≤ 其子节点 → 堆顶是最小值
   - **大顶堆**：每个节点 ≥ 其子节点 → 堆顶是最大值

```
大顶堆（数组 [9, 8, 6, 5, 7, 3]）：

            9
          /   \
         8     6
        / \   /
       5   7 3

堆顶 9 是最大值；但注意：左右子树之间无序（8 > 6 只是巧合）
```

> ⚠️ 堆**不是**有序结构！只能保证堆顶是极值。要整体有序需反复取堆顶（堆排序）。

### 3.2 核心操作：上浮（siftUp）与下沉（siftDown）

```java
// 大顶堆：插入元素，向上调整
private void siftUp(int[] heap, int i) {
    while (i > 0) {
        int parent = (i - 1) >> 1;
        if (heap[i] <= heap[parent]) break;      // 已满足堆序，停止
        swap(heap, i, parent);
        i = parent;
    }
}

// 大顶堆：删除堆顶（或建堆时），向下调整
private void siftDown(int[] heap, int i, int n) {
    while (true) {
        int left = (i << 1) + 1, right = left + 1, largest = i;
        if (left  < n && heap[left]  > heap[largest]) largest = left;
        if (right < n && heap[right] > heap[largest]) largest = right;
        if (largest == i) break;                 // 已满足堆序，停止
        swap(heap, i, largest);
        i = largest;
    }
}
```

**复杂度**：上浮/下沉都是 **O(log n)**（树高）。

### 3.3 建堆：O(n) 而非 O(n log n)

**朴素做法**：逐个插入，每次 `siftUp` → `n × O(log n)` = **O(n log n)**。

**Floyd 建堆法**：从最后一个非叶子节点开始，从右到左、从下到上依次 `siftDown` → **O(n)**。

```java
// O(n) 建堆
for (int i = (n >> 1) - 1; i >= 0; i--) {
    siftDown(heap, i, n);
}
```

**为什么是 O(n)**？

大部分节点在**底层**，它们的下沉距离很短：

| 层 | 节点数 | 最多下沉层数 | 总工作量 |
|---|---|---|---|
| 倒数第 1 层（叶子） | n/2 | 0 | 0 |
| 倒数第 2 层 | n/4 | 1 | n/4 × 1 |
| 倒数第 3 层 | n/8 | 2 | n/8 × 2 |
| ... | ... | k | n/2^(k+1) × k |

总和 = `Σ (n/2^(k+1)) × k` = `n × Σ k/2^(k+1)` ≈ **n × 1 = O(n)**（级数收敛到 2）

**直觉**：节点数多的最底层下沉距离几乎为 0，节点数少的顶层才需要下沉很多层——加权求和后是 O(n)。

### 3.4 PriorityQueue 的复杂度

| 操作 | 复杂度 | 说明 |
|---|---|---|
| `offer(e)` 入队 | **O(log n)** | 尾部插入 + siftUp |
| `poll()` 出队（取极值） | **O(log n)** | 堆顶出 + 末元素补位 + siftDown |
| `peek()` 看堆顶 | **O(1)** | 直接 `heap[0]` |
| `contains(e)` | **O(n)** | ⚠️ 堆不维护元素索引，需全扫描 |
| `remove(e)` | **O(n)** | ⚠️ 先 O(n) 找再 O(log n) 删 |
| 建堆 `new PriorityQueue<>(list)` | **O(n)** | Floyd 建堆 |

> **工程坑**：`PriorityQueue` 的 `contains`/`remove` 是 O(n)。
> 需要频繁删除任意元素时（如 Dijkstra 的 decrease-key、定时任务取消），要自己维护"元素 → 下标"的哈希映射（`HashMap` + 自定义堆），把 remove 降到 O(log n)。

---

## 四、TopK 问题：四种解法（面试必考）

**问题**：从 n 个数中找出最大（或最小）的 K 个。

### 4.1 解法对比

| 解法 | 时间复杂度 | 空间 | 适用场景 | 评价 |
|---|---|---|---|---|
| **① 全排序** | O(n log n) | O(1)~O(n) | K 接近 n | 简单但浪费 |
| **② 堆（推荐）** | **O(n log K)** | O(K) | **n 很大、K 很小** | ⭐ 最实用 |
| **③ 快排分区** | **平均 O(n)**，最坏 O(n²) | O(1) | 内存紧张、可原地 | 最快但不稳定 |
| **④ 计数/桶** | O(n + range) | O(range) | 数据范围小（如 0-100 分） | 特定场景 |

### 4.2 解法②：堆（重点，必背）

**核心**：维护一个**大小为 K 的小顶堆**。

```java
// 求最大的 K 个数 → 用小顶堆
public int[] topKLargest(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();   // Java 默认小顶堆

    for (int num : nums) {
        if (minHeap.size() < k) {
            minHeap.offer(num);                    // 堆没满，直接加
        } else if (num > minHeap.peek()) {         // 比堆顶（当前第 K 大）还大
            minHeap.poll();                        // 淘汰堆顶
            minHeap.offer(num);                    // 新元素入堆
        }
        // num <= 堆顶 → 直接丢弃，不进堆
    }

    int[] ans = new int[k];
    for (int i = k - 1; i >= 0; i--) ans[i] = minHeap.poll();
    return ans;
}
```

**为什么是"小顶堆"求最大 K 个？**（易错点）

```
小顶堆的堆顶 = 堆中最小的元素 = 当前 Top-K 集合的"门槛"

新元素比门槛还小 → 它进不了 Top-K，丢弃
新元素比门槛大   → 它挤掉门槛，成为新的门槛

始终保持：堆里是"目前见过的最大的 K 个"，堆顶是这 K 个里最小的（第 K 名）
```

**记忆口诀**：
- 求**最大** K 个 → **小**顶堆（堆顶是门槛，小者淘汰）
- 求**最小** K 个 → **大**顶堆（堆顶是门槛，大者淘汰）

**复杂度推导**：
- 遍历 n 个元素，堆大小 ≤ K
- 最坏每次都触发 `poll + offer` → `n × O(log K)`
- **总计 O(n log K)**，空间 **O(K）**

**对比全排序**：n = 10 亿、K = 100 时：
- 全排序：10^9 × 30 ≈ 3×10^10 次操作，且需装下全部数据
- 堆：10^9 × log₂100 ≈ 10^9 × 6.6 ≈ 6.6×10^9 次，且只占 100 个元素的内存
- **更关键**：堆可以**流式处理**（数据一次读入，不用全存内存），这是海量数据的唯一解

### 4.3 解法③：快排分区（平均 O(n)）

利用快排的 `partition`：一次分区后，pivot 的最终位置 `p` 是它的排序位置。

```java
public int[] topKFrequentByPartition(int[] nums, int k) {
    int left = 0, right = nums.length - 1;
    int target = nums.length - k;              // 第 K 大应落在的下标
    while (true) {
        int p = partition(nums, left, right);  // O(n) 分区
        if (p == target) {
            return Arrays.copyOfRange(nums, p, nums.length);
        } else if (p < target) {
            left = p + 1;                      // 只在右半边继续，数据量减半
        } else {
            right = p - 1;
        }
    }
}
```

**复杂度**：`n + n/2 + n/4 + ... = 2n` = **O(n)**（平均）。最坏 O(n²)（每次分区极不均匀）。

**优缺点**：
- ✅ 平均 O(n) 比堆的 O(n log K) 快；原地，O(1) 额外空间
- ❌ 最坏 O(n²)；**需要全部数据在内存**（不能流式）；会修改原数组

**选型建议**：

| 场景 | 选谁 |
|---|---|
| 海量数据、内存不足、数据流式到达 | **堆** O(n log K) |
| 数据全在内存、追求平均最快 | **快排分区** O(n) |
| K 很小（如 Top 10） | 堆（log K ≈ 3，接近 O(n)） |
| K 接近 n | 直接排序 |

### 4.4 项目现场一：RRF 融合取 Top-N

```java
// ai-cs-chat/.../rag/retrieve/MultiQueryMerger.java
public static List<Document> merge(List<List<Document>> results, int topK, int rrfK) {
    Map<String, Double> scoreMap = new LinkedHashMap<>();   // docId -> RRF 累计分
    Map<String, Document> docMap  = new LinkedHashMap<>();   // docId -> 文档（保序去重）

    for (List<Document> list : results) {          // R 路检索结果
        int rank = 1;
        for (Document doc : list) {                // 每路 K 条
            String id = docId(doc);
            // RRF 核心公式：每路贡献 1/(k + rank)，跨路累加
            scoreMap.merge(id, 1.0 / (rrfK + rank), Double::sum);
            docMap.putIfAbsent(id, doc);
            rank++;
        }
    }

    // 按 RRF 总分降序 → 取前 topK
    List<String> sortedIds = new ArrayList<>(scoreMap.keySet());
    sortedIds.sort(Comparator.comparingDouble(scoreMap::get).reversed());

    List<Document> merged = new ArrayList<>();
    for (String id : sortedIds) {
        if (merged.size() >= topK) break;
        Document doc = docMap.get(id);
        doc.getMetadata().put("rrfScore", scoreMap.get(id));
        merged.add(doc);
    }
    return merged;
}
```

**RRF（Reciprocal Rank Fusion，倒数排名融合）公式**：

```
score(d) = Σ  1 / (k + rank_i(d))
           i∈路

其中 rank_i(d) 是文档 d 在第 i 路结果中的排名（从 1 开始）
k 是平滑常数，通常取 60
```

**为什么用 RRF 而不是直接加权求和分数？**

| 方案 | 问题 |
|---|---|
| 直接加总分 | 各路检索的**分数尺度不同**（BM25 是 0-30，向量余弦是 0-1），无法直接相加，需要归一化调参 |
| **RRF** | **只用排名，不用分数** → 天然免疫尺度差异，无需归一化，零调参 |

**RRF 的直觉**：

```
路 A: [doc1, doc2, doc3]     路 B: [doc2, doc1, doc4]

doc1: 1/(60+1) + 1/(60+2) = 0.01639 + 0.01613 = 0.03252
doc2: 1/(60+2) + 1/(60+1) = 0.01613 + 0.01639 = 0.03252
doc3: 1/(60+3)            = 0.01587
doc4:                       1/(60+3) = 0.01587

→ 在【多路都靠前】的文档得分最高 → "共识优先"
```

**k 的作用**：k 越大，排名差异的影响越小（曲线越平滑）。
- k=0：极端加权，第 1 名贡献 1，第 2 名贡献 0.5
- k=60（业界默认）：第 1 名 0.0164，第 100 名 0.0063，差距温和

**本项目为什么用全排序而非堆**：
R=3 路 × K=20 条 = D ≤ 60 个文档。全排序 `O(D log D)` ≈ 60 × 6 = 360 次比较，
而堆是 `O(D log topK)` ≈ 60 × 4 = 240 次——差距微乎其微，但全排序代码简单得多。
**又一次"先量化再决策"**：数据量小时，简单实现 > 理论最优。

### 4.5 项目现场二：Redis ZSET 热度榜

```java
// ai-cs-chat/.../cache/HotQaCacheService.java
// 记录问题热度：ZINCRBY 累加频次
redisTemplate.opsForZSet().incrementScore(HOT_KEY, question, 1.0);

// 取 Top-N 热门问题：ZREVRANGE 按 score 降序取前 N
Set<ZSetOperations.TypedTuple<String>> tuples =
    redisTemplate.opsForZSet().reverseRangeWithScores(HOT_KEY, 0, topN - 1);
```

**为什么不用 Java 堆而要放到 Redis ZSET？**

| 维度 | Java PriorityQueue | Redis ZSET |
|---|---|---|
| 分布式 | ❌ 单机内存，多实例各自一份 | ✅ 全局共享 |
| 持久化 | ❌ 重启丢失 | ✅ AOF/RDB |
| 更新 | O(log n) | O(log n)（跳表） |
| 取 Top-N | O(N log N)（要复制出来排） | **O(log n + N)**（跳表直接顺序扫描） |

**ZSET 底层：跳表（Skip List）+ 哈希表**

```
跳表（多层有序链表）：

L3:  1 ──────────────────────▶ 9
     │                         │
L2:  1 ─────────▶ 5 ─────────▶ 9
     │           │             │
L1:  1 ──▶ 3 ──▶ 5 ──▶ 7 ──▶ 9
     │     │     │     │     │
L0:  1 ▶ 2 ▶ 3 ▶ 4 ▶ 5 ▶ 6 ▶ 7 ▶ 8 ▶ 9    ← 完整数据，双向链表

查找 7：从 L3 的 1 开始 → 右边是 9 > 7，下降到 L2 → 5 < 7 继续 →
        9 > 7 下降到 L1 → 5 → 7 找到
        只比较了 4 次（L0 顺序查找要 7 次）
```

| 操作 | 跳表复杂度 |
|---|---|
| 查找 | **O(log n)**（期望） |
| 插入 | **O(log n)** |
| 删除 | **O(log n)** |
| **范围查询**（Top-N） | **O(log n + m)** ⭐ 优势 |

**跳表 vs 红黑树**：
- 两者各项操作都是 O(log n)
- 跳表优势：**范围查询更高效**（L0 层是链表，找到起点后顺序扫描即可；红黑树需中序遍历）；实现简单；易于并发优化
- Redis 选跳表正是看重范围查询（`ZRANGEBYSCORE`、`ZREVRANGE`）

**为什么 Redis 不用堆做 Top-N？**
堆只能 O(1) 拿堆顶，要拿前 N 个必须**破坏性地弹出 N 次**（会修改结构）。跳表可以直接顺序扫描 N 个元素，不破坏结构、还支持任意区间查询。

---

## 五、面试高频问答

**Q1：TopK 用大顶堆还是小顶堆？**
A：求**最大的 K 个**用**小顶堆**。堆顶是当前 Top-K 中最小的那个（门槛），新元素比它大就替换它。反之求最小 K 个用大顶堆。

**Q2：为什么堆的 TopK 是 O(n log K) 而不是 O(n log n)？**
A：堆的大小始终 ≤ K，每次 siftUp/siftDown 的代价是 O(log K) 而非 O(log n)。

**Q3：建堆为什么能做到 O(n)？**
A：Floyd 建堆法从最后一个非叶子节点倒着 siftDown。底层节点数多但下沉距离为 0，顶层节点数少但下沉距离大，加权求和 `Σ(n/2^(k+1))×k` 收敛到 O(n)。

**Q4：PriorityQueue 的 remove(Object) 复杂度是多少？**
A：**O(n)**。因为堆不维护"元素 → 下标"的索引，必须线性扫描找到位置，再做 O(log n) 的删除。需要频繁删除任意元素时应自建索引映射。

**Q5：什么场景用堆，什么场景用快排分区求 TopK？**
A：海量/流式数据、内存受限、K 很小时用**堆**（可流式处理，O(K) 内存）；数据全在内存且追求平均最快时用**快排分区**（平均 O(n)，但最坏 O(n²) 且会修改原数组）。

**Q6：Redis ZSET 为什么用跳表而不是红黑树？**
A：两者单次操作都是 O(log n)，但跳表的**范围查询**更直接（底层链表顺序扫描，O(log n + m)），实现更简单，且便于做并发优化。ZSET 大量使用 `ZRANGEBYSCORE` 这类范围操作，跳表更契合。

**Q7：RRF 融合为什么用排名而不是分数？**
A：不同检索路（BM25 / 向量）的分数尺度不可比，直接相加需要归一化调参。RRF 只用名次，天然免疫尺度差异，无需调参（除平滑常数 k 外），且"多路都靠前"的文档自然获得高分。

---

## 六、动手练习

1. 用**小顶堆**求数组中最小的 K 个数（提示：改用大顶堆，即 `new PriorityQueue<>(Comparator.reverseOrder())`）。
2. 把 `MultiQueryMerger` 的 `sortedIds.sort(...)` 改成用 `PriorityQueue` 取 Top-K，对比在 `R=3,K=20` 与 `R=10,K=1000` 两种规模下的操作次数差异。
3. 证明 Floyd 建堆的复杂度：写出 `Σ(k=0 to log n) (n / 2^(k+1)) × k` 并说明它收敛到 O(n)。（提示：令 S = Σ k/2^k = 2）
4. 思考：本项目 `HotQaCacheService` 的热度榜若需要"按时间窗口统计（如近 7 天热榜）"，ZSET 该怎么做？（提示：按天分 key，如 `hot:2026-09-03`，用 `ZUNIONSTORE` 合并 7 天）

---

> 上一篇：[03-哈希](./03-哈希-冲突扰动与分片路由.md) ｜ 下一篇：[05-图论与搜索](./05-图论与搜索-BFS-DFS-拓扑排序.md)
