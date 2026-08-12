package com.aics.search.hybrid;

import lombok.Data;

import java.util.List;

/**
 * 混合检索分页结果 VO
 */
@Data
public class HybridResultPageVO {

    /** 融合后的结果总数（未分页） */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页大小 */
    private int size;

    /** 当前页结果 */
    private List<HybridSearchResult> records;
}
