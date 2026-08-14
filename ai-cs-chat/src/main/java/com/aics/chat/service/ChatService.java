package com.aics.chat.service;

import com.aics.chat.dto.ChatHistoryMessage;
import com.aics.chat.dto.ChatRagResponseDTO;
import com.aics.common.result.Result;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
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
     * @return AI 回复 + 引用来源列表
     */
    Result<ChatRagResponseDTO> chatWithRag(String sessionId, String message, String knowledgeBase);

    /**
     * RAG 对话（支持 Hybrid / 查询改写增强）
     *
     * @param hybrid  是否启用 ES+向量混合检索
     * @param rewrite 是否启用查询改写/HyDE
     */
    Result<ChatRagResponseDTO> chatWithRag(String sessionId, String message, String knowledgeBase,
                                           boolean hybrid, boolean rewrite);

    /**
     * 流式对话
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return 流式响应
     */
    Result<Map<String, Object>> chatStream(String sessionId, String message);

    /**
     * 真正的 SSE 流式对话（逐 token 推送，打字机效果）
     *
     * @param sessionId      会话ID
     * @param message        用户消息
     * @param knowledgeBase  知识库标识（可空，空则普通对话；非空则 RAG 对话）
     * @return SSE 发射器，逐 token 推送 {@code data: {"content":"..."}} 事件
     */
    SseEmitter chatStreamSse(String sessionId, String message, String knowledgeBase);

    /**
     * SSE 流式对话（支持 Hybrid / 查询改写增强）
     */
    SseEmitter chatStreamSse(String sessionId, String message, String knowledgeBase,
                             boolean hybrid, boolean rewrite);

    /**
     * 查询会话历史（历史回看）
     *
     * @param sessionKey 会话标识
     * @return 按时间升序的历史消息列表
     */
    Result<List<ChatHistoryMessage>> getHistory(String sessionKey);
}