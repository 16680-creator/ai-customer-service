package com.aics.user.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * HeaderAuthenticationFilter 单测：可信头转 Authentication、多角色标准化、请求后清理上下文。
 */
class HeaderAuthenticationFilterTest {

    private final HeaderAuthenticationFilter filter = new HeaderAuthenticationFilter();

    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("可信身份头 - 构造 principal=userId + 标准化多角色")
    void trustedHeadersShouldCreateAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Name", "zhangsan");
        request.addHeader("X-User-Roles", "admin,ROLE_AGENT");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals("42", auth.getName());
            assertEquals("zhangsan", auth.getDetails());
            assertTrue(auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
            assertTrue(auth.getAuthorities().stream().anyMatch(a -> "ROLE_AGENT".equals(a.getAuthority())));
        };

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "请求结束必须清理 ThreadLocal，防身份串线");
    }

    @Test
    @DisplayName("无身份头 - 保持匿名，不构造 Authentication")
    void missingHeadersShouldStayAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
