package com.aics.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体
 */
@Data
@TableName("coupon")
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String couponName;

    private BigDecimal amount;

    private BigDecimal minOrderAmount;

    private String status;

    private LocalDateTime expireTime;

    private LocalDateTime useTime;

    private String orderNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
