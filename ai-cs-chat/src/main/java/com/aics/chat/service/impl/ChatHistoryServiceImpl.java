package com.aics.chat.service.impl;

import com.aics.chat.dto.ChatHistoryMessage;
import com.aics.chat.feign.MessageFeignClient;
import com.aics.chat.mq.ChatMessageProducer;
import com.aics.chat.service.ChatHistoryService;
import com.aics.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话历史存储实现
 * Redis List 结构：key = chat:history:{sessionKey}，元素为 ChatHistoryMessage 的 JSON
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MessageFeignClient messageFeignClient;
    private final ChatMessageProducer chatMessageProducer;

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "chat:history:";

    /**
     * 加载会话历史：Redis LRANGE 优先；未命中时经 Feign 回源 ai-cs-message 并重建缓存；
     * 任意环节失败均降级为空列表，保证对话主流程不中断。
     */
    @Override
    public List<ChatHistoryMessage> load(String sessionKey) {
        String key = buildKey(sessionKey);
        // 1. Redis 优先读
        try {
            List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
            if (!CollectionUtils.isEmpty(raw)) {
                return deserialize(raw);
            }
        } catch (Exception e) {
            log.warn("读取 Redis 会话历史失败，走回源: sessionKey={}, err={}", sessionKey, e.getMessage());
        }

        // 2. 未命中 → 回源 message 表并重建缓存
        return fallbackToMessage(sessionKey, key);
    }

    /**
     * 追加一条会话消息：双写 Redis 热缓存（RPUSH）+ RocketMQ（最终落库 chat_message 表）。
     * Redis 写失败不影响 MQ 投递；MQ 投递失败仅打 warn 日志，不抛异常（消息丢失风险由调用方承担）。
     */
    @Override
    public void append(String sessionKey, String role, String content) {
        String key = buildKey(sessionKey);
        // 1. Redis 热缓存
        try {
            String json = objectMapper.writeValueAsString(new ChatHistoryMessage(role, content));
            redisTemplate.opsForList().rightPush(key, json);
        } catch (Exception e) {
            log.warn("写入 Redis 会话历史失败: sessionKey={}, err={}", sessionKey, e.getMessage());
        }
        // 2. MQ 落库（作为最终持久化事实源）
        chatMessageProducer.send(sessionKey, role, content);
    }

    /** 回源 ai-cs-message 按 sessionKey 拉历史，重建 Redis 缓存；失败降级为空列表 */
    private List<ChatHistoryMessage> fallbackToMessage(String sessionKey, String key) {
        try {
            Result<List<ChatHistoryMessage>> result = messageFeignClient.getMessagesBySessionKey(sessionKey);
            List<ChatHistoryMessage> history = result != null ? result.getData() : null;
            if (CollectionUtils.isEmpty(history)) {
                return new ArrayList<>();
            }
            // 重建缓存（条数上限已由 message 侧 LIMIT 保证）
            List<String> jsonList = new ArrayList<>(history.size());
            for (ChatHistoryMessage msg : history) {
                jsonList.add(objectMapper.writeValueAsString(msg));
            }
            redisTemplate.opsForList().rightPushAll(key, jsonList);
            return history;
        } catch (Exception e) {
            log.warn("回源 message 表拉取会话历史失败，降级为空历史: sessionKey={}, err={}", sessionKey, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<ChatHistoryMessage> deserialize(List<String> raw) {
        List<ChatHistoryMessage> history = new ArrayList<>(raw.size());
        for (String json : raw) {
            try {
                history.add(objectMapper.readValue(json, new TypeReference<ChatHistoryMessage>() {}));
            } catch (JsonProcessingException e) {
                log.warn("反序列化会话历史消息失败，跳过: err={}", e.getMessage());
            }
        }
        return history;
    }

    private String buildKey(String sessionKey) {
        return KEY_PREFIX + sessionKey;
    }
}