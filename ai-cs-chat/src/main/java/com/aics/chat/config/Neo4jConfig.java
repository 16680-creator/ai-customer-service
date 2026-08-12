package com.aics.chat.config;

import com.aics.chat.rag.graph.GraphProperties;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Neo4j 图数据库连接配置（storage=neo4j 时启用）。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aics.rag.graph.storage", havingValue = "neo4j")
public class Neo4jConfig {

    @Bean(destroyMethod = "close")
    public Driver neo4jDriver(GraphProperties properties) {
        if (!StringUtils.hasText(properties.getUri())) {
            throw new IllegalStateException("aics.rag.graph.uri 未配置，storage=neo4j 时必须提供 Neo4j 连接地址");
        }
        log.info("初始化 Neo4j 驱动: uri={}, database={}", properties.getUri(), properties.getDatabase());
        if (StringUtils.hasText(properties.getUsername())) {
            return GraphDatabase.driver(properties.getUri(),
                    AuthTokens.basic(properties.getUsername(), properties.getPassword()));
        }
        return GraphDatabase.driver(properties.getUri());
    }
}