package com.aics.notify.controller;

import com.aics.common.result.Result;
import com.aics.notify.service.NotifyHandoffService;
import com.aics.notify.service.NotifyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotifyController 既有端点单元测试（send / broadcast / online）
 */
@ExtendWith(MockitoExtension.class)
class NotifyControllerTest {

    @Mock
    private NotifyService notifyService;
    @Mock
    private NotifyHandoffService notifyHandoffService;

    @InjectMocks
    private NotifyController notifyController;

    @Test
    @DisplayName("POST /api/notify/send - 返回成功")
    void sendToUser_shouldReturnSuccess() {
        doNothing().when(notifyService).sendToUser("1001", "hello");

        Result<Void> result = notifyController.sendToUser("1001", "hello");

        assertEquals(200, result.getCode());
        verify(notifyService).sendToUser("1001", "hello");
    }

    @Test
    @DisplayName("POST /api/notify/broadcast - 返回成功")
    void broadcast_shouldReturnSuccess() {
        doNothing().when(notifyService).broadcast("hello-all");

        Result<Void> result = notifyController.broadcast("hello-all");

        assertEquals(200, result.getCode());
        verify(notifyService).broadcast("hello-all");
    }

    @Test
    @DisplayName("GET /api/notify/online - 返回在线用户数")
    void getOnlineCount_shouldReturn() {
        when(notifyService.getOnlineCount()).thenReturn(5);

        Result<Integer> result = notifyController.getOnlineCount();

        assertEquals(200, result.getCode());
        assertEquals(5, result.getData());
    }
}
