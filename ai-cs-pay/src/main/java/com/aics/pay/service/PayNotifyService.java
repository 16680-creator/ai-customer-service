package com.aics.pay.service;

import com.aics.pay.channel.NotifyContext;
import com.aics.pay.channel.NotifyResult;
import com.aics.pay.channel.PayChannel;
import com.aics.pay.channel.PayChannelFactory;
import com.aics.common.mq.PaySuccessMessage;
import com.aics.pay.client.OrderPayClient;
import com.aics.pay.mq.PaySuccessMessageProducer;
import org.springframework.beans.factory.annotation.Value;
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
    private final PaySuccessMessageProducer paySuccessMessageProducer;

    /** 支付成功通知模式：tx-message=RocketMQ 事务消息（默认），feign=旧同步回调（保留对比） */
    @Value("${aics.pay.tx-notify.enabled:true}")
    private boolean txNotifyEnabled;

    public void processNotify(String paymentMethod, NotifyContext context) {
        PayChannel channel = payChannelFactory.getChannel(paymentMethod);
        NotifyResult result = channel.parseNotify(context);

        if (result == null || !result.isSuccess()) {
            log.warn("支付通知为非成功状态，不更新订单: orderNo={}, method={}",
                    result == null ? "-" : result.getOrderNo(), paymentMethod);
            return;
        }

        notifyOrderSuccess(result.getOrderNo(), paymentMethod, result.getAmount(), result.getTransactionId());
        log.info("支付通知处理完成: orderNo={}, method={}, amount={}", result.getOrderNo(), paymentMethod, result.getAmount());
    }

    public boolean syncByQuery(String orderNo, String paymentMethod) {
        PayChannel channel = payChannelFactory.getChannel(paymentMethod);
        String channelStatus = channel.queryPayment(orderNo);
        if (PayChannel.STATUS_SUCCESS.equals(channelStatus)) {
            log.info("查单兜底命中已支付: orderNo={}, method={}", orderNo, paymentMethod);
            notifyOrderSuccess(orderNo, paymentMethod, null, null);
            return true;
        }
        return false;
    }

    /**
     * 通知订单服务「支付成功」。
     *
     * <p>tx-message 模式：本地事务（流水 markSuccess）移入
     * {@link PaySuccessTransactionListener#executeLocalTransaction}，由事务消息机制保证
     * 「流水落库成功 ⇒ 消息必达 order」；feign 模式为旧同步回调（回查期 Feign 失败即丢事件）。</p>
     */
    private void notifyOrderSuccess(String orderNo, String paymentMethod, java.math.BigDecimal amount, String tradeNo) {
        if (txNotifyEnabled) {
            paySuccessMessageProducer.sendSuccess(PaySuccessMessage.builder()
                    .orderNo(orderNo).paymentMethod(paymentMethod).amount(amount).tradeNo(tradeNo)
                    .build());
            return;
        }
        payTransactionService.markSuccess(orderNo, tradeNo, amount);
        orderPayClient.confirmPay(orderNo, paymentMethod, amount, tradeNo);
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