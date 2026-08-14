package com.aics.chat.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户角色解析器（3.2 F2 工具授权 / F5 RAG ACL 共用）。
 *
 * <p>角色来源：{@link SecurityProperties#getUserRoles()} 配置映射（userId -&gt; role），
 * 未配置的用户默认角色 {@code USER}。当前以确定性配置驱动（便于 BDD 场景验证），
 * 后续可替换为用户服务的角色接口。</p>
 */
@Component
@RequiredArgsConstructor
public class UserRoleResolver {

    /** 默认角色 */
    public static final String DEFAULT_ROLE = "USER";

    private final SecurityProperties properties;

    /**
     * 解析用户角色：配置命中返回配置角色，否则返回默认 USER。
     */
    public String resolve(Long userId) {
        if (properties.getUserRoles() != null && userId != null
                && properties.getUserRoles().containsKey(userId)) {
            return properties.getUserRoles().get(userId);
        }
        return DEFAULT_ROLE;
    }
}
