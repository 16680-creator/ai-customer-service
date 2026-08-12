package com.aics.search.hybrid;

import lombok.Data;

/**
 * RRF 融合中的排名条目
 *
 * <p>携带关键词路（ES）与向量路（Chroma）各自的命中信息与融合分数：
 * <ul>
 *   <li>rank1：ES 检索中的排名（从 1 开始，0 表示未命中）</li>
 *   <li>rank2：向量检索中的排名（从 1 开始，0 表示未命中）</li>
 *   <li>score：RRF 融合后的分数（由 RrfMerger 计算）</li>
 * </ul>
 */
@Data
public class RankedItem {

    /** 文档唯一标识（ES _id / Chroma documentId） */
    private String id;

    /** RRF 融合后的分数 */
    private double score;

    /** ES（关键词路）排名，0 表示未命中 */
    private int rank1;

    /** 向量检索（Chroma）排名，0 表示未命中 */
    private int rank2;

    /** 文档标题 */
    private String title;

    /** 文档内容 */
    private String content;

    /** 知识库标识 */
    private String knowledgeBase;

    /** 页码 */
    private Integer page;

    /** 文档类型：pdf/docx/txt/markdown/html */
    private String docType;
}
