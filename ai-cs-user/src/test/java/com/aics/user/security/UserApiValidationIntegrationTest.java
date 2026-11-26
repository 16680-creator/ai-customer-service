package com.aics.user.security;

import com.aics.common.autoconfigure.CommonWebAutoConfiguration;
import com.aics.common.autoconfigure.Jackson2AutoConfiguration;
import com.aics.common.result.Result;
import com.aics.user.controller.UserController;
import com.aics.user.mapper.UserMapper;
import com.aics.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP 输入边界回归：使用真实 MVC、Jackson、Bean Validation 和 Security 过滤器链。 */
@WebMvcTest(value = UserController.class, properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.config.import="
})
@Import({UserSecurityConfig.class, HeaderAuthenticationFilter.class,
        Jackson2AutoConfiguration.class, CommonWebAutoConfiguration.class})
class UserApiValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"username\":\"\",\"password\":\"secret\"}",
            "{\"username\":\"   \",\"password\":\"secret\"}",
            "{\"username\":\"alice\",\"password\":\"\"}",
            "{\"username\":\"alice\",\"password\":\"   \"}",
            "{\"username\":null,\"password\":\"secret\"}",
            "{\"username\":\"alice\"}"
    })
    @DisplayName("登录空值或空白输入统一返回 400，不进入业务层")
    void loginBlankInputShouldReturn400(String body) throws Exception {
        postJson("/user/login", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).login(any());
    }

    @Test
    @DisplayName("登录非法 JSON 和错误字段类型统一返回 400")
    void loginMalformedJsonShouldReturn400() throws Exception {
        postJson("/user/login", "{\"username\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        postJson("/user/login", "{\"username\":{},\"password\":\"secret\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).login(any());
    }

    @Test
    @DisplayName("合法登录请求保持原有业务委托")
    void validLoginShouldReachService() throws Exception {
        when(userService.login(any())).thenReturn(Result.success());

        postJson("/user/login", "{\"username\":\"alice\",\"password\":\"secret\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"password\":\"123456\"}",
            "{\"username\":\"   \",\"password\":\"123456\"}",
            "{\"username\":\"alice\",\"password\":\"12345\"}",
            "{\"username\":\"alice\",\"password\":\"      \"}",
            "{\"username\":\"alice\"}"
    })
    @DisplayName("注册必填、空白及密码最短长度约束生效")
    void registerInvalidRequiredInputShouldReturn400(String body) throws Exception {
        postJson("/user/register", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("注册输入不得超过数据库列长及 BCrypt 输入上限")
    void registerTooLongInputShouldReturn400() throws Exception {
        String longUsername = "{\"username\":\"" + "a".repeat(65)
                + "\",\"password\":\"123456\"}";
        String longPassword = "{\"username\":\"alice\",\"password\":\""
                + "a".repeat(73) + "\"}";

        postJson("/user/register", longUsername)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        postJson("/user/register", longPassword)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        postJson("/user/register", "{\"username\":\"alice\",\"password\":\""
                + "密".repeat(25) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("注册字段达到已有长度上限时仍允许进入业务层")
    void registerMaximumLengthInputShouldBeAccepted() throws Exception {
        when(userService.register(any())).thenReturn(Result.success());

        String body = "{\"username\":\"" + "a".repeat(64)
                + "\",\"password\":\"" + "a".repeat(72) + "\"}";
        postJson("/user/register", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("更新缺失 ID 返回 400 而不是授权表达式 NPE")
    void updateWithoutIdShouldReturn400() throws Exception {
        putJson("/user", "{\"username\":\"alice\",\"id\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).updateUser(any());
    }

    @Test
    @DisplayName("非法数字 ID 和类型错误的更新 ID 均返回 400")
    void invalidIdTypesShouldReturn400() throws Exception {
        mockMvc.perform(get("/user/not-a-number")
                        .header("X-User-Id", "42")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        putJson("/user", "{\"id\":\"not-a-number\",\"username\":\"alice\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).updateUser(any());
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions putJson(String path, String body) throws Exception {
        return mockMvc.perform(put(path)
                .header("X-User-Id", "42")
                .header("X-User-Roles", "ROLE_ADMIN")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
