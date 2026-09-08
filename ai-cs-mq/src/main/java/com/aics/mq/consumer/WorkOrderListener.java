package com.aics.mq.consumer;

import com.aics.mq.dto.WorkOrderMessage;
import com.aics.mq.entity.WorkOrder;
import com.aics.mq.mapper.WorkOrderMapper;
import com.aics.mq.producer.WorkOrderProducer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 工单消息消费者。
 * <p>
 * 消费失败处理链路（本演示的核心）：
 * <ul>
 *   <li>simulateFail=true 时抛出异常 → RocketMQ 按 {@code maxReconsumeTimes=3} 自动重试
 *      （默认延迟级别约 10s/30s/1m）</li>
 *   <li>重试耗尽后消息进入死信主题 {@code %DLQ%work-order-consumer-group}，
 *       由 {@link WorkOrderDlqListener} 落库失败记录</li>
 * </ul>
 * 消费逻辑本身是幂等的：按 ticketNo 更新工单状态，重复消费结果一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = WorkOrderProducer.TOPIC,
        consumerGroup = "work-order-consumer-group",
        maxReconsumeTimes = 3
)
public class WorkOrderListener implements RocketMQListener<WorkOrderMessage> {

    private final WorkOrderMapper workOrderMapper;

    @Override
    public void onMessage(WorkOrderMessage message) {
        log.info("收到工单消息: ticketNo={}, title={}, simulateFail={}",
                message.getTicketNo(), message.getTitle(), message.getSimulateFail());
        if (Boolean.TRUE.equals(message.getSimulateFail())) {
            // 模拟业务失败：抛出异常触发 RocketMQ 自动重试，重试耗尽后进死信队列
            throw new RuntimeException("模拟工单消费失败: ticketNo=" + message.getTicketNo());
        }
        // 正常消费：按工单号更新状态 NEW/FAILED -> DONE（幂等：重复消费结果一致）
        WorkOrder order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getTicketNo, message.getTicketNo()));
        if (order == null) {
            // 库里查不到说明写入链路不一致：记录告警并放弃（避免无限重试无意义消息）
            log.warn("工单不存在，跳过消费: ticketNo={}", message.getTicketNo());
            return;
        }
        WorkOrder update = new WorkOrder();
        update.setId(order.getId());
        update.setStatus("DONE");
        workOrderMapper.updateById(update);
        log.info("工单消费完成: ticketNo={}, status -> DONE", message.getTicketNo());
    }
}
