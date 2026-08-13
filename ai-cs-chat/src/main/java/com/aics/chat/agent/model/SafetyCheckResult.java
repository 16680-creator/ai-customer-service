package com.aics.chat.agent.model;

/**
 * 输入安全检查结果
 *
 * @param passed 是否通过（false 表示拦截/降级）
 * @param reason 拦截原因
 */
public record SafetyCheckResult(boolean passed, String reason) {

    public static SafetyCheckResult pass() {
        return new SafetyCheckResult(true, null);
    }

    public static SafetyCheckResult block(String reason) {
        return new SafetyCheckResult(false, reason);
    }
}
