package com.aics.chat.dto;

import lombok.Data;

/**
 * 安全事件 DTO（chat 侧，经 Feign 上报 ai-cs-message 的 security_event 表）。
 *
 * <p>审计要求：敏感输入一律以脱敏摘要（{@link #inputDigest}）上报，不允许明文进入审计存储。</p>
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

    /** Agent runId（可选，便于与执行轨迹联合回放） */
    private String runId;

    /** 命中规则/工具名/知识库标识等 */
    private String rule;

    /** 敏感输入摘要（PII 脱敏 + 截断后） */
    private String inputDigest;

    /** 处理动作：BLOCK/ALLOW/FILTER/... */
    private String action;

    /** 详情/原因 */
    private String detail;
}
