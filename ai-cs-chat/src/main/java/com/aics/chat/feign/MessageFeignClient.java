package com.aics.chat.feign;

import com.aics.chat.dto.ChatHistoryMessage;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 消息服务 Feign 客户端（调用 ai-cs-message，经注册中心负载均衡）
 * 用于按 sessionKey 兜底拉取已持久化的会话历史
 */
@FeignClient(name = "ai-cs-message")
public interface MessageFeignClient {

    /**
     * 按 sessionKey 查询会话消息列表（message 返回 ChatMessage，
     * 用 chat 侧 DTO 反序列化，忽略多余字段）
     */
    @GetMapping("/api/message/session-key/{sessionKey}/messages")
    Result<List<ChatHistoryMessage>> getMessagesBySessionKey(@PathVariable("sessionKey") String sessionKey);
}