package com.aics.pay.service;

import com.aics.pay.channel.NotifyContext;
import com.aics.pay.channel.NotifyResult;
import com.aics.pay.channel.PayChannel;
import com.aics.pay.channel.PayChannelFactory;
import com.aics.pay.client.OrderPayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付通知统一处理：验签（+解密）→ 记录支付流水 → 通知订单服务确认支付
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayNotifyService {

    private final PayChannelFactory payChannelFactory;
    private final PayTransactionService payTransactionService;
    private final OrderPayClient orderPayClient;

    public void processNotify(String paymentMethod, NotifyContext context) {
        PayChannel channel = payChannelFactory.getChannel(paymentMethod);
        NotifyResult result = channel.parseNotify(context);

        if (result == null || !result.isSuccess()) {
            log.warn("支付通知为非成功状态，不更新订单: orderNo={}, method={}",
                    result == null ? "-" : result.getOrderNo(), paymentMethod);
            return;
        }

        payTransactionService.markSuccess(result.getOrderNo(), result.getTransactionId(), result.getAmount());
        orderPayClient.confirmPay(result.getOrderNo(), paymentMethod, result.getAmount(), result.getTransactionId());
        log.info("支付通知处理完成: orderNo={}, method={}, amount={}", result.getOrderNo(), paymentMethod, result.getAmount());
    }

    public boolean syncByQuery(String orderNo, String paymentMethod) {
        PayChannel channel = payChannelFactory.getChannel(paymentMethod);
        String channelStatus = channel.queryPayment(orderNo);
        if (PayChannel.STATUS_SUCCESS.equals(channelStatus)) {
            log.info("查单兜底命中已支付: orderNo={}, method={}", orderNo, paymentMethod);
            payTransactionService.markSuccess(orderNo, null, null);
            orderPayClient.confirmPay(orderNo, paymentMethod, null, null);
            return true;
        }
        return false;
    }

    /** 关单（订单取消/超时后由订单服务回调） */
    public void closeOrder(String orderNo, String paymentMethod) {
        try {
            payChannelFactory.getChannel(paymentMethod).closeOrder(orderNo);
        } catch (Exception e) {
            log.warn("关闭渠道订单失败: orderNo={}, method={}, err={}", orderNo, paymentMethod, e.getMessage());
        }
        try {
            payTransactionService.markClosed(orderNo);
        } catch (Exception e) {
            log.warn("支付流水标记关闭失败: orderNo={}, err={}", orderNo, e.getMessage());
        }
    }
}