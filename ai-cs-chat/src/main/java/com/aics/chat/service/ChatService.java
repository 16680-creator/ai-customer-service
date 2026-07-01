package com.aics.chat.service;

import com.aics.common.result.Result;

import java.util.Map;

/**
 * AI 对话服务接口
 */
public interface ChatService {

    /**
     * 发送对话消息
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return AI 回复
     */
    Result<String> chat(String sessionId, String message);

    /**
     * 基于 RAG 的对话
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @param knowledgeBase 知识库标识
     * @return AI 回复
     */
    Result<String> chatWithRag(String sessionId, String message, String knowledgeBase);

    /**
     * 流式对话
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return 流式响应
     */
    Result<Map<String, Object>> chatStream(String sessionId, String message);
}
