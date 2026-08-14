package com.aics.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j 弹性容错配置说明。
 *
 * <p>配置已外移至 {@code application.yml}（resilience4j.*），
 * 由 Resilience4j Spring Boot 3 Starter 自动加载，
 * 本类仅保留配置说明文档。</p>
 *
 * <h3>配置说明</h3>
 * <ul>
 *   <li><b>chatService</b>：非流式 LLM 调用（chat / chatWithRag / compressHistory）</li>
 *   <li><b>sseChatService</b>：SSE 流式 LLM 调用（首次 token 到达限制）</li>
 * </ul>
 *
 * <h3>超时策略</h3>
 * <pre>
 *   非流式: 30s TimeLimiter → 指数退避重试(3次) → 熔断器(10次滑动窗口/50%阈值)
 *   SSE流式: 60s TimeLimiter → 熔断器(10次滑动窗口/50%阈值)
 * </pre>
 *
 * <h3>降级策略</h3>
 * <ul>
 *   <li>超时降级：返回"AI 助手响应超时，请稍后重试"</li>
 *   <li>熔断降级：返回"AI 助手当前负载较高，服务暂时不可用"</li>
 *   <li>重试耗尽：返回"AI 助手暂时繁忙，请稍后重试"</li>
 * </ul>
 */
@Configuration
public class Resilience4jConfig {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(Resilience4jConfig.class);

    // 请参见 application.yml 中的 resilience4j.* 配置
    // 通过 @TimeLimiter(name = "chatService") / @Retry(name = "chatService") / @CircuitBreaker(name = "chatService")
    // 在 ResilientAiService 中引用
}