package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.OnlineEvalRecordDTO;
import com.aics.message.dto.UserFeedbackDTO;
import com.aics.message.service.OnlineEvalService;
import com.aics.message.vo.OnlineEvalStatsVO;
import com.aics.message.vo.UserFeedbackVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 线上评估与反馈控制器单元测试
 * <p>
 * TDD：验证控制器正确委托 Service 层并返回统一 {@link Result} 结构。
 * 纯 Mockito 直接调用（与模块既有约定一致），不加载 Spring 上下文。
 *
 * <h3>【测试设计】为什么本测试类覆盖 4 个端点、6 个用例</h3>
 * <p>每个端点至少一个"委托成功"用例，统计/反馈列表额外覆盖"参数全空"与"存在/缺失"分支，
 * 保证 Controller 的薄透传层对所有入参形态（含 null）都不丢不改成 Contract。</p>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OnlineEvalControllerTest {

    @Mock
    private OnlineEvalService onlineEvalService;

    @InjectMocks
    private OnlineEvalController onlineEvalController;

    // ==================== POST /api/eval/online-records ====================

    @Test
    @DisplayName("上报评估 - 委托 Service 并返回空结果")
    void recordEval_delegatesAndReturnsResult() {
        OnlineEvalRecordDTO dto = new OnlineEvalRecordDTO();
        dto.setJudgeStatus("SUCCESS");

        Result<Void> result = onlineEvalController.recordEval(dto);

        assertEquals(200, result.getCode());
        verify(onlineEvalService).recordEval(dto);
    }

    // ==================== GET /api/eval/online-records/stats ====================

    @Test
    @DisplayName("统计 - 委托 Service 并返回统计 VO")
    void stats_delegatesAndReturnsResult() {
        OnlineEvalStatsVO vo = new OnlineEvalStatsVO();
        vo.setSampleCount(3L);
        vo.setScoredCount(2L);
        vo.setAvgLlmScore(4.5);
        LocalDateTime start = LocalDateTime.of(2026, 8, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 14, 23, 59);
        when(onlineEvalService.stats(start, end)).thenReturn(vo);

        Result<OnlineEvalStatsVO> result = onlineEvalController.stats(start, end);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(3L, result.getData().getSampleCount());
        assertEquals(4.5, result.getData().getAvgLlmScore());
        verify(onlineEvalService).stats(start, end);
    }

    @Test
    @DisplayName("统计 - 时间范围可空，全部为空时传 null")
    void stats_nullParams() {
        when(onlineEvalService.stats(null, null)).thenReturn(new OnlineEvalStatsVO());

        Result<OnlineEvalStatsVO> result = onlineEvalController.stats(null, null);

        assertEquals(200, result.getCode());
        verify(onlineEvalService).stats(null, null);
    }

    // ==================== POST /api/eval/feedback ====================

    @Test
    @DisplayName("上报反馈 - 委托 Service 并返回空结果")
    void saveFeedback_delegatesAndReturnsResult() {
        UserFeedbackDTO dto = new UserFeedbackDTO();
        dto.setFeedbackType("LIKE");

        Result<Void> result = onlineEvalController.saveFeedback(dto);

        assertEquals(200, result.getCode());
        verify(onlineEvalService).saveFeedback(dto);
    }

    // ==================== GET /api/eval/feedback ====================

    @Test
    @DisplayName("反馈列表 - 委托 Service 并返回列表")
    void listFeedback_delegatesAndReturnsResult() {
        UserFeedbackVO vo = new UserFeedbackVO();
        vo.setId(1L);
        vo.setFeedbackType("LIKE");
        LocalDateTime start = LocalDateTime.of(2026, 8, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 14, 23, 59);
        when(onlineEvalService.listFeedback("trace-1", start, end))
                .thenReturn(Collections.singletonList(vo));

        Result<List<UserFeedbackVO>> result = onlineEvalController.listFeedback("trace-1", start, end);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("LIKE", result.getData().get(0).getFeedbackType());
        verify(onlineEvalService).listFeedback("trace-1", start, end);
    }

    @Test
    @DisplayName("反馈列表 - 过滤条件可空")
    void listFeedback_nullParams() {
        when(onlineEvalService.listFeedback(null, null, null)).thenReturn(Collections.emptyList());

        Result<List<UserFeedbackVO>> result = onlineEvalController.listFeedback(null, null, null);

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
        verify(onlineEvalService).listFeedback(null, null, null);
    }
}
