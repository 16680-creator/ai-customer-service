package com.aics.chat.nl2sql;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 智能问数（NL2SQL）只读数据源装配。
 *
 * <p>为每个业务库创建独立的只读连接池（HikariCP），以 {@code 库标识 -> JdbcTemplate}
 * 的映射暴露给 {@link Nl2SqlQueryService} 使用，AI 按库标识选择数据源执行只读 SQL。</p>
 *
 * <p>安全说明：连接串中追加 {@code readOnly=true} 提示 MySQL 走只读会话；
 * 同时 {@link Nl2SqlQueryService} 会做 SELECT 白名单强校验，双保险保证 AI 只能读不能写。</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(Nl2SqlProperties.class)
public class Nl2SqlDataSourceConfig {

    /**
     * 库标识 -> 只读 JdbcTemplate 映射（user/product/order/chat/knowledge）。
     */
    @Bean
    public Map<String, JdbcTemplate> nl2SqlJdbcTemplates(Nl2SqlProperties properties) {
        Map<String, JdbcTemplate> templates = new LinkedHashMap<>();
        if (properties.getUrls() == null || properties.getUrls().isEmpty()) {
            log.warn("未配置 aics.nl2sql.urls，AI 智能问数工具不可用");
            return templates;
        }
        properties.getUrls().forEach((key, url) -> {
            String jdbcUrl = url;
            // 只读会话提示：readOnly 连接属性（MySQL 驱动 8.x 支持）
            jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "readOnly=true";
            HikariDataSource ds = new HikariDataSource();
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setJdbcUrl(jdbcUrl);
            ds.setUsername(properties.getUsername());
            ds.setPassword(properties.getPassword());
            ds.setMaximumPoolSize(3);
            ds.setMinimumIdle(1);
            ds.setConnectionTimeout(5000);
            ds.setIdleTimeout(600000);
            ds.setMaxLifetime(1500000);
            ds.setPoolName("nl2sql-" + key);
            templates.put(key, new JdbcTemplate(ds));
            log.info("NL2SQL 只读数据源就绪: {} -> {}", key, jdbcUrl.substring(0, jdbcUrl.indexOf("?") > 0 ? jdbcUrl.indexOf("?") : jdbcUrl.length()));
        });
        return templates;
    }
}
