package com.aics.order.client.fallback;

import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.order.client.PayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付服务降级工厂（payClient 熔断开启 / 调用异常时触发）。
 *
 * <p><b>降级语义</b>：closeOrder 本就是「尽力而为」通知（取消/超时关单时使渠道
 * 订单失效），降级记告警并返回 fail，不抛异常——关单主流程优先完成；
 * 调用方 {@code OrderServiceImpl#closePayChannel} 另有 try/catch 双保险。
 * 渠道订单残留由支付服务侧超时兜底关闭。</p>
 */
@Slf4j
@Component
public class PayClientFallbackFactory implements FallbackFactory<PayClient> {

    @Override
    public PayClient create(Throwable cause) {
        log.warn("支付服务调用降级: cause={}", cause.getMessage());
        return new PayClient() {

            @Override
            public Result<Void> closeOrder(Map<String, String> body) {
                log.warn("支付关单通知降级（告警，不阻断关单）: body={}, cause={}", body, cause.getMessage());
                return Result.fail(ResultCode.GATEWAY_SERVICE_UNAVAILABLE);
            }
        };
    }
}
