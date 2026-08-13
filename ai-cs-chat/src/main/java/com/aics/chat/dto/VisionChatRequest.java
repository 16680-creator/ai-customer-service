package com.aics.chat.dto;

import lombok.Data;

/**
 * 图片对话请求 DTO（多模态图生文）。
 *
 * <p>图片通过 MinIO 上传后返回 URL，随请求传入；可选附带文字描述。
 * hybrid/rewrite 控制 RAG 检索模式，与文本 RAG 对话保持一致。</p>
 */
@Data
public class VisionChatRequest {

    /** 会话 ID（复用现有会话体系） */
    private String sessionId;

    /** 图片 URL（须通过 SSRF 白名单校验） */
    private String imageUrl;

    /** 附带文字描述（可空，仅图片时为空） */
    private String message;

    /** 知识库标识（可空，空则纯文本回答；非空则走 RAG 检索） */
    private String knowledgeBase;

    /** 是否启用 ES+向量混合检索 */
    private boolean hybrid = false;

    /** 是否启用查询改写/HyDE */
    private boolean rewrite = false;
}
