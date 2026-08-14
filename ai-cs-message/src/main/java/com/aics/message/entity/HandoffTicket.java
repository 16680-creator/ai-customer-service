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
 * 转人工工单实体（对齐 handoff_ticket 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载 Agent 执行（{@link AgentRun}）无法自主完成、需要转人工处理时的工单信息，
 * 供坐席端分配与跟进。工单号 ticketNo 由服务端生成（HF + 时间戳 + 4位随机数，唯一）。
 * 已执行步骤以 JSON 数组字符串（{@link #executedSteps}）快照保存，便于坐席快速了解上下文。
 * </p>
 */
@Data
@TableName("handoff_ticket")
public class HandoffTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO) // 工单无业务主键，由数据库自增生成（工单号 ticketNo 才是业务唯一键）
    private Long id;

    /** 工单号（HF+时间戳+序号） */
    private String ticketNo;

    /** 所属执行ID */
    private String runId;

    /** 会话ID */
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 触发原因：POLICY_NOT_MET/NEGATIVE_SENTIMENT/EXECUTION_FAILED/USER_REQUEST */
    private String reason;

    /** 优先级：HIGH/NORMAL，默认 NORMAL */
    private String priority = "NORMAL";

    /** 关联订单号 */
    private String orderNo;

    /** 情绪 */
    private String sentiment;

    /** 问题摘要 */
    private String problemSummary;

    /** 已执行步骤清单（JSON数组） */
    private String executedSteps;

    /** 状态：OPEN/ASSIGNED/CLOSED，默认 OPEN */
    private String status = "OPEN";

    /** 分配坐席ID */
    private Long assignedAgent;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间：由 MetaObjectHandler 在插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
