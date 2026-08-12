package com.aics.chat.controller;

import com.aics.chat.rag.retrieve.HybridRetriever;
import com.aics.chat.rag.retrieve.RetrievalMode;
import com.aics.chat.rag.retrieve.RetrieveResult;
import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检索测试控制器：验证不同检索模式（向量/Hybrid/改写/图谱）的召回效果。
 */
@Tag(name = "检索测试")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class RetrieveController {

    private final HybridRetriever hybridRetriever;

    @Operation(summary = "检索测试（VECTOR/HYBRID/HYBRID_QUERY_REWRITE/GRAPH_RAG）")
    @GetMapping("/retrieve/test")
    public Result<RetrieveResult> retrieveTest(@RequestParam("knowledgeBase") String knowledgeBase,
                                               @RequestParam("query") String query,
                                               @RequestParam(value = "mode", defaultValue = "VECTOR") String mode,
                                               @RequestParam(value = "topK", defaultValue = "5") int topK) {
        RetrievalMode m;
        try {
            m = RetrievalMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            m = RetrievalMode.VECTOR;
        }
        return Result.success(hybridRetriever.retrieve(knowledgeBase, query, m, topK));
    }
}