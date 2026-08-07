package com.aics.knowledge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI 配置：Embedding 模型（与 ai-cs-chat 共用硅基流动 bge-m3 + Chroma 向量库）
 */
@Configuration
public class KnowledgeAiConfig {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAiConfig.class);

    @Value("${aics.embedding.base-url:https://api.siliconflow.cn}")
    private String embeddingBaseUrl;

    @Value("${aics.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${aics.embedding.model:BAAI/bge-m3}")
    private String embeddingModel;

    /**
     * 自定义 EmbeddingModel（从 Nacos aics.embedding.* 读取，与 chat 一致）
     */
    @Bean
    @Primary
    public EmbeddingModel knowledgeEmbeddingModel() {
        log.info("创建知识库 EmbeddingModel: base-url={}, model={}", embeddingBaseUrl, embeddingModel);
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(embeddingModel).build());
    }
}