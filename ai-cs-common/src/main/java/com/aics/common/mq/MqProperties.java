package com.aics.common.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息中间件开关配置。
 *
 * <pre>
 * aics:
 *   mq:
 *     type: rocketmq        # rocketmq（默认）| kafka —— 全局开关
 *     send-timeout-millis: 5000
 * </pre>
 */
@ConfigurationProperties(prefix = "aics.mq")
public class MqProperties {

    /** 生效的消息中间件实现，默认 RocketMQ，保证存量行为不变 */
    private MessageBrokerType type = MessageBrokerType.ROCKETMQ;

    /**
     * 发送超时（毫秒）。
     * Kafka 实现用它作为 {@code future.get} 的等待上限；
     * RocketMQ 实现的发送超时由 {@code rocketmq.producer.send-message-timeout} 控制，
     * 该值仅用于「延迟消息」发送。
     */
    private long sendTimeoutMillis = 5_000L;

    public MessageBrokerType getType() {
        return type;
    }

    public void setType(MessageBrokerType type) {
        this.type = type;
    }

    public long getSendTimeoutMillis() {
        return sendTimeoutMillis;
    }

    public void setSendTimeoutMillis(long sendTimeoutMillis) {
        this.sendTimeoutMillis = sendTimeoutMillis;
    }
}
