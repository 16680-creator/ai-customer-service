# 图论与搜索：BFS · DFS · 拓扑排序

> 对应项目：`ai-cs-product`（商品分类树、SKU 层级）、`ai-cs-order`（订单状态机的合法迁移路径）、
> `ai-cs-chat/.../agent`（Agent 工具调用的依赖编排）、
> `deploy/`（微服务启动依赖顺序）。

---

## 一、图的两种存储方式

```
      A ─── B
      │  ╲  │
      │   ╲ │
      C ─── D
   （无向图示例）
```

### 1.1 邻接矩阵 vs 邻接表

| 维度 | 邻接矩阵 `matrix[i][j]` | 邻接表 `List<Integer>[] adj` |
|---|---|---|
| 空间 | **O(V²)** | **O(V + E)** |
| 判断 i→j 是否有边 | **O(1)** ⭐ | O(degree(i)) |
| 遍历 i 的所有邻居 | O(V)（扫一整行） | **O(degree(i))** ⭐ |
| 适用 | 稠密图（E ≈ V²）、需频繁判边 | **稀疏图（E << V²）、需遍历邻居** |

```java
// 邻接表（最常用，尤其稀疏图）
List<Integer>[] adj = new ArrayList[n];
for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
adj[0].add(1);   // 边 0→1
adj[1].add(0);   // 无向图要加两次

// 带权图
List<int[]>[] adj = new ArrayList[n];
adj[0].add(new int[]{1, 5});   // 0→1，权重 5
```

**选型直觉**：真实世界的数据（社交网络、商品关系、服务依赖）几乎都是**稀疏图**（一个人的好友数远小于总用户数），所以**默认用邻接表**。

> 本项目的微服务调用关系：12 个服务，调用边约 20 条。
> 用邻接矩阵需要 144 个格子存 20 条边（浪费 86%）；邻接表存 20 条边即可。

---

## 二、BFS（广度优先搜索）—— 用队列

**核心思想**：一层一层向外扩散。先访问距离起点为 1 的，再访问距离为 2 的……

```
        A          队列：[A]
       / \         访问 A，把 B、C 入队
      B   C        队列：[B, C]
     /     \       访问 B，把 D 入队
    D       E      队列：[C, D]
                   访问 C，把 E 入队
                   队列：[D, E] ...
```

**关键性质**：BFS 在**无权图**中保证找到的路径是**最短路径**（边数最少）。

```java
public int bfsShortestPath(List<Integer>[] adj, int start, int end) {
    int n = adj.length;
    boolean[] visited = new boolean[n];
    int[] dist = new int[n];
    Arrays.fill(dist, -1);

    Queue<Integer> queue = new ArrayDeque<>();
    visited[start] = true;
    dist[start] = 0;
    queue.offer(start);

    while (!queue.isEmpty()) {
        int cur = queue.poll();
        if (cur == end) return dist[cur];          // 首次到达即最短
        for (int next : adj[cur]) {
            if (!visited[next]) {
                visited[next] = true;
                dist[next] = dist[cur] + 1;        // 距离 = 上一层 + 1
                queue.offer(next);
            }
        }
    }
    return -1;   // 不可达
}
```

**复杂度**：**O(V + E)**（每个顶点入队一次，每条边访问一次）。空间 O(V)。

**必须 `visited`**：图可能有环，不标记会无限循环。

**BFS 典型应用**：
- 无权图最短路径
- 层级遍历（二叉树层序遍历）
- 求连通块数量（岛屿数量 LeetCode 200）
- 双向 BFS（起点终点都已知时，两头同时扩散，把 O(b^d) 降到 O(b^(d/2))）

---

## 三、DFS（深度优先搜索）—— 用栈或递归

**核心思想**：一条路走到底，走不通就回溯。

```java
// 递归版（最简洁，递归栈即调用栈）
public void dfs(List<Integer>[] adj, int cur, boolean[] visited) {
    visited[cur] = true;
    System.out.println("访问 " + cur);
    for (int next : adj[cur]) {
        if (!visited[next]) {
            dfs(adj, next, visited);      // 一路深入
        }
    }
    // 隐式回溯：函数返回 = 回到上一层
}

// 显式栈版（避免递归深度过大导致 StackOverflowError）
public void dfsIterative(List<Integer>[] adj, int start) {
    boolean[] visited = new boolean[adj.length];
    Deque<Integer> stack = new ArrayDeque<>();
    stack.push(start);
    visited[start] = true;
    while (!stack.isEmpty()) {
        int cur = stack.pop();
        System.out.println("访问 " + cur);
        for (int next : adj[cur]) {
            if (!visited[next]) {
                visited[next] = true;    // ⚠️ 入栈时就标记，避免重复入栈
                stack.push(next);
            }
        }
    }
}
```

**复杂度**：**O(V + E)**，空间 O(V)（visited + 栈/递归深度）。

> ⚠️ 深图（如长链表状图）用递归可能 `StackOverflowError`。Java 默认栈深度约 1000-10000。
> 生产环境处理不可控图结构时，用**显式栈**更安全。

### 3.1 DFS vs BFS 对比

| 维度 | BFS | DFS |
|---|---|---|
| 数据结构 | **队列** | **栈**（或递归） |
| 搜索方式 | 层层扩散 | 一条路走到底 |
| 最短路径 | ✅ 无权图保证最短 | ❌ 不保证 |
| 空间 | O(V)（队列宽度 = 最宽一层） | O(V)（深度 = 最长路径） |
| 适用 | 最短路、层级、连通性 | 拓扑排序、回溯、环检测、可达性 |

**选型**：
- 求最短/最少步数 → **BFS**
- 求所有可能路径、判断是否有环、依赖排序 → **DFS**

### 3.2 回溯（DFS 的进阶：带状态撤销）

```java
// 模板：全排列
public List<List<Integer>> permute(int[] nums) {
    List<List<Integer>> ans = new ArrayList<>();
    Deque<Integer> path = new ArrayDeque<>();
    boolean[] used = new boolean[nums.length];
    backtrack(nums, path, used, ans);
    return ans;
}

private void backtrack(int[] nums, Deque<Integer> path, boolean[] used, List<List<Integer>> ans) {
    if (path.size() == nums.length) {          // 1. 终止条件
        ans.add(new ArrayList<>(path));        //    必须拷贝！path 会被后续修改
        return;
    }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;                 // 2. 剪枝：跳过不合法选择
        path.addLast(nums[i]);                 // 3. 做选择
        used[i] = true;
        backtrack(nums, path, used, ans);      // 4. 递归
        path.removeLast();                     // 5. 撤销选择（回溯的精髓）
        used[i] = false;
    }
}
```

**回溯三要素**：① 路径（已做的选择）② 选择列表（当前可做的选择）③ 终止条件。

**复杂度**：全排列 O(n! × n)，子集 O(2^n × n)。**指数级**，需靠剪枝优化。

---

## 四、拓扑排序（依赖顺序，工程常用）

**问题**：给定一组依赖关系（A 必须在 B 之前），求一个满足所有依赖的执行顺序。

**前提**：图必须是 **DAG（有向无环图）**。有环则无解（互相依赖，死锁）。

### 4.1 Kahn 算法（BFS，基于入度，推荐）

**思想**：不断取出**入度为 0** 的节点（没有前置依赖的），移除它并减少其后继的入度。

```java
public List<Integer> topoSort(int n, int[][] edges) {
    // 建图 + 统计入度
    List<Integer>[] adj = new ArrayList[n];
    int[] indegree = new int[n];
    for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
    for (int[] e : edges) {
        adj[e[0]].add(e[1]);        // e[0] → e[1]，e[0] 必须先于 e[1]
        indegree[e[1]]++;
    }

    // 入度为 0 的入队（可以最先执行）
    Queue<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < n; i++) {
        if (indegree[i] == 0) queue.offer(i);
    }

    List<Integer> order = new ArrayList<>();
    while (!queue.isEmpty()) {
        int cur = queue.poll();
        order.add(cur);                      // 输出到结果序列
        for (int next : adj[cur]) {
            if (--indegree[next] == 0) {     // 前置依赖全部完成
                queue.offer(next);
            }
        }
    }

    // 结果长度 < n → 有环，无解
    return order.size() == n ? order : List.of();
}
```

**复杂度**：O(V + E)。

**如何检测环**：结果序列长度 < 顶点数 → 存在环（环上的节点入度永远降不到 0）。这是 Kahn 算法自带的环检测。

### 4.2 DFS 逆后序（另一种实现）

**思想**：DFS 后序遍历，把节点压入栈；最后**逆序弹出**即为拓扑序。

```java
private void dfs(int cur, List<Integer>[] adj, boolean[] visited, Deque<Integer> stack) {
    visited[cur] = true;
    for (int next : adj[cur]) {
        if (!visited[next]) dfs(next, adj, visited, stack);
    }
    stack.push(cur);      // 后序：所有后继都处理完，才压入自己
}
// 全部 DFS 完后，依次 pop 即为拓扑序
```

**为什么逆后序有效**：DFS 保证"后继节点先被压栈"，弹出时就是"前驱先出"。

### 4.3 项目关联：微服务启动依赖

```
deploy/docker-compose.yml 的 depends_on 关系（简化）：

    mysql ──┬──▶ user-service ──┐
            ├──▶ order-service ─┼──▶ gateway ──▶ frontend
    redis ──┤                   │
            └──▶ chat-service ──┘
    nacos ─────────────────────┘

拓扑序：mysql, redis, nacos → user, order, chat → gateway → frontend
```

**工程价值**：
- 手写 `init.sql` 执行顺序、K8s `initContainer` 编排、Maven 多模块编译顺序，本质都是拓扑排序
- 如果配置出现循环依赖（A depends_on B，B depends_on A），拓扑排序能**立即发现并报错**，而不是运行时随机失败

> **面试点**：如何检测循环依赖？
> A：拓扑排序（Kahn）——结果数量少于节点数即有环。或 DFS 三色标记法（白=未访问、灰=访问中、黑=已完成；遇到灰色节点即有环）。

---

## 五、并查集（Union-Find，处理连通性神器）

**解决问题**：动态连通性——快速判断两个元素是否属于同一集合，快速合并两个集合。

### 5.1 核心优化（两个，缺一不可）

```java
public class UnionFind {
    private final int[] parent;   // parent[i] = i 的父节点
    private final int[] rank;     // 秩（树高的上界）

    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;   // 初始各自为政
    }

    // 查找（带路径压缩）
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);   // ⭐ 路径压缩：把路径上所有节点直接挂到根
        }
        return parent[x];
    }

    // 合并（按秩合并）
    public void union(int x, int y) {
        int rootX = find(x), rootY = find(y);
        if (rootX == rootY) return;                  // 已连通
        // ⭐ 按秩合并：小树挂到大树下，避免树退化成链表
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
    }

    public boolean connected(int x, int y) { return find(x) == find(y); }
}
```

### 5.2 复杂度：反阿克曼函数

| 优化 | find/union 复杂度 |
|---|---|
| 无优化 | O(n)（退化成链表） |
| 只按秩合并 | O(log n) |
| 只路径压缩 | O(log n) |
| **两者都有** | **O(α(n))** ⭐ |

`α(n)` 是**反阿克曼函数**，实际应用中 **α(n) ≤ 5**（即使 n 是宇宙原子总数 10^80，α(n) 也才 4）。
**工程上视为 O(1)**。

### 5.3 两个优化的原理

**路径压缩**：

```
查找 4 之前：        查找 4 之后（路径压缩）：
      1                   1
     /                  / | \
    2                  2  3  4     ← 路径上所有节点直接挂到根
   /                            
  3                            
 /                            
4                            
```

**按秩合并**：始终让"矮树"挂到"高树"下，避免树高增长。

### 5.4 应用场景

| 场景 | 说明 |
|---|---|
| 连通块计数 | LeetCode 200（岛屿数量）、547（省份数量）|
| 最小生成树 Kruskal | 每次选最小边，用并查集判断是否形成环 |
| 动态连通性 | 社交网络好友关系、网络连接状态 |
| 等式方程可满足性 | LeetCode 990 |

---

## 六、Dijkstra 最短路径（带权非负图）

**适用**：边权**非负**的带权图，求单源最短路径。

**核心**：贪心 + 优先队列。每次取出当前距离最小的未确定节点，用它松弛邻居。

```java
public int[] dijkstra(List<int[]>[] adj, int start) {
    int n = adj.length;
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[start] = 0;

    // 优先队列：按距离排序，{节点, 距离}
    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
    pq.offer(new int[]{start, 0});

    while (!pq.isEmpty()) {
        int[] cur = pq.poll();
        int u = cur[0], d = cur[1];
        if (d > dist[u]) continue;           // 已找到更短路径，跳过（懒删除）

        for (int[] edge : adj[u]) {
            int v = edge[0], w = edge[1];
            if (dist[u] + w < dist[v]) {     // 松弛：能否通过 u 让 v 更近
                dist[v] = dist[u] + w;
                pq.offer(new int[]{v, dist[v]});
            }
        }
    }
    return dist;
}
```

**复杂度**：O((V + E) log V)（每个节点入队，每条边松弛一次，堆操作 O(log V)）。

**⚠️ 限制**：**不能处理负权边**。因为 Dijkstra 基于贪心——一旦某节点被取出就认为距离已确定，负权边可能让它之后变得更短。负权图用 **Bellman-Ford**（O(VE)，可检测负环）。

### 对比四种最短路算法

| 算法 | 适用 | 复杂度 | 负权 |
|---|---|---|---|
| **BFS** | 无权图 | O(V+E) | — |
| **Dijkstra** | 非负权 | O((V+E) log V) | ❌ |
| Bellman-Ford | 任意 | O(VE) | ✅ 可检测负环 |
| SPFA | 任意 | 平均 O(E)，最坏 O(VE) | ✅ |
| Floyd | 全源最短路 | O(V³) | ✅ |

---

## 七、项目关联：树形结构的处理

本项目的商品分类、组织架构、菜单都是**树**，常用操作：

### 7.1 分类树递归构建

```java
// 扁平列表 → 树形结构（O(n) 哈希法，优于 O(n²) 递归查找）
public List<CategoryNode> buildTree(List<Category> list) {
    Map<Long, CategoryNode> map = new HashMap<>();
    List<CategoryNode> roots = new ArrayList<>();

    // 第一遍：所有节点入 Map（O(n)）
    for (Category c : list) {
        map.put(c.getId(), new CategoryNode(c));
    }
    // 第二遍：挂父子关系（O(n)）
    for (Category c : list) {
        CategoryNode node = map.get(c.getId());
        CategoryNode parent = map.get(c.getParentId());
        if (parent == null) {
            roots.add(node);              // 无父节点 → 根节点
        } else {
            parent.getChildren().add(node);
        }
    }
    return roots;   // 总计 O(n)
}
```

**对比 O(n²) 写法**：对每个节点遍历全表找子节点 → n × n = O(n²)。
**哈希法**两次 O(n) 遍历 → **O(n)**。数据量 1000 条时，100 万次 → 2000 次，快 500 倍。

### 7.2 递归遍历的风险

```java
// ⚠️ 风险：分类层级很深（或被脏数据搞成环）→ StackOverflowError
private void traverse(CategoryNode node) {
    process(node);
    for (CategoryNode child : node.getChildren()) traverse(child);   // 递归
}

// ✅ 安全：显式栈 BFS/DFS
private void traverseSafe(CategoryNode root) {
    Deque<CategoryNode> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        CategoryNode cur = stack.pop();
        process(cur);
        for (CategoryNode child : cur.getChildren()) stack.push(child);
    }
}
```

> **工程建议**：树结构来自数据库时，深度不可控（脏数据可能形成环），生产中加**深度上限**或改用显式栈。

---

## 八、面试高频问答

**Q1：BFS 和 DFS 各用什么数据结构？复杂度多少？**
A：BFS 用队列，DFS 用栈（或递归）。两者都是 O(V + E)，空间 O(V)。BFS 在无权图中保证最短路径，DFS 不保证。

**Q2：拓扑排序的前提是什么？如何检测环？**
A：前提是有向无环图（DAG）。Kahn 算法中，若结果序列长度 < 顶点数则有环（环上节点入度永不降为 0）。也可用 DFS 三色标记法（遇到灰色/访问中节点即有环）。

**Q3：并查集为什么不直接用数组存集合编号？**
A：合并两个集合时，需要把其中一个集合的所有元素编号改掉，O(n)。并查集用树结构，合并只需改一个父指针，配合路径压缩 + 按秩合并，复杂度 O(α(n)) ≈ O(1)。

**Q4：路径压缩和按秩合并各自作用？**
A：路径压缩让 `find` 变快（把路径上所有节点直接挂到根）；按秩合并让 `union` 时树不退化（矮树挂高树）。两者结合才达到 O(α(n))。

**Q5：Dijkstra 为什么不能处理负权边？**
A：Dijkstra 是贪心——节点第一次出堆时就认定距离已最短、不再更新。若存在负权边，后续路径可能让它变得更短，导致结果错误。负权图用 Bellman-Ford。

**Q6：邻接矩阵和邻接表怎么选？**
A：绝大多数用邻接表（O(V+E) 空间，遍历邻居 O(degree)）。邻接矩阵只在稠密图或需要 O(1) 判断两点是否有边时使用，空间 O(V²) 太大。

**Q7：递归 DFS 有什么风险？如何规避？**
A：深度过大导致 `StackOverflowError`（Java 默认栈深度约 1000-10000 层）。规避：改用显式栈迭代；或加深度上限；数据来自外部时尤其要防脏数据形成环。

---

## 九、动手练习

1. 用 Kahn 算法对 [04-树堆与TopK](./04-树堆与TopK.md) 中"本项目算法现场"的依赖表做拓扑排序，给出一个合理的学习顺序。
2. 用并查集实现"商品标签分组"：给定 N 个商品和若干"同组"关系，统计最终有几个分组。
3. 把 §7.1 的"扁平列表转树"改成递归写法，对比两者的时间和空间复杂度。
4. 思考：本项目订单状态机（`ai-cs-order`）的状态迁移能否用拓扑排序检测"是否存在不可达状态"？（提示：把状态当节点、合法迁移当边，从"已创建"做 BFS，未被访问到的状态即不可达）

---

> 上一篇：[04-树·堆与 TopK](./04-树堆与TopK.md) ｜ 下一篇：[06-排序与二分查找](./06-排序与二分查找.md)
