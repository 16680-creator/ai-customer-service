package com.aics.search.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI 配置（全文检索基于 Chroma）
 *
 * <p>全文检索从 Elasticsearch 切换到 Chroma 后，查询语句需要先向量化再做相似度检索。
 * 这里提供与 ai-cs-chat 一致的 EmbeddingModel（硅基流动 bge-m3），
 * ChromaVectorStore 由 spring-ai-starter-vector-store-chroma 自动装配。</p>
 */
@Configuration
public class SpringAiConfig {

    private static final Logger log = LoggerFactory.getLogger(SpringAiConfig.class);

    @Value("${aics.embedding.base-url:https://api.siliconflow.cn}")
    private String embeddingBaseUrl;

    @Value("${aics.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${aics.embedding.model:BAAI/bge-m3}")
    private String embeddingModel;

    /**
     * 自定义 EmbeddingModel：使用硅基流动 API（DeepSeek 不支持 /v1/embeddings）。
     * @Primary 确保 ChromaVectorStore 注入点优先使用此 Bean。
     */
    @Bean
    @Primary
    public EmbeddingModel siliconFlowEmbeddingModel() {
        log.info("创建硅基流动 EmbeddingModel(base-url={}, model={})", embeddingBaseUrl, embeddingModel);
        OpenAiApi embeddingApi = OpenAiApi.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .build();
        return new OpenAiEmbeddingModel(embeddingApi,
                org.springframework.ai.document.MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(embeddingModel).build());
    }
}