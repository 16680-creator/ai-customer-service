package com.aics.chat.rag.graph;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内知识图谱存储（默认实现，开发/测试用）。
 *
 * <p>使用 {@link LinkedHashMap} 保存三元组；多跳展开复用共享 BFS 工具
 * {@link GraphTripleBfs}，与 Neo4j 版语义一致。通过
 * {@code aics.rag.graph.storage} 配置切换（默认 in-memory）。</p>
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "aics.rag.graph.storage", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryGraphStore implements GraphStore {

    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_PREDICATE_LENGTH = 128;
    private static final int MAX_OBJECT_LENGTH = 255;

    private final Map<Long, GraphTriple> triples = new LinkedHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public synchronized GraphTriple add(GraphTriple triple) {
        validate(triple);
        GraphTriple copy = new GraphTriple();
        copy.setId(idSequence.getAndIncrement());
        copy.setSubject(triple.getSubject().trim());
        copy.setPredicate(triple.getPredicate().trim());
        copy.setObject(triple.getObject().trim());
        copy.setKnowledgeBase(triple.getKnowledgeBase());
        copy.setSourceDocumentId(triple.getSourceDocumentId());
        triples.put(copy.getId(), copy);
        log.info("图谱新增三元组: id={}, {} -> {} -> {}", copy.getId(),
                copy.getSubject(), copy.getPredicate(), copy.getObject());
        return copy;
    }

    @Override
    public synchronized boolean delete(Long id) {
        return triples.remove(id) != null;
    }

    @Override
    public List<GraphTriple> listByKnowledgeBase(String knowledgeBase) {
        if (knowledgeBase == null) {
            return Collections.emptyList();
        }
        return triples.values().stream()
                .filter(t -> knowledgeBase.equals(t.getKnowledgeBase()))
                .toList();
    }

    @Override
    public synchronized List<GraphTriple> queryMultiHop(String entity, String knowledgeBase, int depth) {
        return GraphTripleBfs.expand(entity, listByKnowledgeBase(knowledgeBase), depth);
    }

    private void validate(GraphTriple triple) {
        if (triple == null) {
            throw new IllegalArgumentException("三元组不能为空");
        }
        if (triple.getSubject() == null || triple.getSubject().isBlank()) {
            throw new IllegalArgumentException("subject 不能为空");
        }
        if (triple.getPredicate() == null || triple.getPredicate().isBlank()) {
            throw new IllegalArgumentException("predicate 不能为空");
        }
        if (triple.getObject() == null || triple.getObject().isBlank()) {
            throw new IllegalArgumentException("object 不能为空");
        }
        if (triple.getSubject().trim().length() > MAX_SUBJECT_LENGTH
                || triple.getObject().trim().length() > MAX_OBJECT_LENGTH) {
            throw new IllegalArgumentException("subject/object 长度超限");
        }
        if (triple.getPredicate().trim().length() > MAX_PREDICATE_LENGTH) {
            throw new IllegalArgumentException("predicate 长度超限");
        }
    }
}