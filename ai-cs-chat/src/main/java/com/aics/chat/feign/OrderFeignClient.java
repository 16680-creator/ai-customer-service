package com.aics.chat.feign;

import com.aics.chat.dto.OrderVO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * 订单服务 Feign 客户端（调用 ai-cs-order，经注册中心负载均衡）
 */
@FeignClient(name = "ai-cs-order")
public interface OrderFeignClient {

    /**
     * 查询订单详情
     */
    @GetMapping("/order/{orderNo}")
    Result<OrderVO> getOrderDetail(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable("orderNo") String orderNo);

    /**
     * 查询用户订单列表
     */
    @GetMapping("/order/list")
    Result<List<OrderVO>> listOrders(@RequestHeader("X-User-Id") Long userId);
}