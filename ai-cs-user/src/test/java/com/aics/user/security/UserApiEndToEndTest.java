package com.aics.user.security;

import com.aics.user.entity.User;
import com.aics.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户服务进程内端到端测试：真实 Controller、Security、Service、Mapper 与 H2。
 * 不连接网关、Nacos、MySQL，也不验证浏览器；不冒充真实部署联调。
 */
@SpringBootTest(properties = {
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.config.import=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:user_api_e2e;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:user-api-e2e-schema.sql",
        "management.otlp.tracing.export.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("api-e2e")
class UserApiEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("注册 → 重复注册 → 登录 → 密码安全存储")
    void registerDuplicateAndLoginFlow() throws Exception {
        String body = "{\"username\":\"e2e_alice\",\"password\":\"123456\",\"nickname\":\"Alice\"}";

        mockMvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("用户已存在"))
                .andExpect(result -> assertThat(utf8Body(result)).contains("用户已存在"))
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .startsWith(MediaType.APPLICATION_JSON_VALUE));

        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"e2e_alice\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("e2e_alice"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"e2e_alice\",\"password\":\"wrong-password\"}"))
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.data.token").doesNotExist());

        User saved = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "e2e_alice"));
        assertThat(saved.getPassword()).isNotEqualTo("123456").startsWith("$2");
        assertThat(saved.getRole()).isEqualTo("user");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    private static String utf8Body(MvcResult result) throws Exception {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("用户可按本人身份更新昵称，审计时间由真实 Mapper 刷新")
    void selfUpdateFlow() throws Exception {
        mockMvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"e2e_update\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
        User saved = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "e2e_update"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/user")
                        .header("X-User-Id", saved.getId().toString())
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + saved.getId()
                                + ",\"nickname\":\"Updated\",\"role\":\"admin\",\"status\":0,\"deleted\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(userMapper.selectById(saved.getId()).getNickname()).isEqualTo("Updated");
        User afterUpdate = userMapper.selectById(saved.getId());
        assertThat(afterUpdate.getRole()).isEqualTo("user");
        assertThat(afterUpdate.getStatus()).isEqualTo(1);
        assertThat(afterUpdate.getDeleted()).isEqualTo(0);
        assertThat(afterUpdate.getUsername()).isEqualTo("e2e_update");
        assertThat(afterUpdate.getPassword()).isEqualTo(saved.getPassword());
    }

    @Test
    @DisplayName("多字节密码达到 BCrypt 72 字节上限时可注册并登录")
    void multibytePasswordAtLimitFlow() throws Exception {
        String password = "密".repeat(24);
        String body = "{\"username\":\"e2e_multibyte\",\"password\":\"" + password + "\"}";

        mockMvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("真实 MVC 链路把非法 JSON 映射为 400 统一响应")
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("注册空白用户名不会到达数据库")
    void blankRegistrationDoesNotCreateUser() throws Exception {
        long countBefore = userMapper.selectCount(null);

        mockMvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"   \",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(userMapper.selectCount(null)).isEqualTo(countBefore);
    }
}
