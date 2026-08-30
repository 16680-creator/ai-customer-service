package com.aics.order.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端显式装配。
 *
 * <p>学习要点（踩坑）：redisson-spring-boot-starter 会从 {@code spring.data.redis.password}
 * 读取密码——YAML 无法表达「无密码」（null 发布后还原为空串），Redisson 拿到空串会发
 * {@code AUTH}，被未设密码的 Redis 拒绝（{@code ERR AUTH called without any password configured}）。
 * Lettuce 对空串自动跳过 AUTH，所以只有 Redisson 受影响。</p>
 *
 * <p>自定义 Bean 后 starter 的 {@code @ConditionalOnMissingBean(RedissonClient.class)}
 * 自动让位；这里不设置 password，Redis 无 AUTH 也能正常工作。</p>
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.database:0}") int database,
            @Value("${spring.data.redis.connect-timeout:15000}") int connectTimeout,
            @Value("${spring.data.redis.timeout:15000}") int timeout) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout)
                .setRetryAttempts(5)
                .setRetryInterval(3000);
        // 注意：不调用 setPassword —— Redis 未启用 AUTH
        return Redisson.create(config);
    }
}
