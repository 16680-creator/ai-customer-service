package com.aics.chat.agent.tool;

import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.agent.model.PolicyRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 静态售后规则（兜底种子，与 deploy/mysql 售后规则种子文档保持一致）
 */
@Component
public class StaticRuleProvider implements RuleProvider {

    private static final Map<AfterSaleActionType, PolicyRule> RULES = Map.of(
            AfterSaleActionType.EXCHANGE, new PolicyRule("ASR-001", AfterSaleActionType.EXCHANGE, 15,
                    "条款编号：ASR-001；适用动作：换货（EXCHANGE）；条件：商品存在非人为质量问题；期限：自签收之日起 15 天内可申请换货。"),
            AfterSaleActionType.RETURN, new PolicyRule("ASR-002", AfterSaleActionType.RETURN, 7,
                    "条款编号：ASR-002；适用动作：退货（RETURN）；条件：商品完好、配件齐全；期限：自签收之日起 7 天内无理由退货。"),
            AfterSaleActionType.REFUND, new PolicyRule("ASR-003", AfterSaleActionType.REFUND, 3,
                    "条款编号：ASR-003；适用动作：退款（REFUND）；期限：退货确认完成后 3 个工作日内原路退款。")
    );

    @Override
    public List<PolicyRule> loadRules(AfterSaleActionType actionType) {
        // 按动作类型取种子规则（未覆盖的动作返回空列表）
        PolicyRule rule = RULES.get(actionType);
        return rule == null ? List.of() : List.of(rule);
    }
}
