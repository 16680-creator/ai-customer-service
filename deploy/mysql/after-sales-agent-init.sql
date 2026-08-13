-- ============================================================
-- AI客服系统 - 智能客服 Agent 编排与人工转接（005-after-sales-agent）
-- 新增 5 张表 + 售后规则种子文档，全部幂等，可重复执行
-- 用法: mysql -uroot -p < after-sales-agent-init.sql
-- ============================================================

-- ==================== 1. chat_db：Agent 执行轨迹 ====================
CREATE DATABASE IF NOT EXISTS chat_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chat_db;

-- Agent 执行记录
CREATE TABLE IF NOT EXISTS agent_run (
    run_id          VARCHAR(64)     NOT NULL COMMENT '执行ID（UUID）',
    session_id      BIGINT          NOT NULL COMMENT '会话ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    intent          VARCHAR(256)    DEFAULT NULL COMMENT '识别意图（多意图逗号分隔）',
    sentiment       VARCHAR(20)     DEFAULT NULL COMMENT '情绪：POSITIVE/NEUTRAL/NEGATIVE/ANGRY',
    status          VARCHAR(20)     NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING/WAITING_CONFIRM/COMPLETED/CANCELLED/HANDOFF/FAILED',
    current_step    INT             NOT NULL DEFAULT 0 COMMENT '当前步骤号',
    prompt_version  VARCHAR(32)     DEFAULT NULL COMMENT 'Prompt/规则版本',
    error_summary   VARCHAR(1024)   DEFAULT NULL COMMENT '失败摘要',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (run_id),
    KEY idx_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent执行记录表';

-- Agent 步骤轨迹
CREATE TABLE IF NOT EXISTS agent_step (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id          VARCHAR(64)     NOT NULL COMMENT '所属执行ID',
    step_no         INT             NOT NULL COMMENT '步骤序号',
    step_type       VARCHAR(32)     NOT NULL COMMENT '步骤类型：SAFETY/INTENT/LOCATE_ORDER/CHECK_POLICY/RECOMMEND/CONFIRM/EXECUTE/HANDOFF',
    tool_name       VARCHAR(64)     DEFAULT NULL COMMENT '工具名（无工具为空）',
    input_digest    VARCHAR(256)    DEFAULT NULL COMMENT '输入摘要（敏感字段脱敏）',
    output_digest   VARCHAR(1024)   DEFAULT NULL COMMENT '输出摘要',
    duration_ms     BIGINT          NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    status          VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAILED/SKIPPED',
    error_summary   VARCHAR(1024)   DEFAULT NULL COMMENT '错误摘要',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_step (run_id, step_no),
    KEY idx_run_id (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent步骤轨迹表';

-- 写操作确认记录
CREATE TABLE IF NOT EXISTS agent_confirmation (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id          VARCHAR(64)     NOT NULL COMMENT '所属执行ID',
    action          VARCHAR(32)     NOT NULL COMMENT '待确认动作：CREATE_EXCHANGE/CREATE_RETURN/CREATE_REFUND',
    payload_digest  VARCHAR(256)    NOT NULL COMMENT '操作摘要的SHA-256',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/REJECTED/EXPIRED',
    confirmed_by    BIGINT          DEFAULT NULL COMMENT '确认人（用户ID）',
    confirmed_at    DATETIME        DEFAULT NULL COMMENT '确认时间',
    timeout_at      DATETIME        NOT NULL COMMENT '确认超时时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_action (run_id, action),
    KEY idx_run_id (run_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent写操作确认表';

-- 转人工工单
CREATE TABLE IF NOT EXISTS handoff_ticket (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no       VARCHAR(32)     NOT NULL COMMENT '工单号（HF+时间戳+序号）',
    run_id          VARCHAR(64)     DEFAULT NULL COMMENT '所属执行ID',
    session_id      BIGINT          DEFAULT NULL COMMENT '会话ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    reason          VARCHAR(32)     NOT NULL COMMENT '触发原因：POLICY_NOT_MET/NEGATIVE_SENTIMENT/EXECUTION_FAILED/USER_REQUEST',
    priority        VARCHAR(16)     NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：HIGH/NORMAL',
    order_no        VARCHAR(32)     DEFAULT NULL COMMENT '关联订单号',
    sentiment       VARCHAR(20)     DEFAULT NULL COMMENT '情绪',
    problem_summary VARCHAR(2048)   DEFAULT NULL COMMENT '问题摘要',
    executed_steps  TEXT            DEFAULT NULL COMMENT '已执行步骤清单（JSON数组）',
    status          VARCHAR(20)     NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/ASSIGNED/CLOSED',
    assigned_agent  BIGINT          DEFAULT NULL COMMENT '分配坐席ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_run_id (run_id),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转人工工单表';

-- ==================== 2. ai_customer_service：售后申请 ====================
CREATE DATABASE IF NOT EXISTS ai_customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_customer_service;

CREATE TABLE IF NOT EXISTS after_sale_application (
    id               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    application_no   VARCHAR(32)     NOT NULL COMMENT '申请单号（AS+时间戳+序号）',
    run_id           VARCHAR(64)     DEFAULT NULL COMMENT 'Agent执行ID（来源可追溯）',
    idempotency_key  VARCHAR(64)     NOT NULL COMMENT '幂等键（runId+action），重复提交返回首次结果',
    user_id          BIGINT          NOT NULL COMMENT '申请用户ID',
    order_no         VARCHAR(32)     NOT NULL COMMENT '关联订单号',
    product_id       BIGINT          DEFAULT NULL COMMENT '商品ID（整单售后可为空）',
    product_name     VARCHAR(200)    DEFAULT NULL COMMENT '商品名称快照',
    quantity         INT             NOT NULL DEFAULT 1 COMMENT '售后数量',
    action_type      VARCHAR(20)     NOT NULL COMMENT '售后动作：EXCHANGE/RETURN/REFUND',
    reason           VARCHAR(512)    DEFAULT NULL COMMENT '售后原因',
    evidence_summary VARCHAR(1024)   DEFAULT NULL COMMENT '证据/规则引用摘要',
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    UNIQUE KEY uk_application_no (application_no),
    KEY idx_user_id (user_id),
    KEY idx_order_no (order_no),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后申请表';

-- ==================== 3. knowledge_db：售后规则种子文档 ====================
CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE knowledge_db;

-- 售后规则种子文档（供 Agent 规则 RAG 检索，knowledgeBase 标识 after-sale-rules，按 title 检索）
INSERT INTO kb_document (id, title, content, doc_type, summary, tags, category_id, status, create_by, deleted)
SELECT * FROM (
    SELECT 9001, 'ASR-001 耳机换货规则',
           '条款编号：ASR-001；适用动作：换货（EXCHANGE）；适用商品：耳机类商品；条件：商品存在非人为质量问题（如无法开机、声音异常、外观损坏非人为）；期限：自签收之日起 15 天内可申请换货；流程：用户提交换货申请，附质量问题描述；超出期限或人为损坏不予换货。',
           'txt', '耳机 15 天内质量问题可换货', 'after-sale-rules,换货', NULL, 1, 1, 0
    UNION ALL SELECT 9002, 'ASR-002 耳机退货规则',
           '条款编号：ASR-002；适用动作：退货（RETURN）；适用商品：耳机类商品；条件：商品完好、配件齐全、不影响二次销售；期限：自签收之日起 7 天内无理由退货；流程：用户提交退货申请，退货完成后退款。',
           'txt', '耳机 7 天无理由退货', 'after-sale-rules,退货', NULL, 1, 1, 0
    UNION ALL SELECT 9003, 'ASR-003 退款规则',
           '条款编号：ASR-003；适用动作：退款（REFUND）；条件：退货完成或订单取消后进入退款；期限：退货确认完成后 3 个工作日内原路退款；流程：退款原路返回至支付账户。',
           'txt', '退货完成后 3 个工作日内退款', 'after-sale-rules,退款', NULL, 1, 1, 0
) t
WHERE NOT EXISTS (SELECT 1 FROM kb_document d WHERE d.title = t.title)
ON DUPLICATE KEY UPDATE content = VALUES(content);
