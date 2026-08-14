-- ============================================================
-- AI客服系统 - 全量数据库初始化脚本（一键执行）
-- 包含 6 个库:
--   user_db          用户服务（含管理员 admin/admin123）
--   knowledge_db     知识库服务
--   chat_db          对话/消息服务
--   product_db       商品服务（含示例分类与商品数据）
--   ai_customer_service 订单服务（购物车/订单/优惠券/满减）
--   nacos_config     Nacos 注册中心持久化（含默认账号 nacos/nacos、命名空间 aics）
-- 全部语句幂等，可重复执行
-- 用法: mysql -uroot -p < all-init.sql
-- ============================================================

-- ==================== 1. user_db 用户数据库 ====================
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    username        VARCHAR(64)     NOT NULL COMMENT '用户名',
    password        VARCHAR(128)    NOT NULL COMMENT '密码（加密存储）',
    nickname        VARCHAR(64)     DEFAULT NULL COMMENT '昵称',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
    avatar          VARCHAR(512)    DEFAULT NULL COMMENT '头像URL',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    role            VARCHAR(32)     DEFAULT 'user' COMMENT '角色标识',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone),
    KEY idx_email (email),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    role_code       VARCHAR(64)     NOT NULL COMMENT '角色编码',
    role_name       VARCHAR(128)    NOT NULL COMMENT '角色名称',
    description     VARCHAR(512)    DEFAULT NULL COMMENT '角色描述',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'admin', '管理员', '系统管理员，拥有所有权限'),
(2, 'agent', '客服', '客服人员，处理用户咨询'),
(3, 'user', '普通用户', '普通用户，使用客服服务')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 管理员用户（密码: admin123 的BCrypt加密）
INSERT INTO sys_user (id, username, password, nickname, role, status) VALUES
(1, 'admin', '$2a$10$hEUYL34lpABtLWdPb.QC9uUnz0ehZwNrq9aOzdCjtmzvme0gf7.Fq', '系统管理员', 'admin', 1)
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- ==================== 2. knowledge_db 知识库数据库 ====================
CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE knowledge_db;

CREATE TABLE IF NOT EXISTS kb_category (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    name            VARCHAR(128)    NOT NULL COMMENT '分类名称',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父分类ID，0为顶级分类',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序序号',
    description     VARCHAR(512)    DEFAULT NULL COMMENT '分类描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分类表';

CREATE TABLE IF NOT EXISTS kb_document (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    title           VARCHAR(256)    NOT NULL COMMENT '文档标题',
    content         LONGTEXT        DEFAULT NULL COMMENT '文档内容',
    doc_type        VARCHAR(32)     DEFAULT NULL COMMENT '文档类型：pdf/docx/txt/markdown/html',
    source_url      VARCHAR(1024)   DEFAULT NULL COMMENT '文档来源URL',
    summary         VARCHAR(1024)   DEFAULT NULL COMMENT '文档摘要',
    tags            VARCHAR(1024)   DEFAULT NULL COMMENT '标签（逗号分隔）',
    category_id     BIGINT          DEFAULT NULL COMMENT '所属分类ID',
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-已索引 2-索引失败',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    KEY idx_create_by (create_by),
    KEY idx_doc_type (doc_type),
    FULLTEXT KEY ft_title_content (title, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文档表';

CREATE TABLE IF NOT EXISTS kb_tag (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    name            VARCHAR(64)     NOT NULL COMMENT '标签名称',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识标签表';

CREATE TABLE IF NOT EXISTS kb_document_tag (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    document_id     BIGINT          NOT NULL COMMENT '文档ID',
    tag_id          BIGINT          NOT NULL COMMENT '标签ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_tag (document_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档标签关联表';

-- 售后规则种子文档（005 售后 Agent，供规则 RAG 检索，标题 ASR-xxx）
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

-- ==================== 3. chat_db 对话数据库 ====================
CREATE DATABASE IF NOT EXISTS chat_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chat_db;

CREATE TABLE IF NOT EXISTS chat_session (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    agent_id        BIGINT          DEFAULT NULL COMMENT '客服ID',
    channel         VARCHAR(32)     DEFAULT 'web' COMMENT '渠道：web/app/wechat/api',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-已结束 1-进行中 2-转人工',
    title           VARCHAR(256)    DEFAULT NULL COMMENT '会话标题',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_agent_id (agent_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话表';

CREATE TABLE IF NOT EXISTS chat_message (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    session_id      BIGINT          NOT NULL COMMENT '会话ID',
    sender_type     TINYINT         NOT NULL COMMENT '发送者类型：1-用户 2-AI 3-客服',
    sender_id       BIGINT          DEFAULT NULL COMMENT '发送者ID',
    content         TEXT            NOT NULL COMMENT '消息内容',
    content_type    VARCHAR(32)     DEFAULT 'text' COMMENT '内容类型：text/image/file/rich',
    metadata        JSON            DEFAULT NULL COMMENT '元数据（附加信息）',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    KEY idx_sender (sender_type, sender_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

CREATE TABLE IF NOT EXISTS chat_feedback (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    session_id      BIGINT          NOT NULL COMMENT '会话ID',
    message_id      BIGINT          NOT NULL COMMENT '消息ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    rating          TINYINT         NOT NULL COMMENT '评分：1-不满意 2-一般 3-满意 4-非常满意',
    comment         VARCHAR(1024)   DEFAULT NULL COMMENT '反馈内容',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    KEY idx_message_id (message_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话反馈表';

-- ==================== 3.1 chat_db：Agent 执行轨迹（005 售后 Agent） ====================

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

-- ==================== 3.2 chat_db：LLM 可观测与成本治理 ====================

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

-- ==================== 4. product_db 商品数据库 ====================
CREATE DATABASE IF NOT EXISTS product_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE product_db;

CREATE TABLE IF NOT EXISTS product_category (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(128)    NOT NULL COMMENT '分类名称',
    parent_id       BIGINT          NOT NULL DEFAULT 0 COMMENT '父分类ID，0为顶级',
    sort            INT             NOT NULL DEFAULT 0 COMMENT '排序序号',
    description     VARCHAR(512)    DEFAULT NULL COMMENT '分类描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS product (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(200)    NOT NULL COMMENT '商品名称',
    description     TEXT            DEFAULT NULL COMMENT '商品描述',
    price           DECIMAL(10,2)   NOT NULL COMMENT '商品价格',
    stock           INT             NOT NULL DEFAULT 0 COMMENT '库存数量',
    category_id     BIGINT          DEFAULT NULL COMMENT '所属分类ID',
    image           VARCHAR(512)    DEFAULT NULL COMMENT '商品主图URL',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    sales           INT             NOT NULL DEFAULT 0 COMMENT '销量',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    KEY idx_name (name),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

INSERT INTO product_category (id, name, parent_id, sort) VALUES
(1, '数码配件', 0, 1),
(2, '生活用品', 0, 2),
(3, '食品饮料', 0, 3),
(4, '耳机音箱', 1, 1),
(5, '手机配件', 1, 2)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO product (id, name, description, price, stock, category_id, image, status, sales) VALUES
(1001, '无线蓝牙耳机', '高品质降噪蓝牙耳机，续航30小时', 199.00, 100, 4, 'https://img.example.com/earphone.jpg', 1, 50),
(1002, '手机壳', '防摔透明手机壳，适配多种机型', 29.00, 500, 5, 'https://img.example.com/case.jpg', 1, 200),
(1003, '便携充电宝', '20000mAh大容量，支持快充', 129.00, 80, 5, 'https://img.example.com/powerbank.jpg', 1, 120)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ==================== 5. ai_customer_service 订单数据库 ====================
CREATE DATABASE IF NOT EXISTS ai_customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_customer_service;

CREATE TABLE IF NOT EXISTS `cart_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称（冗余快照）',
    `product_price` DECIMAL(10,2) NOT NULL COMMENT '加入时单价',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `selected` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否选中',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车项';

CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '商品总金额（原价）',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠总金额',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '应付金额',
    `full_reduction_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '满减优惠金额',
    `coupon_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠券抵扣金额',
    `coupon_id` BIGINT DEFAULT NULL COMMENT '使用的优惠券ID',
    `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式（WECHAT/ALIPAY/BANK_CARD）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `expire_time` DATETIME NOT NULL COMMENT '支付截止时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号（冗余）',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    `product_price` DECIMAL(10,2) NOT NULL COMMENT '成交单价快照',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单项';

CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '持有用户ID',
    `coupon_name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '面额',
    `min_order_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛（满X元可用）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT '状态（UNUSED/USED/EXPIRED）',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `order_no` VARCHAR(32) DEFAULT NULL COMMENT '关联订单号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券';

CREATE TABLE IF NOT EXISTS `full_reduction_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `threshold_amount` DECIMAL(10,2) NOT NULL COMMENT '满足金额门槛',
    `reduction_amount` DECIMAL(10,2) NOT NULL COMMENT '减免金额',
    `start_time` DATETIME NOT NULL COMMENT '生效开始时间',
    `end_time` DATETIME NOT NULL COMMENT '生效结束时间',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled_time` (`enabled`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='满减规则';

-- ==================== 5.1 ai_customer_service：售后申请（005 售后 Agent） ====================
USE ai_customer_service;

CREATE TABLE IF NOT EXISTS after_sale_application (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `application_no` VARCHAR(32) NOT NULL COMMENT '申请单号（AS+时间戳+序号）',
    `run_id` VARCHAR(64) DEFAULT NULL COMMENT 'Agent执行ID（来源可追溯）',
    `idempotency_key` VARCHAR(64) NOT NULL COMMENT '幂等键（runId+action），重复提交返回首次结果',
    `user_id` BIGINT NOT NULL COMMENT '申请用户ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '关联订单号',
    `product_id` BIGINT DEFAULT NULL COMMENT '商品ID（整单售后可为空）',
    `product_name` VARCHAR(200) DEFAULT NULL COMMENT '商品名称快照',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '售后数量',
    `action_type` VARCHAR(20) NOT NULL COMMENT '售后动作：EXCHANGE/RETURN/REFUND',
    `reason` VARCHAR(512) DEFAULT NULL COMMENT '售后原因',
    `evidence_summary` VARCHAR(1024) DEFAULT NULL COMMENT '证据/规则引用摘要',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
    UNIQUE KEY `uk_application_no` (`application_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='售后申请表';

-- ==================== 6. nacos_config Nacos 注册中心数据库 ====================
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nacos_config;

CREATE TABLE IF NOT EXISTS config_info (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'id',
    data_id                     VARCHAR(255)    NOT NULL COMMENT 'data_id',
    group_id                    VARCHAR(128)    DEFAULT NULL COMMENT 'group_id',
    content                     LONGTEXT        NOT NULL COMMENT 'content',
    md5                         VARCHAR(32)     DEFAULT NULL COMMENT 'md5',
    gmt_create                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    src_user                    VARCHAR(128)    DEFAULT NULL COMMENT 'source user',
    src_ip                      VARCHAR(50)     DEFAULT NULL COMMENT 'source ip',
    app_name                    VARCHAR(128)    DEFAULT NULL COMMENT 'app_name',
    tenant_id                   VARCHAR(128)    DEFAULT '' COMMENT '租户字段',
    c_desc                      VARCHAR(256)    DEFAULT NULL COMMENT 'configuration description',
    c_use                       VARCHAR(64)     DEFAULT NULL COMMENT 'configuration usage',
    effect                      VARCHAR(64)     DEFAULT NULL COMMENT '配置生效的描述',
    type                        VARCHAR(64)     DEFAULT NULL COMMENT '配置的类型',
    c_schema                    LONGTEXT        DEFAULT NULL COMMENT '配置的模式',
    encrypted_data_key          VARCHAR(1024)   NOT NULL DEFAULT '' COMMENT '密钥',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_info_datagrouptenant (data_id, group_id, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='config_info';

CREATE TABLE IF NOT EXISTS config_info_aggr (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'id',
    data_id                     VARCHAR(255)    NOT NULL COMMENT 'data_id',
    group_id                    VARCHAR(128)    NOT NULL COMMENT 'group_id',
    datum_id                    VARCHAR(255)    NOT NULL COMMENT 'datum_id',
    content                     LONGTEXT        NOT NULL COMMENT '内容',
    gmt_modified                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    app_name                    VARCHAR(128)    DEFAULT NULL COMMENT 'app_name',
    tenant_id                   VARCHAR(128)    DEFAULT '' COMMENT '租户字段',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_info_aggr_datagrouptenantdatum (data_id, group_id, tenant_id, datum_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='增加租户字段';

CREATE TABLE IF NOT EXISTS config_info_beta (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'id',
    data_id                     VARCHAR(255)    NOT NULL COMMENT 'data_id',
    group_id                    VARCHAR(128)    NOT NULL COMMENT 'group_id',
    app_name                    VARCHAR(128)    DEFAULT NULL COMMENT 'app_name',
    content                     LONGTEXT        NOT NULL COMMENT 'content',
    beta_ips                    VARCHAR(1024)   DEFAULT NULL COMMENT 'betaIps',
    md5                         VARCHAR(32)     DEFAULT NULL COMMENT 'md5',
    gmt_create                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    src_user                    VARCHAR(128)    DEFAULT NULL COMMENT 'source user',
    src_ip                      VARCHAR(50)     DEFAULT NULL COMMENT 'source ip',
    tenant_id                   VARCHAR(128)    DEFAULT '' COMMENT '租户字段',
    encrypted_data_key          VARCHAR(1024)   NOT NULL DEFAULT '' COMMENT '密钥',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_info_beta_datagrouptenant (data_id, group_id, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='config_info_beta';

CREATE TABLE IF NOT EXISTS config_info_tag (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'id',
    data_id                     VARCHAR(255)    NOT NULL COMMENT 'data_id',
    group_id                    VARCHAR(128)    NOT NULL COMMENT 'group_id',
    tenant_id                   VARCHAR(128)    DEFAULT '' COMMENT 'tenant_id',
    tag_id                      VARCHAR(128)    NOT NULL COMMENT 'tag_id',
    app_name                    VARCHAR(128)    DEFAULT NULL COMMENT 'app_name',
    content                     LONGTEXT        NOT NULL COMMENT 'content',
    md5                         VARCHAR(32)     DEFAULT NULL COMMENT 'md5',
    gmt_create                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    src_user                    VARCHAR(128)    DEFAULT NULL COMMENT 'source user',
    src_ip                      VARCHAR(50)     DEFAULT NULL COMMENT 'source ip',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_info_tag_datagrouptenanttag (data_id, group_id, tenant_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='config_info_tag';

CREATE TABLE IF NOT EXISTS config_tags_relation (
    id                          BIGINT          NOT NULL COMMENT 'id',
    tag_name                    VARCHAR(128)    NOT NULL COMMENT 'tag_name',
    tag_type                    VARCHAR(64)     DEFAULT NULL COMMENT 'tag_type',
    data_id                     VARCHAR(255)    NOT NULL COMMENT 'data_id',
    group_id                    VARCHAR(128)    NOT NULL COMMENT 'group_id',
    tenant_id                   VARCHAR(128)    DEFAULT '' COMMENT 'tenant_id',
    nid                         BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'nid',
    PRIMARY KEY (nid),
    UNIQUE KEY uk_config_tags_relation_datagrouptenanttag (data_id, group_id, tenant_id, tag_name, tag_type),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='config_tag_relation';

CREATE TABLE IF NOT EXISTS group_capacity (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    group_id                    VARCHAR(128)    NOT NULL DEFAULT '' COMMENT 'Group ID',
    quota                       INT             NOT NULL DEFAULT 0 COMMENT '配额',
    `usage`                     INT             NOT NULL DEFAULT 0 COMMENT '使用量',
    max_size                    INT             NOT NULL DEFAULT 0 COMMENT '单个配置大小上限，单位为字节',
    max_aggr_count              INT             NOT NULL DEFAULT 0 COMMENT '聚合子配置最大个数',
    max_aggr_size               INT             NOT NULL DEFAULT 0 COMMENT '聚合子配置单个大小上限',
    max_history_count           INT             NOT NULL DEFAULT 0 COMMENT '最大变更历史数量',
    gmt_create                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='集群、各Group容量信息表';

CREATE TABLE IF NOT EXISTS his_config_info (
    id                          BIGINT          NOT NULL COMMENT 'id',
    nid                         BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'nid',
    data_id                     VARCHAR(255)    NOT NULL COMMENT 'data_id',
    group_id                    VARCHAR(128)    NOT NULL COMMENT 'group_id',
    app_name                    VARCHAR(128)    DEFAULT NULL COMMENT 'app_name',
    content                     LONGTEXT        NOT NULL COMMENT 'content',
    md5                         VARCHAR(32)     DEFAULT NULL COMMENT 'md5',
    gmt_create                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    src_user                    VARCHAR(128)    DEFAULT NULL COMMENT 'source user',
    src_ip                      VARCHAR(50)     DEFAULT NULL COMMENT 'source ip',
    op_type                     CHAR(10)        DEFAULT NULL COMMENT 'operation type',
    tenant_id                   VARCHAR(128)    DEFAULT '' COMMENT '租户字段',
    encrypted_data_key          VARCHAR(1024)   NOT NULL DEFAULT '' COMMENT '密钥',
    PRIMARY KEY (nid),
    KEY idx_gmt_create (gmt_create),
    KEY idx_did (data_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='多租户改造';

CREATE TABLE IF NOT EXISTS tenant_capacity (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id                   VARCHAR(128)    NOT NULL DEFAULT '' COMMENT 'Tenant ID',
    quota                       INT             NOT NULL DEFAULT 0 COMMENT '配额',
    `usage`                     INT             NOT NULL DEFAULT 0 COMMENT '使用量',
    max_size                    INT             NOT NULL DEFAULT 0 COMMENT '单个配置大小上限',
    max_aggr_count              INT             NOT NULL DEFAULT 0 COMMENT '聚合子配置最大个数',
    max_aggr_size               INT             NOT NULL DEFAULT 0 COMMENT '聚合子配置单个大小上限',
    max_history_count           INT             NOT NULL DEFAULT 0 COMMENT '最大变更历史数量',
    gmt_create                  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户容量信息表';

CREATE TABLE IF NOT EXISTS tenant_info (
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'id',
    kp                          VARCHAR(128)    NOT NULL COMMENT 'kp',
    tenant_id                   VARCHAR(128)    DEFAULT '' COMMENT 'tenant_id',
    tenant_name                 VARCHAR(128)    DEFAULT '' COMMENT 'tenant_name',
    tenant_desc                 VARCHAR(256)    DEFAULT NULL COMMENT 'tenant_desc',
    create_source               VARCHAR(32)     DEFAULT NULL COMMENT 'create_source',
    gmt_create                  BIGINT          NOT NULL COMMENT '创建时间',
    gmt_modified                BIGINT          NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_info_kptenantid (kp, tenant_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='tenant_info';

CREATE TABLE IF NOT EXISTS users (
    username                    VARCHAR(50)     NOT NULL COMMENT 'username',
    password                    VARCHAR(500)    NOT NULL COMMENT 'password',
    enabled                     TINYINT         NOT NULL DEFAULT 1 COMMENT 'enabled',
    PRIMARY KEY (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='users';

CREATE TABLE IF NOT EXISTS roles (
    username                    VARCHAR(50)     NOT NULL COMMENT 'username',
    role                        VARCHAR(50)     NOT NULL COMMENT 'role',
    PRIMARY KEY (username, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='roles';

CREATE TABLE IF NOT EXISTS permissions (
    role                        VARCHAR(50)     NOT NULL COMMENT 'role',
    resource                    VARCHAR(255)    NOT NULL COMMENT 'resource',
    action                      VARCHAR(8)      NOT NULL COMMENT 'action',
    PRIMARY KEY (role, resource, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='permissions';

-- Nacos 默认管理员 nacos/nacos
INSERT INTO users (username, password, enabled) VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE)
ON DUPLICATE KEY UPDATE username = VALUES(username);
INSERT INTO roles (username, role) VALUES ('nacos', 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- 预创建项目所需命名空间 aics（与代码中 namespace: aics 对应，kp 单机模式固定为 '1'）
INSERT INTO tenant_info (kp, tenant_id, tenant_name, tenant_desc, create_source, gmt_create, gmt_modified)
SELECT '1', 'aics', 'aics', 'AI客服系统命名空间', 'nacos', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tenant_info WHERE kp = '1' AND tenant_id = 'aics');
