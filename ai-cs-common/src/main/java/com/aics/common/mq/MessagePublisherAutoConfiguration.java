package com.aics.common.mq;

import com.aics.common.mq.kafka.KafkaMessagePublisher;
import com.aics.common.mq.rocketmq.RocketMqMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 消息发布器自动装配：按 {@code aics.mq.type} 二选一装配 {@link MessagePublisher}。
 * <ul>
 *   <li>默认 {@code rocketmq}（{@code matchIfMissing = true}），存量服务零改动；</li>
 *   <li>{@code kafka} 时装配 Kafka 实现；</li>
 *   <li>用 {@code @ConditionalOnClass} + Maven {@code optional} 依赖，
 *       使得「没引 MQ starter 的服务」与「只引了 RocketMQ 的服务」都能正常启动；</li>
 *   <li>业务方自定义 {@link MessagePublisher} Bean 时自动让位（{@code @ConditionalOnMissingBean}）。</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MqProperties.class)
@AutoConfigureAfter(name = {
        "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration",
        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
public class MessagePublisherAutoConfiguration {

    /** RocketMQ 分支：classpath 有 RocketMQTemplate 且开关为 rocketmq（或缺省） */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RocketMQTemplate.class)
    @ConditionalOnBean(RocketMQTemplate.class)
    @ConditionalOnProperty(prefix = "aics.mq", name = "type", havingValue = "rocketmq", matchIfMissing = true)
    static class RocketMqPublisherConfiguration {

        @Bean
        @ConditionalOnMissingBean(MessagePublisher.class)
        public MessagePublisher rocketMqMessagePublisher(RocketMQTemplate rocketMqTemplate,
                                                        MqProperties properties) {
            log.info("消息中间件开关 aics.mq.type=rocketmq，装配 RocketMqMessagePublisher");
            return new RocketMqMessagePublisher(rocketMqTemplate, properties);
        }
    }

    /** Kafka 分支：classpath 有 KafkaTemplate 且开关为 kafka */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaTemplate.class)
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnProperty(prefix = "aics.mq", name = "type", havingValue = "kafka")
    static class KafkaPublisherConfiguration {

        @Bean
        @ConditionalOnMissingBean(MessagePublisher.class)
        @SuppressWarnings("unchecked")
        public MessagePublisher kafkaMessagePublisher(KafkaTemplate<?, ?> kafkaTemplate,
                                                      MqProperties properties,
                                                      TaskScheduler aicsKafkaDelayScheduler) {
            log.info("消息中间件开关 aics.mq.type=kafka，装配 KafkaMessagePublisher");
            return new KafkaMessagePublisher((KafkaTemplate<Object, Object>) kafkaTemplate,
                    properties, aicsKafkaDelayScheduler);
        }

        /**
         * Kafka 没有原生延迟消息，降级实现需要一个调度器。
         * 单线程 + 守护线程即可，随上下文关闭而释放
         * （线程池的 initialize() 由 Spring 调用 afterPropertiesSet 时完成，这里不手动调）。
         */
        @Bean
        @ConditionalOnMissingBean(name = "aicsKafkaDelayScheduler")
        public TaskScheduler aicsKafkaDelayScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("aics-kafka-delay-");
            scheduler.setDaemon(true);
            scheduler.setWaitForTasksToCompleteOnShutdown(false);
            return scheduler;
        }
    }
}
