package com.aics.chat.modelrouter;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class RouteRequest {
    // 学习点：路由输入收敛为不可变请求对象——后续加维度（用户等级/上下文需求）只改契约，避免 route() 参数无节制膨胀
    private final ModelScenario scenario;
    private final boolean quotaExceeded;
    private final Set<ModelCapability> requiredCapabilities;
}
