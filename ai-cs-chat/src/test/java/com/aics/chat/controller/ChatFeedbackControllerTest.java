package com.aics.chat.controller;

import com.aics.chat.dto.UserFeedbackDTO;
import com.aics.chat.dto.UserFeedbackVO;
import com.aics.chat.feign.OnlineEvalFeignClient;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * ChatFeedbackController 单元测试：反馈参数校验（类型/评分）与 Feign 委托。
 */
@ExtendWith(MockitoExtension.class)
class ChatFeedbackControllerTest {

    @Mock
    private OnlineEvalFeignClient feignClient;

    private ChatFeedbackController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatFeedbackController(feignClient);
    }

    private UserFeedbackDTO dto(String type, Integer score) {
        UserFeedbackDTO dto = new UserFeedbackDTO();
        dto.setRequestId("req-1");
        dto.setSessionId(10L);
        dto.setUserId(1L);
        dto.setFeedbackType(type);
        dto.setScore(score);
        dto.setComment("很好");
        return dto;
    }

    @Test
    @DisplayName("合法反馈（LIKE + 评分）委托 Feign 落库")
    void submitFeedback_valid() {
        when(feignClient.saveFeedback(any())).thenReturn(Result.success());

        Result<Void> result = controller.submitFeedback(dto("LIKE", 4));

        assertTrue(result.isSuccess());
        verify(feignClient).saveFeedback(any());
    }

    @Test
    @DisplayName("非法反馈类型（非 LIKE/DISLIKE）抛参数错误")
    void submitFeedback_invalidType() {
        assertThrows(BusinessException.class,
                () -> controller.submitFeedback(dto("THUMBS_UP", 4)));
        verifyNoInteractions(feignClient);
    }

    @Test
    @DisplayName("评分超出 1-5 范围抛参数错误")
    void submitFeedback_invalidScore() {
        assertThrows(BusinessException.class,
                () -> controller.submitFeedback(dto("LIKE", 9)));
        verifyNoInteractions(feignClient);
    }

    @Test
    @DisplayName("requestId 未知也可提交（null 照常插入）")
    void submitFeedback_nullRequestId() {
        when(feignClient.saveFeedback(any())).thenReturn(Result.success());
        UserFeedbackDTO dto = dto("DISLIKE", null);
        dto.setRequestId(null);

        controller.submitFeedback(dto);

        verify(feignClient).saveFeedback(argThat(d -> d.getRequestId() == null));
    }

    @Test
    @DisplayName("查询反馈委托 Feign")
    void listFeedback_delegates() {
        when(feignClient.listFeedback(eq("req-1"), isNull(), isNull()))
                .thenReturn(Result.success(List.of(new UserFeedbackVO())));

        Result<List<UserFeedbackVO>> result = controller.listFeedback("req-1");

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}
