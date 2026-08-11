package com.aics.chat.service.impl;

import com.aics.chat.dto.ChatHistoryMessage;
import com.aics.chat.service.ChatHistoryService;
import com.aics.chat.service.ChatService;
import com.aics.chat.service.KnowledgeBaseService;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatHistoryService chatHistoryService;

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
     * 将持久化历史 DTO 转换为 Spring AI Message 列表
     */
    private List<Message> toSpringMessages(List<ChatHistoryMessage> history) {
        List<Message> messages = new ArrayList<>(history.size());
        for (ChatHistoryMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return messages;
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
            // 从持久化历史加载（Redis 优先，未命中回源 message 表），替代内存 Map
            List<Message> history = toSpringMessages(chatHistoryService.load(sessionId));
            history.add(new UserMessage(message));
            chatHistoryService.append(sessionId, "user", message);

            // 历史超过上限时，压缩旧消息为摘要
            if (history.size() > MAX_HISTORY_SIZE) {
                history = compressHistory(history);
            }

            // 调用 AI 模型，携带完整会话历史（工具已通过 defaultToolCallbacks 全局注册）
            String response = chatClient.prompt()
                    .messages(history)
                    .call()
                    .content();

            // 过滤思考过程
            response = cleanResponse(response);

            // 记录 AI 回复到历史
            chatHistoryService.append(sessionId, "assistant", response);
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
            // ===== 真正的 RAG 检索增强生成 =====
            // 1. 语义检索：在指定知识库中，用向量相似度找出与用户问题最相关的 Top-5 文档片段
            //    （底层：问题向量化 → VectorStore 余弦相似度检索 → knowledgeBase 过滤）
            List<Document> docs = knowledgeBaseService.search(knowledgeBase, message, 5, 0.5);

            // 2. 把命中的文档片段拼装成上下文文本
            String context = knowledgeBaseService.buildContext(docs);

            // 3. 将上下文注入 Prompt：让大模型"基于检索到的私有知识"作答，而不是凭空编造
            String ragPrompt = """
                    请严格基于下面的【知识库资料】回答用户问题。
                    重要规则：
                    1. 如果资料中没有相关信息，请如实告知："我暂时没有这方面的资料"，不要编造内容
                    2. 回答时优先引用资料中的内容，不要提及"根据资料/检索结果"之类的表述

                    【知识库资料】
                    %s

                    【用户问题】
                    %s
                    """.formatted(context.isBlank() ? "（未检索到相关资料）" : context, message);

            String response = chatClient.prompt()
                    .user(ragPrompt)
                    .call()
                    .content();

            // 过滤思考过程
            response = cleanResponse(response);

            log.info("RAG对话完成: sessionId={}, 检索命中{}条", sessionId, docs.size());
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
            // 流式对话返回初始信息，实际流式输出通过 SSE 端点 /chat/stream/sse 推送
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("status", "streaming");
            result.put("message", "流式对话已启动，请使用 /chat/stream/sse 端点接收 SSE 流式响应");
            return Result.success(result);
        } catch (Exception e) {
            log.error("流式对话异常: sessionId={}", sessionId, e);
            throw new BusinessException(ResultCode.CHAT_AI_SERVICE_UNAVAILABLE, "AI服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public SseEmitter chatStreamSse(String sessionId, String message, String knowledgeBase) {
        log.info("SSE流式对话请求: sessionId={}, knowledgeBase={}", sessionId, knowledgeBase);
        SseEmitter emitter = new SseEmitter(0L);
        boolean hasKb = StringUtils.hasText(knowledgeBase);

        try {
            // 1. 维护会话历史（与 chat() 一致）：从持久化历史加载 + 记录用户消息（Redis + MQ 双写）
            List<Message> history = toSpringMessages(chatHistoryService.load(sessionId));
            history.add(new UserMessage(message));
            chatHistoryService.append(sessionId, "user", message);
            if (history.size() > MAX_HISTORY_SIZE) {
                history = compressHistory(history);
            }

            // lambda 中需要引用历史列表（effectively final）
            final List<Message> streamHistory = history;

            // 2. 构造流式 Flux：普通对话带历史；RAG 对话注入知识库上下文
            Flux<String> flux;
            if (hasKb) {
                List<Document> docs = knowledgeBaseService.search(knowledgeBase, message, 5, 0.5);
                String context = knowledgeBaseService.buildContext(docs);
                String ragPrompt = """
                        请严格基于下面的【知识库资料】回答用户问题。
                        重要规则：
                        1. 如果资料中没有相关信息，请如实告知："我暂时没有这方面的资料"，不要编造内容
                        2. 回答时优先引用资料中的内容，不要提及"根据资料/检索结果"之类的表述

                        【知识库资料】
                        %s

                        【用户问题】
                        %s
                        """.formatted(context.isBlank() ? "（未检索到相关资料）" : context, message);
                flux = chatClient.prompt().user(ragPrompt).stream().content();
            } else {
                flux = chatClient.prompt().messages(history).stream().content();
            }

            // 3. 订阅流：逐 token 通过 SSE 推送，结束后更新历史 + 投递 MQ
            StringBuilder full = new StringBuilder();
            flux.subscribe(
                    chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            full.append(chunk);
                            try {
                                emitter.send(SseEmitter.event().data(Map.of("content", chunk)));
                            } catch (Exception e) {
                                log.warn("SSE发送失败: sessionId={}, err={}", sessionId, e.getMessage());
                            }
                        }
                    },
                    error -> {
                        log.error("SSE流式对话异常: sessionId={}", sessionId, error);
                        try {
                            emitter.send(SseEmitter.event().data(Map.of("error", String.valueOf(error.getMessage()))));
                        } catch (Exception ignore) {
                            // ignore
                        }
                        emitter.complete();
                    },
                    () -> {
                        String response = cleanResponse(full.toString());
                        try {
                            chatHistoryService.append(sessionId, "assistant", response);
                            streamHistory.add(new AssistantMessage(response));
                            emitter.send(SseEmitter.event().data(Map.of("done", true, "content", response)));
                        } catch (Exception e) {
                            log.warn("SSE完成事件发送失败: sessionId={}, err={}", sessionId, e.getMessage());
                        }
                        emitter.complete();
                    }
            );
        } catch (Exception e) {
            log.error("SSE流式对话初始化异常: sessionId={}", sessionId, e);
            try {
                emitter.send(SseEmitter.event().data(Map.of("error", String.valueOf(e.getMessage()))));
            } catch (Exception ignore) {
                // ignore
            }
            emitter.complete();
        }
        return emitter;
    }

    @Override
    public Result<List<ChatHistoryMessage>> getHistory(String sessionKey) {
        log.info("查询会话历史: sessionKey={}", sessionKey);
        return Result.success(chatHistoryService.load(sessionKey));
    }
}