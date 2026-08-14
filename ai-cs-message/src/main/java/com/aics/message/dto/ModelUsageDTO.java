package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型用量计量请求 DTO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载上报模型用量（model_usage 表）的入参，字段与实体对齐。
 * totalTokens 未传时由服务层按 inputTokens + outputTokens 兜底计算；
 * estimated 未传时默认 false（非估算）；status 未传时默认 SUCCESS。
 *
 * <h3>【设计原理】为什么用量字段用包装类型（Integer/Boolean）而非基本类型</h3>
 * <p>DTO 里"没传"与"传了 0"语义不同：包装类型 null 表示未上报，服务层据此做兜底
 * （如 totalTokens=input+output）；若用基本类型 int，调用方没传时反序列化为 0，
 * 服务层将无法区分"真的用了 0 token"与"没上报"，兜底逻辑就失效了。</p>
 *
 * <h3>【设计原理】为什么 totalTokens 兜底放在服务层而不是 DTO/实体</h3>
 * <p>兜底需要读取 inputTokens+outputTokens 两个字段求和，属于"业务计算"，
 * 放在 Service 层肉眼可见、可单测（见 ModelUsageServiceTest 的兜底用例）；
 * DTO 保持纯数据、实体默认 0 只是最后防线。</p>
 * </p>
 */
@Data
@Schema(description = "模型用量计量请求")
public class ModelUsageDTO {

    @Schema(description = "请求ID（关联 llm_trace）", example = "trace-uuid-001")
    private String requestId;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "会话ID", example = "1001")
    private Long sessionId;

    @Schema(description = "场景：chat/rag/agent/summary/vision/nl2sql/eval", example = "chat")
    // scenario/model 是计量统计（stats 过滤维度）的必填项：缺了它们，用量无法归属到正确的成本维度
    @NotBlank(message = "场景不能为空")
    private String scenario;

    @Schema(description = "模型供应商", example = "openai")
    private String provider;

    @Schema(description = "模型名", example = "gpt-4o")
    @NotBlank(message = "模型名不能为空")
    private String model;

    @Schema(description = "输入Token数", example = "120")
    private Integer inputTokens;

    @Schema(description = "输出Token数", example = "80")
    private Integer outputTokens;

    @Schema(description = "总Token数（未传时=输入+输出）", example = "200")
    private Integer totalTokens;

    @Schema(description = "估算费用（元）", example = "0.012")
    private BigDecimal estimatedCost;

    @Schema(description = "是否估算（1=流式等无法获取精确usage）", example = "false")
    private Boolean estimated;

    @Schema(description = "状态：SUCCESS/FAILED（默认 SUCCESS）", example = "SUCCESS")
    private String status; // 可选：不传时由实体初始值保证为 SUCCESS

    @Schema(description = "错误摘要")
    private String errorSummary;
}
