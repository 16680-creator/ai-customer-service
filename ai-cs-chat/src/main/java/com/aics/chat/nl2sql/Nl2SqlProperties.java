package com.aics.chat.nl2sql;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 智能问数（NL2SQL）只读数据源配置。
 *
 * <p>从 Nacos 配置 {@code aics.nl2sql.*} 读取各业务库的连接信息：</p>
 * <pre>
 * aics:
 *   nl2sql:
 *     username: root
 *     password: ${DB_PASSWORD}
 *     urls:
 *       user:      jdbc:mysql://host:3306/user_db?...
 *       product:   jdbc:mysql://host:3306/product_db?...
 *       order:     jdbc:mysql://host:3306/ai_customer_service?...
 *       chat:      jdbc:mysql://host:3306/chat_db?...
 *       knowledge: jdbc:mysql://host:3306/knowledge_db?...
 * </pre>
 *
 * <p>key 为逻辑库标识（供 AI 工具调用时指定），value 为 JDBC URL。</p>
 */
@Data
@ConfigurationProperties(prefix = "aics.nl2sql")
public class Nl2SqlProperties {

    /** 只读账号用户名 */
    private String username;

    /** 只读账号密码 */
    private String password;

    /** 逻辑库标识 -> JDBC URL 映射 */
    private Map<String, String> urls = new LinkedHashMap<>();
}
