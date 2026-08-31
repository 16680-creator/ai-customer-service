package com.aics.notify.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * NotifyServiceImpl 单测：通知通过 STOMP user destination/topic 推送。
 */
class NotifyServiceImplTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final NotifyServiceImpl notifyService = new NotifyServiceImpl(messagingTemplate);

    @Test
    @DisplayName("sendToUser - 委托 STOMP user destination")
    void sendToUserShouldDelegateToStompUserDestination() {
        notifyService.sendToUser("1001", "hello");
        verify(messagingTemplate).convertAndSendToUser("1001", NotifyServiceImpl.USER_NOTIFY_DESTINATION, "hello");
    }

    @Test
    @DisplayName("broadcast - 委托 STOMP topic")
    void broadcastShouldDelegateToStompTopic() {
        notifyService.broadcast("hello-all");
        verify(messagingTemplate).convertAndSend("/topic/notify", "hello-all");
    }

    @Test
    @DisplayName("getOnlineCount - simple broker 不维护跨会话计数，返回0")
    void getOnlineCountShouldReturnZero() {
        assertEquals(0, notifyService.getOnlineCount());
    }
}
