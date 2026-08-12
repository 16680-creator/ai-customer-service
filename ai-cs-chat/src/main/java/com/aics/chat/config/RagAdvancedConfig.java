package com.aics.chat.config;

import com.aics.chat.rag.graph.GraphProperties;
import com.aics.chat.rag.retrieve.RagRetrieveProperties;
import com.aics.chat.rag.rewrite.QueryRewriteProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 进阶功能配置装配（Hybrid 检索 / 查询改写 / 图谱 / 评估阈值）。
 */
@Configuration
@EnableConfigurationProperties({
        RagRetrieveProperties.class,
        QueryRewriteProperties.class,
        GraphProperties.class
})
public class RagAdvancedConfig {
}