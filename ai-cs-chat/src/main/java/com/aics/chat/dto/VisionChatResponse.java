package com.aics.chat.dto;

import lombok.Data;

import java.util.List;

/**
 * 图片对话响应 DTO（多模态图生文）。
 */
@Data
public class VisionChatResponse {

    /** 最终回答 */
    private String answer;

    /** 引用溯源列表（复用 {@link CitationItemDTO}） */
    private List<CitationItemDTO> citations;

    /** 视觉模型生成的图片描述（供前端展示"我看到了什么"） */
    private String imageDescription;

    /** 是否发生降级（视觉不可用 → 纯文本） */
    private boolean degraded;
}
