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
 * 线上采样评估记录实体（对齐 online_eval_record 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载线上采样的 LLM 回答评估记录（LLM-as-Judge），是「LLM 可观测性、评估与
 * 成本治理」中质量评估的数据基础：采样问题/回答摘要 + 评分结果。
 * 关键字段：{@link #judgeStatus}（评分状态：SUCCESS/FAILED/SKIPPED，默认 SUCCESS）、
 * {@link #llmScore}（LLM-as-Judge 评分 1-5，评分失败时为空）。
 *
 * <h3>【设计原理】为什么是"采样"评估而非全量评估</h3>
 * <p>对每条 LLM 回答都跑一次 LLM-as-Judge 意味着"为评估再多花一次模型调用"，
 * 成本与延迟都不可接受，因此只对线上流量按策略采样（如按 requestId 哈希/比例）；
 * 问题与回答只存截断摘要（digest），控制存储体积，避免完整对话内容冗余落库。</p>
 *
 * <h3>【设计原理】为什么 judgeStatus 单独成列，而不靠 llmScore 是否为空推断</h3>
 * <p>评分可能因模型超时（FAILED，需告警）或策略跳过（SKIPPED，属允许行为）而未产出分数，
 * 两种语义需要区分；score 为空只能表达"没分数"，表达不了"为什么没分数"。</p>
 * </p>
 */
@Data
@TableName("online_eval_record")
public class OnlineEvalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO) // 评估记录无业务主键，由数据库自增生成
    private Long id;

    /** 请求ID（关联 llm_trace.request_id） */
    private String requestId;

    /** 会话ID */
    private Long sessionId;

    /** 用户ID */
    private Long userId;

    /** 问题摘要（截断） */
    private String questionDigest;

    /** 回答摘要（截断） */
    private String answerDigest;

    /** LLM-as-Judge 评分（1-5） */
    private Integer llmScore;

    /** 评分状态：SUCCESS/FAILED/SKIPPED，默认 SUCCESS */
    private String judgeStatus = "SUCCESS"; // 三态独立成列：FAILED（评分失败需告警）与 SKIPPED（策略跳过）语义不同，不能靠 score 为空推断

    /** 评分失败摘要 */
    private String errorSummary;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
