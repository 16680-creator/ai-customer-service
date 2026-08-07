package com.aics.order.task;

import com.aics.order.entity.Order;
import com.aics.order.enums.OrderStatus;
import com.aics.order.mapper.OrderMapper;
import com.aics.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时扫描定时任务
 * 兜底机制：周期性扫描超时未支付订单并取消（主路径为 RocketMQ 延迟消息，此处兜底防止消息丢失）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /** 每 5 分钟扫描一次，启动 1 分钟后首次执行 */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void scanExpiredOrders() {
        log.info("定时任务：扫描超时未支付订单...");
        List<Order> expired = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING_PAY.getCode())
                        .lt(Order::getExpireTime, LocalDateTime.now()));
        if (expired.isEmpty()) {
            log.info("定时任务：无超时订单");
            return;
        }
        for (Order order : expired) {
            try {
                orderService.cancelExpiredOrder(order.getOrderNo());
                log.info("定时任务：已取消超时订单 orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                log.error("定时任务：取消订单失败 orderNo={}", order.getOrderNo(), e);
            }
        }
    }
}