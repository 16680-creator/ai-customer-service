package com.aics.pay.mq;

import com.aics.common.mq.PaySuccessMessage;
import com.aics.pay.entity.PayTransaction;
import com.aics.pay.service.PayTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 支付成功事务消息的本地事务执行器与回查器。
 *
 * <p>学习要点：
 * <ul>
 *   <li><b>executeLocalTransaction</b>：半消息发送成功后被回调，这里执行真正的本地事务
 *       （支付流水标记成功）。成功返回 COMMIT（消息对消费者可见），
 *       抛异常/失败返回 ROLLBACK（半消息删除）。</li>
 *   <li><b>checkLocalTransaction</b>：Broker 对「半消息悬而未决」的情况（如应用在
 *       落库后、提交前宕机）定时回查，用支付流水状态做二次判定：SUCCESS → COMMIT、
 *       PENDING → UNKNOWN（稍后再查）、CLOSED/不存在 → ROLLBACK。
 *       回查必须幂等且以落库状态为唯一权威。</li>
 *   <li>rocketmq-spring 2.3.0：监听器实现 {@link RocketMQLocalTransactionListener}
 *       （Spring Message 风格），payload 即发送时的对象，无需手动反序列化。</li>
 * </ul></p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
// 学习要点（rocketmq-spring 2.3.0）：txProducerGroup 属性已移除，监听器默认绑定主 RocketMQTemplate
@RocketMQTransactionListener
public class PaySuccessTransactionListener implements RocketMQLocalTransactionListener {

    private final PayTransactionService payTransactionService;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        PaySuccessMessage payload = arg instanceof PaySuccessMessage p ? p
                : (PaySuccessMessage) msg.getPayload();
        try {
            // 本地事务：支付流水 PENDING → SUCCESS（幂等，重复通知不重复处理）
            payTransactionService.markSuccess(payload.getOrderNo(), payload.getTradeNo(), payload.getAmount());
            log.info("事务消息本地事务执行成功: orderNo={}", payload.getOrderNo());
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            log.error("事务消息本地事务执行失败，半消息将回滚: orderNo={}, err={}",
                    payload.getOrderNo(), e.getMessage(), e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        PaySuccessMessage payload = (PaySuccessMessage) msg.getPayload();
        PayTransaction tx = payTransactionService.getByOrderNo(payload.getOrderNo());
        if (tx == null) {
            log.warn("回查未找到支付流水，回滚半消息: orderNo={}", payload.getOrderNo());
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        return switch (tx.getStatus()) {
            case "SUCCESS" -> RocketMQLocalTransactionState.COMMIT;
            case "PENDING" -> RocketMQLocalTransactionState.UNKNOWN; // 流水未决，等待下次回查
            default -> RocketMQLocalTransactionState.ROLLBACK; // CLOSED/REFUNDING 等
        };
    }
}
