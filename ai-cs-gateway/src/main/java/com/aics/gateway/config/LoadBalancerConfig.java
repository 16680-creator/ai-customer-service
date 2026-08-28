package com.aics.gateway.config;

import com.aics.gateway.loadbalancer.InstanceInFlightRegistry;
import com.aics.gateway.loadbalancer.LeastConnectionsLoadBalancer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 负载均衡算法配置（轮询 / 最少连接）。
 *
 * <p>{@code aics.gateway.loadbalancer.algorithm}：
 * <ul>
 *   <li><b>round-robin</b>（默认）：不注册自定义均衡器，走 Spring Cloud LoadBalancer
 *       内置的 {@code RoundRobinLoadBalancer}（每个服务实例轮流分发）；</li>
 *   <li><b>least-connections</b>：注册 {@link LeastConnectionsLoadBalancer}，
 *       按"实例当前在途请求数最少"分流，适合请求耗时差异大的 AI 对话场景。</li>
 * </ul></p>
 *
 * <h3>学习点：LoadBalancer 子上下文</h3>
 * <p>Spring Cloud LoadBalancer 为每个下游服务创建独立子上下文，默认配置
 * {@code LoadBalancerClientConfiguration} 里的 RoundRobinLoadBalancer 带
 * {@code @ConditionalOnMissingBean}——此处通过 defaultConfiguration 提供的同类型 Bean
 * 存在时默认轮询自动退位，实现"配置切换算法、不改框架源码"。</p>
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfig.LeastConnectionsConfiguration.class)
public class LoadBalancerConfig {

    /**
     * 每个下游服务的 LoadBalancer 子上下文都会实例化该配置类：
     * 算法配置为 least-connections 时注册最少连接均衡器，否则不产生 Bean（回退轮询）。
     */
    public static class LeastConnectionsConfiguration {

        @Bean
        @ConditionalOnProperty(name = "aics.gateway.loadbalancer.algorithm", havingValue = "least-connections")
        public ReactorLoadBalancer<ServiceInstance> leastConnectionsLoadBalancer(
                Environment environment,
                LoadBalancerClientFactory loadBalancerClientFactory,
                InstanceInFlightRegistry registry) {
            String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
            ObjectProvider<ServiceInstanceListSupplier> supplierProvider =
                    loadBalancerClientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class);
            return new LeastConnectionsLoadBalancer(supplierProvider, serviceId, registry);
        }
    }
}
