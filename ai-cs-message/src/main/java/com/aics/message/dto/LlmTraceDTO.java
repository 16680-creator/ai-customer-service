package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LLM 调用链追踪请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载上报 LLM 调用链追踪（llm_trace 表）的入参，字段与实体对齐。
 * requestId 由调用方（chat 模块 LLM 编排链路）生成，作为幂等键：重复上报返回首次结果，
 * 不覆盖首次记录；status 不传时由实体初始值保证为 SUCCESS。
 *
 * <h3>【设计原理】为什么 DTO 与实体分离</h3>
 * <ul>
 *   <li>DTO 是"入参契约"：只暴露调用方允许传入的字段，配合 jakarta validation 在 Controller
 *       边界完成参数校验；实体字段再多（主键/自动填充/默认值）也不会被调用方越权写入；</li>
 *   <li>实体是"存储契约"：含 {@code @TableId/@TableField(fill)} 等持久化语义，
 *       两者解耦后 API 演进不影响表结构，表结构演进不影响 API 兼容性。</li>
 * </ul>
 *
 * <h3>【设计原理】为什么可选字段不在 DTO 里给默认值</h3>
 * <p>status/totalDurationMs 等可选字段默认值统一由实体初始值兜底（如 status="SUCCESS"），
 * DTO 保持"null=未传"的原义：既能区分"显式传 null"与"没传"，也避免 DTO 与实体两处
 * 默认值漂移导致行为不一致。</p>
 * </p>
 */
@Data
@Schema(description = "LLM 调用链追踪请求")
public class LlmTraceDTO {

    @Schema(description = "请求ID（UUID，幂等键）", example = "trace-uuid-001")
    // @NotBlank 在 Controller 的 @Valid 处触发：幂等键缺失时无法判重，直接以 400 拒绝（由全局异常处理器统一转 Result）
    @NotBlank(message = "请求ID不能为空") // 幂等键：requestId 已存在时服务端直接返回首次创建的 requestId
    private String requestId;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "会话ID（字符串 sessionKey，兼容普通对话与 Agent 流程）", example = "1001")
    private String sessionId;

    @Schema(description = "场景：chat/rag/agent/summary/vision/nl2sql/eval", example = "chat")
    @NotBlank(message = "场景不能为空")
    private String scenario;

    @Schema(description = "状态：SUCCESS/FAILED（默认 SUCCESS）", example = "SUCCESS")
    private String status; // 可选：不传时由实体初始值保证为 SUCCESS

    @Schema(description = "总耗时（毫秒）", example = "1200")
    private Long totalDurationMs;

    @Schema(description = "调用链 span 列表 JSON", example = "[{\"name\":\"llm-call\",\"durationMs\":1100}]")
    private String spansJson;

    @Schema(description = "失败摘要")
    private String errorSummary;
}
