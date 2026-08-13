package com.aics.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 售后资格查询请求
 */
@Data
@Schema(description = "售后资格查询请求")
public class EligibilityQueryDTO {

    @Schema(description = "订单号", example = "20260801120000010001")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "售后动作：EXCHANGE/RETURN/REFUND", example = "EXCHANGE")
    @NotBlank(message = "售后动作不能为空")
    private String actionType;
}
