package com.aics.chat.observability;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Trace 上下文持有器：ThreadLocal + MDC，提供跨异步边界的显式捕获/恢复。
 *
 * <p>设计要点（见 design.md D5）：
 * <ul>
 *   <li><b>ThreadLocal 承载</b>：当前请求线程可直接 {@link #current()} 读取上下文；</li>
 *   <li><b>MDC 关联</b>：写入 {@code requestId}，让既有日志零成本带上请求 ID；</li>
 *   <li><b>显式传播</b>：进入异步边界（{@code CompletableFuture.supplyAsync}、SSE 订阅回调）前
 *       调用 {@link #capture()} 捕获上下文引用，在异步线程内 {@link #restore(TraceContext)} 恢复；
 *       TraceContext 内部是线程安全的（CopyOnWriteArrayList），同一引用可跨线程追加 span。</li>
 * </ul>
 * 采样：{@link #begin} 时按 {@link ObservabilityProperties#getSampleRate()} 决定是否创建上下文，
 * 未命中的请求不创建，业务链路完全不受影响。</p>
 */
public final class TraceContextHolder {

    private static final String MDC_REQUEST_ID = "requestId";

    private static final ThreadLocal<TraceContext> HOLDER = new ThreadLocal<>();

    private TraceContextHolder() {
    }

    /**
     * 开始一次请求的 trace：生成 requestId 并写入 ThreadLocal 与 MDC。
     * 采样未命中时返回 null（不创建上下文，业务不受影响）。
     *
     * @param properties 观测配置（enabled / sampleRate）
     * @param userId     用户 ID（可空）
     * @param sessionId  会话 ID（可空）
     * @param scenario   场景标识
     * @return 上下文；未启用或未命中采样时返回 null
     */
    public static TraceContext begin(ObservabilityProperties properties, Long userId, String sessionId, String scenario) {
        // 总开关关闭时不创建上下文：业务链路零侵入
        if (properties == null || !properties.isEnabled()) {
            return null;
        }
        // 采样率控制：rate<=0 不采集；rate>=1 全量；否则按概率
        // 学习点：采样是分布式追踪的经典降本手段——全量 trace 在高峰期存储/导出成本高，
        // 按概率抽样后仍可通过统计推断整体质量，代价是丢失少量请求的完整链路
        double rate = properties.getSampleRate();
        if (rate <= 0 || (rate < 1 && Math.random() >= rate)) {
            return null;
        }
        // requestId 用 UUID 保证全局唯一：它是 trace 查询与用量关联的幂等键，
        // 并发下若用自增/时间戳易碰撞，导致不同请求的 span 串线
        TraceContext ctx = new TraceContext(UUID.randomUUID().toString(), userId, sessionId, scenario);
        // ThreadLocal 绑定当前请求线程：Servlet 线程池复用时必须 clear，否则下个请求读到脏上下文
        HOLDER.set(ctx);
        // MDC 写入 requestId：SLF4J 的 MDC 会自动注入到 %X{requestId} 占位符，
        // 让既有 log.info 日志零改造即可按请求关联——这是"最小侵入"的关键设计
        MDC.put(MDC_REQUEST_ID, ctx.getRequestId());
        return ctx;
    }

    /**
     * 获取当前线程上下文（无上下文时返回 null）。
     */
    public static TraceContext current() {
        return HOLDER.get();
    }

    /**
     * 捕获当前上下文引用（跨异步边界前调用；无上下文时返回 null）。
     */
    public static TraceContext capture() {
        return HOLDER.get();
    }

    /**
     * 在异步线程内恢复上下文（含 MDC requestId）。
     *
     * <p>学习点：ThreadLocal 天然不能跨线程传递。Java 线程池（如
     * {@code CompletableFuture.supplyAsync}）执行任务时是新线程，读不到调用线程的 ThreadLocal。
     * 解决思路有三：① 显式传参（本类方案，capture/restore 同一引用）；② ThreadLocal 子类重写
     * {@code initialValue} 从父线程拷贝（InheritableThreadLocal，但线程池复用会串数据）；
     * ③ Reactor/TransmittableThreadLocal 自动传播（侵入框架）。本项目选显式方案：
     * 可控、可测、与既有代码风格一致。</p>
     */
    public static void restore(TraceContext ctx) {
        if (ctx == null) {
            return;
        }
        // 恢复同一 TraceContext 引用（span 列表是 CopyOnWriteArrayList，线程安全），
        // 异步线程追加的 span 会实时反映到请求主上下文中
        HOLDER.set(ctx);
        MDC.put(MDC_REQUEST_ID, ctx.getRequestId());
    }

    /**
     * 清理当前线程上下文与 MDC（请求结束 / 异步任务完成时调用）。
     *
     * <p>学习点：Tomcat 等 Servlet 容器使用线程池复用线程，若不清理 ThreadLocal，
     * 下一个请求会继承上一个请求的 requestId/上下文，导致 trace 串线与内存泄漏
     * （ThreadLocal 值被线程持有，无法被 GC）。故 begin/restore 与 clear 必须成对出现。</p>
     */
    public static void clear() {
        HOLDER.remove();
        MDC.remove(MDC_REQUEST_ID);
    }
}
