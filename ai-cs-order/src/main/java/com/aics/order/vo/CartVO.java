package com.aics.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车展示 VO
 */
@Data
public class CartVO {

    private List<CartItemVO> items;

    private BigDecimal totalAmount;

    private Integer selectedCount;

    @Data
    public static class CartItemVO {
        private Long id;
        private Long productId;
        private String productName;
        private BigDecimal productPrice;
        private Integer quantity;
        private Boolean selected;
        private BigDecimal subtotal;
    }
}
