package com.aics.chat.rag.retrieve;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多查询结果融合（RRF，Reciprocal Rank Fusion）。
 *
 * <p>把多路检索结果（如多个改写子查询、HyDE 文档查询）按排名融合：
 * 对每个文档累加 {@code 1 / (rrfK + rank)}，按总分降序取 Top-N，并按文档 ID 去重。</p>
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
