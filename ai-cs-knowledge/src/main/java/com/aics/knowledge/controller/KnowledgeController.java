package com.aics.knowledge.controller;

import com.aics.common.result.Result;
import com.aics.knowledge.entity.KnowledgeDocument;
import com.aics.knowledge.service.KnowledgeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库管理控制器
 *
 * <p>职责：对外暴露知识文档 CRUD REST 接口，统一路径前缀 /knowledge。</p>
 *
 * <p>协作关系：</p>
 * <ul>
 *   <li>请求委派给 {@link com.aics.knowledge.service.KnowledgeService} 处理</li>
 *   <li>Service 完成数据库操作后会通过 RocketMQ 异步触发向量化（见
 *       {@link com.aics.knowledge.mq.KnowledgeSyncProducer}）</li>
 *   <li>向量库（Chroma）写入由 {@link com.aics.knowledge.service.KnowledgeVectorService} 完成</li>
 * </ul>
 *
 * <p>技术要点：使用 SpringDoc OpenAPI 注解生成接口文档，返回值统一包装为
 * {@link com.aics.common.result.Result}。</p>
 */
@Tag(name = "知识库管理")
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@Validated
public class KnowledgeController {

    /** 知识库业务服务 */
    private final KnowledgeService knowledgeService;

    /**
     * 创建知识文档
     * <p>入库后异步触发向量化（Chroma 入库），用于 RAG 检索。</p>
     *
     * @param document 文档信息（标题、内容、标签等）
     * @return 创建结果
     */
    @Operation(summary = "创建知识文档")
    @PostMapping
    public Result<Void> createDocument(@RequestBody KnowledgeDocument document) {
        return knowledgeService.createDocument(document);
    }

    /**
     * 根据ID查询知识文档
     *
     * @param id 文档ID
     * @return 文档详情
     */
    @Operation(summary = "查询知识文档")
    @GetMapping("/{id}")
    public Result<KnowledgeDocument> getDocumentById(@PathVariable("id") Long id) {
        return knowledgeService.getDocumentById(id);
    }

    /**
     * 分页查询知识文档
     * <p>支持按标题或标签关键词模糊检索。</p>
     *
     * @param page     页码（默认 1）
     * @param pageSize 每页大小（默认 10）
     * @param keyword  搜索关键词（可选，匹配 title 或 tags）
     * @return 分页结果
     */
    @Operation(summary = "分页查询知识文档")
    @GetMapping("/list")
    public Result<Page<KnowledgeDocument>> listDocuments(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return knowledgeService.listDocuments(page, pageSize, keyword);
    }

    /**
     * 更新知识文档
     * <p>更新后异步重新向量化，保证 RAG 检索到最新内容。</p>
     *
     * @param document 文档信息（含待更新字段及主键）
     * @return 更新结果
     */
    @Operation(summary = "更新知识文档")
    @PutMapping
    public Result<Void> updateDocument(@RequestBody KnowledgeDocument document) {
        return knowledgeService.updateDocument(document);
    }

    /**
     * 删除知识文档
     * <p>删除前先投递 DELETE 同步消息，消费者据此从 Chroma 移除对应向量。</p>
     *
     * @param id 文档ID
     * @return 删除结果
     */
    @Operation(summary = "删除知识文档")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable("id") Long id) {
        return knowledgeService.deleteDocument(id);
    }
}
