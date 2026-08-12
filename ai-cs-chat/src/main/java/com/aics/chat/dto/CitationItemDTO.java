package com.aics.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * RAG 回答的引用来源条目（Citation）
 *
 * <p>对应检索命中的一段文档，用于前端展示"回答依据"，
 * 包含来源文档、页码、相关度分数与原文片段。</p>
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class CitationItemDTO {

    /** 来源文档 ID */
    private Long documentId;

    /** 文档标题 */
    private String title;

    /** 页码（PDF 分页读取时存在，metadata key 为 page_number） */
    private Integer page;

    /** 相关度分数 */
    private Double score;

    /** 引用原文片段 */
    private String content;
}
