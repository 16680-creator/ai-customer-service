-- ============================================================
-- AI客服系统 - 数据库初始化脚本
-- 包含: user_db、knowledge_db、chat_db
-- ============================================================

-- ==================== user_db 用户数据库 ====================
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;

-- 系统用户表
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

-- 系统角色表
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

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 初始化角色数据
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'admin', '管理员', '系统管理员，拥有所有权限'),
(2, 'agent', '客服', '客服人员，处理用户咨询'),
(3, 'user', '普通用户', '普通用户，使用客服服务')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 初始化管理员用户（密码: admin123 的BCrypt加密）
INSERT INTO sys_user (id, username, password, nickname, role, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin', 1)
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);


-- ==================== knowledge_db 知识库数据库 ====================
CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE knowledge_db;

-- 知识库分类表
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

-- 知识文档表
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

-- 知识标签表
CREATE TABLE IF NOT EXISTS kb_tag (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    name            VARCHAR(64)     NOT NULL COMMENT '标签名称',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识标签表';

-- 文档标签关联表
CREATE TABLE IF NOT EXISTS kb_document_tag (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    document_id     BIGINT          NOT NULL COMMENT '文档ID',
    tag_id          BIGINT          NOT NULL COMMENT '标签ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_tag (document_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档标签关联表';


-- ==================== chat_db 对话数据库 ====================
CREATE DATABASE IF NOT EXISTS chat_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chat_db;

-- 对话会话表
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

-- 对话消息表
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

-- 对话反馈表
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


-- ==================== Nacos 配置数据库 ====================
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nacos_config;

-- Nacos 所需表结构（官方标准）
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
    usage                       INT             NOT NULL DEFAULT 0 COMMENT '使用量',
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
    id                          BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'id',
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
    usage                       INT             NOT NULL DEFAULT 0 COMMENT '使用量',
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

-- Nacos 默认用户
INSERT INTO users (username, password, enabled) VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE) ON DUPLICATE KEY UPDATE username = VALUES(username);
INSERT INTO roles (username, role) VALUES ('nacos', 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE role = VALUES(role);
