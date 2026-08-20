package com.aics.chat.agent.tool;

import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.agent.model.PolicyCheckResult;
import com.aics.chat.agent.model.PolicyRule;
import com.aics.chat.agent.state.AgentStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后规则校验工具（只读）：结合规则期限与订单时间判断是否满足售后条件。
 *
 * <p>结论必须携带规则引用（条款编号），无依据时明确告知依据不足，不编造规则。</p>
 */
@Slf4j
@Component
public class PolicyCheckTool implements AgentTool {

    private final RuleProvider ruleProvider;

    public PolicyCheckTool(@Qualifier("knowledgeRuleProvider") RuleProvider ruleProvider) {
        this.ruleProvider = ruleProvider;
    }

    @Override
    public String name() {
        return AgentStateMachine.TOOL_POLICY_CHECK;
    }

    @Override
    public RiskLevel riskLevel() {
        return RiskLevel.READ;
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    /**
     * 校验订单是否满足售后条件
     *
     * @param actionType      售后动作
     * @param orderCreateTime 订单下单时间（期限从下单时间起算）
     * @return 结论（含规则引用）
     */
    public PolicyCheckResult check(AfterSaleActionType actionType, LocalDateTime orderCreateTime) {
        // 加载规则（知识库优先，静态种子兜底）
        List<PolicyRule> rules = ruleProvider.loadRules(actionType);
        // 无规则依据：判定依据不足，不编造结论
        if (rules.isEmpty()) {
            return PolicyCheckResult.insufficient();
        }
        // 取期限最短的规则做判定（最严口径）
        PolicyRule rule = rules.get(0);
        // 无法确定订单时间：无法判定是否在期限内
        if (orderCreateTime == null) {
            return PolicyCheckResult.notEligible(rule, "无法确定订单时间，无法判定是否在售后期限内");
        }
        // 订单已过天数 = 当前时间 - 下单时间
        long days = Duration.between(orderCreateTime, LocalDateTime.now()).toDays();
        // 期限内：资格满足（携带规则引用）
        if (days <= rule.days()) {
            log.info("售后规则校验通过: rule={}, orderDays={}", rule.id(), days);
            return PolicyCheckResult.eligible(rule);
        }
        // 超期：资格不满足（携带规则引用与超期原因）
        return PolicyCheckResult.notEligible(rule,
                "订单已下单 " + days + " 天，超出规则 " + rule.id() + " 规定的 " + rule.days() + " 天期限");
    }
}
