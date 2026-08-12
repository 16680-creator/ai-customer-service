package com.aics.chat.rag.rerank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rerank 重排序结果条目
 *
 * <p>{@code index} 对应输入 documents 列表中的原始下标，
 * {@code relevanceScore} 为模型打分（0~1）。
 * SiliconFlow 响应中 {@code document} 是一个对象（{@code {"text":"..."}}），
 * 与 OpenAI 系接口返回纯字符串不同，因此用 {@link RerankDocument} 接收。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RerankResultItem {

    /** 对应输入文档列表中的原始下标 */
    private int index;

    /** 相关度分数（0~1，越大越相关） */
    @JsonProperty("relevance_score")
    private double relevanceScore;

    /** SiliconFlow 返回的命中文档对象（含 text 字段） */
    @JsonProperty("document")
    private RerankDocument document;

    /** 命中文档文本（由 document.text 提取，兼容旧调用方） */
    public String getText() {
        return document != null ? document.getText() : null;
    }

    /**
     * SiliconFlow rerank 命中文档对象。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RerankDocument {
        private String text;
    }
}
