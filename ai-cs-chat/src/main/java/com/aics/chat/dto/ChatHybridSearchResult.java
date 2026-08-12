package com.aics.chat.dto;

import lombok.Data;

/**
 * 混合检索结果（chat 侧 DTO，镜像 ai-cs-search 的 HybridSearchResult）。
 */
@Data
public class ChatHybridSearchResult {

    /** 文档唯一标识（ES _id / Chroma documentId） */
    private String documentId;

    /** 文档标题 */
    private String title;

    /** 文档内容 */
    private String content;

    /** RRF 融合分数 */
    private double score;

    /** 知识库标识 */
    private String knowledgeBase;

    /** 页码 */
    private Integer page;

    /** 文档类型 */
    private String docType;
}