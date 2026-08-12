package com.aics.chat.controller;

import com.aics.chat.rag.eval.RagEvalReport;
import com.aics.chat.rag.eval.RagEvalRequest;
import com.aics.chat.rag.eval.RagEvaluator;
import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 评估控制器：运行 golden 测试集评估，供研发/CI 使用。
 */
@Tag(name = "RAG评估")
@RestController
@RequestMapping("/rag/eval")
@RequiredArgsConstructor
public class RagEvalController {

    private final RagEvaluator ragEvaluator;

    @Operation(summary = "运行 RAG 评估")
    @PostMapping("/run")
    public Result<RagEvalReport> run(@RequestBody RagEvalRequest request) {
        return Result.success(ragEvaluator.evaluate(request));
    }
}