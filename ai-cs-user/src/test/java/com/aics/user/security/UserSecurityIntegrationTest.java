package com.aics.user.security;

import com.aics.common.result.Result;
import com.aics.user.controller.UserController;
import com.aics.user.mapper.UserMapper;
import com.aics.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityFilterChain + @PreAuthorize 真实集成测试（不只反射看注解）。
 */
@WebMvcTest(value = UserController.class, properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.config.import="
})
@Import({UserSecurityConfig.class, HeaderAuthenticationFilter.class})
class UserSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // UserApplication 的 @MapperScan 在 MVC 切片里仍会注册 Mapper，mock 掉以隔离持久层
    @MockBean
    private UserMapper userMapper;

    @Test
    @DisplayName("无可信身份头访问受保护接口 - 401 统一 Result")
    void unauthenticatedShouldReturn401() throws Exception {
        mockMvc.perform(get("/user/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未认证，请先登录"));
    }

    @Test
    @DisplayName("普通用户查询他人 - @PreAuthorize 拒绝 403")
    void otherUserShouldReturn403() throws Exception {
        mockMvc.perform(get("/user/42")
                        .header("X-User-Id", "7")
                        .header("X-User-Name", "alice")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限访问"));
    }

    @Test
    @DisplayName("本人查询本人 - 200；ADMIN 查询他人 - 200")
    void selfAndAdminShouldPass() throws Exception {
        when(userService.getUserById(42L)).thenReturn(Result.success());

        mockMvc.perform(get("/user/42")
                        .header("X-User-Id", "42")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/user/42")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }
}
