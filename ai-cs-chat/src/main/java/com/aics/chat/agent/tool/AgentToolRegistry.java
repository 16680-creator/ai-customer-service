package com.aics.chat.agent.tool;

import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.agent.state.AfterSaleState;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 工具注册中心
 *
 * <p>统一管理工具注册与查询；配合状态机校验「当前状态是否允许调用该工具」，
 * 以及「写操作是否已确认」，构成工具调用的双重门禁。</p>
 */
@Component
public class AgentToolRegistry {

    private final AgentStateMachine stateMachine;
    private final Map<String, AgentTool> tools = new HashMap<>();

    public AgentToolRegistry(AgentStateMachine stateMachine, List<AgentTool> toolList) {
        this.stateMachine = stateMachine;
        if (toolList != null) {
            toolList.forEach(t -> tools.put(t.name(), t));
        }
    }

    /**
     * 按名称获取工具（不存在抛业务异常）
     */
    public AgentTool get(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new BusinessException(ResultCode.AGENT_RUN_NOT_FOUND, "工具未注册: " + name);
        }
        return tool;
    }

    /**
     * 是否注册
     */
    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    /**
     * 工具是否要求确认
     */
    public boolean requiresConfirmation(String name) {
        AgentTool tool = tools.get(name);
        return tool != null && tool.requiresConfirmation();
    }

    /**
     * 校验：当前状态允许调用该工具（不通过抛异常）
     */
    public void assertToolAllowed(AfterSaleState state, String toolName) {
        if (!stateMachine.isToolAllowed(state, toolName)) {
            throw new BusinessException(ResultCode.AGENT_WRITE_OP_NOT_CONFIRMED,
                    "状态 " + state + " 不允许调用工具 " + toolName);
        }
    }

    /**
     * 已注册工具清单（审计/展示用）
     */
    public Map<String, AgentTool> all() {
        return Map.copyOf(tools);
    }
}
