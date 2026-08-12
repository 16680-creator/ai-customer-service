# 混合检索实战：ES BM25 与向量 RRF 融合

> 本文档对应 `ai-cs-search` 模块的 `hybrid` 包，讲解**双路召回 + RRF 融合**的混合检索。
> 前置知识：[03-RAG向量检索实战.md](03-RAG向量检索实战.md)（向量检索）、[04-RAG进阶实战-Rerank重排序.md](04-RAG进阶实战-Rerank重排序.md)。
>
> **核心目标**：让"精确关键词查询"和"模糊语义查询"都能被检索命中——
> ES BM25 管字面匹配，Chroma 向量管语义相似，最后用 RRF 算法把两路排名融合成一份结果。

---

## 一、实战背景：为什么需要混合检索？

**单一检索方式的盲区**：

| 场景 | 向量检索（语义） | BM25 关键词检索（字面） |
|------|-----------------|----------------------|
| "15天无理由退货" vs 知识库写"支持无理由退换" | ✅ 语义相似能命中 | ❌ 字面不重叠，可能漏 |
| 精确型号/编号 "AICS-X200" | ❌ 语义相近的噪声多，精确串反而不突出 | ✅ 术语精确命中，分数极高 |
| 人名/订单号/货号等**实体查询** | ❌ 语义编码对实体不敏感 | ✅ 完美匹配 |
| 同义改写（"运费谁出" vs "邮费自理"） | ✅ | ❌ |

**结论**：客服场景两类问题都有——用户既会问"退货麻烦吗"（语义），也会报"订单号 A20260812"（精确）。
单一检索必然顾此失彼，**混合检索**（Hybrid Search）把两路结果合并，取长补短。

```
用户问题
   │
   ├──► ES BM25 关键词检索（multiMatch: title^2 + content）Top-20
   │        精确术语命中（型号/编号/实体）
   │
   ├──► Chroma 向量语义检索（bge-m3 相似度）Top-20
   │        同义改写/语义相关命中
   │
   └──► RRF 倒数排名融合（k=60）──► 输出 Top-N（双路都命中的排最前）
```

---

## 二、BM25 原理简介

BM25（Best Matching 25）是经典的信息检索排序函数，对查询词 \(q_i\) 与文档 \(D\) 打分：

\[
\text{score}(D, Q) = \sum_{i=1}^{n} \text{IDF}(q_i) \cdot
\frac{f(q_i, D) \cdot (k_1 + 1)}{f(q_i, D) + k_1 \cdot (1 - b + b \cdot \frac{|D|}{\text{avgdl}})}
\]

核心直觉（记住结论即可）：

1. **词频（TF）**：词在文档中出现越多越相关，但用 \(k_1\)（默认 1.2）做饱和处理——出现 10 次的增益远小于从 1 次到 2 次；
2. **逆文档频率（IDF）**：词越罕见越有区分度——"退货"比"我们"值钱得多；
3. **文档长度归一化**：参数 \(b\)（默认 0.75）惩罚长文档——同一句话在短文档里说明它更"聚焦"；
4. **字段加权**：标题命中比正文命中更值钱（本项目 `title^2`，标题权重翻倍）。

> ES 内置了完整的 BM25 实现（`match` / `multi_match` 查询默认走 BM25），
> 我们不需要自己实现算法，只需要会用查询 DSL 和 Java Client。

---

## 三、RRF 倒数排名融合原理

RRF（Reciprocal Rank Fusion）不需要归一化不同检索器的分数（向量分数 0~1、BM25 分数可能上千，
直接相加没有意义），而是**只看排名**：

\[
\text{RRFScore}(d) = \sum_{r \in \text{rankings}(d)} \frac{1}{k + r}
\]

- \(r\)：文档在某一路结果中的排名（第 1 名贡献最大）；
- \(k\)：平滑常数（本项目 60），防止排名靠后的结果权重差异过大，也避免除零。

**示例**：某文档在 ES 路排第 2、在向量路排第 5，则 \(\frac{1}{60+2} + \frac{1}{60+5} = 0.0161 + 0.0154 = 0.0315\)；
只在 ES 路排第 1 的文档得 \(\frac{1}{61} = 0.0164\)。**两路都命中的文档分数天然叠加，排在最前**——
这正是我们想要的：既被关键词命中又被语义命中的，最可能是正确答案。

> 对比：加权线性融合（`α·向量分 + β·BM25分`）需要调 α/β，且分数分布不一致时容易失真；
> RRF 只用排名、无需调参、鲁棒性好，是业界混合检索的事实标准。

---

## 四、整体架构与文件清单

模块：`ai-cs-search`（端口 8084），包 `com.aics.search.hybrid`

| 文件 | 职责 |
|------|------|
| `HybridSearchService.java` | 混合检索接口 |
| `HybridSearchServiceImpl.java` | 双路召回 + RRF 融合实现（核心） |
| `RrfMerger.java` | RRF 算法工具类（纯函数，可单测） |
| `RankedItem.java` | 融合中间条目（rank1/rank2/score/标题/内容等） |
| `HybridSearchResult.java` | 对外结果 VO |
| `HybridResultPageVO.java` | 分页 VO |
| `controller/SearchController.java` | `GET /search/hybrid` 接口 |

依赖（`ai-cs-search/pom.xml`）：

```xml
<!-- Elasticsearch Java Client（版本由父 POM BOM 管理 8.12.2，用于关键词路） -->
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
</dependency>

<!-- Chroma 向量库（语义路） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-chroma</artifactId>
</dependency>
```

---

## 五、核心代码实现

### 1. 服务入口：双路召回 + 降级（HybridSearchServiceImpl.hybridSearch）

文件：`ai-cs-search/src/main/java/com/aics/search/hybrid/HybridSearchServiceImpl.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchServiceImpl implements HybridSearchService {

    /** 单路召回条数 */
    private static final int RETRIEVE_TOP_K = 20;

    /** RRF 平滑常数（越大排名靠后的结果权重差异越小） */
    private static final int RRF_K = 60;

    private final ElasticsearchClient esClient;
    private final VectorStore vectorStore;

    @Override
    public List<HybridSearchResult> hybridSearch(String knowledgeBase, String query, int topK) {
        log.info("混合检索开始: knowledgeBase={}, query={}, topK={}", knowledgeBase, query, topK);
        // 双路并行召回（各自内部捕获异常，互不影响）
        List<RankedItem> esItems = esSearch(knowledgeBase, query);
        List<RankedItem> vectorItems = vectorSearch(knowledgeBase, query);

        if (esItems.isEmpty() && vectorItems.isEmpty()) {
            log.warn("混合检索两路均无结果: knowledgeBase={}, query={}", knowledgeBase, query);
            return Collections.emptyList();
        }
        if (esItems.isEmpty()) {
            // 降级：ES 路异常/无结果 → 仅返回向量结果
            return toResults(vectorItems);
        }
        if (vectorItems.isEmpty()) {
            // 降级：向量路异常/无结果 → 仅返回 ES 结果
            return toResults(esItems);
        }

        // 双路都正常 → RRF 融合
        List<RankedItem> merged = RrfMerger.merge(esItems, vectorItems, topK, RRF_K);
        log.info("混合检索完成: knowledgeBase={}, 融合结果={} 条", knowledgeBase, merged.size());
        return toResults(merged);
    }
}
```

### 2. ES 关键词路（BM25 multiMatch）

```java
private List<RankedItem> esSearch(String knowledgeBase, String query) {
    try {
        String indexName = esIndexName(knowledgeBase);   // 索引名 = 知识库标识小写
        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchResponse<Map<String, Object>> response = esClient.search(s -> s
                        .index(indexName)
                        .size(RETRIEVE_TOP_K)
                        .query(q -> q.bool(b -> b
                                // 过滤：只搜当前知识库（term 精确匹配）
                                .filter(f -> f.term(t -> t.field("knowledgeBase").value(knowledgeBase)))
                                // 必须：multiMatch 关键词匹配，标题权重 x2
                                .must(m -> m.multiMatch(mm -> mm
                                        .fields("title^2", "content")
                                        .query(query))))),
                (Class) Map.class);

        List<RankedItem> items = new ArrayList<>();
        int rank = 1;
        for (Hit<Map<String, Object>> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source == null) continue;
            RankedItem item = new RankedItem();
            item.setId(hit.id());                 // ES _id 作为融合唯一键
            item.setRank1(rank++);                // 记录本路排名（RRF 只认排名）
            item.setScore(hit.score() != null ? hit.score() : 0.0);  // BM25 原始分
            item.setTitle(strValue(source.get("title")));
            item.setContent(strValue(source.get("content")));
            item.setKnowledgeBase(knowledgeBase);
            item.setPage(intValue(source.get("page")));
            item.setDocType(strValue(source.get("docType")));
            items.add(item);
        }
        log.info("ES 检索完成: knowledgeBase={}, 命中={}", knowledgeBase, items.size());
        return items;
    } catch (Exception e) {
        // 任何异常都不阻断主流程，返回空列表 → 调用方降级为仅向量检索
        log.warn("ES 检索失败（降级为仅向量检索）: knowledgeBase={}, err={}", knowledgeBase, e.getMessage());
        return Collections.emptyList();
    }
}
```

**查询 DSL 解读**（等价 JSON）：

```json
{
  "size": 20,
  "query": {
    "bool": {
      "filter": [ { "term": { "knowledgeBase": "product-manual" } } ],
      "must": [ { "multi_match": { "query": "退货政策", "fields": ["title^2", "content"] } } ]
    }
  }
}
```

> `filter` 只过滤不影响分数（还能被 ES 缓存），`must` 参与 BM25 打分；
> `title^2` 表示标题字段的匹配得分乘以 2——标题能精准描述内容，命中标题比命中正文更可信。

### 3. 向量语义路（Chroma similaritySearch）

```java
private List<RankedItem> vectorSearch(String knowledgeBase, String query) {
    try {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(RETRIEVE_TOP_K)
                .filterExpression("knowledgeBase == '" + knowledgeBase + "'")
                .build();
        List<Document> docs = vectorStore.similaritySearch(searchRequest);

        List<RankedItem> items = new ArrayList<>();
        int rank = 1;
        for (Document doc : docs) {
            Map<String, Object> metadata = doc.getMetadata();
            RankedItem item = new RankedItem();
            // 融合唯一键：优先 metadata.documentId（入库时写入），保证与 ES 路 _id 对齐
            Object documentId = metadata.get("documentId");
            item.setId(documentId != null ? String.valueOf(documentId) : doc.getId());
            item.setRank2(rank++);
            item.setScore(doc.getScore() != null ? doc.getScore() : 0.0);
            item.setTitle(strValue(metadata.get("title")));
            item.setContent(doc.getText());
            item.setKnowledgeBase(knowledgeBase);
            item.setPage(intValue(metadata.get("page")));
            item.setDocType(strValue(metadata.get("docType")));
            items.add(item);
        }
        log.info("向量检索完成: knowledgeBase={}, 命中={}", knowledgeBase, items.size());
        return items;
    } catch (Exception e) {
        log.warn("向量检索失败（降级为仅 ES 检索）: knowledgeBase={}, err={}", knowledgeBase, e.getMessage());
        return Collections.emptyList();
    }
}
```

> **两路 ID 对齐是关键**：ES 路用 `_id`、向量路用 `metadata.documentId`，
> 只有两条路的 ID 体系一致，RRF 才能正确把"同一文档"的两路排名叠加。

### 4. RRF 融合算法（RrfMerger）

文件：`ai-cs-search/src/main/java/com/aics/search/hybrid/RrfMerger.java`

```java
public final class RrfMerger {

    private RrfMerger() {
    }

    /**
     * 融合两路检索结果
     *
     * @param list1 第一路结果（如 ES 关键词检索），按相关性降序
     * @param list2 第二路结果（如向量相似度检索），按相关性降序
     * @param topK  返回前 topK 条
     * @param k     RRF 平滑常数（推荐 60）
     */
    public static List<RankedItem> merge(List<RankedItem> list1, List<RankedItem> list2, int topK, int k) {
        Map<String, Double> scores = new HashMap<>();       // id → RRF 总分
        Map<String, RankedItem> items = new HashMap<>();    // id → 合并后的条目
        addScores(scores, items, list1, k, true);           // ES 路累加
        addScores(scores, items, list2, k, false);          // 向量路累加
        // 回填融合分数
        items.values().forEach(item -> item.setScore(scores.getOrDefault(item.getId(), 0.0)));
        // 按融合分数降序，取 topK
        return items.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    private static void addScores(Map<String, Double> scores, Map<String, RankedItem> items,
                                  List<RankedItem> list, int k, boolean isFirst) {
        for (int i = 0; i < list.size(); i++) {
            RankedItem item = list.get(i);
            if (item.getId() == null || item.getId().isBlank()) continue;
            int rank = i + 1;
            // RRF 核心公式：1/(k + rank)
            scores.merge(item.getId(), 1.0 / (k + rank), Double::sum);
            RankedItem merged = items.computeIfAbsent(item.getId(), id -> new RankedItem());
            if (isFirst) {
                merged.setRank1(rank);   // 记录 ES 路排名
            } else {
                merged.setRank2(rank);   // 记录向量路排名
            }
            fillMissing(merged, item);   // 补全标题/内容等字段（先到者优先）
        }
    }
}
```

**数值验证**（k=60，两条路的 Top-3 相同文档）：

| 文档 | ES 排名 | 向量排名 | RRF 分 = 1/(60+r1) + 1/(60+r2) | 融合名次 |
|------|---------|----------|-------------------------------|---------|
| D1 | 1 | 2 | 0.01639 + 0.01613 = 0.03252 | 第 1 |
| D2 | 3 | 1 | 0.01587 + 0.01639 = 0.03226 | 第 2 |
| D3 | 2 | 4 | 0.01613 + 0.01563 = 0.03176 | 第 3 |

双路都命中的文档必然压过只单路命中的（单路第 1 名也才 0.0164），这正是 RRF 的威力。

### 5. 对外接口（SearchController）

```java
@Operation(summary = "混合检索（ES 关键词 + 向量语义，RRF 融合）")
@GetMapping("/hybrid")
public Result<HybridResultPageVO> hybridSearch(
        @RequestParam("index") @NotBlank(message = "知识库标识不能为空") String index,
        @RequestParam("query") @NotBlank(message = "搜索关键词不能为空") String query,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "10") int size) {
    int currentPage = Math.max(1, page);
    int pageSize = Math.min(Math.max(1, size), 100);
    // 融合取前 page*size 条，再在内存中分页（RRF 结果条数有限，内存分页足够）
    int topK = currentPage * pageSize;
    List<HybridSearchResult> all = hybridSearchService.hybridSearch(index, query, topK);
    int from = (currentPage - 1) * pageSize;
    List<HybridSearchResult> records = from >= all.size()
            ? List.of()
            : all.subList(from, Math.min(from + pageSize, all.size()));
    HybridResultPageVO vo = new HybridResultPageVO();
    vo.setTotal(all.size());
    vo.setPage(currentPage);
    vo.setSize(pageSize);
    vo.setRecords(records);
    return Result.success(vo);
}
```

---

## 六、降级策略

| 故障场景 | 行为 | 用户感受 |
|----------|------|----------|
| ES 服务挂了 / 查询异常 | 仅返回向量结果（日志：`ES 检索失败（降级为仅向量检索）`） | 结果变少但可用 |
| Chroma 挂了 / 向量异常 | 仅返回 ES 结果（日志：`向量检索失败（降级为仅 ES 检索）`） | 同上 |
| 双路都失败 | 返回空列表（日志：`混合检索两路均无结果`） | 明确告知无结果 |
| 某路无命中（非异常） | 自动降级为另一路结果 | 正常返回另一路 |

> 实现要点：**每一路方法内部 `try/catch` 吞掉自己的异常并返回空列表**，
> 调用方只判断"空/非空"，无需感知异常细节——故障隔离在单路内部完成。

---

## 七、配置说明

### 1. ES 连接（`ai-cs-search` 配置）

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200          # ES 地址（Nacos aics-shared 或环境变量）
    # username: elastic                  # 开启安全认证时配置
    # password: ${ES_PASSWORD}
```

### 2. Chroma 连接（与 ai-cs-chat 共用 aics-knowledge 集合）

```yaml
spring:
  ai:
    vectorstore:
      chroma:
        initialize-schema: true
        collection-name: aics-knowledge
        client:
          host: http://localhost
          port: 8000
```

### 3. 索引与文档写入（ES 侧需先建索引、同步数据）

```bash
# 创建索引（mappings 含 title/content/knowledgeBase/page/docType 字段）
curl -X POST "http://localhost:8084/search/index/product-manual" \
  -H "Content-Type: application/json" \
  -d '{"properties":{"title":{"type":"text"},"content":{"type":"text"},"knowledgeBase":{"type":"keyword"},"page":{"type":"integer"},"docType":{"type":"keyword"}}}'

# 写入文档（_id 建议用 documentId，与 Chroma 路对齐）
curl -X POST "http://localhost:8084/search/document/product-manual" \
  -H "Content-Type: application/json" \
  -d '{"_id":"1001","title":"退货政策","content":"我们支持15天无理由退货，运费由买家承担。","knowledgeBase":"product-manual","docType":"txt"}'
```

### 4. 调参建议

| 参数 | 位置 | 建议 |
|------|------|------|
| `RETRIEVE_TOP_K`（单路召回数） | 代码常量 | 20 起步；知识库大可到 50 |
| `RRF_K`（平滑常数） | 代码常量 | 60 是标准值；希望"双路命中"权重更突出可调小（如 30） |
| `title^2` 字段权重 | ES 查询 | 标题质量高可提到 `title^3` |
| `size`（每页条数） | 接口参数 | 上限 100，内存分页足够 |

---

## 八、验证方法

```bash
# 1. 前置：ES(9200) + Chroma(8000) + ai-cs-search(8084) 均已启动
#    ES 已建 product-manual 索引并写入文档；Chroma 已入库对应向量

# 2. 语义类查询（验证向量路）：
curl "http://localhost:8084/search/hybrid?index=product-manual&query=运费谁出&size=5"
#    日志出现：向量检索完成: knowledgeBase=product-manual, 命中=...

# 3. 精确类查询（验证 ES 路，如型号/编号）：
curl "http://localhost:8084/search/hybrid?index=product-manual&query=AICS-X200&size=5"
#    日志出现：ES 检索完成: knowledgeBase=product-manual, 命中=...

# 4. 观察融合日志与结果字段：
#    混合检索完成: knowledgeBase=product-manual, 融合结果=5 条
#    响应中每条记录带 esRank / vectorRank：0 表示该路未命中，非 0 表示该路排名

# 5. 降级演练：停掉 ES 后再次调用 → 仍返回向量结果，日志出现降级提示
```

---

## 九、常见问题

**Q1：混合检索和 Rerank 是什么关系？能叠加吗？**
混合检索解决"召回来源单一"，Rerank 解决"排序不精确"，两者可以串联：
`双路召回 + RRF 融合 → 候选集 → Rerank 精排 Top-5`。生产级 RAG 通常是两者都上。

**Q2：ES 索引和 Chroma 的数据一致性怎么保证？**
双写：文档入库时同时写 ES 索引与 Chroma 向量（可参考
[08-知识库增量同步实战-RocketMQ驱动向量化.md](08-知识库增量同步实战-RocketMQ驱动向量化.md) 的消息驱动思路）。

**Q3：为什么融合用排名而不是分数？**
向量分数（余弦相似度 0~1）与 BM25 分数（可到几十甚至上百）量纲完全不同，直接加权需要
调两套权重且随数据分布漂移；RRF 只用排名，天然无量纲、无需调参、实现简单。

**Q4：RRF 会不会把不相关的顶上来？**
RRF 融合的是"排名位置"，单路召回本身已按相关性排序，融合只是重新组合；双路都命中的
高排名文档理论上正是最相关的。若仍担心，可接 Rerank 再做一次精排把关。

**Q5：ES 索引名为什么要求小写？**
ES 索引名规范不允许大写字母，所以 `esIndexName()` 统一 `toLowerCase()`，调用方传
"Product-Manual" 也不会报错。

---

## 十、总结

| 环节 | 要点 |
|------|------|
| 为什么 | 语义查询与精确查询各有盲区，双路互补 |
| BM25 | ES 内置实现，`multiMatch(title^2, content)` + knowledgeBase 过滤 |
| RRF | \(1/(k+r)\) 按排名融合，k=60，无需调权重 |
| 降级 | 单路异常自动降级为另一路结果，故障隔离 |
| 接口 | `GET /search/hybrid?index=&query=&page=&size=` |

至此，检索能力覆盖"精确 + 语义"两类查询，双路结果经 RRF 融合输出，
为 RAG 问答和知识库搜索提供了更可靠的召回基础。
