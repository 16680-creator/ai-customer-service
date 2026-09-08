package com.aics.mq.consumer;

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
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 死信队列消费者：把重试耗尽的工单消息落库为失败记录。
 * <p>
 * RocketMQ 消费重试 {@code maxReconsumeTimes} 次仍失败后，消息进入死信主题
 * {@code %DLQ%<consumerGroup>}；本监听器消费死信，把消息体写入
 * mq_fail_record（PENDING），供前端查看与重新推送。
 * 落库前按 msg_id 查重（配合唯一约束）保证幂等，死信被重复投递时不会产生重复记录。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "%DLQ%work-order-consumer-group",
        consumerGroup = "work-order-dlq-consumer-group"
)
public class WorkOrderDlqListener implements RocketMQListener<MessageExt> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WorkOrderMapper workOrderMapper;
    private final MqFailRecordMapper mqFailRecordMapper;

    @Override
    public void onMessage(MessageExt message) {
        String body = message.getBody() == null ? "" : new String(message.getBody(), StandardCharsets.UTF_8);
        log.warn("消费死信消息: msgId={}, reconsumeTimes={}, body={}",
                message.getMsgId(), message.getReconsumeTimes(), body);

        // 幂等：同一死信消息只落一条记录
        Long exists = mqFailRecordMapper.selectCount(new LambdaQueryWrapper<MqFailRecord>()
                .eq(MqFailRecord::getMsgId, message.getMsgId()));
        if (exists != null && exists > 0) {
            log.warn("死信消息已落库，跳过: msgId={}", message.getMsgId());
            return;
        }

        WorkOrderMessage payload;
        try {
            payload = MAPPER.readValue(body, WorkOrderMessage.class);
        } catch (Exception e) {
            // 死信消息体不是合法工单 JSON（例如 DLQ 里混入的其他消息）：仅告警，不落业务记录
            log.error("死信消息体反序列化失败，跳过: msgId={}", message.getMsgId(), e);
            return;
        }

        // 1. 落失败记录（PENDING，等待前端重推）
        MqFailRecord record = new MqFailRecord();
        record.setMsgId(message.getMsgId());
        record.setTopic(WorkOrderProducer.TOPIC);
        record.setConsumerGroup("work-order-consumer-group");
        record.setBizKey(payload.getTicketNo());
        record.setPayload(body);
        record.setFailReason("消费重试 " + message.getReconsumeTimes() + " 次仍失败，进入死信队列");
        record.setReconsumeTimes((int) message.getReconsumeTimes());
        record.setStatus("PENDING");
        mqFailRecordMapper.insert(record);

        // 2. 工单标记 FAILED
        WorkOrder order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getTicketNo, payload.getTicketNo()));
        if (order != null) {
            WorkOrder update = new WorkOrder();
            update.setId(order.getId());
            update.setStatus("FAILED");
            workOrderMapper.updateById(update);
        }
        log.warn("死信消息已落库: msgId={}, ticketNo={}", message.getMsgId(), payload.getTicketNo());
    }
}
