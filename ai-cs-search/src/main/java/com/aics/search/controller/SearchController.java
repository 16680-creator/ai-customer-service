package com.aics.knowledge.mapper;

import com.aics.knowledge.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文档 Mapper
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<KnowledgeDocument> {
}package com.aics.search.controller;

import com.aics.common.result.Result;
import com.aics.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 搜索控制器
 */
@Tag(name = "全文搜索")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "全文搜索")
    @GetMapping("/{index}")
    public Result<List<Map<String, Object>>> search(
            @PathVariable @NotBlank(message = "索引名称不能为空") String index,
            @RequestParam @NotBlank(message = "搜索关键词不能为空") String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return searchService.search(index, query, page, size);
    }

    @Operation(summary = "创建索引")
    @PostMapping("/index/{index}")
    public Result<Void> createIndex(
            @PathVariable @NotBlank(message = "索引名称不能为空") String index,
            @RequestBody Map<String, Object> mappings) {
        return searchService.createIndex(index, mappings);
    }

    @Operation(summary = "索引文档")
    @PostMapping("/document/{index}")
    public Result<Void> indexDocument(
            @PathVariable @NotBlank(message = "索引名称不能为空") String index,
            @RequestBody Map<String, Object> document) {
        return searchService.indexDocument(index, document);
    }

    @Operation(summary = "删除索引")
    @DeleteMapping("/index/{index}")
    public Result<Void> deleteIndex(
            @PathVariable @NotBlank(message = "索引名称不能为空") String index) {
        return searchService.deleteIndex(index);
    }
}
