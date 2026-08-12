package com.aics.chat.rag.graph;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Neo4j 图数据库存储实现 —— GraphRAG 的生产级存储。
 *
 * <h3>学习要点（技术：Neo4j / Cypher / 图存储抽象）</h3>
 * <ul>
 *   <li><b>为什么用 Neo4j</b>：真正的图数据库专为"关系遍历"优化，
 *       适合大规模实体关联；本项目用 {@code :Triple} 节点保存三元组，简单直观。</li>
 *   <li><b>Cypher 语句</b>：CREATE 写节点、MATCH+DELETE 删节点、MATCH 查询；
 *       参数用 {@link org.neo4j.driver.Values#parameters} 绑定，避免拼接注入。</li>
 *   <li><b>多跳语义复用</b>：先按知识库拉出三元组，再复用 {@link GraphTripleBfs}
 *       做 BFS——与内存版完全一致，只是"存储介质"不同，这正是接口抽象的好处。</li>
 *   <li><b>条件装配</b>：{@code storage=neo4j} 时才激活本 Bean（见 @ConditionalOnProperty），
 *       默认内存版，Neo4j 未部署时主链路不受影响。</li>
 *   <li><b>资源管理</b>：Session 用 try-with-resources 自动关闭；{@code @PreDestroy} 关闭 Driver。</li>
 * </ul>
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "aics.rag.graph.storage", havingValue = "neo4j")
public class Neo4jGraphStore implements GraphStore {

    private final Driver driver;
    private final GraphProperties properties;
    private final AtomicLong idSequence = new AtomicLong(1);

    public Neo4jGraphStore(Driver driver, GraphProperties properties) {
        this.driver = driver;
        this.properties = properties;
    }

    @Override
    public GraphTriple add(GraphTriple triple) {
        validate(triple);
        GraphTriple copy = new GraphTriple();
        copy.setId(idSequence.getAndIncrement());
        copy.setSubject(triple.getSubject().trim());
        copy.setPredicate(triple.getPredicate().trim());
        copy.setObject(triple.getObject().trim());
        copy.setKnowledgeBase(triple.getKnowledgeBase());
        copy.setSourceDocumentId(triple.getSourceDocumentId());

        try (Session session = driver.session(properties.databaseConfig())) {
            session.run("CREATE (t:Triple {id: $id, subject: $subject, predicate: $predicate, "
                            + "object: $object, knowledgeBase: $kb, sourceDocumentId: $sid})",
                    Values.parameters("id", copy.getId(), "subject", copy.getSubject(),
                            "predicate", copy.getPredicate(), "object", copy.getObject(),
                            "kb", copy.getKnowledgeBase(),
                            "sid", copy.getSourceDocumentId() == null ? null : copy.getSourceDocumentId()));
        }
        log.info("Neo4j 图谱新增三元组: id={}, {} -> {} -> {}", copy.getId(),
                copy.getSubject(), copy.getPredicate(), copy.getObject());
        return copy;
    }

    @Override
    public boolean delete(Long id) {
        try (Session session = driver.session(properties.databaseConfig())) {
            var result = session.run("MATCH (t:Triple {id: $id}) DELETE t RETURN count(t) AS deleted",
                    Values.parameters("id", id));
            if (result.hasNext()) {
                return result.next().get("deleted").asLong() > 0;
            }
        }
        return false;
    }

    @Override
    public List<GraphTriple> listByKnowledgeBase(String knowledgeBase) {
        if (knowledgeBase == null) {
            return List.of();
        }
        List<GraphTriple> triples = new ArrayList<>();
        try (Session session = driver.session(properties.databaseConfig())) {
            var result = session.run(
                    "MATCH (t:Triple {knowledgeBase: $kb}) RETURN t.id AS id, t.subject AS subject, "
                            + "t.predicate AS predicate, t.object AS object, t.knowledgeBase AS kb, "
                            + "t.sourceDocumentId AS sid",
                    Values.parameters("kb", knowledgeBase));
            while (result.hasNext()) {
                Record r = result.next();
                GraphTriple t = new GraphTriple();
                t.setId(r.get("id").asLong());
                t.setSubject(r.get("subject").asString());
                t.setPredicate(r.get("predicate").asString());
                t.setObject(r.get("object").asString());
                t.setKnowledgeBase(r.get("kb").asString());
                if (!r.get("sid").isNull()) {
                    t.setSourceDocumentId(r.get("sid").asLong());
                }
                triples.add(t);
            }
        }
        return triples;
    }

    @Override
    public List<GraphTriple> queryMultiHop(String entity, String knowledgeBase, int depth) {
        return GraphTripleBfs.expand(entity, listByKnowledgeBase(knowledgeBase), depth);
    }

    private void validate(GraphTriple triple) {
        if (triple == null || triple.getSubject() == null || triple.getSubject().isBlank()
                || triple.getPredicate() == null || triple.getPredicate().isBlank()
                || triple.getObject() == null || triple.getObject().isBlank()) {
            throw new IllegalArgumentException("三元组 subject/predicate/object 不能为空");
        }
    }

    @PreDestroy
    public void close() {
        driver.close();
    }
}