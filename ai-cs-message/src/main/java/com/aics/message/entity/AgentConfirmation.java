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
 * Agent 写操作确认记录实体（对齐 agent_confirmation 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载 Agent 执行（{@link AgentRun}）中高风险写操作（创建换货/退货/退款申请）的
 * 用户确认记录，用于追踪"待确认 → 已确认/已拒绝/已过期"的完整生命周期。
 * 表级唯一键 (run_id, action)，同一执行内同一动作仅一条确认记录；重复上报时幂等覆盖。
 * </p>
 */
@Data
@TableName("agent_confirmation")
public class AgentConfirmation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属执行ID */
    private String runId;

    /** 待确认动作：CREATE_EXCHANGE/CREATE_RETURN/CREATE_REFUND */
    private String action;

    /** 操作摘要的SHA-256 */
    private String payloadDigest;

    /** 状态：PENDING/CONFIRMED/REJECTED/EXPIRED，默认 PENDING */
    private String status = "PENDING";

    /** 确认人（用户ID） */
    private Long confirmedBy;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 确认超时时间 */
    private LocalDateTime timeoutAt;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
