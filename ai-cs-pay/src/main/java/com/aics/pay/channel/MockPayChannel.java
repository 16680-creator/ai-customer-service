package com.aics.pay.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟支付渠道（用于本地学习/演示完整支付流程）
 *
 * <p>职责与真实渠道完全一致：
 * <ol>
 *   <li>{@link #createPayment}：下单，生成"模拟收银台"跳转地址（等价于微信 Native 的 code_url / 支付宝收银台）</li>
 *   <li>{@link #markPaid}：模拟"用户在收银台完成支付"（等价于用户在微信/支付宝 App 支付成功）</li>
 *   <li>支付成功后由 MockPayController 触发与真实渠道相同的异步通知处理路径</li>
 *   <li>{@link #refund}：模拟渠道退款</li>
 * </ol>
 * 后续接入支付宝/微信时，复制本类改为 {@code AlipayChannel} / {@code WechatNativeChannel} 即可。
 */
@Slf4j
@Component
public class MockPayChannel implements PayChannel {

    /** 模拟收银台前端地址（Vite dev server） */
    @Value("${aics.pay.mock.cashier-url:http://localhost:5173/mock-pay}")
    private String cashierUrl = "http://localhost:5173/mock-pay";

    /** 渠道侧交易状态：orderNo -> PENDING/SUCCESS/CLOSED/REFUNDED（模拟渠道记账） */
    private final Map<String, String> channelState = new ConcurrentHashMap<>();

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REFUNDED = "REFUNDED";

    @Override
    public String getMethod() {
        return "MOCK";
    }

    @Override
    public PayResult createPayment(PayContext context) {
        channelState.put(context.getOrderNo(), STATUS_PENDING);
        String payUrl = cashierUrl
                + "?orderNo=" + context.getOrderNo()
                + "&amount=" + context.getPayAmount();
        log.info("[MockPay] 下单成功: orderNo={}, amount={}, payUrl={}", context.getOrderNo(), context.getPayAmount(), payUrl);
        return PayResult.builder()
                .payType("REDIRECT")
                .payUrl(payUrl)
                .tradeNo("MOCK-" + context.getOrderNo())
                .build();
    }

    @Override
    public String queryPayment(String orderNo) {
        return channelState.getOrDefault(orderNo, STATUS_PENDING);
    }

    @Override
    public NotifyResult parseNotify(NotifyContext context) {
        // TODO: 真实渠道在这里验签（微信 v3 验签+解密 / 支付宝 RSA2 验签 / 银联证书验签）
        log.info("[MockPay] 模拟回调验签通过");
        Map<String, String> params = context.getParams() == null ? Map.of() : context.getParams();
        String orderNo = params.getOrDefault("orderNo", "");
        boolean success = !"FAIL".equalsIgnoreCase(params.getOrDefault("result", "SUCCESS"));
        String amountStr = params.getOrDefault("amount", "0");
        return NotifyResult.builder()
                .orderNo(orderNo)
                .success(success)
                .amount(new BigDecimal(amountStr))
                .build();
    }

    @Override
    public void closeOrder(String orderNo) {
        markClosed(orderNo);
    }

    @Override
    public RefundResult refund(String orderNo, BigDecimal refundAmount) {
        channelState.put(orderNo, STATUS_REFUNDED);
        log.info("[MockPay] 模拟退款成功: orderNo={}, amount={}", orderNo, refundAmount);
        return RefundResult.builder()
                .refundNo("MOCK-REFUND-" + orderNo)
                .status("SUCCESS")
                .build();
    }

    /** 模拟"用户在收银台完成支付"（渠道侧标记为已支付） */
    public void markPaid(String orderNo) {
        channelState.put(orderNo, STATUS_SUCCESS);
        log.info("[MockPay] 用户模拟支付成功: orderNo={}", orderNo);
    }

    /** 模拟"渠道侧关闭订单"（超时未支付） */
    public void markClosed(String orderNo) {
        channelState.put(orderNo, STATUS_CLOSED);
        log.info("[MockPay] 模拟渠道关闭订单: orderNo={}", orderNo);
    }
}