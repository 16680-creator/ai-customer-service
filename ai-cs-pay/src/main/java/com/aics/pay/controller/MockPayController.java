package com.aics.pay.controller;

import com.aics.common.enums.PaymentMethod;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.pay.channel.MockPayChannel;
import com.aics.pay.channel.NotifyContext;
import com.aics.pay.client.OrderPayClient;
import com.aics.pay.dto.OrderPayDetailVO;
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
 * 模拟支付渠道控制器（学习/演示）：模拟收银台 + 触发与真实渠道相同的通知处理
 */
@Slf4j
@Tag(name = "模拟支付", description = "模拟支付渠道（学习完整支付流程用）")
@RestController
@RequestMapping("/pay/mock")
@RequiredArgsConstructor
public class MockPayController {

    private final OrderPayClient orderPayClient;
    private final MockPayChannel mockPayChannel;
    private final PayNotifyService payNotifyService;
    private final PayTransactionService payTransactionService;

    @Operation(summary = "模拟支付（用户在收银台完成支付）")
    @PostMapping("/pay")
    public Result<Map<String, String>> mockPay(@RequestHeader("X-User-Id") Long userId,
                                               @RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");
        String result = body.getOrDefault("result", "SUCCESS");

        OrderPayDetailVO order = orderPayClient.getOrderDetail(orderNo);
        if (order == null || !userId.equals(order.getUserId()) || !"PENDING_PAY".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或不可支付");
        }

        if ("SUCCESS".equalsIgnoreCase(result)) {
            // 1) 渠道侧标记已支付
            mockPayChannel.markPaid(orderNo);
            // 2) 触发与真实渠道完全相同的通知处理：验签 → 记录流水 → 通知订单服务确认支付
            Map<String, String> notifyParams = new LinkedHashMap<>();
            notifyParams.put("orderNo", orderNo);
            notifyParams.put("result", result);
            notifyParams.put("amount", order.getPayAmount() == null ? "0" : order.getPayAmount().toPlainString());
            payNotifyService.processNotify(PaymentMethod.MOCK.getCode(),
                    NotifyContext.builder().params(notifyParams).build());
        } else {
            log.info("[MockPay] 用户取消/支付失败: orderNo={}", orderNo);
        }

        OrderPayDetailVO updated = orderPayClient.getOrderDetail(orderNo);
        return Result.success(Map.of("orderNo", orderNo, "status", updated.getStatus()));
    }

    @Operation(summary = "模拟退款")
    @PostMapping("/refund")
    public Result<Map<String, String>> mockRefund(@RequestHeader("X-User-Id") Long userId,
                                                  @RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");

        OrderPayDetailVO order = orderPayClient.getOrderDetail(orderNo);
        if (order == null || !userId.equals(order.getUserId()) || !"PAID".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "仅已支付订单可退款");
        }

        // 渠道侧发起退款 + 流水退款中
        mockPayChannel.refund(orderNo, order.getPayAmount());
        payTransactionService.markRefunding(orderNo);

        // 通知订单服务退款确认（订单 → 已退款 + 回补库存）
        orderPayClient.refundConfirm(orderNo);
        payTransactionService.markRefunded(orderNo);

        OrderPayDetailVO updated = orderPayClient.getOrderDetail(orderNo);
        return Result.success("退款成功", Map.of("orderNo", orderNo, "status", updated.getStatus()));
    }
}