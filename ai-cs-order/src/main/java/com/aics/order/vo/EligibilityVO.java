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
    private boolean eligible; // 是否可申请（false 时 reason 说明原因）

    @Schema(description = "不可申请原因（可申请时为空）")
    private String reason; // 不可申请原因，供 AI 客服向用户解释

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private String orderStatus; // 订单存在时回填，供解释资格结论使用
}
