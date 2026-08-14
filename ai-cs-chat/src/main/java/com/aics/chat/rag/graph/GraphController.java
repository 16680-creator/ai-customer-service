package com.aics.chat.rag.graph;

import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱管理控制器 —— GraphRAG 的图谱数据入口。
 *
 * <h3>学习要点（技术：知识图谱 / GraphRAG）</h3>
 * <ul>
 *   <li><b>三元组</b>：知识用 {@code (主体 subject, 关系 predicate, 客体 object)}
 *       表示，如「退款政策 →指向→ 申请入口」。多条三元组连成有向图。</li>
 *   <li><b>GraphRAG 的价值</b>：普通 RAG 只能检索单篇文档；图谱能把跨文档的
 *       关联知识（产品→配件→兼容性）沿关系多跳展开，回答"链路型"问题。</li>
 *   <li><b>多跳查询</b>：从实体出发按关系做 BFS 展开（见 {@link GraphTripleBfs}），
 *       深度可控，循环边有防护。</li>
 * </ul>
 */
@Tag(name = "知识图谱")
@RestController
@RequestMapping("/rag/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphStore graphStore;
    private final GraphRagService graphRagService;

    /**
     * 新增三元组：把一条「主体-关系-客体」写入图谱存储。
     *
     * @param request 三元组请求体（subject/predicate/object/knowledgeBase）
     * @return 带主键的三元组
     */
    @Operation(summary = "新增三元组")
    @PostMapping("/triple")
    public Result<GraphTriple> addTriple(@RequestBody GraphTripleRequest request) {
        GraphTriple triple = new GraphTriple();
        triple.setSubject(request.getSubject());
        triple.setPredicate(request.getPredicate());
        triple.setObject(request.getObject());
        triple.setKnowledgeBase(request.getKnowledgeBase());
        triple.setSourceDocumentId(request.getSourceDocumentId());
        return Result.success(graphStore.add(triple));
    }

    /**
     * 多跳图谱检索：从实体出发按指定深度展开关联三元组。
     *
     * @param entity        起始实体（如"退款政策"）
     * @param depth         展开深度（2 表示"直接关系 + 再跳一层"）
     * @param knowledgeBase 知识库标识
     * @return 图谱命中（含展开的三元组列表）
     */
    @Operation(summary = "多跳图谱检索")
    @GetMapping("/query")
    public Result<GraphHit> query(@RequestParam("entity") String entity,
                                  @RequestParam(value = "depth", defaultValue = "2") int depth,
                                  @RequestParam("knowledgeBase") String knowledgeBase) {
        List<GraphTriple> triples = graphStore.queryMultiHop(entity, knowledgeBase, depth);
        GraphHit hit = new GraphHit();
        hit.setEntity(entity);
        hit.setTriples(triples);
        hit.setDepth(depth);
        return Result.success(hit);
    }

    @Operation(summary = "按问题检索图谱上下文（供 RAG 编排）")
    @GetMapping("/retrieve")
    public Result<Map<String, Object>> retrieve(@RequestParam("question") String question,
                                                @RequestParam("knowledgeBase") String knowledgeBase) {
        List<GraphTriple> hits = graphRagService.retrieveWithGraph(question, knowledgeBase);
        return Result.success(Map.of("enabled", true, "hits", hits, "count", hits.size()));
    }

    @Operation(summary = "列出三元组")
    @GetMapping("/triples")
    public Result<List<GraphTriple>> triples(@RequestParam("knowledgeBase") String knowledgeBase) {
        return Result.success(graphStore.listByKnowledgeBase(knowledgeBase));
    }

    @Data
    public static class GraphTripleRequest {
        private String subject;
        private String predicate;
        private String object;
        private String knowledgeBase;
        private Long sourceDocumentId;
    }
}