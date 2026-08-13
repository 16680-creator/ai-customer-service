package com.aics.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转人工通知 DTO
 */
@Data
@Schema(description = "转人工通知请求")
public class HandoffNoticeDTO {

    @Schema(description = "客服工单号", example = "AS20250601001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工单号不能为空")
    private String ticketNo;

    @Schema(description = "用户ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "优先级（默认 NORMAL）", example = "NORMAL", defaultValue = "NORMAL")
    private String priority = "NORMAL";

    @Schema(description = "关联订单号（可选）", example = "ORD20250601001")
    private String orderNo;

    @Schema(description = "转人工摘要", example = "用户咨询退款进度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "转人工摘要不能为空")
    private String summary;
}
