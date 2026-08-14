package com.aics.chat.service;

import com.aics.chat.dto.VisionChatRequest;
import com.aics.chat.dto.VisionChatResponse;
import com.aics.common.result.Result;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 多模态图片对话服务接口（图生文）。
 *
 * <p>两段式架构：视觉模型"看懂图"生成文本描述 → 描述文本复用现有 RAG 链路
 * 检索 + DeepSeek 生成回答，最大化复用 {@link ChatService} 能力。</p>
 */
public interface VisionChatService {

    /**
     * 图片对话（同步）。
     *
     * @param request 图片对话请求（sessionId/imageUrl/message/knowledgeBase/hybrid/rewrite）
     * @return 回答 + 引用溯源 + 图片描述 + 降级标记
     */
    Result<VisionChatResponse> chatWithVision(VisionChatRequest request);

    /**
     * 图片对话（SSE 流式，逐 token 推送）。
     *
     * @param request 图片对话请求
     * @return SSE 发射器
     */
    SseEmitter chatWithVisionSse(VisionChatRequest request);
}
