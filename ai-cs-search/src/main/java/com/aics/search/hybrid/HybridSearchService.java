package com.aics.search.hybrid;

import java.util.List;

/**
 * 混合检索服务（ES 关键词 + Chroma 向量，RRF 融合）
 */
public interface HybridSearchService {

    /**
     * 混合检索：ES 关键词检索（Top-20）+ Chroma 向量检索（Top-20），RRF 融合后返回 topK 条。
     *
     * <p>降级策略：ES 异常时仅返回向量结果；向量异常时仅返回 ES 结果；两路均失败返回空列表。</p>
     *
     * @param knowledgeBase 知识库标识（ES 索引名 / Chroma knowledgeBase 过滤条件）
     * @param query         查询语句
     * @param topK          返回条数
     * @return 融合后的检索结果（按融合分数降序）
     */
    List<HybridSearchResult> hybridSearch(String knowledgeBase, String query, int topK);
}
