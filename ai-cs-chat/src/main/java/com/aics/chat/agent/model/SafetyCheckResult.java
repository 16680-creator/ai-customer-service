package com.aics.chat.agent.model;

/**
 * 输入安全检查结果
 *
 * @param passed 是否通过（false 表示拦截/降级）
 * @param reason 拦截原因
 */
public record SafetyCheckResult(boolean passed, String reason) {

    public static SafetyCheckResult pass() {
        // 通过：无拦截原因
        return new SafetyCheckResult(true, null);
    }

    public static SafetyCheckResult block(String reason) {
        // 拦截：携带具体原因（供轨迹与用户提示使用）
        return new SafetyCheckResult(false, reason);
    }
}
