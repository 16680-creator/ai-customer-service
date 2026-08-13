package com.aics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 售后申请请求
 */
@Data
@Schema(description = "售后申请请求")
public class AfterSaleApplyDTO {

    @Schema(description = "订单号", example = "20260801120000010001")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "商品ID（整单售后可为空）", example = "1001")
    private Long productId;

    @Schema(description = "售后数量", example = "1")
    @Min(value = 1, message = "售后数量至少为1")
    private Integer quantity = 1;

    @Schema(description = "售后动作：EXCHANGE/RETURN/REFUND", example = "EXCHANGE")
    @NotBlank(message = "售后动作不能为空")
    private String actionType;

    @Schema(description = "售后原因", example = "商品存在质量问题")
    @NotBlank(message = "售后原因不能为空")
    @Size(max = 512, message = "售后原因最长512个字符")
    private String reason;

    @Schema(description = "Agent 执行ID（来源可追溯）", example = "uuid-run-001")
    private String runId;

    @Schema(description = "幂等键（runId+action），重复提交返回首次结果", example = "uuid-run-001-EXCHANGE")
    @NotBlank(message = "幂等键不能为空")
    @Size(max = 64, message = "幂等键最长64个字符")
    private String idempotencyKey;

    @Schema(description = "证据/规则引用摘要（Agent 售后规则校验结果）", example = "满足规则 ASR-001（15 天期限内）")
    @Size(max = 1024, message = "证据摘要最长1024个字符")
    private String evidenceSummary;
}
