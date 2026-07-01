package com.aics.knowledge.service;

import com.aics.common.result.Result;
import com.aics.knowledge.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 知识库服务接口
 */
public interface KnowledgeService {

    /**
     * 创建知识文档
     *
     * @param document 文档信息
     * @return 创建结果
     */
    Result<Void> createDocument(KnowledgeDocument document);

    /**
     * 根据ID查询文档
     *
     * @param id 文档ID
     * @return 文档信息
     */
    Result<KnowledgeDocument> getDocumentById(Long id);

    /**
     * 分页查询文档
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @return 分页结果
     */
    Result<Page<KnowledgeDocument>> listDocuments(int page, int pageSize, String keyword);

    /**
     * 更新文档
     *
     * @param document 文档信息
     * @return 更新结果
     */
    Result<Void> updateDocument(KnowledgeDocument document);

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 删除结果
     */
    Result<Void> deleteDocument(Long id);
}
