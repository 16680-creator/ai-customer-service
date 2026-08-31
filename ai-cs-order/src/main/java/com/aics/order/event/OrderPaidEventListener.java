package com.aics.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付成功事件监听器：本地事务提交后才投递跨服务通知。
 *
 * <h3>学习要点（@TransactionalEventListener AFTER_COMMIT）</h3>
 * <ul>
 *   <li>事件在事务内发布，但 listener 由 TransactionSynchronization 挂到 afterCommit 回调；</li>
 *   <li>事务回滚时 listener 完全不执行——杜绝"订单未支付却推送支付成功"；</li>
 *   <li>MQ 投递失败只告警，不反向回滚已经提交的订单事务（此处是最终一致副作用）。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidEventListener {

    private final RocketMQTemplate rocketMQTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderPaidEvent event) {
        try {
            Map<String, String> notify = new LinkedHashMap<>();
            notify.put("userId", String.valueOf(event.userId()));
            notify.put("message", "您的订单 " + event.orderNo() + " 已支付成功，感谢您的购买！");
            rocketMQTemplate.convertAndSend("notify-topic", notify);
            log.info("事务提交后支付成功通知已投递: orderNo={}, userId={}", event.orderNo(), event.userId());
        } catch (Exception e) {
            log.warn("事务提交后支付成功通知投递失败: orderNo={}, err={}", event.orderNo(), e.getMessage());
        }
    }
}
