package com.aics.order.client;

import com.aics.common.result.Result;
import com.aics.order.dto.ProductRemoteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品服务 Feign 客户端（订单服务 -> 商品服务，经 Nacos 注册中心负载均衡）。
 *
 * <h3>学习要点（技术：OpenFeign / Seata XID 传播）</h3>
 * <ul>
 *   <li><b>声明式调用</b>：接口注解即 HTTP 契约，替代手写 RestTemplate 拼路径，
 *       编译期即可校验路径/参数（配合 FeignContractTest）。</li>
 *   <li><b>Seata XID 自动传播</b>：spring-cloud-starter-alibaba-seata 内置
 *       {@code SeataFeignClientAutoConfiguration}，全局事务发起后，
 *       {@code SeataFeignRequestInterceptor} 自动把 XID 写入 {@code TX_XID} 请求头；
 *       这是 RestTemplate 链路做不到的（旧实现因此使 product 分支游离在全局事务外）。</li>
 *   <li><b>分支事务成立条件</b>：调用方 XID 传播（本接口）+ 下游 MVC 拦截器绑定
 *       （product 侧由 seata-http 的 JakartaSeataWebMvcConfigurer 完成）+
 *       下游数据源代理（enable-auto-data-source-proxy）三者齐备。</li>
 * </ul>
 */
@FeignClient(name = "ai-cs-product", contextId = "productClient", path = "/product",
        fallbackFactory = com.aics.order.client.fallback.ProductClientFallbackFactory.class)
public interface ProductClient {

    /**
     * 实时扣减库存（商品服务 DB 原子扣减，库存不足返回 4xx → Feign 抛 FeignException）。
     * 在 Seata 全局事务内调用时，该请求成为 AT 分支事务。
     */
    @PutMapping("/{id}/stock/deduct")
    Result<Void> deductStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    /**
     * 实时回补库存（取消 / 超时 / 退款路径，不在下单全局事务内的独立调用）。
     */
    @PutMapping("/{id}/stock/restore")
    Result<Void> restoreStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    /**
     * 查询商品详情（购物车加购时取名称/价格/库存/上下架状态）。
     * 返回 {@link ProductRemoteDTO} 信封结构与商品服务 Result 响应对齐。
     */
    @GetMapping("/{id}")
    ProductRemoteDTO getProduct(@PathVariable("id") Long id);
}
