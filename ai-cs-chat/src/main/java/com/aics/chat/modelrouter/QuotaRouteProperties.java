package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuotaRouteProperties {
    // 设计要点：配额降级保留独立开关和可配置档位——成本治理要能一键关闭，且“便宜档”由部署环境决定而不是硬编码
    private boolean enabled = true;
    private String overLimitFallbackTier = "cheap";
}
