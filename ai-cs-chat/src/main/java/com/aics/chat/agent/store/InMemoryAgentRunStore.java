package com.aics.chat.agent.store;

import com.aics.chat.agent.context.AfterSaleContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 run 状态存储（测试与单实例场景）
 */
@Component
public class InMemoryAgentRunStore implements AgentRunStore {

    private final Map<String, AfterSaleContext> store = new ConcurrentHashMap<>();

    @Override
    public void save(AfterSaleContext context) {
        store.put(context.getRunId(), context);
    }

    @Override
    public Optional<AfterSaleContext> load(String runId) {
        return Optional.ofNullable(store.get(runId));
    }
}
