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
 * 多模态图片对话服务实现 —— 图片理解 + RAG 检索 + LLM 回答。
 *
 * <h3>【AI 技术详解】多模态（Multimodal）图片对话</h3>
 * <ul>
 *   <li><b>什么是多模态</b>：AI 能同时处理多种模态（文本、图片、音频、视频）</li>
 *   <li><b>本项目的多模态</b>：图片 + 文本 → 文本回答（图生文）</li>
 *   <li><b>为什么需要多模态</b>：用户可能上传截图（错误码、商品图、订单详情）询问问题</li>
 * </ul>
 *
 * <h3>【AI 技术详解】两段式编排架构</h3>
 * <pre>
 *   用户上传图片 + 问题
 *           │
 *           ▼
 *   ┌───────────────────────┐
 *   │ 第一段：视觉模型看图   │ ← 硅基流动 Qwen2.5-VL-72B
 *   │ 图片 → 文本描述       │   （多模态大模型，理解图片内容）
 *   └───────────────────────┘
 *           │
 *           ▼
 *   ┌───────────────────────┐
 *   │ 第二段：RAG 检索+回答  │ ← DeepSeek
 *   │ 描述文本 + 用户问题    │   （基于知识库检索生成回答）
 *   │ → 检索知识库           │
 *   │ → LLM 生成回答        │
 *   └───────────────────────┘
 *           │
 *           ▼
 *       返回回答 + 引用溯源
 * </pre>
 *
 * <h3>【AI 技术详解】为什么用两段式而非直接用多模态模型回答？</h3>
 * <ul>
 *   <li><b>复用 RAG 能力</b>：视觉模型只负责"看图"，回答仍基于知识库检索，保证回答有依据</li>
 *   <li><b>模型分工</b>：视觉模型擅长理解图片，文本模型擅长生成回答，各司其职</li>
 *   <li><b>降级友好</b>：视觉模型不可用时，有文字仍可回答（降级为纯文本对话）</li>
 *   <li><b>成本优化</b>：视觉模型调用成本高，只用一次"看图"，后续用便宜的文本模型</li>
 * </ul>
 *
 * <h3>降级策略（视觉是增强项，不可用不影响核心对话）</h3>
 * <ul>
 *   <li>图片 URL 校验失败 → {@link ResultCode#CHAT_IMAGE_URL_INVALID}（SSRF 防护）</li>
 *   <li>视觉理解失败 + 有文字 → 降级为纯文本对话（{@code degraded=true}）</li>
 *   <li>视觉理解失败 + 仅图片 → {@link ResultCode#CHAT_VISION_SERVICE_UNAVAILABLE} 明确提示</li>
 * </ul>
 *
 * <h3>【技术关联】安全防护</h3>
 * <ul>
 *   <li><b>SSRF 防护</b>：{@link ImageUrlValidator} 白名单校验图片 URL，防止探测内网</li>
 *   <li><b>PII 脱敏</b>：{@link PiiMasker} 对视觉描述中的手机号/身份证号脱敏，防止泄露</li>
 *   <li><b>图片格式校验</b>：只允许 jpg/png/webp/gif，防止上传恶意文件</li>
 *   <li><b>大小限制</b>：5MB 上限，防止资源耗尽</li>
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
     * 【AI 核心】视觉理解：图片 URL → 文本描述；失败返回 null，成功时脱敏。
     *
     * <p><b>【AI 技术详解】视觉模型调用流程</b>：
     * <ol>
     *   <li>构造多模态消息：文本指令 + 图片 URL</li>
     *   <li>调用视觉模型（硅基流动 Qwen2.5-VL-72B）</li>
     *   <li>模型返回图片描述文本</li>
     *   <li>PII 脱敏（手机号/身份证号）</li>
     * </ol>
     *
     * <p><b>【技术关联】与 VisionModelClient 的关系</b>：
     * <ul>
     *   <li>VisionModelClient：封装视觉模型调用，带弹性容错（超时/重试/熔断）</li>
     *   <li>本方法：调用 VisionModelClient 并处理结果（脱敏、降级）</li>
     * </ul>
     */
    private String describeImage(String imageUrl) {
        try {
            // describeAsync(...).get()：阻塞等待视觉模型返回描述（Future.get 解包）
            String description = visionModelClient.describeAsync(imageUrl).get();
            // 视觉描述可能含截图中的手机号/身份证号，注入 Prompt 前先脱敏
            return piiMasker.mask(description);
        } catch (Exception e) {
            // 视觉理解失败（超时/熔断/异常）→ 返回 null，由调用方降级
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
     * <p>把两段输入拼成一条检索查询，交给 RAG 链路：有文字用文字，有图片描述则用"图片内容：xxx"补充。</p>
     */
    private String buildQuery(String message, String description) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(message)) {
            sb.append(message);   // 用户附带文字优先
        }
        if (StringUtils.hasText(description)) {
            if (sb.length() > 0) {
                sb.append("；");   // 文字与图片描述之间用分号分隔
            }
            sb.append("图片内容：").append(description);   // 图片描述作为补充上下文
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
