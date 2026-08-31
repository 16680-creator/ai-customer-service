package com.aics.order.event;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 支付成功事务事件监听器测试：锁定 AFTER_COMMIT 阶段与通知载荷。
 */
class OrderPaidEventListenerTest {

    @Test
    @DisplayName("监听器必须绑定 AFTER_COMMIT - 回滚事务不触发通知")
    void listenerMustUseAfterCommitPhase() throws Exception {
        Method method = OrderPaidEventListener.class.getMethod("handle", OrderPaidEvent.class);
        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);
        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
        assertTrue(annotation.fallbackExecution() == false,
                "无事务发布时不得执行：只有真实事务提交才允许推送支付成功通知");
    }

    @Test
    @DisplayName("事务提交后事件 - 投递 notify-topic 正确载荷")
    @SuppressWarnings("unchecked")
    void shouldPublishNotifyPayload() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        OrderPaidEventListener listener = new OrderPaidEventListener(template);

        listener.handle(new OrderPaidEvent("ORD-PAID-1", 42L));

        org.mockito.ArgumentCaptor<Map> captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(template).convertAndSend(eq("notify-topic"), captor.capture());
        Map<String, String> map = (Map<String, String>) captor.getValue();
        assertEquals("42", map.get("userId"));
        assertTrue(map.get("message").contains("ORD-PAID-1"));
    }

    @Test
    @DisplayName("MQ 投递失败 - 只告警不外抛（订单已提交不能反向回滚）")
    void mqFailureMustNotPropagate() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        doThrow(new RuntimeException("mq down")).when(template)
                .convertAndSend(eq("notify-topic"), any(Map.class));
        OrderPaidEventListener listener = new OrderPaidEventListener(template);

        listener.handle(new OrderPaidEvent("ORD-PAID-2", 43L));

        verify(template).convertAndSend(eq("notify-topic"), any(Map.class));
    }
}
