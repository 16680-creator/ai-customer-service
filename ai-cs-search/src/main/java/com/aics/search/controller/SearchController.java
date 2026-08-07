package com.aics.search.controller;

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
            @PathVariable("index") @NotBlank(message = "索引名称不能为空") String index,
            @RequestParam("query") @NotBlank(message = "搜索关键词不能为空") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return searchService.search(index, query, page, size);
    }

    @Operation(summary = "创建索引")
    @PostMapping("/index/{index}")
    public Result<Void> createIndex(
            @PathVariable("index") @NotBlank(message = "索引名称不能为空") String index,
            @RequestBody Map<String, Object> mappings) {
        return searchService.createIndex(index, mappings);
    }

    @Operation(summary = "索引文档")
    @PostMapping("/document/{index}")
    public Result<Void> indexDocument(
            @PathVariable("index") @NotBlank(message = "索引名称不能为空") String index,
            @RequestBody Map<String, Object> document) {
        return searchService.indexDocument(index, document);
    }

    @Operation(summary = "删除索引")
    @DeleteMapping("/index/{index}")
    public Result<Void> deleteIndex(
            @PathVariable("index") @NotBlank(message = "索引名称不能为空") String index) {
        return searchService.deleteIndex(index);
    }
}