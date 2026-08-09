package com.aics.pay.controller;

import com.aics.common.enums.PaymentMethod;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.pay.channel.PayChannel;
import com.aics.pay.channel.PayChannelFactory;
import com.aics.pay.channel.PayContext;
import com.aics.pay.channel.PayResult;
import com.aics.pay.client.OrderPayClient;
import com.aics.pay.dto.OrderPayDetailVO;
import com.aics.pay.service.PayCompensationService;
import com.aics.pay.service.PayNotifyService;
import com.aics.pay.service.PayTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付接口：渠道下单 + 状态查询 + 关单 + 补偿对账
 *
 * <p>订单信息通过 {@link OrderPayClient} 从订单服务获取；支付状态变更由订单服务落库。
 */
@Slf4j
@Tag(name = "支付", description = "支付下单/查询/关单/补偿（独立支付服务）")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private static final String STATUS_PENDING_PAY = "PENDING_PAY";

    private final PayChannelFactory payChannelFactory;
    private final PayTransactionService payTransactionService;
    private final OrderPayClient orderPayClient;
    private final PayNotifyService payNotifyService;
    private final PayCompensationService payCompensationService;

    @Operation(summary = "创建支付（渠道下单）")
    @PostMapping("/create")
    public Result<Map<String, String>> createPayment(@RequestHeader("X-User-Id") Long userId,
                                                     @RequestBody(required = false) Map<String, String> body) {
        String orderNo = body == null ? null : body.get("orderNo");
        String paymentMethod = body == null ? PaymentMethod.MOCK.getCode()
                : body.getOrDefault("paymentMethod", PaymentMethod.MOCK.getCode());

        OrderPayDetailVO order = orderPayClient.getOrderDetail(orderNo);
        if (order == null || !userId.equals(order.getUserId())
                || !STATUS_PENDING_PAY.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或不可支付");
        }

        PayChannel channel = payChannelFactory.getChannel(paymentMethod);
        PayResult payResult = channel.createPayment(PayContext.builder()
                .orderNo(orderNo)
                .payAmount(order.getPayAmount())
                .subject("订单 " + orderNo)
                .notifyUrl("/api/pay/callback/" + paymentMethod)
                .build());

        payTransactionService.createOrUpdatePending(orderNo, userId, paymentMethod, order.getPayAmount());

        Map<String, String> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("payType", payResult.getPayType());
        data.put("payUrl", payResult.getPayUrl() == null ? "" : payResult.getPayUrl());
        data.put("codeUrl", payResult.getCodeUrl() == null ? "" : payResult.getCodeUrl());
        data.put("payAmount", order.getPayAmount() == null ? "0" : order.getPayAmount().toPlainString());
        data.put("status", order.getStatus());
        data.put("expireTime", order.getExpireTime() == null ? "" : order.getExpireTime());
        return Result.success("支付下单成功", data);
    }

    @Operation(summary = "查询支付/订单状态（含查单兜底）")
    @GetMapping("/status/{orderNo}")
    public Result<Map<String, String>> queryPayStatus(@RequestHeader("X-User-Id") Long userId,
                                                      @PathVariable("orderNo") String orderNo) {
        OrderPayDetailVO order = orderPayClient.getOrderDetail(orderNo);

        // 查单兜底：待支付时主动向渠道查询，渠道已支付则通知订单服务落库
        if (order != null && STATUS_PENDING_PAY.equals(order.getStatus())) {
            try {
                String method = order.getPaymentMethod() == null
                        ? PaymentMethod.MOCK.getCode() : order.getPaymentMethod();
                payNotifyService.syncByQuery(orderNo, method);
                order = orderPayClient.getOrderDetail(orderNo);
            } catch (Exception e) {
                log.warn("查单兜底异常: orderNo={}, err={}", orderNo, e.getMessage());
            }
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", order.getStatus());
        data.put("payAmount", order.getPayAmount() == null ? "0" : order.getPayAmount().toPlainString());
        return Result.success(data);
    }

    @Operation(summary = "关闭订单支付（订单取消/超时时由订单服务回调）")
    @PostMapping("/close")
    public Result<Void> closeOrder(@RequestBody(required = false) Map<String, String> body) {
        String orderNo = body == null ? null : body.get("orderNo");
        String method = body == null ? PaymentMethod.MOCK.getCode()
                : body.getOrDefault("paymentMethod", PaymentMethod.MOCK.getCode());
        payNotifyService.closeOrder(orderNo, method);
        return Result.success();
    }

    @Operation(summary = "支付补偿/对账（查单兜底 + 一致性对账）")
    @PostMapping("/compensate")
    public Result<Map<String, Object>> compensate() {
        return Result.success(payCompensationService.compensate());
    }
}