package com.aics.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安全事件实体（对齐 security_event 表，3.2 F7 审计留痕）。
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载 AI 安全网关与 Guardrails 产生的全部安全事件（Prompt 注入拦截、
 * 内容审核、工具越权、RAG ACL 过滤、SQL 拦截等），由 chat 模块经 Feign 上报，
 * 供安全审计按 runId/requestId/userId 追溯。
 * 幂等键：{@link #eventId}（chat 侧生成的 UUID，重复上报跳过）。
 * </p>
 */
@Data
@TableName("security_event")
public class SecurityEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

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

    /** 命中规则/工具名/知识库标识 */
    private String rule;

    /** 敏感输入摘要（PII 脱敏 + 截断后，禁止明文） */
    private String inputDigest;

    /** 处理动作：BLOCK/ALLOW/FILTER */
    private String action;

    /** 详情/原因 */
    private String detail;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
