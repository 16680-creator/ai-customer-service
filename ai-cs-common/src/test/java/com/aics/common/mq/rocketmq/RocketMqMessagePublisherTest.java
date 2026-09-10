package com.aics.common.mq.rocketmq;

import com.aics.common.mq.MessageBrokerType;
import com.aics.common.mq.MessagePublishException;
import com.aics.common.mq.MqProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * RocketMQ 发布器单测：topic:tag 目的地拼接、延迟级别映射、异常统一包装。
 */
class RocketMqMessagePublisherTest {

    private final RocketMQTemplate template = mock(RocketMQTemplate.class);
    private final RocketMqMessagePublisher publisher = new RocketMqMessagePublisher(template, new MqProperties());

    @Test
    @DisplayName("send - key 非空时目的地为 topic:tag")
    void sendShouldBuildTagDestination() {
        publisher.send("work-order-topic", "WORK_ORDER", "payload");

        verify(template).syncSend("work-order-topic:WORK_ORDER", "payload");
    }

    @Test
    @DisplayName("send - key 为空时目的地只有 topic")
    void sendShouldUseTopicOnlyWhenKeyBlank() {
        publisher.send("notify-topic", null, "payload");

        verify(template).syncSend("notify-topic", "payload");
    }

    @Test
    @DisplayName("send - Broker 异常统一包装为 MessagePublishException")
    void sendShouldWrapBrokerException() {
        // 用 any(Object.class) 消除 syncSend(String, Message) 与 syncSend(String, Collection) 的重载歧义
        doThrow(new RuntimeException("name server not reachable"))
                .when(template).syncSend(eq("notify-topic"), any(Object.class));

        assertThatThrownBy(() -> publisher.send("notify-topic", null, "payload"))
                .isInstanceOf(MessagePublishException.class)
                .hasMessageContaining("notify-topic")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("sendDelayed - 30 分钟映射到 RocketMQ 延迟级别 16")
    void shouldResolveDelayLevelForThirtyMinutes() {
        publisher.sendDelayed("order-timeout-topic", "ORDER", "orderNo", 30 * 60 * 1000L);

        verify(template).syncSend(eq("order-timeout-topic:ORDER"), any(Message.class), anyLong(), eq(16));
    }

    @Test
    @DisplayName("延迟级别映射 - 向上取整到最近的固定级别")
    void shouldRoundUpDelayLevel() {
        assertThat(RocketMqMessagePublisher.resolveDelayLevel(1)).isEqualTo(1);        // 1s
        assertThat(RocketMqMessagePublisher.resolveDelayLevel(1_001)).isEqualTo(2);    // 5s
        assertThat(RocketMqMessagePublisher.resolveDelayLevel(60_000)).isEqualTo(5);   // 1m
        assertThat(RocketMqMessagePublisher.resolveDelayLevel(7_200_000)).isEqualTo(18); // 2h
    }

    @Test
    @DisplayName("延迟级别映射 - 非正数与超过 2 小时都拒绝，而不是静默降级")
    void shouldRejectInvalidDelay() {
        assertThatThrownBy(() -> RocketMqMessagePublisher.resolveDelayLevel(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RocketMqMessagePublisher.resolveDelayLevel(7_200_001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 小时");
    }

    @Test
    @DisplayName("brokerType - 标识为 ROCKETMQ")
    void shouldReportBrokerType() {
        assertThat(publisher.brokerType()).isEqualTo(MessageBrokerType.ROCKETMQ);
    }
}
