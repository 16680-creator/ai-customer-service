package com.aics.chat.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * 向量存储（VectorStore）配置 —— RAG 检索增强生成的核心存储层。
 *
 * <p>作用：把知识库文档转成向量后存进向量库，提问时在向量库里做"语义相似度检索"，
 * 找出与问题最相关的文档片段，作为上下文喂给大模型，从而让模型回答"它本来不知道"的私有知识。</p>
 *
 * <h3>当前实现（Chroma，持久化向量库）</h3>
 * <pre>
 *   EmbeddingModel → 由 ai-cs-common 提供：
 *                     - aics.ai.embedding.provider=local（默认） → HashEmbeddingModel（零依赖哈希向量）
 *                     - aics.ai.embedding.provider=openai       → OpenAI 兼容向量（需配 spring.ai.openai.*）
 *   VectorStore    → 本类基于 Chroma starter 的 API/配置声明，并覆盖自动装配以支持延迟创建
 *                    连接配置见 application.yml 的 spring.ai.vectorstore.chroma.*
 * </pre>
 *
 * <p>本类通过显式接口代理延迟创建 Chroma Bean。Spring AI 2.1 的
 * {@code ChromaVectorStore} 实现了 {@code InitializingBean}，即使
 * {@code initialize-schema=false}，容器创建实例时仍会查询远端集合。延迟到首次 RAG
 * 操作时创建，可以让未启动 Chroma 的服务完成启动；实际入库/检索仍需可达的 Chroma。</p>
 *
 * <h3>切换为其他向量库</h3>
 * <p>当前用 Chroma 实现 {@code VectorStore} 接口。若要换回内存版（本地零依赖快速验证）或
 * 切换 Elasticsearch / Milvus / Qdrant / Redis / PGVector 等，只需：</p>
 * <pre>
 * 1. pom.xml 替换/增加对应 starter 依赖（如 spring-ai-elasticsearch-store-spring-boot-starter）
 * 2. application.yml 配置对应连接参数
 * 3. 业务代码（KnowledgeBaseService / SpringAiConfig）一行不改，因为它们只依赖 VectorStore 接口
 * </pre>
 *
 * <h3>启动 Chroma（本地开发）</h3>
 * <pre>
 * 方式一：Docker
 *   docker run -d --name chroma -p 8000:8000 chromadb/chroma
 * 方式二：Python
 *   pip install chromadb && chroma run --host 0.0.0.0 --port 8000
 * </pre>
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    @Primary
    public VectorStore vectorStore(@Lazy ChromaVectorStore chromaVectorStore) {
        // @Lazy 无法可靠覆盖接口注入的初始化；显式代理仅在首次 VectorStore 方法调用时解析目标 Bean。
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(VectorStore.class);
        proxyFactory.setTargetSource(new org.springframework.aop.target.LazyInitTargetSource() {
            @Override
            public Object getTarget() {
                return chromaVectorStore;
            }
        });
        return (VectorStore) proxyFactory.getProxy();
    }

    @Bean
    @Lazy
    public ChromaVectorStore chromaVectorStore(EmbeddingModel embeddingModel,
                                               ChromaApi chromaApi,
                                               ChromaVectorStoreProperties storeProperties,
                                               ObjectProvider<ObservationRegistry> observationRegistry,
                                               ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                               BatchingStrategy chromaBatchingStrategy) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName(storeProperties.getCollectionName())
                .databaseName(storeProperties.getDatabaseName())
                .tenantName(storeProperties.getTenantName())
                .initializeSchema(storeProperties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable())
                .batchingStrategy(chromaBatchingStrategy)
                .build();
    }
}
