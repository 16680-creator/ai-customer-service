package com.aics.chat.modelrouter;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class RouteRequest {
    private final ModelScenario scenario;
    private final boolean quotaExceeded;
    private final Set<ModelCapability> requiredCapabilities;
}
