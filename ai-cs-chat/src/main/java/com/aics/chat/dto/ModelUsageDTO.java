package com.aics.chat.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型用量上报 DTO（chat 侧，与 ai-cs-message 的 ModelUsageDTO 对齐，用于 Feign 持久化）
 *
 * <h3>【AI 技术详解】为什么用量要单独上报而不是只依赖 trace？</h3>
 * <ul>
 *   <li><b>计量与追踪的留存策略不同</b>：trace 关注"一次请求发生了什么"（调用链），
 *       usage 关注"消耗了多少 token/钱"（成本核算），后者需要按用户/场景/模型聚合统计，
 *       独立成表便于 SQL 聚合与配额计算。</li>
 *   <li><b>requestId 关联两条记录</b>：usage 通过 requestId 反查 llm_trace，
 *       成本异常时能定位到具体调用链。</li>
 *   <li><b>estimated 标记估算与精确</b>：流式场景下 Spring AI 往往取不到 usage，此时按
 *       输出字符数估算 token 并标记 estimated=true，成本看板据此区分"实付"与"预估"，
 *       避免把估算值当精确值做配额强校验。</li>
 * </ul>
 */
@Data
public class ModelUsageDTO {

    /** 请求 ID（关联 llm_trace） */
    private String requestId;

    /** 用户 ID */
    private Long userId;

    /** 会话 ID */
    private Long sessionId;

    /** 场景：chat/rag/agent/summary/vision/nl2sql/eval */
    private String scenario;

    /** 模型供应商 */
    private String provider;

    /** 模型名 */
    private String model;

    /** 输入 Token 数 */
    private Integer inputTokens;

    /** 输出 Token 数 */
    private Integer outputTokens;

    /** 总 Token 数 */
    private Integer totalTokens;

    /** 估算费用（元） */
    // 费用 = token 数 × 单价，由 chat 侧按模型单价表估算（元）
    private BigDecimal estimatedCost;

    /** 是否估算 */
    // true=按字符/输出估算的 token（流式取不到 usage 时）；false=模型返回的精确 usage。
    // 配额强校验只认精确值，估算值仅用于成本展示
    private Boolean estimated;

    /** 状态：SUCCESS/FAILED */
    // 失败调用也落一条 usage（token 为 0），用于统计失败率与成本损失归因
    private String status;

    /** 错误摘要 */
    private String errorSummary;
}
