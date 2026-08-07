package com.aics.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 知识库服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aics.knowledge", "com.aics.common"}, exclude = {org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class})
@EnableDiscoveryClient
@MapperScan("com.aics.knowledge.mapper")
public class KnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}
