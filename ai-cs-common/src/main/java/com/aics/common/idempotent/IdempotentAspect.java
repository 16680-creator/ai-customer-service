package com.aics.common.idempotent;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 幂等切面：@Idempotent 注解的执行器，以 Redis SET NX EX 实现分布式"首次占位"。
 *
 * <p>失败语义（见 {@link Idempotent} javadoc）：业务异常释放 key 允许重试；
 * Redis 异常 fail-open——去重层是加速挡而非权威，权威在业务状态检查与 DB 唯一键。
 *
 * <p>注意：方法内若自带 {@code @Transactional}，占位发生在事务提交前——
 * 这正是想要的行为：同 key 并发请求在占位期间直接拒绝，而不是等到锁在行上排队。
 */
@Aspect
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);
    private static final String TOKEN = "1";

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    private final StringRedisTemplate redisTemplate;
    private final IdempotentProperties properties;

    /** SpEL 表达式解析结果按注解 key 表达式缓存，避免每次请求重复解析 */
    private final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    public IdempotentAspect(StringRedisTemplate redisTemplate, IdempotentProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 占位在事务内不会随回滚自动释放：失败路径显式删除（见下方 catch），此处仅提示
            log.debug("幂等方法存在活跃事务，失败时将主动释放 key: {}", idempotent.key());
        }
        String redisKey = properties.getKeyPrefix() + resolveKey(joinPoint, idempotent);
        Duration ttl = Duration.ofSeconds(idempotent.ttlSeconds());

        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        boolean acquired;
        try {
            acquired = Boolean.TRUE.equals(ops.setIfAbsent(redisKey, TOKEN, ttl));
        } catch (RuntimeException e) {
            log.warn("幂等组件 Redis 异常，fail-open 放行 key={}", redisKey, e);
            return joinPoint.proceed();
        }
        if (!acquired) {
            throw new IdempotentRejectException(idempotent.message());
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            tryRelease(redisKey);
            throw t;
        }
    }

    /** 业务失败立即释放占位，让渠道重试/MQ 重投能够再次进入 */
    private void tryRelease(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (RuntimeException e) {
            log.warn("幂等组件释放 key 失败（等待 TTL 到期自然失效）: {}", redisKey, e);
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = PARAMETER_NAMES.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            String name = (parameterNames != null && i < parameterNames.length)
                    ? parameterNames[i] : "p" + i;
            context.setVariable(name, args[i]);
        }
        Expression expression = expressionCache.computeIfAbsent(idempotent.key(), PARSER::parseExpression);
        String key = expression.getValue(context, String.class);
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("幂等 key 解析结果为空: " + idempotent.key());
        }
        return key;
    }
}
