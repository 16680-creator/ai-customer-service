package com.aics.order.client;

import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 支付服务 Feign 客户端（订单服务 -> 支付服务，经 Nacos 注册中心负载均衡）。
 *
 * <p>注意：关单通知属「尽力而为」语义（取消/超时关单时使渠道订单失效），
 * 调用方 {@code OrderServiceImpl#closePayChannel} 自行吞异常 + 告警日志，
 * 不因支付服务不可用阻断关单主流程。</p>
 */
@FeignClient(name = "ai-cs-pay", contextId = "payClient", path = "/pay",
        fallbackFactory = com.aics.order.client.fallback.PayClientFallbackFactory.class)
public interface PayClient {

    /**
     * 通知支付服务关闭渠道订单（使支付二维码失效）。
     *
     * @param body 含 orderNo（订单号）、paymentMethod（支付渠道）
     */
    @PostMapping("/close")
    Result<Void> closeOrder(@RequestBody Map<String, String> body);
}
