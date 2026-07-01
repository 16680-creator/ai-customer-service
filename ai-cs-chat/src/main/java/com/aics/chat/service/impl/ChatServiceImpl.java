package com.aics.chat.service.impl;

import com.aics.chat.service.ChatService;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 对话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final OpenAiChatModel chatModel;

    /** 会话历史存储（生产环境应使用 Redis 或持久化存储） */
    private final Map<String, List<Message>> sessionHistory = new ConcurrentHashMap<>();

    /** 最大历史消息数 */
    private static final int MAX_HISTORY_SIZE = 20;

    @Override
    public Result<String> chat(String sessionId, String message) {
        log.info("对话请求: sessionId={}, message={}", sessionId, message);

        try {
            // 维护会话历史
            List<Message> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
            history.add(new UserMessage(message));

            // 限制历史长度
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
                sessionHistory.put(sessionId, history);
            }

            // 调用 AI 模型
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            // 记录 AI 回复到历史
            history.add(new AssistantMessage(response));

            log.info("对话完成: sessionId={}, responseLength={}", sessionId, response.length());
            return Result.success(response);
        } catch (Exception e) {
            log.error("对话异常: sessionId={}", sessionId, e);
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> chatWithRag(String sessionId, String message, String knowledgeBase) {
        log.info("RAG对话请求: sessionId={}, knowledgeBase={}", sessionId, knowledgeBase);

        try {
            // 构建 RAG 提示词（此处为简化实现，生产环境应接入向量检索）
            String ragPrompt = String.format(
                    "请基于以下知识库【%s】的内容回答用户问题。如果知识库中没有相关信息，请如实告知。\n\n用户问题：%s",
                    knowledgeBase, message
            );

            String response = chatClient.prompt()
                    .user(ragPrompt)
                    .call()
                    .content();

            log.info("RAG对话完成: sessionId={}", sessionId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("RAG对话异常: sessionId={}", sessionId, e);
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> chatStream(String sessionId, String message) {
        log.info("流式对话请求: sessionId={}", sessionId);

        try {
            // 流式对话返回初始信息，实际流式输出通过 SSE 或 WebSocket 推送
            Map<String, Object> result = Map.of(
                    "sessionId", sessionId,
                    "status", "streaming",
                    "message", "流式对话已启动，请通过SSE端点接收响应"
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("流式对话异常: sessionId={}", sessionId, e);
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + e.getMessage());
        }
    }
}
