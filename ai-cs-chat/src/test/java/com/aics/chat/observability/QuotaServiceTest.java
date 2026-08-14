package com.aics.chat.observability;

import com.aics.chat.dto.ModelUsageQuotaVO;
import com.aics.chat.dto.ModelUsageStatsVO;
import com.aics.chat.dto.QuotaCheckResult;
import com.aics.chat.feign.ModelUsageFeignClient;
import com.aics.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * QuotaService 单元测试：Token/费用配额超限判定、未配置配额不限、Feign 异常 fail-open。
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private ModelUsageFeignClient feignClient;

    private QuotaService service;

    @BeforeEach
    void setUp() {
        service = new QuotaService(feignClient);
    }

    @Test
    @DisplayName("Token 配额超限：usedTokens >= quotaTokens 判定超限")
    void check_tokenExceeded() {
        ModelUsageQuotaVO quota = new ModelUsageQuotaVO();
        quota.setWindowType("DAILY");
        quota.setQuotaTokens(1000L);
        when(feignClient.getQuota(eq(1L), eq("chat"))).thenReturn(Result.success(quota));
        ModelUsageStatsVO stats = new ModelUsageStatsVO();
        stats.setTotalTokens(1500L);
        when(feignClient.getStats(eq(1L), eq("chat"), any(), any(), any())).thenReturn(Result.success(stats));

        QuotaCheckResult result = service.check(1L, "chat");

        assertTrue(result.isExceeded());
        assertEquals(1500L, result.getUsedTokens());
        assertEquals(1000L, result.getQuotaTokens());
    }

    @Test
    @DisplayName("费用配额超限：usedCost >= quotaCost 判定超限")
    void check_costExceeded() {
        ModelUsageQuotaVO quota = new ModelUsageQuotaVO();
        quota.setWindowType("MONTHLY");
        quota.setQuotaCost(new BigDecimal("50"));
        when(feignClient.getQuota(eq(2L), eq("agent"))).thenReturn(Result.success(quota));
        ModelUsageStatsVO stats = new ModelUsageStatsVO();
        stats.setEstimatedCost(new BigDecimal("60"));
        when(feignClient.getStats(eq(2L), eq("agent"), any(), any(), any())).thenReturn(Result.success(stats));

        QuotaCheckResult result = service.check(2L, "agent");

        assertTrue(result.isExceeded());
    }

    @Test
    @DisplayName("用量未超限：返回 notExceeded")
    void check_notExceeded() {
        ModelUsageQuotaVO quota = new ModelUsageQuotaVO();
        quota.setWindowType("DAILY");
        quota.setQuotaTokens(1000L);
        when(feignClient.getQuota(eq(1L), eq("chat"))).thenReturn(Result.success(quota));
        ModelUsageStatsVO stats = new ModelUsageStatsVO();
        stats.setTotalTokens(500L);
        when(feignClient.getStats(eq(1L), eq("chat"), any(), any(), any())).thenReturn(Result.success(stats));

        QuotaCheckResult result = service.check(1L, "chat");

        assertFalse(result.isExceeded());
    }

    @Test
    @DisplayName("未配置配额（quota 为 null）视为不限制")
    void check_noQuotaConfig() {
        when(feignClient.getQuota(eq(1L), eq("chat"))).thenReturn(Result.success(null));

        QuotaCheckResult result = service.check(1L, "chat");

        assertFalse(result.isExceeded());
    }

    @Test
    @DisplayName("Feign 异常 fail-open：按未超限降级")
    void check_feignFailure_failOpen() {
        when(feignClient.getQuota(eq(1L), eq("chat"))).thenThrow(new RuntimeException("down"));

        QuotaCheckResult result = service.check(1L, "chat");

        assertFalse(result.isExceeded());
    }

    @Test
    @DisplayName("userId 为 null 时不检查（未登录请求不受配额限制）")
    void check_nullUserId() {
        QuotaCheckResult result = service.check(null, "chat");
        assertFalse(result.isExceeded());
    }
}
