package com.aics.common.idempotent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 幂等切面行为测试：不依赖真实 Redis，用 Mockito 模拟 StringRedisTemplate，
 * 通过 AspectJProxyFactory 织入切面调用真实注解方法。
 */
class IdempotentAspectTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    private final IdempotentProperties properties = new IdempotentProperties();

    private final DemoService target = new DemoService();
    private final DemoService service = proxy(target);

    /** 被测目标：SpEL 用参数名 #orderNo（依赖 -parameters 编译标志） */
    static class DemoService {
        int calls;

        @Idempotent(key = "'pay:callback:' + #orderNo", ttlSeconds = 60)
        public String handle(String orderNo) {
            calls++;
            return "ok:" + orderNo;
        }

        @Idempotent(key = "'boom:' + #orderNo")
        public String fail(String orderNo) {
            throw new IllegalStateException("业务失败");
        }
    }

    private DemoService proxy(DemoService t) {
        AspectJProxyFactory factory = new AspectJProxyFactory(t);
        factory.addAspect(new IdempotentAspect(redisTemplate, properties));
        return factory.getProxy();
    }

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("首次调用：占位成功放行业务，key = 前缀 + SpEL 解析结果")
    void firstCallShouldProceed() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertThat(service.handle("O1")).isEqualTo("ok:O1");

        verify(valueOps).setIfAbsent(eq("aics:idem:pay:callback:O1"), eq("1"),
                eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("重复调用：拒绝并抛 409，业务方法不被触达")
    void duplicateCallShouldReject() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> service.handle("O1"))
                .isInstanceOf(IdempotentRejectException.class)
                .hasMessage("重复请求，请勿重复提交");

        assertThat(target.calls).isZero();
    }

    @Test
    @DisplayName("业务异常：释放 key 允许重试，异常原样上抛")
    void businessFailureShouldReleaseKey() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertThatThrownBy(() -> service.fail("O2")).isInstanceOf(IllegalStateException.class);

        verify(redisTemplate).delete("aics:idem:boom:O2");
    }

    @Test
    @DisplayName("业务成功：不删除 key（保留至 TTL 到期形成去重窗口）")
    void successShouldKeepKey() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        service.handle("O3");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Redis 故障：fail-open 放行，业务不受去重层故障影响")
    void redisFailureShouldFailOpen() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new DataAccessResourceFailureException("redis down"));

        assertThat(service.handle("O4")).isEqualTo("ok:O4");
        assertThat(target.calls).isEqualTo(1);
    }

    @Test
    @DisplayName("自定义 keyPrefix：命名空间可配置")
    void customPrefixShouldApply() {
        properties.setKeyPrefix("test:idem:");
        DemoService proxied = proxy(new DemoService());
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertThat(proxied.handle("O5")).isEqualTo("ok:O5");

        verify(valueOps).setIfAbsent(eq("test:idem:pay:callback:O5"), anyString(), any(Duration.class));
    }
}
