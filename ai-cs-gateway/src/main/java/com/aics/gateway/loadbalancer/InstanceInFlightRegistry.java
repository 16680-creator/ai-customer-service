package com.aics.gateway.loadbalancer;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实例在途请求计数器（最少连接负载均衡的依赖）。
 *
 * <p>记录每个下游实例当前正在处理的请求数：请求进入时 +1、响应结束（含异常/取消）时 -1。
 * 由 {@link InstanceInFlightFilter} 在负载均衡决策之后维护，
 * {@link LeastConnectionsLoadBalancer} 决策时读取最小值。</p>
 *
 * <h3>学习点：为什么最少连接需要全局计数器</h3>
 * <ul>
 *   <li>轮询只看"历史分发次数"，不看实例的真实负载——慢实例会积压在途请求；
 *       最少连接看"当前正在处理的请求数"，天然把新请求导向更空闲的实例。</li>
 *   <li>计数必须覆盖请求完整生命周期：用 {@code chain.filter(...).doFinally(...)}
 *       挂在响应终结信号上，正常完成/异常/客户端断开三条路径都会 -1，不泄漏计数。</li>
 *   <li>该实现为单实例内存版：网关多副本部署时各副本只见自己的在途数，
 *       属"每副本最少连接"，仍是负载感知的近似；跨副本精确统计需引入共享存储（如 Redis），
 *       但引入的读写延迟往往得不偿失。</li>
 * </ul>
 */
@Component
public class InstanceInFlightRegistry {

    /** key = serviceId:instanceId -> 在途请求数 */
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    /** 组合计数键：服务名 + 实例ID（实例ID在同服务的多副本间唯一） */
    public static String key(String serviceId, String instanceId) {
        return serviceId + ":" + instanceId;
    }

    /** 请求进入实例时 +1 */
    public void increment(String key) {
        counters.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    /** 请求结束时 -1（不低于 0，防御 doFinally 重复触发） */
    public void decrement(String key) {
        AtomicInteger counter = counters.get(key);
        if (counter != null) {
            counter.updateAndGet(current -> Math.max(0, current - 1));
        }
    }

    /** 读取实例当前在途请求数（未记录过返回 0） */
    public int current(String key) {
        AtomicInteger counter = counters.get(key);
        return counter == null ? 0 : counter.get();
    }
}
