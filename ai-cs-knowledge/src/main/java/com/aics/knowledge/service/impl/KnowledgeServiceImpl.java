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
 *
 * <p>协作关系：</p>
 * <ul>
 *   <li>{@link KnowledgeMapper}：执行 kb_document 表的 CRUD</li>
 *   <li>{@link KnowledgeSyncProducer}：DB 操作成功后投递同步消息，触发向量化</li>
 *   <li>向量化实际由 {@link com.aics.knowledge.service.KnowledgeVectorService} 完成（经消费者调用）</li>
 * </ul>
 *
 * <p>事务说明：本类未显式声明 @Transactional，单次写操作依赖 MyBatis-Plus 默认行为；
 * MQ 投递放在 DB 写入之后，确保只在数据落库后才触发同步。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    /** 知识文档 Mapper */
    private final KnowledgeMapper knowledgeMapper;
    /** 同步消息生产者（投递到 RocketMQ） */
    private final KnowledgeSyncProducer knowledgeSyncProducer;

    /**
     * 创建知识文档
     *
     * <p>流程：设置初始状态 0-待处理 → 入库 → 投递 CREATE 同步消息（异步向量化）。</p>
     *
     * @param document 文档信息
     * @return 创建结果
     */
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

    /**
     * 根据ID查询文档
     *
     * @param id 文档ID
     * @return 文档信息
     * @throws com.aics.common.exception.BusinessException 文档不存在时抛出 KNOWLEDGE_NOT_FOUND
     */
    @Override
    public Result<KnowledgeDocument> getDocumentById(Long id) {
        log.info("查询知识文档: id={}", id);
        KnowledgeDocument document = knowledgeMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        return Result.success(document);
    }

    /**
     * 分页查询文档
     *
     * <p>关键词非空时按 title 或 tags 模糊匹配，结果按创建时间倒序排列。</p>
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词（可选）
     * @return 分页结果
     */
    @Override
    public Result<Page<KnowledgeDocument>> listDocuments(int page, int pageSize, String keyword) {
        log.info("分页查询知识文档: page={}, pageSize={}, keyword={}", page, pageSize, keyword);
        Page<KnowledgeDocument> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            // title OR tags 模糊匹配
            wrapper.like(KnowledgeDocument::getTitle, keyword)
                    .or()
                    .like(KnowledgeDocument::getTags, keyword);
        }
        // 按创建时间倒序，最新文档优先展示
        wrapper.orderByDesc(KnowledgeDocument::getCreateTime);
        Page<KnowledgeDocument> result = knowledgeMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 更新文档
     *
     * <p>流程：按主键更新 → 投递 UPDATE 同步消息（消费者会先删旧分块再写新分块，
     * 保证 RAG 检索到最新内容）。</p>
     *
     * @param document 文档信息（含待更新字段及主键）
     * @return 更新结果
     */
    @Override
    public Result<Void> updateDocument(KnowledgeDocument document) {
        log.info("更新知识文档: id={}", document.getId());
        knowledgeMapper.updateById(document);
        log.info("知识文档更新成功: id={}", document.getId());
        // 发送 RocketMQ 消息异步重新向量化，保证 RAG 检索到最新内容
        knowledgeSyncProducer.send("UPDATE", document);
        return Result.success();
    }

    /**
     * 删除文档
     *
     * <p>流程：先查询文档（用于投递 DELETE 消息携带 documentId）→ 投递 DELETE 同步消息
     * → 逻辑删除 DB 记录。先发消息再删 DB，确保消费者能拿到 documentId 完成向量清理。</p>
     *
     * @param id 文档ID
     * @return 删除结果
     */
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
