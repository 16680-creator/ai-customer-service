package com.aics.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 售后资格查询结果
 */
@Data
@Schema(description = "售后资格查询结果")
public class EligibilityVO {

    @Schema(description = "是否可申请售后")
    private boolean eligible;

    @Schema(description = "不可申请原因（可申请时为空）")
    private String reason;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private String orderStatus;
}
