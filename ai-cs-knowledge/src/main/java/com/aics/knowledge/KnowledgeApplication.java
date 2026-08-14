package com.aics.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 知识库服务（ai-cs-knowledge）启动类
 *
 * <p>职责：作为整个知识库微服务的入口，负责 Spring 容器引导、
 * MyBatis-Plus Mapper 扫描以及 Nacos 服务注册发现。</p>
 *
 * <p>技术要点：</p>
 * <ul>
 *   <li>scanBasePackages 同时扫描 com.aics.knowledge（本模块）与 com.aics.common（公共模块），
 *       以便引用通用组件（Result、BusinessException 等）</li>
 *   <li>exclude 排除 Spring AI 默认的 OpenAiEmbeddingAutoConfiguration，
 *       改由 {@link com.aics.knowledge.config.KnowledgeAiConfig} 自定义 EmbeddingModel
 *       （使用硅基流动 bge-m3 模型，与 ai-cs-chat 共用同一向量空间）</li>
 *   <li>@EnableDiscoveryClient 向 Nacos 注册，供 ai-cs-chat 等模块通过服务发现调用</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {"com.aics.knowledge", "com.aics.common"}, exclude = {org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class})
@EnableDiscoveryClient
@MapperScan("com.aics.knowledge.mapper")
public class KnowledgeApplication {

    /**
     * 程序入口方法
     *
     * @param args 启动参数（可透传 Spring Boot 配置项）
     */
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}
