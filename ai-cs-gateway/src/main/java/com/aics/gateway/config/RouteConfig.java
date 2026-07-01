package com.aics.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置
 */
@Configuration
public class RouteConfig {

    /**
     * 自定义路由规则
     * 注意：生产环境建议通过 Nacos 配置中心动态管理路由，此处仅作本地开发兜底
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 用户服务
                .route("ai-cs-user", r -> r
                        .path("/api/user/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ai-cs-user"))
                // 知识库服务
                .route("ai-cs-knowledge", r -> r
                        .path("/api/knowledge/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ai-cs-knowledge"))
                // AI 对话服务
                .route("ai-cs-chat", r -> r
                        .path("/api/chat/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ai-cs-chat"))
                // 搜索服务
                .route("ai-cs-search", r -> r
                        .path("/api/search/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ai-cs-search"))
                // 消息服务
                .route("ai-cs-message", r -> r
                        .path("/api/message/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ai-cs-message"))
                // 通知服务
                .route("ai-cs-notify", r -> r
                        .path("/api/notify/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://ai-cs-notify"))
                .build();
    }
}
