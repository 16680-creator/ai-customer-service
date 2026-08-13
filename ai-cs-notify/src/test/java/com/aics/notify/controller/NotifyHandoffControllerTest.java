package com.aics.notify.controller;

import com.aics.common.exception.GlobalExceptionHandler;
import com.aics.notify.dto.HandoffNoticeDTO;
import com.aics.notify.service.NotifyHandoffService;
import com.aics.notify.service.NotifyService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 转人工通知控制器单元测试（TDD Red 阶段编写）
 * MockMvc standalone：真实触发 @Valid 校验 + GlobalExceptionHandler 返回 BAD_REQUEST。
 */
class NotifyHandoffControllerTest {

    private MockMvc mockMvc;
    private NotifyService notifyService;
    private NotifyHandoffService notifyHandoffService;

    @BeforeEach
    void setUp() {
        notifyService = mock(NotifyService.class);
        notifyHandoffService = mock(NotifyHandoffService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new NotifyController(notifyService, notifyHandoffService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/notify/handoff - 正常委托并返回成功 Result 结构")
    void handoff_shouldDelegateAndReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/notify/handoff")
                        .contentType("application/json")
                        .content("{\"ticketNo\":\"AS20250601001\",\"userId\":1001,\"priority\":\"URGENT\",\"orderNo\":\"ORD001\",\"summary\":\"用户咨询退款进度\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(notifyHandoffService).sendHandoffNotice(any(HandoffNoticeDTO.class));
        verify(notifyService, never()).sendToUser(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/notify/handoff - 缺少必填字段返回 BAD_REQUEST")
    void handoff_invalidBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/notify/handoff")
                        .contentType("application/json")
                        .content("{\"userId\":1001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(notifyHandoffService, never()).sendHandoffNotice(any());
    }

    @Test
    @DisplayName("POST /api/notify/handoff - userId 缺失返回 BAD_REQUEST")
    void handoff_missingUserId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/notify/handoff")
                        .contentType("application/json")
                        .content("{\"ticketNo\":\"AS20250601001\",\"summary\":\"用户咨询退款进度\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
