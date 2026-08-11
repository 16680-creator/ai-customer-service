package com.aics.chat.service;

import com.aics.chat.dto.ChatHistoryMessage;

import java.util.List;

/**
 * 会话历史存储服务
 * 采用「Redis 热缓存 + message 表兜底持久化」两级存储：
 * - 读：Redis LRANGE 优先，未命中时经 Feign 回源 ai-cs-message 并重建缓存
 * - 写：RPUSH 到 Redis + 投递 RocketMQ 最终落库到 chat_message 表
 */
public interface ChatHistoryService {

    /**
     * 加载会话历史
     *
     * @param sessionKey 会话标识
     * @return 按时间升序的历史消息列表；Redis 不可用或回源失败时降级为空列表
     */
    List<ChatHistoryMessage> load(String sessionKey);

    /**
     * 追加一条会话消息（Redis 热缓存 + MQ 落库双写）
     *
     * @param sessionKey 会话标识
     * @param role       角色：user/assistant
     * @param content    消息内容
     */
    void append(String sessionKey, String role, String content);
}