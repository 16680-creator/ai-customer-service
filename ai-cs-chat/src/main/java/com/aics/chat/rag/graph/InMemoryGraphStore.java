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
 * 进程内知识图谱存储 —— 默认实现（零外部依赖，开发/测试用）。
 *
 * <h3>学习要点（技术：图存储抽象 / 条件装配）</h3>
 * <ul>
 *   <li><b>为什么先做内存版</b>：不依赖 Neo4j 即可验证完整 GraphRAG 链路，
 *       且作为单测的基础实现。</li>
 *   <li><b>接口抽象</b>：业务层只依赖 {@link GraphStore} 接口；生产可切换
 *       {@link Neo4jGraphStore}（通过 Nacos {@code aics.rag.graph.storage} 配置）。</li>
 *   <li><b>条件装配</b>：{@code @ConditionalOnProperty} 保证默认只激活内存版，
 *       配置 neo4j 时才激活 Neo4j 版，避免两个 Bean 冲突。</li>
 *   <li><b>线程安全</b>：add/delete/queryMultiHop 加 synchronized，
 *       因为 HashMap 在多线程写时可能破坏结构。</li>
 * </ul>
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
        validate(triple);                          // 校验 subject/predicate/object 非空、长度合规
        GraphTriple copy = new GraphTriple();      // 复制一份，避免外部对象被后续修改影响
        copy.setId(idSequence.getAndIncrement());  // 进程内自增 ID（Neo4j 版语义一致）
        copy.setSubject(triple.getSubject().trim());
        copy.setPredicate(triple.getPredicate().trim());
        copy.setObject(triple.getObject().trim());
        copy.setKnowledgeBase(triple.getKnowledgeBase());
        copy.setSourceDocumentId(triple.getSourceDocumentId());
        triples.put(copy.getId(), copy);           // 写入内存 Map
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