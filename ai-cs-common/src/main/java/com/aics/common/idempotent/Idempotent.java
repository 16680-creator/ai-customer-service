package com.aics.common.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解：标注在回调/消费/写操作方法上，以 Redis SET NX 挡住并发重放与重复投递。
 *
 * <p>语义（三层幂等中的第一层"加速挡"，DB 唯一键仍是终审）：
 * <ul>
 *   <li>首次调用：占位成功，放行业务方法；业务成功后 key 保留至 TTL 到期，窗口内重复请求被拒绝；</li>
 *   <li>业务异常：立即释放 key，允许渠道重试/MQ 重投；</li>
 *   <li>重复调用：抛 {@link IdempotentRejectException}（code 409）；</li>
 *   <li>Redis 故障：fail-open 放行——幂等降级，不能因为去重层故障阻断业务。</li>
 * </ul>
 *
 * <p>key 为 SpEL 表达式，可用方法参数名（如 {@code #orderNo}）或位置参数（{@code #p0}）。
 * 依赖编译期 {@code -parameters}（根 POM 已开启 maven.compiler.parameters）。
 *
 * <p>示例：{@code @Idempotent(key = "'pay:callback:' + #orderNo", ttlSeconds = 300)}
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等键 SpEL 表达式，最终 Redis key = keyPrefix + 解析结果 */
    String key();

    /** 成功后去重窗口（秒），窗口内重复请求直接拒绝 */
    long ttlSeconds() default 300;

    /** 重复请求被拒绝时的提示语 */
    String message() default "重复请求，请勿重复提交";
}
