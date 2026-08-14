package com.aics.chat.security;

/**
 * 内容审核结果（3.2 F4）。
 *
 * @param passed   是否通过审核
 * @param category 命中分类（ABUSE/ILLEGAL/PORNO/SELF_HARM/FAILOVER 等）
 * @param reason   拒绝原因（通过时为 null）
 */
public record ContentReviewResult(boolean passed, String category, String reason) {

    public static ContentReviewResult pass() {
        return new ContentReviewResult(true, null, null);
    }

    public static ContentReviewResult block(String category, String reason) {
        return new ContentReviewResult(false, category, reason);
    }
}
