package com.aics.chat.security;

/**
 * 内容审核器接口（3.2 F4）。
 *
 * <p>默认实现 {@link RegexContentReviewer} 为确定性正则审核；
 * 后续可替换为 LLM/第三方审核服务（保持接口不变，审核服务故障时由
 * {@link ContentSafetyService} 按配置降级）。</p>
 */
public interface ContentReviewer {

    /**
     * 审核一段文本。
     *
     * @param text  待审核文本
     * @param stage 环节：INPUT/OUTPUT
     * @return 审核结果
     */
    ContentReviewResult review(String text, String stage);
}
