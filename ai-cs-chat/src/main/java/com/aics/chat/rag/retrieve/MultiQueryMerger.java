package com.aics.chat.rag.retrieve;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多查询结果融合 —— RRF（Reciprocal Rank Fusion，倒数排名融合）。
 *
 * <h3>学习要点（技术：RRF 融合算法）</h3>
 * <ul>
 *   <li><b>为什么需要融合</b>：查询改写后有多路子查询结果、还有 HyDE 结果，
 *       需要合并成一份有序列表。RRF 是业界常用、无需训练分数的融合算法。</li>
 *   <li><b>核心公式</b>：对每个文档累加 {@code 1 / (rrfK + rank)}。
 *       排名越靠前贡献越大；在【多路中都靠前】的文档总分最高——天然实现"共识优先"。</li>
 *   <li><b>rrfK 常数</b>：通常取 60，越大排名差异的影响越小（平滑）。</li>
 *   <li><b>去重</b>：同一文档多路出现只保留一次，并把融合分写入 metadata.rrfScore。</li>
 * </ul>
 */
public final class MultiQueryMerger {

    private MultiQueryMerger() {
    }

    /**
     * 融合多路文档列表。
     *
     * @param results 多路检索结果（每路按相关度降序）
     * @param topK    融合后返回条数
     * @param rrfK    RRF 平滑常数（通常 60）
     * @return 按 RRF 分数降序去重后的文档列表
     */
    public static List<Document> merge(List<List<Document>> results, int topK, int rrfK) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        // docId -> RRF 累计分
        Map<String, Double> scoreMap = new LinkedHashMap<>();
        // docId -> 文档（保留第一次出现）
        Map<String, Document> docMap = new LinkedHashMap<>();
        int k = rrfK <= 0 ? 60 : rrfK;

        for (List<Document> list : results) {
            if (list == null) {
                continue;
            }
            int rank = 1;
            for (Document doc : list) {
                if (doc == null) {
                    continue;
                }
                String id = docId(doc);
                scoreMap.merge(id, 1.0 / (k + rank), Double::sum);
                docMap.putIfAbsent(id, doc);
                rank++;
            }
        }

        List<String> sortedIds = new ArrayList<>(scoreMap.keySet());
        sortedIds.sort(Comparator.comparingDouble(scoreMap::get).reversed());

        List<Document> merged = new ArrayList<>();
        for (String id : sortedIds) {
            if (merged.size() >= topK) {
                break;
            }
            Document doc = docMap.get(id);
            doc.getMetadata().put("rrfScore", scoreMap.get(id));
            merged.add(doc);
        }
        return merged;
    }

    private static String docId(Document doc) {
        Object documentId = doc.getMetadata().get("documentId");
        if (documentId != null) {
            return String.valueOf(documentId);
        }
        return doc.getId();
    }
}
