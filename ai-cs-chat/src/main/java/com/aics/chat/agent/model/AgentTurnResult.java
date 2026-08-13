package com.aics.chat.agent.model;

import java.util.List;

/**
 * Agent 单轮执行结果（返回给前端）
 *
 * @param runId             执行 ID
 * @param state             当前状态
 * @param intents           识别意图
 * @param reply             回复文本
 * @param needsUserInput    是否需要用户继续输入
 * @param routeToNormalChat 是否路由回普通对话
 * @param confirmationToken 写操作确认凭证（等待确认时返回）
 * @param actionPlan        待确认操作摘要
 * @param candidates        候选列表（候选订单号等）
 * @param handoff           转人工信息
 * @param applicationNo     售后申请单号（完成后返回）
 * @param errorCode         错误码（失败时）
 */
public record AgentTurnResult(String runId, String state, List<AgentIntentType> intents,
                              String reply, boolean needsUserInput, boolean routeToNormalChat,
                              String confirmationToken, AgentActionPlan actionPlan,
                              List<String> candidates, HandoffInfo handoff,
                              String applicationNo, String errorCode) {

    public static AgentTurnResult of(String runId, String state, List<AgentIntentType> intents,
                                     String reply, boolean needsUserInput, boolean routeToNormalChat,
                                     String confirmationToken, AgentActionPlan actionPlan,
                                     List<String> candidates, HandoffInfo handoff,
                                     String applicationNo, String errorCode) {
        return new AgentTurnResult(runId, state, intents, reply, needsUserInput, routeToNormalChat,
                confirmationToken, actionPlan, candidates, handoff, applicationNo, errorCode);
    }
}
