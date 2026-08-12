package com.aics.chat.rag.rerank;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rerank 重排序结果条目
 *
 * <p>{@code index} 对应输入 documents 列表中的原始下标，
 * {@code relevanceScore} 为模型打分（0~1），
 * {@code text} 为命中文档文本（兼容 SiliconFlow 响应中的 {@code document} 字段）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RerankResultItem {

    /** 对应输入文档列表中的原始下标 */
    private int index;

    /** 相关度分数（0~1，越大越相关） */
    @JsonProperty("relevance_score")
    private double relevanceScore;

    /** 命中文档文本（兼容响应字段 document/text） */
    @JsonAlias("document")
    private String text;
}
