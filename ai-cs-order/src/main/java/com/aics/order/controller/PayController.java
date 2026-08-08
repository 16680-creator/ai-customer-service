package com.aics.order.controller;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.order.enums.OrderStatus;
import com.aics.order.enums.PaymentMethod;
import com.aics.order.pay.channel.PayChannel;
import com.aics.order.pay.channel.PayChannelFactory;
import com.aics.order.pay.channel.PayContext;
import com.aics.order.pay.channel.PayResult;
import com.aics.order.service.OrderService;
import com.aics.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付接口：渠道下单 + 状态查询
 *
 * <p>通过 {@link PayChannelFactory} 按支付方式路由到对应 {@link PayChannel} 实现，
 * 新增支付方式（支付宝/微信/银联/聚合）无需改动本控制器。
 */
@Slf4j
@Tag(name = "支付", description = "支付下单与查询（渠道由 PayChannel 抽象，可扩展）")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayChannelFactory payChannelFactory;
    private final OrderService orderService;

    @Operation(summary = "创建支付（渠道下单）")
    @PostMapping("/create")
    public Result<Map<String, String>> createPayment(@RequestHeader("X-User-Id") Long userId,
                                                     @RequestBody(required = false) Map<String, String> body) {
        String orderNo = body == null ? null : body.get("orderNo");
        String paymentMethod = body == null ? PaymentMethod.MOCK.getCode()
                : body.getOrDefault("paymentMethod", PaymentMethod.MOCK.getCode());

        OrderVO order = orderService.getOrderDetail(userId, orderNo);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或不可支付");
        }

        PayChannel channel = payChannelFactory.getChannel(paymentMethod);
        PayResult payResult = channel.createPayment(PayContext.builder()
                .orderNo(orderNo)
                .payAmount(order.getPayAmount())
                .subject("订单 " + orderNo)
                .notifyUrl("/api/pay/callback/" + paymentMethod)
                .build());

        Map<String, String> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("payType", payResult.getPayType());
        data.put("payUrl", payResult.getPayUrl() == null ? "" : payResult.getPayUrl());
        data.put("codeUrl", payResult.getCodeUrl() == null ? "" : payResult.getCodeUrl());
        data.put("payAmount", order.getPayAmount() == null ? "0" : order.getPayAmount().toPlainString());
        data.put("status", order.getStatus());
        return Result.success("支付下单成功", data);
    }

    @Operation(summary = "查询支付/订单状态")
    @GetMapping("/status/{orderNo}")
    public Result<Map<String, String>> queryPayStatus(@RequestHeader("X-User-Id") Long userId,
                                                      @PathVariable("orderNo") String orderNo) {
        OrderVO order = orderService.getOrderDetail(userId, orderNo);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", order.getStatus());
        data.put("payAmount", order.getPayAmount() == null ? "0" : order.getPayAmount().toPlainString());
        return Result.success(data);
    }
}