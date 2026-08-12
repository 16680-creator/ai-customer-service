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
 * RAG 评估控制器 —— 运行 golden 测试集评估，量化 RAG 检索与回答质量。
 *
 * <h3>学习要点（技术：LLM-as-Judge / golden 回归测试）</h3>
 * <ul>
 *   <li><b>为什么需要评估</b>：RAG 检索链路（向量召回、Rerank、Hybrid、改写）改动后，
 *       必须用固定测试集量化"命中率/回答质量"是否回退，否则无法证明改进有效。</li>
 *   <li><b>golden 集</b>：预置一批「问题 + 期望命中文档 + 参考答案」，是质量基线的输入。</li>
 *   <li><b>LLM-as-Judge</b>：让大模型给回答按 1-5 分打分，作为回答质量的近似度量。</li>
 *   <li><b>CI 门禁</b>：评估结果低于阈值即构建失败，见 pom.xml 的 {@code -Peval} profile。</li>
 * </ul>
 */
@Tag(name = "RAG评估")
@RestController
@RequestMapping("/rag/eval")
@RequiredArgsConstructor
public class RagEvalController {

    private final RagEvaluator ragEvaluator;

    /**
     * 运行 RAG 评估。
     *
     * <p>请求体携带 golden 集路径、知识库、检索模式与阈值；评估器逐条检索→算指标→
     * LLM 打分→汇总报告（{@link RagEvalReport}），结果随统一响应返回。</p>
     *
     * @param request 评估请求（goldenSetPath 默认 classpath:eval/golden-set.json）
     * @return 评估报告（含指标、均分、逐条明细、passed 门禁结果）
     */
    @Operation(summary = "运行 RAG 评估")
    @PostMapping("/run")
    public Result<RagEvalReport> run(@RequestBody RagEvalRequest request) {
        return Result.success(ragEvaluator.evaluate(request));
    }
}