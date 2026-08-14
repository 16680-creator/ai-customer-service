package com.aics.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 调用链追踪响应 VO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载 LLM 调用链追踪（llm_trace 表）的查询响应，字段与实体对齐，
 * 用于链路追踪查看与失败分析。
 *
 * <h3>【设计原理】为什么查询返回 VO 而不是直接返回实体</h3>
 * <ul>
 *   <li>实体携带 MyBatis-Plus 注解与序列化细节，直接返回会让"响应契约"与"持久化结构"
 *       强耦合：表加列就会悄悄改变 API 响应；VO 显式声明响应字段，契约稳定；</li>
 *   <li>VO 字段与实体一一对应但可独立演进（如将来对 spansJson 做脱敏、裁剪），
 *       不影响表结构与写入路径。</li>
 * </ul>
 * </p>
 */
@Data
@Schema(description = "LLM 调用链追踪响应")
public class LlmTraceVO {

    @Schema(description = "请求ID", example = "trace-uuid-001")
    private String requestId;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "会话ID（字符串 sessionKey）", example = "1001")
    private String sessionId;

    @Schema(description = "场景：chat/rag/agent/summary/vision/nl2sql/eval", example = "chat")
    private String scenario;

    @Schema(description = "状态：SUCCESS/FAILED", example = "SUCCESS")
    private String status;

    @Schema(description = "总耗时（毫秒）", example = "1200")
    private Long totalDurationMs;

    @Schema(description = "调用链 span 列表 JSON")
    private String spansJson;

    @Schema(description = "失败摘要")
    private String errorSummary;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
