package com.aics.notify.service;

import com.aics.notify.dto.HandoffNoticeDTO;
import com.aics.notify.service.impl.NotifyHandoffServiceImpl;
import com.aics.notify.service.impl.NotifyServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 转人工通知 STOMP 测试：验证 payload 与 user destination，不再依赖旧静态 WebSocket 会话表。
 */
class NotifyHandoffServiceTest {

    private ObjectMapper objectMapper;
    private SimpMessagingTemplate messagingTemplate;
    private NotifyHandoffServiceImpl handoffService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messagingTemplate = mock(SimpMessagingTemplate.class);
        handoffService = new NotifyHandoffServiceImpl(objectMapper, messagingTemplate);
    }

    @Test
    @DisplayName("转人工通知 - JSON 含 HANDOFF 事件，定向发送到 /user/{id}/queue/notify")
    void sendHandoffNoticeShouldPushStompUserDestination() throws Exception {
        HandoffNoticeDTO dto = new HandoffNoticeDTO();
        dto.setTicketNo("AS20250601001");
        dto.setUserId(1001L);
        dto.setPriority("URGENT");
        dto.setOrderNo("ORD20250601001");
        dto.setSummary("用户咨询退款进度");

        handoffService.sendHandoffNotice(dto);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSendToUser(eq("1001"),
                eq(NotifyServiceImpl.USER_NOTIFY_DESTINATION), captor.capture());
        JsonNode node = objectMapper.readTree(captor.getValue());
        assertEquals("HANDOFF", node.get("event").asText());
        assertEquals("AS20250601001", node.get("ticketNo").asText());
        assertEquals(1001L, node.get("userId").asLong());
    }
}
