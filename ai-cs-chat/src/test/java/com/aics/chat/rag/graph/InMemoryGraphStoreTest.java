package com.aics.chat.rag.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InMemoryGraphStore 单元测试：三元组 CRUD 与多跳 BFS 查询。
 */
class InMemoryGraphStoreTest {

    private InMemoryGraphStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryGraphStore();
    }

    private GraphTriple triple(String s, String p, String o) {
        GraphTriple t = new GraphTriple();
        t.setSubject(s);
        t.setPredicate(p);
        t.setObject(o);
        t.setKnowledgeBase("kb");
        return t;
    }

    @Test
    @DisplayName("新增三元组: 返回自增 ID 并可查回")
    void add_and_get() {
        GraphTriple saved = store.add(triple("退款政策", "指向", "申请入口"));
        assertThat(saved.getId()).isNotNull();
        assertThat(store.listByKnowledgeBase("kb")).hasSize(1);
    }

    @Test
    @DisplayName("多跳查询: 沿 subject->object 展开指定深度")
    void queryMultiHop_expands() {
        store.add(triple("退款政策", "指向", "申请入口"));
        store.add(triple("申请入口", "指向", "审核时效"));
        List<GraphTriple> hits = store.queryMultiHop("退款政策", "kb", 2);
        assertThat(hits).extracting(GraphTriple::getSubject)
                .contains("退款政策", "申请入口");
        assertThat(hits).extracting(GraphTriple::getObject)
                .contains("申请入口", "审核时效");
    }

    @Test
    @DisplayName("多跳查询: 深度为 0 只返回直接关系")
    void queryMultiHop_depthZero() {
        store.add(triple("退款政策", "指向", "申请入口"));
        List<GraphTriple> hits = store.queryMultiHop("退款政策", "kb", 0);
        assertThat(hits).hasSize(1);
    }

    @Test
    @DisplayName("循环边: 不无限递归")
    void queryMultiHop_cycleSafe() {
        store.add(triple("A", "指向", "B"));
        store.add(triple("B", "指向", "A"));
        List<GraphTriple> hits = store.queryMultiHop("A", "kb", 5);
        assertThat(hits).hasSize(2);
    }

    @Test
    @DisplayName("空图或未命中: 返回空列表")
    void queryMultiHop_noHit() {
        assertThat(store.queryMultiHop("不存在", "kb", 2)).isEmpty();
    }

    @Test
    @DisplayName("校验: 空 subject/predicate/object 拒绝")
    void add_validation() {
        assertThatThrownBy(() -> store.add(triple("", "指向", "申请入口")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.add(triple("A", "", "B")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.add(triple("A", "指向", "  ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
