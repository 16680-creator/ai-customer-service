package com.aics.pay.mq;

import com.aics.common.mq.PaySuccessMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 支付成功事务消息发送器（pay → order）。
 *
 * <p>学习要点（RocketMQ 事务消息三步）：
 * <ol>
 *   <li><b>半消息</b>：{@code sendMessageInTransaction} 先把消息投到 Broker 的半消息主题，
 *       对消费者不可见；</li>
 *   <li><b>本地事务</b>：Broker 回调 {@link PaySuccessTransactionListener#executeLocalTransaction}
 *       执行支付流水落库，成功 COMMIT、失败 ROLLBACK；</li>
 *   <li><b>回查</b>：Broker 对未决半消息定时回查 {@code checkLocalTransaction}，
 *       以支付流水的真实状态给出二次确认——即便应用在「落库后、提交前」宕机也不会丢事件。</li>
 * </ol>
 * 对比旧的「markSuccess 后同步 Feign 通知 order」：回调超时/失败即丢事件，
 * 需要额外的查单兜底补偿；事务消息把可靠性下沉到 Broker。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaySuccessMessageProducer {

    /** 事务消息主题（order 侧 PaySuccessListener 消费） */
    public static final String TOPIC = "pay-success-topic";

    /** 事务生产组：与 {@code @RocketMQTransactionListener} 的 txProducerGroup 一致 */
    public static final String TX_PRODUCER_GROUP = "pay-success-tx-producer-group";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送支付成功事务消息（半消息），本地事务在 TransactionListener 中执行。
     *
     * @param message 支付成功事件载荷（arg 原样传给 executeLocalTransaction）
     */
    public void sendSuccess(PaySuccessMessage message) {
        Message<PaySuccessMessage> msg = MessageBuilder.withPayload(message)
                .setHeader("TOPIC", TOPIC)
                .build();
        // 2.3.0 签名：sendMessageInTransaction(destination, message, arg)，destination 即 topic
        var result = rocketMQTemplate.sendMessageInTransaction(TOPIC, msg, message);
        log.info("支付成功事务消息发送完成: orderNo={}, sendStatus={}, localTx={}",
                message.getOrderNo(), result.getSendStatus(), result.getLocalTransactionState());
    }

    /**
     * 回查时从消息体反序列化载荷（rocketMQTemplate 发送时将 payload 序列化为 JSON 存入 body）。
     */
    public static PaySuccessMessage extractPayload(org.apache.rocketmq.common.message.Message msg) {
        try {
            return MAPPER.readValue(msg.getBody(), PaySuccessMessage.class);
        } catch (IOException e) {
            throw new IllegalStateException("支付成功消息体反序列化失败", e);
        }
    }

    public static final ObjectMapper MAPPER = new ObjectMapper();
}
