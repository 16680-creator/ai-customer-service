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
 * 检索测试控制器 —— 验证不同检索模式（向量/Hybrid/改写/图谱）的召回效果。
 *
 * <h3>学习要点（技术：多路检索模式）</h3>
 * <ul>
 *   <li><b>VECTOR</b>：纯向量语义检索（bge-m3 向量化后余弦相似度），默认存量行为。</li>
 *   <li><b>HYBRID</b>：ES 关键词(BM25) + 向量语义，经 RRF 倒数排名融合（见 ai-cs-search）。</li>
 *   <li><b>HYBRID_QUERY_REWRITE</b>：先让 LLM 改写模糊问题为多个精确子查询 + HyDE 假设文档，再融合检索。</li>
 *   <li><b>GRAPH_RAG</b>：知识图谱多跳检索补充上下文，未命中自动降级普通检索。</li>
 *   <li>各模式全局开关默认关闭（见 Nacos {@code aics.rag.*}），未开启时自动降级纯向量，保证存量兼容。</li>
 * </ul>
 */
@Tag(name = "检索测试")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class RetrieveController {

    private final HybridRetriever hybridRetriever;

    /**
     * 检索测试接口：指定模式执行一次检索，返回命中文档与降级信息。
     *
     * <p>非法 mode 参数会被安全降级为 VECTOR，不返回错误——保证测试接口健壮。</p>
     *
     * @param knowledgeBase 知识库标识（如 product-manual / knowledge）
     * @param query         检索问题
     * @param mode          检索模式（默认 VECTOR）
     * @param topK          返回条数（默认 5）
     * @return 统一检索结果（含实际执行模式、是否降级、命中文档）
     */
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