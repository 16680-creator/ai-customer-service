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
 * Neo4j 图数据库连接配置 —— 创建 {@link Driver} 连接。
 *
 * <h3>学习要点（技术：驱动装配 / 条件配置）</h3>
 * <ul>
 *   <li><b>Driver 生命周期</b>：Neo4j 驱动是重量级连接池，全局单例，
 *       {@code destroyMethod = "close"} 让 Spring 容器关闭时自动释放。</li>
 *   <li><b>鉴权</b>：配置了用户名才用 basic 认证，否则走无认证连接（本地开发）。</li>
 *   <li><b>条件启用</b>：仅当 Nacos {@code aics.rag.graph.storage=neo4j} 时才装配，
 *       且 uri 必填（缺失直接启动失败并给出明确提示），避免"配了半截"的坑。</li>
 * </ul>
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