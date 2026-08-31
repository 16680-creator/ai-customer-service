package com.aics.common.autoconfigure;

import com.aics.common.storage.FileStorageService;
import com.aics.common.storage.MinioProperties;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MinIO 客户端自动装配：classpath 有 MinioClient 且业务模块没自定义 Bean 时生效。
 */
@AutoConfiguration
@ConditionalOnClass(MinioClient.class)
@EnableConfigurationProperties(MinioProperties.class)
public class MinioAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MinioClient.class)
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService fileStorageService(MinioClient minioClient, MinioProperties properties) {
        return new FileStorageService(minioClient, properties);
    }
}
