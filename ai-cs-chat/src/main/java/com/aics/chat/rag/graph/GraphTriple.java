package com.aics.chat.rag.graph;

import lombok.Data;

/**
 * 知识图谱三元组。
 */
@Data
public class GraphTriple {

    /** 主键（InMemory 自增 / DB 自增） */
    private Long id;

    /** 主体实体 */
    private String subject;

    /** 关系 */
    private String predicate;

    /** 客体实体 */
    private String object;

    /** 知识库标识 */
    private String knowledgeBase;

    /** 来源文档 ID（可空） */
    private Long sourceDocumentId;
}
