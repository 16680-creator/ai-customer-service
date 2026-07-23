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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

    /** 最大历史消息数，超过时触发压缩 */
    private static final int MAX_HISTORY_SIZE = 20;

    /** 压缩后保留的最近消息数 */
    private static final int KEEP_RECENT_SIZE = 10;

    /** 过滤模型思考过程标签 */
    private static final Pattern THINK_PATTERN = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);

    /**
     * 清除 AI 回复中的思考过程标签
     */
    private String cleanResponse(String response) {
        if (response == null) return "";
        return THINK_PATTERN.matcher(response).replaceAll("").trim();
    }

    /**
     * 压缩会话历史：将旧消息交给 AI 生成摘要，替换为一条 SystemMessage
     * 保留最近的 KEEP_RECENT_SIZE 条消息
     */
    private List<Message> compressHistory(List<Message> history) {
        int splitIndex = history.size() - KEEP_RECENT_SIZE;
        List<Message> oldMessages = history.subList(0, splitIndex);
        List<Message> recentMessages = new ArrayList<>(history.subList(splitIndex, history.size()));

        // 拼接旧消息为文本
        StringBuilder conversation = new StringBuilder();
        for (Message msg : oldMessages) {
            String role = msg instanceof UserMessage ? "用户" : "助手";
            conversation.append(role).append("：").append(msg.getText()).append("\n");
        }

        try {
            // 调用 AI 生成摘要
            String summary = chatModel.call(
                    new Prompt("请将以下对话历史压缩为简洁的摘要，保留关键信息（用户名、订单号、重要决定等），"
                            + "用1-3句话概括，作为后续对话的上下文参考：\n\n" + conversation)
            ).getResult().getOutput().getText();

            summary = cleanResponse(summary);
            log.info("会话历史压缩完成: {}条消息 -> 摘要({}字)", oldMessages.size(), summary.length());

            // 构建压缩后的历史：摘要 + 最近消息
            List<Message> compressed = new ArrayList<>();
            compressed.add(new SystemMessage("以下是之前对话的摘要，请参考：\n" + summary));
            compressed.addAll(recentMessages);
            return compressed;
        } catch (Exception e) {
            log.warn("会话压缩失败，回退为截断模式", e);
            return recentMessages;
        }
    }

    @Override
    public Result<String> chat(String sessionId, String message) {
        log.info("对话请求: sessionId={}, message={}", sessionId, message);

        try {
            // 维护会话历史
            List<Message> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
            history.add(new UserMessage(message));

            // 历史超过上限时，压缩旧消息为摘要
            if (history.size() > MAX_HISTORY_SIZE) {
                history = compressHistory(history);
                sessionHistory.put(sessionId, history);
            }

            // 调用 AI 模型，携带完整会话历史（工具已通过 defaultToolCallbacks 全局注册）
            String response = chatClient.prompt()
                    .messages(history)
                    .call()
                    .content();

            // 过滤思考过程
            response = cleanResponse(response);

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

            // 过滤思考过程
            response = cleanResponse(response);

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
