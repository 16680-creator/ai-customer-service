package com.aics.gateway.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 最少连接（Least Connections）负载均衡器。
 *
 * <p>在候选实例中选择"当前在途请求数最少"的实例；多个实例并列最少时，
 * 退化为轮询（AtomicInteger 计数器均摊），避免全部流量集中到同一个"恰好最少"的实例。</p>
 *
 * <h3>学习点：最少连接 vs 轮询</h3>
 * <ul>
 *   <li>轮询假设所有实例处理能力相同、每个请求耗时相近——AI 客服场景不成立：
 *       LLM 对话请求耗时从数百毫秒到数十秒不等，慢请求会在轮询下持续砸向同一批实例。</li>
 *   <li>最少连接按"实例当前正在处理的请求数"分流，请求耗时方差大时负载更均衡；
 *       代价是需要维护在途计数（见 {@link InstanceInFlightFilter}）。</li>
 *   <li>本类实现 Spring Cloud LoadBalancer 的 {@link ReactorServiceInstanceLoadBalancer}
 *       接口，通过 {@code @LoadBalancerClients(defaultConfiguration=...)} 注册进每个
 *       下游服务的 LoadBalancer 子上下文，替换默认的 RoundRobinLoadBalancer。</li>
 * </ul>
 */
public class LeastConnectionsLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final org.springframework.beans.factory.ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
    private final String serviceId;
    private final InstanceInFlightRegistry registry;

    /** 并列最少时的轮询计数器（跨请求共享，保证均摊） */
    private final AtomicInteger position = new AtomicInteger(0);

    public LeastConnectionsLoadBalancer(
            org.springframework.beans.factory.ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
            String serviceId,
            InstanceInFlightRegistry registry) {
        this.supplierProvider = supplierProvider;
        this.serviceId = serviceId;
        this.registry = registry;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(org.springframework.cloud.client.loadbalancer.Request request) {
        ServiceInstanceListSupplier supplier = supplierProvider.getIfAvailable(
                org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next().map(this::chooseInstance);
    }

    private Response<ServiceInstance> chooseInstance(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return new EmptyResponse();
        }
        // 第一轮：找最小在途数
        int minInFlight = Integer.MAX_VALUE;
        for (ServiceInstance instance : instances) {
            int inFlight = registry.current(
                    InstanceInFlightRegistry.key(serviceId, instance.getInstanceId()));
            minInFlight = Math.min(minInFlight, inFlight);
        }
        // 第二轮：收集所有并列最小的实例，轮询取一个（均摊突发）
        // minInFlight 在上轮循环中多次赋值，lambda 只能引用事实最终变量，故先拷贝
        final int min = minInFlight;
        List<ServiceInstance> candidates = instances.stream()
                .filter(instance -> registry.current(
                        InstanceInFlightRegistry.key(serviceId, instance.getInstanceId())) == min)
                .toList();
        ServiceInstance chosen = candidates.get(
                Math.floorMod(position.getAndIncrement(), candidates.size()));
        return new DefaultResponse(chosen);
    }
}
