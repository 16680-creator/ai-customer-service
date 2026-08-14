package com.aics.message.service;

import com.aics.message.dto.ModelUsageQuotaDTO;
import com.aics.message.entity.ModelUsageQuota;
import com.aics.message.mapper.ModelUsageQuotaMapper;
import com.aics.message.service.impl.ModelUsageQuotaServiceImpl;
import com.aics.message.vo.ModelUsageQuotaVO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 模型用量配额服务单元测试
 * <p>
 * TDD：先写测试（Red），再实现 {@link ModelUsageQuotaServiceImpl} 至通过（Green）。
 * 纯 Mockito 单测（与模块既有约定一致，Mapper 全部 mock），不加载 Spring 上下文。
 * 覆盖：upsert 插入（默认 DAILY）/ upsert 更新（可空字段不覆盖）/ 查询存在 / 查询缺失返回 null。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ModelUsageQuotaServiceTest {

    @Mock
    private ModelUsageQuotaMapper modelUsageQuotaMapper;

    @InjectMocks
    private ModelUsageQuotaServiceImpl modelUsageQuotaService;

    // ==================== upsertQuota ====================

    @Test
    @DisplayName("设置配额 - 不存在时插入，windowType 缺省为 DAILY")
    void upsertQuota_new_shouldInsert() {
        ModelUsageQuotaDTO dto = buildQuotaDTO();
        dto.setWindowType(null);
        when(modelUsageQuotaMapper.selectOne(any())).thenReturn(null);
        when(modelUsageQuotaMapper.insert(any(ModelUsageQuota.class))).thenReturn(1);

        modelUsageQuotaService.upsertQuota(dto);

        ArgumentCaptor<ModelUsageQuota> captor = ArgumentCaptor.forClass(ModelUsageQuota.class);
        verify(modelUsageQuotaMapper).insert(captor.capture());
        ModelUsageQuota inserted = captor.getValue();
        assertEquals(1000L, inserted.getUserId());
        assertEquals("chat", inserted.getScenario());
        assertEquals("DAILY", inserted.getWindowType(), "windowType 未传时应默认 DAILY");
        assertEquals(100000L, inserted.getQuotaTokens());
        assertEquals(0, new BigDecimal("50.000000").compareTo(inserted.getQuotaCost()));
        verify(modelUsageQuotaMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("设置配额 - 已存在时更新而非重复插入，可空字段不覆盖原值")
    void upsertQuota_existing_shouldUpdate() {
        ModelUsageQuota existing = new ModelUsageQuota();
        existing.setId(9L);
        existing.setUserId(1000L);
        existing.setScenario("chat");
        existing.setWindowType("DAILY");
        existing.setQuotaTokens(50000L);
        existing.setQuotaCost(new BigDecimal("20.000000"));
        existing.setPeriodStart(LocalDateTime.of(2026, 8, 14, 0, 0));
        when(modelUsageQuotaMapper.selectOne(any())).thenReturn(existing);

        ModelUsageQuotaDTO dto = buildQuotaDTO();
        dto.setWindowType("WEEKLY");
        dto.setQuotaCost(null); // 可空字段不覆盖原值
        dto.setPeriodStart(null); // 可空字段不覆盖原值
        modelUsageQuotaService.upsertQuota(dto);

        ArgumentCaptor<ModelUsageQuota> captor = ArgumentCaptor.forClass(ModelUsageQuota.class);
        verify(modelUsageQuotaMapper).updateById(captor.capture());
        ModelUsageQuota updated = captor.getValue();
        // 关键断言：更新后的对象保留了原 id（updateById 按主键定位）与未传字段的原值，
        // 证明 upsert 的"部分更新、可空字段不覆盖"契约成立；若实现误用整行覆盖，
        // quotaCost/periodStart 会被 DTO 的 null 冲掉，此断言即失败
        assertEquals(9L, updated.getId());
        assertEquals("WEEKLY", updated.getWindowType(), "windowType 应更新为新值");
        assertEquals(100000L, updated.getQuotaTokens(), "quotaTokens 应更新为新值");
        assertEquals(0, new BigDecimal("20.000000").compareTo(updated.getQuotaCost()),
                "quotaCost 未传时应保持原值");
        assertEquals(LocalDateTime.of(2026, 8, 14, 0, 0), updated.getPeriodStart(),
                "periodStart 未传时应保持原值");
        // 反向验证：更新路径绝不允许走 insert，否则"已存在则更新"契约被破坏
        verify(modelUsageQuotaMapper, never()).insert(any());
    }

    @Test
    @DisplayName("设置配额 - 按 userId+scenario 组合条件查询")
    void upsertQuota_queryByUserAndScenario() {
        when(modelUsageQuotaMapper.selectOne(any())).thenReturn(null);
        when(modelUsageQuotaMapper.insert(any(ModelUsageQuota.class))).thenReturn(1);

        modelUsageQuotaService.upsertQuota(buildQuotaDTO());

        ArgumentCaptor<Wrapper<ModelUsageQuota>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(modelUsageQuotaMapper).selectOne(wrapperCaptor.capture());
        LambdaQueryWrapper<ModelUsageQuota> wrapper = (LambdaQueryWrapper<ModelUsageQuota>) wrapperCaptor.getValue();
        // 两个 eq 条件（userId/scenario）：首个条件占 3 个片段，后续条件增 4 个片段（含隐式 AND），共 7 个 normal 片段。
        // 断言片段数=验证"查询条件按定位键精确组装"：若只按 userId 或只按 scenario 查，
        // 片段数会是 3 而非 7，upsert 就会定位错记录
        assertEquals(7, wrapper.getExpression().getNormal().size(), "查询条件应包含 userId/scenario");
    }

    // ==================== getQuota ====================

    @Test
    @DisplayName("查询配额 - 存在时返回完整 VO")
    void getQuota_found() {
        ModelUsageQuota quota = new ModelUsageQuota();
        quota.setUserId(1000L);
        quota.setScenario("chat");
        quota.setWindowType("DAILY");
        quota.setQuotaTokens(100000L);
        quota.setQuotaCost(new BigDecimal("50.000000"));
        quota.setPeriodStart(LocalDateTime.of(2026, 8, 14, 0, 0));
        when(modelUsageQuotaMapper.selectOne(any())).thenReturn(quota);

        ModelUsageQuotaVO vo = modelUsageQuotaService.getQuota(1000L, "chat");

        assertNotNull(vo);
        assertEquals(1000L, vo.getUserId());
        assertEquals("chat", vo.getScenario());
        assertEquals("DAILY", vo.getWindowType());
        assertEquals(100000L, vo.getQuotaTokens());
        assertEquals(0, new BigDecimal("50.000000").compareTo(vo.getQuotaCost()));
        assertEquals(LocalDateTime.of(2026, 8, 14, 0, 0), vo.getPeriodStart());
    }

    @Test
    @DisplayName("查询配额 - 不存在返回 null，不抛异常")
    void getQuota_notFound_returnsNull() {
        when(modelUsageQuotaMapper.selectOne(any())).thenReturn(null);

        ModelUsageQuotaVO vo = modelUsageQuotaService.getQuota(1000L, "rag");

        assertNull(vo);
    }

    // ==================== 测试数据构造 ====================

    private ModelUsageQuotaDTO buildQuotaDTO() {
        ModelUsageQuotaDTO dto = new ModelUsageQuotaDTO();
        dto.setUserId(1000L);
        dto.setScenario("chat");
        dto.setQuotaTokens(100000L);
        dto.setQuotaCost(new BigDecimal("50.000000"));
        return dto;
    }
}
