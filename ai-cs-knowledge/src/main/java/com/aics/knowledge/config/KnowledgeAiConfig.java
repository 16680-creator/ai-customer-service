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
 * 知识库模块 Spring AI 配置
 *
 * <p>职责：为本模块自定义 EmbeddingModel Bean，用于知识文档向量化入库（写入 Chroma）。</p>
 *
 * <p>技术要点：</p>
 * <ul>
 *   <li>与 ai-cs-chat 模块共用硅基流动 bge-m3 Embedding 模型 + Chroma 向量库，
 *       确保对话侧 RAG 检索与知识库侧写入使用同一向量空间，语义可比对</li>
 *   <li>排除 Spring AI 默认的 OpenAiEmbeddingAutoConfiguration（见启动类），
 *       避免默认配置覆盖本类 @Primary 自定义 Bean</li>
 *   <li>配置项 aics.embedding.* 统一从 Nacos 读取，便于多环境切换</li>
 * </ul>
 */
@Configuration
public class KnowledgeAiConfig {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAiConfig.class);

    /** Embedding 服务基础地址（默认硅基流动） */
    @Value("${aics.embedding.base-url:https://api.siliconflow.cn}")
    private String embeddingBaseUrl;

    /** Embedding 服务 API Key */
    @Value("${aics.embedding.api-key:}")
    private String embeddingApiKey;

    /** Embedding 模型名称（默认 BAAI/bge-m3，与 ai-cs-chat 保持一致） */
    @Value("${aics.embedding.model:BAAI/bge-m3}")
    private String embeddingModel;

    /**
     * 自定义 EmbeddingModel Bean（从 Nacos aics.embedding.* 读取，与 chat 一致）
     *
     * <p>使用 @Primary 确保本 Bean 优先于 Spring AI 自动配置被注入到
     * {@link com.aics.knowledge.service.KnowledgeVectorService} 的 VectorStore 中。</p>
     *
     * @return OpenAI 协议兼容的 EmbeddingModel 实例
     */
    @Bean
    @Primary
    public EmbeddingModel knowledgeEmbeddingModel() {
        log.info("创建知识库 EmbeddingModel: base-url={}, model={}", embeddingBaseUrl, embeddingModel);
        // 基于 OpenAI 协议构建 API 客户端（硅基流动兼容 OpenAI 接口）
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .build();
        // MetadataMode.EMBED：仅对文档内容做 Embedding，元数据不参与向量化
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(embeddingModel).build());
    }
}