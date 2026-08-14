package com.aics.chat.agent.tool;

/**
 * Agent 工具抽象
 *
 * <p>实现类提供各自的类型化执行方法（由编排器直接调用，不做模型自由 ReAct），
 * 注册中心统一管理注册、状态权限与风险等级。</p>
 */
public interface AgentTool {

    /** 工具名（注册键，与 AgentStateMachine.allowedTools 一致） */
    String name();

    /** 风险等级 */
    RiskLevel riskLevel();

    /** 是否要求用户确认（写操作必须 true） */
    boolean requiresConfirmation();

    /** 风险等级 */
    enum RiskLevel {
        /** 只读工具（查询/检索/推荐） */
        READ,
        /** 写工具（创建售后申请、转人工工单等） */
        WRITE
    }
}
