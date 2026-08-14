package com.aics.chat.rag.rewrite;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 查询改写/HyDE 配置。
 */
@Data
@ConfigurationProperties(prefix = "aics.rag.rewrite")
public class QueryRewriteProperties {

    /** 是否启用查询改写（默认关闭） */
    private boolean enabled = false;

    /** 子查询数量 */
    private int subQueryCount = 3;

    /** 是否生成 HyDE 假设文档 */
    private boolean hydeEnabled = true;
}