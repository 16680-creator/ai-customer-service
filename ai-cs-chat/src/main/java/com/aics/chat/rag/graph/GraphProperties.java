package com.aics.chat.rag.graph;

import lombok.Data;
import org.neo4j.driver.SessionConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 图谱检索配置。
 */
@Data
@ConfigurationProperties(prefix = "aics.rag.graph")
public class GraphProperties {

    /** 是否启用图谱检索（默认关闭，未配置 Neo4j 时不影响主链路） */
    private boolean enabled = false;

    /** 多跳展开最大深度 */
    private int maxDepth = 2;

    /** 存储实现：in-memory（默认）/ neo4j */
    private String storage = "in-memory";

    /** Neo4j 连接地址（storage=neo4j 时必填，如 bolt://127.0.0.1:7687） */
    private String uri;

    /** Neo4j 用户名 */
    private String username;

    /** Neo4j 密码 */
    private String password;

    /** Neo4j 数据库名（可空，使用默认库） */
    private String database;

    /** 是否使用 Neo4j 存储 */
    public boolean isNeo4j() {
        return "neo4j".equalsIgnoreCase(storage);
    }

    /**
     * 构建 Session 配置（指定数据库时使用，否则默认库）。
     */
    public SessionConfig databaseConfig() {
        if (StringUtils.hasText(database)) {
            return SessionConfig.builder().withDatabase(database).build();
        }
        return SessionConfig.defaultConfig();
    }
}