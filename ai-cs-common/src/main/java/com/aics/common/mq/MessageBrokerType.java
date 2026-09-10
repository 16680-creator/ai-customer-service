package com.aics.common.mq;

/**
 * 消息中间件类型。
 * <p>
 * 由配置项 {@code aics.mq.type} 决定（默认 {@link #ROCKETMQ}），
 * 业务代码只依赖 {@link MessagePublisher}，不感知具体 Broker。</p>
 */
public enum MessageBrokerType {

    /** 阿里 RocketMQ：原生延迟消息、事务消息、Tag 过滤、DLQ */
    ROCKETMQ,

    /** Apache Kafka：高吞吐日志型追加、分区有序、生态（Connect/Flink） */
    KAFKA
}
