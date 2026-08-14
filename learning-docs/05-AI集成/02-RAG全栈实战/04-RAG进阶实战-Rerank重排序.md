# RAG 进阶实战：Rerank 重排序（召回 Top-20 → 精排 Top-5）

> 本文档对应 `ai-cs-chat` 模块的 `rag/rerank` 包，讲解**两阶段检索**的第二阶段——Rerank 精排。
> 前置知识：[03-RAG向量检索实战.md](03-RAG向量检索实战.md)（向量检索与两阶段升级链路）。
>
> **核心目标**：用硅基流动的 `BAAI/bge-reranker-v2-m3` 模型，对向量宽召回的结果做交叉编码精排，
> 把"语义相似但不相关"的片段过滤掉，只把真正相关的前 5 条喂给大模型。

---

## 一、实战背景：为什么需要 Rerank？

纯向量检索（第一阶段）的三个痛点：

1. **召回噪声**：余弦相似度衡量的是"整体语义接近"，两个话题相近但答案不同的片段，分数可能都很高；
2. **关键词失灵**：含精确术语（如型号 "AICS-X200"）的片段，向量检索未必排在前面；
3. **精度天花板**：向量相似度分数（0~1 区间）在不同知识库间不可比，无法做统一阈值。

**Rerank 的思路**：用交叉编码器（Cross-Encoder）把"问题 + 文档片段"拼接后整体编码打分，
模型能看到两者之间**词级交互**（不像向量检索的双塔编码各算各的），
所以相关性判断更准确——这就是"精排"。

```
阶段一（宽召回）             阶段二（精排）
向量相似度 Top-20  ──────►  Cross-Encoder 逐条打分 ──────►  按分数降序取 Top-5
（快、召回全）                （慢、判得准）                   过滤 < minScore
```

> 一条经验法则：**向量检索负责"找得到"，Rerank 负责"找得对"**。
> 两阶段检索是生产级 RAG 的标准做法（如 LangChain 的 `ContextualCompressionRetriever`、LlamaIndex 的 reranker）。

---

## 二、整体架构与文件清单

| 文件 | 职责 |
|------|------|
| `rag/rerank/RerankService.java` | 重排序服务接口（返回 `Mono<List<RerankResultItem>>`） |
| `rag/rerank/SiliconFlowRerankService.java` | 硅基流动 API 实现（RestClient 调用 `/v1/rerank`） |
| `rag/rerank/RerankProperties.java` | 配置项（前缀 `aics.rerank`） |
| `rag/rerank/RerankResultItem.java` | 重排序结果条目（index / relevanceScore / text） |
| `config/SpringAiConfig.java` | `@EnableConfigurationProperties(RerankProperties.class)` 注册配置 |
| `service/KnowledgeBaseService.java` | 两阶段检索编排：宽召回 → 调 Rerank → 回退逻辑 |

```
用户问题
   │
   ▼
KnowledgeBaseService.search()
   ├─ ① 向量宽召回 Top-20（Chroma + bge-m3）
   ├─ ② RerankService.rerank(query, docs, topN) ──► 硅基流动 /v1/rerank（bge-reranker-v2-m3）
   │        │ 成功：按 relevanceScore 降序、过滤 < minScore
   │        └ 失败/无 Key：返回空 → 调用方降级为相似度排序
   └─ ③ 返回 Top-5 片段 → 拼上下文 → LLM 回答
```

---

## 三、Rerank 原理简介

### 1. 交叉编码器（Cross-Encoder）vs 双塔编码器（Bi-Encoder）

| 对比项 | Bi-Encoder（向量检索用，如 bge-m3） | Cross-Encoder（Rerank 用，如 bge-reranker-v2-m3） |
|--------|------------------------------------|--------------------------------------------------|
| 编码方式 | 问题、文档**各自**编码成向量 | 问题 + 文档**拼接**成一条输入整体编码 |
| 计算量 | 文档向量可**离线预计算**，在线只算问题向量，快 | 每条候选都要在线跑一次模型，慢 |
| 相关性精度 | 一般（看不到词级交互） | 高（能看到问题与文档的词级对齐） |
| 典型场景 | 千万级文档的召回阶段 | 几十条候选的精排阶段 |

> **为什么能这么配合？** 向量检索慢在"全库扫描"却快在"单条打分"（预计算），
> Rerank 快在"只处理少量候选"却慢在"单条打分"。两阶段正好互补：
> 第一阶段把百万文档缩到 20 条，第二阶段对 20 条做精确打分，成本可控、精度拉满。

### 2. 硅基流动 Rerank API 简介

- 接口：`POST {baseUrl}/v1/rerank`（OpenAI 兼容风格）
- 模型：`BAAI/bge-reranker-v2-m3`（开源、中文效果好、上下文 8192 token）
- 请求体关键字段：`model` / `query` / `documents`（待重排文本数组）/ `top_n` / `return_documents`
- 响应关键字段：`results[]`，每条含 `index`（对应输入 documents 下标）、`relevance_score`（0~1）、`document`（原文，仅 return_documents=true 时返回）

```bash
# 手动验证 API（curl 方式）
curl -X POST "https://api.siliconflow.cn/v1/rerank" \
  -H "Authorization: Bearer $SILICONFLOW_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "BAAI/bge-reranker-v2-m3",
    "query": "退货政策是什么？",
    "documents": ["我们支持15天无理由退货", "新品发布会于下周举行"],
    "top_n": 2,
    "return_documents": true
  }'

# 响应示例（results 按相关度降序）
# {"results":[{"index":0,"relevance_score":0.98,"document":"我们支持15天无理由退货"},
#             {"index":1,"relevance_score":0.12,"document":"新品发布会于下周举行"}]}
```

---

## 四、代码实现

### 1. 服务接口（RerankService）

文件：`ai-cs-chat/src/main/java/com/aics/chat/rag/rerank/RerankService.java`

```java
public interface RerankService {
    /**
     * 对粗召回文档执行重排序。
     *
     * @param query     用户问题
     * @param documents 粗召回的文档列表
     * @param topN      重排序后返回的条数（实际以配置的 topN/minScore 为准）
     * @return 按相关度分数降序的重排序结果；异常/无 API Key 时为 empty（block 得到 null）
     */
    Mono<List<RerankResultItem>> rerank(String query, List<Document> documents, int topN);
}
```

> 返回 `Mono`（响应式）而不是直接返回列表，是为了在**异步超时**（`.timeout()`）和
> **异常降级**（`.onErrorResume()`）上更优雅——调用方 `block()` 拿到 `null` 即走回退逻辑。

### 2. 完整实现（SiliconFlowRerankService）

文件：`ai-cs-chat/src/main/java/com/aics/chat/rag/rerank/SiliconFlowRerankService.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SiliconFlowRerankService implements RerankService {

    private final RerankProperties properties;
    private final RestClient.Builder restClientBuilder;   // Spring Boot 自动配置注入，clone 后使用

    @Override
    public Mono<List<RerankResultItem>> rerank(String query, List<Document> documents, int topN) {
        // 无 API Key 或无待重排文档时直接降级
        if (!StringUtils.hasText(properties.getApiKey())
                || query == null || documents == null || documents.isEmpty()) {
            log.info("Rerank降级: apiKey为空或无待重排文档");
            return Mono.empty();
        }
        return Mono.fromCallable(() -> doRerank(query, documents, topN))
                .subscribeOn(Schedulers.boundedElastic())  // fromCallable 在订阅线程同步执行，必须切到弹性线程池，timeout 计时器才生效
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .onErrorResume(e -> {
                    log.warn("Rerank调用失败，降级为向量相似度排序: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * 同步调用 Rerank API 并解析结果。
     */
    private List<RerankResultItem> doRerank(String query, List<Document> documents, int topN) {
        // 复用注入的 RestClient.Builder（Spring Boot 自动配置提供），clone 后设置 baseUrl，
        // 避免污染共享 builder；不再每次 new RestClient，提升复用率
        RestClient restClient = restClientBuilder.clone()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        RerankRequest request = new RerankRequest();
        request.setModel(properties.getModel());
        request.setQuery(query);
        request.setDocuments(documents.stream().map(Document::getText).toList());
        request.setTopN(Math.min(topN, documents.size()));
        request.setReturnDocuments(true);

        RerankResponse response = restClient.post()
                .uri("/v1/rerank")
                .body(request)
                .retrieve()
                .body(RerankResponse.class);

        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            log.warn("Rerank响应为空: query={}", query);
            return List.of();
        }

        // 过滤低于 minScore 的条目，并按相关度降序排序
        List<RerankResultItem> items = response.getResults().stream()
                .filter(r -> r.getRelevanceScore() >= properties.getMinScore())
                .sorted(Comparator.comparingDouble(RerankResultItem::getRelevanceScore).reversed())
                .toList();
        log.info("Rerank完成: 输入{}条, 过滤后{}条, 耗时配置={}ms",
                documents.size(), items.size(), properties.getTimeoutMs());
        return items;
    }

    /**
     * Rerank API 请求体。
     */
    @Data
    public static class RerankRequest {
        private String model;
        private String query;
        private List<String> documents;

        @JsonProperty("top_n")
        private int topN;

        @JsonProperty("return_documents")
        private boolean returnDocuments;
    }

    /**
     * Rerank API 响应体（只取 results，其余字段忽略）。
     */
    @Data
    public static class RerankResponse {
        private String id;
        private String model;
        private List<RerankResultItem> results;
    }
}
```

**关键点逐行解读**：

| 代码片段 | 作用 |
|----------|------|
| `Mono.fromCallable(() -> doRerank(...))` | 把同步 API 调用包进响应式管道，统一做超时/降级 |
| `.timeout(Duration.ofMillis(properties.getTimeoutMs()))` | 兜底超时：Rerank 慢时不能让用户一直等 |
| `.onErrorResume(e -> Mono.empty())` | 任何异常（超时/网络/4xx/5xx）都降级为空，不抛给上层 |
| `Math.min(topN, documents.size())` | top_n 不能大于候选数，避免 API 报错 |
| `filter(r -> r.getRelevanceScore() >= minScore)` | 精排后仍按分数阈值过滤，宁缺毋滥（合规要求） |
| `restClientBuilder.clone()` 复用 | 注入的 `RestClient.Builder` clone 后使用，避免每次 new，也不污染共享 builder |

### 3. 结果条目（RerankResultItem）

文件：`ai-cs-chat/src/main/java/com/aics/chat/rag/rerank/RerankResultItem.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RerankResultItem {

    /** 对应输入文档列表中的原始下标 */
    private int index;

    /** 相关度分数（0~1，越大越相关） */
    @JsonProperty("relevance_score")
    private double relevanceScore;

    /** 命中文档文本（兼容响应字段 document/text） */
    @JsonAlias("document")
    private String text;
}
```

> `index` 是串联两阶段的**关键桥梁**：Rerank 只返回"输入列表里的第几个"，调用方
> `KnowledgeBaseService` 通过 `recallDocs.get(item.getIndex())` 取回原 `Document`（含完整 metadata）。

### 4. 配置类（RerankProperties）

文件：`ai-cs-chat/src/main/java/com/aics/chat/rag/rerank/RerankProperties.java`

```java
@Data
@ConfigurationProperties("aics.rerank")
public class RerankProperties {

    /** Rerank API 基础地址 */
    private String baseUrl = "https://api.siliconflow.cn";

    /** Rerank API Key（为空时 Rerank 降级，回退为向量相似度排序） */
    private String apiKey = "";

    /** Rerank 模型 */
    private String model = "BAAI/bge-reranker-v2-m3";

    /** 重排序后返回的 Top-N 条数 */
    private int topN = 5;

    /** 重排序最小相关度分数阈值（低于该分数的引用不返回） */
    private double minScore = 0.7;

    /** Rerank 调用超时时间（毫秒） */
    private long timeoutMs = 5000;
}
```

> 注册方式：`SpringAiConfig` 上标注 `@EnableConfigurationProperties(RerankProperties.class)`，
> 配置来源为 Nacos 的 `ai-cs-chat.yml`。

### 5. 在两阶段检索中的编排（KnowledgeBaseService.search）

```java
// 阶段二：Rerank 精排（可选，bean 不存在或调用失败时退化）
RerankService rerankService = rerankServiceProvider.getIfAvailable();
if (rerankService != null) {
    try {
        List<RerankResultItem> reranked = rerankService.rerank(query, recallDocs, topK).block();
        if (reranked != null && !reranked.isEmpty()) {
            List<Document> result = new ArrayList<>(reranked.size());
            for (RerankResultItem item : reranked) {
                int idx = item.getIndex();
                if (idx >= 0 && idx < recallDocs.size()) {
                    result.add(recallDocs.get(idx));   // 按下标回取原文档
                }
            }
            return result;
        }
    } catch (Exception e) {
        log.warn("知识库[{}]Rerank调用异常，降级为相似度排序: {}", knowledgeBase, e.getMessage());
    }
}
// 退化路径：向量检索结果已按相似度降序，直接取 Top-N
```

---

## 五、降级策略（核心）

Rerank 是**增强项，不是必需品**——它的任何故障都不能影响用户提问。本项目做了四层降级：

| 层级 | 触发条件 | 处理方式 |
|------|----------|----------|
| ① 配置级 | `apiKey` 为空（未开通 / 未配置） | `rerank()` 直接返回 `Mono.empty()`，不发起网络请求 |
| ② 超时级 | 调用超过 `timeout-ms`（默认 5s） | `.timeout()` 触发，返回空 |
| ③ 异常级 | 网络错误 / HTTP 非 2xx / 响应为空 | `.onErrorResume()` 捕获，返回空 |
| ④ 依赖级 | `RerankService` Bean 不存在（可选依赖） | `ObjectProvider.getIfAvailable()` 返回 null，跳过精排 |

降级后统一走**向量相似度排序取 Top-N**，链路依然完整，只是精度回落。

```bash
# 降级日志特征
# 无 Key：     Rerank降级: apiKey为空或无待重排文档
# 调用失败：   Rerank调用失败，降级为向量相似度排序: xxx
# 编排层：     知识库[product-manual]检索完成(相似度排序): 召回20条 -> 返回5条
```

> 设计启示：**第三方 AI API 永远要当成"会挂的组件"来设计**，
> 超时 + 降级 + 可选注入三者缺一不可。

---

## 六、配置项说明

### Nacos 配置（dataId: ai-cs-chat.yml，源文件 `tools/nacos-config/ai-cs-chat.yml`）

```yaml
aics:
  rerank:
    base-url: https://api.siliconflow.cn   # API 地址（一般不用改）
    api-key: ${SILICONFLOW_API_KEY}        # 从环境变量注入，不要硬编码！
    model: BAAI/bge-reranker-v2-m3         # 重排模型
    top-n: 5                               # 精排后返回条数
    min-score: 0.7                         # 相关度阈值，低于则丢弃
    timeout-ms: 5000                       # 超时（连接 + 读取）

  # 配合使用的两阶段检索配置
  rag:
    recall-top-k: 20                       # 宽召回条数
    recall-threshold: 0.5                  # 宽召回阈值
```

### 环境变量

| 变量 | 用途 |
|------|------|
| `SILICONFLOW_API_KEY` | 硅基流动 API Key（Embedding 与 Rerank 共用） |

### 调参建议

- `min-score`：知识库质量高 → 可调到 0.8 过滤更狠；用户常问模糊问题 → 降到 0.6 防误杀；
- `top-n`：Prompt 上下文充裕可到 8~10；回答以总结为主则 3~5 足够；
- `timeout-ms`：中文长文本重排较慢，实测 3~8s 正常，5s 是合理折中；
- 成本控制：`recall-top-k` × Rerank 单价 = 单次问答成本，20 条是性价比平衡点。

---

## 七、验证方法

```bash
# 1. 前置：Nacos 已发布 ai-cs-chat.yml（含 aics.rerank.*），环境变量 SILICONFLOW_API_KEY 已设置

# 2. 启动 ai-cs-chat（JDK21），观察启动日志应出现配置加载

# 3. 入库测试数据
curl -X POST "http://localhost:8083/rag/knowledge-base/text" \
     -d "knowledgeBase=product-manual" \
     -d "text=我们支持15天无理由退货，运费由买家承担。"

# 4. 检索测试（走两阶段链路）
curl "http://localhost:8083/rag/knowledge-base/search?knowledgeBase=product-manual&query=退货政策"

# 5. 观察日志应有：
#    知识库[product-manual]宽召回完成: 命中20条
#    Rerank完成: 输入20条, 过滤后1条, 耗时配置=5000ms
#    知识库[product-manual]Rerank精排完成: 召回20条 -> 返回1条

# 6. 降级演练：把 Nacos 中 api-key 置空 → 重启 → 检索
#    日志出现 "Rerank降级: apiKey为空或无待重排文档"，接口仍正常返回
```

---

## 八、常见问题

**Q1：Rerank 结果比向量检索还差？**
先查 `min-score` 是否过高导致误杀；再看宽召回 `recall-top-k` 是否太小（比如候选本身就 5 条，
精排无意义）。Rerank 的威力建立在"召回充分"之上。

**Q2：每次回答都多花几百毫秒，值得吗？**
值得。实测 bge-reranker-v2-m3 对 20 条候选约 1~3s；换来的是回答"引用来源更准、幻觉更少"，
客服场景下合规价值远大于延迟成本。若延迟敏感，可降 `recall-top-k` 到 10。

**Q3：RestClient 是每次调用都 new 吗？**
不是。当前实现通过构造器注入 `RestClient.Builder`（Spring Boot 自动配置提供），在 `doRerank` 中
`restClientBuilder.clone().baseUrl(...)` 复用并构建，避免污染共享 builder，也避免每次 new 的开销。

**Q4：换其他 Rerank 服务商（如 Cohere / Jina）怎么办？**
实现一个新的 `RerankService` 实现类即可，两阶段编排代码（`KnowledgeBaseService`）一行不用改——这就是接口抽象的价值。

---

## 九、总结

| 环节 | 要点 |
|------|------|
| 为什么 | 向量检索召回噪声大，需要交叉编码器精排 |
| 怎么调 | `RestClient` 调硅基流动 `/v1/rerank`，`bge-reranker-v2-m3` |
| 怎么降级 | 空 Key / 超时 / 异常 / Bean 缺失 → 四层降级为相似度排序 |
| 配置在哪 | Nacos `aics.rerank.*`（base-url/api-key/model/top-n/min-score/timeout-ms） |
| 成本控制 | `recall-top-k`（20）× 单条重排价格；`min-score` 把关质量 |

至此，RAG 检索链路从"单次向量 Top-5"升级为 **宽召回 Top-20 + Rerank 精排 Top-5**，
精度与成本可控，且任一环节故障都不影响主流程。
