package com.aics.chat.rag.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Neo4jGraphStore 单元测试：Mock Driver/Session，验证 Cypher 调用与多跳 BFS 复用。
 */
class Neo4jGraphStoreTest {

    private Driver driver;
    private Session session;
    private Result result;
    private GraphProperties properties;
    private Neo4jGraphStore store;

    @BeforeEach
    void setUp() {
        driver = mock(Driver.class);
        session = mock(Session.class);
        result = mock(Result.class);
        when(driver.session(any(SessionConfig.class))).thenReturn(session);
        when(session.run(anyString(), any(Value.class))).thenReturn(result);
        properties = new GraphProperties();
        properties.setStorage("neo4j");
        store = new Neo4jGraphStore(driver, properties);
    }

    @Test
    @DisplayName("新增三元组: 生成 ID 并执行 CREATE")
    void add_runsCreate() {
        GraphTriple t = new GraphTriple();
        t.setSubject("退款政策");
        t.setPredicate("指向");
        t.setObject("申请入口");
        t.setKnowledgeBase("kb");

        GraphTriple saved = store.add(t);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSubject()).isEqualTo("退款政策");
        org.mockito.Mockito.verify(session).run(org.mockito.ArgumentMatchers.contains("CREATE (t:Triple"),
                org.mockito.ArgumentMatchers.any(Value.class));
    }

    @Test
    @DisplayName("列出三元组: 解析 Record 字段")
    void list_parsesRecords() {
        when(result.hasNext()).thenReturn(true, true, false);
        Record r1 = mock(Record.class);
        Record r2 = mock(Record.class);
        when(result.next()).thenReturn(r1, r2);
        when(r1.get("id")).thenReturn(Values.value(1L));
        when(r1.get("subject")).thenReturn(Values.value("A"));
        when(r1.get("predicate")).thenReturn(Values.value("指向"));
        when(r1.get("object")).thenReturn(Values.value("B"));
        when(r1.get("kb")).thenReturn(Values.value("kb"));
        when(r1.get("sid")).thenReturn(Values.NULL);
        when(r2.get("id")).thenReturn(Values.value(2L));
        when(r2.get("subject")).thenReturn(Values.value("B"));
        when(r2.get("predicate")).thenReturn(Values.value("指向"));
        when(r2.get("object")).thenReturn(Values.value("C"));
        when(r2.get("kb")).thenReturn(Values.value("kb"));
        when(r2.get("sid")).thenReturn(Values.NULL);

        List<GraphTriple> triples = store.listByKnowledgeBase("kb");

        assertThat(triples).hasSize(2);
        assertThat(triples.get(0).getSubject()).isEqualTo("A");
        assertThat(triples.get(1).getObject()).isEqualTo("C");
    }

    @Test
    @DisplayName("多跳查询: 复用 BFS 展开（A -> B -> C）")
    void queryMultiHop_bfs() {
        GraphTriple t1 = new GraphTriple();
        t1.setId(1L);
        t1.setSubject("A");
        t1.setPredicate("指向");
        t1.setObject("B");
        t1.setKnowledgeBase("kb");
        GraphTriple t2 = new GraphTriple();
        t2.setId(2L);
        t2.setSubject("B");
        t2.setPredicate("指向");
        t2.setObject("C");
        t2.setKnowledgeBase("kb");

        when(result.hasNext()).thenReturn(true, true, false);
        Record r1 = mock(Record.class);
        Record r2 = mock(Record.class);
        when(result.next()).thenReturn(r1, r2);
        when(r1.get("id")).thenReturn(Values.value(1L));
        when(r1.get("subject")).thenReturn(Values.value("A"));
        when(r1.get("predicate")).thenReturn(Values.value("指向"));
        when(r1.get("object")).thenReturn(Values.value("B"));
        when(r1.get("kb")).thenReturn(Values.value("kb"));
        when(r1.get("sid")).thenReturn(Values.NULL);
        when(r2.get("id")).thenReturn(Values.value(2L));
        when(r2.get("subject")).thenReturn(Values.value("B"));
        when(r2.get("predicate")).thenReturn(Values.value("指向"));
        when(r2.get("object")).thenReturn(Values.value("C"));
        when(r2.get("kb")).thenReturn(Values.value("kb"));
        when(r2.get("sid")).thenReturn(Values.NULL);

        List<GraphTriple> hits = store.queryMultiHop("A", "kb", 2);

        assertThat(hits).hasSize(2);
        assertThat(hits).extracting(GraphTriple::getObject).contains("B", "C");
    }

    @Test
    @DisplayName("删除: deleted>0 返回 true")
    void delete_returnsTrue() {
        when(result.hasNext()).thenReturn(true);
        Record r = mock(Record.class);
        when(result.next()).thenReturn(r);
        Value value = mock(Value.class);
        when(r.get("deleted")).thenReturn(value);
        when(value.asLong()).thenReturn(1L);

        assertThat(store.delete(1L)).isTrue();
    }

    @Test
    @DisplayName("校验: 空字段拒绝")
    void add_validation() {
        GraphTriple t = new GraphTriple();
        t.setSubject("");
        t.setPredicate("指向");
        t.setObject("B");
        t.setKnowledgeBase("kb");
        assertThatThrownBy(() -> store.add(t)).isInstanceOf(IllegalArgumentException.class);
    }
}