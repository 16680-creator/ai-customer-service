package com.aics.chat.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Sentinel 流控规则装配（代码注册模式）。
 *
 * <p>学习要点：规则来源有三种——代码注册（本类，随应用发布）、Dashboard 控制台推送（内存态，
 * 重启即失）、Nacos 数据源持久化（规则中心化，需引入 sentinel-datasource-nacos）。
 * 这里先用代码注册把「限流兜底」固化进应用，Dashboard/规则中心作为后续演进方向。</p>
 *
 * <p>规则设计：AI 对话链路的瓶颈在 LLM 供应商侧，QPS 超限后走 blockHandler 友好降级，
 * 而不是让请求堆到上游被供应商 429（触发 Resilience4j 熔断雪崩）。
 * 分工：Sentinel 管「入口限流」，Resilience4j 管「对下游的熔断/超时/重试」。</p>
 */
@Slf4j
@Configuration
public class SentinelFlowConfig {

    @PostConstruct
    public void initFlowRules() {
        FlowRule sendRule = new FlowRule();
        sendRule.setResource(SentinelRules.RESOURCE_CHAT_SEND);
        sendRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        sendRule.setCount(SentinelRules.CHAT_SEND_QPS_THRESHOLD);
        sendRule.setControlBehavior(SentinelRules.CONTROL_BEHAVIOR_WARM_UP);
        sendRule.setWarmUpPeriodSec(SentinelRules.WARM_UP_PERIOD_SEC);

        FlowRule ragRule = new FlowRule();
        ragRule.setResource(SentinelRules.RESOURCE_CHAT_RAG);
        ragRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        ragRule.setCount(SentinelRules.CHAT_RAG_QPS_THRESHOLD);

        FlowRuleManager.loadRules(List.of(sendRule, ragRule));
        log.info("Sentinel 流控规则已注册: chat-send QPS<={} (WarmUp {}s), chat-rag QPS<={}",
                SentinelRules.CHAT_SEND_QPS_THRESHOLD, SentinelRules.WARM_UP_PERIOD_SEC,
                SentinelRules.CHAT_RAG_QPS_THRESHOLD);
    }
}
