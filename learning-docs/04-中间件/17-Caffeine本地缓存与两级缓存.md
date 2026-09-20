# 17-Caffeine 本地缓存与两级缓存：从零开始理解"最快的缓存在你进程里"（认知拓展）

> **定位**：本仓缓存现状——product/order 用 **Spring Cache + RedisCacheManager**（[02-Spring微服务/09](../02-Spring微服务/09-SpringCache与事务领域事件.md)），chat 的热门问答/语义缓存是 **Redis ZSET+Hash / 向量比对**（`HotQaCacheService`/`SemanticCacheService`），[11-数据结构与算法/09](../11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存.md) 手写过 LRU/LFU。**Caffeine 是 Java 进程内缓存的事实标准**——把它学会，"两级缓存"就从算法题变成工程方案。本篇为认知拓展 + 最小落地路径评估。
> 前置阅读：[11-数据结构与算法/09-缓存淘汰](../11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存.md)（必读，LRU/LFU 手写版）、[02-Spring微服务/09](../02-Spring微服务/09-SpringCache与事务领域事件.md)、[12-性能工程/04-内存泄漏排查](../12-性能工程/04-内存泄漏排查实战.md)（本地缓存的 OOM 风险）。

---

## ⚡ 30 秒速记卡

```text
① 本地缓存快在"零网络 RTT、零序列化"——但每台机器各一份，天然不一致 + 占堆内存
② Caffeine 的灵魂是 W-TinyLFU：LFU 解决"怀旧问题"，再配滑动窗口解决"老登占坑"
③ 四个旋钮：maximumSize(容量) / expireAfterWrite(寿命) / refreshAfterWrite(异步刷新) / weigher(按权重算)
④ refreshAfterWrite 只在"过期后第一次读"触发异步刷新并先回旧值——与 expireAfterWrite 配合才成立
⑤ 两级缓存 = Caffeine(L1,纳秒) + Redis(L2,毫秒) + 广播失效(MQ/发布订阅)；本项目失效广播可直接复用 RocketMQ
```

---

## 一、为什么要本地缓存：算一笔延迟账

| 路径 | 量级 |
|---|---|
| Caffeine 命中（堆内 HashMap 级） | **~100ns** |
| Redis 命中（内网 RTT + 序列化） | ~1ms |
| MySQL 主键查 | ~5~10ms |
| 你的 RAG 全链路（Embedding→检索→LLM） | **秒级** |

对"分类树、商品详情"这类热读，Redis 已经够快；但对"**每请求都要读、读放大严重**"的元数据（如你的 `EmbeddingMath` 归一化词表、模型路由配置快照），本地缓存收益巨大。同时要清醒：**它是空间换时间 + 一致性换速度**——每实例各一份副本，改了配置不会立即全局生效。

---

## 二、W-TinyLFU：比 LRU 聪明在哪（先复习你的手写版）

你手写的 LRU（[11-算法/09](../11-数据结构与算法/09-缓存淘汰-LRU-LFU与两级缓存.md)）有两个天然缺陷：

1. **偶发批量扫描污染**：一次全表导出把热键全部挤出（LRU 只看"最近"不看"频率"）；
2. **纯 LFU 的"怀旧问题"**：昨天的爆款键计数高，今天的热点挤不进来。

Caffeine 的 W-TinyLFU 三步走（讲人话版）：

```text
① 新键先进"窗口区"（小队列，给新热点一个试用机会，防一棒子打死）
② 淘汰候选者之间比"频率"：频率存在 Count-Min Sketch（小型频数草图，内存省、允许少量高估）
③ 频率随时间衰减（半衰期机制）：老的计数定期减半 → "老登"权重下降，新热点能上位
```

> 一句话记忆：**LRU 看"最近"，LFU 看"历史"，W-TinyLFU 看"最近的频率"，还带遗忘曲线。**

---

## 三、核心 API：四个旋钮 + 三种缓存类型

```java
Cache<String, ProductVO> cache = Caffeine.newBuilder()
        .maximumWeight(64 * 1024 * 1024)              // 按权重限容量（下面定义 1 字符=1）
        .weigher((String k, ProductVO v) -> v.estimateSize())
        .expireAfterWrite(Duration.ofMinutes(5))      // 写后 5 分钟强制过期（防脏数据无限活）
        .refreshAfterWrite(Duration.ofMinutes(1))     // 1 分钟后读→先回旧值，异步重新加载
        .recordStats()                                // 开命中率统计（getStats()）
        .build(key -> productMapper.selectById(Long.valueOf(key)));  // LoadingCache：miss 自动回源

ProductVO v = cache.getIfPresent("1001");             // 纯读，不回源
ProductVO v2 = cache.get("1001", k -> load(k));       // 不命中则加载（自带防击穿：同 key 并发只加载一次）
cache.invalidate("1001");                             // 主动失效（改库后调用）
```

| 旋钮 | 作用 | 坑 |
|---|---|---|
| `maximumSize` / `maximumWeight` | 条数上限 / 权重上限（大对象必须用权重） | 存大 JSON 不设 weight → OOM（[12-性能工程/04](../12-性能工程/04-内存泄漏排查实战.md) 的本地缓存章节） |
| `expireAfterWrite` | 距上次写过的寿命 | 与 `refreshAfterWrite` **搭配**才正确：refresh 让旧值多活一会儿，expire 是最终寿命 |
| `expireAfterAccess` | 距上次访问的寿命 | 会话/临时数据用 |
| `refreshAfterWrite` | 到期后**先回旧值、异步刷新** | 只在"过期后被读到"才触发——冷键永远不会刷新（好事，省资源） |
| `recordStats` | 命中率/驱逐数 | 上线必开，暴露 `/actuator` 或日志 |

> **防击穿免配置**：`LoadingCache.get` 对同 key 并发只有一个线程回源，其余等待——这就是 `@Cacheable(sync=true)`（`ProductServiceImpl:109`）想要的语义在本地层的原生实现。

---

## 四、Spring 集成：同一个 `CacheManager`，换一个实现而已

你在 [02-Spring微服务/09](../02-Spring微服务/09-SpringCache与事务领域事件.md) 已经用过 `RedisCacheManager`——**Spring Cache 的抽象此刻开始兑现红利**：换实现不改业务注解。

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("product:detail", "product:categories");
    mgr.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats());
    return mgr;
}
// 业务代码原封不动：@Cacheable(cacheNames = "product:detail", key = "#id")
```

---

## 五、两级缓存完整方案：L1(Caffeine) + L2(Redis) + 失效广播

```text
读：L1 命中 ──► 返回（ns）
    └未命中→ L2(Redis) 命中 ──► 回填 L1 ──► 返回
              └未命中→ DB ──► 回填 L2 + L1

写：改 DB ──► 删 L2 ──► 广播"失效消息"（RocketMQ topic / Redis pub-sub）
              │                        │
              ▼                        ▼
        本机 L1 evict          其他实例收到 → evict 各自 L1
```

**本项目的现成锚点（这套方案几乎是"拼装"而非"新建"）**：

| 组件 | 本仓已有 |
|---|---|
| L2 + 失效广播通道 | RocketMQ（02/08/12 三篇）或 Redis pub-sub |
| 需要两级化的真实候选 | chat 缓存族：`HotQaCacheService`（Redis ZSET+Hash 精确命中）/`SemanticCacheService`（向量比对）——**每次对话都查**，加 L1 可省一次 Redis RTT |
| 一致性兜底 | 短 TTL（本地 5~30s）+ 上面广播；RAG 缓存本质是"可容忍秒级旧答案"，天然适合 |
| Spring 抽象 | `CacheManager` 换实现（本文第四节） |

> **诚实评估**：product 详情这类"低频写"上两级收益中等（Redis 已 1ms）；**chat 对话热路径**上每次省 1ms×多次查询 + 削 Redis 压力，才是本仓最值得两级化的位置——但注意 L1 里别放大对象（回答 JSON 通常几 KB，可接受）。

---

## 六、动手实验（10 分钟）

```xml
<dependency>
  <groupId>com.github.ben-manes.caffeine</groupId>
  <artifactId>caffeine</artifactId>
</dependency>  <!-- 版本随 Boot BOM -->
```

```java
var cache = Caffeine.newBuilder().maximumSize(3).recordStats().build<String,String>();
cache.put("a","1"); cache.put("b","2"); cache.put("c","3");
cache.getIfPresent("a");                 // a 变热
cache.put("d","4");                      // 容量超限 → 驱逐的不是 a（W-TinyLFU 保护热点），验证一下是 b 或 c
System.out.println(cache.stats());       // hitRate / evictionCount
```

---

## 七、面试要点总结

```text
关键词：
本地缓存=零RTT零序列化 vs 一致性/堆内存代价 · W-TinyLFU=窗口区+Count-Min Sketch+频率衰减
四旋钮 maximumSize/weigher/expireAfterWrite/refreshAfterWrite · refresh 先回旧值异步刷新（冷键不刷）
LoadingCache 同 key 并发单线程回源（防击穿）· Spring CacheManager 换实现零改注解
两级缓存=L1+L2+失效广播 · 大对象必须 weigher，防 OOM
项目锚点：ProductCacheConfig(RedisCacheManager) · HotQaCacheService/SemanticCacheService(候选两级化)
         11-算法/09 手写 LRU（面试讲 W-TinyLFU 正好接上）
```

## 学习检查清单

- [ ] 能说清 LRU 的两个缺陷与 W-TinyLFU 的三步修正
- [ ] 能解释 refreshAfterWrite 为什么必须配 expireAfterWrite、为什么冷键不刷新
- [ ] 能用 Spring CacheManager 把 Redis 缓存平滑换成/叠加 Caffeine
- [ ] 能画出两级缓存的读写路径与失效广播
- [ ] 能指出本仓最值得两级化的位置并给出理由

## 下一步

- [18-gRPC 与 Protobuf](18-gRPC与Protobuf跨语言调用.md)：当缓存都救不了延迟时，压缩"传输"这一环；
- [02-Spring微服务/09-SpringCache与事务领域事件](../02-Spring微服务/09-SpringCache与事务领域事件.md)：回看 L2 层的现有实现。
