package com.aics.common.idempotent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 幂等组件自动装配契约测试（与 CommonAutoConfigurationTest 同一套路）：
 * 验证 imports 条件装配、enabled 开关与用户自定义 Bean 让位。
 */
class IdempotentAutoConfigurationTest {

    @Test
    @DisplayName("有 Redis 依赖 - 默认自动装配 IdempotentAspect")
    void shouldAutoConfigureWhenRedisPresent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IdempotentAutoConfiguration.class))
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> assertThat(context).hasSingleBean(IdempotentAspect.class));
    }

    @Test
    @DisplayName("aics.idempotent.enabled=false - 整体不装配")
    void shouldBackOffWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IdempotentAutoConfiguration.class))
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withPropertyValues("aics.idempotent.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(IdempotentAspect.class));
    }

    @Test
    @DisplayName("业务方自定义 IdempotentAspect - 自动配置让位")
    void shouldBackOffForUserDefinedAspect() {
        IdempotentAspect custom = new IdempotentAspect(
                mock(StringRedisTemplate.class), new IdempotentProperties());
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IdempotentAutoConfiguration.class))
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean("customIdempotentAspect", IdempotentAspect.class, () -> custom)
                .run(context -> assertThat(context).getBean(IdempotentAspect.class).isSameAs(custom));
    }
}
