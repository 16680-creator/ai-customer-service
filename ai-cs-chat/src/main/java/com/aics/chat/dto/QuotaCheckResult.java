package com.aics.chat.dto;

import lombok.Data;

/**
 * 配额超限判定结果（供上层决定降级或拒绝）。
 *
 * <h3>【AI 技术详解】为什么用"带数据的对象"而不是布尔值/枚举？</h3>
 * <p>调用方拿到"超限"后还需要回答两个问题：超了多少、限在哪。若只返回 boolean，
 * 上层还得再查一次配额明细才能构造降级提示；本对象把 used/quota 与 exceeded 打包返回，
 * 一次调用即可决定：</p>
 * <ul>
 *   <li>未超限：走正常调用（快速路径，不需要明细，用 {@link #notExceeded()} 零开销返回）；</li>
 *   <li>已超限：用 used/quota 构造友好提示（如"今日 Token 用量已达上限"），
 *       或按业务策略降级（摘要、简化回答）而非直接报错。</li>
 * </ul>
 * <p>对应 spec「超过配额时返回可被调用方识别的超限结果」。</p>
 */
@Data
public class QuotaCheckResult {

    /** 是否超限 */
    private boolean exceeded;

    /** 当前累计 Token */
    private Long usedTokens;

    /** Token 配额（NULL=不限） */
    private Long quotaTokens;

    /** 当前累计费用（元） */
    private java.math.BigDecimal usedCost;

    /** 费用配额（元，NULL=不限） */
    private java.math.BigDecimal quotaCost;

    // 工厂方法而非直接 new：语义化命名让调用点可读（"未超限"一眼可知），
    // 且未超限分支无需填充明细字段，避免调用方漏传导致数据不一致
    public static QuotaCheckResult notExceeded() {
        QuotaCheckResult r = new QuotaCheckResult();
        r.setExceeded(false);
        return r;
    }

    // 超限路径必须携带明细：调用方依赖 used/quota 构造降级提示或审计日志
    public static QuotaCheckResult exceeded(Long usedTokens, Long quotaTokens,
                                            java.math.BigDecimal usedCost, java.math.BigDecimal quotaCost) {
        QuotaCheckResult r = new QuotaCheckResult();
        r.setExceeded(true);
        r.setUsedTokens(usedTokens);
        r.setQuotaTokens(quotaTokens);
        r.setUsedCost(usedCost);
        r.setQuotaCost(quotaCost);
        return r;
    }
}
