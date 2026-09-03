package com.aics.common.idempotent;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 幂等组件配置项（aics.idempotent.*）。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "aics.idempotent")
public class IdempotentProperties {

    /** 是否启用幂等切面（自动装配开关，默认开启；无 Redis 依赖的服务因 @ConditionalOnClass 自动跳过） */
    private boolean enabled = true;

    /** Redis key 命名空间前缀，便于运维统一扫描/清理 */
    private String keyPrefix = "aics:idem:";
}
