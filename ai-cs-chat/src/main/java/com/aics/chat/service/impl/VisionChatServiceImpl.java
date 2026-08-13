package com.aics.chat.service.impl;

import com.aics.chat.dto.ChatRagResponseDTO;
import com.aics.chat.dto.VisionChatRequest;
import com.aics.chat.dto.VisionChatResponse;
import com.aics.chat.service.ChatService;
import com.aics.chat.service.VisionChatService;
import com.aics.chat.util.ImageUrlValidator;
import com.aics.chat.util.PiiMasker;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 多模态图片对话服务实现。
 *
 * <h3>两段式编排</h3>
 * <ol>
 *   <li><b>看图</b>：{@link VisionModelClient} 调视觉模型（硅基流动 Qwen2.5-VL）把图片转成文本描述；</li>
 *   <li><b>回答</b>：把描述文本（+ 用户文字）组合成查询，复用 {@link ChatService} 的 RAG 链路
 *       检索知识库并由 DeepSeek 生成回答。</li>
 * </ol>
 *
 * <h3>降级策略（视觉是增强项，不可用不影响核心对话）</h3>
 * <ul>
 *   <li>图片 URL 校验失败 → {@link ResultCode#CHAT_IMAGE_URL_INVALID}；</li>
 *   <li>视觉理解失败 + 有文字 → 降级为纯文本对话（{@code degraded=true}）；</li>
 *   <li>视觉理解失败 + 仅图片 → {@link ResultCode#CHAT_VISION_SERVICE_UNAVAILABLE} 明确提示。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisionChatServiceImpl implements VisionChatService {

    private final VisionModelClient visionModelClient;
    private final ImageUrlValidator imageUrlValidator;
    private final ChatService chatService;
    private final PiiMasker piiMasker;

    @Override
    public Result<VisionChatResponse> chatWithVision(VisionChatRequest request) {
        // 1. 校验图片 URL（SSRF 白名单）
        if (!imageUrlValidator.isValid(request.getImageUrl())) {
            throw new BusinessException(ResultCode.CHAT_IMAGE_URL_INVALID, "图片地址无效或不允许访问");
        }

        // 2. 视觉理解：图片 → 文本描述
        String description = describeImage(request.getImageUrl());
        if (description == null) {
            return degradeToText(request);
        }

        // 3. 组合查询 + RAG 检索 + 回答（复用现有 RAG 对话链路）
        String query = buildQuery(request.getMessage(), description);
        Result<ChatRagResponseDTO> rag = chatService.chatWithRag(
                request.getSessionId(), query, request.getKnowledgeBase(),
                request.isHybrid(), request.isRewrite());
        ChatRagResponseDTO data = rag.getData();

        VisionChatResponse response = new VisionChatResponse();
        response.setAnswer(data.getContent());
        response.setCitations(data.getCitations());
        response.setImageDescription(description);
        response.setDegraded(false);
        log.info("图片对话完成: sessionId={}, 引用{}条", request.getSessionId(),
                data.getCitations() == null ? 0 : data.getCitations().size());
        return Result.success(response);
    }

    @Override
    public SseEmitter chatWithVisionSse(VisionChatRequest request) {
        // 校验失败：推送错误事件后立即结束
        if (!imageUrlValidator.isValid(request.getImageUrl())) {
            return errorEmitter("图片地址无效或不允许访问");
        }

        // 视觉理解（SSE 前同步完成，得到描述后再流式回答）
        String description = describeImage(request.getImageUrl());
        if (description == null) {
            if (!StringUtils.hasText(request.getMessage())) {
                return errorEmitter("当前无法识别图片，请文字描述");
            }
            // 有文字 → 降级为纯文本流式对话
            return chatService.chatStreamSse(request.getSessionId(), request.getMessage(), null, false, false);
        }

        String query = buildQuery(request.getMessage(), description);
        return chatService.chatStreamSse(request.getSessionId(), query, request.getKnowledgeBase(),
                request.isHybrid(), request.isRewrite());
    }

    /**
     * 视觉理解：图片 URL → 文本描述；失败返回 null，成功时脱敏。
     */
    private String describeImage(String imageUrl) {
        try {
            String description = visionModelClient.describeAsync(imageUrl).get();
            return piiMasker.mask(description);
        } catch (Exception e) {
            log.warn("视觉理解失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 视觉不可用降级：有文字走纯文本，仅图片抛明确错误。
     */
    private Result<VisionChatResponse> degradeToText(VisionChatRequest request) {
        if (StringUtils.hasText(request.getMessage())) {
            Result<String> text = chatService.chat(request.getSessionId(), request.getMessage());
            VisionChatResponse response = new VisionChatResponse();
            response.setAnswer(text.getData());
            response.setDegraded(true);
            log.info("图片对话降级为纯文本: sessionId={}", request.getSessionId());
            return Result.success(response);
        }
        throw new BusinessException(ResultCode.CHAT_VISION_SERVICE_UNAVAILABLE, "当前无法识别图片，请文字描述");
    }

    /**
     * 组合查询：用户文字 + 图片描述。
     */
    private String buildQuery(String message, String description) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(message)) {
            sb.append(message);
        }
        if (StringUtils.hasText(description)) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append("图片内容：").append(description);
        }
        return sb.toString();
    }

    /**
     * 构造一个立即推送错误事件并结束的 SSE 发射器。
     */
    private SseEmitter errorEmitter(String errorMsg) {
        SseEmitter emitter = new SseEmitter(60_000L);
        try {
            emitter.send(SseEmitter.event().data(Map.of("error", errorMsg)));
            emitter.complete();
        } catch (Exception ignore) {
            emitter.completeWithError(ignore);
        }
        return emitter;
    }
}
