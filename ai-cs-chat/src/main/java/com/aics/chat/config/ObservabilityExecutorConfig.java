package com.aics.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 可观测性线程池配置：用量上报等异步落库任务使用独立线程池，隔离主链路。
 *
 * <h3>【AI 技术详解】为什么可观测性任务必须用独立线程池？</h3>
 * <ul>
 *   <li><b>不占用 Tomcat 工作线程</b>：HTTP 请求线程数量有限（默认 200），
 *       若每个聊天请求都同步做 Feign 落库，高峰期线程会被下游拖垮，进而阻塞正常对话；</li>
 *   <li><b>故障隔离</b>：message 服务不可用时，异步任务只是排队/失败告警，
 *       主链路 LLM 调用不受影响——可观测性不能反过来"观测死"业务；</li>
 *   <li><b>削峰</b>：队列容量作为缓冲，瞬时爆发时任务排队而不是拒绝丢失。</li>
 * </ul>
 */
@Configuration
public class ObservabilityExecutorConfig {

    /**
     * 模型用量异步上报线程池。
     * <p>职责：执行 ModelUsageRecorder 的 Feign 落库任务，失败仅告警；
     * 独立线程池避免占用 Tomcat 工作线程，也避免阻塞请求主链路。</p>
     */
    @Bean(name = "usageExecutor")
    public ThreadPoolTaskExecutor usageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心 1 / 最大 4：用量上报是低频轻量任务，不需要大线程池，避免无谓的线程开销
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(4);
        // 队列 1000：削峰缓冲；拒绝策略默认 AbortPolicy，队列满时报错由调用方捕获告警
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("usage-report-");
        // 优雅停机：应用关闭前等待队列任务完成，避免停机瞬间丢失未落库的用量数据
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * 线上采样评估异步执行线程池。
     * <p>职责：执行 OnlineEvalService 的 LLM-as-Judge 评分任务（采样命中时），
     * 异步执行避免增加用户请求延迟；评分失败不重试。</p>
     */
    @Bean(name = "evalExecutor")
    public ThreadPoolTaskExecutor evalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // LLM-as-Judge 是额外的 LLM 调用：采样命中才执行，并发上限 2 控制额外的
        // token 成本与 API 并发，避免评估本身成为成本大头
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("online-eval-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
