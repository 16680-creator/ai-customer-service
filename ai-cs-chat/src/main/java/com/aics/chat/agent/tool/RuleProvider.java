package com.aics.chat.agent.tool;

import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.agent.model.PolicyRule;

import java.util.List;

/**
 * 售后规则来源抽象
 *
 * <p>运行期使用知识库 RAG（{@link KnowledgeRuleProvider}），
 * 检索失败/未命中时降级静态种子规则（{@link StaticRuleProvider}），
 * 保证规则判断始终有确定性的兜底依据。</p>
 */
public interface RuleProvider {

    /**
     * 加载指定售后动作适用的规则（按期限升序）
     */
    List<PolicyRule> loadRules(AfterSaleActionType actionType);

    /**
     * 按知识库文档解析规则（标题 ASR-xxx，内容含期限）
     *
     * @param title   文档标题
     * @param content 文档内容
     * @return 规则（解析失败返回 null）
     */
    static PolicyRule parse(String title, String content) {
        // 空内容无法解析
        if (content == null || content.isBlank()) {
            return null;
        }
        // 动作类型：按内容关键词识别
        AfterSaleActionType actionType = null;
        if (content.contains("换货")) {
            actionType = AfterSaleActionType.EXCHANGE;
        } else if (content.contains("退货")) {
            actionType = AfterSaleActionType.RETURN;
        } else if (content.contains("退款")) {
            actionType = AfterSaleActionType.REFUND;
        }
        // 无法识别动作类型：非规则文档
        if (actionType == null) {
            return null;
        }
        // 期限：优先"工作日"，其次"天"
        java.util.regex.Matcher daysMatcher = java.util.regex.Pattern
                .compile("(\\d+)\\s*个?工作日").matcher(content);
        if (!daysMatcher.find()) {
            daysMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*天").matcher(content);
        }
        // 未找到期限：非规则文档
        if (!daysMatcher.find()) {
            return null;
        }
        int days;
        try {
            days = Integer.parseInt(daysMatcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
        // 条款编号：优先"条款编号：ASR-xxx"，否则用标题
        String id = title == null ? null : title.trim();
        java.util.regex.Matcher idMatcher = java.util.regex.Pattern
                .compile("(ASR-\\d+)").matcher(content);
        if (idMatcher.find()) {
            id = idMatcher.group(1);
        }
        // 解析成功：组装规则条目
        return new PolicyRule(id, actionType, days, content.trim());
    }
}
