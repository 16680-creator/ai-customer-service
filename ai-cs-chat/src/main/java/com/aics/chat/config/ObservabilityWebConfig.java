package com.aics.chat.config;

import com.aics.chat.observability.ObservabilityProperties;
import com.aics.chat.observability.TraceInterceptor;
import com.aics.chat.observability.TraceRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册 trace 拦截器（对话/Agent 请求自动开始与结束 LLM 调用链追踪）。
 *
 * <h3>【AI 技术详解】为什么用 MVC 拦截器而不是 Filter 或 AOP？</h3>
 * <ul>
 *   <li><b>拦截器能拿到精确的 URL 路径匹配</b>：addPathPatterns 按 Ant 风格路径
 *       精确圈定 /chat/** 与 /agent/**，Filter 则要手写路径判断；</li>
 *   <li><b>与 Spring MVC 生命周期契合</b>：preHandle 在 controller 执行前、postHandle
 *       在渲染前触发，天然适配"请求进入开始 trace、响应结束结束 trace"的语义；
 *       AOP 无法感知 URL 匹配，且对不入容器的方法（如健康检查）难以排除。</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class ObservabilityWebConfig implements WebMvcConfigurer {

    // 拦截器所需依赖通过构造器注入（@RequiredArgsConstructor）：采样配置用于决定
    // 本次请求是否开启 trace，TraceRecorder 负责组装 span 链并异步上报
    private final ObservabilityProperties observabilityProperties;
    private final ObjectFactory<TraceRecorder> traceRecorderFactory;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 仅拦截对话与 Agent 相关接口：这两类是 LLM 调用链的主要入口；
        // 静态资源/健康检查不参与 trace，避免无谓的 span 开销与监控噪音
        registry.addInterceptor(new TraceInterceptor(observabilityProperties, traceRecorderFactory))
                .addPathPatterns("/chat/**", "/agent/**");
    }
}
