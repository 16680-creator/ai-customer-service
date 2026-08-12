package com.aics.knowledge.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.knowledge.entity.KnowledgeDocument;
import com.aics.knowledge.mapper.KnowledgeMapper;
import com.aics.knowledge.mq.KnowledgeSyncProducer;
import com.aics.knowledge.service.KnowledgeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 知识库服务实现
 *
 * <p>DB 操作完成后通过 RocketMQ 异步同步到搜索服务（Chroma 向量库），
 * 解耦 DB 事务与向量化操作。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeSyncProducer knowledgeSyncProducer;

    @Override
    public Result<Void> createDocument(KnowledgeDocument document) {
        log.info("创建知识文档: title={}", document.getTitle());
        document.setStatus(0);
        knowledgeMapper.insert(document);
        log.info("知识文档创建成功: id={}", document.getId());
        // 发送 RocketMQ 消息异步向量化入库（Chroma），解耦 DB 事务与向量操作
        knowledgeSyncProducer.send("CREATE", document);
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
        // 发送 RocketMQ 消息异步重新向量化，保证 RAG 检索到最新内容
        knowledgeSyncProducer.send("UPDATE", document);
        return Result.success();
    }

    @Override
    public Result<Void> deleteDocument(Long id) {
        log.info("删除知识文档: id={}", id);
        // 先查询文档，用于发送 DELETE 同步消息（含 documentId）
        KnowledgeDocument document = knowledgeMapper.selectById(id);
        if (document != null) {
            knowledgeSyncProducer.send("DELETE", document);
        } else {
            log.warn("知识文档不存在，直接执行 DB 删除: id={}", id);
        }
        knowledgeMapper.deleteById(id);
        log.info("知识文档删除成功: id={}", id);
        return Result.success();
    }
}
