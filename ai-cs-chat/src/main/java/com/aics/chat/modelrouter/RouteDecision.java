package com.aics.chat.modelrouter;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RouteDecision {
    private final String selectedModelId;
    private final List<String> fallbackChain;
    private final RouteReason reason;
}
