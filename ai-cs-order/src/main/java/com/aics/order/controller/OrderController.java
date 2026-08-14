package com.aics.order.controller;

import com.aics.common.result.Result;
import com.aics.order.dto.OrderCreateDTO;
import com.aics.order.service.OrderService;
import com.aics.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单控制器
 */
@Tag(name = "订单管理", description = "订单创建、查询、取消、支付重试")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "提交订单")
    @PostMapping("/create")
    public Result<OrderVO> createOrder(@RequestHeader("X-User-Id") Long userId,
                                       @Valid @RequestBody OrderCreateDTO dto) {
        OrderVO vo = orderService.createOrder(userId, dto.getCartItemIds(), dto.getCouponId(), dto.getPaymentMethod());
        return Result.success("下单成功", vo);
    }

    @Operation(summary = "查询用户订单列表（AI 客服调用）")
    @GetMapping("/list")
    public Result<java.util.List<OrderVO>> listOrders(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.listOrders(userId));
    }
    @Operation(summary = "查询订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> getOrderDetail(@RequestHeader("X-User-Id") Long userId,
                                          @PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.getOrderDetail(userId, orderNo));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable("orderNo") String orderNo) {
        orderService.cancelOrder(userId, orderNo);
        return Result.success("订单已取消", null);
    }


}
