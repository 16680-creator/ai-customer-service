package com.aics.search.service;

import com.aics.common.result.Result;

import java.util.List;
import java.util.Map;

/**
 * 搜索服务接口
 */
public interface SearchService {

    /**
     * 全文搜索
     *
     * @param index 索引名称
     * @param query 搜索关键词
     * @param page  页码
     * @param size  每页大小
     * @return 搜索结果列表
     */
    Result<List<Map<String, Object>>> search(String index, String query, int page, int size);

    /**
     * 创建索引
     *
     * @param index       索引名称
     * @param mappings    索引映射
     * @return 创建结果
     */
    Result<Void> createIndex(String index, Map<String, Object> mappings);

    /**
     * 索引文档
     *
     * @param index     索引名称
     * @param document  文档内容
     * @return 索引结果
     */
    Result<Void> indexDocument(String index, Map<String, Object> document);

    /**
     * 删除索引
     *
     * @param index 索引名称
     * @return 删除结果
     */
    Result<Void> deleteIndex(String index);
}
