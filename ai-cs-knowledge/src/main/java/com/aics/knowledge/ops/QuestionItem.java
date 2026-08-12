package com.aics.knowledge.ops;

import lombok.Data;

/**
 * 待聚类提问条目。
 */
@Data
public class QuestionItem {

    /** 提问 ID */
    private Long id;

    /** 提问文本 */
    private String text;
}