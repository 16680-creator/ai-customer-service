package com.aics.chat.rag.rewrite;

import lombok.Data;

import java.util.List;

/**
 * 查询改写结果。
 */
@Data
public class RewriteResult {

    /** 原始问题 */
    private String originalQuery;

    /** 改写后的子查询列表 */
    private List<String> subQueries;

    /** 假设性文档（HyDE），可空 */
    private String hydeDocument;
}