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
 * OpenAI 兼容 Embedding 配置 —— 商品向量化的模型来源。
 *
 * <h3>学习要点（技术：Embedding 模型装配）</h3>
 * <ul>
 *   <li><b>为什么用 OpenAI 协议</b>：硅基流动等平台兼容 OpenAI 的 /embeddings 接口，
 *       用 Spring AI 的 OpenAiEmbeddingModel 一行接入 bge-m3 多语言向量模型。</li>
 *   <li><b>@ConditionalOnProperty</b>：仅当配置 {@code aics.ai.embedding.provider=openai} 时启用；
 *       默认走本地 HashEmbeddingModel（零依赖兜底，见 ai-cs-common）。</li>
 *   <li><b>为什么向量模型必须唯一</b>：所有商品/文档向量必须来自同一模型，
 *       否则向量空间不一致、余弦相似度无意义。</li>
 * </ul>
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
