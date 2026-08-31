package com.aics.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关限流配置（{@code aics.gateway.rate-limit.*}）。
 *
 * <h3>学习要点（技术：动态配置 @ConfigurationProperties 重绑定）</h3>
 * <ul>
 *   <li>选 {@code @ConfigurationProperties} 而非 {@code @Value} + {@code @RefreshScope}：
 *       Nacos 配置变更触发 ContextRefresher 后，spring-cloud-context 的
 *       ConfigurationPropertiesRebinder 会<strong>自动重绑定</strong>所有
 *       ConfigurationProperties Bean（销毁后重新 bind 现有实例），
 *       无需给使用方加 RefreshScope；{@code @Value} 字段则必须加 RefreshScope 才会刷新。</li>
 *   <li>默认值写在字段初始化处：配置缺失时行为可预期。</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "aics.gateway.rate-limit")
public class RateLimitProperties {

    /**
     * 旧版本地内存限流总开关。默认关闭（false）：
     * 内存限流配额不跨实例共享，多实例部署形同虚设，已由 RouteConfig 每路由的
     * RequestRateLimiter（Redis 分布式令牌桶）替代；Redis 不可用时改回 true 兜底。
     */
    private boolean enabled = false;

    /** 限流算法：sliding-window（默认）/ token-bucket（仅本地内存限流生效） */
    private String algorithm = "sliding-window";

    /** 滑动窗口内允许的请求总数 */
    private int requests = 60;

    /** 滑动窗口长度（秒） */
    private int windowSeconds = 60;

    /** token-bucket 算法的每秒补充令牌数（= 每用户长期 QPS 上限） */
    private int qps = 5;

    /** 分布式限流（RequestRateLimiter）：每用户每秒补充令牌数 */
    private int replenishRate = 5;

    /** 分布式限流（RequestRateLimiter）：桶容量（允许的短时突发上限） */
    private int burstCapacity = 10;
}
