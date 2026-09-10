package com.aics.common.mq.rocketmq;

import com.aics.common.mq.MessageBrokerType;
import com.aics.common.mq.MessagePublishException;
import com.aics.common.mq.MessagePublisher;
import com.aics.common.mq.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

/**
 * RocketMQ 实现。
 * <p>
 * 目的地拼接规则沿用 RocketMQ 约定 {@code topic:tag}；
 * 延迟消息使用 RocketMQ 的固定延迟级别（共 18 级，服务端不支持任意毫秒）。</p>
 */
@Slf4j
public class RocketMqMessagePublisher implements MessagePublisher {

    /**
     * RocketMQ 支持的 18 个固定延迟级别对应的毫秒数，下标 0 对应 delayLevel = 1。
     * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     */
    private static final long[] DELAY_LEVEL_MILLIS = {
            1_000L, 5_000L, 10_000L, 30_000L,
            60_000L, 120_000L, 180_000L, 240_000L, 300_000L,
            360_000L, 420_000L, 480_000L, 540_000L, 600_000L,
            1_200_000L, 1_800_000L, 3_600_000L, 7_200_000L
    };

    private final RocketMQTemplate rocketMqTemplate;
    private final MqProperties properties;

    public RocketMqMessagePublisher(RocketMQTemplate rocketMqTemplate, MqProperties properties) {
        this.rocketMqTemplate = rocketMqTemplate;
        this.properties = properties;
    }

    @Override
    public void send(String topic, String key, Object payload) {
        String destination = destination(topic, key);
        try {
            rocketMqTemplate.syncSend(destination, payload);
            log.debug("RocketMQ 消息发送成功: destination={}", destination);
        } catch (Exception e) {
            throw new MessagePublishException("RocketMQ 消息发送失败: destination=" + destination, e);
        }
    }

    @Override
    public void sendDelayed(String topic, String key, Object payload, long delayMillis) {
        String destination = destination(topic, key);
        int delayLevel = resolveDelayLevel(delayMillis);
        try {
            rocketMqTemplate.syncSend(destination,
                    MessageBuilder.withPayload(payload).build(),
                    properties.getSendTimeoutMillis(),
                    delayLevel);
            log.debug("RocketMQ 延迟消息发送成功: destination={}, delayLevel={}", destination, delayLevel);
        } catch (Exception e) {
            throw new MessagePublishException(
                    "RocketMQ 延迟消息发送失败: destination=" + destination + ", delayLevel=" + delayLevel, e);
        }
    }

    @Override
    public MessageBrokerType brokerType() {
        return MessageBrokerType.ROCKETMQ;
    }

    /** topic:tag 目的地；tag 为空时只用 topic */
    static String destination(String topic, String key) {
        return StringUtils.hasText(key) ? topic + ":" + key : topic;
    }

    /**
     * 把期望延迟毫秒映射到 RocketMQ 固定延迟级别：
     * 取第一个「不早于」期望值的级别，超过 2 小时直接拒绝（而不是静默改小）。
     *
     * @return delayLevel，取值 1~18
     */
    static int resolveDelayLevel(long delayMillis) {
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("延迟时间必须大于 0: " + delayMillis);
        }
        for (int i = 0; i < DELAY_LEVEL_MILLIS.length; i++) {
            if (delayMillis <= DELAY_LEVEL_MILLIS[i]) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException(
                "RocketMQ 最大延迟级别为 2 小时，无法满足 " + delayMillis + "ms；请改用定时任务或延迟队列");
    }
}
