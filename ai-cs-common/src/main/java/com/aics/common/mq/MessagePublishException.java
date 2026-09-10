package com.aics.common.mq;

/**
 * 统一消息发送异常：屏蔽 RocketMQ / Kafka 各自的异常类型，让业务侧只捕获一种异常。
 */
public class MessagePublishException extends RuntimeException {

    public MessagePublishException(String message) {
        super(message);
    }

    public MessagePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
