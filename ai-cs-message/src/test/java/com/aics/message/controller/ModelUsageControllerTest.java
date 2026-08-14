package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.ModelUsageDTO;
import com.aics.message.service.ModelUsageService;
import com.aics.message.vo.ModelUsageStatsVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 模型用量计量控制器单元测试
 * <p>
 * TDD：验证控制器正确委托 Service 层并返回统一 {@link Result} 结构。
 * 纯 Mockito 直接调用（与模块既有约定一致），不加载 Spring 上下文。
 *
 * <h3>【测试设计】为什么验证"可空参数原样传 null"</h3>
 * <p>统计接口的 5 个过滤参数都可空：Controller 若错误地给默认值/丢弃参数，
 * 会改变统计口径。用 verify 断言"null 被原样透传"，确保 Controller 保持零默认值逻辑
 * （默认值策略只属于 Service 的条件布尔过滤与前端展示）。</p>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ModelUsageControllerTest {

    @Mock
    private ModelUsageService modelUsageService;

    @InjectMocks
    private ModelUsageController modelUsageController;

    // ==================== POST /api/model-usage/records ====================

    @Test
    @DisplayName("上报用量 - 委托 Service 并返回空结果")
    void recordUsage_delegatesAndReturnsResult() {
        ModelUsageDTO dto = new ModelUsageDTO();
        dto.setScenario("chat");
        dto.setModel("gpt-4o");

        Result<Void> result = modelUsageController.recordUsage(dto);

        assertEquals(200, result.getCode());
        verify(modelUsageService).recordUsage(dto);
    }

    // ==================== GET /api/model-usage/stats ====================

    @Test
    @DisplayName("统计用量 - 委托 Service 并返回统计 VO")
    void stats_delegatesAndReturnsResult() {
        ModelUsageStatsVO vo = new ModelUsageStatsVO();
        vo.setCallCount(2L);
        vo.setTotalTokens(450L);
        vo.setEstimatedCost(new BigDecimal("0.010000"));
        LocalDateTime start = LocalDateTime.of(2026, 8, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 14, 23, 59);
        when(modelUsageService.stats(1000L, "chat", "gpt-4o", start, end)).thenReturn(vo);

        Result<ModelUsageStatsVO> result = modelUsageController.stats(1000L, "chat", "gpt-4o", start, end);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2L, result.getData().getCallCount());
        verify(modelUsageService).stats(1000L, "chat", "gpt-4o", start, end);
    }

    @Test
    @DisplayName("统计用量 - 参数可空，全部为空时传 null")
    void stats_nullParams() {
        when(modelUsageService.stats(null, null, null, null, null))
                .thenReturn(new ModelUsageStatsVO());

        Result<ModelUsageStatsVO> result = modelUsageController.stats(null, null, null, null, null);

        assertEquals(200, result.getCode());
        verify(modelUsageService).stats(null, null, null, null, null);
    }
}
