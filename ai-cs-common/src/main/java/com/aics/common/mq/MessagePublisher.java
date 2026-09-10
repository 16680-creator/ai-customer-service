package com.aics.common.mq;

/**
 * 统一消息发布接口（消息中间件抽象层）。
 * <p>
 * 设计目标：业务代码只面向本接口编程，通过配置项 {@code aics.mq.type} 在
 * RocketMQ 与 Kafka 之间切换，而不改动业务逻辑。</p>
 *
 * <p>字段语义在两个 Broker 上的映射：</p>
 * <table border="1">
 *   <tr><th>抽象参数</th><th>RocketMQ</th><th>Kafka</th></tr>
 *   <tr><td>topic</td><td>Topic</td><td>Topic</td></tr>
 *   <tr><td>key</td><td>Tag（同 Topic 下二级分类）</td><td>Record Key（决定分区，同 Key 有序）</td></tr>
 * </table>
 *
 * <p><b>能力差异（务必知晓）</b>：RocketMQ 支持服务端「固定延迟级别」延迟消息，
 * Kafka 没有原生延迟消息，{@link #sendDelayed} 在 Kafka 实现里降级为进程内调度，
 * 进程重启会丢失。需要强可靠延迟消息的场景请保持在 RocketMQ 实现上。</p>
 *
 * @see MessageBrokerType
 */
public interface MessagePublisher {

    /**
     * 发送一条消息。
     * <p>同步语义：发送失败（网络异常 / Broker 拒绝 / 超时）直接抛
     * {@link MessagePublishException}，由调用方决定补偿策略。</p>
     *
     * @param topic   主题
     * @param key     二级分类/Routing Key，可为 {@code null}
     * @param payload 消息体，会被序列化（RocketMQ 走 JSON，Kafka 走 JsonSerializer）
     */
    void send(String topic, String key, Object payload);

    /**
     * 发送一条延迟消息。
     *
     * @param delayMillis 期望延迟毫秒数
     * @see MessagePublisher 类注释中的能力差异说明
     */
    void sendDelayed(String topic, String key, Object payload, long delayMillis);

    /**
     * 当前生效的中间件类型，便于日志与排查（例如启动时打印）。
     */
    MessageBrokerType brokerType();
}
