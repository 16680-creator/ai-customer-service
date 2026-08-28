package com.aics.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * RAG 对话响应 DTO
 *
 * <p>包含大模型生成的回答正文 {@link #content} 以及回答引用的来源列表 {@link #citations}，
 * 供前端展示"回答 + 引用溯源"；命中缓存层时附带 {@link #cacheHit}/{@link #cacheSource}
 * 标记，前端可展示"缓存命中"角标。</p>
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class ChatRagResponseDTO {

    /** AI 生成的回答内容 */
    private String content;

    /** 回答引用的知识库来源列表（按相关度排序） */
    private List<CitationItemDTO> citations;

    /** 是否命中缓存（true=热门问答/语义缓存直接返回，未调用 LLM） */
    private Boolean cacheHit;

    /** 缓存来源：hot-qa（热门问答精确命中）/ semantic（语义缓存相似命中） */
    private String cacheSource;
}
