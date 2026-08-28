package com.aics.gateway.loadbalancer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 最少连接负载均衡器测试：在途数最少者优先，并列时轮询均摊。
 */
class LeastConnectionsLoadBalancerTest {

    private final InstanceInFlightRegistry registry = new InstanceInFlightRegistry();

    @SuppressWarnings("unchecked")
    private LeastConnectionsLoadBalancer newBalancer(List<ServiceInstance> instances) {
        ServiceInstanceListSupplier supplier = mock(ServiceInstanceListSupplier.class);
        when(supplier.get(any(Request.class))).thenReturn(Flux.just(instances));
        ObjectProvider<ServiceInstanceListSupplier> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenReturn(supplier);
        return new LeastConnectionsLoadBalancer(provider, "ai-cs-chat", registry);
    }

    private ServiceInstance instance(String instanceId) {
        ServiceInstance mock = mock(ServiceInstance.class);
        when(mock.getInstanceId()).thenReturn(instanceId);
        when(mock.getServiceId()).thenReturn("ai-cs-chat");
        return mock;
    }

    @Test
    void 在途数最少的实例优先() {
        ServiceInstance busy = instance("chat-1");
        ServiceInstance idle = instance("chat-2");
        registry.increment(InstanceInFlightRegistry.key("ai-cs-chat", "chat-1"));
        registry.increment(InstanceInFlightRegistry.key("ai-cs-chat", "chat-1"));

        LeastConnectionsLoadBalancer balancer = newBalancer(List.of(busy, idle));
        Object chosen = balancer.choose(mock(Request.class)).block().getServer();
        assertEquals("chat-2", ((ServiceInstance) chosen).getInstanceId(), "应选择在途数最少的 chat-2");
    }

    @Test
    void 并列最少时轮询均摊() {
        ServiceInstance a = instance("chat-1");
        ServiceInstance b = instance("chat-2");
        LeastConnectionsLoadBalancer balancer = newBalancer(List.of(a, b));

        Set<String> chosenIds = new java.util.HashSet<>();
        for (int i = 0; i < 10; i++) {
            Object chosen = balancer.choose(mock(Request.class)).block().getServer();
            chosenIds.add(((ServiceInstance) chosen).getInstanceId());
        }
        assertEquals(Set.of("chat-1", "chat-2"), chosenIds, "并列最少时应轮询均摊到两个实例");
    }

    @Test
    void 无候选实例返回空响应() {
        LeastConnectionsLoadBalancer balancer = newBalancer(List.of());
        assertFalse(balancer.choose(mock(Request.class)).block().hasServer(), "无实例时应返回空响应");
    }

    @Test
    void 计数器增减对称_归零后不越界() {
        String key = InstanceInFlightRegistry.key("ai-cs-chat", "chat-1");
        registry.increment(key);
        assertEquals(1, registry.current(key));
        registry.decrement(key);
        assertEquals(0, registry.current(key));
        // 防御性减一不得出现负数
        registry.decrement(key);
        assertEquals(0, registry.current(key));
    }
}
