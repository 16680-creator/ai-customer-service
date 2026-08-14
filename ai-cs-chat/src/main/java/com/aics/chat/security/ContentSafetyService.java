package com.aics.chat.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 内容安全服务（3.2 F4）：模型输入/输出双向审核。
 *
 * <p>行为契约（对应 Gherkin Feature 04）：</p>
 * <ul>
 *   <li>输入违规：拒答且不调用模型（调用方短路返回）；</li>
 *   <li>输出违规：拦截回答，调用方返回兜底文案或转人工；</li>
 *   <li>审核服务故障：按 {@code aics.security.content-fail-mode} 降级
 *       （BLOCK=默认拦截 / ALLOW=放行并告警），且不静默失败。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSafetyService {

    private final ContentReviewer contentReviewer;
    private final SecurityProperties properties;
    private final SecurityAuditRecorder auditRecorder;

    /**
     * 输入审核：违规返回拦截结果（调用方不得调用模型）。
     */
    public ContentReviewResult reviewInput(String text) {
        return safeReview(text, "INPUT");
    }

    /**
     * 输出审核：违规返回拦截结果（调用方使用兜底文案）。
     * 开关 {@code aics.security.content-output-check-enabled} 关闭时直接放行。
     */
    public ContentReviewResult reviewOutput(String text) {
        if (!properties.isContentOutputCheckEnabled()) {
            return ContentReviewResult.pass();
        }
        return safeReview(text, "OUTPUT");
    }

    /**
     * 审核并记录审计事件；审核器抛异常时按配置降级。
     */
    private ContentReviewResult safeReview(String text, String stage) {
        try {
            ContentReviewResult result = contentReviewer.review(text, stage);
            if (!result.passed()) {
                auditRecorder.record(SecurityEventType.CONTENT_REVIEW, stage, null,
                        result.category(), text, "BLOCK",
                        "内容审核拦截: " + result.reason());
            }
            return result;
        } catch (Exception e) {
            // 审核服务不可用：按配置降级（BLOCK 默认拦截 / ALLOW 放行并告警），不静默失败
            // 学习点：这是 fail-closed 与 fail-open 的工程抉择——
            //   BLOCK（fail-closed）：安全优先，审核不了就拒绝，适合强合规场景；
            //   ALLOW（fail-open）：可用性优先，审核不了就放行但必须告警，适合怕误伤用户体验的场景。
            // 无论哪种都必须记录 DEGRADE 审计事件，让“降级”这件事本身可追溯（不能静默失败）。
            log.warn("内容审核服务不可用，按配置降级: stage={}, failMode={}, err={}",
                    stage, properties.getContentFailMode(), e.getMessage());
            boolean block = "BLOCK".equalsIgnoreCase(properties.getContentFailMode());
            auditRecorder.record(SecurityEventType.CONTENT_REVIEW, "DEGRADE", null, "FAILOVER",
                    text, block ? "BLOCK" : "ALLOW", "审核服务不可用，按配置降级为"
                            + (block ? "拦截" : "放行"));
            return block
                    ? ContentReviewResult.block("FAILOVER", "审核服务不可用，已按配置拦截")
                    : ContentReviewResult.pass();
        }
    }
}
