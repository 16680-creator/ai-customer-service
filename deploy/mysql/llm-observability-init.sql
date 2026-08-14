-- ============================================================
-- AI客服系统 - LLM 可观测性、评估与成本治理（2026-08-14-llm-observability-cost-governance）
-- 新增 5 张表（全部在 chat_db），全部幂等，可重复执行
-- 用法: mysql -uroot -p < llm-observability-init.sql
-- ============================================================

-- ==================== 1. chat_db：LLM 可观测与成本治理 ====================
CREATE DATABASE IF NOT EXISTS chat_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chat_db;

-- LLM 调用链追踪（requestId 为业务幂等键，重复上报返回首次结果）
CREATE TABLE IF NOT EXISTS llm_trace (
    request_id        VARCHAR(64)     NOT NULL COMMENT '请求ID（幂等键）',
    user_id           BIGINT          DEFAULT NULL COMMENT '用户ID',
    session_id        VARCHAR(64)     DEFAULT NULL COMMENT '会话ID（字符串：普通对话 sessionKey 或 Agent 流程会话ID）',
    scenario          VARCHAR(32)     NOT NULL COMMENT '场景：chat/rag/agent/summary/vision/nl2sql/eval',
    status            VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAILED',
    total_duration_ms BIGINT          NOT NULL DEFAULT 0 COMMENT '总耗时（毫秒）',
    spans_json        TEXT            DEFAULT NULL COMMENT '调用链 span 列表 JSON',
    error_summary     VARCHAR(1024)   DEFAULT NULL COMMENT '失败摘要',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (request_id),
    KEY idx_user_id (user_id),
    KEY idx_scenario (scenario),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM调用链追踪表';

-- 模型用量计量（一次 LLM 调用的 Token 用量与估算费用）
CREATE TABLE IF NOT EXISTS model_usage (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_id      VARCHAR(64)     DEFAULT NULL COMMENT '请求ID',
    user_id         BIGINT          DEFAULT NULL COMMENT '用户ID',
    session_id      BIGINT          DEFAULT NULL COMMENT '会话ID',
    scenario        VARCHAR(32)     NOT NULL COMMENT '场景：chat/rag/agent/summary/vision/nl2sql/eval',
    provider        VARCHAR(64)     DEFAULT NULL COMMENT '模型供应商',
    model           VARCHAR(64)     NOT NULL COMMENT '模型名',
    input_tokens    INT             NOT NULL DEFAULT 0 COMMENT '输入Token数',
    output_tokens   INT             NOT NULL DEFAULT 0 COMMENT '输出Token数',
    total_tokens    INT             NOT NULL DEFAULT 0 COMMENT '总Token数',
    estimated_cost  DECIMAL(12,6)   NOT NULL DEFAULT 0 COMMENT '估算费用（元）',
    estimated       TINYINT         NOT NULL DEFAULT 0 COMMENT '是否估算（1=流式等无法获取精确usage）',
    status          VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAILED',
    error_summary   VARCHAR(1024)   DEFAULT NULL COMMENT '错误摘要',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, create_time),
    KEY idx_scenario_time (scenario, create_time),
    KEY idx_model_time (model, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型用量计量表';

-- 模型用量配额（按 userId+scenario 唯一，配额 NULL=不限）
CREATE TABLE IF NOT EXISTS model_usage_quota (
    id           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT          NOT NULL COMMENT '用户ID',
    scenario     VARCHAR(32)     NOT NULL COMMENT '场景',
    window_type  VARCHAR(16)     NOT NULL DEFAULT 'DAILY' COMMENT '窗口：DAILY/WEEKLY/MONTHLY',
    quota_tokens BIGINT          DEFAULT NULL COMMENT 'Token配额（NULL=不限）',
    quota_cost   DECIMAL(12,6)   DEFAULT NULL COMMENT '费用配额（元，NULL=不限）',
    period_start DATETIME        DEFAULT NULL COMMENT '窗口起始时间',
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_scenario (user_id, scenario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型用量配额表';

-- 线上采样评估记录（LLM-as-Judge 评分结果）
CREATE TABLE IF NOT EXISTS online_eval_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_id      VARCHAR(64)     DEFAULT NULL COMMENT '请求ID',
    session_id      BIGINT          DEFAULT NULL COMMENT '会话ID',
    user_id         BIGINT          DEFAULT NULL COMMENT '用户ID',
    question_digest VARCHAR(1024)   DEFAULT NULL COMMENT '问题摘要（截断）',
    answer_digest   VARCHAR(2048)   DEFAULT NULL COMMENT '回答摘要（截断）',
    llm_score       INT             DEFAULT NULL COMMENT 'LLM-as-Judge 评分（1-5）',
    judge_status    VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS' COMMENT '评分状态：SUCCESS/FAILED/SKIPPED',
    error_summary   VARCHAR(1024)   DEFAULT NULL COMMENT '评分失败摘要',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_request_id (request_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='线上采样评估记录表';

-- 用户反馈（点赞/点踩/评分/补充文本，requestId 未知时为 NULL）
CREATE TABLE IF NOT EXISTS user_feedback (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_id    VARCHAR(64)     DEFAULT NULL COMMENT '请求ID（未知时为NULL）',
    session_id    BIGINT          DEFAULT NULL COMMENT '会话ID',
    user_id       BIGINT          DEFAULT NULL COMMENT '用户ID',
    feedback_type VARCHAR(16)     NOT NULL COMMENT '反馈类型：LIKE/DISLIKE',
    score         INT             DEFAULT NULL COMMENT '评分（1-5，可选）',
    comment       VARCHAR(1024)   DEFAULT NULL COMMENT '补充文本',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_request_id (request_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈表';
