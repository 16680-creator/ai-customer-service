-- ============================================================
-- XXL-Job 调度中心建库建表脚本（v2.4.1）
-- 来源：https://github.com/xuxueli/xxl-job/blob/2.4.1/doc/db/tables_xxl_job.sql
-- 部署步骤见 learning-docs/07-运维部署/06-XXL-Job分布式调度.md
-- ============================================================

CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xxl_job;

CREATE TABLE if not exists xxl_job_info
(
    id                          int auto_increment comment '任务ID' primary key,
    job_desc                    varchar(255)    not null comment '任务描述',
    author                      varchar(64)     not null comment '负责人',
    alarm_email                 varchar(255)    null comment '报警邮件',
    schedule_type               varchar(50)     not null default 'NONE' comment '调度类型',
    schedule_conf               varchar(128)    null comment '调度配置',
    misfire_strategy            varchar(50)     not null default 'DO_NOTHING' comment '调度过期策略',
    executor_route_strategy     varchar(50)     not null comment '执行器路由策略',
    executor_handler            varchar(255)    not null comment '执行器任务Handler',
    executor_param              varchar(512)    null comment '执行器任务参数',
    executor_block_strategy     varchar(50)     not null comment '阻塞处理策略',
    executor_timeout            int             not null default 0 comment '任务执行超时时间(秒)',
    executor_fail_retry_count   int             not null default 0 comment '失败重试次数',
    glue_type                   varchar(50)     not null comment 'GLUE类型',
    glue_source                 mediumtext      null comment 'GLUE源代码',
    glue_remark                 varchar(128)    null comment 'GLUE备注',
    glue_updatetime             datetime        null comment 'GLUE更新时间',
    child_jobid                 varchar(255)    null comment '子任务ID',
    trigger_status              tinyint         not null default 0 comment '调度状态：0-停止 1-运行',
    trigger_last_time           bigint          not null default 0 comment '上次调度时间',
    trigger_next_time           bigint          not null default 0 comment '下次调度时间'
) engine = InnoDB default charset = utf8mb4 comment '任务信息表';

CREATE index idx_xxl_job_info_trigger_next_time on xxl_job_info (trigger_next_time);

CREATE TABLE if not exists xxl_job_log
(
    id                        bigint auto_increment primary key,
    job_group                 int           not null comment '执行器主键ID',
    job_id                    int           not null comment '任务主键ID',
    executor_address          varchar(255)  null comment '执行器地址',
    executor_handler          varchar(255)  null comment '任务Handler',
    executor_param            varchar(512)  null comment '任务参数',
    executor_sharding_param   varchar(20)   null comment '分片参数',
    executor_fail_retry_count int           not null default 0 comment '失败重试次数',
    trigger_time              datetime      null comment '调度时间',
    trigger_code              int           not null comment '调度结果码',
    trigger_msg               text          null comment '调度日志',
    handle_time               datetime      null comment '执行时间',
    handle_code               int           not null comment '执行结果码',
    handle_msg                text          null comment '执行日志',
    alarm_status              tinyint       not null default 0 comment '告警状态：0-默认 1-无需告警 2-告警成功 3-告警失败'
) engine = InnoDB default charset = utf8mb4 comment '任务日志表';

CREATE index idx_xxl_job_log_trigger_time on xxl_job_log (trigger_time);
CREATE index idx_xxl_job_log_handle_code on xxl_job_log (handle_code);

CREATE TABLE if not exists xxl_job_log_report
(
    id            int auto_increment primary key,
    trigger_day   datetime   not null comment '调度日期',
    running_count int        not null default 0 comment '运行中-日志数量',
    suc_count     int        not null default 0 comment '执行成功-日志数量',
    fail_count    int        not null default 0 comment '执行失败-日志数量',
    update_time   datetime   null
) engine = InnoDB default charset = utf8mb4 comment '任务日志报表';

CREATE unique index idx_xxl_job_log_report_trigger_day on xxl_job_log_report (trigger_day);

CREATE TABLE if not exists xxl_job_logglue
(
    id          int auto_increment primary key,
    job_id      int           not null comment '任务主键ID',
    glue_type   varchar(50)   null comment 'GLUE类型',
    glue_source mediumtext    null comment 'GLUE源代码',
    glue_remark varchar(128)  not null comment 'GLUE备注',
    add_time    datetime      null,
    update_time datetime      null
) engine = InnoDB default charset = utf8mb4 comment 'GLUE 记录表';

CREATE TABLE if not exists xxl_job_registry
(
    id             bigint auto_increment primary key,
    registry_group varchar(50)  not null,
    registry_key   varchar(255) not null,
    registry_value varchar(255) not null,
    update_time    datetime     null
) engine = InnoDB default charset = utf8mb4 comment '执行器注册表';

CREATE index idx_xxl_job_registry_t on xxl_job_registry (update_time);
CREATE unique index idx_xxl_job_registry_uk on xxl_job_registry (registry_group, registry_key, registry_value);

CREATE TABLE if not exists xxl_job_group
(
    id           int auto_increment primary key,
    app_name     varchar(64)  not null comment '执行器AppName',
    title        varchar(12)  not null comment '执行器名称',
    address_type tinyint      not null default 0 comment '执行器地址类型：0-自动注册 1-手动录入',
    address_list text         null comment '执行器地址列表',
    update_time  datetime     null
) engine = InnoDB default charset = utf8mb4 comment '执行器信息表';

CREATE TABLE if not exists xxl_job_user
(
    id         int auto_increment primary key,
    username   varchar(50) not null comment '账号',
    password   varchar(50) not null comment '密码',
    role       tinyint     not null comment '角色：0-普通用户 1-管理员',
    permission varchar(255) null comment '权限：执行器ID列表'
) engine = InnoDB default charset = utf8mb4 comment '用户表';

INSERT IGNORE INTO xxl_job_user (id, username, password, role, permission)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, '');

CREATE TABLE if not exists xxl_job_lock
(
    lock_name varchar(50) not null primary key
) engine = InnoDB default charset = utf8mb4 comment '锁表';

INSERT IGNORE INTO xxl_job_lock (lock_name) VALUES ('schedule_lock');
