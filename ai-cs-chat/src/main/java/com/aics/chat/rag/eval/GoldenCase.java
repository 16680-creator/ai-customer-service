package com.aics.chat.rag.eval;

import lombok.Data;

import java.util.List;

/**
 * golden 测试集条目。
 */
@Data
public class GoldenCase {

    /** 用例 ID */
    private String id;

    /** 用户问题 */
    private String question;

    /** 知识库标识 */
    private String knowledgeBase;

    /** 期望命中的文档 ID（用于 Recall/MRR/HitRate） */
    private List<String> expectedDocumentIds;

    /** 参考答案（用于 LLM-as-Judge，可空） */
    private String referenceAnswer;

    /** 期望包含的关键词（回答质量辅助判据，可空） */
    private List<String> expectedKeywords;
}