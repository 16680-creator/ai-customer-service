package com.aics.knowledge.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.knowledge.entity.KnowledgeDocument;
import com.aics.knowledge.mapper.KnowledgeMapper;
import com.aics.knowledge.service.KnowledgeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;

    @Override
    public Result<Void> createDocument(KnowledgeDocument document) {
        log.info("创建知识文档: title={}", document.getTitle());
        document.setStatus(0);
        knowledgeMapper.insert(document);
        log.info("知识文档创建成功: id={}", document.getId());
        return Result.success();
    }

    @Override
    public Result<KnowledgeDocument> getDocumentById(Long id) {
        log.info("查询知识文档: id={}", id);
        KnowledgeDocument document = knowledgeMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        return Result.success(document);
    }

    @Override
    public Result<Page<KnowledgeDocument>> listDocuments(int page, int pageSize, String keyword) {
        log.info("分页查询知识文档: page={}, pageSize={}, keyword={}", page, pageSize, keyword);
        Page<KnowledgeDocument> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(KnowledgeDocument::getTitle, keyword)
                    .or()
                    .like(KnowledgeDocument::getTags, keyword);
        }
        wrapper.orderByDesc(KnowledgeDocument::getCreateTime);
        Page<KnowledgeDocument> result = knowledgeMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<Void> updateDocument(KnowledgeDocument document) {
        log.info("更新知识文档: id={}", document.getId());
        knowledgeMapper.updateById(document);
        log.info("知识文档更新成功: id={}", document.getId());
        return Result.success();
    }

    @Override
    public Result<Void> deleteDocument(Long id) {
        log.info("删除知识文档: id={}", id);
        knowledgeMapper.deleteById(id);
        log.info("知识文档删除成功: id={}", id);
        return Result.success();
    }
}
