package com.aics.chat.rag.rerank;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rerank（重排序）配置
 *
 * <p>前缀 {@code aics.rerank}，对应 Nacos 配置：
 * <pre>
 * aics.rerank.base-url=https://api.siliconflow.cn
 * aics.rerank.api-key=sk-xxx
 * aics.rerank.model=BAAI/bge-reranker-v2-m3
 * aics.rerank.top-n=5
 * aics.rerank.min-score=0.7
 * aics.rerank.timeout-ms=5000
 * </pre>
 * </p>
 */
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

    /** 重排序最小相关度分数阈值（宪法要求：低于该分数的引用不返回） */
    private double minScore = 0.7;

    /** Rerank 调用超时时间（毫秒） */
    private long timeoutMs = 5000;
}
