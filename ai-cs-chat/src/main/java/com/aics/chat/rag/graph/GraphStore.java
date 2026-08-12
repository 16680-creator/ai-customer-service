package com.aics.chat.rag.graph;

import java.util.List;

/**
 * 知识图谱存储抽象。
 *
 * <p>默认实现 {@link InMemoryGraphStore}（进程内，开发/测试用）；
 * 后续可替换为 Neo4j 等图数据库实现，业务层只依赖本接口。</p>
 */
public interface GraphStore {

    /**
     * 新增三元组（校验 subject/predicate/object 非空）。
     *
     * @param triple 三元组
     * @return 带主键的三元组
     */
    GraphTriple add(GraphTriple triple);

    /**
     * 按 ID 删除三元组。
     *
     * @param id 主键
     * @return 是否删除成功
     */
    boolean delete(Long id);

    /**
     * 按知识库列出三元组。
     *
     * @param knowledgeBase 知识库标识
     * @return 三元组列表
     */
    List<GraphTriple> listByKnowledgeBase(String knowledgeBase);

    /**
     * 以实体为起点做多跳展开（BFS，subject 或 object 匹配）。
     *
     * @param entity        起始实体
     * @param knowledgeBase 知识库标识
     * @param depth         展开深度（0 表示仅直接关系）
     * @return 命中的三元组（去重）
     */
    List<GraphTriple> queryMultiHop(String entity, String knowledgeBase, int depth);
}
