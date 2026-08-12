package com.aics.chat.service;

import com.aics.chat.dto.OrderVO;
import com.aics.chat.feign.OrderFeignClient;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 订单查询服务（Tool Calling）
 * <p>通过 Feign 调用真实 ai-cs-order 服务查询订单，不再提供本地 mock 数据，
 * 避免把写死的示例订单展示给用户。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderFeignClient orderFeignClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 根据订单号查询订单详情
     *
     * @param orderId 订单编号
     * @return 订单详情 JSON 字符串
     */
    @Tool(description = "根据订单号查询订单详情，包括订单状态、商品信息、金额、物流等")
    public String queryOrderByOrderId(
            @ToolParam(description = "订单编号，格式如 ORD20260809001") String orderId) {

        log.info("按订单号查询: {}", orderId);
        Long userId = ChatUserContext.getUserId();
        if (userId == null) {
            return "无法识别当前登录用户身份，请先登录后再查询订单";
        }
        try {
            Result<OrderVO> result = orderFeignClient.getOrderDetail(userId, orderId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return formatOrderVO(result.getData());
            }
        } catch (Exception e) {
            log.warn("订单服务查询失败: orderId={}, err={}", orderId, e.getMessage());
            return "订单服务暂时不可用，请稍后再试";
        }
        return "未找到订单号为 " + orderId + " 的订单，请检查订单号是否正确";
    }

    /**
     * 根据用户ID查询所有订单
     *
     * @param userId 用户ID（纯数字格式），为空时自动取当前登录用户
     * @return 该用户所有订单的 JSON 字符串
     */
    @Tool(description = "根据用户ID查询该用户的所有订单列表")
    public String queryOrdersByUserId(
            @ToolParam(description = "用户ID（纯数字格式，如 1），一般无需传参") String userId) {

        log.info("按用户查询订单: {}", userId);
        Long uid = ChatUserContext.getUserId();
        if (uid == null && userId != null && userId.matches("\\d+")) {
            uid = Long.parseLong(userId);
        }
        if (uid == null) {
            return "无法识别当前登录用户身份，请先登录后再查询订单";
        }
        try {
            Result<List<OrderVO>> result = orderFeignClient.listOrders(uid);
            if (result != null && result.isSuccess() && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    return "用户 " + uid + " 暂无任何订单";
                }
                return formatOrderVOList(result.getData(), uid);
            }
        } catch (Exception e) {
            log.warn("订单服务查询失败: userId={}, err={}", uid, e.getMessage());
            return "订单服务暂时不可用，请稍后再试";
        }
        return "用户 " + uid + " 暂无任何订单";
    }

    /**
     * 格式化真实订单 VO 为可读文本
     */
    private String formatOrderVO(OrderVO vo) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 订单号：").append(vo.getOrderNo()).append("\n");
        sb.append("📊 状态：").append(vo.getStatus()).append("\n");
        sb.append("💰 订单总额：¥").append(vo.getTotalAmount() != null ? vo.getTotalAmount() : "0")
                .append("，应付：¥").append(vo.getPayAmount() != null ? vo.getPayAmount() : "0").append("\n");
        if (vo.getDiscountAmount() != null && vo.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("🎁 优惠：¥").append(vo.getDiscountAmount()).append("\n");
        }
        if (vo.getPaymentMethod() != null) {
            sb.append("💳 支付方式：").append(vo.getPaymentMethod()).append("\n");
        }
        if (vo.getItems() != null && !vo.getItems().isEmpty()) {
            sb.append("🛒 商品清单：\n");
            for (OrderVO.OrderItemVO item : vo.getItems()) {
                sb.append("  • ").append(item.getProductName())
                        .append(" × ").append(item.getQuantity())
                        .append(" = ¥").append(item.getSubtotal()).append("\n");
            }
        }
        if (vo.getCreateTime() != null) {
            sb.append("🕐 下单时间：").append(vo.getCreateTime().format(FORMATTER)).append("\n");
        }
        if (vo.getExpireTime() != null) {
            sb.append("⏰ 支付截止：").append(vo.getExpireTime().format(FORMATTER)).append("\n");
        }
        if (vo.getPayUrl() != null && !vo.getPayUrl().isBlank()) {
            sb.append("🔗 支付链接：").append(vo.getPayUrl()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 格式化订单列表
     */
    private String formatOrderVOList(List<OrderVO> vos, Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户 ").append(userId).append(" 共有 ").append(vos.size()).append(" 个订单：\n\n");
        for (OrderVO vo : vos) {
            sb.append(formatOrderVO(vo)).append("\n\n---\n\n");
        }
        return sb.toString().trim();
    }
}