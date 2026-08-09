package com.aics.order.controller;

import com.aics.common.result.Result;
import com.aics.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单-支付内部接口（由独立支付服务调用，不走网关鉴权）
 */
@Tag(name = "订单支付内部接口", description = "支付服务回调订单服务使用")
@RestController
@RequestMapping("/order/pay")
@RequiredArgsConstructor
public class PayOrderController {

    private final OrderService orderService;

    @Operation(summary = "支付确认（幂等 + 金额校验）")
    @PostMapping("/confirm")
    public Result<Void> confirmPay(@RequestBody Map<String, Object> body) {
        String orderNo = (String) body.get("orderNo");
        String paymentMethod = (String) body.get("paymentMethod");
        String amountStr = body.get("amount") == null ? null : String.valueOf(body.get("amount"));
        BigDecimal amount = amountStr == null || amountStr.isBlank() ? null : new BigDecimal(amountStr);
        String tradeNo = (String) body.get("tradeNo");
        orderService.confirmPay(orderNo, paymentMethod, amount, tradeNo);
        return Result.success();
    }

    @Operation(summary = "退款确认（已支付 → 已退款 + 回补库存）")
    @PostMapping("/refund-confirm")
    public Result<Void> refundConfirm(@RequestBody Map<String, Object> body) {
        orderService.refundConfirm((String) body.get("orderNo"));
        return Result.success();
    }

    @Operation(summary = "订单支付信息（状态/金额/过期时间）")
    @GetMapping("/detail/{orderNo}")
    public Result<Map<String, Object>> orderPayDetail(@PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.getOrderPayDetail(orderNo));
    }
}