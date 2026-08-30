package com.aics.order.listener;

import com.aics.common.mq.PaySuccessMessage;
import com.aics.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 支付成功事件消费者（pay 事务消息 → order 确认订单）。
 *
 * <p>学习要点（事务消息的消费侧）：
 * <ul>
 *   <li>消费失败的兜底是 RocketMQ 重试（本组 maxReconsumeTimes=8，间隔递增），
 *       重试耗尽进入死信主题 {@code %DLQ%pay-success-consumer-group}；</li>
 *   <li>下游 {@code confirmPay} 自带幂等（已支付订单重复确认直接返回），
 *       因此「至少一次投递 + 幂等消费」是消息语义下的标准安全组合；</li>
 *   <li>对比旧同步 Feign 回调：渠道回调抖动不再直接丢单——事件在 Broker 持久化，
 *       order 重启/短暂不可用后仍能补消费。</li>
 * </ul></p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "pay-success-topic",
        consumerGroup = "pay-success-consumer-group",
        maxReconsumeTimes = 8
)
public class PaySuccessListener implements RocketMQListener<PaySuccessMessage> {

    private final OrderService orderService;

    @Override
    public void onMessage(PaySuccessMessage message) {
        log.info("收到支付成功事件: orderNo={}, method={}, amount={}, tradeNo={}",
                message.getOrderNo(), message.getPaymentMethod(), message.getAmount(), message.getTradeNo());
        orderService.confirmPay(message.getOrderNo(), message.getPaymentMethod(),
                message.getAmount(), message.getTradeNo());
    }
}
