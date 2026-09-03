package com.aics.common.idempotent;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 幂等组件自动装配：
 * <ul>
 *   <li>{@code @ConditionalOnClass(StringRedisTemplate)} —— 依赖声明为 optional，
 *       未引入 Redis 的服务直接跳过整个装配；</li>
 *   <li>{@code aics.idempotent.enabled=false} 可整体关闭；</li>
 *   <li>业务方自定义 {@link IdempotentAspect} Bean 时自动让位。</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "aics.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IdempotentProperties.class)
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(StringRedisTemplate redisTemplate,
                                             IdempotentProperties properties) {
        return new IdempotentAspect(redisTemplate, properties);
    }
}
