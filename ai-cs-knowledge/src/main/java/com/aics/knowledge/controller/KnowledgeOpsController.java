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
 * 知识库运营控制器 —— 数据驱动地建设知识库。
 *
 * <h3>学习要点（技术：知识库运营闭环）</h3>
 * <ul>
 *   <li><b>闭环流程</b>：①聚类历史提问成主题 → ②检测"高频但知识库命中率低"的缺口
 *       → ③运营把缺口主题一键收录为 FAQ → ④FAQ 自动创建知识文档并向量化，
 *       反哺 RAG 检索。知识库从"拍脑袋建设"变成"看数据迭代"。</li>
 *   <li><b>三个接口</b>：/cluster 运行聚类与缺口分析；/cluster/report 查最新报告；
 *       /faq 收录 FAQ。</li>
 * </ul>
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