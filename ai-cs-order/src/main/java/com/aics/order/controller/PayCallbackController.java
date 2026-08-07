package com.aics.order.controller;

import com.aics.order.service.OrderService;
import com.aics.order.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付回调控制器（内部接口，由支付渠道异步通知）
 */
@Slf4j
@Tag(name = "支付回调", description = "支付渠道异步通知处理")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayCallbackController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @Operation(summary = "支付结果回调")
    @PostMapping("/callback/{paymentMethod}")
    public Map<String, String> payCallback(@PathVariable("paymentMethod") String paymentMethod,
                                           @RequestBody Map<String, String> params) {
        String orderNo = params.get("orderNo");
        log.info("收到支付回调: method={}, orderNo={}", paymentMethod, orderNo);

        // 验签
        if (!paymentService.verifyCallback(paymentMethod, params.toString())) {
            log.warn("支付回调验签失败: orderNo={}", orderNo);
            return Map.of("code", "FAIL", "message", "验签失败");
        }

        // 处理支付成功
        orderService.handlePayCallback(orderNo, paymentMethod);

        return Map.of("code", "SUCCESS", "message", "OK");
    }
}
