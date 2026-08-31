package com.aics.common.autoconfigure;

import com.aics.common.ai.embedding.EmbeddingAutoConfig;
import com.aics.common.ai.embedding.HashEmbeddingModel;
import com.aics.common.exception.GlobalExceptionHandler;
import com.aics.common.storage.MinioProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * common 自动装配契约测试：验证 imports 条件装配与用户自定义 Bean 覆盖。
 */
class CommonAutoConfigurationTest {

    @Test
    @DisplayName("Servlet Web 应用 - 自动提供 GlobalExceptionHandler")
    void webAutoConfigurationShouldProvideExceptionHandler() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CommonWebAutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    @DisplayName("MinIO 在 classpath - 自动绑定配置并创建客户端")
    void minioAutoConfigurationShouldCreateClient() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MinioAutoConfiguration.class))
                .withPropertyValues(
                        "aics.minio.endpoint=http://127.0.0.1:9000",
                        "aics.minio.access-key=test",
                        "aics.minio.secret-key=test1234")
                .run(context -> {
                    assertThat(context).hasSingleBean(MinioProperties.class);
                    assertThat(context).hasSingleBean(MinioClient.class);
                });
    }

    @Test
    @DisplayName("业务模块自定义 MinioClient - 自动配置必须让位")
    void minioAutoConfigurationShouldBackOff() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MinioAutoConfiguration.class))
                .withBean(MinioClient.class, () -> MinioClient.builder()
                        .endpoint("http://127.0.0.1:9001").credentials("x", "12345678").build())
                .run(context -> assertThat(context).hasSingleBean(MinioClient.class));
    }

    @Test
    @DisplayName("Embedding provider=local - 自动提供 HashEmbeddingModel")
    void embeddingAutoConfigurationShouldProvideLocalModel() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EmbeddingAutoConfig.class))
                .withPropertyValues("aics.ai.embedding.provider=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmbeddingModel.class);
                    assertThat(context.getBean(EmbeddingModel.class)).isInstanceOf(HashEmbeddingModel.class);
                });
    }

    @Test
    @DisplayName("Embedding provider=openai - 本地模型自动配置不生效")
    void embeddingAutoConfigurationShouldDisableForOpenai() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EmbeddingAutoConfig.class))
                .withPropertyValues("aics.ai.embedding.provider=openai")
                .run(context -> assertThat(context).doesNotHaveBean(HashEmbeddingModel.class));
    }
}
