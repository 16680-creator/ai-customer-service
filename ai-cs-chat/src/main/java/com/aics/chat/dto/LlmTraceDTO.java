package com.aics.chat.dto;

import lombok.Data;

/**
 * LLM 调用链追踪上报 DTO（chat 侧，与 ai-cs-message 的 LlmTraceDTO 对齐，用于 Feign 持久化）
 *
 * <h3>【AI 技术详解】为什么上报与查询要拆成 DTO / VO 两套类？</h3>
 * <ul>
 *   <li><b>写契约与读契约不对称</b>：上报 DTO 是 chat 侧"能提供什么"的契约；查询 VO 是
 *       message 侧"回读什么"的契约（多出服务端生成的 createTime）。合并成一个类会让两个
 *       方向互相污染：入参多出只读字段、出参多出必填约束，语义不清晰。</li>
 *   <li><b>跨服务演进安全</b>：Feign 契约按 DTO/VO 各自演进，写侧加字段不影响读侧，
 *       降低接口升级时两端的耦合与回归面。</li>
 * </ul>
 *
 * <p>requestId 由 chat 侧生成作为<b>幂等键</b>：Feign 底层是 HTTP，网络抖动可能触发重试，
 * message 侧以 requestId 去重——同一 requestId 重复上报只保留首条，保证一条调用链不落库两次。
 * sessionId 兼容普通对话的字符串 sessionKey 与 Agent 流程的 Long 会话 ID
 * （TraceRecorder 组装时统一转为字符串）。</p>
 */
@Data
public class LlmTraceDTO {

    /** 请求 ID（UUID，幂等键） */
    private String requestId;

    /** 用户 ID（可空） */
    private Long userId;

    /** 会话 ID（字符串，兼容 sessionKey 与 Long 会话 ID） */
    // 统一转成字符串落库：普通对话的 sessionKey 是 UUID 字符串，Agent 流程是 Long 会话 ID，
    // 入库列统一为 varchar，查询时无需区分两种 ID 类型
    private String sessionId;

    /** 场景：chat/rag/agent/summary/vision/nl2sql/eval */
    private String scenario;

    /** 状态：SUCCESS/FAILED */
    private String status;

    /** 总耗时（毫秒） */
    private Long totalDurationMs;

    /** 调用链 span 列表 JSON */
    // 整条调用链以 JSON 快照落库：各 span（LLM 调用/工具/文档检索/费用）结构随功能演进，
    // 存 JSON 免于为每个新字段加列迁移；查询端反序列化即可还原完整调用链
    private String spansJson;

    /** 失败摘要 */
    private String errorSummary;
}
