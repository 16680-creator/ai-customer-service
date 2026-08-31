package com.aics.product.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * 商品服务声明式缓存配置。
 *
 * <h3>缓存边界</h3>
 * <ul>
 *   <li>{@code product:detail}：商品详情，TTL 30 分钟；更新/删品/扣补库存主动驱逐。</li>
 *   <li>{@code product:categories}：分类列表，TTL 10 分钟；创建分类主动驱逐。</li>
 *   <li>分页商品列表不缓存：参数组合基数高且库存/销量高频变化，命中率低、失效面大。</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class ProductCacheConfig {

    public static final String PRODUCT_DETAIL = "product:detail";
    public static final String PRODUCT_CATEGORIES = "product:categories";

    @Bean
    public RedisCacheConfiguration productDefaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(20));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer productCacheManagerCustomizer() {
        RedisCacheConfiguration base = productDefaultCacheConfiguration();
        return builder -> builder
                .withCacheConfiguration(PRODUCT_DETAIL, base.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(PRODUCT_CATEGORIES, base.entryTtl(Duration.ofMinutes(10)));
    }
}
