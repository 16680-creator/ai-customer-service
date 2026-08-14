package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.SecurityEventDTO;
import com.aics.message.service.SecurityEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

/**
 * 安全事件控制器单元测试（3.2 F7 审计留痕）。
 * <p>
 * 验证控制器正确委托 Service 层并返回统一 {@link Result} 结构。
 * 纯 Mockito 直接调用（与模块既有约定一致），不加载 Spring 上下文。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SecurityEventControllerTest {

    @Mock
    private SecurityEventService securityEventService;

    @InjectMocks
    private SecurityEventController securityEventController;

    @Test
    @DisplayName("POST /api/security/events - 委托 Service 并返回统一结果")
    void record_delegatesAndReturnsResult() {
        SecurityEventDTO dto = new SecurityEventDTO();
        dto.setEventId("evt-1");
        dto.setType("TOOL_UNAUTHORIZED");
        dto.setStage("TOOL");
        dto.setUserId(1L);
        dto.setRule("create_after_sale");
        dto.setAction("BLOCK");

        Result<Void> result = securityEventController.record(dto);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        verify(securityEventService).record(dto);
    }
}
