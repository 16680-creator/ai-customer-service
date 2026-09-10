package com.aics.common.mq;

import com.aics.common.mq.kafka.KafkaMessagePublisher;
import com.aics.common.mq.rocketmq.RocketMqMessagePublisher;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 消息中间件开关契约测试：验证 {@code aics.mq.type} 的装配结果、
 * classpath 缺 MQ 客户端时的降级，以及用户自定义 Bean 的优先级。
 */
class MessagePublisherAutoConfigurationTest {

    /** RocketMQ + Kafka 双客户端都在 classpath（最接近 ai-cs-mq 的双栈场景） */
    private final ApplicationContextRunner dualStackRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessagePublisherAutoConfiguration.class))
            .withBean(RocketMQTemplate.class, () -> mock(RocketMQTemplate.class))
            .withBean("kafkaTemplate", KafkaTemplate.class, () -> mock(KafkaTemplate.class));

    @Test
    @DisplayName("未配置开关 - 默认装配 RocketMQ 实现（存量行为不变）")
    void shouldDefaultToRocketMq() {
        dualStackRunner.run(context -> {
            assertThat(context).hasSingleBean(MessagePublisher.class);
            assertThat(context).hasSingleBean(MqProperties.class);
            assertThat(context.getBean(MessagePublisher.class)).isInstanceOf(RocketMqMessagePublisher.class);
            assertThat(context.getBean(MessagePublisher.class).brokerType()).isEqualTo(MessageBrokerType.ROCKETMQ);
        });
    }

    @Test
    @DisplayName("aics.mq.type=kafka - 装配 Kafka 实现，且不再有 RocketMQ 实现")
    void shouldSwitchToKafka() {
        dualStackRunner
                .withPropertyValues("aics.mq.type=kafka")
                .run(context -> {
                    assertThat(context).hasSingleBean(MessagePublisher.class);
                    assertThat(context.getBean(MessagePublisher.class)).isInstanceOf(KafkaMessagePublisher.class);
                    assertThat(context.getBean(MessagePublisher.class).brokerType()).isEqualTo(MessageBrokerType.KAFKA);
                });
    }

    @Test
    @DisplayName("aics.mq.type=kafka - 延迟消息降级所需的调度器一并装配")
    void shouldProvideDelaySchedulerForKafka() {
        dualStackRunner
                .withPropertyValues("aics.mq.type=kafka")
                .run(context -> assertThat(context).hasBean("aicsKafkaDelayScheduler"));
    }

    @Test
    @DisplayName("aics.mq.type=rocketmq - 不装配 Kafka 的降级调度器")
    void shouldNotProvideKafkaSchedulerInRocketMqMode() {
        dualStackRunner
                .withPropertyValues("aics.mq.type=rocketmq")
                .run(context -> assertThat(context).doesNotHaveBean("aicsKafkaDelayScheduler"));
    }

    @Test
    @DisplayName("与真实 RocketMQ 自动配置共存 - 排序与 @ConditionalOnBean 都成立（开关读到真实 RocketMQTemplate）")
    void shouldIntegrateWithRealRocketMqAutoConfiguration() {
        // 用一个 mock 的 DefaultMQProducer 顶替真实生产者：
        // RocketMQAutoConfiguration 仍会走完整流程装配 RocketMQTemplate，但不会真的连 NameServer，
        // 既验证了「@AutoConfigureAfter 排序 → @ConditionalOnBean 命中」这条链路，又不拖慢测试。
        DefaultMQProducer producer = mock(DefaultMQProducer.class);
        new ApplicationContextRunner()
                // 故意把本配置声明在 RocketMQ 自动配置之前，证明排序靠 @AutoConfigureAfter 而非声明顺序
                .withConfiguration(AutoConfigurations.of(
                        MessagePublisherAutoConfiguration.class,
                        org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration.class))
                .withBean("defaultMQProducer", DefaultMQProducer.class, () -> producer)
                .withPropertyValues(
                        "rocketmq.name-server=127.0.0.1:9876",
                        "rocketmq.producer.group=aics-test-producer-group")
                .run(context -> {
                    assertThat(context).hasSingleBean(RocketMQTemplate.class);
                    assertThat(context).hasSingleBean(MessagePublisher.class);
                    assertThat(context.getBean(MessagePublisher.class)).isInstanceOf(RocketMqMessagePublisher.class);
                });
    }

    @Test
    @DisplayName("classpath 无 MQ 客户端 Bean（仅引了 common）- 整体不装配，应用仍可启动")
    void shouldBackOffWhenNoBrokerClient() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MessagePublisherAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(MessagePublisher.class));
    }

    @Test
    @DisplayName("业务方自定义 MessagePublisher - 自动配置让位")
    void shouldBackOffForUserDefinedPublisher() {
        MessagePublisher custom = new MessagePublisher() {
            @Override
            public void send(String topic, String key, Object payload) {
                // 测试替身：不需要真实发送
            }

            @Override
            public void sendDelayed(String topic, String key, Object payload, long delayMillis) {
                // 测试替身：不需要真实发送
            }

            @Override
            public MessageBrokerType brokerType() {
                return MessageBrokerType.KAFKA;
            }
        };
        dualStackRunner
                .withBean("customMessagePublisher", MessagePublisher.class, () -> custom)
                .run(context -> assertThat(context).getBean(MessagePublisher.class).isSameAs(custom));
    }
}
