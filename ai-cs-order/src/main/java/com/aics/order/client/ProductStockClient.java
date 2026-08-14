package com.aics.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 商品库存调用客户端（订单服务 -> 商品服务，走 Nacos 服务名 + 负载均衡 RestTemplate）。
 *
 * <p>库存以商品服务 DB 为权威源、实时扣减：订单创建时预占库存（调 {@link #deductStock}），
 * 取消 / 超时 / 退款时回补（调 {@link #restoreStock}）。对应商品服务
 * {@code ProductController} 的 {@code /product/{id}/stock/deduct} 与 {@code /product/{id}/stock/restore}。</p>
 *
 * <p>商品服务在库存不足时会返回 4xx，RestTemplate 抛出 {@code HttpClientErrorException}，
 * 由调用方捕获并回滚已扣项，保证「先扣后用、失败回补」的尽力一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockClient {

    private static final String BASE_URL = "http://ai-cs-product/product";

    private final RestTemplate restTemplate;

    /**
     * 实时扣减库存（商品服务 DB 原子扣减）。
     */
    public void deductStock(Long productId, int quantity) {
        restTemplate.put(BASE_URL + "/{id}/stock/deduct?quantity={quantity}",
                null, productId, quantity);
    }

    /**
     * 实时回补库存（商品服务 DB 原子回补）。
     */
    public void restoreStock(Long productId, int quantity) {
        restTemplate.put(BASE_URL + "/{id}/stock/restore?quantity={quantity}",
                null, productId, quantity);
    }
}
