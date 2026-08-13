package com.aics.chat.agent.model;

import java.util.List;

/**
 * 售后规则资格判断结果
 *
 * @param eligible  是否满足条件
 * @param ruleId    命中的规则条款编号（无依据为空）
 * @param ruleContent 规则原文引用
 * @param reason    结论原因
 */
public record PolicyCheckResult(boolean eligible, String ruleId, String ruleContent,
                                String reason, List<String> citations) {

    public static PolicyCheckResult eligible(PolicyRule rule) {
        return new PolicyCheckResult(true, rule.id(), rule.content(),
                "满足规则 " + rule.id() + "（" + rule.days() + " 天期限内）", List.of(rule.id()));
    }

    public static PolicyCheckResult notEligible(PolicyRule rule, String reason) {
        return new PolicyCheckResult(false, rule.id(), rule.content(), reason, List.of(rule.id()));
    }

    public static PolicyCheckResult insufficient() {
        return new PolicyCheckResult(false, null, null, "知识库未命中相关售后规则，依据不足", List.of());
    }
}
