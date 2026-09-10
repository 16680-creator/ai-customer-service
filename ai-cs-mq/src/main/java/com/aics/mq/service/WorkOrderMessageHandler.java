package com.aics.mq.service;

import com.aics.mq.dto.WorkOrderMessage;
import com.aics.mq.entity.WorkOrder;
import com.aics.mq.mapper.WorkOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单消息消费逻辑（与 Broker 无关的「业务处理」部分）。
 * <p>
 * RocketMQ 监听器（{@code WorkOrderListener}）与 Kafka 监听器
 * （{@code WorkOrderKafkaListener}）都委托到这里，保证切换中间件时
 * 业务语义完全一致——这正是抽象层要解决的问题：**变的是通道，不是逻辑**。</p>
 *
 * <p>幂等：按 ticketNo 更新状态，重复消费结果一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderMessageHandler {

    private final WorkOrderMapper workOrderMapper;

    /**
     * 处理一条工单消息。
     *
     * @throws RuntimeException 模拟消费失败，由各 Broker 的重试/死信机制接管
     */
    public void handle(WorkOrderMessage message) {
        log.info("收到工单消息: ticketNo={}, title={}, simulateFail={}",
                message.getTicketNo(), message.getTitle(), message.getSimulateFail());

        if (Boolean.TRUE.equals(message.getSimulateFail())) {
            // 模拟业务失败：抛出异常触发 Broker 自动重试，重试耗尽后进入死信通道
            throw new RuntimeException("模拟工单消费失败: ticketNo=" + message.getTicketNo());
        }

        // 正常消费：NEW/FAILED -> DONE（幂等：重复消费结果一致）
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
