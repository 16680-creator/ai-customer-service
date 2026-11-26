package com.aics.chat.config;

import com.aics.common.ai.embedding.HashEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

class VectorStoreConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class, ChromaVectorStoreAutoConfiguration.class))
            .withUserConfiguration(TestDependencies.class, VectorStoreConfig.class)
            .withPropertyValues(
                    "spring.ai.vectorstore.chroma.client.host=http://chroma.invalid",
                    "spring.ai.vectorstore.chroma.client.port=8000",
                    "spring.ai.vectorstore.chroma.collection-name=offline-startup-test",
                    "spring.ai.vectorstore.chroma.initialize-schema=false");

    @Test
    void createsLazyProxyWithoutConnectingToChromaDuringStartup() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("vectorStore");
            assertThat(context.getBean("vectorStore")).isInstanceOf(VectorStore.class);
            // 主 VectorStore 是显式接口代理，不能触发 Chroma 的 afterPropertiesSet 网络查询。
            // 按具体类型直接 getBean 是主动初始化，不属于启动装配路径。
            VectorStoreConsumer consumer = context.getBean(VectorStoreConsumer.class);
            assertThat(consumer.vectorStore).isNotInstanceOf(ChromaVectorStore.class);
        });
    }

    record VectorStoreConsumer(VectorStore vectorStore) {
    }

    @TestConfiguration
    static class TestDependencies {

        @Bean
        VectorStoreConsumer vectorStoreConsumer(VectorStore vectorStore) {
            return new VectorStoreConsumer(vectorStore);
        }

        @Bean
        HashEmbeddingModel embeddingModel() {
            return new HashEmbeddingModel();
        }

        @Bean
        TokenCountBatchingStrategy chromaBatchingStrategy() {
            return new TokenCountBatchingStrategy();
        }
    }
}
