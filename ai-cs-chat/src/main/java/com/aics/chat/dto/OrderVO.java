package com.aics.chat.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图对象（chat 侧 DTO，与 ai-cs-order 的 OrderVO 字段一致，用于 Feign 反序列化）
 *
 * <p>chat 服务通过 {@link com.aics.chat.feign.OrderFeignClient} 调用 ai-cs-order，
 * 接收方需要与生产方字段名/类型完全一致才能正确反序列化，故在此镜像一份。</p>
 */
@Data
public class OrderVO {

    /** 订单号 */
    private String orderNo;

    /** 订单状态（与 ai-cs-order 状态机一致） */
    private String status;

    /** 订单总金额（原价） */
    private BigDecimal totalAmount;

    /** 优惠总额（满减 + 优惠券） */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 满减金额 */
    private BigDecimal fullReductionAmount;

    /** 优惠券抵扣金额 */
    private BigDecimal couponAmount;

    /** 支付方式（如 ALIPAY/WECHAT） */
    private String paymentMethod;

    /** 商品明细列表 */
    private List<OrderItemVO> items;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 待支付截止时间（超过自动取消） */
    private LocalDateTime expireTime;

    /** 支付链接（未支付订单返回） */
    private String payUrl;

    /**
     * 订单商品项 VO
     */
    @Data
    public static class OrderItemVO {
        /** 商品 ID */
        private Long productId;
        /** 商品名称 */
        private String productName;
        /** 商品单价 */
        private BigDecimal productPrice;
        /** 购买数量 */
        private Integer quantity;
        /** 小计金额 */
        private BigDecimal subtotal;
    }
}