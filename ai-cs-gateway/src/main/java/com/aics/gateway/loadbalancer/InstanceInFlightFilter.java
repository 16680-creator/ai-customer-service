package com.aics.gateway.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 实例在途请求统计过滤器：负载均衡器选定实例后 +1，响应终结时 -1。
 *
 * <p>执行顺序紧随 {@link ReactiveLoadBalancerClientFilter}（order 10150 + 1）——
 * 只有此时 exchange 属性里才有"本次请求被分到哪个实例"的信息；
 * 用 {@code doFinally} 保证正常完成 / 异常 / 客户端断开三种终结路径都会减一，计数不泄漏。</p>
 *
 * <p>学习点：统计放这里而不是均衡器内部——均衡器的 choose() 只负责"选谁"，
 * 拿不到请求后续的生命周期；过滤器能包住 chain.filter(exchange) 的整个 Mono，
 * 是唯一能同时看到"选了谁"和"什么时候结束"的位置。</p>
 */
@Component
public class InstanceInFlightFilter implements GlobalFilter, Ordered {

    private final InstanceInFlightRegistry registry;

    public InstanceInFlightFilter(InstanceInFlightRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Object attribute = exchange.getAttributes()
                .get(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR);
        // Response 的泛型捕获类型无法直接向下转型，先经 Object 再 instanceof 收窄
        if (!(attribute instanceof Response<?> response)
                || !(response.getServer() instanceof ServiceInstance instance)) {
            // 未经过负载均衡（如直接转发 / 未选到实例）：不统计
            return chain.filter(exchange);
        }
        String key = InstanceInFlightRegistry.key(instance.getServiceId(), instance.getInstanceId());
        registry.increment(key);
        return chain.filter(exchange).doFinally(signal -> registry.decrement(key));
    }

    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;
    }
}
