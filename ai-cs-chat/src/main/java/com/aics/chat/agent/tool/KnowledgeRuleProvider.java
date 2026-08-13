package com.aics.chat.agent.tool;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.agent.model.PolicyRule;
import com.aics.chat.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 知识库售后规则提供者（RAG 路径）
 *
 * <p>从「after-sale-rules」知识库检索规则文档并解析为 {@link PolicyRule}；
 * 检索失败或未命中时降级静态规则（{@link StaticRuleProvider}），
 * 保证规则判断不因检索服务抖动而中断。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRuleProvider implements RuleProvider {

    private final KnowledgeBaseService knowledgeBaseService;
    private final AgentProperties properties;
    private final StaticRuleProvider staticRuleProvider;

    @Override
    public List<PolicyRule> loadRules(AfterSaleActionType actionType) {
        List<PolicyRule> rules = new ArrayList<>();
        try {
            String query = actionType.getDesc() + " 规则 期限";
            List<Document> docs = knowledgeBaseService.search(
                    properties.getRuleKnowledgeBase(), query,
                    properties.getRuleTopK(), properties.getRuleSimilarityThreshold());
            for (Document doc : docs) {
                String title = doc.getMetadata() != null
                        ? String.valueOf(doc.getMetadata().getOrDefault("title", "")) : "";
                PolicyRule rule = RuleProvider.parse(title, doc.getText());
                if (rule != null && rule.actionType() == actionType) {
                    rules.add(rule);
                }
            }
        } catch (Exception e) {
            log.warn("售后规则知识库检索失败，降级静态规则: {}", e.getMessage());
        }
        if (rules.isEmpty()) {
            rules = staticRuleProvider.loadRules(actionType);
        }
        rules.sort(Comparator.comparingInt(PolicyRule::days));
        return rules;
    }
}
