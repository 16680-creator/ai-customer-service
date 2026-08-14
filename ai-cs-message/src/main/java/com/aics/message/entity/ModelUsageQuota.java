package com.aics.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型用量配额实体（对齐 model_usage_quota 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载用户在某场景下的模型用量配额（Token 配额/费用配额），是「LLM 可观测性、
 * 评估与成本治理」中成本治理的规则数据：按 (user_id, scenario) 唯一，配额 NULL 表示不限。
 * 关键字段：{@link #windowType}（窗口：DAILY/WEEKLY/MONTHLY，默认 DAILY）、
 * {@link #quotaTokens}（Token 配额）、{@link #quotaCost}（费用配额，单位元）、
 * {@link #periodStart}（窗口起始时间，用于滚动窗口对齐）。
 *
 * <h3>【设计原理】为什么按 (userId, scenario) 唯一</h3>
 * <p>配额是"用户 × 场景"维度配置的（同一用户 chat 与 rag 可配不同额度），表级
 * UNIQUE KEY uk_user_scenario 从数据库层兜底防重：服务层 upsert 先查后写即使存在
 * 并发窗口，重复插入也会被唯一键拒绝，幂等有最终保障。</p>
 *
 * <h3>【设计原理】为什么配额为 NULL 表示"不限"</h3>
 * <p>NULL 在 SQL 中语义是"未配置/未知"，天然表达"没有配过配额 = 不限制"；
 * 若用 0 或 -1 表示不限，统计与比较逻辑必须处处特判，且"配了 0（禁止调用）"与"没配（不限）"
 * 会混淆。NULL 让两者自然区分。</p>
 * </p>
 */
@Data
@TableName("model_usage_quota")
public class ModelUsageQuota implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO) // 配额记录无业务主键，由数据库自增生成
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 场景 */
    private String scenario;

    /** 窗口：DAILY/WEEKLY/MONTHLY，默认 DAILY */
    private String windowType = "DAILY"; // 默认最小粒度窗口：成本控制越细越早发现超支；periodStart 与窗口类型配合做滚动对齐

    /** Token配额（NULL=不限） */
    private Long quotaTokens;

    /** 费用配额（元，NULL=不限） */
    private BigDecimal quotaCost;

    /** 窗口起始时间 */
    private LocalDateTime periodStart;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间：由 MetaObjectHandler 在插入/更新时自动填充 */
    // fill=INSERT_UPDATE：insert 与 updateById 都会自动刷新 updateTime，保证"最后修改时间"可信
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
