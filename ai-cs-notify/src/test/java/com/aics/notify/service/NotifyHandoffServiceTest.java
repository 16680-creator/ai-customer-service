package com.aics.notify.service;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.notify.dto.HandoffNoticeDTO;
import com.aics.notify.service.impl.NotifyHandoffServiceImpl;
import com.aics.notify.websocket.NotifyWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 转人工通知服务单元测试（TDD Red 阶段编写）
 */
class NotifyHandoffServiceTest {

    private ObjectMapper objectMapper;
    private NotifyHandoffServiceImpl handoffService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handoffService = new NotifyHandoffServiceImpl(objectMapper);
    }

    private HandoffNoticeDTO buildDTO() {
        HandoffNoticeDTO dto = new HandoffNoticeDTO();
        dto.setTicketNo("AS20250601001");
        dto.setUserId(1001L);
        dto.setPriority("URGENT");
        dto.setOrderNo("ORD20250601001");
        dto.setSummary("用户咨询退款进度");
        return dto;
    }

    @Test
    @DisplayName("正常推送 - 序列化为含 event=HANDOFF 的 JSON 并按 userId 定向推送")
    void sendHandoffNotice_shouldPushJsonWithEvent() throws Exception {
        try (MockedStatic<NotifyWebSocketHandler> mocked = mockStatic(NotifyWebSocketHandler.class)) {
            handoffService.sendHandoffNotice(buildDTO());

            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            mocked.verify(() -> NotifyWebSocketHandler.sendMessageToUser(eq("1001"), jsonCaptor.capture()));

            JsonNode node = objectMapper.readTree(jsonCaptor.getValue());
            assertEquals("HANDOFF", node.get("event").asText());
            assertEquals("AS20250601001", node.get("ticketNo").asText());
            assertEquals(1001L, node.get("userId").asLong());
            assertEquals("URGENT", node.get("priority").asText());
            assertEquals("ORD20250601001", node.get("orderNo").asText());
            assertEquals("用户咨询退款进度", node.get("summary").asText());
        }
    }

    @Test
    @DisplayName("默认优先级 - 未显式设置时 JSON 携带 NORMAL")
    void sendHandoffNotice_defaultPriority() throws Exception {
        HandoffNoticeDTO dto = new HandoffNoticeDTO();
        dto.setTicketNo("AS20250601002");
        dto.setUserId(1002L);
        dto.setSummary("用户咨询物流");
        try (MockedStatic<NotifyWebSocketHandler> mocked = mockStatic(NotifyWebSocketHandler.class)) {
            handoffService.sendHandoffNotice(dto);

            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            mocked.verify(() -> NotifyWebSocketHandler.sendMessageToUser(eq("1002"), jsonCaptor.capture()));

            JsonNode node = objectMapper.readTree(jsonCaptor.getValue());
            assertEquals("NORMAL", node.get("priority").asText());
        }
    }

    @Test
    @DisplayName("用户不在线 - 推送静默不抛异常")
    void sendHandoffNotice_offlineUser_shouldNotThrow() {
        // 未建立任何 WebSocket 连接时调用真实静态方法：仅告警日志，不抛异常
        try (MockedStatic<NotifyWebSocketHandler> mocked =
                     mockStatic(NotifyWebSocketHandler.class, Mockito.CALLS_REAL_METHODS)) {
            assertDoesNotThrow(() -> handoffService.sendHandoffNotice(buildDTO()));
        }
    }

    @Test
    @DisplayName("序列化失败 - 抛 BusinessException(NOTIFY_SEND_FAIL)")
    void sendHandoffNotice_serializeFail_shouldThrow() {
        ObjectMapper failingMapper = Mockito.mock(ObjectMapper.class);
        when(failingMapper.valueToTree(any())).thenThrow(new IllegalStateException("序列化失败"));
        NotifyHandoffServiceImpl service = new NotifyHandoffServiceImpl(failingMapper);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.sendHandoffNotice(buildDTO()));
        assertEquals(ResultCode.NOTIFY_SEND_FAIL.getCode(), ex.getCode());
    }
}
