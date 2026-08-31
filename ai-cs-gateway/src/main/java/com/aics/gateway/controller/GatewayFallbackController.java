package com.aics.gateway.controller;

import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网关统一降级端点（CircuitBreaker filter 的 fallbackUri 目标）。
 *
 * <p>下游服务熔断/不可用时，请求被 forward 到这里，返回统一 {@link Result} 结构的 503，
 * 替代裸 500 错误页——前端/调用方拿到的是可识别的业务响应格式。
 * forward 是网关内部跳转，不占用下游服务资源。</p>
 */
@RestController
public class GatewayFallbackController {

    @RequestMapping("/gateway-fallback")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> fallback() {
        return Result.fail(ResultCode.GATEWAY_SERVICE_UNAVAILABLE);
    }
}
