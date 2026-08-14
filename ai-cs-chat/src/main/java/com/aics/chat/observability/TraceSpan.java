package com.aics.chat.observability;

import lombok.Data;

/**
 * 调用链中的一个 span（环节观测单元）
 *
 * <p>对应 docs/15 第 3.3 节的调用链模型：intent / retrieval / rerank / llm / tools / answer。
 * 每个 span 记录环节类型、模型、Token、耗时、状态与摘要，序列化为 JSON 后落库
 * （llm_trace.spans_json），敏感信息只保留摘要或截断，不落明文。</p>
 */
@Data
public class TraceSpan {

    /** 环节类型：INTENT / RETRIEVAL / RERANK / LLM / TOOL / ANSWER / SAFETY */
    private String spanType;

    /** 观测名（如 chat.llm / rag.retrieval / agent.tool） */
    private String name;

    /** 模型供应商（如 deepseek / siliconflow） */
    private String provider;

    /** 模型名（如 deepseek-chat / BAAI/bge-reranker-v2-m3） */
    private String model;

    /** 输入 Token 数（可空：流式取不到时为空） */
    private Integer promptTokens;

    /** 输出 Token 数（可空） */
    private Integer completionTokens;

    /** 首 Token 延迟（毫秒，流式场景） */
    private Long firstTokenMs;

    /** 环节耗时（毫秒） */
    private Long durationMs;

    /** 重试次数 */
    private Integer retries;

    /** 状态：SUCCESS / FAILED / SKIPPED */
    private String status = "SUCCESS";

    /** 错误摘要（失败时） */
    private String errorSummary;

    /** 环节明细（JSON 字符串：检索文档 ID、工具参数摘要、安全结果等） */
    private String detail;
}
