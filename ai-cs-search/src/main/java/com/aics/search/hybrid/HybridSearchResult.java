package com.aics.search.hybrid;

import lombok.Data;

/**
 * 混合检索结果 VO（ES 关键词 + Chroma 向量，RRF 融合）
 */
@Data
public class HybridSearchResult {

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

    /** 文档类型：pdf/docx/txt/markdown/html */
    private String docType;

    /** ES（关键词路）排名，0 表示未命中 */
    private int esRank;

    /** 向量检索（Chroma）排名，0 表示未命中 */
    private int vectorRank;
}
