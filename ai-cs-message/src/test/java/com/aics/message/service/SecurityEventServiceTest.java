package com.aics.message.service;

import com.aics.message.dto.SecurityEventDTO;
import com.aics.message.entity.SecurityEvent;
import com.aics.message.mapper.SecurityEventMapper;
import com.aics.message.service.impl.SecurityEventServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 安全事件服务单元测试（3.2 F7 审计留痕）。
 * <p>
 * 纯 Mockito 单测（与模块既有约定一致，Mapper mock，不加载 Spring 上下文）。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SecurityEventServiceTest {

    @Mock
    private SecurityEventMapper securityEventMapper;

    @InjectMocks
    private SecurityEventServiceImpl securityEventService;

    private SecurityEventDTO buildDTO() {
        SecurityEventDTO dto = new SecurityEventDTO();
        dto.setEventId("evt-1");
        dto.setType("PROMPT_INJECTION");
        dto.setStage("INPUT");
        dto.setUserId(1L);
        dto.setRule("SafetyGuardService");
        dto.setInputDigest("138****8000");
        dto.setAction("BLOCK");
        dto.setDetail("检测到提示词注入");
        return dto;
    }

    @Test
    @DisplayName("记录安全事件 - 成功落库")
    void record_success() {
        when(securityEventMapper.selectCount(any())).thenReturn(0L);
        when(securityEventMapper.insert(any(SecurityEvent.class))).thenReturn(1);

        securityEventService.record(buildDTO());

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventMapper).insert(captor.capture());
        SecurityEvent inserted = captor.getValue();
        assertEquals("evt-1", inserted.getEventId());
        assertEquals("PROMPT_INJECTION", inserted.getType());
        assertEquals("INPUT", inserted.getStage());
        assertEquals(1L, inserted.getUserId());
        assertEquals("138****8000", inserted.getInputDigest());
        assertEquals("BLOCK", inserted.getAction());
    }

    @Test
    @DisplayName("记录安全事件 - 同 eventId 幂等跳过（Feign 重试不产生重复审计）")
    void record_idempotent() {
        when(securityEventMapper.selectCount(any())).thenReturn(1L);

        securityEventService.record(buildDTO());

        verify(securityEventMapper, never()).insert(any(SecurityEvent.class));
    }

    @Test
    @DisplayName("记录安全事件 - 缺 eventId 直接跳过")
    void record_missingEventId() {
        SecurityEventDTO dto = buildDTO();
        dto.setEventId(null);

        securityEventService.record(dto);

        verify(securityEventMapper, never()).selectCount(any());
        verify(securityEventMapper, never()).insert(any(SecurityEvent.class));
    }
}
