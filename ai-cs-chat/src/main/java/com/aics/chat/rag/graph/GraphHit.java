package com.aics.chat.rag.graph;

import lombok.Data;

import java.util.List;

/**
 * 图谱多跳查询命中。
 */
@Data
public class GraphHit {

    /** 起始实体 */
    private String entity;

    /** 多跳展开的三元组 */
    private List<GraphTriple> triples;

    /** 展开深度 */
    private int depth;
}
