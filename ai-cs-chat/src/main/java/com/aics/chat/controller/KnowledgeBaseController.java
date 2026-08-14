package com.aics.chat.controller;

import com.aics.chat.service.KnowledgeBaseService;
import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库管理接口 —— 为 RAG 功能提供数据入库和检索的入口。
 *
 * <p>使用流程：</p>
 * <pre>
 * 1. 先入库：调用 POST /rag/knowledge-base/upload 或 /text 把资料写入向量库
 * 2. 再提问：调用真正的 RAG 对话接口（ChatController 的 /chat/rag），即可基于知识库回答
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/rag/knowledge-base")
@RequiredArgsConstructor
@Tag(name = "知识库管理")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 上传文本内容入库。
     *
     * @param knowledgeBase 知识库标识（建议用英文/数字，如 "product-manual"）
     * @param text          需要入库的文本内容
     * @return 入库的分块数量
     */
    @Operation(summary = "文本入库")
    @PostMapping("/text")
    public Result<Map<String, Object>> addText(@RequestParam("knowledgeBase") String knowledgeBase,
                                               @RequestParam("text") String text) {
        int count = knowledgeBaseService.addText(knowledgeBase, text);
        return Result.success(Map.of("knowledgeBase", knowledgeBase, "chunks", count));
    }

    /**
     * 上传文件（PDF/TXT）入库。
     *
     * @param knowledgeBase 知识库标识
     * @param file          上传的文档文件
     * @return 入库的分块数量
     */
    @Operation(summary = "文件入库")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("knowledgeBase") String knowledgeBase,
                                              @RequestParam("file") MultipartFile file) {
        int count = knowledgeBaseService.addFile(knowledgeBase, file);
        return Result.success(Map.of("knowledgeBase", knowledgeBase,
                "fileName", file.getOriginalFilename(), "chunks", count));
    }

    /**
     * 检索测试：查看某个问题在指定知识库中能命中哪些资料。
     *
     * @param knowledgeBase 知识库标识
     * @param query         检索问题
     * @return 命中的文档片段（含相似度）
     */
    @Operation(summary = "语义检索测试")
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam("knowledgeBase") String knowledgeBase,
                                                    @RequestParam("query") String query) {
        List<Document> hits = knowledgeBaseService.search(knowledgeBase, query, 5, 0.3);
        List<Map<String, Object>> result = hits.stream().map(doc -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("text", doc.getText());
            item.put("score", doc.getScore() != null ? doc.getScore() : 0.0);
            return item;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}