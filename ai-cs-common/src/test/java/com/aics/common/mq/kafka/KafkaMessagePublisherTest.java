package com.aics.common.mq.kafka;

import com.aics.common.mq.MessageBrokerType;
import com.aics.common.mq.MessagePublishException;
import com.aics.common.mq.MqProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kafka 发布器单测：发送结果同步化、失败包装、延迟消息降级调度。
 */
class KafkaMessagePublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<Object, Object> template = mock(KafkaTemplate.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final KafkaMessagePublisher publisher =
            new KafkaMessagePublisher(template, new MqProperties(), scheduler);

    @Test
    @DisplayName("send - 等待 future 完成后正常返回")
    void sendShouldAwaitFuture() {
        when(template.send("work-order-topic", "WORK_ORDER", "payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.send("work-order-topic", "WORK_ORDER", "payload");

        verify(template).send("work-order-topic", "WORK_ORDER", "payload");
    }

    @Test
    @DisplayName("send - future 异常统一包装为 MessagePublishException")
    void sendShouldWrapFailure() {
        when(template.send(eq("work-order-topic"), eq("WORK_ORDER"), eq("payload")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        assertThatThrownBy(() -> publisher.send("work-order-topic", "WORK_ORDER", "payload"))
                .isInstanceOf(MessagePublishException.class)
                .hasMessageContaining("work-order-topic")
                .hasCauseInstanceOf(java.util.concurrent.ExecutionException.class);
    }

    @Test
    @DisplayName("sendDelayed - Kafka 无原生延迟，降级为内存调度器延时投递")
    void sendDelayedShouldDegradeToScheduler() {
        when(template.send(eq("order-timeout-topic"), eq("ORDER"), eq("orderNo")))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.sendDelayed("order-timeout-topic", "ORDER", "orderNo", 30_000L);

        // 延迟期间不应立即发送
        verify(template, org.mockito.Mockito.never()).send(any(), any(), any());

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), any(Instant.class));

        // 到点后调度任务真正投递
        task.getValue().run();
        verify(template).send("order-timeout-topic", "ORDER", "orderNo");
    }

    @Test
    @DisplayName("sendDelayed - 调度线程里发送失败只记录日志，不向外抛")
    void sendDelayedShouldSwallowFailureInSchedulerThread() {
        when(template.send(eq("order-timeout-topic"), eq("ORDER"), eq("orderNo")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        publisher.sendDelayed("order-timeout-topic", "ORDER", "orderNo", 10L);

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), any(Instant.class));

        // 不应抛异常（否则调度线程会被异常污染）
        task.getValue().run();
    }

    @Test
    @DisplayName("brokerType - 标识为 KAFKA")
    void shouldReportBrokerType() {
        assertThat(publisher.brokerType()).isEqualTo(MessageBrokerType.KAFKA);
    }
}
