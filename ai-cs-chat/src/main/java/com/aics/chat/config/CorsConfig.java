package com.aics.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置
 */
// @Configuration  // CORS 统一由网关处理，避免重复响应头导致浏览器 Network Error（2026-08-07）
public class CorsConfig {

    /**
     * 配置全局 CORS 过滤器。
     *
     * <p>当前类已被注释掉 {@code @Configuration}（CORS 统一由网关处理），此 Bean 默认不会注册。
     * 保留代码以便需要在本服务单独启用 CORS 时直接放开 {@code @Configuration} 注解。</p>
     *
     * <p>策略说明：</p>
     * <ul>
     *   <li>{@code setAllowedOriginPatterns("*")}：用 Origin Patterns 而非 Origins，
     *       这样配合 {@code setAllowCredentials(true)} 不会被浏览器拒绝（Spring 5.3+ 推荐）。</li>
     *   <li>{@code setAllowCredentials(true)}：允许携带 Cookie，便于会话/JWT 鉴权。</li>
     *   <li>{@code setMaxAge(3600L)}：预检请求（OPTIONS）结果缓存 1 小时，减少浏览器预检次数。</li>
     * </ul>
     *
     * @return 全局 CORS 过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许任意来源（配合 allowCredentials 必须用 OriginPatterns）
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许的 HTTP 方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许任意请求头
        config.setAllowedHeaders(List.of("*"));
        // 允许携带 Cookie
        config.setAllowCredentials(true);
        // 预检结果缓存 1 小时
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
