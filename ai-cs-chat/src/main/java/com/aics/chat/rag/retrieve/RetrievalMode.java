package com.aics.chat.rag.retrieve;

/**
 * 检索模式。
 */
public enum RetrievalMode {
    /** 纯向量语义检索（默认，存量行为） */
    VECTOR,
    /** ES 关键词 + 向量语义 RRF 混合检索 */
    HYBRID,
    /** 混合检索 + 查询改写/HyDE */
    HYBRID_QUERY_REWRITE,
    /** 图谱优先（未命中降级普通检索） */
    GRAPH_RAG
}