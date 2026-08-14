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
 * 模型用量计量实体（对齐 model_usage 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载一次 LLM 调用的 Token 用量与估算费用，是「LLM 可观测性、评估与成本治理」
 * 中成本计量与配额治理的数据基础。
 * 关键字段：{@link #model}（模型名）、{@link #totalTokens}（总 Token 数）、
 * {@link #estimatedCost}（估算费用，单位元）、{@link #estimated}（是否估算：流式等
 * 无法获取精确 usage 时置 1，成本按估算值计入）。
 *
 * <h3>【设计原理】为什么费用是"估算"而不是精确计费</h3>
 * <ul>
 *   <li>非流式调用可从响应元数据拿到精确 usage（prompt/completion tokens），费用可精确计算；</li>
 *   <li>流式调用多数供应商取不到 usage，只能按 tokenizer 或输入/输出长度估算，因此用
 *       {@link #estimated} 标记，成本看板可区分"精确值"与"估算值"，避免把估算当精确误导决策；</li>
 *   <li>金额统一用 BigDecimal 而非 double：费用涉及累加与比较，double 的二进制浮点误差会随
 *       累加放大，DECIMAL(12,6) 与 BigDecimal 一一对应，规避精度问题。</li>
 * </ul>
 *
 * <h3>【设计原理】为什么这是"追加型"日志表</h3>
 * <p>每次 LLM 调用对应一条用量记录，只增不改（失败也保留，便于成本归因），
 * 因此 id 用自增主键、不做唯一约束，写入性能最优；统计聚合在 Service 层内存完成。</p>
 * </p>
 */
@Data
@TableName("model_usage")
public class ModelUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO) // AUTO=由 DB 自增：追加型日志数据无业务主键，自增写入性能最优、天然按写入序归档
    private Long id;

    /** 请求ID（关联 llm_trace.request_id） */
    private String requestId;

    /** 用户ID */
    private Long userId;

    /** 会话ID */
    private Long sessionId;

    /** 场景：chat/rag/agent/summary/vision/nl2sql/eval */
    private String scenario;

    /** 模型供应商 */
    private String provider;

    /** 模型名 */
    private String model;

    /** 输入Token数，默认 0 */
    private Integer inputTokens = 0;

    /** 输出Token数，默认 0 */
    private Integer outputTokens = 0;

    /** 总Token数，默认 0（未上报时由服务层按 input+output 兜底计算） */
    private Integer totalTokens = 0;

    /** 估算费用（元），默认 0 */
    private BigDecimal estimatedCost = BigDecimal.ZERO; // 默认 0 而非 null：统计求和时无需判空即可累加（见 ModelUsageServiceImpl.stats）

    /** 是否估算（1=流式等无法获取精确usage），默认 false */
    private Boolean estimated = false;

    /** 状态：SUCCESS/FAILED，默认 SUCCESS */
    private String status = "SUCCESS";

    /** 错误摘要 */
    private String errorSummary;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
