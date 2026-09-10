package com.aics.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 消费端配置（仅在 {@code aics.mq.type=kafka} 时装配）。
 * <p>
 * 核心是把 RocketMQ 的「重试 → 死信队列」语义在 Kafka 上重新搭出来
 * （Kafka 本身没有 DLQ，需要应用侧实现）：</p>
 * <pre>
 * 业务消息消费失败
 *   → DefaultErrorHandler 原地重试 3 次（固定 1s 间隔）
 *   → 仍失败：DeadLetterPublishingRecoverer 转发到 &lt;topic&gt;-dlt
 *   → WorkOrderKafkaDlqListener 消费 -dlt 主题并落 mq_fail_record
 * </pre>
 *
 * <p>这里显式定义名为 {@code kafkaListenerContainerFactory} 的 Bean，
 * 因为 Spring Boot 自动配置的容器工厂是 {@code @ConditionalOnMissingBean(name =
 * "kafkaListenerContainerFactory")}——业务配置优先级更高，会覆盖它，从而拿到
 * 「自定义错误处理器」的控制权。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "aics.mq", name = "type", havingValue = "kafka")
public class KafkaConsumerConfig {

    /** DeadLetterPublishingRecoverer 默认的死信主题后缀：&lt;原主题&gt;-dlt */
    public static final String DLT_SUFFIX = "-dlt";

    /** 业务消费者：重试 3 次后转发死信主题 */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<?, ?> consumerFactory,
            KafkaTemplate<?, ?> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(castConsumerFactory(consumerFactory));

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(castOperations(kafkaTemplate));
        // 对齐 RocketMQ 侧 maxReconsumeTimes=3：固定 1s 间隔、额外重试 3 次
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3)));
        log.info("Kafka 消费者容器工厂已装配：重试 3 次后转发至 <topic>{}", DLT_SUFFIX);
        return factory;
    }

    /**
     * 死信消费者专用工厂：不再重试、不再转发，
     * 否则死信消费失败会继续产生 {@code -dlt-dlt} 无限递归。
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> workOrderDltContainerFactory(
            ConsumerFactory<?, ?> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(castConsumerFactory(consumerFactory));
        // FixedBackOff(0, 0) = 不重试，失败只记录日志
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0)));
        return factory;
    }

    @SuppressWarnings("unchecked")
    private static ConsumerFactory<String, Object> castConsumerFactory(ConsumerFactory<?, ?> consumerFactory) {
        return (ConsumerFactory<String, Object>) consumerFactory;
    }

    @SuppressWarnings("unchecked")
    private static KafkaOperations<Object, Object> castOperations(KafkaTemplate<?, ?> kafkaTemplate) {
        return (KafkaOperations<Object, Object>) kafkaTemplate;
    }
}
