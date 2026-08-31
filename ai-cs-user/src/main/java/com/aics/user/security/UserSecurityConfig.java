package com.aics.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户服务安全配置：网关负责认证，user 服务负责授权。
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class UserSecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/register", "/user/login", "/actuator/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未认证，请先登录"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "无权限访问")))
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status);
        body.put("message", message);
        body.put("data", null);
        body.put("timestamp", System.currentTimeMillis());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
