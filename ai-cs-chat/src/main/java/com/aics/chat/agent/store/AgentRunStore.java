package com.aics.chat.agent.store;

import com.aics.chat.agent.context.AfterSaleContext;

import java.util.Optional;

/**
 * Agent run 状态存取（多轮对话共享，接口化便于 Redis / 内存实现切换）
 */
public interface AgentRunStore {

    /**
     * 保存 run 上下文
     */
    void save(AfterSaleContext context);

    /**
     * 按 runId 加载上下文
     */
    Optional<AfterSaleContext> load(String runId);
}
