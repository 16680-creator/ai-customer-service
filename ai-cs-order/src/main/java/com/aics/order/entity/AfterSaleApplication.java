package com.aics.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后申请实体
 */
@Data
@TableName("after_sale_application")
public class AfterSaleApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请单号（AS+时间戳+序号） */
    private String applicationNo;

    /** Agent 执行ID（来源可追溯） */
    private String runId;

    /** 幂等键（runId+action），重复提交返回首次结果 */
    private String idempotencyKey;

    /** 申请用户ID */
    private Long userId;

    /** 关联订单号 */
    private String orderNo;

    /** 商品ID（整单售后可为空） */
    private Long productId;

    /** 商品名称快照 */
    private String productName;

    /** 售后数量 */
    private Integer quantity;

    /** 售后动作：EXCHANGE/RETURN/REFUND */
    private String actionType;

    /** 售后原因 */
    private String reason;

    /** 证据/规则引用摘要 */
    private String evidenceSummary;

    /** 状态：PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
