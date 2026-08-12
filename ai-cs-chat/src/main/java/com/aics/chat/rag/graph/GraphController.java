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
 * 知识图谱管理控制器：三元组 CRUD 与多跳查询。
 */
@Tag(name = "知识图谱")
@RestController
@RequestMapping("/rag/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphStore graphStore;
    private final GraphRagService graphRagService;

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