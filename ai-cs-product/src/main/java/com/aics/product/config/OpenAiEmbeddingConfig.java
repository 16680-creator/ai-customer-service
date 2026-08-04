package com.aics.product.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI 兼容 Embedding 配置（支持中转站）。
 * 通过 aics.ai.embedding.provider=openai 启用；
 * base-url / api-key / 模型名均支持环境变量注入。
 */
@Configuration
@ConditionalOnProperty(name = "aics.ai.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingConfig {

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}")
    private String model;

    @Bean
    public EmbeddingModel openAiEmbeddingModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(model).build());
    }
}
