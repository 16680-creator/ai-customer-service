package com.aics.chat.service;

import com.aics.chat.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 订单查询服务（Tool Calling）
 * 当用户询问订单信息时，AI 会自动调用此服务查询订单
 */
@Slf4j
@Service
public class OrderQueryService {

    /** 模拟订单数据库 */
    private static final Map<String, Order> ORDER_DB = new HashMap<>();

    static {
        // 初始化模拟数据
        ORDER_DB.put("ORD20260720001", Order.builder()
                .orderId("ORD20260720001")
                .userId("user_10086")
                .status("SHIPPED")
                .statusDesc("已发货")
                .items(List.of(
                        Order.OrderItem.builder()
                                .productId("P001")
                                .productName("Apple iPhone 15 Pro Max 256GB")
                                .spec("原色钛金属")
                                .price(new BigDecimal("9999.00"))
                                .quantity(1)
                                .subtotal(new BigDecimal("9999.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("9999.00"))
                .shippingAddress("上海市浦东新区张江高科技园区")
                .receiverName("张三")
                .receiverPhone("138****8888")
                .expressCompany("顺丰速运")
                .expressNo("SF1234567890")
                .createTime(LocalDateTime.of(2026, 7, 20, 10, 30, 0))
                .payTime(LocalDateTime.of(2026, 7, 20, 10, 32, 0))
                .shipTime(LocalDateTime.of(2026, 7, 21, 14, 0, 0))
                .remark("请轻拿轻放")
                .build());

        ORDER_DB.put("ORD20260721002", Order.builder()
                .orderId("ORD20260721002")
                .userId("user_10086")
                .status("DELIVERED")
                .statusDesc("已送达")
                .items(List.of(
                        Order.OrderItem.builder()
                                .productId("P002")
                                .productName("AirPods Pro 2 (USB-C)")
                                .spec("白色")
                                .price(new BigDecimal("1899.00"))
                                .quantity(1)
                                .subtotal(new BigDecimal("1899.00"))
                                .build(),
                        Order.OrderItem.builder()
                                .productId("P003")
                                .productName("MagSafe 充电器")
                                .spec("标准版")
                                .price(new BigDecimal("399.00"))
                                .quantity(2)
                                .subtotal(new BigDecimal("798.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("2697.00"))
                .shippingAddress("上海市浦东新区张江高科技园区")
                .receiverName("张三")
                .receiverPhone("138****8888")
                .expressCompany("京东物流")
                .expressNo("JD9876543210")
                .createTime(LocalDateTime.of(2026, 7, 21, 9, 15, 0))
                .payTime(LocalDateTime.of(2026, 7, 21, 9, 16, 0))
                .shipTime(LocalDateTime.of(2026, 7, 22, 8, 30, 0))
                .completeTime(LocalDateTime.of(2026, 7, 23, 11, 20, 0))
                .build());

        ORDER_DB.put("ORD20260722003", Order.builder()
                .orderId("ORD20260722003")
                .userId("user_10010")
                .status("PAID")
                .statusDesc("已支付待发货")
                .items(List.of(
                        Order.OrderItem.builder()
                                .productId("P004")
                                .productName("MacBook Pro 14英寸 M3 Pro")
                                .spec("深空黑/18GB/512GB")
                                .price(new BigDecimal("16999.00"))
                                .quantity(1)
                                .subtotal(new BigDecimal("16999.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("16999.00"))
                .shippingAddress("北京市海淀区中关村软件园")
                .receiverName("李四")
                .receiverPhone("139****6666")
                .createTime(LocalDateTime.of(2026, 7, 22, 16, 45, 0))
                .payTime(LocalDateTime.of(2026, 7, 22, 16, 48, 0))
                .remark("需要开发票")
                .build());

        ORDER_DB.put("ORD20260723004", Order.builder()
                .orderId("ORD20260723004")
                .userId("user_10010")
                .status("CANCELLED")
                .statusDesc("已取消")
                .items(List.of(
                        Order.OrderItem.builder()
                                .productId("P005")
                                .productName("iPad Air M2")
                                .spec("星光色/256GB/WiFi")
                                .price(new BigDecimal("5499.00"))
                                .quantity(1)
                                .subtotal(new BigDecimal("5499.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("5499.00"))
                .shippingAddress("北京市海淀区中关村软件园")
                .receiverName("李四")
                .receiverPhone("139****6666")
                .createTime(LocalDateTime.of(2026, 7, 23, 8, 0, 0))
                .remark("用户主动取消")
                .build());
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 根据订单号查询订单
     *
     * @param orderId 订单编号，格式如 ORD20260720001
     * @return 订单详情 JSON 字符串
     */
    @Tool(description = "根据订单号查询订单详情，包括订单状态、商品信息、金额、物流等")
    public String queryOrderByOrderId(
            @ToolParam(description = "订单编号，格式如 ORD20260720001") String orderId) {

        log.info("按订单号查询: {}", orderId);

        Order order = ORDER_DB.get(orderId);
        if (order == null) {
            return "未找到订单号为 " + orderId + " 的订单，请检查订单号是否正确";
        }

        return formatOrder(order);
    }

    /**
     * 根据用户ID查询所有订单
     *
     * @param userId 用户ID，格式如 user_10086
     * @return 该用户所有订单的 JSON 字符串
     */
    @Tool(description = "根据用户ID查询该用户的所有订单列表")
    public String queryOrdersByUserId(
            @ToolParam(description = "用户ID，格式如 user_10086") String userId) {

        log.info("按用户查询订单: {}", userId);

        List<Order> orders = ORDER_DB.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .toList();

        if (orders.isEmpty()) {
            return "未找到用户 " + userId + " 的任何订单";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("用户 ").append(userId).append(" 共有 ").append(orders.size()).append(" 个订单：\n\n");
        for (Order order : orders) {
            sb.append(formatOrder(order)).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    /**
     * 格式化订单信息为可读的字符串
     */
    private String formatOrder(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 订单号：").append(order.getOrderId()).append("\n");
        sb.append("📊 状态：").append(order.getStatusDesc()).append("\n");
        sb.append("👤 收货人：").append(order.getReceiverName())
                .append(" (").append(order.getReceiverPhone()).append(")\n");
        sb.append("📍 地址：").append(order.getShippingAddress()).append("\n\n");

        sb.append("🛒 商品清单：\n");
        for (Order.OrderItem item : order.getItems()) {
            sb.append("  • ").append(item.getProductName())
                    .append(" (").append(item.getSpec()).append(")")
                    .append(" × ").append(item.getQuantity())
                    .append(" = ¥").append(item.getSubtotal()).append("\n");
        }

        sb.append("\n💰 订单总额：¥").append(order.getTotalAmount()).append("\n");

        if (order.getExpressCompany() != null) {
            sb.append("🚚 物流：").append(order.getExpressCompany())
                    .append(" ").append(order.getExpressNo()).append("\n");
        }

        sb.append("🕐 下单时间：").append(order.getCreateTime().format(FORMATTER)).append("\n");
        if (order.getPayTime() != null) {
            sb.append("🕐 支付时间：").append(order.getPayTime().format(FORMATTER)).append("\n");
        }
        if (order.getShipTime() != null) {
            sb.append("🕐 发货时间：").append(order.getShipTime().format(FORMATTER)).append("\n");
        }
        if (order.getCompleteTime() != null) {
            sb.append("🕐 完成时间：").append(order.getCompleteTime().format(FORMATTER)).append("\n");
        }

        if (order.getRemark() != null && !order.getRemark().isBlank()) {
            sb.append("📝 备注：").append(order.getRemark()).append("\n");
        }

        return sb.toString();
    }
}
