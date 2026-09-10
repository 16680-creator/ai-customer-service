package com.aics.mq.service;

import com.aics.mq.dto.WorkOrderMessage;
import com.aics.mq.entity.WorkOrder;
import com.aics.mq.mapper.WorkOrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 工单消费逻辑单测（与 Broker 无关）。
 * <p>这段逻辑同时被 RocketMQ 与 Kafka 监听器复用，因此这里是双栈语义一致性的锚点。</p>
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderMessageHandlerTest {

    @Mock
    private WorkOrderMapper workOrderMapper;

    @InjectMocks
    private WorkOrderMessageHandler handler;

    private static WorkOrderMessage message(String ticketNo, Boolean simulateFail) {
        WorkOrderMessage message = new WorkOrderMessage();
        message.setTicketNo(ticketNo);
        message.setTitle("无法退款");
        message.setSimulateFail(simulateFail);
        return message;
    }

    @Test
    @DisplayName("正常消息 - 工单状态更新为 DONE")
    void shouldMarkDone() {
        WorkOrder existing = new WorkOrder();
        existing.setId(7L);
        existing.setTicketNo("WO123");
        when(workOrderMapper.selectOne(any())).thenReturn(existing);

        handler.handle(message("WO123", false));

        ArgumentCaptor<WorkOrder> update = ArgumentCaptor.forClass(WorkOrder.class);
        verify(workOrderMapper).updateById(update.capture());
        assertThat(update.getValue().getId()).isEqualTo(7L);
        assertThat(update.getValue().getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("simulateFail=true - 抛异常交给 Broker 重试，不触碰数据库")
    void shouldThrowToTriggerRetry() {
        assertThatThrownBy(() -> handler.handle(message("WO123", true)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("WO123");

        verifyNoInteractions(workOrderMapper);
    }

    @Test
    @DisplayName("工单不存在 - 告警并放弃，不更新也不抛异常（避免无意义重试）")
    void shouldSkipWhenOrderMissing() {
        when(workOrderMapper.selectOne(any())).thenReturn(null);

        handler.handle(message("WO404", false));

        verify(workOrderMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("同一消息重复消费 - 幂等：仍然只把状态写成 DONE")
    void shouldBeIdempotent() {
        WorkOrder existing = new WorkOrder();
        existing.setId(9L);
        existing.setTicketNo("WO999");
        when(workOrderMapper.selectOne(any())).thenReturn(existing);

        handler.handle(message("WO999", false));
        handler.handle(message("WO999", false));

        verify(workOrderMapper, org.mockito.Mockito.times(2)).updateById(any());
    }
}
