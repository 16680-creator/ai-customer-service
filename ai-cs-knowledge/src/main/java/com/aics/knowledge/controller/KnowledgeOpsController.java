package com.aics.knowledge.controller;

import com.aics.common.result.Result;
import com.aics.knowledge.ops.ClusterReport;
import com.aics.knowledge.ops.FaqService;
import com.aics.knowledge.ops.FaqSuggestion;
import com.aics.knowledge.ops.QuestionClusterService;
import com.aics.knowledge.ops.QuestionItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库运营控制器：提问聚类、缺口分析、FAQ 收录。
 */
@Tag(name = "知识库运营")
@RestController
@RequestMapping("/knowledge/ops")
@RequiredArgsConstructor
public class KnowledgeOpsController {

    private final QuestionClusterService questionClusterService;
    private final FaqService faqService;

    @Operation(summary = "运行提问聚类与缺口分析")
    @PostMapping("/cluster")
    public Result<ClusterReport> cluster(@RequestBody ClusterRequest request) {
        ClusterReport report = questionClusterService.cluster(
                request.getPeriod(), request.getQuestions(), request.getGapHitRateThreshold());
        return Result.success(report);
    }

    @Operation(summary = "收录 FAQ（触发知识向量更新）")
    @PostMapping("/faq")
    public Result<Map<String, Object>> adoptFaq(@RequestBody FaqSuggestion suggestion) {
        return Result.success(faqService.adopt(suggestion));
    }

    @Data
    public static class ClusterRequest {
        private String period;
        private List<QuestionItem> questions;
        private Double gapHitRateThreshold;
    }
}