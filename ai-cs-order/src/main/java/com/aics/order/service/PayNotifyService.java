package com.aics.order.service;

import com.aics.order.pay.channel.NotifyContext;
import com.aics.order.pay.channel.NotifyResult;
import com.aics.order.pay.channel.PayChannel;
import com.aics.order.pay.channel.PayChannelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付通知统一处理（真实回调与模拟回调共用同一路径）
 *
 * <p>处理链路：渠道验签（+微信解密）→ 幂等更新订单状态（PENDING_PAY → PAID）→ 触发后续通知。
 * 渠道差异（验签/解密方式）被 {@link PayChannel} 隔离，业务逻辑只有一份。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayNotifyService {

    private final PayChannelFactory payChannelFactory;
    private final OrderService orderService;

    public void processNotify(String paymentMethod, NotifyContext context) {
        // 1. 渠道解析通知：验签（+解密），失败会抛出异常（安全红线：防止伪造支付通知）
        PayChannel channel = payChannelFactory.getChannel(paymentMethod);
        NotifyResult result = channel.parseNotify(context);

        // 2. 仅支付成功的通知更新订单（失败/关闭状态忽略）
        if (result == null || !result.isSuccess()) {
            log.warn("支付通知为非成功状态，不更新订单: orderNo={}, method={}",
                    result == null ? "-" : result.getOrderNo(), paymentMethod);
            return;
        }

        // 3. 幂等更新订单状态（内部已做状态判断，重复回调直接返回）
        orderService.handlePayCallback(result.getOrderNo(), paymentMethod);
        log.info("支付通知处理完成: orderNo={}, method={}, amount={}",
                result.getOrderNo(), paymentMethod, result.getAmount());
    }
}