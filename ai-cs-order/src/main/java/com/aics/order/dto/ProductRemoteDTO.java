package com.aics.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品服务返回的商品信息（order -> product 服务间调用）
 */
@Data
public class ProductRemoteDTO {

    private int code;

    private String message;

    private ProductData data;

    @Data
    public static class ProductData {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer stock;
        private Integer status;
    }
}