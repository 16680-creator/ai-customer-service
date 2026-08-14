package com.aics.knowledge.config;

import com.aics.knowledge.ops.OpsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库运营配置装配。
 */
@Configuration
@EnableConfigurationProperties(OpsProperties.class)
public class OpsConfig {
}