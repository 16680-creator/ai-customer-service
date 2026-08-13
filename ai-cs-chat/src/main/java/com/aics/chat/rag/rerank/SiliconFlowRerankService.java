package com.aics.chat.rag.rerank;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * 基于硅基流动（SiliconFlow）Rerank API 的重排序服务实现。
 *
 * <p>调用 {@code POST {baseUrl}/v1/rerank}，模型默认 {@code BAAI/bge-reranker-v2-m3}。
 * 任何异常（超时、网络错误、无 API Key）都会降级：返回空 Mono，
 * 调用方 {@code block()} 得到 {@code null} 后回退为向量相似度排序。</p>
 *
 * <h3>【AI 技术详解】Rerank（重排序）原理</h3>
 * <ul>
 *   <li><b>为什么需要 Rerank</b>：
 *       <ul>
 *         <li><b>向量检索的局限</b>：余弦相似度是"粗排"，速度快但精度一般</li>
 *         <li><b>Rerank 的优势</b>：交叉编码器（Cross-Encoder）精排，更准但更慢</li>
 *         <li><b>两阶段策略</b>：先粗后精，兼顾性能与精度</li>
 *       </ul>
 *   </li>
 *   <li><b>Rerank 模型原理</b>：
 *       <ul>
 *         <li><b>双塔模型（Bi-Encoder）</b>：向量检索用的模型，文档和查询分别编码，
 *             计算余弦相似度。速度快但无法捕捉文档与查询的交互关系</li>
 *         <li><b>交叉编码器（Cross-Encoder）</b>：Rerank 用的模型，将查询和文档拼接后一起编码，
 *             能捕捉更细粒度的语义关系。精度高但速度慢</li>
 *         <li><b>bge-reranker-v2-m3</b>：BAAI 开源的 Rerank 模型，支持多语言</li>
 *       </ul>
 *   </li>
 *   <li><b>两阶段检索流程</b>：
 *       <ol>
 *         <li>第一阶段：向量检索 Top-20（宽召回，速度快）</li>
 *         <li>第二阶段：Rerank 精排 Top-5（精度高，速度慢）</li>
 *         <li>最终返回最相关的 5 个文档片段</li>
 *       </ol>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】Rerank 与 Embedding 的区别</h3>
 * <ul>
 *   <li><b>Embedding（向量化）</b>：
 *       <ul>
 *         <li>用途：将文本转为向量，用于相似度检索</li>
 *         <li>模型：Bi-Encoder（双塔），文档和查询分别编码</li>
 *         <li>速度：快（向量可预计算，检索时只算余弦相似度）</li>
 *         <li>精度：一般（无法捕捉文档与查询的交互关系）</li>
 *       </ul>
 *   </li>
 *   <li><b>Rerank（重排序）</b>：
 *       <ul>
 *         <li>用途：对检索结果精排，提升相关性</li>
 *         <li>模型：Cross-Encoder（交叉编码器），查询和文档一起编码</li>
 *         <li>速度：慢（每次都要重新计算）</li>
 *         <li>精度：高（能捕捉更细粒度的语义关系）</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>【技术关联】与 KnowledgeBaseService 的协作</h3>
 * <pre>
 *   KnowledgeBaseService.search()
 *       ├── 第一阶段：VectorStore.similaritySearch() → Top-20 宽召回
 *       ├── 第二阶段：RerankService.rerank() → Top-5 精排
 *       └── 降级：Rerank 失败时回退为向量相似度排序
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiliconFlowRerankService implements RerankService {

    private final RerankProperties properties;

    /**
     * RestClient 构建器（Spring Boot 自动配置提供；clone 后使用，避免污染共享 builder）。
     * 通过构造器注入，便于单元测试使用 MockRestServiceServer 模拟 Rerank API。
     */
    private final RestClient.Builder restClientBuilder;

    /**
     * 【AI 核心】异步执行 Rerank 调用，超时/异常时降级返回空 Mono。
     *
     * <p><b>【AI 技术详解】Reactor 响应式编程</b>：
     * <ul>
     *   <li><b>Mono</b>：Reactor 的异步类型，代表 0 或 1 个元素的异步流</li>
     *   <li><b>冷源（Cold Source）</b>：Mono.fromCallable() 创建的是冷源，
     *       只有订阅时才执行（类似懒加载）</li>
     *   <li><b>subscribeOn</b>：指定执行线程池，避免阻塞主线程</li>
     *   <li><b>timeout</b>：超时控制，超时后抛出 TimeoutException</li>
     *   <li><b>onErrorResume</b>：错误恢复，异常时返回默认值（空 Mono）</li>
     * </ul>
     *
     * <p><b>【技术关联】为什么用 Reactor 而不是 CompletableFuture</b>：
     * <ul>
     *   <li>Rerank 是可选增强，不是必需依赖，用 Mono 更优雅地处理"无结果"场景</li>
     *   <li>Mono.empty() 表示"无结果"，比 CompletableFuture.completedFuture(null) 更语义化</li>
     *   <li>Reactor 的操作符（timeout、onErrorResume）比 CompletableFuture 更丰富</li>
     * </ul>
     *
     * @param query     用户问题
     * @param documents 第一阶段向量召回的文档列表
     * @param topN      重排序后返回的条数（实际以配置的 topN/minScore 为准）
     * @return 按相关度降序的重排序结果；异常/无 API Key 时为 empty（block 得到 null）
     */
    @Override
    public Mono<List<RerankResultItem>> rerank(String query, List<Document> documents, int topN) {
        // 无 API Key 或无待重排文档时直接降级
        if (!StringUtils.hasText(properties.getApiKey())
                || query == null || documents == null || documents.isEmpty()) {
            log.info("Rerank降级: apiKey为空或无待重排文档");
            return Mono.empty();
        }
        // 注意：fromCallable 的 callable 在订阅线程同步执行，必须 subscribeOn 到弹性线程池，
        // 否则 timeout 计时器要等阻塞调用完成后才启动，超时配置不会生效
        return Mono.fromCallable(() -> doRerank(query, documents, topN))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .onErrorResume(e -> {
                    log.warn("Rerank调用失败，降级为向量相似度排序: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * 同步调用 Rerank API 并解析结果。
     *
     * <p>这里通过 {@code restClientBuilder.clone()} 复用 Spring Boot 自动配置的构建器
     * （共享消息转换器、拦截器等），再设置本次调用的 baseUrl 与 Authorization 头。
     * 不直接 {@code new RestClient.Builder()} 是为了保留框架注入的默认能力；
     * 不复用同一个 builder 实例是为了避免多线程下污染 builder 的状态。</p>
     */
    private List<RerankResultItem> doRerank(String query, List<Document> documents, int topN) {
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

        // 过滤低于 minScore 的条目（宪法要求），并按相关度降序排序
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
     * Rerank API 响应体（忽略未知字段如 meta，只取 results）。
     */
    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class RerankResponse {
        private String id;
        private String model;
        private List<RerankResultItem> results;
    }
}
