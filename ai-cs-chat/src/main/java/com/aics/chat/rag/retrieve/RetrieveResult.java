package com.aics.chat.rag.retrieve;

import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 统一检索结果。
 */
@Data
public class RetrieveResult {

    /** 实际执行的查询 */
    private String query;

    /** 命中文档（含 metadata.knowledgeBase/documentId/title/page） */
    private List<Document> documents;

    /** 实际执行的检索模式 */
    private String mode;

    /** 是否发生降级 */
    private boolean degraded;

    /** 降级原因 */
    private String degradeReason;
}