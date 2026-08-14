package com.aics.chat.observability;

import com.aics.chat.dto.ModelUsageQuotaVO;
import com.aics.chat.dto.ModelUsageStatsVO;
import com.aics.chat.dto.QuotaCheckResult;
import com.aics.chat.feign.ModelUsageFeignClient;
import com.aics.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 配额服务：按「用户 × 场景 × 时间窗口」检查累计用量是否超限。
 *
 * <p>实现：从 message 侧读取配额配置（{@code windowType} + 配额）与当前窗口累计用量
 * （stats 接口），任一维度（Token/费用）超限即判定超限。Feign 异常时按「未超限」降级
 * （fail-open），保证配额检查故障不影响对话主链路。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final ModelUsageFeignClient modelUsageFeignClient;

    /**
     * 检查用户在某场景当前窗口的用量是否超过配额。
     *
     * @param userId   用户 ID
     * @param scenario 场景
     * @return 超限结果（Feign 异常时按未超限降级）
     */
    public QuotaCheckResult check(Long userId, String scenario) {
        if (userId == null) {
            return QuotaCheckResult.notExceeded();
        }
        try {
            // 1. 读配额配置（不存在=未配置配额，不限制）
            // 学习点：先查"有没有配额"再查"用了多少"——配额是治理配置，用量是运行时事实，
            // 两者分离存储（model_usage_quota vs model_usage），避免每次调用都写配置表
            Result<ModelUsageQuotaVO> quotaResult = modelUsageFeignClient.getQuota(userId, scenario);
            ModelUsageQuotaVO quota = quotaResult == null ? null : quotaResult.getData();
            if (quota == null || (quota.getQuotaTokens() == null && quota.getQuotaCost() == null)) {
                return QuotaCheckResult.notExceeded();
            }
            // 2. 读当前窗口累计用量
            LocalDateTime windowStart = windowStart(quota.getWindowType());
            Result<ModelUsageStatsVO> statsResult = modelUsageFeignClient.getStats(
                    userId, scenario, null, windowStart, LocalDateTime.now());
            ModelUsageStatsVO stats = statsResult == null ? null : statsResult.getData();

            Long usedTokens = stats == null ? 0L : (stats.getTotalTokens() == null ? 0L : stats.getTotalTokens());
            BigDecimal usedCost = stats == null || stats.getEstimatedCost() == null
                    ? BigDecimal.ZERO : stats.getEstimatedCost();

            // 3. 任一维度超限即超限
            // 学习点：Token 与费用双维度是"双保险"——Token 上限防用量失控，
            // 费用上限防供应商调价/模型升级导致预算超支；两者是或关系，先触达哪个都拦截
            boolean tokenExceeded = quota.getQuotaTokens() != null && usedTokens >= quota.getQuotaTokens();
            boolean costExceeded = quota.getQuotaCost() != null && usedCost.compareTo(quota.getQuotaCost()) >= 0;
            if (tokenExceeded || costExceeded) {
                log.info("模型用量配额超限: userId={}, scenario={}, usedTokens={}, quotaTokens={}, usedCost={}, quotaCost={}",
                        userId, scenario, usedTokens, quota.getQuotaTokens(), usedCost, quota.getQuotaCost());
                return QuotaCheckResult.exceeded(usedTokens, quota.getQuotaTokens(), usedCost, quota.getQuotaCost());
            }
            return QuotaCheckResult.notExceeded();
        } catch (Exception e) {
            // fail-open：配额检查故障不阻断对话
            // 学习点：治理系统的失败策略要区分"可用性"与"安全性"——配额检查是成本治理而非
            // 安全门禁（不涉及越权/资金风险），因此选 fail-open（故障放行）而非 fail-closed：
            // 宁可多花一点钱，也不能让配额服务抖动导致整个客服系统不可用
            log.warn("配额检查失败，按未超限降级: userId={}, scenario={}, err={}",
                    userId, scenario, e.getMessage());
            return QuotaCheckResult.notExceeded();
        }
    }

    /** 按窗口类型计算当前窗口起始时间（DAILY=当日 00:00 / WEEKLY=本周一 00:00 / MONTHLY=本月 1 日 00:00） */
    private LocalDateTime windowStart(String windowType) {
        LocalDate today = LocalDate.now();
        return switch (windowType == null ? "DAILY" : windowType.toUpperCase()) {
            case "WEEKLY" -> today.with(DayOfWeek.MONDAY).atStartOfDay();
            case "MONTHLY" -> today.withDayOfMonth(1).atStartOfDay();
            default -> today.atStartOfDay();
        };
    }
}
