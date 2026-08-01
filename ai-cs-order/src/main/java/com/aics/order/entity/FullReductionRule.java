package com.aics.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 满减规则实体
 */
@Data
@TableName("full_reduction_rule")
public class FullReductionRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;

    private BigDecimal thresholdAmount;

    private BigDecimal reductionAmount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
