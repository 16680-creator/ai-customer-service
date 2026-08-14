-- ============================================================
-- AI客服系统 - AI 安全网关与 Guardrails（3.2 P0）
-- 新增 security_event 表（chat_db），全部幂等，可重复执行
-- 用法: mysql -uroot -p < security-guardrails-init.sql
-- ============================================================

-- ==================== chat_db：安全事件审计 ====================
CREATE DATABASE IF NOT EXISTS chat_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chat_db;

-- AI 安全事件审计表（Prompt 注入拦截/内容审核/工具越权/RAG ACL 过滤/SQL 拦截）
CREATE TABLE IF NOT EXISTS security_event (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id      VARCHAR(64)     NOT NULL COMMENT '事件ID（UUID，幂等键）',
    type          VARCHAR(32)     NOT NULL COMMENT '事件类型：PROMPT_INJECTION/CONTENT_REVIEW/TOOL_UNAUTHORIZED/RAG_ACL_DENIED/PII_MASKED/SQL_BLOCKED/RATE_LIMITED',
    stage         VARCHAR(16)     NOT NULL COMMENT '发生环节：INPUT/OUTPUT/TOOL/RETRIEVAL/GATEWAY/DEGRADE',
    user_id       BIGINT          DEFAULT NULL COMMENT '用户ID',
    session_id    VARCHAR(64)     DEFAULT NULL COMMENT '会话ID',
    run_id        VARCHAR(64)     DEFAULT NULL COMMENT 'Agent执行ID（与 agent_run 联合回放）',
    rule          VARCHAR(128)    DEFAULT NULL COMMENT '命中规则/工具名/知识库标识',
    input_digest  VARCHAR(512)    DEFAULT NULL COMMENT '敏感输入摘要（PII脱敏+截断，禁止明文）',
    action        VARCHAR(16)     NOT NULL COMMENT '处理动作：BLOCK/ALLOW/FILTER',
    detail        VARCHAR(1024)   DEFAULT NULL COMMENT '详情/原因',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_id (event_id),
    KEY idx_user_id (user_id),
    KEY idx_type_time (type, create_time),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI安全事件审计表';
