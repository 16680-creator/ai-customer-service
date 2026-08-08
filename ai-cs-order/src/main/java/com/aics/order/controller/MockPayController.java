package com.aics.order.controller;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.order.enums.OrderStatus;
import com.aics.order.enums.PaymentMethod;
import com.aics.order.pay.channel.MockPayChannel;
import com.aics.order.pay.channel.NotifyContext;
import com.aics.order.service.OrderService;
import com.aics.order.service.PayNotifyService;
import com.aics.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟支付渠道控制器（仅供学习/演示）
 *
 * <p>等价于"真实渠道的收银台 + 异步通知"：
 * <ul>
 *   <li>{@code POST /api/pay/mock/pay}：模拟用户在收银台完成支付，随后触发与真实渠道完全相同的异步通知处理路径（验签 → 幂等更新 → 通知）</li>
 *   <li>{@code POST /api/pay/mock/refund}：模拟渠道退款</li>
 * </ul>
 * 接入真实渠道后，本控制器可删除，流程由对应渠道的回调替代。
 */
@Slf4j
@Tag(name = "模拟支付", description = "模拟支付渠道（学习完整支付流程用）")
@RestController
@RequestMapping("/pay/mock")
@RequiredArgsConstructor
public class MockPayController {

    private final OrderService orderService;
    private final MockPayChannel mockPayChannel;
    private final PayNotifyService payNotifyService;

    @Operation(summary = "模拟支付（用户在收银台完成支付）")
    @PostMapping("/pay")
    public Result<Map<String, String>> mockPay(@RequestHeader("X-User-Id") Long userId,
                                               @RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");
        String result = body.getOrDefault("result", "SUCCESS");

        OrderVO order = orderService.getOrderDetail(userId, orderNo);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或不可支付");
        }

        if ("SUCCESS".equalsIgnoreCase(result)) {
            // 1) 渠道侧标记已支付（等价于用户在微信/支付宝 App 完成支付）
            mockPayChannel.markPaid(orderNo);
            // 2) 触发与真实渠道完全相同的异步通知处理：验签 → 幂等更新订单状态 → 投递通知
            Map<String, String> notifyParams = new LinkedHashMap<>();
            notifyParams.put("orderNo", orderNo);
            notifyParams.put("result", result);
            notifyParams.put("amount", order.getPayAmount() == null ? "0" : order.getPayAmount().toPlainString());
            payNotifyService.processNotify(PaymentMethod.MOCK.getCode(),
                    NotifyContext.builder().params(notifyParams).build());
        } else {
            log.info("[MockPay] 用户取消/支付失败: orderNo={}", orderNo);
        }

        OrderVO updated = orderService.getOrderDetail(userId, orderNo);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", updated.getStatus());
        return Result.success(data);
    }

    @Operation(summary = "模拟退款")
    @PostMapping("/refund")
    public Result<Map<String, String>> mockRefund(@RequestHeader("X-User-Id") Long userId,
                                                  @RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");

        OrderVO order = orderService.getOrderDetail(userId, orderNo);
        if (!OrderStatus.PAID.getCode().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "仅已支付订单可退款");
        }

        // 渠道侧发起退款
        mockPayChannel.refund(orderNo, order.getPayAmount());
        // 本地状态更新 + 库存回补
        orderService.refundOrder(userId, orderNo);

        OrderVO updated = orderService.getOrderDetail(userId, orderNo);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", updated.getStatus());
        return Result.success("退款成功", data);
    }
}