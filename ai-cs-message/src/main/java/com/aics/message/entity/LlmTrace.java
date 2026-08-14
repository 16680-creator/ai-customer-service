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
 * LLM 调用链追踪实体（对齐 llm_trace 表）
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载一次 LLM 调用（或调用链）的追踪元数据，是「LLM 可观测性、评估与成本治理」
 * 能力的基础数据之一，用于链路追踪、失败分析、场景耗时统计。
 * 主键为调用方生成的 requestId（业务幂等键，{@link IdType#INPUT}），重复上报时按
 * requestId 幂等处理：已存在则直接返回，不覆盖首次记录。
 * 关键字段：{@link #scenario}（场景：chat/rag/agent/summary/vision/nl2sql/eval）、
 * {@link #status}（SUCCESS/FAILED）、{@link #spansJson}（调用链 span 列表 JSON）。
 *
 * <h3>【设计原理】为什么主键用 requestId 而不是数据库自增</h3>
 * <ul>
 *   <li>requestId 由调用方（chat 模块 LLM 编排链路）在一次调用开始时生成，先于落库存在，
 *       天然可作为全链路唯一标识随 Feign 调用 / MQ 消息传递；自增主键只能在入库后获得，
 *       无法用于"重复上报判重"。</li>
 *   <li>以 requestId 作主键后，幂等语义由数据库主键约束兜底：即使服务层先查后写存在并发窗口，
 *       重复插入也会被主键冲突拒绝，不会产生两条相同 requestId 的记录。</li>
 * </ul>
 *
 * <h3>【设计原理】为什么 sessionId 用 String 而非 Long</h3>
 * <p>普通对话的会话标识是字符串 sessionKey（如 "1001" 或 "session-abc"），而 Agent 流程使用
 * Long 型会话 ID；统一存 VARCHAR(64) 可无损兼容两种来源，避免在调用方做类型转换或丢失信息。</p>
 *
 * <h3>【设计原理】实体字段默认值必须与建表 SQL 默认值一致（双写一致）</h3>
 * <p>{@link #status}、{@link #totalDurationMs} 等字段在实体与 llm-observability-init.sql 中保持
 * 相同默认值：DTO 未传时由实体初始值兜底，即使某条写入路径绕过实体（如直连 SQL）也会被 DB 的
 * DEFAULT 再兜底一次，任何路径都不会写入 NULL 或脏值。</p>
 * </p>
 */
@Data
@TableName("llm_trace")
public class LlmTrace implements Serializable {

    private static final long serialVersionUID = 1L; // 固定序列化版本号：实体字段变更后旧字节流仍可反序列化，避免运行时 InvalidClassException

    /** 请求ID（UUID，幂等键，主键） */
    @TableId(type = IdType.INPUT) // 主键由调用方（chat 模块 LLM 编排链路）生成，非数据库自增
    private String requestId;

    /** 用户ID */
    private Long userId;

    /** 会话ID（普通对话为字符串 sessionKey，Agent 流程为数字字符串，统一按字符串存储） */
    private String sessionId;

    /** 场景：chat/rag/agent/summary/vision/nl2sql/eval */
    private String scenario;

    /** 状态：SUCCESS/FAILED，默认 SUCCESS */
    private String status = "SUCCESS"; // 默认值与建表 DEFAULT 'SUCCESS' 双写一致：DTO 未传时兜底，避免写入 NULL

    /** 总耗时（毫秒），默认 0 */
    private Long totalDurationMs = 0L;

    /** 调用链 span 列表 JSON（含各环节耗时/输入输出摘要） */
    private String spansJson;

    /** 失败摘要 */
    private String errorSummary;

    /** 创建时间：由 MetaObjectHandler 在插入时自动填充 */
    // fill=INSERT：插入时由 MybatisPlusConfig 的 MetaObjectHandler 注入 LocalDateTime.now()，
    // 业务代码零侵入维护时间字段；这也是为什么实体无需 setCreateTime 的原因
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
