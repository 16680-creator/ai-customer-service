package com.aics.mq.consumer.kafka;

import com.aics.mq.config.KafkaConsumerConfig;
import com.aics.mq.dto.WorkOrderMessage;
import com.aics.mq.entity.MqFailRecord;
import com.aics.mq.entity.WorkOrder;
import com.aics.mq.mapper.MqFailRecordMapper;
import com.aics.mq.mapper.WorkOrderMapper;
import com.aics.mq.producer.WorkOrderProducer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 死信主题消费者（Kafka 分支）：重试耗尽后被转发到
 * {@code work-order-topic-dlt} 的消息在这里落库为失败记录。
 * <p>
 * 与 RocketMQ 版本的对应关系：
 * {@code %DLQ%work-order-consumer-group} ↔ {@code work-order-topic-dlt}
 * —— 前者是 Broker 内建能力，后者是应用侧用
 * {@code DeadLetterPublishingRecoverer} 自己造出来的。</p>
 *
 * <p>幂等键：Kafka 消息没有 RocketMQ 那种全局 msgId，这里用
 * {@code topic-partition-offset} 作为天然唯一标识（配合 msg_id 唯一索引）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aics.mq", name = "type", havingValue = "kafka")
public class WorkOrderKafkaDlqListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WorkOrderMapper workOrderMapper;
    private final MqFailRecordMapper mqFailRecordMapper;

    @KafkaListener(
            topics = WorkOrderProducer.TOPIC + KafkaConsumerConfig.DLT_SUFFIX,
            groupId = "work-order-dlq-consumer-group",
            containerFactory = "workOrderDltContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, WorkOrderMessage> record) {
        String msgId = record.topic() + "-" + record.partition() + "-" + record.offset();
        log.warn("消费 Kafka 死信消息: msgId={}, key={}, value={}", msgId, record.key(), record.value());

        // 幂等：同一死信消息只落一条记录
        Long exists = mqFailRecordMapper.selectCount(new LambdaQueryWrapper<MqFailRecord>()
                .eq(MqFailRecord::getMsgId, msgId));
        if (exists != null && exists > 0) {
            log.warn("死信消息已落库，跳过: msgId={}", msgId);
            return;
        }

        WorkOrderMessage payload = record.value();
        if (payload == null || payload.getTicketNo() == null) {
            // 死信里混入非法消息体：只告警，不落业务记录（避免占用重推入口）
            log.error("死信消息体为空或缺少 ticketNo，跳过: msgId={}", msgId);
            return;
        }

        String body;
        try {
            body = MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("死信消息体序列化失败，跳过: msgId={}", msgId, e);
            return;
        }

        // 1. 落失败记录（PENDING，等待前端重推）
        MqFailRecord failRecord = new MqFailRecord();
        failRecord.setMsgId(msgId);
        failRecord.setTopic(WorkOrderProducer.TOPIC);
        failRecord.setConsumerGroup("work-order-consumer-group");
        failRecord.setBizKey(payload.getTicketNo());
        failRecord.setPayload(body);
        failRecord.setFailReason("Kafka 消费重试 3 次仍失败，进入死信主题 " + record.topic());
        failRecord.setReconsumeTimes(3);
        failRecord.setStatus("PENDING");
        mqFailRecordMapper.insert(failRecord);

        // 2. 工单标记 FAILED
        WorkOrder order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getTicketNo, payload.getTicketNo()));
        if (order != null) {
            WorkOrder update = new WorkOrder();
            update.setId(order.getId());
            update.setStatus("FAILED");
            workOrderMapper.updateById(update);
        }
        log.warn("Kafka 死信消息已落库: msgId={}, ticketNo={}", msgId, payload.getTicketNo());
    }
}
