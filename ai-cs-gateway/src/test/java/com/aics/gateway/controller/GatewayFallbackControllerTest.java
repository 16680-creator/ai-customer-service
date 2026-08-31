package com.aics.gateway.controller;

import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 网关降级端点测试：熔断兜底响应必须是统一 Result 结构（前端可识别），code=503。
 */
class GatewayFallbackControllerTest {

    private final GatewayFallbackController controller = new GatewayFallbackController();

    @Test
    @DisplayName("降级响应 - 统一 Result 结构 + 503 语义")
    void fallbackShouldReturnUnifiedResult() {
        Result<Void> result = controller.fallback();
        assertEquals(ResultCode.GATEWAY_SERVICE_UNAVAILABLE.getCode(), result.getCode());
        assertEquals(ResultCode.GATEWAY_SERVICE_UNAVAILABLE.getMessage(), result.getMessage());
    }
}
