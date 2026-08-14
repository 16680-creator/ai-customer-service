package com.aics.chat.agent.tool;

import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.dto.OrderVO;
import com.aics.chat.feign.OrderFeignClient;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 订单定位工具（只读）：定位当前用户可售后的订单。
 *
 * <p>订单列表来自订单服务本人查询（服务端按 X-User-Id 过滤），
 * 归属校验由订单服务与本人查询双重保证；按订单号匹配时仅在本人列表中匹配，
 * 杜绝越权访问他人订单。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLocatorTool implements AgentTool {

    private final OrderFeignClient orderFeignClient;

    @Override
    public String name() {
        return AgentStateMachine.TOOL_ORDER_LOCATOR;
    }

    @Override
    public RiskLevel riskLevel() {
        return RiskLevel.READ;
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    /**
     * 定位订单
     *
     * @param orderNo 用户提供的订单号（可为空，空则取最近的可售后订单）
     * @return SUCCESS：唯一命中订单；CANDIDATES：多候选；FAIL：无订单/不可用
     */
    public ToolResult locate(String orderNo) {
        // 当前登录用户（服务端透传，保证只查本人订单）
        Long userId = ChatUserContext.getUserId();
        if (userId == null) {
            return ToolResult.fail("无法识别当前登录用户身份，请先登录");
        }
        try {
            Result<List<OrderVO>> result = orderFeignClient.listOrders(userId);
            if (result == null || !result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                return ToolResult.fail("您目前没有任何订单");
            }
            // 仅已支付（PAID）订单可售后
            List<OrderVO> paidOrders = result.getData().stream()
                    .filter(o -> "PAID".equals(o.getStatus()))
                    .toList();
            if (paidOrders.isEmpty()) {
                return ToolResult.fail("您没有已支付的可售后订单");
            }
            if (StringUtils.hasText(orderNo)) {
                // 用户指定订单号：仅在本人已支付订单中精确匹配
                OrderVO matched = paidOrders.stream()
                        .filter(o -> orderNo.trim().equals(o.getOrderNo()))
                        .findFirst()
                        .orElse(null);
                if (matched != null) {
                    return ToolResult.success("订单定位成功", matched);
                }
                // 非本人订单或不存在：明确拒绝，杜绝越权
                return ToolResult.fail("订单 " + orderNo + " 不存在或不属于当前用户，无法售后");
            }
            // 未指定订单号：唯一可售后订单直接命中
            if (paidOrders.size() == 1) {
                return ToolResult.success("订单定位成功", paidOrders.get(0));
            }
            // 多个可售后订单：返回候选列表让用户选择
            return ToolResult.candidates("存在多个可售后订单，请用户选择", paidOrders);
        } catch (Exception e) {
            log.warn("订单定位失败: userId={}, err={}", userId, e.getMessage());
            // Feign 异常兜底：返回可解释失败
            return ToolResult.fail("订单服务暂时不可用，请稍后再试");
        }
    }
}
