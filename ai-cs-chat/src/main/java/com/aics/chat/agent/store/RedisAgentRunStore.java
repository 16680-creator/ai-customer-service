package com.aics.chat.agent.store;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.context.AfterSaleContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis run 状态存储（多轮确认跨请求、多实例共享）
 *
 * <p>键：aics:agent:run:{runId}，TTL = 总超时 + 确认超时余量。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAgentRunStore implements AgentRunStore {

    private static final String KEY_PREFIX = "aics:agent:run:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AgentProperties properties;

    @Override
    public void save(AfterSaleContext context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            long ttlSeconds = properties.getTotalTimeoutMs() / 1000
                    + properties.getConfirmTimeoutMinutes() * 60L + 60;
            redisTemplate.opsForValue().set(KEY_PREFIX + context.getRunId(), json, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Agent run 状态写入 Redis 失败: runId={}, err={}", context.getRunId(), e.getMessage());
        }
    }

    @Override
    public Optional<AfterSaleContext> load(String runId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + runId);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, AfterSaleContext.class));
        } catch (Exception e) {
            log.warn("Agent run 状态读取 Redis 失败: runId={}, err={}", runId, e.getMessage());
            return Optional.empty();
        }
    }
}
