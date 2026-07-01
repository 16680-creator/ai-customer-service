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
 * 知识库控制器
 */
@Tag(name = "知识库管理")
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@Validated
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "创建知识文档")
    @PostMapping
    public Result<Void> createDocument(@RequestBody KnowledgeDocument document) {
        return knowledgeService.createDocument(document);
    }

    @Operation(summary = "查询知识文档")
    @GetMapping("/{id}")
    public Result<KnowledgeDocument> getDocumentById(@PathVariable Long id) {
        return knowledgeService.getDocumentById(id);
    }

    @Operation(summary = "分页查询知识文档")
    @GetMapping("/list")
    public Result<Page<KnowledgeDocument>> listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return knowledgeService.listDocuments(page, pageSize, keyword);
    }

    @Operation(summary = "更新知识文档")
    @PutMapping
    public Result<Void> updateDocument(@RequestBody KnowledgeDocument document) {
        return knowledgeService.updateDocument(document);
    }

    @Operation(summary = "删除知识文档")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        return knowledgeService.deleteDocument(id);
    }
}
