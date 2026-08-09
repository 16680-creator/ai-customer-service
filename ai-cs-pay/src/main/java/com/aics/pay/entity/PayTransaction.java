package com.aics.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水表：记录每笔订单的支付渠道、渠道流水号、金额与状态（审计/对账基础）
 *
 * <p>status：PENDING 待支付 / SUCCESS 支付成功 / CLOSED 已关闭 / REFUNDING 退款中 / REFUNDED 已退款
 */
@Data
@TableName("pay_transaction")
public class PayTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String paymentMethod;

    /** 渠道流水号（支付宝 trade_no / 微信 transaction_id / 银联 queryId） */
    private String tradeNo;

    private BigDecimal payAmount;

    private String status;

    /** 渠道通知次数（幂等/防重审计） */
    private Integer notifyCount;

    private LocalDateTime payTime;

    private LocalDateTime refundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}