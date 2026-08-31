package com.aics.chat.history.mongo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Mongo 对话归档写入器。
 *
 * <p>默认关闭：现有 Redis 热上下文 + RocketMQ/message 表持久化不受影响；启用后是
 * 最佳努力第三路归档，用于文档式审计与未来分页检索，失败只告警不阻断 AI 回复。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aics.chat.mongo-archive", name = "enabled", havingValue = "true")
public class MongoChatHistoryArchiveService {

    private final ChatMessageArchiveRepository repository;

    public void archive(String sessionKey, String role, String content) {
        try {
            repository.save(new ChatMessageArchiveDocument(
                    null, sessionKey, extractUserId(sessionKey), role, content, Instant.now()));
        } catch (Exception e) {
            log.warn("Mongo 对话归档失败（不阻断对话）: sessionKey={}, err={}", sessionKey, e.getMessage());
        }
    }

    private String extractUserId(String sessionKey) {
        // 当前 sessionKey 格式由调用方决定；无法稳定解析时保留为空，后续可从认证上下文补齐。
        return null;
    }
}
