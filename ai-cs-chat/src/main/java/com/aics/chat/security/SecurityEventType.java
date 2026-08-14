package com.aics.chat.security;

/**
 * 安全事件类型（3.2 F7 审计留痕）。
 *
 * <p>与 Gherkin Feature 07 的场景一一对应：拦截、越权、违规、脱敏、SQL 拦截均产生一条事件。</p>
 */
public enum SecurityEventType {

    /** 输入 Guardrail：Prompt 注入检测命中 */
    PROMPT_INJECTION,

    /** 内容安全：输入/输出审核（含审核服务降级） */
    CONTENT_REVIEW,

    /** 工具授权：越权/角色权限不足 */
    TOOL_UNAUTHORIZED,

    /** RAG 检索：租户/角色/文档 ACL 过滤 */
    RAG_ACL_DENIED,

    /** PII 识别与脱敏（日志/trace 落库前） */
    PII_MASKED,

    /** NL2SQL 安全校验拦截 */
    SQL_BLOCKED,

    /** 网关限流（预留，网关侧记录） */
    RATE_LIMITED
}
