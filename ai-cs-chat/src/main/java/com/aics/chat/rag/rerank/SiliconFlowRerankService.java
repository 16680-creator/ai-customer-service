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
     * 异步执行 Rerank 调用，超时/异常时降级返回空 Mono。
     *
     * <p>实现要点：</p>
     * <ul>
     *   <li>{@code Mono.fromCallable(...)}：把同步阻塞的 HTTP 调用包装成 Mono，
     *       callable 在订阅时才执行（冷源）。</li>
     *   <li>{@code .subscribeOn(Schedulers.boundedElastic())}：把阻塞调用调度到弹性线程池执行，
     *       避免占用 Reactor 主线程；<b>必须</b>显式 subscribeOn，否则 fromCallable 会在调用方线程同步执行，
     *       导致后续 {@code .timeout()} 计时器要等阻塞调用返回后才启动，超时配置形同虚设。</li>
     *   <li>{@code .timeout(Duration)}：超过 {@link RerankProperties#getTimeoutMs()} 即抛
     *       {@code TimeoutException}。</li>
     *   <li>{@code .onErrorResume(e -> Mono.empty())}：任何异常（超时、网络错误、解析失败）都降级为空 Mono，
     *       调用方 {@code block()} 得到 {@code null} 后回退为向量相似度排序。</li>
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
