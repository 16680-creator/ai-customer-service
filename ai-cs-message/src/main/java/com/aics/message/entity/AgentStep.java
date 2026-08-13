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
 * Agent 步骤轨迹实体（对齐 agent_step 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载一次 Agent 执行（{@link AgentRun}）中的单个步骤轨迹，用于审计回放。
 * 表级唯一键 (run_id, step_no)，同一执行内步骤号唯一；同一步骤重复上报时按步骤号幂等覆盖。
 * 输入输出均以脱敏摘要（digest）形式保存，避免敏感数据落库。
 * </p>
 */
@Data
@TableName("agent_step")
public class AgentStep implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属执行ID */
    private String runId;

    /** 步骤序号 */
    private Integer stepNo;

    /** 步骤类型：SAFETY/INTENT/LOCATE_ORDER/CHECK_POLICY/RECOMMEND/CONFIRM/EXECUTE/HANDOFF */
    private String stepType;

    /** 工具名（无工具为空） */
    private String toolName;

    /** 输入摘要（敏感字段脱敏） */
    private String inputDigest;

    /** 输出摘要 */
    private String outputDigest;

    /** 耗时（毫秒），默认 0 */
    private Long durationMs = 0L;

    /** 状态：SUCCESS/FAILED/SKIPPED，默认 SUCCESS */
    private String status = "SUCCESS";

    /** 错误摘要 */
    private String errorSummary;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
