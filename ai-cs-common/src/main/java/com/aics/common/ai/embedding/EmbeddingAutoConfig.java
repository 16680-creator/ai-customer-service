package com.aics.common.ai.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Embedding 模型自动配置。
 * 默认使用本地哈希向量实现（aics.ai.embedding.provider=local）；
 * 配置为 openai 时由业务模块提供 OpenAI 兼容 EmbeddingModel Bean 覆盖。
 */
@AutoConfiguration
@ConditionalOnProperty(name = "aics.ai.embedding.provider", havingValue = "local", matchIfMissing = true)
public class EmbeddingAutoConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel hashEmbeddingModel() {
        return new HashEmbeddingModel();
    }
}
