package com.aics.knowledge.ops;

import lombok.Data;

/**
 * FAQ 收录建议。
 */
@Data
public class FaqSuggestion {

    /** FAQ 问题 */
    private String question;

    /** FAQ 答案 */
    private String answer;

    /** 知识库标识（默认 faq） */
    private String knowledgeBase;

    /** 来源主题 ID */
    private String clusterTopicId;
}