package com.aics.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 网关可信身份头 → Spring Security Authentication 适配器。
 *
 * <p>本过滤器<strong>不校验 JWT</strong>：认证由网关 AuthFilter 完成；服务只把网关清洗并
 * 注入的 X-User-* 头转成 SecurityContext，负责方法级授权。客户端直连服务端口是部署层
 * 必须禁止的（K8s Service/防火墙只允许网关与内部服务访问）。</p>
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    static final String USER_ID = "X-User-Id";
    static final String USER_NAME = "X-User-Name";
    static final String USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(USER_ID);
        if (userId != null && !userId.isBlank()) {
            String roles = request.getHeader(USER_ROLES);
            List<SimpleGrantedAuthority> authorities = roles == null ? List.of() :
                    Arrays.stream(roles.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                            .map(SimpleGrantedAuthority::new)
                            .toList();
            // principal=userId：@PreAuthorize 可用 authentication.name 与路径 id 对比（本人访问）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(request.getHeader(USER_NAME));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // OncePerRequestFilter 跨线程池复用，必须清理 ThreadLocal，防身份串线
            SecurityContextHolder.clearContext();
        }
    }
}
