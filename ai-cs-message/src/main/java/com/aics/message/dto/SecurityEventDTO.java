package com.aics.message.dto;

import lombok.Data;

/**
 * 安全事件 DTO（3.2 F7 审计留痕，message 侧接收 chat 模块 Feign 上报）。
 */
@Data
public class SecurityEventDTO {

    /** 事件ID（UUID，幂等键） */
    private String eventId;

    /** 事件类型：PROMPT_INJECTION/CONTENT_REVIEW/TOOL_UNAUTHORIZED/RAG_ACL_DENIED/PII_MASKED/SQL_BLOCKED/RATE_LIMITED */
    private String type;

    /** 发生环节：INPUT/OUTPUT/TOOL/RETRIEVAL/GATEWAY/DEGRADE */
    private String stage;

    /** 用户ID */
    private Long userId;

    /** 会话ID（可选） */
    private String sessionId;

    /** Agent runId（可选） */
    private String runId;

    /** 命中规则/工具名/知识库标识 */
    private String rule;

    /** 敏感输入摘要（PII 脱敏 + 截断后） */
    private String inputDigest;

    /** 处理动作：BLOCK/ALLOW/FILTER */
    private String action;

    /** 详情/原因 */
    private String detail;
}
