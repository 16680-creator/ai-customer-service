package com.aics.message.service;

import com.aics.message.dto.ModelUsageDTO;
import com.aics.message.entity.ModelUsage;
import com.aics.message.mapper.ModelUsageMapper;
import com.aics.message.service.impl.ModelUsageServiceImpl;
import com.aics.message.vo.ModelUsageStatsVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 模型用量计量服务单元测试
 * <p>
 * TDD：先写测试（Red），再实现 {@link ModelUsageServiceImpl} 至通过（Green）。
 * 纯 Mockito 单测（与模块既有约定一致，Mapper 全部 mock），不加载 Spring 上下文。
 * 覆盖：totalTokens 兜底计算 / 显式 totalTokens / estimated 缺省 / 统计内存聚合 / 空数据。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ModelUsageServiceTest {

    @Mock
    private ModelUsageMapper modelUsageMapper;

    @InjectMocks
    private ModelUsageServiceImpl modelUsageService;

    // ==================== recordUsage ====================

    @Test
    @DisplayName("记录用量 - totalTokens 未传时按 input+output 兜底计算")
    void recordUsage_totalTokensDefault() {
        ModelUsageDTO dto = buildUsageDTO();
        dto.setInputTokens(120);
        dto.setOutputTokens(80);
        dto.setTotalTokens(null);
        when(modelUsageMapper.insert(any(ModelUsage.class))).thenReturn(1);

        modelUsageService.recordUsage(dto);

        ArgumentCaptor<ModelUsage> captor = ArgumentCaptor.forClass(ModelUsage.class);
        verify(modelUsageMapper).insert(captor.capture());
        ModelUsage inserted = captor.getValue();
        assertEquals(120, inserted.getInputTokens());
        assertEquals(80, inserted.getOutputTokens());
        assertEquals(200, inserted.getTotalTokens(), "totalTokens 未传时应为 input+output");
        assertEquals("chat", inserted.getScenario());
        assertEquals("gpt-4o", inserted.getModel());
        assertEquals("SUCCESS", inserted.getStatus());
        assertFalse(inserted.getEstimated(), "estimated 未传时应为默认 false");
    }

    @Test
    @DisplayName("记录用量 - 显式 totalTokens 时以显式值为准")
    void recordUsage_explicitTotalTokens() {
        ModelUsageDTO dto = buildUsageDTO();
        dto.setInputTokens(100);
        dto.setOutputTokens(50);
        dto.setTotalTokens(300);
        dto.setEstimated(true);
        dto.setEstimatedCost(new BigDecimal("0.012000"));
        when(modelUsageMapper.insert(any(ModelUsage.class))).thenReturn(1);

        modelUsageService.recordUsage(dto);

        ArgumentCaptor<ModelUsage> captor = ArgumentCaptor.forClass(ModelUsage.class);
        verify(modelUsageMapper).insert(captor.capture());
        ModelUsage inserted = captor.getValue();
        assertEquals(300, inserted.getTotalTokens(), "显式 totalTokens 应以显式值为准");
        assertTrue(inserted.getEstimated());
        assertEquals(0, new BigDecimal("0.012000").compareTo(inserted.getEstimatedCost()));
    }

    @Test
    @DisplayName("记录用量 - input/output 均为 null 时 totalTokens 为 0")
    void recordUsage_nullTokens_totalZero() {
        ModelUsageDTO dto = buildUsageDTO();
        dto.setInputTokens(null);
        dto.setOutputTokens(null);
        dto.setTotalTokens(null);
        when(modelUsageMapper.insert(any(ModelUsage.class))).thenReturn(1);

        modelUsageService.recordUsage(dto);

        ArgumentCaptor<ModelUsage> captor = ArgumentCaptor.forClass(ModelUsage.class);
        verify(modelUsageMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getTotalTokens());
    }

    // ==================== stats ====================

    @Test
    @DisplayName("统计用量 - 过滤条件正确且内存聚合各维度求和")
    void stats_aggregates() {
        ModelUsage u1 = new ModelUsage();
        u1.setInputTokens(100);
        u1.setOutputTokens(50);
        u1.setTotalTokens(150);
        u1.setEstimatedCost(new BigDecimal("0.010000"));
        ModelUsage u2 = new ModelUsage();
        u2.setInputTokens(200);
        u2.setOutputTokens(100);
        u2.setTotalTokens(300);
        u2.setEstimatedCost(null); // 费用为 null 不应计入求和
        when(modelUsageMapper.selectList(any())).thenReturn(Arrays.asList(u1, u2));

        LocalDateTime start = LocalDateTime.of(2026, 8, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 14, 23, 59);
        ModelUsageStatsVO vo = modelUsageService.stats(1000L, "chat", "gpt-4o", start, end);

        assertEquals(2L, vo.getCallCount());
        assertEquals(300L, vo.getInputTokens());
        assertEquals(150L, vo.getOutputTokens());
        assertEquals(450L, vo.getTotalTokens());
        // 为什么用 compareTo 而非 equals 断言 BigDecimal：equals 要求 scale 一致
        // （"0.01" != "0.010"），compareTo 只比数值，与 DECIMAL(12,6) 语义一致
        assertEquals(0, new BigDecimal("0.010000").compareTo(vo.getEstimatedCost()),
                "费用求和应跳过 null 记录");

        // 过滤条件传递正确：5 个过滤条件（userId/scenario/model/起始时间/结束时间），
        // 首个条件占 3 个片段（列名/运算符/参数），后续条件各增 4 个片段（含隐式 AND），
        // 共 3 + 4×4 = 19 个 normal 片段。
        // 为什么断言片段数：这是"条件真的传给了 Mapper"的纯 Mockito 可观测面
        // （getSqlSegment() 需要实体 lambda 缓存，纯单测下不可用）
        ArgumentCaptor<Wrapper<ModelUsage>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(modelUsageMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<ModelUsage> wrapper = (LambdaQueryWrapper<ModelUsage>) wrapperCaptor.getValue();
        assertEquals(19, wrapper.getExpression().getNormal().size(),
                "应生成 userId/scenario/model/startTime/endTime 五个过滤条件");
    }

    @Test
    @DisplayName("统计用量 - 无数据时返回全 0 统计")
    void stats_noData() {
        when(modelUsageMapper.selectList(any())).thenReturn(Collections.emptyList());

        ModelUsageStatsVO vo = modelUsageService.stats(null, null, null, null, null);

        assertEquals(0L, vo.getCallCount());
        assertEquals(0L, vo.getInputTokens());
        assertEquals(0L, vo.getOutputTokens());
        assertEquals(0L, vo.getTotalTokens());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getEstimatedCost()));
    }

    // ==================== 测试数据构造 ====================

    private ModelUsageDTO buildUsageDTO() {
        ModelUsageDTO dto = new ModelUsageDTO();
        dto.setRequestId("trace-1");
        dto.setUserId(1000L);
        dto.setSessionId(100L);
        dto.setScenario("chat");
        dto.setProvider("openai");
        dto.setModel("gpt-4o");
        return dto;
    }
}
