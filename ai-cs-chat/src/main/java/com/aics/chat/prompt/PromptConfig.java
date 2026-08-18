package com.aics.chat.prompt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prompt 配置化模块装配。
 *
 * <p>将 {@link PromptRouter} 与 {@link PromptRegistry} 注册为 Bean。
 * {@link PromptRegistry} 实现 {@link org.springframework.beans.factory.InitializingBean}，
 * 在 Bean 初始化时完成配置校验（activeVersion 存在性、灰度权重）。</p>
 */
@Configuration
public class PromptConfig {

    @Bean
    public PromptRouter promptRouter() {
        return new PromptRouter();
    }

    @Bean
    public PromptRegistry promptRegistry(PromptProperties properties, PromptRouter router) {
        return new PromptRegistry(properties, router);
    }
}
