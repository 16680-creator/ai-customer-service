package com.aics.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 配置
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.aics.search.repository")
public class ElasticsearchConfig {
    // Spring Data Elasticsearch 自动配置
    // 连接信息通过 application.yml 中的 spring.elasticsearch 配置
}
