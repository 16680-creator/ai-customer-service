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
 * Agent 执行记录实体（对齐 agent_run 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载一次 Agent 编排执行（run）的元数据，是 {@link AgentStep}、
 * {@link AgentConfirmation} 的父级聚合；执行结束后可转 {@link HandoffTicket} 人工处理。
 * 主键为业务侧生成的 runId（UUID，{@link IdType#INPUT}），与 chat 模块的 Agent 编排链路对齐。
 * 关键字段：{@link #status}（执行状态机）、{@link #currentStep}（当前步骤号）、
 * {@link #promptVersion}（Prompt/规则版本，便于审计回溯）。
 * </p>
 */
@Data
@TableName("agent_run")
public class AgentRun implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 执行ID（UUID，主键） */
    @TableId(type = IdType.INPUT)
    private String runId;

    /** 会话ID */
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 识别意图（多意图逗号分隔） */
    private String intent;

    /** 情绪：POSITIVE/NEUTRAL/NEGATIVE/ANGRY */
    private String sentiment;

    /** 状态：RUNNING/WAITING_CONFIRM/COMPLETED/CANCELLED/HANDOFF/FAILED，默认 RUNNING */
    private String status = "RUNNING";

    /** 当前步骤号，默认 0 */
    private Integer currentStep = 0;

    /** Prompt/规则版本 */
    private String promptVersion;

    /** 失败摘要 */
    private String errorSummary;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间：由 MetaObjectHandler 在插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
