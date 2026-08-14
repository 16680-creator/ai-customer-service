package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuotaRouteProperties {
    private boolean enabled = true;
    private String overLimitFallbackTier = "cheap";
}
