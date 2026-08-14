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

    // 线程安全的内存 Map（测试/单实例场景；重启即丢失）
    private final Map<String, AfterSaleContext> store = new ConcurrentHashMap<>();

    @Override
    public void save(AfterSaleContext context) {
        // 以 runId 为键直接覆盖写入
        store.put(context.getRunId(), context);
    }

    @Override
    public Optional<AfterSaleContext> load(String runId) {
        // 按 runId 读取，不存在返回空
        return Optional.ofNullable(store.get(runId));
    }
}
