package com.aics.chat.modelrouter;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RouteDecision {
    // 学习点：路由结果同时给出选中模型和完整 fallback 链——调用方按链重试/降级，无需二次路由，整次调用决策保持一致
    private final String selectedModelId;
    private final List<String> fallbackChain;
    private final RouteReason reason;
}
